package uw.auth.service.ratelimit.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import uw.auth.service.ratelimit.MscRateLimiter;
import uw.auth.service.ratelimit.RateLimitInfo;

import java.util.Collections;

/**
 * 基于redis实现的序列管理器。
 *
 * @author axeon
 */
public class GlobalRateLimiter implements MscRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(GlobalRateLimiter.class);

    private static final String REDIS_PREFIX = "RateLimit:";

    /**
     * redis模板。
     */
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * LUA脚本。
     * 返回值为：可用请求数,等待毫秒数。
     */
    private static final RedisScript<String> LUA_RATE_LIMIT = RedisScript.of(
            "local key = KEYS[1];\n" +
                    "local requests = tonumber(ARGV[1]);\n" +
                    "local millis = tonumber(ARGV[2])*1000;\n" +
                    "local permits = tonumber(ARGV[3]);\n" +
                    "local remainRequests=0;\n" +
                    "local waitMillis=0;\n" +
                    "local nowRate= redis.call('INCRBY', key,permits);\n" +
                    "if (nowRate==permits) then \n" +//如果是第一次执行，设置有效期保护。
                    "    redis.call('PEXPIRE',key,millis);\n" +
                    "end\n" +
                    "remainRequests = requests-nowRate;\n" +
                    "if (remainRequests<1) then \n" +//请求数超过限制
                    "    waitMillis = redis.call('PTTL',key);\n" +
                    "    if (waitMillis == -1) then \n" +
                    "        redis.call('PEXPIRE',key,millis);\n" +
                    "        waitMillis = millis;\n" +
                    "    end\n" +
                    "end \n" +
                    "return remainRequests..','..waitMillis", String.class);

    public GlobalRateLimiter(final RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
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
        String retString = redisTemplate.execute(LUA_RATE_LIMIT, Collections.singletonList(REDIS_PREFIX + rateLimitInfo.getUri()), rateLimitInfo.getRequests(), rateLimitInfo.getSeconds(), permits);
        int[] ret = new int[2];
        int p = retString.indexOf(',');
        if (p > 0) {
            try {
                ret[0] = Integer.parseInt(retString.substring(0, p));
                ret[1] = Integer.parseInt(retString.substring(p + 1));
            } catch (Exception e) {
                log.error("RateLimit error by return data: {}", retString);
            }
        }
        return ret;
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
