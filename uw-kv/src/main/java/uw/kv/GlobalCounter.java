package uw.kv;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import uw.kv.util.RedisKeyUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 实现的全局计数器。
 * <p>
 * 直接操作 Redis 的数值键，保证集群内强一致。value 序列化采用 Long 字符串（longCacheRedisTemplate）。
 * Redis key 格式：{@code uw-counter:{counterType}:{counterId}}。
 */
public class GlobalCounter {

    private static final Logger log = LoggerFactory.getLogger(GlobalCounter.class);

    /**
     * redis前缀。
     */
    private static final String REDIS_PREFIX = "uw-counter:";

    /**
     * redis操作模板。
     */
    private static RedisTemplate<String, Long> longCacheRedisTemplate;


    /**
     * 构造方法，由 Spring 注入 Long RedisTemplate 到 static 字段。
     *
     * @param longCacheRedisTemplate Long 值 RedisTemplate
     */
    /**
     * INCREX 能力标记：<b>启动时探测一次</b>（构造器直接试命令）。运行期无冷却/重探——Redis 版本在
     * 应用运行期不会变化，升级伴随滚动重启；探测漏网时运行期遇 unknown command 仅一次性翻转为 false，
     * 此后固定降级裸 INCRBY（计数保全、TTL 回收失效改为 key 堆积）。其他 Redis 故障不触发降级。
     */
    private static volatile boolean increxSupported;

    /**
     * INCREX 能力探测 key（与限速器共用，短 TTL 自动回收）。
     */
    private static final String INCREX_PROBE_KEY = "uw-kv:increx-probe";

    public GlobalCounter(RedisTemplate<String, Long> longCacheRedisTemplate) {
        GlobalCounter.longCacheRedisTemplate = longCacheRedisTemplate;
        // 启动探测一次：直接试命令（与限速器同款）；连接异常乐观默认支持，运行期 failover 兜底
        try {
            GlobalFixedWindowRateLimiter.increx(longCacheRedisTemplate, INCREX_PROBE_KEY, 1, GlobalFixedWindowRateLimiter.PX_BYTES, "1000");
            increxSupported = true;
        } catch (Exception e) {
            increxSupported = !isUnknownCommandError(e);
        }
    }

    /**
     * 增加计数。
     *
     * @param entityType   entity类型(主要用于构造counterType)。
     * @param counterId    计数器ID
     * @param incrementNum 增加的计数。
     * @return 计数数值，Redis 异常返回 -1
     */
    public static long increment(Class<?> entityType, Object counterId, long incrementNum) {
        return increment(entityType.getSimpleName(), counterId, incrementNum);
    }

    /**
     * 增加计数。
     *
     * @param counterType  计数器类型
     * @param counterId    计数器ID
     * @param incrementNum 增加的计数。
     * @return 计数数值，Redis 异常返回 -1
     */
    public static long increment(String counterType, Object counterId, long incrementNum) {
        Long count = longCacheRedisTemplate.opsForValue().increment(RedisKeyUtils.buildTypeId(REDIS_PREFIX, counterType, counterId), incrementNum);
        return count != null ? count : -1;
    }

    /**
     * 增加计数并设置 key 过期时间（Redis Open Source 8.8+ 的 INCREX 命令：单命令原子完成 INCRBY+PEXPIRE）。
     * <p>
     * 每次执行都把 TTL 重设为 {@code keyExpireMillis}（滑动续期）：key 自最后一次活动起 TTL 后被 Redis 自动回收。
     * 适用于"key 存活跟随活动"的场景；周期性窗口计数（key 过期=计数作废，回收时点须固定在周期终点后）
     * 请用 {@link #incrementExpireAt}（PXAT 固定时间戳），避免周期内静默超过 TTL 导致计数提前回收清零。
     * 常驻计数器（依赖 setIfAbsent 基线恢复语义）请用 {@link #increment(String, Object, long)}，不要配 TTL。
     * INCREX 返回二元素数组 [新值, 实际应用增量]，本方法取第一个元素（与 increment 返回值口径一致）。
     * <p>
     * 注意：Spring Data Redis 未封装 INCREX，此处经 Lettuce 原生 dispatch 发送（自定义命令字与返回解析），
     * 不依赖 Spring 跟进版本；目标 Redis 低于 8.8 时降级为裸 INCRBY（计数功能保全、key 无 TTL 堆积——
     * 启动时探测一次定路径，见类头说明与 {@link #increxWithFallback}）。
     *
     * @param counterType     counterType
     * @param counterId       计数器ID
     * @param incrementNum    增加的计数
     * @param keyExpireMillis key 过期毫秒数（须大于 0）
     * @return 计数数值，Redis 异常或返回解析失败返回 -1
     */
    public static long incrementTtl(String counterType, Object counterId, long incrementNum, long keyExpireMillis) {
        return increxWithFallback(counterType, counterId, incrementNum, GlobalFixedWindowRateLimiter.PX_BYTES, Long.toString(keyExpireMillis));
    }

