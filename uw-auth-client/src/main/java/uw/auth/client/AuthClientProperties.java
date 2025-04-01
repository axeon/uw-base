package uw.auth.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * REST认证相关配置
 */
@ConfigurationProperties(prefix = "uw.auth.client")
public class AuthClientProperties {

    /**
     * 是否开启Spring Cloud Load Balance支持
     */
    private boolean enableSpringCloud = true;

    /**
     * 应用名称
     */
    @Value("${project.name}")
    private String appName;

    /**
     * 应用版本
     */
    @Value("${project.version}")
    private String appVersion;

    /**
     * app主机地址。
     */
    @Value("${spring.cloud.nacos.discovery.ip:}")
    private String appHost;

    /**
     * app端口号。
     */
    @Value("${server.port:8080}")
    private int appPort;

    /**
     * 认证服务器地址
     */
    private String authCenterHost = "http://uw-auth-center";

    /**
     * 登录入口
     */
    private String loginEntryPoint = "/auth/login";

    /**
     * 刷新token入口
     */
    private String refreshEntryPoint = "/auth/refreshToken";

    /**
     * 用户名
     */
    private String loginId;

    /**
     * 密码
     */
    private String loginPass;

    /**
     * 密码
     */
    private String loginSecret ;

    /**
     * 用户类型，默认为RPC用户
     */
    private int userType = 1;

    /**
     * SaasId.
     */
    private long saasId = 0;

    /**
     * 连接池配置
     */
    private HttpPool httpPool = new HttpPool();


    /**
     * HTTP连接池配置，时间单位为毫秒
     */
    public static class HttpPool {
        /**
         * 连接池最大值
         */
        private int maxTotal = 100000;

        /**
         * 每台主机最大连接数
         */
        private int defaultMaxPerRoute = 10000;

        /**
         * 连接超时时间
         */
        private int connectTimeout = 30_000;

        /**
         * 请求超时时间
         */
        private int connectionRequestTimeout = 180_000;

        /**
         * Socket超时时间
         */
        private int socketTimeout = 30_000;

        /**
         * HTTP头不存在Keep-Alive时的keep alive时间
         */
        private int keepAliveTimeIfNotPresent = 0;

        public int getMaxTotal() {
            return maxTotal;
        }

        public void setMaxTotal(int maxTotal) {
            this.maxTotal = maxTotal;
        }

        public int getDefaultMaxPerRoute() {
            return defaultMaxPerRoute;
        }

        public void setDefaultMaxPerRoute(int defaultMaxPerRoute) {
            this.defaultMaxPerRoute = defaultMaxPerRoute;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getConnectionRequestTimeout() {
            return connectionRequestTimeout;
        }

        public void setConnectionRequestTimeout(int connectionRequestTimeout) {
            this.connectionRequestTimeout = connectionRequestTimeout;
        }

        public int getSocketTimeout() {
            return socketTimeout;
        }

        public void setSocketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
        }

        public int getKeepAliveTimeIfNotPresent() {
            return keepAliveTimeIfNotPresent;
        }

        public void setKeepAliveTimeIfNotPresent(int keepAliveTimeIfNotPresent) {
            this.keepAliveTimeIfNotPresent = keepAliveTimeIfNotPresent;
        }
    }

    public boolean isEnableSpringCloud() {
        return enableSpringCloud;
    }

    public void setEnableSpringCloud(boolean enableSpringCloud) {
        this.enableSpringCloud = enableSpringCloud;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getAppHost() {
        return appHost;
    }

    public void setAppHost(String appHost) {
        this.appHost = appHost;
    }

    public int getAppPort() {
        return appPort;
    }

    public void setAppPort(int appPort) {
        this.appPort = appPort;
    }

    public String getAuthCenterHost() {
        return authCenterHost;
    }

    public void setAuthCenterHost(String authCenterHost) {
        this.authCenterHost = authCenterHost;
    }

    public String getLoginEntryPoint() {
        return loginEntryPoint;
    }

    public void setLoginEntryPoint(String loginEntryPoint) {
        this.loginEntryPoint = loginEntryPoint;
    }

    public String getRefreshEntryPoint() {
        return refreshEntryPoint;
    }

    public void setRefreshEntryPoint(String refreshEntryPoint) {
        this.refreshEntryPoint = refreshEntryPoint;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getLoginPass() {
        return loginPass;
    }

    public void setLoginPass(String loginPass) {
        this.loginPass = loginPass;
    }

    public String getLoginSecret() {
        return loginSecret;
    }

    public void setLoginSecret(String loginSecret) {
        this.loginSecret = loginSecret;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public long getSaasId() {
        return saasId;
    }

    public void setSaasId(long saasId) {
        this.saasId = saasId;
    }

    public HttpPool getHttpPool() {
        return httpPool;
    }

    public void setHttpPool(HttpPool httpPool) {
        this.httpPool = httpPool;
    }
}
