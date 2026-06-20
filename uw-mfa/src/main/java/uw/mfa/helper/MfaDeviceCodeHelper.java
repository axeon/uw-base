package uw.mfa.helper;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.RestClient;
import uw.common.response.ResponseData;
import uw.mfa.conf.UwMfaProperties;
import uw.mfa.constant.MfaDeviceType;
import uw.mfa.constant.MfaResponseCode;
import uw.mfa.util.RedisKeyUtils;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 设备验证码帮助类。
 * <p>封装手机短信/邮件验证码的生成、发送（通过RPC调用通知服务）、校验（一次性消费）、</p>
 * <p>发送频率限制与校验错误限制能力。支持自定义验证码长度与通知模板（含 $DEVICE_CODE$ / $EXPIRE_MINUTES$ 占位符）。</p>
 */
public class MfaDeviceCodeHelper {

    private static final Logger log = LoggerFactory.getLogger(MfaDeviceCodeHelper.class);
    /**
     * Redis 设备验证码存储key前缀。
     */
    private static final String REDIS_DEVICE_CODE_PREFIX = "deviceCode";
    /**
     * Redis 发码频率限制key前缀。
     */
    private static final String REDIS_DEVICE_CODE_SEND_PREFIX = "deviceSend";
    /**
     * Redis 校验错误限制key前缀。
     */
    private static final String REDIS_DEVICE_CODE_VERIFY_PREFIX = "deviceVerify";
    /**
     * 安全随机数生成器（用于生成验证码）。
     */
    private static final SecureRandom RANDOM = new SecureRandom();
    /**
     * 验证码字符集（数字0-9）。
     */
    private static final char[] CHAR_NUMS = "0123456789".toCharArray();

    /**
     * 通知模板中验证码占位符。
     */
    private static final String TEMPLATE_DEVICE_CODE = "$DEVICE_CODE$";

    /**
     * 通知模板中过期分钟数占位符。
     */
    private static final String TEMPLATE_EXPIRE_MINUTES = "$EXPIRE_MINUTES$";

    /**
     * 调用通知服务的RPC客户端。
     */
    private static RestClient authRestClient;

    /**
     * MFA配置。
     */
    private static UwMfaProperties uwMfaProperties;

    /**
     * MFA专用RedisTemplate。
     */
    private static RedisTemplate<String, String> mfaRedisTemplate;

    /**
     * kv便捷操作ops。
     */
    private static ValueOperations<String, String> mfaRedisOp;

    /**
     * 构造器（由Spring注入）。
     *
     * @param uwMfaProperties   MFA配置
     * @param mfaRedisTemplate  MFA专用RedisTemplate
     * @param authRestClient    通知服务RPC客户端
     */
    public MfaDeviceCodeHelper(UwMfaProperties uwMfaProperties, @Qualifier("mfaRedisTemplate") final RedisTemplate<String, String> mfaRedisTemplate, @Qualifier("authRestClient") final RestClient authRestClient) {
        MfaDeviceCodeHelper.uwMfaProperties = uwMfaProperties;
        MfaDeviceCodeHelper.mfaRedisTemplate = mfaRedisTemplate;
        MfaDeviceCodeHelper.mfaRedisOp = mfaRedisTemplate.opsForValue();
        MfaDeviceCodeHelper.authRestClient = authRestClient;
    }

    /**
     * 发送设备验证码（默认长度与模板）。
     *
     * @param userIp     用户IP（用于发送频率限制）
     * @param saasId     SaaS ID（-1表示欠费拦截）
     * @param deviceType 设备类型，见 {@link MfaDeviceType}
     * @param deviceId   设备ID（手机号或邮箱）
     * @return 发送结果ResponseData
     */
    public static ResponseData sendDeviceCode(String userIp, long saasId, int deviceType, String deviceId) {
        return sendDeviceCode(userIp, saasId, deviceType, deviceId, 0);
    }


