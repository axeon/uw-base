package uw.webot.captcha;

import java.time.Duration;

/**
 * 验证码配置类。
 * <p>
 * 配置验证码服务类型、API密钥、重试次数等参数。
 * </p>
 *
 * @author axeon
 * @since 1.0.0
 */
public class CaptchaConfig {

    /**
     * 验证码服务类型枚举。
     * <p>
     * 支持 2Captcha、Capsolver、OCR 三种服务类型。
     * </p>
     */
    public enum ServiceType {
        TWOCAPTCHA,
        CAPSOLVER,
        OCR
    }

    /**
     * 默认服务类型。
     */
    public static final ServiceType DEFAULT_SERVICE_TYPE = ServiceType.OCR;

    /**
     * 服务类型。
     */
    private ServiceType serviceType = DEFAULT_SERVICE_TYPE;

    /**
     * 服务URL。
     */
    private String serviceUrl;

    /**
     * API密钥。
     */
    private String apiKey;

    /**
     * 最大重试次数。
     */
    private int maxRetries = 3;

    /**
     * 超时时间。
     */
    private Duration maxTimeout = Duration.ofMinutes(3);

    public CaptchaConfig() {
    }

    public CaptchaConfig(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getMaxTimeout() {
        return maxTimeout;
    }

    public void setMaxTimeout(Duration maxTimeout) {
        this.maxTimeout = maxTimeout;
    }
}
