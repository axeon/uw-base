package uw.mfa.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "uw.mfa")
public class UwMfaProperties {

    /**
     * ip白名单。在白名单的IP不受登录限制影响。
     */
    private String ipWhiteList = "127.0.0.1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16";

    /**
     * 错误检查过期时间 10min
     */
    private long ipLimitSeconds = 600L;

    /**
     * IP限制报警次数，默认3次才弹验证码。
     */
    private long ipLimitWarnTimes = 3L;

    /**
     * IP限制失败次数，默认10次开始屏蔽。
     */
    private long ipLimitErrorTimes = 10L;

    /**
     * 需要支持的算法，默认全部支持。
     */
    private String captchaStrategies = "StringCaptchaStrategy,CalculateCaptchaStrategy,SlidePuzzleCaptchaStrategy,ClickWordCaptchaStrategy,RotatePuzzleCaptchaStrategy";

    /**
     * 验证码答案过期时间，默认3分钟。
     */
    private long captchaExpiredSeconds = 180L;

    /**
     * CAPTCHA发送限制时间，默认1分钟。
     */
    private long captchaSendLimitSeconds = 60L;

    /**
     * CAPTCHA发送限制次数，默认10次。
     */
    private long captchaSendLimitTimes = 10L;

    /**
     * 设备识别码发送限制时间，默认30分钟。
     */
    private long deviceCodeSendLimitSeconds = 1800L;

    /**
     * 设备识别码发送限制次数，默认10次。
     */
    private long deviceCodeSendLimitTimes = 10L;

    /**
     * 设备识别码过期时间，默认5min。
     */
    private long deviceCodeExpiredSeconds = 300L;

    /**
     * 设备识别码默认长度，默认4。
     */
    private int deviceCodeDefaultLength = 4;

    /**
     * 设备识别码通知标题。
     */
    private String deviceNotifySubject = "验证码";

    /**
     * 设备识别码通知内容。
     */
    private String deviceNotifyContent = "验证码[$DEVICE_CODE$]，$EXPIRE_MINUTES$分钟后过期，如非本人操作，请忽略此信息。";

    /**
     * 设备识别码短信发送API。
     */
    private String deviceNotifyMobileApi = "http://saas-base-app/rpc/saasMsg/sendSms";

    /**
     * 设备识别码EMAIL发送API。
     */
    private String deviceNotifyEmailApi = "http://saas-base-app/rpc/saasMsg/sendMail";

    /**
     * Redis配置
     */
    private RedisProperties redis = new RedisProperties();

    public String getIpWhiteList() {
        return ipWhiteList;
    }

    public void setIpWhiteList(String ipWhiteList) {
        this.ipWhiteList = ipWhiteList;
    }

    public long getIpLimitSeconds() {
        return ipLimitSeconds;
    }

    public void setIpLimitSeconds(long ipLimitSeconds) {
        this.ipLimitSeconds = ipLimitSeconds;
    }

    public long getIpLimitWarnTimes() {
        return ipLimitWarnTimes;
    }

    public void setIpLimitWarnTimes(long ipLimitWarnTimes) {
        this.ipLimitWarnTimes = ipLimitWarnTimes;
    }

    public long getIpLimitErrorTimes() {
        return ipLimitErrorTimes;
    }

    public void setIpLimitErrorTimes(long ipLimitErrorTimes) {
        this.ipLimitErrorTimes = ipLimitErrorTimes;
    }

    public String getCaptchaStrategies() {
        return captchaStrategies;
    }

    public void setCaptchaStrategies(String captchaStrategies) {
        this.captchaStrategies = captchaStrategies;
    }

    public long getCaptchaExpiredSeconds() {
        return captchaExpiredSeconds;
    }

    public void setCaptchaExpiredSeconds(long captchaExpiredSeconds) {
        this.captchaExpiredSeconds = captchaExpiredSeconds;
    }

    public long getCaptchaSendLimitSeconds() {
        return captchaSendLimitSeconds;
    }

    public void setCaptchaSendLimitSeconds(long captchaSendLimitSeconds) {
        this.captchaSendLimitSeconds = captchaSendLimitSeconds;
    }

    public long getCaptchaSendLimitTimes() {
        return captchaSendLimitTimes;
    }

    public void setCaptchaSendLimitTimes(long captchaSendLimitTimes) {
        this.captchaSendLimitTimes = captchaSendLimitTimes;
    }

    public long getDeviceCodeSendLimitSeconds() {
        return deviceCodeSendLimitSeconds;
    }

    public void setDeviceCodeSendLimitSeconds(long deviceCodeSendLimitSeconds) {
        this.deviceCodeSendLimitSeconds = deviceCodeSendLimitSeconds;
    }

    public long getDeviceCodeSendLimitTimes() {
        return deviceCodeSendLimitTimes;
    }

    public void setDeviceCodeSendLimitTimes(long deviceCodeSendLimitTimes) {
        this.deviceCodeSendLimitTimes = deviceCodeSendLimitTimes;
    }

    public long getDeviceCodeExpiredSeconds() {
        return deviceCodeExpiredSeconds;
    }

    public void setDeviceCodeExpiredSeconds(long deviceCodeExpiredSeconds) {
        this.deviceCodeExpiredSeconds = deviceCodeExpiredSeconds;
    }

    public int getDeviceCodeDefaultLength() {
        return deviceCodeDefaultLength;
    }

    public void setDeviceCodeDefaultLength(int deviceCodeDefaultLength) {
        this.deviceCodeDefaultLength = deviceCodeDefaultLength;
    }

    public String getDeviceNotifySubject() {
        return deviceNotifySubject;
    }

    public void setDeviceNotifySubject(String deviceNotifySubject) {
        this.deviceNotifySubject = deviceNotifySubject;
    }

    public String getDeviceNotifyContent() {
        return deviceNotifyContent;
    }

    public void setDeviceNotifyContent(String deviceNotifyContent) {
        this.deviceNotifyContent = deviceNotifyContent;
    }

    public String getDeviceNotifyMobileApi() {
        return deviceNotifyMobileApi;
    }

    public void setDeviceNotifyMobileApi(String deviceNotifyMobileApi) {
        this.deviceNotifyMobileApi = deviceNotifyMobileApi;
    }

    public String getDeviceNotifyEmailApi() {
        return deviceNotifyEmailApi;
    }

    public void setDeviceNotifyEmailApi(String deviceNotifyEmailApi) {
        this.deviceNotifyEmailApi = deviceNotifyEmailApi;
    }

    public RedisProperties getRedis() {
        return redis;
    }

    public void setRedis(RedisProperties redis) {
        this.redis = redis;
    }

    public static class RedisProperties extends org.springframework.boot.autoconfigure.data.redis.RedisProperties {
    }
}