    /**
     * 发送设备验证码（指定验证码长度，默认模板）。
     *
     * @param userIp     用户IP
     * @param saasId     SaaS ID（-1表示欠费拦截）
     * @param deviceType 设备类型，见 {@link MfaDeviceType}
     * @param deviceId   设备ID（手机号或邮箱）
     * @param codeLen    验证码长度，小于1时使用默认长度
     * @return 发送结果ResponseData
     */
    public static ResponseData sendDeviceCode(String userIp, long saasId, int deviceType, String deviceId, int codeLen) {
        return sendDeviceCode(userIp, saasId, deviceType, deviceId, codeLen, null, null);
    }

    /**
     * 发送设备验证码（完整参数）。
     * <p>流程：欠费检查 → 发送频率限制检查 → 生成验证码写入Redis → 调用通知服务 → 失败时清理验证码。</p>
     *
     * @param userIp         用户IP
     * @param saasId         SaaS ID（-1表示欠费拦截）
     * @param deviceType     设备类型，见 {@link MfaDeviceType}（仅支持MOBILE_CODE/EMAIL_CODE）
     * @param deviceId       设备ID（手机号或邮箱）
     * @param codeLen        验证码长度，小于1时使用默认长度
     * @param notifySubject  通知主题（邮件用），为空使用默认值
     * @param notifyContent  通知内容模板，为空使用默认值
     * @return 发送结果ResponseData
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
        String redisKey = RedisKeyUtils.buildKey(REDIS_DEVICE_CODE_SEND_PREFIX, userIp);
        long sentTimes = Objects.requireNonNullElse(mfaRedisOp.increment(redisKey), 0L);
        if (sentTimes == 1L) {
            mfaRedisTemplate.expire(redisKey, uwMfaProperties.getDeviceCodeSendLimitSeconds(), TimeUnit.SECONDS);
        }
        if (sentTimes >= uwMfaProperties.getDeviceCodeSendLimitTimes()) {
            long ttl = mfaRedisTemplate.getExpire(redisKey, TimeUnit.MINUTES) + 1;
            if (ttl < 1) {
                ttl = 1;
            }
            return ResponseData.errorCode(MfaResponseCode.DEVICE_CODE_SEND_LIMIT_ERROR, userIp, uwMfaProperties.getDeviceCodeSendLimitSeconds() / 60, uwMfaProperties.getDeviceCodeSendLimitTimes(), ttl);
        }
        String deviceCode = genCode(codeLen);
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put(TEMPLATE_DEVICE_CODE, deviceCode);
        paramMap.put(TEMPLATE_EXPIRE_MINUTES, String.valueOf(uwMfaProperties.getDeviceCodeExpiredSeconds() / 60));
        String deviceCodeKey = RedisKeyUtils.buildKey(REDIS_DEVICE_CODE_PREFIX, deviceType, deviceId);
        if (deviceType == MfaDeviceType.MOBILE_CODE.getValue()) {
            mfaRedisOp.set(deviceCodeKey, deviceCode, uwMfaProperties.getDeviceCodeExpiredSeconds(), TimeUnit.SECONDS);
            //调用接口发送短信
            ResponseData responseData = MfaDeviceCodeHelper.sendSms(saasId, "mfa", deviceId, deviceId, replaceTemplate(notifyContent, paramMap), paramMap);
            if (responseData.isSuccess()) {
                return ResponseData.success();
            } else {
                //发送失败时清理已写入的验证码, 避免残留且防止用户凭残留码绕过发送限制
                mfaRedisTemplate.delete(deviceCodeKey);
                return ResponseData.errorCode(MfaResponseCode.DEVICE_CODE_SEND_ERROR);
            }
        } else if (deviceType == MfaDeviceType.EMAIL_CODE.getValue()) {
            mfaRedisOp.set(deviceCodeKey, deviceCode, uwMfaProperties.getDeviceCodeExpiredSeconds(), TimeUnit.SECONDS);
            //调用接口发送邮件
            ResponseData responseData = MfaDeviceCodeHelper.sendEmail(saasId, "mfa", deviceId, deviceId, replaceTemplate(notifySubject, paramMap), replaceTemplate(notifyContent, paramMap), paramMap);
            if (responseData.isSuccess()) {
                return ResponseData.success();
            } else {
                //发送失败时清理已写入的验证码, 避免残留且防止用户凭残留码绕过发送限制
                mfaRedisTemplate.delete(deviceCodeKey);
                return ResponseData.errorCode(MfaResponseCode.DEVICE_CODE_SEND_ERROR);
            }
        } else {
            return ResponseData.errorCode(MfaResponseCode.DEVICE_TYPE_ERROR);
        }
    }

    /**
     * 校验设备验证码。
     * <p>先检查校验错误限制，再从Redis一次性取出（getAndDelete）验证码比对（忽略大小写），</p>
     * <p>校验失败递增错误次数。</p>
     *
     * @param deviceType 设备类型
     * @param deviceId   设备ID
     * @param deviceCode 用户输入的验证码
     * @return 校验成功返回success，参数缺失返回DEVICE_CODE_LOST_ERROR，超限返回DEVICE_CODE_VERIFY_LIMIT_ERROR，错误返回DEVICE_CODE_VERIFY_ERROR
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
        if (Strings.CI.equals(redisCode, deviceCode)) {
            return ResponseData.success();
        } else {
            incrementVerifyErrorTimes(deviceId);
            return ResponseData.errorCode(MfaResponseCode.DEVICE_CODE_VERIFY_ERROR);
        }
    }

    /**
     * 检查校验错误限制。
     *
     * @param deviceId 设备ID
     * @return 未超限返回success，超限返回DEVICE_CODE_VERIFY_LIMIT_ERROR
     */
    public static ResponseData checkVerifyErrorLimit(String deviceId) {
        String redisKey = RedisKeyUtils.buildKey(REDIS_DEVICE_CODE_VERIFY_PREFIX, deviceId);
        String limitInfo = mfaRedisOp.get(RedisKeyUtils.buildKey(REDIS_DEVICE_CODE_VERIFY_PREFIX, deviceId));
        int errorCount = 0;
        if (StringUtils.isNotBlank(limitInfo)) {
            try {
                errorCount = Integer.parseInt(limitInfo);
            } catch (NumberFormatException e) {
                errorCount = 0;
            }
        }
        if (errorCount >= uwMfaProperties.getDeviceCodeVerifyErrorTimes()) {
            long ttl = mfaRedisTemplate.getExpire(redisKey, TimeUnit.MINUTES) + 1;
            if (ttl < 1) {
                ttl = 1;
            }
            return ResponseData.errorCode(MfaResponseCode.DEVICE_CODE_VERIFY_LIMIT_ERROR, deviceId, uwMfaProperties.getDeviceCodeVerifyLimitSeconds() / 60, errorCount, ttl);
        }
        return ResponseData.success();
    }

