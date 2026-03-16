package uw.oauth2.client.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OAuth2客户端配置属性类
 * <p>
 * 用于配置第三方登录平台的OAuth2参数
 * 支持通过application.yml或application.properties进行配置
 * 配置前缀：uw.oauth2.client
 */
@ConfigurationProperties(prefix = "uw.oauth2.client")
public class OAuth2ClientProperties {

    /**
     * 扫码登录链接配置，设置为后端API，必须以“/”结尾。
     */
    private String qrcodeUri = "http://localhost:8080/oauth2/qrcode/";

    /**
     * 重定向URL，用于接收授权结果。一般设置为前端网页。
     */
    private String redirectUri = "http://localhost:8080/ui/oauth2/redirect";

    /**
     * 第三方登录平台配置映射
     * key为平台名称，value为平台配置
     */
    private Map<String, ProviderConfig> providers = new LinkedHashMap<>();

    /**
     * 默认配置
     */
    private static final Map<String, ProviderConfig> DEFAULT_PROVIDERS = new LinkedHashMap<String, ProviderConfig>() {
        {

            // 谷歌默认配置
            put("google", new ProviderConfig("https://accounts.google.com/o/oauth2/v2/auth", "openid email profile", "https://oauth2.googleapis.com/token", "https://www.googleapis.com/oauth2/v3/userinfo"));
            // Apple默认配置
            put("apple", new ProviderConfig("https://appleid.apple.com/auth/authorize", "openid email name", "https://appleid.apple.com/auth/token", null));
            // 微信默认配置
            put("wechat", new ProviderConfig("https://open.weixin.qq.com/connect/qrconnect", "snsapi_login", "https://api.weixin.qq.com/sns/oauth2/access_token", "https://api.weixin.qq.com/sns/userinfo"));
            // 支付宝默认配置
            put("alipay", new ProviderConfig("https://openauth.alipay.com/oauth2/publicAppAuthorize.htm", "auth_user", "https://openapi.alipay.com/gateway.do?method=alipay.system.oauth.token", "https://openapi.alipay.com/gateway.do?method=alipay.user.info.share"));
            // GitHub默认配置
            put("github", new ProviderConfig("https://github.com/login/oauth/authorize", "user:email", "https://github.com/login/oauth/access_token", "https://api.github.com/user"));
        }
    };

    public String getQrcodeUri() {
        return qrcodeUri;
    }

    public void setQrcodeUri(String qrcodeUri) {
        this.qrcodeUri = qrcodeUri;
    }

    /**
     * 获取基础URL
     *
     * @return 基础URL
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * 设置基础URL
     *
     * @param redirectUri 基础URL
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /**
     * 获取第三方登录平台配置映射
     *
     * @return 第三方登录平台配置映射
     */
    public Map<String, ProviderConfig> getProviders() {
        return providers;
    }

    /**
     * 设置第三方登录平台配置映射
     *
     * @param providers 第三方登录平台配置映射
     */
    public void setProviders(Map<String, ProviderConfig> providers) {
        // 如果YAML中提供了配置，则将默认配置与YAML配置合并
        if (providers != null) {
            // 将默认配置添加到YAML配置中（如果YAML中没有该配置）
            for (Map.Entry<String, ProviderConfig> entry : providers.entrySet()) {
                ProviderConfig config = entry.getValue();
                ProviderConfig defaultConfig = DEFAULT_PROVIDERS.get(entry.getKey());
                // 如果默认配置不为空，则将默认配置的属性添加到YAML配置中
                if (defaultConfig != null) {
                    if (config.getAuthUri() == null && defaultConfig.getAuthUri() != null) {
                        config.setAuthUri(defaultConfig.getAuthUri());
                    }
                    if (config.getAuthScope() == null && defaultConfig.getAuthScope() != null) {
                        config.setAuthScope(defaultConfig.getAuthScope());
                    }
                    if (config.getTokenUri() == null && defaultConfig.getTokenUri() != null) {
                        config.setTokenUri(defaultConfig.getTokenUri());
                    }
                    if (config.getUserInfoUri() == null && defaultConfig.getUserInfoUri() != null) {
                        config.setUserInfoUri(defaultConfig.getUserInfoUri());
                    }
                }
            }
            this.providers = providers;
        }
    }

    /**
     * 第三方登录平台配置类
     * <p>
     * 用于配置单个第三方登录平台的OAuth2参数
     * 常用配置项已提供默认值，降低配置工作量
     */
    public static class ProviderConfig {

        /**
         * 客户端ID
         */
        private String clientId;

        /**
         * 客户端密钥
         */
        private String clientSecret;

        /**
         * 授权URL
         */
        private String authUri;

        /**
         * 授权作用域
         */
        private String authScope;

        /**
         * 令牌URL
         */
        private String tokenUri;

        /**
         * 用户信息URL
         */
        private String userInfoUri;

        /**
         * 扩展参数.
         * 可用于存储私钥、公钥等额外配置
         * 私钥key: privateKey
         * 公钥key: publicKey
         */
        private Map<String, String> extParam = new HashMap<>();

        /**
         * 无参构造器
         */
        public ProviderConfig() {
        }

        public ProviderConfig(String authUri, String authScope, String tokenUri, String userInfoUri) {
            this.authUri = authUri;
            this.authScope = authScope;
            this.tokenUri = tokenUri;
            this.userInfoUri = userInfoUri;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getAuthUri() {
            return authUri;
        }

        public void setAuthUri(String authUri) {
            this.authUri = authUri;
        }

        public String getAuthScope() {
            return authScope;
        }

        public void setAuthScope(String authScope) {
            this.authScope = authScope;
        }

        public String getTokenUri() {
            return tokenUri;
        }

        public void setTokenUri(String tokenUri) {
            this.tokenUri = tokenUri;
        }

        public String getUserInfoUri() {
            return userInfoUri;
        }

        public void setUserInfoUri(String userInfoUri) {
            this.userInfoUri = userInfoUri;
        }

        public Map<String, String> getExtParam() {
            return extParam;
        }

        public void setExtParam(Map<String, String> extParam) {
            this.extParam = extParam;
        }
    }

}