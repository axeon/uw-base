package uw.mfa.helper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import uw.common.dto.ResponseData;
import uw.common.util.IpMatchUtils;
import uw.mfa.conf.UwMfaProperties;
import uw.mfa.constant.MfaResponseCode;
import uw.mfa.util.RedisKeyUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * IP限制帮助类。
 * 支持IP白名单，和IP错误计数限制。
 */
public class MfaIPLimitHelper {

    /**
     * redis错误限制前缀.
     */
    private static final String REDIS_LIMIT_IP_PREFIX = "limitIP";

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
        if (StringUtils.isNotBlank( uwMfaProperties.getIpWhiteList() )) {
            ipWhiteList = IpMatchUtils.sortList( uwMfaProperties.getIpWhiteList().split( "," ) );
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
        if (ipWhiteList != null && IpMatchUtils.matches( ipWhiteList, ip )) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 检查IP错误限制。
     *
     * @param userIp
     * @return
     */
    public static ResponseData checkIpErrorLimit(String userIp) {
        //不在白名单的，才检查登录限制。
        if (!checkIpWhiteList( userIp )) {
            String key = RedisKeyUtils.buildKey( REDIS_LIMIT_IP_PREFIX, userIp );
            String ics = mfaRedisOp.get( RedisKeyUtils.buildKey( REDIS_LIMIT_IP_PREFIX, userIp ) );
            if (StringUtils.isNotBlank( ics )) {
                int ic = Integer.parseInt( ics );
                if (ic >= uwMfaProperties.getIpLimitErrorTimes()) {
                    long ttl = mfaRedisTemplate.getExpire( key, TimeUnit.MINUTES ) + 1;
                    return ResponseData.errorCode( MfaResponseCode.IP_LIMIT_ERROR, userIp, (uwMfaProperties.getIpLimitSeconds() / 60), ic, ttl );
                } else if (ic >= uwMfaProperties.getIpLimitWarnTimes()) {
                    return ResponseData.warnCode( MfaResponseCode.IP_LIMIT_WARN, userIp, (uwMfaProperties.getIpLimitSeconds() / 60), ic );
                }
            }
        }
        return ResponseData.success();
    }


    /**
     * 递增IP错误次数
     *
     * @param userIp
     */
    public static void incrementIpErrorTimes(String userIp, String remark) {
        if (!MfaIPLimitHelper.checkIpWhiteList( userIp )) {
            if (mfaRedisOp.increment( RedisKeyUtils.buildKey( REDIS_LIMIT_IP_PREFIX, userIp ) ) == 1L) {
                mfaRedisTemplate.expire( RedisKeyUtils.buildKey( REDIS_LIMIT_IP_PREFIX, userIp ), uwMfaProperties.getIpLimitSeconds(), TimeUnit.SECONDS );
            }
        }
    }

    /**
     * 清除IP登录限制。
     *
     * @param ip
     */
    public static void clearIpErrorLimit(String ip) {
        if (!MfaIPLimitHelper.checkIpWhiteList( ip )) {
            mfaRedisTemplate.delete( RedisKeyUtils.buildKey( REDIS_LIMIT_IP_PREFIX, ip ) );
        }
    }


    /**
     * 计数限制信息条数。
     *
     * @return
     */
    public static long countLimitInfo() {
        return mfaRedisTemplate.execute( (RedisCallback<Long>) connection -> connection.dbSize() );
    }
}
