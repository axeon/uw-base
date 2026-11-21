package uw.task.util;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import uw.common.util.SystemClock;

import java.util.Collections;

/**
 * uw-task 私有全局固定窗口限速器（单维度 permits，语义等价移植自 uw-kv GlobalFixedWindowRateLimiter，
 * key 空间私有 uw-task-rate:）。与 {@link TaskLocalRateLimiter} 的本地形态语义可互推。
 *
 * <p>实现走 Lua 单往返（INCRBY+PEXPIREAT 原子累加，客户端判定）——比 uw-kv 版省去 INCREX(Redis 8.8)
 * 快路径，单往返语义等价；Redis 异常时降级放行（保证调用方可用性优先）。
 * 返回 0=通过；&gt;0=需等待毫秒（按判定完成时刻的本地时钟近似计算，恒大于 0）。</p>
 *
 * @author axeon
 */
public final class TaskGlobalRateLimiter {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(TaskGlobalRateLimiter.class);

    /**
     * redis key 前缀（uw-task 私有空间）。
     */
    private static final String REDIS_PREFIX = "uw-task-rate:";

    /**
     * 固定窗口 Lua：INCRBY + PEXPIREAT(窗口终点) 单往返原子，返回累加后计数。
     */
    private static final DefaultRedisScript<Long> FIXED_WINDOW_SCRIPT = new DefaultRedisScript<>(
            "local n = redis.call('INCRBY', KEYS[1], ARGV[1]) "
                    + "redis.call('PEXPIREAT', KEYS[1], ARGV[2]) "
                    + "return n",
            Long.class);

    /**
     * 私有 String 模板（init 注入连接工厂后自建）。
     */
    private static StringRedisTemplate stringRedisTemplate;

    private TaskGlobalRateLimiter() {
    }

    /**
     * 注入私有连接工厂（自动配置调用一次）。
     */
    private static RedisConnectionFactory taskRedisConnectionFactory;

    /**
     * 注入私有连接工厂并构建操作模板（自动配置调用一次，须在 tryAcquire 前调用）。
     *
     * @param connectionFactory uw-task 私有 Redis 连接工厂
     */
    public static void init(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();
        TaskGlobalRateLimiter.stringRedisTemplate = template;
    }

    /**
     * 尝试获取配额（单维度）。
     *
     * @param name          限流器名称（调用方已编码域前缀）
     * @param limitPermits  窗口内许可上限
     * @param windowMillis  窗口毫秒
     * @param permits       申请数量（负入参防御为 0）
     * @return 0=通过；&gt;0=超限需等待毫秒
     */
    public static long tryAcquire(String name, int limitPermits, long windowMillis, int permits) {
        if (name == null || name.isEmpty() || limitPermits <= 0 || windowMillis < 1) {
            return 0;
        }
        if (permits < 0) {
            permits = 0;
        }
        long now = SystemClock.now();
        long windowEnd = (now / windowMillis + 1) * windowMillis;
        String key = REDIS_PREFIX + name;
        try {
            Long count = stringRedisTemplate.execute(FIXED_WINDOW_SCRIPT,
                    Collections.singletonList(key), String.valueOf(permits), String.valueOf(windowEnd));
            long c = count != null ? count : 0;
            if (c > limitPermits) {
                long remain = windowEnd - SystemClock.now();
                return remain > 0 ? remain : 1L;
            }
            return 0;
        } catch (Exception e) {
            log.warn("TaskGlobalRateLimiter: 限流检查失败, 降级放行, key=[{}]", key, e);
            return 0;
        }
    }
}
