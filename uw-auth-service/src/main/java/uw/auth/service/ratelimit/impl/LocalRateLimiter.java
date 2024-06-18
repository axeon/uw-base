package uw.auth.service.ratelimit.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.RateLimiter;
import uw.auth.service.ratelimit.MscRateLimiter;
import uw.auth.service.ratelimit.RateLimitInfo;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 基于guava RateLimiter实现的限速器。
 *
 * @author axeon
 */
public class LocalRateLimiter implements MscRateLimiter {

    /**
     * 限速器缓存。
     */
    private Cache<String, RateLimiter> limiterCache = null;


    public LocalRateLimiter(int cacheNum) {
        limiterCache = Caffeine
                .newBuilder()
                .maximumSize(cacheNum)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build();
    }

    /**
     * 尝试可否获得授权。
     *
     * @param rateLimitInfo 限速信息
     * @param permits       申请访问次数
     * @return 如果未超限则返回0，-1为不确定时间，其他为需要等待的毫秒数
     */
    @Override
    public int[] tryAcquire(RateLimitInfo rateLimitInfo, int permits) {
        if (rateLimitInfo == null)
            return DEFAULT_PASS_VALUE;
        final double rate = (double) rateLimitInfo.getRequests() / (double) rateLimitInfo.getSeconds();
        RateLimiter rateLimiter = limiterCache.get(rateLimitInfo.getUri(), new Function<String, RateLimiter>() {
            @Override
            public RateLimiter apply(String s) {
                return RateLimiter.create(rate);
            }
        });
        if (rateLimiter != null) {
            //检查并修改
            if (rateLimiter.getRate() != rate) {
                rateLimiter.setRate(rate);
            }
            if (rateLimiter.tryAcquire(permits)) {
                return DEFAULT_PASS_VALUE;
            } else {
                return DEFAULT_DENY_VALUE;
            }
        } else {
            return DEFAULT_PASS_VALUE;
        }
    }

    /**
     * 尝试可否获得授权。
     *
     * @param rateLimitInfo 限速信息
     * @return 如果未超限则返回0，-1为不确定时间，其他为需要等待的毫秒数
     */
    @Override
    public int[] tryAcquire(RateLimitInfo rateLimitInfo) {
        return tryAcquire(rateLimitInfo, 1);
    }

}
