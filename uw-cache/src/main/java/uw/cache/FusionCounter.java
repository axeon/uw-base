package uw.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.cache.util.RedisKeyUtils;
import uw.common.util.SystemClock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/**
 * 基于 AtomicLong 和 Redis 的复合（融合）计数器。
 * <p>
 * 本地通过 AtomicLong 高频累加，按 {@code syncGlobalMillis} 间隔原子化地将增量同步到 Redis（{@link GlobalCounter}），
 * 兼顾写入性能与全局一致性。支持配置回写函数，按 {@code writeBackMillis} 间隔异步回写数据库（虚拟线程执行）。
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
     * key 是 counterType，value 是该类型的 Config 实例。
     */
    private static final ConcurrentMap<String, LocalCounter.Config> configMap = new ConcurrentHashMap<>();

    /**
     * key 是 counterType:counterId，value 是对应的 LocalCounter 实例。
     */
    private static final ConcurrentMap<String, LocalCounter> counterMap = new ConcurrentHashMap<>();

    /**
     * 虚拟线程执行器。
     */
    private static final ExecutorService executorService = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("uw-counter").factory());

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
        configMap.put(counterType, new LocalCounter.Config(counterType, syncGlobalMillis));
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
        configMap.put(counterType, new LocalCounter.Config(counterType, syncGlobalMillis, writeBackMillis, writeBackConsumer));
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
        LocalCounter localCounter = counterMap.computeIfAbsent(counterType + RedisKeyUtils.KEY_SPLITTER + counterId, key -> new LocalCounter(configMap.computeIfAbsent(counterType, config -> new LocalCounter.Config(counterType, DEFAULT_SYNC_GLOBAL_MILLIS)), counterId, 0));
        return localCounter.increment(incrementNum);
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
        LocalCounter localCounter = counterMap.computeIfAbsent(counterType + RedisKeyUtils.KEY_SPLITTER + counterId, key -> new LocalCounter(configMap.computeIfAbsent(counterType, config -> new LocalCounter.Config(counterType, DEFAULT_SYNC_GLOBAL_MILLIS)), counterId, 0));
        return localCounter.decrement(incrementNum);
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
        return counterMap.computeIfAbsent(counterType + RedisKeyUtils.KEY_SPLITTER + counterId, key -> new LocalCounter(configMap.computeIfAbsent(counterType, config -> new LocalCounter.Config(counterType, DEFAULT_SYNC_GLOBAL_MILLIS)), counterId, initNum));
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
        LocalCounter localCounter = counterMap.computeIfAbsent(counterType + RedisKeyUtils.KEY_SPLITTER + counterId, key -> new LocalCounter(configMap.computeIfAbsent(counterType, config -> new LocalCounter.Config(counterType, DEFAULT_SYNC_GLOBAL_MILLIS)), counterId, 0));
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
        LocalCounter localCounter = counterMap.remove(counterType + RedisKeyUtils.KEY_SPLITTER + counterId);
        if (localCounter != null) {
            localCounter.sync(true);
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
        LocalCounter localCounter = counterMap.remove(counterType + RedisKeyUtils.KEY_SPLITTER + counterId);
        if (localCounter != null) {
            localCounter.sync(true);
        }
        return GlobalCounter.getAndDelete(counterType, counterId);
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
            long num = counter.addAndGet(incrementNum);
            sync(false);
            return lastSyncNum + num;
        }

        /**
         * 递减计数，并尝试同步到 Redis。
         *
         * @param incrementNum 递减数值
         * @return 递减后的计数数值
         */
        public long decrement(long incrementNum) {
            long num = counter.addAndGet(-incrementNum);
            sync(false);
            return lastSyncNum + num;
        }

        /**
         * 检查并按需同步本地增量到 Redis。
         * <p>
         * forceSync 为 true 或距上次同步超过 syncGlobalMillis 时，原子取出本地增量并同步到 Redis，
         * 同时按 writeBackMillis 间隔异步触发回写函数。
         *
         * @param forceSync 是否强制同步（忽略间隔）
         */
        public void sync(boolean forceSync) {
            long now = SystemClock.now();
            if (forceSync || now > lastSyncTime + config.syncGlobalMillis) {
                long num = 0;
                //此处代码用于控制线程并发
                if ((num = counter.getAndSet(0)) != 0) {
                    lastSyncNum = GlobalCounter.increment(config.counterType, counterId, num);
                    //异步执行回写函数
                    if (config.writeBackConsumer != null && now > lastSyncTime + config.writeBackMillis) {
                        executorService.submit(() -> config.writeBackConsumer.accept(counterId, lastSyncNum));
                    }
                    //设置同步时间
                    lastSyncTime = now;
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
             * 回写库表的方法。
             */
            private final BiConsumer<Object, Long> writeBackConsumer;

            /**
             * 构造基本配置（无回写）。
             *
             * @param counterType     计数器类型
             * @param syncGlobalMillis 同步 Redis 间隔毫秒数
             */
            public Config(String counterType, long syncGlobalMillis) {
                this.counterType = counterType;
                this.syncGlobalMillis = syncGlobalMillis;
                this.writeBackMillis = 0;
                this.writeBackConsumer = null;
            }

            /**
             * 构造带回写的配置。
             *
             * @param counterType       计数器类型
             * @param syncGlobalMillis  同步 Redis 间隔毫秒数
             * @param writeBackMillis   回写数据库间隔毫秒数
             * @param writeBackConsumer 回写函数
             */
            public Config(String counterType, long syncGlobalMillis, long writeBackMillis, BiConsumer<Object, Long> writeBackConsumer) {
                this.counterType = counterType;
                this.syncGlobalMillis = syncGlobalMillis;
                this.writeBackMillis = writeBackMillis;
                this.writeBackConsumer = writeBackConsumer;
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
        }

    }

}
