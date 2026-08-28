package uw.cache;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.BaseRedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.core.ScanOptions;
import uw.cache.util.RedisKeyUtils;
import uw.common.util.SystemClock;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

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
     * INCREX 命令的整型增量选项字面量（BYINT）。
     */
    private static final byte[] BYINT_BYTES = "BYINT".getBytes(StandardCharsets.UTF_8);

    /**
     * INCREX 命令的毫秒级过期选项字面量（PX）。
     */
    private static final byte[] PX_BYTES = "PX".getBytes(StandardCharsets.UTF_8);

    /**
     * INCREX 命令的绝对过期时间戳选项字面量（PXAT，Unix 毫秒）。
     */
    private static final byte[] PXAT_BYTES = "PXAT".getBytes(StandardCharsets.UTF_8);

    /**
     * INCREX 命令关键字（Spring Data Redis / Lettuce 均未收录 8.8+ 新命令，自定义 ProtocolKeyword 发送）。
     */
    private static final ProtocolKeyword INCREX_KEYWORD = new ProtocolKeyword() {
        @Override
        public byte[] getBytes() {
            return "INCREX".getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String toString() {
            return "INCREX";
        }
    };

    /**
     * redis操作模板。
     */
    private static RedisTemplate<String, Long> longCacheRedisTemplate;


    /**
     * 构造方法，由 Spring 注入 Long RedisTemplate 到 static 字段。
     *
     * @param longCacheRedisTemplate Long 值 RedisTemplate
     */
    public GlobalCounter(RedisTemplate<String, Long> longCacheRedisTemplate) {
        GlobalCounter.longCacheRedisTemplate = longCacheRedisTemplate;
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
     * 不依赖 Spring 跟进版本；目标 Redis 低于 8.8 时自动降级为裸 INCRBY（计数功能保全、key 无 TTL 堆积，
     * 每小时自动重试探测，见 {@link #increxWithFallback} 的 WARN 日志）。
     *
     * @param counterType     counterType
     * @param counterId       计数器ID
     * @param incrementNum    增加的计数
     * @param keyExpireMillis key 过期毫秒数（须大于 0）
     * @return 计数数值，Redis 异常或返回解析失败返回 -1
     */
    public static long incrementTtl(String counterType, Object counterId, long incrementNum, long keyExpireMillis) {
        return increxWithFallback(counterType, counterId, incrementNum, PX_BYTES, Long.toString(keyExpireMillis));
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
        return increxWithFallback(counterType, counterId, incrementNum, PXAT_BYTES, Long.toString(expireAtMillis));
    }

    /**
     * INCREX 能力降级开关：上次探测到服务端不支持 INCREX（版本 &lt; 8.8）的时间戳毫秒，0=可用。
     * <p>
     * 低版本返回 ERR unknown command 时自动降级为裸 INCRBY（计数功能保全、TTL 回收失效改为 key 堆积），
     * 并按 {@link #INCREX_REPROBE_MILLIS} 冷却后自动重试（环境升级后无需重启应用即可恢复 INCREX）。
     * 其他 Redis 故障（连接异常等）不触发降级，照常上抛由调用方回滚重试。
     */
    private static final AtomicLong increxUnavailableSince = new AtomicLong(0);

    /**
     * 降级后的 INCREX 重试探测间隔（1 小时）。
     */
    private static final long INCREX_REPROBE_MILLIS = 3_600_000L;

    /**
     * 带能力降级的 INCREX 入口：服务端不支持（unknown command）时 WARN 并降级裸 INCRBY，其余异常原样上抛。
     * <p>
     * 不降级的后果更糟：调用方按同步失败回滚增量重试，5s 一轮无限循环——TTL 计数器静默失效
     * （计数不增长、限额判定失效）且 ERROR 日志按同步节奏刷屏。
     */
    private static long increxWithFallback(String counterType, Object counterId, long incrementNum, byte[] expireOption, String expireValue) {
        long unavailableSince = increxUnavailableSince.get();
        if (unavailableSince > 0 && SystemClock.now() < unavailableSince + INCREX_REPROBE_MILLIS) {
            //冷却期内:直接降级,不再打 Redis 撞墙
            return increment(counterType, counterId, incrementNum);
        }
        try {
            long result = increx(counterType, counterId, incrementNum, expireOption, expireValue);
            //冷却期后探测成功:CAS 恢复(并发探测只让一个线程打日志)
            if (unavailableSince > 0 && increxUnavailableSince.compareAndSet(unavailableSince, 0)) {
                log.info("INCREX已恢复可用(环境已升级至8.8+),TTL计数恢复: counterType={}", counterType);
            }
            return result;
        } catch (Exception e) {
            if (isUnknownCommandError(e)) {
                increxUnavailableSince.set(SystemClock.now());
                log.warn("Redis不支持INCREX(版本<8.8),TTL计数已自动降级为INCRBY:计数功能正常但key无TTL将堆积,请升级Redis至8.8+(每小时自动重试探测), counterType={}", counterType);
                return increment(counterType, counterId, incrementNum);
            }
            throw e;
        }
    }

    /**
     * 判断异常链中是否为"unknown command"类服务端错误（INCREX 不存在的特征）。
     * 遍历 cause 链匹配 {@link io.lettuce.core.RedisCommandExecutionException} 的 message，
     * 与连接类故障（RedisConnectionFailureException 等）区分——后者不触发降级。
     */
    static boolean isUnknownCommandError(Throwable e) {
        Throwable t = e;
        for (int i = 0; t != null && i < 10; i++) {
            if (t instanceof io.lettuce.core.RedisCommandExecutionException
                    && t.getMessage() != null && t.getMessage().toLowerCase().contains("unknown command")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /**
     * 测试辅助：模拟 INCREX 不可用（验证降级路径；生产代码勿用）。
     */
    static void markIncrexUnavailableForTest() {
        increxUnavailableSince.set(SystemClock.now());
    }

    /**
     * 测试辅助：重置 INCREX 降级状态（生产代码勿用）。
     */
    static void resetIncrexUnavailableForTest() {
        increxUnavailableSince.set(0);
    }

    /**
     * INCREX 公共实现：按过期选项（PX=相对毫秒 / PXAT=绝对毫秒时间戳）发送命令并解析返回值。
     * <p>
     * 走 Lettuce 原生 dispatch + {@link LongArrayOutput}：Spring Data Redis 的
     * {@code connection.execute(命令名, 参数)} 对未收录的新命令用 ByteArrayOutput 解析，
     * 而 INCREX 返回 integer 数组，ByteArrayOutput 不支持 {@code set(long)}，客户端解码直接抛
     * UnsupportedOperationException（服务端命令实际已执行成功）。原生 dispatch 自定义 output 可正确接收。
     * <p>
     * INCREX 执行异常（Redis 故障等）按 increment 同样的方式上抛（调用方回滚增量重试）。
     * 返回形态意外时（增量已入 Redis）补一次标准 GET 取全局现值（GET 返回≥本次增量，方向安全），
     * 而不是返回 -1——调用方会把 -1 当执行失败回滚增量，下轮重推将多记。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static long increx(String counterType, Object counterId, long incrementNum, byte[] expireOption, String expireValue) {
        String key = RedisKeyUtils.buildTypeId(REDIS_PREFIX, counterType, counterId);
        RedisSerializer keySerializer = longCacheRedisTemplate.getKeySerializer();
        byte[] keyBytes = keySerializer != null ? keySerializer.serialize(key) : key.getBytes(StandardCharsets.UTF_8);
        List<Long> result = longCacheRedisTemplate.execute((RedisCallback<List<Long>>) connection -> {
            CommandArgs<byte[], byte[]> args = new CommandArgs<>(ByteArrayCodec.INSTANCE)
                    .addKey(keyBytes)
                    .add(BYINT_BYTES).add(Long.toString(incrementNum).getBytes(StandardCharsets.UTF_8))
                    .add(expireOption).add(expireValue.getBytes(StandardCharsets.UTF_8));
            Object nativeConnection = connection.getNativeConnection();
            try {
                //常规路径:共享异步命令视图(Spring LettuceConnection 的 native 为 RedisAsyncCommandsImpl)
                if (nativeConnection instanceof BaseRedisAsyncCommands<?, ?> asyncCommands) {
                    RedisFuture<List<Long>> future = ((BaseRedisAsyncCommands<byte[], byte[]>) asyncCommands)
                            .dispatch(INCREX_KEYWORD, new LongArrayOutput(), args);
                    return future.get(10, TimeUnit.SECONDS);
                }
                //防御路径:同步视图直接 dispatch
                if (nativeConnection instanceof RedisCommands) {
                    RedisCommands<byte[], byte[]> commands = (RedisCommands<byte[], byte[]>) nativeConnection;
                    return commands.dispatch(INCREX_KEYWORD, new LongArrayOutput(), args);
                }
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new IllegalStateException("INCREX执行失败, counterType=" + counterType + ", counterId=" + counterId, e.getCause() != null ? e.getCause() : e);
            }
            throw new IllegalStateException("INCREX需要Lettuce原生连接,当前: " + nativeConnection.getClass().getName());
        });
        //INCREX 整数模式返回 [新值, 实际增量],取第一个元素
        if (result != null && !result.isEmpty() && result.get(0) != null) {
            return result.get(0);
        }
        log.error("INCREX返回形态解析失败,退回标准GET取全局现值, counterType={}, counterId={}, result={}", counterType, counterId, result);
        return get(counterType, counterId);
    }

    /**
     * INCREX 返回的 integer 数组解析器（[新值, 实际应用增量]）。
     * <p>
     * Lettuce 内置输出无 integer 数组形态（ByteArrayOutput 不支持 set(long)），自定义接收；
     * 同时覆写 set(ByteBuffer) 兼容 bulk 字符串形态的数字元素（防御协议实现差异）。
     */
    private static final class LongArrayOutput extends CommandOutput<byte[], byte[], List<Long>> {

        LongArrayOutput() {
            super(ByteArrayCodec.INSTANCE, new ArrayList<>(2));
        }

        @Override
        public void set(long integer) {
            output.add(integer);
        }

        @Override
        public void set(ByteBuffer bytes) {
            output.add(bytes == null ? 0L : Long.parseLong(decodeString(bytes)));
        }
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
