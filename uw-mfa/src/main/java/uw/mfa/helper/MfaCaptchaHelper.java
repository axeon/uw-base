package uw.mfa.helper;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import uw.common.dto.ResponseData;
import uw.mfa.captcha.CaptchaService;
import uw.mfa.captcha.vo.CaptchaData;
import uw.mfa.captcha.vo.CaptchaQuestion;
import uw.mfa.conf.UwMfaProperties;
import uw.mfa.constant.MfaErrorType;
import uw.mfa.util.RedisKeyUtils;

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
        Long sentTimes = mfaRedisOp.increment( redisKey );
        if (sentTimes == 1L) {
            mfaRedisTemplate.expire( redisKey, uwMfaProperties.getCaptchaSendLimitSeconds(), TimeUnit.SECONDS );
        }
        if (sentTimes >= uwMfaProperties.getCaptchaSendLimitTimes()) {
            return ResponseData.errorCode( MfaErrorType.CAPTCHA_SEND_LIMIT.getCode(), String.format( MfaErrorType.CAPTCHA_SEND_LIMIT.getMessage(), userIp,
                    (uwMfaProperties.getCaptchaSendLimitSeconds() / 60), uwMfaProperties.getCaptchaSendLimitTimes(), (uwMfaProperties.getCaptchaSendLimitSeconds() / 60) ) );
        }
        ResponseData<CaptchaData> captchaResData = captchaService.generateCaptcha( captchaId );
        if (captchaResData.isNotSuccess()) {
            return ResponseData.errorCode( MfaErrorType.CAPTCHA_GENERATE_FAIL.getCode(), MfaErrorType.CAPTCHA_GENERATE_FAIL.getMessage() + captchaResData.getMsg() );
        }
        CaptchaData captchaData = captchaResData.getData();
        CaptchaQuestion captchaQuestion = captchaData.getCaptchaQuestion();
        //设置有效期
        captchaQuestion.setCaptchaTtl( uwMfaProperties.getCaptchaExpiredSeconds() );
        mfaRedisOp.set( RedisKeyUtils.buildKey( REDIS_CAPTCHA_PREFIX, captchaQuestion.getCaptchaId() ), captchaData.getCaptchaResult(),
                uwMfaProperties.getCaptchaExpiredSeconds(), TimeUnit.SECONDS );
        return ResponseData.success( captchaQuestion);
    }

    /**
     * 验证captcha。
     *
     * @param captchaId
     * @return
     */
    public static ResponseData verifyCaptcha(String userIp, String captchaId, String captchaSign) {
        if (StringUtils.isBlank( captchaId ) || StringUtils.isBlank( captchaSign )) {
            return ResponseData.errorCode( MfaErrorType.CAPTCHA_LOST.getCode(), MfaErrorType.CAPTCHA_LOST.getMessage() );
        }
        String captchaResult = mfaRedisOp.getAndDelete( RedisKeyUtils.buildKey( REDIS_CAPTCHA_PREFIX, captchaId ) );
        ResponseData verifyData = captchaService.verifyCaptcha( captchaId, captchaSign, captchaResult );
        if (verifyData.isNotSuccess()) {
            return ResponseData.errorCode( MfaErrorType.CAPTCHA_VERIFY_FAIL.getCode(), MfaErrorType.CAPTCHA_VERIFY_FAIL.getMessage() + verifyData.getMsg() );
        }
        return ResponseData.success();
    }


}
