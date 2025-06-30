package uw.mfa.helper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.*;
import uw.common.dto.ResponseData;
import uw.common.util.IpMatchUtils;
import uw.mfa.conf.UwMfaProperties;
import uw.mfa.constant.MfaResponseCode;
import uw.mfa.util.RedisKeyUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * IP限制帮助类。
 * 支持IP白名单，和IP错误计数限制。
 */
public class MfaIPLimitHelper {

    /**
     * redis错误限制前缀.
     */
    private static final String REDIS_LIMIT_IP_PREFIX = "ipLimit";

    /**
     * mfaRedisTemplate。
     */
    private static RedisTemplate<String, String> mfaRedisTemplate;

    /**
     * kv便捷操作的op。
     */
    private static ValueOperations<String, String> mfaRedisOp;

    /**
     * ip白名单。
     */
    private static List<IpMatchUtils.IpRange> ipWhiteList;

    /**
     * 配置文件。
     */
    private static UwMfaProperties uwMfaProperties;

    /**
     *
     */
    public MfaIPLimitHelper(UwMfaProperties uwMfaProperties, @Qualifier("mfaRedisTemplate") final RedisTemplate<String, String> mfaRedisTemplate) {
        if (StringUtils.isNotBlank(uwMfaProperties.getIpWhiteList())) {
            ipWhiteList = IpMatchUtils.sortList(uwMfaProperties.getIpWhiteList().split(","));
        }
        MfaIPLimitHelper.uwMfaProperties = uwMfaProperties;
        MfaIPLimitHelper.mfaRedisTemplate = mfaRedisTemplate;
        MfaIPLimitHelper.mfaRedisOp = mfaRedisTemplate.opsForValue();
    }

    /**
     * 检查IP白名单配置。
     *
     * @param ip
     * @return
     */
    public static boolean checkIpWhiteList(String ip) {
        return ipWhiteList != null && IpMatchUtils.matches(ipWhiteList, ip);
    }

    /**
     * 检查IP错误限制。
     *
     * @param userIp
     * @return
     */
    public static ResponseData checkIpErrorLimit(String userIp) {
        String redisKey = RedisKeyUtils.buildKey(REDIS_LIMIT_IP_PREFIX, userIp);
        String limitIpInfo = mfaRedisOp.get(RedisKeyUtils.buildKey(REDIS_LIMIT_IP_PREFIX, userIp));
        int errorCount = 0;
        if (StringUtils.isNotBlank(limitIpInfo)) {
            errorCount = Integer.parseInt(limitIpInfo);
        }
        if (errorCount >= uwMfaProperties.getIpLimitErrorTimes()) {
            long ttl = mfaRedisTemplate.getExpire(redisKey, TimeUnit.MINUTES) + 1;
            return ResponseData.errorCode(MfaResponseCode.IP_LIMIT_ERROR, userIp, uwMfaProperties.getIpLimitSeconds() / 60, errorCount, ttl);
        } else if (errorCount >= uwMfaProperties.getIpLimitWarnTimes()) {
            // 对于非白名单和登录错误次数达到限制，则发送警告。
            return ResponseData.warnCode(MfaResponseCode.IP_LIMIT_WARN, userIp, uwMfaProperties.getIpLimitSeconds() / 60, errorCount);
        }

        return ResponseData.success();
    }


    /**
     * 递增IP错误次数
     *
     * @param userIp
     */
    public static boolean incrementIpErrorTimes(String userIp, String remark) {
        Long times = mfaRedisOp.increment(RedisKeyUtils.buildKey(REDIS_LIMIT_IP_PREFIX, userIp));
        if (times != null && times == 1L) {
            return mfaRedisTemplate.expire(RedisKeyUtils.buildKey(REDIS_LIMIT_IP_PREFIX, userIp), uwMfaProperties.getIpLimitSeconds(), TimeUnit.SECONDS);
        }
        return false;
    }

    /**
     * 清除IP登录限制。
     *
     * @param ip
     */
    public static boolean clearIpErrorLimit(String ip) {
        return mfaRedisTemplate.delete(RedisKeyUtils.buildKey(REDIS_LIMIT_IP_PREFIX, ip));
    }


    /**
     * 计数限制信息条数。
     *
     * @return
     */
    public static long countMfaInfo() {
        Long count = mfaRedisTemplate.execute((RedisCallback<Long>) connection -> connection.serverCommands().dbSize());
        return count == null ? 0L : count;
    }

    /**
     * 获取IP限制列表。
     *
     * @return
     */
    public static Set<String> getIpErrorLimitList() {
        Set<String> keys = new LinkedHashSet<>();
        try (Cursor<String> cursor = mfaRedisTemplate.scan(ScanOptions.scanOptions().match(REDIS_LIMIT_IP_PREFIX + ":*").count(1000).build())) {
            cursor.forEachRemaining(key -> {
                keys.add(key.substring(REDIS_LIMIT_IP_PREFIX.length() + 1));
            });
        }
        return keys;
    }
}
