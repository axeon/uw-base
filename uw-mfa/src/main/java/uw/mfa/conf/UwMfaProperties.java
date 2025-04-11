package uw.mfa.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import uw.mfa.constant.HmacAlgorithm;

@ConfigurationProperties(prefix = "uw.mfa")
public class UwMfaProperties {

    /**
     * ip白名单。在白名单的IP不受登录限制影响。
     */
    private String ipWhiteList = "127.0.0.1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,::1/128,fe80::/10,FC00::/7";

    /**
     * 错误检查过期时间 10min
     */
    private long ipLimitSeconds = 600L;

    /**
     * IP限制报警次数，默认3次才弹Captcha。
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
     * Captcha答案过期时间，默认3分钟。
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
    private String deviceNotifySubject = "设备识别码";

    /**
     * 设备识别码通知内容。
     */
    private String deviceNotifyContent = "设备识别码[$DEVICE_CODE$]，$EXPIRE_MINUTES$分钟后过期，如非本人操作，请忽略此信息。";

    /**
     * 设备识别码短信发送API。
     */
    private String deviceNotifyMobileApi = "http://saas-base-app/rpc/saasMsg/sendSms";

    /**
     * 设备识别码EMAIL发送API。
     */
    private String deviceNotifyEmailApi = "http://saas-base-app/rpc/saasMsg/sendMail";

    /**
     * TOTP算法，默认SHA256。
     */
    private HmacAlgorithm totpAlgorithm = HmacAlgorithm.SHA256;

    /**
     * TOTP密钥默认长度，默认32位。
     */
    private int totpSecretLength = 32;

    /**
     * TOTP验证码默认长度，默认6位。
     */
    private int totpCodeLength = 6;

    /**
     * TOTP验证码时间窗口，默认30秒。
     */
    private int totpTimePeriod = 30;

    /**
     * TOTP验证时间窗口偏移量
     */
    private int totpTimePeriodDiscrepancy = 3;

    /**
     * TOTP是否生成二维码，默认生成。
     */
    private boolean totpGenQr = true;

    /**
     * TOTP二维码尺寸，默认350px。
     */
    private int totpQrSize = 350;

    /**
     * TOTP签发人，默认uw-mfa。
     */
    private String totpIssuer = "uw-mfa" ;

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

    public HmacAlgorithm getTotpAlgorithm() {
        return totpAlgorithm;
    }

    public void setTotpAlgorithm(HmacAlgorithm totpAlgorithm) {
        this.totpAlgorithm = totpAlgorithm;
    }

    public int getTotpSecretLength() {
        return totpSecretLength;
    }

    public void setTotpSecretLength(int totpSecretLength) {
        this.totpSecretLength = totpSecretLength;
    }

    public int getTotpCodeLength() {
        return totpCodeLength;
    }

    public void setTotpCodeLength(int totpCodeLength) {
        this.totpCodeLength = totpCodeLength;
    }

    public int getTotpTimePeriod() {
        return totpTimePeriod;
    }

    public void setTotpTimePeriod(int totpTimePeriod) {
        this.totpTimePeriod = totpTimePeriod;
    }

    public int getTotpTimePeriodDiscrepancy() {
        return totpTimePeriodDiscrepancy;
    }

    public void setTotpTimePeriodDiscrepancy(int totpTimePeriodDiscrepancy) {
        this.totpTimePeriodDiscrepancy = totpTimePeriodDiscrepancy;
    }

    public boolean isTotpGenQr() {
        return totpGenQr;
    }

    public void setTotpGenQr(boolean totpGenQr) {
        this.totpGenQr = totpGenQr;
    }

    public String getTotpIssuer() {
        return totpIssuer;
    }

    public void setTotpIssuer(String totpIssuer) {
        this.totpIssuer = totpIssuer;
    }

    public int getTotpQrSize() {
        return totpQrSize;
    }

    public void setTotpQrSize(int totpQrSize) {
        this.totpQrSize = totpQrSize;
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
