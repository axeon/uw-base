package uw.mfa.helper;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;
import uw.common.response.ResponseData;
import uw.mfa.conf.UwMfaProperties;
import uw.mfa.constant.MfaResponseCode;
import uw.mfa.totp.ToptRecoveryCodeGenerator;
import uw.mfa.totp.TotpCodeVerifier;
import uw.mfa.totp.TotpSecretDataGenerator;
import uw.mfa.totp.vo.TotpSecretData;
import uw.mfa.util.RedisKeyUtils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * TOTP验证码帮助类。
 * <p>封装TOTP密钥签发（含otpauth URI与二维码）、验证码校验（含校验错误限制）、恢复码生成能力。</p>
 * <p>遵循RFC 6238规范，兼容Google Authenticator等主流App。</p>
 */
public class MfaTotpHelper {

    private static final Logger log = LoggerFactory.getLogger(MfaTotpHelper.class);

    /**
     * Redis TOTP校验错误限制key前缀。
     */
    private static final String REDIS_TOTP_VERIFY_PREFIX = "totpVerify";

    /**
     * MFA配置。
     */
    private static UwMfaProperties uwMfaProperties;

    /**
     * TOTP密钥签发器。
     */
    private static TotpSecretDataGenerator totpSecretDataGenerator;

    /**
     * TOTP验证码校验器。
     */
    private static TotpCodeVerifier totpCodeVerifier;

    /**
     * MFA专用RedisTemplate。
     */
    private static RedisTemplate<String, String> mfaRedisTemplate;

    /**
     * kv便捷操作ops。
     */
    private static ValueOperations<String, String> mfaRedisOp;


    /**
     * 构造器（由Spring注入，按配置初始化签发器与校验器）。
     *
     * @param uwMfaProperties   MFA配置
     * @param mfaRedisTemplate  MFA专用RedisTemplate
     */
    public MfaTotpHelper(UwMfaProperties uwMfaProperties, @Qualifier("mfaRedisTemplate") final RedisTemplate<String, String> mfaRedisTemplate) {
        MfaTotpHelper.uwMfaProperties = uwMfaProperties;
        MfaTotpHelper.mfaRedisTemplate = mfaRedisTemplate;
        MfaTotpHelper.mfaRedisOp = mfaRedisTemplate.opsForValue();
        MfaTotpHelper.totpCodeVerifier = new TotpCodeVerifier(uwMfaProperties.getTotpAlgorithm(), uwMfaProperties.getTotpCodeLength(), uwMfaProperties.getTotpTimePeriod(),
                uwMfaProperties.getTotpTimePeriodDiscrepancy());
        MfaTotpHelper.totpSecretDataGenerator = new TotpSecretDataGenerator(uwMfaProperties.getTotpIssuer(), uwMfaProperties.getTotpAlgorithm(),
                uwMfaProperties.getTotpSecretLength(), uwMfaProperties.getTotpCodeLength(), uwMfaProperties.getTotpTimePeriod(), uwMfaProperties.isTotpGenQr(),
                uwMfaProperties.getTotpQrSize());
    }

    /**
     * 签发TOTP密钥数据（使用默认签发人与二维码尺寸）。
     *
     * @param label 标签（通常为用户标识）
     * @return 含密钥、URI、二维码的TotpSecretData
     */
    public static ResponseData<TotpSecretData> issue(String label) {
        return totpSecretDataGenerator.issue(label, null, 0);
    }

    /**
     * 签发TOTP密钥数据（自定义签发人与二维码尺寸）。
     *
     * @param label  标签（通常为用户标识）
     * @param issuer 签发人，为空使用默认值
     * @param qrSize 二维码尺寸，小于100使用默认值
     * @return 含密钥、URI、二维码的TotpSecretData
     */
    public static ResponseData<TotpSecretData> issue(String label, String issuer, int qrSize) {
        return totpSecretDataGenerator.issue(label, issuer, qrSize);
    }