    /**
     * 递增校验错误次数（首次递增设置过期时间）。
     *
     * @param deviceId 设备ID
     * @return 本次是否设置了过期时间（仅首次递增返回true）
     */
    public static boolean incrementVerifyErrorTimes(String deviceId) {
        Long times = mfaRedisOp.increment(RedisKeyUtils.buildKey(REDIS_DEVICE_CODE_VERIFY_PREFIX, deviceId));
        if (times != null && times == 1L) {
            return mfaRedisTemplate.expire(RedisKeyUtils.buildKey(REDIS_DEVICE_CODE_VERIFY_PREFIX, deviceId), uwMfaProperties.getDeviceCodeVerifyLimitSeconds(), TimeUnit.SECONDS);
        }
        return false;
    }

    /**
     * 清除设备验证码发送频率限制。
     *
     * @param ip 用户IP
     * @return 删除成功返回true
     */
    public static boolean clearSendLimit(String ip) {
        return mfaRedisTemplate.delete(RedisKeyUtils.buildKey(REDIS_DEVICE_CODE_SEND_PREFIX, ip));
    }

    /**
     * 清除设备验证码校验错误限制。
     *
     * @param deviceId 设备ID
     * @return 删除成功返回true
     */
    public static boolean clearVerifyLimit(String deviceId) {
        return mfaRedisTemplate.delete(RedisKeyUtils.buildKey(REDIS_DEVICE_CODE_VERIFY_PREFIX, deviceId));
    }

