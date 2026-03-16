package uw.webot.session;

import jakarta.validation.constraints.NotNull;
import uw.webot.core.BrowserConfig;
import uw.webot.proxy.ProxyConfig;
import uw.webot.stealth.StealthConfig;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;

/**
 * 会话配置类。
 * 包含创建 WebotSession 所需的所有配置参数。
 * 支持 Builder 模式构建。
 *
 * @author axeon
 * @since 1.0.0
 */
public class SessionConfig implements Serializable {

    /**
     * 默认会话过期时间。
     */
    public static final Duration DEFAULT_EXPIRE_TIME = Duration.ofDays(30);

    /**
     * 会话过期时间。
     */
    @NotNull
    private Duration expireTime = DEFAULT_EXPIRE_TIME;

    /**
     * 浏览器配置（必需）。
     */
    private BrowserConfig browserConfig = BrowserConfig.builder().build();

    /**
     * 验证码配置（可选）。
     */
    private String captchaConfigKey;

    /**
     * 反检测配置（可选）。
     */
    private String stealthConfigKey;

    /**
     * 代理配置（可选）。
     */
    private String proxyConfigKey;

    /**
     * 会话初始数据。
     */
    private Map<String, Object> extParam;

    public SessionConfig() {
    }

    private SessionConfig(Builder builder) {
        setExpireTime(builder.expireTime);
        setBrowserConfig(builder.browserConfig);
        setCaptchaConfigKey(builder.captchaConfigKey);
        setStealthConfigKey(builder.stealthConfigKey);
        setProxyConfigKey(builder.proxyConfigKey);
        setExtParam(builder.extParam);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(SessionConfig copy) {
        Builder builder = new Builder();
        builder.expireTime = copy.getExpireTime();
        builder.browserConfig = copy.getBrowserConfig();
        builder.captchaConfigKey = copy.getCaptchaConfigKey();
        builder.stealthConfigKey = copy.getStealthConfigKey();
        builder.proxyConfigKey = copy.getProxyConfigKey();
        builder.extParam = copy.getExtParam();
        return builder;
    }

    public Duration getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Duration expireTime) {
        this.expireTime = expireTime;
    }

    public BrowserConfig getBrowserConfig() {
        return browserConfig;
    }

    public void setBrowserConfig(BrowserConfig browserConfig) {
        this.browserConfig = browserConfig;
    }

    public String getCaptchaConfigKey() {
        return captchaConfigKey;
    }

    public void setCaptchaConfigKey(String captchaConfigKey) {
        this.captchaConfigKey = captchaConfigKey;
    }

    public String getStealthConfigKey() {
        return stealthConfigKey;
    }

    public void setStealthConfigKey(String stealthConfigKey) {
        this.stealthConfigKey = stealthConfigKey;
    }

    public String getProxyConfigKey() {
        return proxyConfigKey;
    }

    public void setProxyConfigKey(String proxyConfigKey) {
        this.proxyConfigKey = proxyConfigKey;
    }

    public Map<String, Object> getExtParam() {
        return extParam;
    }

    public void setExtParam(Map<String, Object> extParam) {
        this.extParam = extParam;
    }


    public static final class Builder {
        private Duration expireTime =DEFAULT_EXPIRE_TIME;
        private BrowserConfig browserConfig;
        private String captchaConfigKey;
        private String stealthConfigKey;
        private String proxyConfigKey;
        private Map<String, Object> extParam;

        private Builder() {
        }

        public Builder expireTime(@NotNull Duration expireTime) {
            this.expireTime = expireTime;
            return this;
        }

        public Builder browserConfig(BrowserConfig browserConfig) {
            this.browserConfig = browserConfig;
            return this;
        }

        public Builder captchaConfigKey(String captchaConfigKey) {
            this.captchaConfigKey = captchaConfigKey;
            return this;
        }

        public Builder stealthConfigKey(String stealthConfigKey) {
            this.stealthConfigKey = stealthConfigKey;
            return this;
        }

        public Builder proxyConfigKey(String proxyConfigKey) {
            this.proxyConfigKey = proxyConfigKey;
            return this;
        }

        public Builder extParam(Map<String, Object> extParam) {
            this.extParam = extParam;
            return this;
        }

        public SessionConfig build() {
            return new SessionConfig(this);
        }
    }
}
