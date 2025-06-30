package uw.mfa.helper;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;
import uw.common.dto.ResponseData;
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
 */
public class MfaTotpHelper {

    private static final Logger log = LoggerFactory.getLogger(MfaTotpHelper.class);

    /**
     * redis 码校验前缀.
     */
    private static final String REDIS_TOTP_VERIFY_PREFIX = "totpVerify";

    /**
     * MFA配置。
     */
    private static UwMfaProperties uwMfaProperties;

    /**
     * Totp签发器。
     */
    private static TotpSecretDataGenerator totpSecretDataGenerator;

    /**
     * Totp验证器。
     */
    private static TotpCodeVerifier totpCodeVerifier;

    /**
     * mfaRedisTemplate。
     */
    private static RedisTemplate<String, String> mfaRedisTemplate;

    /**
     * kv便捷操作的op。
     */
    private static ValueOperations<String, String> mfaRedisOp;


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
     * 签发Totp密钥数据。
     *
     * @param label
     * @return
     */
    public static ResponseData<TotpSecretData> issue(String label) {
        return totpSecretDataGenerator.issue(label, null, 0);
    }

    /**
     * 签发Totp密钥数据。
     *
     * @param label  标签
     * @param issuer 签发人
     * @param qrSize 二维码尺寸
     * @return
     */
    public static ResponseData<TotpSecretData> issue(String label, String issuer, int qrSize) {
        return totpSecretDataGenerator.issue(label, issuer, qrSize);
    }

    /**
     * 验证Totp密钥数据。
     *
     * @param userInfo
     * @param totpSecret 密钥
     * @param totpCode   验证码
     * @return
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
     * 生成16位随机恢复码。
     *
     * @return
     */
    public static String[] generateRecoveryCode(int amount) {
        return ToptRecoveryCodeGenerator.generateCodes(amount);
    }

    /**
     * 检查校验错误限制。
     *
     * @param userInfo
     * @return
     */
    public static ResponseData checkVerifyErrorLimit(String userInfo) {
        String key = RedisKeyUtils.buildKey(REDIS_TOTP_VERIFY_PREFIX, userInfo);
        String limitInfo = mfaRedisOp.get(RedisKeyUtils.buildKey(REDIS_TOTP_VERIFY_PREFIX, userInfo));
        int errorCount = 0;
        if (StringUtils.isNotBlank(limitInfo)) {
            errorCount = Integer.parseInt(limitInfo);
        }
        if (errorCount >= uwMfaProperties.getTotpVerifyErrorTimes()) {
            long ttl = mfaRedisTemplate.getExpire(key, TimeUnit.MINUTES) + 1;
            return ResponseData.errorCode(MfaResponseCode.TOTP_VERIFY_LIMIT_ERROR, userInfo, (uwMfaProperties.getTotpVerifyLimitSeconds() / 60), errorCount, ttl);
        }
        return ResponseData.success();
    }

    /**
     * 递增校验错误次数
     *
     * @param userInfo
     */
    public static boolean incrementVerifyErrorTimes(String userInfo) {
        Long times = mfaRedisOp.increment(RedisKeyUtils.buildKey(REDIS_TOTP_VERIFY_PREFIX, userInfo));
        if (times != null && times == 1L) {
            return mfaRedisTemplate.expire(RedisKeyUtils.buildKey(REDIS_TOTP_VERIFY_PREFIX, userInfo), uwMfaProperties.getTotpVerifyLimitSeconds(), TimeUnit.SECONDS);
        }
        return false;
    }

    /**
     * 清除设备验证码校验限制。
     *
     * @param userInfo
     */
    public static boolean clearVerifyLimit(String userInfo) {
        return mfaRedisTemplate.delete(RedisKeyUtils.buildKey(REDIS_TOTP_VERIFY_PREFIX, userInfo));
    }

    /**
     * 获取校验错误列表。
     *
     * @return
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
