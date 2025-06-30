package uw.mfa.helper;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.RestTemplate;
import uw.common.dto.ResponseData;
import uw.common.util.SystemClock;
import uw.mfa.conf.UwMfaProperties;
import uw.mfa.constant.MfaDeviceType;
import uw.mfa.constant.MfaResponseCode;
import uw.mfa.util.RedisKeyUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 设备验证码帮助类。
 */
public class MfaDeviceCodeHelper {

    private static final Logger log = LoggerFactory.getLogger(MfaDeviceCodeHelper.class);
    /**
     * redis 验证码前缀.
     */
    private static final String REDIS_DEVICE_CODE_PREFIX = "deviceCode";
    /**
     * redis 发码限制前缀.
     */
    private static final String REDIS_DEVICE_CODE_LIMIT_PREFIX = "deviceCodeLimit";
    /**
     * redis 码校验前缀.
     */
    private static final String REDIS_DEVICE_CODE_VERIFY_PREFIX = "deviceCodeVerify";
    /**
     * 随机生成器
     */
    private static final Random RANDOM = new Random(SystemClock.now());
    /**
     * ASCII
     */
    private static final char[] CHAR_NUMS = "0123456789".toCharArray();

    /**
     * 设备验证码模版替换名。
     */
    private static final String TEMPLATE_DEVICE_CODE = "$DEVICE_CODE$";

    /**
     * 过期秒数模版替换名
     */
    private static final String TEMPLATE_EXPIRE_MINUTES = "$EXPIRE_MINUTES$";

    /**
     * RPC Client
     */
    private static RestTemplate authRestTemplate;

    /**
     * 配置文件。
     */
    private static UwMfaProperties uwMfaProperties;

    /**
     * mfaRedisTemplate。
     */
    private static RedisTemplate<String, String> mfaRedisTemplate;

    /**
     * kv便捷操作的op。
     */
    private static ValueOperations<String, String> mfaRedisOp;

    public MfaDeviceCodeHelper(UwMfaProperties uwMfaProperties, @Qualifier("mfaRedisTemplate") final RedisTemplate<String, String> mfaRedisTemplate, @Qualifier("authRestTemplate") final RestTemplate authRestTemplate) {
        MfaDeviceCodeHelper.uwMfaProperties = uwMfaProperties;
        MfaDeviceCodeHelper.mfaRedisTemplate = mfaRedisTemplate;
        MfaDeviceCodeHelper.mfaRedisOp = mfaRedisTemplate.opsForValue();
        MfaDeviceCodeHelper.authRestTemplate = authRestTemplate;
    }

    /**
     * 发送设备验证码。
     *
     * @param deviceType 登录类型
     * @param deviceId
     */
    public static ResponseData sendDeviceCode(String userIp, long saasId, int deviceType, String deviceId) {
        return sendDeviceCode(userIp, saasId, deviceType, deviceId, 0);
    }


    /**
     * 发送设备验证码。
     *
     * @param deviceType 登录类型
     * @param deviceId
     */
    public static ResponseData sendDeviceCode(String userIp, long saasId, int deviceType, String deviceId, int codeLen) {
        return sendDeviceCode(userIp, saasId, deviceType, deviceId, codeLen, null, null);
    }

