package uw.auth.service.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * auth-server配置。
 *
 * @author axeon
 */
@ConfigurationProperties(prefix = "uw.auth.service")
public class AuthServiceProperties {

    /**
     * 是否启用网关。
     */
    private boolean enableGateway = true;

    /**
     * 认证中心服务地址。
     */
    private String authCenterHost = "http://uw-auth-center";

    /**
     * 认证保护路径。 即受保护的资源，多个请用英文逗号分隔。
     */
    private String authProtectedPaths = "/*";

    /**
     * IP保护路径。 即受保护的资源，多个请用英文逗号分隔。
     */
    private String ipProtectedPaths = "/rpc/*,/agent/*";

    /**
     * IP白名单列表。
     */
    private String ipWhiteList = "127.0.0.1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,::1/128,fe80::/10,FC00::/7";

    /**
     * 应用Id,统一由auth-center分配，一般不需要指派。
     */
    private long appId;

    /**
     * 应用名称
     */
    @Value("${project.name}")
    private String appName;

    /**
     * 应用显示名称
     */
    private String appLabel;

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
     * 跨域配置
     */
    private CORSProperties cors = new CORSProperties();

    /**
     * 用户缓存大小配置
     */
    private Map<Integer, Long> tokenCache = new HashMap<>();

    public boolean isEnableGateway() {
        return enableGateway;
    }

    public void setEnableGateway(boolean enableGateway) {
        this.enableGateway = enableGateway;
    }

    public String getAuthCenterHost() {
        return authCenterHost;
    }

    public void setAuthCenterHost(String authCenterHost) {
        this.authCenterHost = authCenterHost;
    }

    public String getAuthProtectedPaths() {
        return authProtectedPaths;
    }

    public void setAuthProtectedPaths(String authProtectedPaths) {
        this.authProtectedPaths = authProtectedPaths;
    }

    public String getIpProtectedPaths() {
        return ipProtectedPaths;
    }

    public void setIpProtectedPaths(String ipProtectedPaths) {
        this.ipProtectedPaths = ipProtectedPaths;
    }

    public String getIpWhiteList() {
        return ipWhiteList;
    }

    public void setIpWhiteList(String ipWhiteList) {
        this.ipWhiteList = ipWhiteList;
    }

    public long getAppId() {
        return appId;
    }

    public void setAppId(long appId) {
        this.appId = appId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppLabel() {
        return appLabel;
    }

    public void setAppLabel(String appLabel) {
        this.appLabel = appLabel;
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

    public CORSProperties getCors() {
        return cors;
    }

    public void setCors(CORSProperties cors) {
        this.cors = cors;
    }

    public Map<Integer, Long> getTokenCache() {
        return tokenCache;
    }

    public void setTokenCache(Map<Integer, Long> tokenCache) {
        this.tokenCache = tokenCache;
    }

    public static class CORSProperties {
        /**
         * CORS映射路径
         */
        private String mapping = "/**";
        /**
         * 最大时长
         */
        private Long maxAge = 3600L;
        /**
         * 是否允许认证信息
         */
        private Boolean allowCredentials = true;
        /**
         * 允许的源
         * springBoot新版本不允许设置为 "*"
         */
        private String allowedOrigins = "http://localhost:8080, http://127.0.0.1:8080";
        /**
         * 允许的方法
         */
        private String allowedMethods = "GET, POST, OPTIONS, PUT, PATCH, DELETE";
        /**
         * 允许的头信息
         */
        private String allowedHeaders = "Origin, No-Cache, X-Requested-With, If-Modified-Since, Pragma, Last-Modified, Cache-Control, Expires, Content-Type, X-E4M-With, " +
                "Language, Authorization, Accept";
        /**
         * 允许的源
         */
        private String allowedOriginPattern = "*";

        public String getMapping() {
            return mapping;
        }

        public void setMapping(String mapping) {
            this.mapping = mapping;
        }

        public Long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(Long maxAge) {
            this.maxAge = maxAge;
        }

        public Boolean getAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(Boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public String getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(String allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public String getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(String allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public String getAllowedOriginPattern() {
            return allowedOriginPattern;
        }

        public void setAllowedOriginPattern(String allowedOriginPattern) {
            this.allowedOriginPattern = allowedOriginPattern;
        }
    }

}
