package uw.kv;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.LimitedVirtualThreadExecutor;
import uw.common.util.SystemClock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 基于 AtomicLong 和 Redis 的复合（融合）计数器。
 * <p>
 * 本地通过 AtomicLong 高频累加，按 {@code syncGlobalMillis} 间隔原子化地将增量同步到 Redis（{@link GlobalCounter}），
 * 兼顾写入性能与全局一致性。支持配置回写函数，按 {@code writeBackMillis} 间隔异步回写数据库（背压受限虚拟线程执行）；
 * 支持 key 过期：周期性窗口计数推荐 {@code keyExpireAtResolver}（INCREX PXAT 固定回收时刻=周期终点+buffer，
 * 周期内静默不提前回收），亦可 {@code keyExpireMillis}（滑动续期，仅适合活动窗口型 key）——
 * 均需 Redis Open Source 8.8+，低版本自动降级为裸 INCRBY（计数保全，TTL 回收失效，见
 * {@link GlobalCounter#incrementTtl}）。
 * 本地实例表按容量 W-TinyLFU 逐出（逐出前冲刷增量并补终态回写）。
 * <p>
 * 使用前应在 static 块中调用 {@link #config} 注册 counterType；未注册时首次调用会按默认 60s 同步间隔自动配置。
 * <p>
 * 注意：本地计数与 Redis 之间存在 {@code syncGlobalMillis} 级别的最终一致延迟，强一致读请用 {@code get(type, id, true)}。
 */
public class FusionCounter {

    private static final Logger log = LoggerFactory.getLogger(FusionCounter.class);

    /**
     * 默认同步全局时间间隔为1分钟。
     */
    private static final long DEFAULT_SYNC_GLOBAL_MILLIS = 60_000L;

    /**
     * 本地计数器实例表的默认容量上限（每 counterType 独立计；超容逐出最久未访问实例，
     * 量级参照 FusionCache 的 localCacheMaxNum 放大，覆盖周期翻转产生的新实例尖峰）。
     */
    private static final long DEFAULT_LOCAL_CACHE_MAX_NUM = 100_000L;

    /**
     * 回写任务执行器（背压受限虚拟线程）：Semaphore 限并发 + 有界队列缓冲，并发与队列全满时丢弃任务。
     * <p>
     * writeBack 为绝对值快照覆盖，丢弃由下轮回写自愈（flush 终态回写丢弃则 DB 滞后一段，逐出低频可接受）；
     * getRejectedCount() 可观测丢弃量。不用 Block/CallerRuns 策略——submit 发生在 sync 调用线程
     * （业务消费热路径），阻塞策略会把 DB 延迟传导回业务线程。
     */
    private static final LimitedVirtualThreadExecutor writeBackExecutor = new LimitedVirtualThreadExecutor(
            "uw-counter-writeback", 8, 1000, new LimitedVirtualThreadExecutor.DiscardPolicy());

    /**
     * key 是 counterType，value 是该类型的包装（类型级配置 + 本地实例表）。
     * <p>
     * 对齐 {@link FusionCache} 的 CacheWrapper 结构：每 counterType 独立持有配置与 Caffeine 实例表，
     * 实例表 key 直接用 counterId 对象（免拼 {@code type:id} 字符串，热路径少一次分配），
     * 配置随包装走（不再单列 configMap，热路径少一次哈希查找）。同一 counterType 的 counterId
     * 类型须一致（Long/String 混用会得到不同实例）。
     */
    private static final ConcurrentMap<String, CounterWrapper> cacheWrapperMap = new ConcurrentHashMap<>();

    /**
     * 计数器类型包装：类型级 {@link LocalCounter.Config} + 该类型的本地实例表。
     * <p>
     * 本地实例表采用 Caffeine（W-TinyLFU 按容量逐出）：周期类 counterId（如编码周期起点的窗口计数）会随时间
     * 无限新增实例，裸 Map 只增不减将泄漏本地内存。超容逐出旧实例时经 evictionListener
     * 同步（executor=Runnable::run）冲刷其本地增量到 Redis 后再丢弃；显式 delete/getAndDelete 走
     * {@code asMap().remove}，不触发逐出监听（由调用方语义决定是否冲增量）。
     * 逐出冲刷与并发写入的竞态残余、冲刷失败的下场与进程崩溃丢增量同语义（ERROR 留痕，方向为少记）。
     */
    private static final class CounterWrapper {

        /**
         * 类型级配置。
         */
        private final LocalCounter.Config config;

        /**
         * 该类型的本地计数器实例表（key=counterId 对象）。
         */
        private final Cache<Object, LocalCounter> localCounterCache;

        CounterWrapper(LocalCounter.Config config) {
            this.config = config;
            this.localCounterCache = Caffeine.newBuilder()
                    .maximumSize(DEFAULT_LOCAL_CACHE_MAX_NUM)
                    .executor(Runnable::run)
                    .evictionListener((Object key, LocalCounter localCounter, RemovalCause cause) -> localCounter.flush())
                    .build();
        }

        /**
         * 获取或创建本地计数器实例（不存在时以包装内配置构造）。
         */
        private LocalCounter getLocalCounter(Object counterId) {
            return localCounterCache.get(counterId, key -> new LocalCounter(config, counterId, 0));
        }

        /**
         * 获取或创建本地计数器实例（不存在时以 initNum 构造：Redis 无值则写入作基线）。
         */
        private LocalCounter getLocalCounter(Object counterId, long initNum) {
            return localCounterCache.get(counterId, key -> new LocalCounter(config, counterId, initNum));
        }

        /**
         * 冲刷并丢弃该类型全部本地实例的增量（重复 config 替换包装前调用，防增量随旧实例丢失）。
         */
        private void flushAll() {
            for (LocalCounter localCounter : localCounterCache.asMap().values()) {
                localCounter.flush();
            }
        }
    }

    /**
     * 配置计数器。
     *
     * @param entityType       entity类型(主要用于构造counterType)。
     * @param syncGlobalMillis 同步全局间隔毫秒数
     */
    public static void config(final Class<?> entityType, final long syncGlobalMillis) {
        config(entityType.getSimpleName(), syncGlobalMillis);
    }


    /**
     * 初始化计数器。
     *
     * @param counterType      计数器类型。
     * @param syncGlobalMillis 同步全局间隔毫秒数
     */
    public static void config(final String counterType, final long syncGlobalMillis) {
        registerConfig(counterType, new LocalCounter.Config(counterType, syncGlobalMillis));
    }

    /**
     * 初始化计数器。
     *
     * @param entityType        entity类型(主要用于构造counterType)。
     * @param syncGlobalMillis  同步全局间隔毫秒数
     * @param writeBackMillis   回写数据库间隔毫秒数
     * @param writeBackConsumer 回写函数，可以在此函数中写入数据库。
     */
    public static void config(final Class<?> entityType, final long syncGlobalMillis, final long writeBackMillis, final BiConsumer<Object, Long> writeBackConsumer) {
        config(entityType.getSimpleName(), syncGlobalMillis, writeBackMillis, writeBackConsumer);
    }

    /**
     * 初始化计数器。
     *
     * @param counterType       计数器类型。
     * @param syncGlobalMillis  同步全局间隔毫秒数
     * @param writeBackMillis   回写数据库间隔毫秒数
     * @param writeBackConsumer 回写函数，可以在此函数中写入数据库。
     */
    public static void config(final String counterType, final long syncGlobalMillis, final long writeBackMillis, final BiConsumer<Object, Long> writeBackConsumer) {
        registerConfig(counterType, new LocalCounter.Config(counterType, syncGlobalMillis, writeBackMillis, writeBackConsumer));
    }

    /**
     * 初始化计数器（带 key 过期时间）。
     *
     * @param counterType      计数器类型。
     * @param syncGlobalMillis 同步全局间隔毫秒数
     * @param writeBackMillis  回写数据库间隔毫秒数
     * @param writeBackConsumer 回写函数，可以在此函数中写入数据库。
     * @param keyExpireMillis  Redis key 过期毫秒数（滑动续期，0=常驻不过期）。大于 0 时同步走 INCREX
     *                         （需 Redis Open Source 8.8+），适用于周期性窗口计数（key 过期=计数作废，
     *                         由 Redis 自动回收，免外部清理任务）；常驻计数器（依赖 setIfAbsent 基线恢复）必须传 0。
     */
    public static void config(final String counterType, final long syncGlobalMillis, final long writeBackMillis,
                              final BiConsumer<Object, Long> writeBackConsumer, final long keyExpireMillis) {
        registerConfig(counterType, new LocalCounter.Config(counterType, syncGlobalMillis, writeBackMillis, writeBackConsumer, keyExpireMillis));
    }

    /**
     * 初始化计数器（带 key 绝对回收时刻解析函数，周期性窗口计数推荐用法）。
     *
     * @param counterType        计数器类型。
     * @param syncGlobalMillis   同步全局间隔毫秒数
     * @param writeBackMillis    回写数据库间隔毫秒数
     * @param writeBackConsumer  回写函数，可以在此函数中写入数据库。
     * @param keyExpireAtResolver 由 counterId 解析该 key 的绝对回收时刻（Unix 毫秒时间戳）。
     *                            同步走 INCREX PXAT（需 Redis Open Source 8.8+）：回收时点固定（幂等重设同一时刻），
     *                            周期内静默多久都不会提前回收——周期窗口计数必须用本模式而非滑动 TTL
     *                            （滑动 TTL 若小于周期内静默窗，恢复访问时计数已清零）。
     *                            返回 null 或 &lt;=0 时本次降级按常驻处理（不设过期）。函数异常按同步失败处理（增量回滚重试）。
     */
    public static void config(final String counterType, final long syncGlobalMillis, final long writeBackMillis,
                              final BiConsumer<Object, Long> writeBackConsumer, final Function<Object, Long> keyExpireAtResolver) {
        registerConfig(counterType, new LocalCounter.Config(counterType, syncGlobalMillis, writeBackMillis, writeBackConsumer, 0, keyExpireAtResolver));
    }

    /**
     * 注册/替换类型包装：重复 config 时先冲刷旧包装全部本地增量（防随旧实例丢失），再替换并告警
     * （config 应仅在 static 块调用一次，运行时重复注册属于误用）。
     */
    private static void registerConfig(String counterType, LocalCounter.Config config) {
        CounterWrapper oldWrapper = cacheWrapperMap.put(counterType, new CounterWrapper(config));
        if (oldWrapper != null) {
            oldWrapper.flushAll();
            log.warn("FusionCounter重复config,旧本地实例已冲刷并被替换, counterType={}", counterType);
        }
    }

    /**
     * 增加计数（+1）。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @return 增加后的计数数值（本地基线 + 本地增量，可能略落后于 Redis）
     */
    public static long increment(Class<?> entityType, Object counterId) {
        return increment(entityType.getSimpleName(), counterId);
    }

    /**
     * 增加计数（+1）。
     *
     * @param counterType 计数器类型。
     * @param counterId   计数器ID
     * @return 增加后的计数数值
     */
    public static long increment(String counterType, Object counterId) {
        return increment(counterType, counterId, 1);
    }

    /**
     * 增加计数（+N）。
     *
     * @param entityType   entity类型(主要用于构造counterType)。
     * @param counterId    计数器ID
     * @param incrementNum 增加的计数。
     * @return 增加后的计数数值
     */
    public static long increment(Class<?> entityType, Object counterId, long incrementNum) {
        return increment(entityType.getSimpleName(), counterId, incrementNum);
    }

    /**
     * 增加计数（+N）。
     *
     * @param counterType  计数器类型。
     * @param counterId    计数器ID
     * @param incrementNum 增加的计数。
     * @return 增加后的计数数值
     */
    public static long increment(String counterType, Object counterId, long incrementNum) {
        return getWrapper(counterType).getLocalCounter(counterId).increment(incrementNum);
    }

    /**
     * 减少计数（-1）。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @return 减少后的计数数值
     */
    public static long decrement(Class<?> entityType, Object counterId) {
        return decrement(entityType.getSimpleName(), counterId);
    }

    /**
     * 减少计数（-1）。
     *
     * @param counterType 计数器类型。
     * @param counterId   计数器ID
     * @return 减少后的计数数值
     */
    public static long decrement(String counterType, Object counterId) {
        return decrement(counterType, counterId, 1);
    }

    /**
     * 减少计数（-N）。
     *
     * @param entityType   entity类型(主要用于构造counterType)。
     * @param counterId    计数器ID
     * @param incrementNum 减少的计数。
     * @return 减少后的计数数值
     */
    public static long decrement(Class<?> entityType, Object counterId, long incrementNum) {
        return decrement(entityType.getSimpleName(), counterId, incrementNum);
    }

    /**
     * 减少计数（-N）。
     *
     * @param counterType  计数器类型。
     * @param counterId    计数器ID
     * @param incrementNum 减少的计数。
     * @return 减少后的计数数值
     */
    public static long decrement(String counterType, Object counterId, long incrementNum) {
        return getWrapper(counterType).getLocalCounter(counterId).decrement(incrementNum);
    }


    /**
     * 设置计数器初始值。
     * <p>
     * 若 Redis 中无值则写入 initNum，已存在则不覆盖；返回本地计数器实例。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @param initNum    初始值。
     * @return 本地计数器实例
     */
    public static LocalCounter init(Class<?> entityType, Object counterId, long initNum) {
        return init(entityType.getSimpleName(), counterId, initNum);
    }

    /**
     * 设置计数器初始值。
     * <p>
     * 若 Redis 中无值则写入 initNum，已存在则不覆盖；返回本地计数器实例。
     *
     * @param counterType 计数器类型。
     * @param counterId   计数器ID
     * @param initNum     初始值。
     * @return 本地计数器实例
     */
    public static LocalCounter init(String counterType, Object counterId, long initNum) {
        return getWrapper(counterType).getLocalCounter(counterId, initNum);
    }

    /**
     * 获取计数器数值。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @return 计数数值。
     */
    public static long get(Class<?> entityType, Object counterId) {
        return get(entityType.getSimpleName(), counterId);
    }

    /**
     * 获取计数器数值。
     *
     * @param counterType 计数器类型。
     * @param counterId   计数器ID
     * @return 计数数值。
     */
    public static long get(String counterType, Object counterId) {
        return get(counterType, counterId, false);
    }

    /**
     * 获取计数器数值。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @param forceSync  强制同步。
     * @return 计数数值。
     */
    public static long get(Class<?> entityType, Object counterId, boolean forceSync) {
        return get(entityType.getSimpleName(), counterId, forceSync);
    }

    /**
     * 获取计数器数值。
     *
     * @param counterType 计数器类型。
     * @param counterId   计数器ID
     * @param forceSync   强制同步。
     * @return 计数数值。
     */
    public static long get(String counterType, Object counterId, boolean forceSync) {
        LocalCounter localCounter = getWrapper(counterType).getLocalCounter(counterId);
        if (forceSync) {
            localCounter.sync(true);
        }
        return localCounter.get();
    }

    /**
     * 删除计数器。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @return 是否成功
     */
    public static boolean delete(Class<?> entityType, Object counterId) {
        return delete(entityType.getSimpleName(), counterId);
    }

    /**
     * 删除计数器。
     *
     * @param counterType 计数器类型。
     * @param counterId   计数器ID
     * @return 是否成功
     */
    public static boolean delete(String counterType, Object counterId) {
        CounterWrapper wrapper = cacheWrapperMap.get(counterType);
        if (wrapper != null) {
            LocalCounter localCounter = wrapper.localCounterCache.asMap().remove(counterId);
            if (localCounter != null) {
                localCounter.sync(true);
            }
        }
        return GlobalCounter.delete(counterType, counterId);
    }


    /**
     * 获取数值后删除计数器。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @return 计数数值
     */
    public static long getAndDelete(Class<?> entityType, Object counterId) {
        return getAndDelete(entityType.getSimpleName(), counterId);
    }

    /**
     * 获取数值后删除计数器。
     *
     * @param counterType 计数器类型。
     * @param counterId   计数器ID
     * @return 计数数值
     */
    public static long getAndDelete(String counterType, Object counterId) {
        CounterWrapper wrapper = cacheWrapperMap.get(counterType);
        if (wrapper != null) {
            LocalCounter localCounter = wrapper.localCounterCache.asMap().remove(counterId);
            if (localCounter != null) {
                localCounter.sync(true);
            }
        }
        return GlobalCounter.getAndDelete(counterType, counterId);
    }

    /**
     * 获取或创建类型包装（未注册的类型按默认 60s 同步间隔自动创建，保持免注册即用的行为）。
     */
    private static CounterWrapper getWrapper(String counterType) {
        return cacheWrapperMap.computeIfAbsent(counterType, type -> new CounterWrapper(new LocalCounter.Config(type, DEFAULT_SYNC_GLOBAL_MILLIS)));
    }

    /**
     * 本地计数器。
     * <p>
     * 维护一个 counterType:counterId 维度的本地累加器，由 {@link FusionCounter} 内部管理生命周期。
     * 每个实例持有：配置、ID、AtomicLong 本地增量、上次同步基线与时间（volatile 保证可见性）。
     */
    public static class LocalCounter {

        /**
         * 计数器配置。
         */
        private final Config config;

        /**
         * 计数器ID。
         */
        private final Object counterId;
        /**
         * 本地递增计数器，记录自上次同步以来的增量。
         */
        private final AtomicLong counter = new AtomicLong();
        /**
         * 上次同步时间。
         * <p>
         * 使用 volatile 保证多线程可见性：sync() 由 increment/decrement 的多个调用线程并发触发，
         * lastSyncTime 的写入需要对其他读线程立即可见，避免因缓存行不可见导致重复同步或漏同步。
         */
        private volatile long lastSyncTime = 0;
        /**
         * 上次同步数值。
         * <p>
         * 使用 volatile 保证可见性：get() 读取 lastSyncNum + counter.get()，
         * sync() 更新 lastSyncNum 时需对其他线程的 get() 立即可见，否则会读到陈旧的同步基线。
         */
        private volatile long lastSyncNum = 0;
        /**
         * 上次回写数据库时间。
         * <p>
         * 独立于 {@code lastSyncTime} 跟踪：每次增量同步（间隔 syncGlobalMillis）都会刷新 lastSyncTime，
         * 回写条件若复用它，在 syncGlobalMillis &lt; writeBackMillis 的活跃计数器上将永远不成立
         * （回写退化为仅"两次增量间隔超过 writeBackMillis"时触发）。
         * 使用 volatile 保证多线程可见性。
         */
        private volatile long lastWriteBackTime = 0;

        /**
         * 构造本地计数器并完成初始化（设置 Redis 初始值或读取现有值）。
         *
         * @param config   计数器配置
         * @param counterId 计数器ID
         * @param initNum   初始值（!=0 且 Redis 无值时写入）
         */
        public LocalCounter(Config config, Object counterId, long initNum) {
            this.config = config;
            this.counterId = counterId;
            init(initNum);
        }

        /**
         * 获取当前计数（同步基线 + 本地增量）。
         *
         * @return 当前计数。
         */
        public long get() {
            return lastSyncNum + counter.get();
        }

        /**
         * 递增计数，并尝试同步到 Redis。
         *
         * @param incrementNum 递增数值
         * @return 递增后的计数数值
         */
        public long increment(long incrementNum) {
            counter.addAndGet(incrementNum);
            sync(false);
            //返回 get() 而非"addAndGet快照+sync后基线":并发下另一线程可能在addAndGet与sync之间
            //完成同步(含本次增量推入Redis,基线已更新、本地已清零),旧写法会把本次增量双算进返回值
            return get();
        }

        /**
         * 递减计数，并尝试同步到 Redis。
         *
         * @param incrementNum 递减数值
         * @return 递减后的计数数值
         */
        public long decrement(long incrementNum) {
            counter.addAndGet(-incrementNum);
            sync(false);
            return get();
        }

        /**
         * 检查并按需同步本地增量到 Redis。
         * <p>
         * forceSync 为 true 或距上次同步超过 syncGlobalMillis 时，原子取出本地增量并同步到 Redis，
         * 同时按 writeBackMillis 间隔异步触发回写函数（独立跟踪回写时间）。
         * Redis 同步失败（连接异常或 INCRBY 返回无效值）时回滚本地增量待下轮重试、不更新同步基线，
         * 避免增量静默丢失或哨兵值(-1)污染本地计数与回写。
         * forceSync 且本地无增量时补拉全局现值刷新基线（把其他实例已同步的增量反映进强一致读）。
         *
         * @param forceSync 是否强制同步（忽略间隔）
         */
        public void sync(boolean forceSync) {
            long now = SystemClock.now();
            if (forceSync || now > lastSyncTime + config.syncGlobalMillis) {
                //设置同步时间(进入即刷新:无增量时避免后续调用重复取增量,同步失败时下轮间隔后再重试)
                lastSyncTime = now;
                //此处代码用于控制线程并发(getAndSet原子取出,并发线程只有一个能拿到非0增量)
                long num = counter.getAndSet(0);
                if (num != 0) {
                    long globalNum;
                    try {
                        globalNum = pushToRedis(num);
                        if (globalNum < 0) {
                            //INCRBY失败(null返回-1哨兵;计数器值域非负):按同步失败处理
                            throw new IllegalStateException("GlobalCounter.increment failed, counterType=" + config.counterType + ", counterId=" + counterId);
                        }
                    } catch (Exception e) {
                        //Redis连接异常等:增量已从本地原子取出,须回滚待下轮重试(否则永久丢失)
                        counter.addAndGet(num);
                        log.error("FusionCounter同步Redis失败,增量已回滚待下轮重试, counterType={}, counterId={}, num={}",
                                config.counterType, counterId, num, e);
                        return;
                    }
                    lastSyncNum = globalNum;
                    //异步执行回写函数(独立跟踪回写时间——不可复用lastSyncTime,活跃计数器每次增量同步
                    //都会刷新它,回写条件将永远不成立);值取局部快照,防止异步执行时读到更新值。
                    //投递独立于推送try:投递异常不得回滚已成功推送的增量(否则下轮重推多记),仅留痕待下轮自愈
                    if (config.writeBackConsumer != null && now > lastWriteBackTime + config.writeBackMillis) {
                        long writeBackNum = lastSyncNum;
                        try {
                            writeBackExecutor.submit(() -> config.writeBackConsumer.accept(counterId, writeBackNum));
                            lastWriteBackTime = now;
                        } catch (Exception e) {
                            log.warn("FusionCounter回写任务投递失败,快照滞后待下轮自愈, counterType={}, counterId={}, num={}",
                                    config.counterType, counterId, writeBackNum, e);
                        }
                    }
                } else if (forceSync) {
                    //强一致读兜底:本地无增量时原实现直接跳过,读到的仍是陈旧基线(其他实例已同步的增量
                    //不可见);补拉全局现值刷新基线。拉取失败不上抛,退回本地基线(略旧)保证读路径韧性
                    try {
                        lastSyncNum = GlobalCounter.get(config.counterType, counterId);
                    } catch (Exception e) {
                        log.error("FusionCounter强一致读拉取全局值失败,退回本地基线, counterType={}, counterId={}",
                                config.counterType, counterId, e);
                    }
                }
            }
        }

        /**
         * 增量推送 Redis：按配置三级分发——keyExpireAtResolver（周期窗口计数，INCREX PXAT 固定回收时刻，
         * 优先级最高）&gt; keyExpireMillis（滑动 TTL，INCREX PX）&gt; 裸 INCRBY（常驻）。
         *
         * @param num 增量数值
         * @return 推送后的全局现值，失败返回 -1（回滚与否由调用方决定：sync 回滚重试，flush 仅留痕）
         */
        private long pushToRedis(long num) {
            if (config.keyExpireAtResolver != null) {
                Long expireAt = config.keyExpireAtResolver.apply(counterId);
                if (expireAt != null && expireAt > 0) {
                    return GlobalCounter.incrementExpireAt(config.counterType, counterId, num, expireAt);
                }
            }
            if (config.keyExpireMillis > 0) {
                return GlobalCounter.incrementTtl(config.counterType, counterId, num, config.keyExpireMillis);
            }
            return GlobalCounter.increment(config.counterType, counterId, num);
        }

        /**
         * 逐出前的增量冲刷（localCounterCache 容量逐出时由 evictionListener 同步回调）：
         * 把剩余本地增量推 Redis 后，实例即可安全丢弃。
         * <p>
         * 与 {@link #sync(boolean)} 的差异：失败不回滚（实例即将被逐出，无人重试），仅 ERROR 留痕——
         * 与进程崩溃丢增量同语义（方向为少记）；并发 addAndGet 与本次 getAndSet 的竞态残余同理
         * （被逐出的应为冷实例，概率极低）。
         */
        void flush() {
            long num = counter.getAndSet(0);
            if (num != 0) {
                try {
                    long globalNum = pushToRedis(num);
                    //推Redis成功后异步补一次终态回写:实例逐出后不再有定时回写链路,
                    //若最后一轮同步未到60s回写阈值,DB快照将永久滞后最后一段(仅投递任务,
                    //逐出回调线程不做DB IO,不阻塞Caffeine maintenance与读热路径)
                    if (globalNum >= 0 && config.writeBackConsumer != null) {
                        writeBackExecutor.submit(() -> config.writeBackConsumer.accept(counterId, globalNum));
                    }
                } catch (Exception e) {
                    log.error("FusionCounter本地计数器逐出冲刷增量失败,增量丢失, counterType={}, counterId={}, num={}",
                            config.counterType, counterId, num, e);
                }
            }
        }

        /**
         * 初始化操作：设置 Redis 初始值或读取现有值作为本地同步基线。
         *
         * @param initNum 初始值（!=0 且 Redis 无值时写入）
         */
        private void init(long initNum) {
            if (initNum != 0 && GlobalCounter.setIfAbsent(config.counterType, counterId, initNum)) {
                lastSyncNum = initNum;
            } else {
                lastSyncNum = GlobalCounter.get(config.counterType, counterId);
            }
            lastSyncTime = SystemClock.now();
        }

        /**
         * 本地计数器配置。
         * <p>
         * 不可变配置对象，包含计数器类型、同步间隔、回写间隔与回写函数。
         */
        public static class Config {
            /**
             * 计数器类型。
             */
            private final String counterType;

            /**
             * 同步redis时间间隔。
             */
            private final long syncGlobalMillis;

            /**
             * 回写数据库时间间隔。
             */
            private final long writeBackMillis;

            /**
             * Redis key 过期毫秒数（滑动续期；0=常驻不过期）。
             * <p>
             * 大于 0 时增量同步走 {@link GlobalCounter#incrementTtl}（INCREX PX，Redis Open Source 8.8+）。
             * 周期性窗口计数请改用 {@link #keyExpireAtResolver}（PXAT 固定回收时刻）——
             * 滑动 TTL 若小于周期内静默窗，恢复访问时计数已被回收清零。
             */
            private final long keyExpireMillis;

            /**
             * 由 counterId 解析 key 绝对回收时刻（Unix 毫秒）的函数；null=不启用。
             * <p>
             * 非 null 时增量同步走 {@link GlobalCounter#incrementExpireAt}（INCREX PXAT）：
             * 回收时点固定且幂等（每次重设同一时刻），周期窗口计数的正确姿势。
             * 优先级高于 {@link #keyExpireMillis}；返回 null 或 &lt;=0 时本次降级按常驻处理。
             */
            private final Function<Object, Long> keyExpireAtResolver;

            /**
             * 回写库表的方法。
             */
            private final BiConsumer<Object, Long> writeBackConsumer;

            /**
             * 构造基本配置（无回写，key 常驻不过期）。
             *
             * @param counterType     计数器类型
             * @param syncGlobalMillis 同步 Redis 间隔毫秒数
             */
            public Config(String counterType, long syncGlobalMillis) {
                this(counterType, syncGlobalMillis, 0, null, 0);
            }

            /**
             * 构造带回写的配置（key 常驻不过期）。
             *
             * @param counterType       计数器类型
             * @param syncGlobalMillis  同步 Redis 间隔毫秒数
             * @param writeBackMillis   回写数据库间隔毫秒数
             * @param writeBackConsumer 回写函数
             */
            public Config(String counterType, long syncGlobalMillis, long writeBackMillis, BiConsumer<Object, Long> writeBackConsumer) {
                this(counterType, syncGlobalMillis, writeBackMillis, writeBackConsumer, 0);
            }

            /**
             * 构造带回写与 key 过期的配置。
             *
             * @param counterType       计数器类型
             * @param syncGlobalMillis  同步 Redis 间隔毫秒数
             * @param writeBackMillis   回写数据库间隔毫秒数
             * @param writeBackConsumer 回写函数
             * @param keyExpireMillis   Redis key 过期毫秒数（滑动续期，0=常驻；大于 0 需 Redis Open Source 8.8+）
             */
            public Config(String counterType, long syncGlobalMillis, long writeBackMillis, BiConsumer<Object, Long> writeBackConsumer, long keyExpireMillis) {
                this(counterType, syncGlobalMillis, writeBackMillis, writeBackConsumer, keyExpireMillis, null);
            }

            /**
             * 构造完整配置（滑动 TTL 与绝对回收时刻解析函数二选一，resolver 优先）。
             *
             * @param counterType         计数器类型
             * @param syncGlobalMillis    同步 Redis 间隔毫秒数
             * @param writeBackMillis     回写数据库间隔毫秒数
             * @param writeBackConsumer   回写函数
             * @param keyExpireMillis     Redis key 过期毫秒数（滑动续期，0=常驻）
             * @param keyExpireAtResolver 由 counterId 解析绝对回收时刻（Unix 毫秒），null=不启用
             */
            public Config(String counterType, long syncGlobalMillis, long writeBackMillis, BiConsumer<Object, Long> writeBackConsumer,
                          long keyExpireMillis, Function<Object, Long> keyExpireAtResolver) {
                this.counterType = counterType;
                this.syncGlobalMillis = syncGlobalMillis;
                this.writeBackMillis = writeBackMillis;
                this.writeBackConsumer = writeBackConsumer;
                this.keyExpireMillis = keyExpireMillis;
                this.keyExpireAtResolver = keyExpireAtResolver;
            }

            /**
             * 获取计数器类型。
             *
             * @return 计数器类型
             */
            public String getCounterType() {
                return counterType;
            }

            /**
             * 获取同步 Redis 间隔毫秒数。
             *
             * @return 同步间隔毫秒数
             */
            public long getSyncGlobalMillis() {
                return syncGlobalMillis;
            }

            /**
             * 获取回写数据库间隔毫秒数。
             *
             * @return 回写间隔毫秒数
             */
            public long getWriteBackMillis() {
                return writeBackMillis;
            }

            /**
             * 获取回写函数。
             *
             * @return 回写函数，无回写时为 null
             */
            public BiConsumer<Object, Long> getWriteBackConsumer() {
                return writeBackConsumer;
            }

            /**
             * 获取 Redis key 过期毫秒数。
             *
             * @return 过期毫秒数（滑动续期），0 表示常驻不过期
             */
            public long getKeyExpireMillis() {
                return keyExpireMillis;
            }

            /**
             * 获取 key 绝对回收时刻解析函数。
             *
             * @return 解析函数，null 表示不启用（走滑动 TTL 或常驻）
             */
            public Function<Object, Long> getKeyExpireAtResolver() {
                return keyExpireAtResolver;
            }
        }

    }

}