    /**
     * 发送设备验证码。
     *
     * @param deviceType 登录类型
     * @param deviceId
     */
    public static ResponseData sendDeviceCode(String userIp, long saasId, int deviceType, String deviceId, int codeLen, String notifySubject, String notifyContent) {
        if (saasId == -1) {
            return ResponseData.errorCode(MfaResponseCode.DEVICE_CODE_FEE_ERROR, deviceId);
        }
        if (codeLen < 1) {
            codeLen = uwMfaProperties.getDeviceCodeDefaultLength();
        }
        if (StringUtils.isBlank(notifySubject)) {
            notifySubject = uwMfaProperties.getDeviceNotifySubject();
        }
        if (StringUtils.isBlank(notifyContent)) {
            notifyContent = uwMfaProperties.getDeviceNotifyContent();
        }
        //检查验证码发送限制情况
        String redisKey = RedisKeyUtils.buildKey(REDIS_DEVICE_CODE_LIMIT_PREFIX, userIp);
        long sentTimes = Objects.requireNonNullElse(mfaRedisOp.increment(redisKey), 0L);
        if (sentTimes == 1L) {
            mfaRedisTemplate.expire(redisKey, uwMfaProperties.getDeviceCodeSendLimitSeconds(), TimeUnit.SECONDS);
        }
        if (sentTimes >= uwMfaProperties.getDeviceCodeSendLimitTimes()) {
            long ttl = mfaRedisTemplate.getExpire(redisKey, TimeUnit.MINUTES) + 1;
            return ResponseData.errorCode(MfaResponseCode.DEVICE_CODE_SEND_LIMIT_ERROR, userIp, uwMfaProperties.getDeviceCodeSendLimitSeconds() / 60, uwMfaProperties.getDeviceCodeSendLimitTimes(), ttl);
        }
        String deviceCode = genCode(codeLen);
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put(TEMPLATE_DEVICE_CODE, deviceCode);
        paramMap.put(TEMPLATE_EXPIRE_MINUTES, String.valueOf(uwMfaProperties.getDeviceCodeExpiredSeconds() / 60));
        if (deviceType == MfaDeviceType.MOBILE_CODE.getValue()) {
            mfaRedisOp.set(RedisKeyUtils.buildKey(MfaDeviceCodeHelper.REDIS_DEVICE_CODE_PREFIX, deviceType, deviceId), deviceCode, uwMfaProperties.getDeviceCodeExpiredSeconds(), TimeUnit.SECONDS);
            //调用接口发送短信
            ResponseData responseData = MfaDeviceCodeHelper.sendSms(saasId, "mfa", deviceId, deviceId, replaceTemplate(notifyContent, paramMap), paramMap);
            if (responseData.isSuccess()) {
                return ResponseData.success();
            } else {
                return ResponseData.errorCode(MfaResponseCode.DEVICE_CODE_SEND_ERROR);
            }
        } else if (deviceType == MfaDeviceType.EMAIL_CODE.getValue()) {
            mfaRedisOp.set(RedisKeyUtils.buildKey(MfaDeviceCodeHelper.REDIS_DEVICE_CODE_PREFIX, deviceType, deviceId), deviceCode, uwMfaProperties.getDeviceCodeExpiredSeconds(), TimeUnit.SECONDS);
            //调用接口发送邮件
            ResponseData responseData = MfaDeviceCodeHelper.sendEmail(saasId, "mfa", deviceId, deviceId, replaceTemplate(notifySubject, paramMap), replaceTemplate(notifyContent, paramMap), paramMap);
            if (responseData.isSuccess()) {
                return ResponseData.success();
            } else {
                return ResponseData.errorCode(MfaResponseCode.DEVICE_CODE_SEND_ERROR);
            }
        } else {
            return ResponseData.errorCode(MfaResponseCode.DEVICE_TYPE_ERROR);
        }
    }

    /**
     * 校验设备验证码。
     *
     * @return
     */
    public static ResponseData verifyDeviceCode(int deviceType, String deviceId, String deviceCode) {
        if (StringUtils.isBlank(deviceId) || StringUtils.isBlank(deviceCode)) {
            return ResponseData.errorCode(MfaResponseCode.DEVICE_CODE_LOST_ERROR);
        }
        //检查设备码验证限制情况
        ResponseData checkData = checkVerifyErrorLimit(deviceId);
        if (checkData.isNotSuccess()) {
            return checkData;
        }
        String redisCode = mfaRedisOp.getAndDelete(RedisKeyUtils.buildKey(MfaDeviceCodeHelper.REDIS_DEVICE_CODE_PREFIX, deviceType, deviceId));
        if (StringUtils.equals(redisCode, deviceCode)) {
            return ResponseData.success();
        } else {
            incrementVerifyErrorTimes(deviceId);
            return ResponseData.errorCode(MfaResponseCode.DEVICE_CODE_VERIFY_ERROR);
        }
    }

    /**
     * 检查校验错误限制。
     *
     * @param deviceId
     * @return
     */
    public static ResponseData checkVerifyErrorLimit(String deviceId) {
        String redisKey = RedisKeyUtils.buildKey(REDIS_DEVICE_CODE_VERIFY_PREFIX, deviceId);
        String limitInfo = mfaRedisOp.get(RedisKeyUtils.buildKey(REDIS_DEVICE_CODE_VERIFY_PREFIX, deviceId));
        int errorCount = 0;
        if (StringUtils.isNotBlank(limitInfo)) {
            errorCount = Integer.parseInt(limitInfo);
        }
        if (errorCount >= uwMfaProperties.getDeviceCodeVerifyErrorTimes()) {
            long ttl = mfaRedisTemplate.getExpire(redisKey, TimeUnit.MINUTES) + 1;
            return ResponseData.errorCode(MfaResponseCode.DEVICE_CODE_VERIFY_LIMIT_ERROR, deviceId, uwMfaProperties.getDeviceCodeVerifyLimitSeconds() / 60, errorCount, ttl);
        }
        return ResponseData.success();
    }

