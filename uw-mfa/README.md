[TOC]

# 项目说明

uw-mfa Multi-Factor Authentication (MFA) 一个融合了IP限制，CAPTCHA，设备码的多重认证库。

# 基础配置

## maven引用

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-mfa</artifactId>
    <version>${uw-mfa.version}</version>
</dependency>
```
## 配置项
```java
/**
 * ip白名单。在白名单的IP不受登录限制影响。
 */
private String ipWhiteList = "127.0.0.1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,::1/128,fe80::/10,FC00::/7";
/**
 * 错误检查过期时间 10min
 */
private long ipLimitSeconds = 600L;

/**
 * IP最大失败次数
 */
private long ipLimitTimes = 10L;

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
private String deviceNotifySubject = "验证码";

/**
 * 设备识别码通知内容。
 */
private String deviceNotifyContent =  "验证码[$DEVICE_CODE$]，$EXPIRE_SECONDS$分钟后过期，如非本人操作，请忽略此信息。";

/**
 * 设备识别码短信发送API。
 */
private String deviceNotifyMobileApi = "http://saas-market-app/rpc/msg/sms";

/**
 * 设备识别码EMAIL发送API。
 */
private String deviceNotifyEmailApi = "http://saas-market-app/rpc/msg/mail";

```

# 使用参考
MfaFusionHelper MFA融合帮助类。
```java
/**
 * 一个融合MFA帮助类。
 * 输出和检测Captcha，DeviceCode前后对IP限制进行了检查。
 */
public class MfaFusionHelper {

    /**
     * 检查IP是否在白名单中。
     *
     * @param ip
     * @return
     */
    public static boolean checkIpWhiteList(String ip);

    /**
     * 检查IP错误限制。
     * 如果有报错记录，就开始输出warn。
     * 当超过报错限制，则输出error。
     *
     * @param ip
     * @return
     */
    public static ResponseData checkIpErrorLimit(String ip);

    /**
     * 递增错误次数。
     * 当调用程序判定出错需要限制用户行为时，则需要调用此方法。
     *
     * @param ip
     */
    public static void incrementIpErrorTimes(String ip, String remark);

    /**
     * 清除IP错误限制。
     *
     * @param ip
     */
    public static void clearIpErrorLimit(String ip);

    /**
     * 统计限制信息条数，此信息直接从redis输出。
     *
     * @return
     */
    public static long countLimitInfo();

    /**
     * 生成captcha。
     * 此方法内会对IP限制进行检测，如果warn才会输出captcha，否则不会输出。
     *
     * @param captchaId
     */
    public static ResponseData<CaptchaQuestion> generateCaptcha(String userIp, String captchaId);

    /**
     * 验证captcha。
     * 调用此方法需要特别注意，不能直接外部暴露，否则会被重试攻击。
     *
     * @param captchaId
     * @return
     */
    public static ResponseData verifyCaptcha(String userIp, String captchaId, String captchaSign);

    /**
     * 发送验证码。
     * 此方法内会对IP限制进行检测，如果warn会进行captcha检测，否则不会检测。
     *
     * @param deviceType 登录类型
     * @param deviceId
     */
    public static ResponseData sendDeviceCode(String userIp, long saasId, int deviceType, String deviceId, String captchaId, String captchaSign);

    /**
     * 发送验证码。
     * 此方法内会对IP限制进行检测，如果warn会进行captcha检测，否则不会检测。
     *
     * @param deviceType 登录类型
     * @param deviceId
     */
    public static ResponseData sendDeviceCode(String userIp, long saasId, int deviceType, String deviceId, String captchaId, String captchaSign, int codeLen);

    /**
     * 发送验证码。
     * 此方法内会对IP限制进行检测，如果warn会进行captcha检测，否则不会检测。
     *
     * @param deviceType 登录类型
     * @param deviceId
     */
    public static ResponseData sendDeviceCode(String userIp, long saasId, int deviceType, String deviceId, String captchaId, String captchaSign, int codeLen,
                                              String notifySubject, String notifyContent);


    /**
     * 检查设备识别码。
     * 如果识别错误，则直接递增IP错误。
     *
     * @return
     */
    public static ResponseData verifyDeviceCode(String userIp, int deviceType, String deviceId, String deviceCode);

```