    /**
     * 增加计数并设置 key 的绝对过期时间戳（INCREX PXAT：增量+固定回收时刻单命令原子）。
     * <p>
     * 每次执行都把过期时间重设为同一 {@code expireAtMillis}（幂等，无漂移）：key 在固定时刻被 Redis 回收，
     * 与周期内是否持续活动无关——周期性窗口计数的正确姿势：counterId 编码周期起点，由业务按周期终点
     * （+buffer）计算回收时刻，周期内静默多久都不会提前回收；周期翻转换 key 后旧 key 在终点时刻确定性回收。
     * 过期时间戳若已过去，Redis 在应用增量后立即删除 key（在途残留增量随之丢弃，方向少记，量级为同步间隔内）。
     *
     * @param counterType    counterType
     * @param counterId      计数器ID
     * @param incrementNum   增加的计数
     * @param expireAtMillis 过期的 Unix 绝对时间戳（毫秒，须大于 0）
     * @return 计数数值，Redis 异常或返回解析失败返回 -1
     */
    public static long incrementExpireAt(String counterType, Object counterId, long incrementNum, long expireAtMillis) {
        return increxWithFallback(counterType, counterId, incrementNum, GlobalFixedWindowRateLimiter.PXAT_BYTES, Long.toString(expireAtMillis));
    }

    /**
     * 带能力降级的 INCREX 入口：不支持（启动探测结论，或运行期漏网兜底）时 WARN 并降级裸 INCRBY，
     * 其余异常原样上抛。
     * <p>
     * 不降级的后果更糟：调用方按同步失败回滚增量重试，5s 一轮无限循环——TTL 计数器静默失效
     * （计数不增长、限额判定失效）且 ERROR 日志按同步节奏刷屏。
     */
    private static long increxWithFallback(String counterType, Object counterId, long incrementNum, byte[] expireOption, String expireValue) {
        if (!increxSupported) {
            return increment(counterType, counterId, incrementNum);
        }
        try {
            String key = RedisKeyUtils.buildTypeId(REDIS_PREFIX, counterType, counterId);
            return GlobalFixedWindowRateLimiter.increx(longCacheRedisTemplate, key, incrementNum, expireOption, expireValue);
        } catch (Exception e) {
            if (isUnknownCommandError(e)) {
                //启动探测漏网(如启动时Redis不可达的乐观默认):一次性翻转,不重探
                increxSupported = false;
                log.warn("Redis不支持INCREX(版本<8.8),TTL计数已降级为INCRBY:计数功能正常但key无TTL将堆积,重启前不再尝试INCREX, counterType={}", counterType);
                return increment(counterType, counterId, incrementNum);
            }
            throw e;
        }
    }

    /**
     * 判断异常链中是否为"unknown command"类服务端错误（INCREX 不存在的特征），委派 {@link GlobalFixedWindowRateLimiter#isUnknownCommandError}。
     * 与连接类故障（RedisConnectionFailureException 等）区分——后者不触发降级。
     */
    static boolean isUnknownCommandError(Throwable e) {
        return GlobalFixedWindowRateLimiter.isUnknownCommandError(e);
    }

    /**
     * 测试辅助：强制走降级路径（验证降级；生产代码勿用）。
     */
    static void markIncrexUnavailableForTest() {
        increxSupported = false;
    }

    /**
     * 测试辅助：重置 INCREX 降级状态（生产代码勿用）。
     */
    static void resetIncrexUnavailableForTest() {
        increxSupported = true;
    }

    /**
     * 减少计数。
     *
     * @param entityType   entity类型(主要用于构造counterType)。
     * @param counterId    计数器ID
     * @param decrementNum 减少的计数。
     * @return 计数数值，Redis 异常返回 -1
     */
    public static long decrement(Class<?> entityType, Object counterId, long decrementNum) {
        return decrement(entityType.getSimpleName(), counterId, decrementNum);
    }

    /**
     * 减少计数。
     *
     * @param counterType  计数器类型
     * @param counterId    计数器ID
     * @param decrementNum 减少的计数。
     * @return 计数数值，Redis 异常返回 -1
     */
    public static long decrement(String counterType, Object counterId, long decrementNum) {
        Long count = longCacheRedisTemplate.opsForValue().decrement(RedisKeyUtils.buildTypeId(REDIS_PREFIX, counterType, counterId), decrementNum);
        return count != null ? count : -1;
    }

    /**
     * 设置计数器数值。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @param num        数值
     */
    public static void set(Class<?> entityType, Object counterId, long num) {
        set(entityType.getSimpleName(), counterId, num);
    }

    /**
     * 设置计数器数值。
     *
     * @param counterType 计数器类型
     * @param counterId   计数器ID
     * @param num         数值
     */
    public static void set(String counterType, Object counterId, long num) {
        longCacheRedisTemplate.opsForValue().set(RedisKeyUtils.buildTypeId(REDIS_PREFIX, counterType, counterId), num);
    }

    /**
     * 如果没有，则设置计数器数值。
     * 一般用于设置初始数值。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @param num        数值
     * @return true 表示设置成功（原先不存在），false 表示已存在未覆盖
     */
    public static boolean setIfAbsent(Class<?> entityType, Object counterId, long num) {
        return setIfAbsent(entityType.getSimpleName(), counterId, num);
    }

