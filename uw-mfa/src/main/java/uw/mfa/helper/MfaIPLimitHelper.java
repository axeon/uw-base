package uw.mfa.helper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.*;
import uw.common.response.ResponseData;
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
 * <p>支持IP白名单（CIDR格式，命中白名单直接豁免）与基于Redis的IP错误计数限制（warn/error两级）。</p>
 */
public class MfaIPLimitHelper {

    /**
     * Redis IP错误限制key前缀。
     */
    private static final String REDIS_LIMIT_IP_PREFIX = "ipLimit";

    /**
     * MFA专用RedisTemplate。
     */
    private static RedisTemplate<String, String> mfaRedisTemplate;

    /**
     * kv便捷操作ops。
     */
    private static ValueOperations<String, String> mfaRedisOp;

    /**
     * 已排序的IP白名单范围列表。
     */
    private static List<IpMatchUtils.IpRange> ipWhiteList;

    /**
     * MFA配置。
     */
    private static UwMfaProperties uwMfaProperties;

    /**
     * 构造器（由Spring注入，初始化静态依赖与白名单）。
     *
     * @param uwMfaProperties   MFA配置
     * @param mfaRedisTemplate  MFA专用RedisTemplate
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
     * 检查IP是否在白名单中。
     *
     * @param ip 用户IP
     * @return 在白名单返回true，否则false
     */
    public static boolean checkIpWhiteList(String ip) {
        return ipWhiteList != null && IpMatchUtils.matches(ipWhiteList, ip);
    }

    /**
     * 检查IP错误限制。
     * <p>白名单IP直接放行；否则按错误次数返回：达到errorTimes返回error（已屏蔽），</p>
     * <p>达到warnTimes返回warn（提示需验证码），未达阈值返回success。</p>
     *
     * @param userIp 用户IP
     * @return success/warn/error 三态ResponseData
     */
    public static ResponseData checkIpErrorLimit(String userIp) {
        // 白名单IP直接放行, 不受错误限制影响
        if (checkIpWhiteList(userIp)) {
            return ResponseData.success();
        }
        String redisKey = RedisKeyUtils.buildKey(REDIS_LIMIT_IP_PREFIX, userIp);
        String limitIpInfo = mfaRedisOp.get(RedisKeyUtils.buildKey(REDIS_LIMIT_IP_PREFIX, userIp));
        int errorCount = 0;
        if (StringUtils.isNotBlank(limitIpInfo)) {
            try {
                errorCount = Integer.parseInt(limitIpInfo);
            } catch (NumberFormatException e) {
                // Redis值被篡改或脏数据, 重置为0避免误判触发限制
                errorCount = 0;
            }
        }
        if (errorCount >= uwMfaProperties.getIpLimitErrorTimes()) {
            long ttl = mfaRedisTemplate.getExpire(redisKey, TimeUnit.MINUTES) + 1;
            if (ttl < 1) {
                ttl = 1;
            }
            return ResponseData.errorCode(MfaResponseCode.IP_LIMIT_ERROR, userIp, uwMfaProperties.getIpLimitSeconds() / 60, errorCount, ttl);
        } else if (errorCount >= uwMfaProperties.getIpLimitWarnTimes()) {
            // 对于非白名单和登录错误次数达到限制，则发送警告。
            return ResponseData.warnCode(MfaResponseCode.IP_LIMIT_WARN, userIp, uwMfaProperties.getIpLimitSeconds() / 60, errorCount);
        }

        return ResponseData.success();
    }


    /**
     * 递增IP错误次数。
     * <p>白名单IP不计入；首次递增时同步设置过期时间，后续递增不重置TTL。</p>
     *
     * @param userIp 用户IP
     * @param remark 备注（错误码等，预留）
     * @return 本次是否设置了过期时间（仅首次递增返回true）
     */
    public static boolean incrementIpErrorTimes(String userIp, String remark) {
        // 白名单IP不计入错误次数
        if (checkIpWhiteList(userIp)) {
            return false;
        }
        Long times = mfaRedisOp.increment(RedisKeyUtils.buildKey(REDIS_LIMIT_IP_PREFIX, userIp));
        if (times != null && times == 1L) {
            return mfaRedisTemplate.expire(RedisKeyUtils.buildKey(REDIS_LIMIT_IP_PREFIX, userIp), uwMfaProperties.getIpLimitSeconds(), TimeUnit.SECONDS);
        }
        return false;
    }

    /**
     * 清除IP错误限制。
     *
     * @param ip 用户IP
     * @return 删除成功返回true
     */
    public static boolean clearIpErrorLimit(String ip) {
        return mfaRedisTemplate.delete(RedisKeyUtils.buildKey(REDIS_LIMIT_IP_PREFIX, ip));
    }


    /**
     * 统计MFA Redis库所有限制信息条数（基于dbSize，包含所有限制类型）。
     *
     * @return 信息条数
     */
    public static long countMfaInfo() {
        Long count = mfaRedisTemplate.execute((RedisCallback<Long>) connection -> connection.serverCommands().dbSize());
        return count == null ? 0L : count;
    }

    /**
     * 获取IP错误限制列表（扫描ipLimit:* key并去除前缀）。
     *
     * @return IP集合
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
