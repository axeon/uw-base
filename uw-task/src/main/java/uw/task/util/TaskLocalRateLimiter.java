package uw.task.util;

import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于guava RateLimiter实现的限速器。
 *
 * @author axeon
 */
public class TaskLocalRateLimiter {

    private final ConcurrentHashMap<String, RateLimiter> map = new ConcurrentHashMap<>();

    /**
     * 尝试获取限制允许状态。
     *
     * @param name
     * @return
     */
    public boolean tryAcquire(String name, int requests, int seconds, long waitTime, int permits) {
        if (requests == 0 || seconds == 0) {
            return true;
        }
        final double rate = (double) requests / (double) seconds;
        RateLimiter limiter = map.computeIfAbsent(name, key -> RateLimiter.create(rate));
        if (limiter != null) {
            //检查并修改
            if (limiter.getRate() != rate) {
                limiter.setRate(rate);
            }
            return limiter.tryAcquire(permits, waitTime, TimeUnit.SECONDS);
        } else {
            return true;
        }
    }

}
