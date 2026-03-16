package uw.webot;

import uw.webot.core.BrowserConfig;
import uw.webot.proxy.ProxyConfig;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Webot会话对象。
 * 封装会话数据、状态信息和配置信息，提供属性操作方法。
 * <p>
 * 注意：userId由业务方管理，业务方需要自行维护userId与sessionId的关联关系。
 * </p>
 */
public class WebotSession implements Serializable {

    /**
     * 会话ID。
     */
    private String sessionId;

    /**
     * 创建时间。
     */
    private long createTime;

    /**
     * 最后访问时间。
     */
    private volatile long lastAccessTime;

    /**
     * 过期时间。
     */
    private volatile long expirationTime;

    /**
     * 属性存储。
     */
    private Map<String, Object> extParam = new HashMap<>();

    /**
     * 浏览器配置。
     */
    private BrowserConfig browserConfig;

    /**
     * 验证码配置。
     */
    private String captchaConfigKey;

    /**
     * 反检测配置。
     */
    private String stealthConfigKey;

    /**
     * 代理配置。
     */
    private ProxyConfig.ProxyServer proxyServer;

    /**
     * 浏览器存储状态json。
     */
    private String storageStateJson;

    public WebotSession() {
    }

    public WebotSession(String sessionId, long ttlMillis) {
        this.sessionId = sessionId;
        this.createTime = System.currentTimeMillis();
        this.expirationTime = createTime + ttlMillis;
    }


    private WebotSession(Builder builder) {
        setSessionId(builder.sessionId);
        setCreateTime(builder.createTime);
        setLastAccessTime(builder.lastAccessTime);
        setExpirationTime(builder.expirationTime);
        setExtParam(builder.extParam);
        setBrowserConfig(builder.browserConfig);
        setCaptchaConfigKey(builder.captchaConfigKey);
        setStealthConfigKey(builder.stealthConfigKey);
        setProxyServer(builder.proxyServer);
        setStorageStateJson(builder.storageStateJson);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(WebotSession copy) {
        Builder builder = new Builder();
        builder.sessionId = copy.getSessionId();
        builder.createTime = copy.getCreateTime();
        builder.lastAccessTime = copy.getLastAccessTime();
        builder.expirationTime = copy.getExpirationTime();
        builder.extParam = copy.getExtParam();
        builder.browserConfig = copy.getBrowserConfig();
        builder.captchaConfigKey = copy.getCaptchaConfigKey();
        builder.stealthConfigKey = copy.getStealthConfigKey();
        builder.proxyServer = copy.getProxyServer();
        builder.storageStateJson = copy.getStorageStateJson();
        return builder;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public Map<String, Object> getExtParam() {
        return extParam;
    }

    public void setExtParam(Map<String, Object> extParam) {
        this.extParam = extParam;
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

    public ProxyConfig.ProxyServer getProxyServer() {
        return proxyServer;
    }

    public void setProxyServer(ProxyConfig.ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    public String getStorageStateJson() {
        return storageStateJson;
    }

    public void setStorageStateJson(String storageStateJson) {
        this.storageStateJson = storageStateJson;
    }

    public static final class Builder {
        private String sessionId;
        private long createTime;
        private long lastAccessTime;
        private long expirationTime;
        private Map<String, Object> extParam;
        private BrowserConfig browserConfig;
        private String captchaConfigKey;
        private String stealthConfigKey;
        private ProxyConfig.ProxyServer proxyServer;
        private String storageStateJson;

        private Builder() {
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder createTime(long createTime) {
            this.createTime = createTime;
            return this;
        }

        public Builder lastAccessTime(long lastAccessTime) {
            this.lastAccessTime = lastAccessTime;
            return this;
        }

        public Builder expirationTime(long expirationTime) {
            this.expirationTime = expirationTime;
            return this;
        }

        public Builder extParam(Map<String, Object> extParam) {
            this.extParam = extParam;
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

        public Builder proxyServer(ProxyConfig.ProxyServer proxyServer) {
            this.proxyServer = proxyServer;
            return this;
        }

        public Builder storageStateJson(String storageStateJson) {
            this.storageStateJson = storageStateJson;
            return this;
        }

        public WebotSession build() {
            return new WebotSession(this);
        }
    }
}