    /**
     * 校验TOTP验证码。
     * <p>先检查校验错误限制，再调用校验器比对，校验失败递增错误次数。</p>
     *
     * @param userInfo   用户标识（用于校验错误限制key）
     * @param totpSecret Base32密钥
     * @param totpCode   待校验的验证码
     * @return 校验成功返回success，超限返回TOTP_VERIFY_LIMIT_ERROR，其他失败返回对应errorCode
     */
    public static ResponseData verifyCode(String userInfo, String totpSecret, String totpCode) {
        //检查设备码验证限制情况
        ResponseData checkData = checkVerifyErrorLimit(userInfo);
        if (checkData.isNotSuccess()) {
            return checkData;
        }
        ResponseData verifyData = totpCodeVerifier.verifyCode(totpSecret, totpCode);
        if (verifyData.isNotSuccess()) {
            incrementVerifyErrorTimes(userInfo);
        }
        return verifyData;
    }

    /**
     * 批量生成16位随机恢复码（用于TOTP密钥丢失应急登录）。
     *
     * @param amount 生成数量，小于1按1处理
     * @return 恢复码字符串数组
     */
    public static String[] generateRecoveryCode(int amount) {
        return ToptRecoveryCodeGenerator.generateCodes(amount);
    }

    /**
     * 检查TOTP校验错误限制。
     *
     * @param userInfo 用户标识
     * @return 未超限返回success，超限返回TOTP_VERIFY_LIMIT_ERROR
     */
    public static ResponseData checkVerifyErrorLimit(String userInfo) {
        String redisKey = RedisKeyUtils.buildKey(REDIS_TOTP_VERIFY_PREFIX, userInfo);
        String limitInfo = mfaRedisOp.get(RedisKeyUtils.buildKey(REDIS_TOTP_VERIFY_PREFIX, userInfo));
        int errorCount = 0;
        if (StringUtils.isNotBlank(limitInfo)) {
            try {
                errorCount = Integer.parseInt(limitInfo);
            } catch (NumberFormatException e) {
                errorCount = 0;
            }
        }
        if (errorCount >= uwMfaProperties.getTotpVerifyErrorTimes()) {
            long ttl = mfaRedisTemplate.getExpire(redisKey, TimeUnit.MINUTES) + 1;
            if (ttl < 1) {
                ttl = 1;
            }
            return ResponseData.errorCode(MfaResponseCode.TOTP_VERIFY_LIMIT_ERROR, userInfo, uwMfaProperties.getTotpVerifyLimitSeconds() / 60, errorCount, ttl);
        }
        return ResponseData.success();
    }

    /**
     * 递增TOTP校验错误次数（首次递增设置过期时间）。
     *
     * @param userInfo 用户标识
     * @return 本次是否设置了过期时间（仅首次递增返回true）
     */
    public static boolean incrementVerifyErrorTimes(String userInfo) {
        Long times = mfaRedisOp.increment(RedisKeyUtils.buildKey(REDIS_TOTP_VERIFY_PREFIX, userInfo));
        if (times != null && times == 1L) {
            return mfaRedisTemplate.expire(RedisKeyUtils.buildKey(REDIS_TOTP_VERIFY_PREFIX, userInfo), uwMfaProperties.getTotpVerifyLimitSeconds(), TimeUnit.SECONDS);
        }
        return false;
    }

    /**
     * 清除TOTP校验错误限制。
     *
     * @param userInfo 用户标识
     * @return 删除成功返回true
     */
    public static boolean clearVerifyLimit(String userInfo) {
        return mfaRedisTemplate.delete(RedisKeyUtils.buildKey(REDIS_TOTP_VERIFY_PREFIX, userInfo));
    }

    /**
     * 获取TOTP校验错误限制列表（扫描totpVerify:* key并去除前缀）。
     *
     * @return 用户标识集合
     */
    public static Set<String> getVerifyErrorList() {
        Set<String> keys = new LinkedHashSet<>();
        try (Cursor<String> cursor = mfaRedisTemplate.scan(ScanOptions.scanOptions().match(REDIS_TOTP_VERIFY_PREFIX + ":*").count(1000).build())) {
            cursor.forEachRemaining(key -> {
                keys.add(key.substring(REDIS_TOTP_VERIFY_PREFIX.length() + 1));
            });
        }
        return keys;
    }


}