    /**
     * 如果没有，则设置计数器数值。
     * 一般用于设置初始数值。
     *
     * @param counterType 计数器类型
     * @param counterId   计数器ID
     * @param num         数值
     * @return true 表示设置成功（原先不存在），false 表示已存在未覆盖
     */
    public static boolean setIfAbsent(String counterType, Object counterId, long num) {
        return Boolean.TRUE.equals(longCacheRedisTemplate.opsForValue().setIfAbsent(RedisKeyUtils.buildTypeId(REDIS_PREFIX, counterType, counterId), num));
    }

    /**
     * 获取计数器数值。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @return 计数数值，不存在返回 0
     */
    public static long get(Class<?> entityType, Object counterId) {
        return get(entityType.getSimpleName(), counterId);
    }

    /**
     * 获取计数器数值。
     *
     * @param counterType 计数器类型
     * @param counterId   计数器ID
     * @return 计数数值，不存在返回 0
     */
    public static long get(String counterType, Object counterId) {
        Long count = longCacheRedisTemplate.opsForValue().get(RedisKeyUtils.buildTypeId(REDIS_PREFIX, counterType, counterId));
        return count != null ? count : 0;
    }

    /**
     * 获取计数器 key 的剩余存活毫秒数（PTTL）。
     * <p>
     * 适用于配额/限流类计数（{@code incrementTtl}/{@code incrementExpireAt} 写入的带 TTL 计数）的
     * "剩余时长提示"场景（如超限后告知调用方还需等待多久）。常驻计数器（无 TTL）返回 -1。
     *
     * @param counterType 计数器类型
     * @param counterId   计数器ID
     * @return 剩余毫秒；key 不存在返回 -2；无 TTL 返回 -1；Redis 异常返回 -1
     */
    public static long getRemainMillis(String counterType, Object counterId) {
        Long ttl = longCacheRedisTemplate.getExpire(RedisKeyUtils.buildTypeId(REDIS_PREFIX, counterType, counterId), TimeUnit.MILLISECONDS);
        return ttl != null ? ttl : -1;
    }

    /**
     * 获取计数器 key 的剩余存活毫秒数（PTTL），计数器类型取实体类名。
     *
     * @param entityType entity类型(主要用于构造counterType)
     * @param counterId  计数器ID
     * @return 剩余毫秒；key 不存在返回 -2；无 TTL 返回 -1
     */
    public static long getRemainMillis(Class<?> entityType, Object counterId) {
        return getRemainMillis(entityType.getSimpleName(), counterId);
    }

    /**
     * 删除计数器。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @return true 表示删除成功
     */
    public static boolean delete(Class<?> entityType, Object counterId) {
        return delete(entityType.getSimpleName(), counterId);
    }

    /**
     * 删除计数器。
     *
     * @param counterType 计数器类型
     * @param counterId   计数器ID
     * @return true 表示删除成功
     */
    public static boolean delete(String counterType, Object counterId) {
        return Boolean.TRUE.equals(longCacheRedisTemplate.delete(RedisKeyUtils.buildTypeId(REDIS_PREFIX, counterType, counterId)));
    }

    /**
     * 获取数值后删除计数器。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @return 计数数值，不存在返回 -1
     */
    public static long getAndDelete(Class<?> entityType, Object counterId) {
        return getAndDelete(entityType.getSimpleName(), counterId);
    }

    /**
     * 获取数值后删除计数器。
     *
     * @param counterType 计数器类型
     * @param counterId   计数器ID
     * @return 计数数值，不存在返回 -1
     */
    public static long getAndDelete(String counterType, Object counterId) {
        Long count = longCacheRedisTemplate.opsForValue().getAndDelete(RedisKeyUtils.buildTypeId(REDIS_PREFIX, counterType, counterId));
        return count != null ? count : -1;
    }

    /**
     * 获取缓存中的所有key。
     *
     * @param entityType 缓存对象类(主要用于构造cacheName)
     * @param keyPrefix  key前缀，请注意key最后不用加"*"
     * @return key集合
     */
    public static Set<String> keys(Class<?> entityType, String keyPrefix) {
        return keys(entityType.getSimpleName(), keyPrefix);
    }

    /**
     * 获取缓存中的所有key。
     *
     * @param counterType 缓存名
     * @param keyPrefix   key前缀，请注意key最后不用加"*",全部清除用*即可。
     * @return key集合
     */
    public static Set<String> keys(String counterType, String keyPrefix) {
        if (StringUtils.isBlank(keyPrefix)) {
            keyPrefix = "*";
        } else {
            keyPrefix = keyPrefix + "*";
        }
        int redisPrefixLength = REDIS_PREFIX.length() + counterType.length() + 1;
        String pattern = RedisKeyUtils.buildTypeId(REDIS_PREFIX, counterType, keyPrefix);
        Set<String> keys = new HashSet<>();
        try (Cursor<String> cursor = longCacheRedisTemplate.scan(ScanOptions.scanOptions().match(pattern).count(1000).build())) {
            cursor.forEachRemaining(key -> {
                keys.add(key.substring(redisPrefixLength));
            });
        }
        return keys;
    }
}
