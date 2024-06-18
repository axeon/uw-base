package uw.auth.service.ratelimit.impl;

import uw.auth.service.ratelimit.MscRateLimiter;
import uw.auth.service.ratelimit.RateLimitInfo;

/**
 * 基于guava RateLimiter实现的限速器。
 *
 * @author axeon
 */
public class NoneRateLimiter implements MscRateLimiter {


    /**
     * 尝试可否获得授权。
     *
     * @param rateLimitInfo 限速信息
     * @param permits       申请访问次数
     * @return 如果未超限则返回0，-1为不确定时间，其他为需要等待的毫秒数
     */
    @Override
    public int[] tryAcquire(RateLimitInfo rateLimitInfo, int permits) {
        return DEFAULT_PASS_VALUE;
    }

    /**
     * 尝试可否获得授权。
     *
     * @param rateLimitInfo 限速信息
     * @return 如果未超限则返回0，-1为不确定时间，其他为需要等待的毫秒数
     */
    @Override
    public int[] tryAcquire(RateLimitInfo rateLimitInfo) {
        return DEFAULT_PASS_VALUE;
    }


}
