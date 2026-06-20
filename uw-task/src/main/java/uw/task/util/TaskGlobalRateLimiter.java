package uw.task.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Collections;

/**
 * 基于 Redis 的全局固定窗口限流器。
 *
 * <p>在多个实例之间共享同一限流配额，采用固定窗口计数算法：以 {@code seconds} 秒为一个窗口，
 * 窗口内累计请求数不得超过 {@code requests}。窗口内首次请求时通过 {@code INCRBY} 创建计数并设置
 * TTL（等于窗口长度），到期后 key 自动失效，下一个窗口从 0 重新计数。</p>
 *
 * <h3>算法取舍</h3>
 * 固定窗口在窗口边界处存在最多 2 倍的突发流量（上一窗口末尾与本窗口起始叠加）。本类用于任务执行流控，
 * 任务本身离散且有执行耗时，对该突发不敏感；同时固定窗口每次检测仅需 1~2 条 O(1) Redis 命令、
 * 单对象内存约 16 字节，在"限速对象多、检测频繁"的生产场景下，CPU 与内存开销均最优。
 * 若需平滑匀速，可使用 {@link TaskLocalRateLimiter}（进程内令牌桶，无 Redis 开销）。</p>
 *
 * <h3>固定窗口 Lua 脚本</h3>
 * <pre>
 * -- KEYS[1] = 限流 key
 * -- ARGV[1] = requests  窗口内配额上限
 * -- ARGV[2] = seconds   窗口长度（秒）
 * -- ARGV[3] = permits   本次申请数量
 * -- 返回：0 表示通过，&gt;0 表示已超限、需等待的毫秒数（窗口剩余时间）
 * </pre>
 *
 * <h3>返回值语义</h3>
 * <ul>
 *   <li>{@code 0}：窗口内未超限，已计入本次申请，请求通过。</li>
 *   <li>{@code >0}：已超限，返回当前窗口的剩余毫秒数，调用方可据此退避后重试。</li>
 * </ul>
 *
 * <h3>异常处理</h3>
 * Redis 异常时返回 0（放行），优先保证任务可用性，等同限流降级。
 *
 * @author axeon
 */
public class TaskGlobalRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(TaskGlobalRateLimiter.class);

    /**
     * Redis 计数 key 前缀。
     */
    private static final String REDIS_PREFIX = "uw-task-rate:";

    /**
     * 固定窗口计数 Lua 脚本。
     * <p>原子地完成"累加本次申请数 → 首次设置窗口 TTL → 判断是否超限"。
     * 超限时返回窗口剩余毫秒（{@code PTTL}），未超限返回 0。</p>
     */
    private static final DefaultRedisScript<Long> FIXED_WINDOW_SCRIPT = new DefaultRedisScript<>(
            "local key = KEYS[1] "
                    + "local permits = tonumber(ARGV[3]) "
                    + "local windowMillis = tonumber(ARGV[2]) * 1000 "
                    // 累加本次申请数；Redis 对已过期的 key 会先惰性删除，故首请求时结果恰为 permits
                    + "local nowRate = redis.call('INCRBY', key, permits) "
                    + "if nowRate == permits then "
                    // 窗口内首次请求，设置窗口 TTL
                    + "  redis.call('PEXPIRE', key, windowMillis) "
                    + "end "
                    + "if nowRate > tonumber(ARGV[1]) then "
                    // 超限：返回当前窗口剩余毫秒，供调用方退避
                    + "  local waitMillis = redis.call('PTTL', key) "
                    // PTTL 返回 -1（无 TTL，异常态）或 -2（key 不存在）时，退化为一个完整窗口
                    + "  if waitMillis < 0 then waitMillis = windowMillis end "
                    + "  return waitMillis "
                    + "end "
                    + "return 0",
            Long.class);

    private final RedisTemplate<String, Long> redisTemplate;

    /**
     * 构造全局限流器。
     *
     * @param redisConnectionFactory Redis 连接工厂
     */
    public TaskGlobalRateLimiter(final RedisConnectionFactory redisConnectionFactory) {
        redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        // 本模板仅用于执行脚本，key 走 String 序列化；无需 value serializer，统一设为 String
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();
    }

    /**
     * 尝试获取 {@code permits} 个配额。
     *
     * @param name     限流器名称（决定独立配额池）
     * @param requests 时间窗口内的配额上限；为 0 表示不限流
     * @param seconds  时间窗口长度（秒）；为 0 表示不限流
     * @param permits  本次申请的数量
     * @return {@code 0} 表示通过；{@code >0} 表示已超限，需等待的毫秒数（窗口剩余时间）；Redis 异常时返回 0（降级放行）
     */
    public long tryAcquire(String name, int requests, int seconds, int permits) {
        if (requests == 0 || seconds == 0) {
            return 0;
        }
        try {
            Long waitMillis = redisTemplate.execute(
                    FIXED_WINDOW_SCRIPT,
                    Collections.singletonList(REDIS_PREFIX + name),
                    String.valueOf(requests), String.valueOf(seconds), String.valueOf(permits));
            return waitMillis == null ? 0 : waitMillis;
        } catch (Exception e) {
            // Redis 异常时降级放行，优先保证任务可用性
            log.warn("TaskGlobalRateLimiter: 限流检查失败, 降级放行, name=[{}]", name, e);
            return 0;
        }
    }

}