    /**
     * 获取设备验证码发送限制IP列表（扫描deviceSend:* key并去除前缀）。
     *
     * @return IP集合
     */
    public static Set<String> getSendLimitList() {
        Set<String> keys = new LinkedHashSet<>();
        try (Cursor<String> cursor = mfaRedisTemplate.scan(ScanOptions.scanOptions().match(REDIS_DEVICE_CODE_SEND_PREFIX + ":*").count(1000).build())) {
            cursor.forEachRemaining(key -> {
                keys.add(key.substring(REDIS_DEVICE_CODE_SEND_PREFIX.length() + 1));
            });
        }
        return keys;
    }

    /**
     * 获取设备验证码校验错误限制列表（扫描deviceVerify:* key并去除前缀）。
     *
     * @return 设备ID集合
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
     * 通过RPC调用通知服务发送短信验证码。
     *
     * @param saasId    发送使用的saasId
     * @param refType   业务关联类型
     * @param refId     业务关联ID
     * @param mobile    接收验证码的手机号
     * @param content   已替换占位符的短信内容
     * @param paramMap  模板参数（含验证码与过期分钟数）
     * @return 通知服务响应
     */
    private static ResponseData sendSms(long saasId, String refType, String refId, String mobile, String content, Map<String, String> paramMap) {
        Map<String, Object> data = new HashMap<>();
        data.put("saasId", saasId);
        data.put("refType", refType);
        data.put("refId", refId);
        data.put("mobile", mobile);
        data.put("content", content);
        data.put("paramMap", paramMap);
        return authRestClient.post()
                .uri(uwMfaProperties.getDeviceNotifyMobileApi())
                .body(data)
                .retrieve()
                .body(ResponseData.class);
    }

    /**
     * 通过RPC调用通知服务发送邮件验证码。
     *
     * @param saasId    发送使用的saasId
     * @param refType   业务关联类型
     * @param refId     业务关联ID
     * @param toAddress 收信人邮箱
     * @param subject   已替换占位符的邮件主题
     * @param content   已替换占位符的邮件内容
     * @param paramMap  模板参数（含验证码与过期分钟数）
     * @return 通知服务响应
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
        return authRestClient.post()
                .uri(uwMfaProperties.getDeviceNotifyEmailApi())
                .body(data)
                .retrieve()
                .body(ResponseData.class);
    }

    /**
     * 使用安全随机数生成指定长度的纯数字验证码。
     *
     * @param codeLen 验证码长度
     * @return 数字验证码字符串
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
     * 替换通知模板中的验证码与过期分钟数占位符。
     *
     * @param data     原始模板内容
     * @param paramMap 参数Map（含 $DEVICE_CODE$ / $EXPIRE_MINUTES$）
     * @return 替换后的内容
     */
    private static String replaceTemplate(String data, Map<String, String> paramMap) {
        String code = paramMap.getOrDefault(TEMPLATE_DEVICE_CODE, "");
        String minutes = paramMap.getOrDefault(TEMPLATE_EXPIRE_MINUTES, "");
        data = data.replace(TEMPLATE_DEVICE_CODE, code);
        data = data.replace(TEMPLATE_EXPIRE_MINUTES, minutes);
        return data;
    }


}
