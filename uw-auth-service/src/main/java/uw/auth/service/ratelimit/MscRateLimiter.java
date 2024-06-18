package uw.auth.service.ratelimit;

/**
 * 限速控制器接口。
 */
public interface MscRateLimiter {

    /**
     * 默认通过值。
     */
    int[] DEFAULT_PASS_VALUE = new int[]{0, 0};

    /**
     * 默认拒绝值。
     */
    int[] DEFAULT_DENY_VALUE = new int[]{-1, -1};


    /**
     * 尝试可否获得授权。
     *
     * @param rateLimitInfo 限速信息
     * @param permits       申请访问次数
     * @return 0:可用许可数(0为未知，负数为不许可),1:需要等待毫秒数(0为无需等待)。
     */
    int[] tryAcquire(RateLimitInfo rateLimitInfo, int permits);

    /**
     * 尝试可否获得授权。
     *
     * @param rateLimitInfo 限速信息
     * @return 如果未超限则返回0，-1为不确定时间，其他为需要等待的毫秒数a
     */
    int[] tryAcquire(RateLimitInfo rateLimitInfo);

}