    /**
     * 递增校验错误次数
     *
     * @param deviceId
     */
    public static boolean incrementVerifyErrorTimes(String deviceId) {
        Long times = mfaRedisOp.increment(RedisKeyUtils.buildKey(REDIS_DEVICE_CODE_VERIFY_PREFIX, deviceId));
        if (times != null && times == 1L) {
            return mfaRedisTemplate.expire(RedisKeyUtils.buildKey(REDIS_DEVICE_CODE_VERIFY_PREFIX, deviceId), uwMfaProperties.getDeviceCodeVerifyLimitSeconds(), TimeUnit.SECONDS);
        }
        return false;
    }

    /**
     * 清除设备验证码发送限制。
     *
     * @param ip
     */
    public static boolean clearSendLimit(String ip) {
        return mfaRedisTemplate.delete(RedisKeyUtils.buildKey(REDIS_DEVICE_CODE_LIMIT_PREFIX, ip));
    }

    /**
     * 清除设备验证码校验限制。
     *
     * @param deviceId
     */
    public static boolean clearVerifyLimit(String deviceId) {
        return mfaRedisTemplate.delete(RedisKeyUtils.buildKey(REDIS_DEVICE_CODE_VERIFY_PREFIX, deviceId));
    }

    /**
     * 获取DeviceCode发送限制列表。
     *
     * @return
     */
    public static Set<String> getSendLimitList() {
        Set<String> keys = new LinkedHashSet<>();
        try (Cursor<String> cursor = mfaRedisTemplate.scan(ScanOptions.scanOptions().match(REDIS_DEVICE_CODE_LIMIT_PREFIX + ":*").count(1000).build())) {
            cursor.forEachRemaining(key -> {
                keys.add(key.substring(REDIS_DEVICE_CODE_LIMIT_PREFIX.length() + 1));
            });
        }
        return keys;
    }

    /**
     * 获取DeviceCode错误限制列表。
     *
     * @return
     */
    public static Set<String> getVerifyErrorList() {
        Set<String> keys = new LinkedHashSet<>();
        try (Cursor<String> cursor = mfaRedisTemplate.scan(ScanOptions.scanOptions().match(REDIS_DEVICE_CODE_VERIFY_PREFIX + ":*").count(1000).build())) {
            cursor.forEachRemaining(key -> {
                keys.add(key.substring(REDIS_DEVICE_CODE_VERIFY_PREFIX.length() + 1));
            });
        }
        return keys;
    }

    /**
     * 发送短信验证码。
     *
     * @param saasId  发送使用的saasId
     * @param refType 关联类型
     * @param mobile  接收验证码的手机号
     * @param content 内容
     * @return
     */
    private static ResponseData sendSms(long saasId, String refType, String refId, String mobile, String content, Map<String, String> paramMap) {
        Map<String, Object> data = new HashMap<>();
        data.put("saasId", saasId);
        data.put("refType", refType);
        data.put("refId", refId);
        data.put("mobile", mobile);
        data.put("content", content);
        data.put("paramMap", paramMap);
        return authRestTemplate.postForObject(uwMfaProperties.getDeviceNotifyMobileApi(), data, ResponseData.class);
    }

    /**
     * 发送邮件验证码。
     *
     * @param saasId    发送使用的saasId
     * @param refType   业务关联类
     * @param toAddress 收信人邮箱
     * @param subject   邮件主题
     * @param content   邮件内容
     * @return
     */
    private static ResponseData sendEmail(long saasId, String refType, String refId, String toAddress, String subject, String content, Map<String, String> paramMap) {
        Map<String, Object> data = new HashMap<>();
        data.put("saasId", saasId);
        data.put("refType", refType);
        data.put("refId", refId);
        data.put("toAddress", toAddress);
        data.put("subject", subject);
        data.put("content", content);
        data.put("paramMap", paramMap);
        //默认邮件为html格式
        data.put("isHtml", 1);
        return authRestTemplate.postForObject(uwMfaProperties.getDeviceNotifyEmailApi(), data, ResponseData.class);
    }

    /**
     * 生成验证码。
     *
     * @param codeLen
     * @return
     */
    private static String genCode(int codeLen) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < codeLen; i++) {
            char c = CHAR_NUMS[RANDOM.nextInt(CHAR_NUMS.length)];
            String ch = String.valueOf(c);
            builder.append(ch);
        }
        return builder.toString();
    }

    /**
     * 替换数据。
     *
     * @param data
     * @param paramMap
     * @return
     */
    private static String replaceTemplate(String data, Map<String, String> paramMap) {
        data = data.replace(TEMPLATE_DEVICE_CODE, paramMap.get(TEMPLATE_DEVICE_CODE));
        data = data.replace(TEMPLATE_EXPIRE_MINUTES, paramMap.get(TEMPLATE_EXPIRE_MINUTES));
        return data;
    }


}
