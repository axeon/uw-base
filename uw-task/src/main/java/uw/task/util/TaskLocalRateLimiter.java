package uw.task.util;

import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 进程内令牌桶限流器，基于 Guava {@link RateLimiter} 实现。
 *
 * <p>限流作用域为单个 JVM 实例，不跨进程共享配额。算法为令牌桶：令牌按
 * {@code requests / seconds}（个/秒）的速率匀速产生，{@code permits} 个令牌可用则立即通过，
 * 否则在 {@code waitTime} 秒内等待，仍不足则拒绝。</p>
 *
 * <h3>与 {@link TaskGlobalRateLimiter} 的关系</h3>
 * 两者算法一致（均为令牌桶），区别仅在作用域与返回值：
 * <ul>
 *   <li>本类：进程内，返回布尔（一次性判定，配合 {@code waitTime} 内部阻塞等待）。</li>
 *   <li>{@link TaskGlobalRateLimiter}：跨进程（Redis），返回等待毫秒（便于调用方循环退避重试）。</li>
 * </ul>
 *
 * <h3>缓存与清理</h3>
 * 限流器按 {@code name} 缓存于 {@link ConcurrentHashMap}，永不淘汰。调用方应使用有限的
 * {@code name} 集合（如固定的任务类名、任务类名+标签），避免动态 name 导致内存增长。
 *
 * @author axeon
 */
public class TaskLocalRateLimiter {

    /**
     * 按 name 缓存的限流器实例。name 应为有限集合，否则会造成内存泄漏。
     */
    private final ConcurrentHashMap<String, RateLimiter> map = new ConcurrentHashMap<>();

    /**
     * 尝试获取 {@code permits} 个令牌，最多等待 {@code waitTime} 秒。
     *
     * <p>同一 {@code name} 的速率配置可动态变化：若本次传入的 {@code requests/seconds}
     * 与已缓存限流器的速率不同，会通过 {@link RateLimiter#setRate} 实时调整。</p>
     *
     * @param name     限流器名称（决定独立配额池）
     * @param requests 时间窗口内的配额总数；为 0 表示不限流
     * @param seconds  时间窗口长度（秒）；为 0 表示不限流
     * @param waitTime 令牌不足时最长等待秒数，超时仍未获取则返回 false
     * @param permits  本次申请的令牌数
     * @return true 表示在 waitTime 内成功获取令牌；false 表示超时未获取
     */
    public boolean tryAcquire(String name, int requests, int seconds, long waitTime, int permits) {
        if (requests == 0 || seconds == 0) {
            return true;
        }
        final double rate = (double) requests / (double) seconds;
        RateLimiter limiter = map.computeIfAbsent(name, key -> RateLimiter.create(rate));
        // 速率配置可能动态变化，按需调整
        if (limiter.getRate() != rate) {
            limiter.setRate(rate);
        }
        return limiter.tryAcquire(permits, waitTime, TimeUnit.SECONDS);
    }

}
