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
import uw.mfa.captcha.CaptchaService;
import uw.mfa.captcha.vo.CaptchaData;
import uw.mfa.captcha.vo.CaptchaQuestion;
import uw.mfa.conf.UwMfaProperties;
import uw.mfa.constant.MfaResponseCode;
import uw.mfa.util.RedisKeyUtils;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * captcha帮助类。
 */
public class MfaCaptchaHelper {

    private static final Logger log = LoggerFactory.getLogger( MfaCaptchaHelper.class );

    /**
     * redis captcha前缀.
     */
    private static final String REDIS_CAPTCHA_PREFIX = "captcha";

    /**
     * redisCAPTCHA限制前缀.
     */
    private static final String REDIS_LIMIT_CAPTCHA_PREFIX = "limitCaptcha";

    /**
     * mfaRedisTemplate。
     */
    private static RedisTemplate<String, String> mfaRedisTemplate;

    /**
     * kv便捷操作的op。
     */
    private static ValueOperations<String, String> mfaRedisOp;

    /**
     * captcha服务。
     */
    private static CaptchaService captchaService = new CaptchaService();
    /**
     * 配置文件。
     */
    private static UwMfaProperties uwMfaProperties;

    /**
     *
     */
    public MfaCaptchaHelper(UwMfaProperties uwMfaProperties, @Qualifier("mfaRedisTemplate") final RedisTemplate<String, String> mfaRedisTemplate) {
        MfaCaptchaHelper.uwMfaProperties = uwMfaProperties;
        MfaCaptchaHelper.mfaRedisTemplate = mfaRedisTemplate;
        MfaCaptchaHelper.mfaRedisOp = mfaRedisTemplate.opsForValue();
        MfaCaptchaHelper.captchaService = new CaptchaService( uwMfaProperties.getCaptchaStrategies() );
    }

    /**
     * 生成captcha。
     *
     * @param captchaId
     */
    public static ResponseData<CaptchaQuestion> generateCaptcha(String userIp, String captchaId) {
        //检查发送限制情况
        String redisKey = RedisKeyUtils.buildKey( REDIS_LIMIT_CAPTCHA_PREFIX, userIp );
        long sentTimes = Objects.requireNonNullElse(mfaRedisOp.increment(redisKey),0L);
        if (sentTimes == 1L) {
            mfaRedisTemplate.expire( redisKey, uwMfaProperties.getCaptchaSendLimitSeconds(), TimeUnit.SECONDS );
        }
        if (sentTimes >= uwMfaProperties.getCaptchaSendLimitTimes()) {
            long ttl = mfaRedisTemplate.getExpire(redisKey, TimeUnit.MINUTES) + 1;
            return ResponseData.errorCode( MfaResponseCode.CAPTCHA_SEND_LIMIT_ERROR, userIp, (uwMfaProperties.getCaptchaSendLimitSeconds() / 60),
                    uwMfaProperties.getCaptchaSendLimitTimes(), ttl );
        }
        ResponseData<CaptchaData> captchaResData = captchaService.generateCaptcha( captchaId );
        if (captchaResData.isNotSuccess()) {
            return ResponseData.errorCode( MfaResponseCode.CAPTCHA_GENERATE_ERROR );
        }
        CaptchaData captchaData = captchaResData.getData();
        CaptchaQuestion captchaQuestion = captchaData.getCaptchaQuestion();
        //设置有效期
        captchaQuestion.setCaptchaTTL( uwMfaProperties.getCaptchaExpiredSeconds() );
        mfaRedisOp.set( RedisKeyUtils.buildKey( REDIS_CAPTCHA_PREFIX, captchaQuestion.getCaptchaId() ), captchaData.getCaptchaResult(),
                uwMfaProperties.getCaptchaExpiredSeconds(), TimeUnit.SECONDS );
        return ResponseData.success( captchaQuestion );
    }

    /**
     * 验证captcha。
     *
     * @param captchaId
     * @return
     */
    public static ResponseData verifyCaptcha(String captchaId, String captchaSign) {
        if (StringUtils.isBlank( captchaId ) || StringUtils.isBlank( captchaSign )) {
            return ResponseData.errorCode( MfaResponseCode.CAPTCHA_LOST_ERROR );
        }
        String captchaResult = mfaRedisOp.getAndDelete( RedisKeyUtils.buildKey( REDIS_CAPTCHA_PREFIX, captchaId ) );
        ResponseData verifyData = captchaService.verifyCaptcha( captchaId, captchaSign, captchaResult );
        if (verifyData.isNotSuccess()) {
            return ResponseData.errorCode( MfaResponseCode.CAPTCHA_VERIFY_ERROR );
        }
        return ResponseData.success();
    }


    /**
     * 清除CAPTCHA验证码发送限制。
     *
     * @param ip
     */
    public static boolean clearSendLimit(String ip) {
        return mfaRedisTemplate.delete(RedisKeyUtils.buildKey(REDIS_LIMIT_CAPTCHA_PREFIX, ip));
    }

    /**
     * 获取IP限制列表。
     *
     * @return
     */
    public static Set<String> getSendLimitList() {
        Set<String> keys = new LinkedHashSet<>();
        try (Cursor<String> cursor = mfaRedisTemplate.scan(ScanOptions.scanOptions().match(REDIS_LIMIT_CAPTCHA_PREFIX + ":*").count(1000).build())) {
            cursor.forEachRemaining(key -> {
                keys.add(key.substring(REDIS_LIMIT_CAPTCHA_PREFIX.length() + 1));
            });
        }
        return keys;
    }


}
