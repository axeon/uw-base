package uw.task.util;

import uw.common.util.FixedWindowRateLimiter;
import uw.common.util.SystemClock;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内固定窗口限速器（uw-common {@link FixedWindowRateLimiter} 的任务域薄壳）。
 *
 * <p>限流作用域为单个 JVM 实例，不跨进程共享配额。算法为固定窗口：窗口边界对齐墙钟网格，
 * 与全局限速（{@code TaskGlobalRateLimiter}）语义一致——同配置下本地/全局行为可互推。</p>
 *
 * <p><b>等待语义</b></p>
 * 超限时 sleep 后重试，sleep 取"到窗口边界的剩余毫秒"与"到等待截止的剩余毫秒"的较小值，
保证总等待时长不超过 {@code waitTime} 秒，超时返回 false——
 * 与 {@link TaskRateLimitHelper#acquireGlobalWithWait} 的全局限速等待循环同款语义。
 *
 * <p><b>缓存与清理</b></p>
 * 限流器按 {@code name} 缓存于 {@link ConcurrentHashMap}，永不淘汰。调用方应使用有限的
 * {@code name} 集合（如固定的任务类名、任务类名+标签），避免动态 name 导致内存增长。
 * 同一 name 的 {@code requests/seconds} 配置动态变化时重建限流器（当前窗口计数归零，属切换边界正常行为）。
 *
 * @author axeon
 */
public class TaskLocalRateLimiter {

    /**
     * 默认构造器。
     */
    public TaskLocalRateLimiter() {
    }

    /**
     * 按 name 缓存的限流器实例（携带构造参数用于配置变化检测）。
     */
    private final ConcurrentHashMap<String, LimiterHolder> map = new ConcurrentHashMap<>();

    /**
     * 尝试获取 {@code permits} 个令牌，最多等待 {@code waitTime} 秒。
     *
     * @param name     限流器名称（决定独立配额池）
     * @param requests 时间窗口内的配额总数；为 0 表示不限流
     * @param seconds  时间窗口长度（秒）；为 0 表示不限流
     * @param waitTime 配额不足时最长等待秒数，超时仍未获取则返回 false
     * @param permits  本次申请的令牌数
     * @return true 表示在 waitTime 内成功获取配额；false 表示超时未获取
     */
    public boolean tryAcquire(String name, int requests, int seconds, long waitTime, int permits) {
        if (requests == 0 || seconds == 0) {
            return true;
        }
        FixedWindowRateLimiter limiter = limiter(name, requests, seconds);
        long end = SystemClock.now() + waitTime * 1000L;
        long now;
        while ((now = SystemClock.now()) <= end) {
            FixedWindowRateLimiter.Result rejected = limiter.tryAcquire(permits, 0);
            if (rejected == null) {
                return true;
            }
            try {
                // 至少 sleep 1ms 避免边界抖动空转；不超过等待截止时刻
                Thread.sleep(Math.max(1L, Math.min(rejected.remainMillis, end - now)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * 获取（或按新配置重建）name 对应的本地限流器。
     *
     * @param name     限流器名称
     * @param requests 配额总数
     * @param seconds  窗口长度（秒）
     * @return 本地固定窗口限流器
     */
    private FixedWindowRateLimiter limiter(String name, int requests, int seconds) {
        LimiterHolder holder = map.compute(name, (key, existing) ->
                existing != null && existing.requests == requests && existing.seconds == seconds
                        ? existing
                        : new LimiterHolder(requests, seconds, new FixedWindowRateLimiter(seconds * 1000L, requests, Long.MAX_VALUE)));
        return holder.limiter;
    }

    /**
     * 限流器持有者：记录构造参数用于配置变化检测（变化即重建）。
     */
    private static final class LimiterHolder {

        /**
         * 窗口内配额总数。
         */
        final int requests;

        /**
         * 窗口长度（秒）。
         */
        final int seconds;

        /**
         * 本地固定窗口限流器。
         */
        final FixedWindowRateLimiter limiter;

        /**
         * 记录限流器的构造参数（配置变化检测）与实例。
         *
         * @param requests 窗口内配额总数
         * @param seconds  窗口长度（秒）
         * @param limiter  本地固定窗口限流器
         */
        LimiterHolder(int requests, int seconds, FixedWindowRateLimiter limiter) {
            this.requests = requests;
            this.seconds = seconds;
            this.limiter = limiter;
        }
    }
}
