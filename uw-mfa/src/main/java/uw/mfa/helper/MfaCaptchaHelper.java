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
 * Captcha帮助类。
 * <p>封装Captcha的生成（含发送频率限制）、校验（一次性消费）、发送限制管理能力，</p>
 * <p>正确答案存储于Redis，校验时通过getAndDelete实现一次性消费防止重放。</p>
 */
public class MfaCaptchaHelper {

    private static final Logger log = LoggerFactory.getLogger(MfaCaptchaHelper.class);

    /**
     * Redis Captcha答案存储key前缀。
     */
    private static final String REDIS_CAPTCHA_PREFIX = "captcha";

    /**
     * Redis Captcha发送频率限制key前缀。
     */
    private static final String REDIS_CAPTCHA_LIMIT_PREFIX = "captchaLimit";

    /**
     * MFA专用RedisTemplate。
     */
    private static RedisTemplate<String, String> mfaRedisTemplate;

    /**
     * kv便捷操作ops。
     */
    private static ValueOperations<String, String> mfaRedisOp;

    /**
     * Captcha服务（类加载时初始化为全策略，构造器注入时按配置重置）。
     */
    private static CaptchaService captchaService = new CaptchaService();
    /**
     * MFA配置。
     */
    private static UwMfaProperties uwMfaProperties;

    /**
     * 构造器（由Spring注入，按配置初始化Captcha策略）。
     *
     * @param uwMfaProperties   MFA配置
     * @param mfaRedisTemplate  MFA专用RedisTemplate
     */
    public MfaCaptchaHelper(UwMfaProperties uwMfaProperties, @Qualifier("mfaRedisTemplate") final RedisTemplate<String, String> mfaRedisTemplate) {
        MfaCaptchaHelper.uwMfaProperties = uwMfaProperties;
        MfaCaptchaHelper.mfaRedisTemplate = mfaRedisTemplate;
        MfaCaptchaHelper.mfaRedisOp = mfaRedisTemplate.opsForValue();
        MfaCaptchaHelper.captchaService = new CaptchaService(uwMfaProperties.getCaptchaStrategies());
    }

    /**
     * 生成Captcha。
     * <p>先检查发送频率限制，未超限则生成问题并存储答案到Redis（带过期时间）。</p>
     *
     * @param userIp    用户IP（用于发送频率限制）
     * @param captchaId 前端传入的captchaId，为空或非32位时自动生成
     * @return 成功返回CaptchaQuestion，超限返回CAPTCHA_SEND_LIMIT_ERROR
     */
    public static ResponseData<CaptchaQuestion> generateCaptcha(String userIp, String captchaId) {
        //检查发送限制情况
        String redisKey = RedisKeyUtils.buildKey(REDIS_CAPTCHA_LIMIT_PREFIX, userIp);
        long sentTimes = Objects.requireNonNullElse(mfaRedisOp.increment(redisKey), 0L);
        if (sentTimes == 1L) {
            mfaRedisTemplate.expire(redisKey, uwMfaProperties.getCaptchaSendLimitSeconds(), TimeUnit.SECONDS);
        }
        if (sentTimes >= uwMfaProperties.getCaptchaSendLimitTimes()) {
            long ttl = mfaRedisTemplate.getExpire(redisKey, TimeUnit.MINUTES) + 1;
            if (ttl < 1) {
                ttl = 1;
            }
            return ResponseData.errorCode(MfaResponseCode.CAPTCHA_SEND_LIMIT_ERROR, userIp, uwMfaProperties.getCaptchaSendLimitSeconds() / 60, uwMfaProperties.getCaptchaSendLimitTimes(), ttl);
        }
        ResponseData<CaptchaData> captchaResData = captchaService.generateCaptcha(captchaId);
        if (captchaResData.isNotSuccess()) {
            return ResponseData.errorCode(MfaResponseCode.CAPTCHA_GENERATE_ERROR);
        }
        CaptchaData captchaData = captchaResData.getData();
        CaptchaQuestion captchaQuestion = captchaData.getCaptchaQuestion();
        //设置有效期
        captchaQuestion.setCaptchaTTL(uwMfaProperties.getCaptchaExpiredSeconds());
        mfaRedisOp.set(RedisKeyUtils.buildKey(REDIS_CAPTCHA_PREFIX, captchaQuestion.getCaptchaId()), captchaData.getCaptchaResult(), uwMfaProperties.getCaptchaExpiredSeconds(), TimeUnit.SECONDS);
        return ResponseData.success(captchaQuestion);
    }

    /**
     * 验证Captcha。
     * <p>从Redis一次性取出（getAndDelete）正确答案进行校验，防止重放攻击。</p>
     *
     * @param captchaId   captchaId
     * @param captchaSign 前端提交的加密应答
     * @return 校验成功返回success，参数缺失返回CAPTCHA_LOST_ERROR，校验失败返回CAPTCHA_VERIFY_ERROR
     */
    public static ResponseData verifyCaptcha(String captchaId, String captchaSign) {
        if (StringUtils.isBlank(captchaId) || StringUtils.isBlank(captchaSign)) {
            return ResponseData.errorCode(MfaResponseCode.CAPTCHA_LOST_ERROR);
        }
        String captchaResult = mfaRedisOp.getAndDelete(RedisKeyUtils.buildKey(REDIS_CAPTCHA_PREFIX, captchaId));
        ResponseData verifyData = captchaService.verifyCaptcha(captchaId, captchaSign, captchaResult);
        if (verifyData.isNotSuccess()) {
            return ResponseData.errorCode(MfaResponseCode.CAPTCHA_VERIFY_ERROR);
        }
        return ResponseData.success();
    }


    /**
     * 清除Captcha发送频率限制。
     *
     * @param ip 用户IP
     * @return 删除成功返回true
     */
    public static boolean clearSendLimit(String ip) {
        return mfaRedisTemplate.delete(RedisKeyUtils.buildKey(REDIS_CAPTCHA_LIMIT_PREFIX, ip));
    }

    /**
     * 获取Captcha发送限制IP列表（扫描captchaLimit:* key并去除前缀）。
     *
     * @return IP集合
     */
    public static Set<String> getSendLimitList() {
        Set<String> keys = new LinkedHashSet<>();
        try (Cursor<String> cursor = mfaRedisTemplate.scan(ScanOptions.scanOptions().match(REDIS_CAPTCHA_LIMIT_PREFIX + ":*").count(1000).build())) {
            cursor.forEachRemaining(key -> {
                keys.add(key.substring(REDIS_CAPTCHA_LIMIT_PREFIX.length() + 1));
            });
        }
        return keys;
    }


}
