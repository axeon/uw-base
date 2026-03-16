package uw.oauth2.client;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.dto.ResponseData;
import uw.oauth2.client.conf.OAuth2ClientProperties;
import uw.oauth2.client.constant.OAuth2ClientAuthStatus;
import uw.oauth2.client.constant.OAuth2ClientResponseCode;
import uw.oauth2.client.provider.*;
import uw.oauth2.client.vo.OAuth2StateId;
import uw.oauth2.client.vo.OAuth2Token;
import uw.oauth2.client.vo.OAuth2UserInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth2服务类，对外提供统一的OAuth2认证服务。
 * 管理所有的OAuth2 Provider，处理认证流程。
 */
public class OAuth2ClientHelper {

    /**
     * 日志记录器。
     */
    private static final Logger logger = LoggerFactory.getLogger(OAuth2ClientHelper.class);

    /**
     * 所有Provider实例。
     */
    private static final Map<String, OAuth2Provider> providerMap = new ConcurrentHashMap<>();

    /**
     * OAuth2客户端配置。
     */
    private static OAuth2ClientProperties properties;

    /**
     * 构造函数。
     *
     * @param properties OAuth2客户端配置。
     */
    public OAuth2ClientHelper(OAuth2ClientProperties properties) {
        OAuth2ClientHelper.properties = properties;
        // 过滤掉无效的Provider配置。
        OAuth2ClientHelper.properties.getProviders().entrySet().removeIf(entry -> {
            OAuth2ClientProperties.ProviderConfig config = entry.getValue();
            return config == null || StringUtils.isBlank(config.getClientId());
        });
        // 遍历配置中的所有Provider。
        properties.getProviders().forEach((providerCode, config) -> {
            // 创建Provider实例。
            OAuth2Provider provider = createProvider(providerCode, config, properties.getRedirectUri(), properties.getQrcodeUri());
            // 注册Provider。
            providerMap.put(providerCode, provider);
        });
    }

    /**
     * 创建Provider实例。
     *
     * @param providerCode   Provider名称。
     * @param providerConfig Provider配置。
     * @param redirectUri    重定向URL。
     * @param qrcodeUri      二维码URL。
     * @return Provider实例。
     */
    private static OAuth2Provider createProvider(String providerCode, OAuth2ClientProperties.ProviderConfig providerConfig, String redirectUri, String qrcodeUri) {
        // 根据Provider名称选择不同的实现。
        return switch (providerCode.toLowerCase()) {
            case "google", "github" ->
                // 这些平台都符合OAuth2标准，使用StandardOAuth2Provider
                    new StandardOAuth2Provider(providerCode, providerConfig, redirectUri, qrcodeUri);
            case "apple" ->
                // 苹果使用专门的Provider实现，处理Sign in with Apple流程
                    new AppleOAuth2Provider(providerCode, providerConfig, redirectUri, qrcodeUri);
            case "wechat" ->
                // 微信使用特定的Provider实现
                    new WechatOAuth2Provider(providerCode, providerConfig, redirectUri, qrcodeUri);
            case "alipay" ->
                // 支付宝使用特定的Provider实现
                    new AlipayOAuth2Provider(providerCode, providerConfig, redirectUri, qrcodeUri);
            default ->
                // 默认使用StandardOAuth2Provider
                    new StandardOAuth2Provider(providerCode, providerConfig, redirectUri, qrcodeUri);
        };
    }

    /**
     * 注册Provider。
     *
     * @param providerCode Provider名称。
     * @param provider     Provider实例。
     */
    public static void registerProvider(String providerCode, OAuth2Provider provider) {
        providerMap.put(providerCode, provider);
    }

    /**
     * 获取Provider。
     *
     * @param providerCode Provider名称。
     * @return Provider实例。
     */
    public static OAuth2Provider getProvider(String providerCode) {
        return providerMap.get(providerCode);
    }

    /**
     * 获取Provider实例Map。
     *
     * @return Provider实例Map。
     */
    public static Map<String, OAuth2Provider> getProviderMap() {
        return providerMap;
    }

    /**
     * 获取Provider配置Map。
     *
     * @return Provider配置Map。
     */
    public static Map<String, OAuth2ClientProperties.ProviderConfig> getConfigMap() {
        return properties.getProviders();
    }

    /**
     * 构建授权URL。
     *
     * @param providerCode Provider名称。
     * @param authStateId  验证状态ID。
     * @return 授权URL。
     */
    public static ResponseData<String> buildAuthUrl(String providerCode, String authStateId) {
        // 如果没有指定providerCode,则从state中获取Provider名称。
        if (StringUtils.isBlank(providerCode) && StringUtils.isNotBlank(authStateId)) {
            providerCode = OAuth2StateId.parse(authStateId).getProviderCode();
        }
        if (StringUtils.isBlank(providerCode)) {
            return ResponseData.errorCode(OAuth2ClientResponseCode.INVALID_PROVIDER);
        }
        OAuth2Provider provider = getProvider(providerCode);
        if (provider == null) {
            return ResponseData.errorCode(OAuth2ClientResponseCode.INVALID_PROVIDER);
        }
        return ResponseData.success(provider.buildAuthUrl(authStateId));
    }

    /**
     * 构建二维码URL。
     *
     * @param providerCode Provider名称。
     * @return 授权URL。
     */
    public static ResponseData<String> buildQrCode(String providerCode) {
        OAuth2Provider provider = getProvider(providerCode);
        if (provider == null) {
            return ResponseData.errorCode(OAuth2ClientResponseCode.INVALID_PROVIDER);
        }
        return ResponseData.success(provider.buildQrCode());
    }

    /**
     * 处理授权回调，获取访问令牌。
     *
     * @param providerCode Provider名称。
     * @param authCode     授权码。
     * @param authStateId  状态参数。
     * @param extParam     扩展参数。
     * @return 访问令牌信息。
     */
    public static ResponseData<OAuth2Token> getToken(String providerCode, String authCode, String authStateId, Map<String, String> extParam) {
        // 如果没有指定providerCode,则从state中获取Provider名称。
        if (StringUtils.isBlank(providerCode)) {
            providerCode = OAuth2StateId.parse(authStateId).getProviderCode();
        }
        if (StringUtils.isBlank(providerCode)) {
            return ResponseData.errorCode(OAuth2ClientResponseCode.INVALID_PROVIDER);
        }
        OAuth2Provider provider = getProvider(providerCode);
        if (provider == null) {
            return ResponseData.errorCode(OAuth2ClientResponseCode.INVALID_PROVIDER);
        }
        return provider.getToken(authCode, authStateId, extParam);
    }

    /**
     * 使用访问令牌获取用户信息。
     *
     * @param providerCode Provider名称。
     * @param oAuth2Token  访问令牌。
     * @return 用户信息。
     */
    public static ResponseData<OAuth2UserInfo> getUserInfo(String providerCode, OAuth2Token oAuth2Token) {
        OAuth2Provider provider = getProvider(providerCode);
        if (provider == null) {
            return ResponseData.errorCode(OAuth2ClientResponseCode.INVALID_PROVIDER);
        }
        return provider.getUserInfo(oAuth2Token);
    }

    /**
     * 获取授权状态。
     *
     * @param authStateId 状态参数。
     * @return 授权状态。
     */
    public static OAuth2ClientAuthStatus getAuthState(String authStateId) {
        if (StringUtils.isBlank(authStateId)) {
            return OAuth2ClientAuthStatus.FAILED;
        }
        String providerCode = OAuth2StateId.parse(authStateId).getProviderCode();
        if (StringUtils.isBlank(providerCode)) {
            return OAuth2ClientAuthStatus.FAILED;
        }
        OAuth2Provider provider = getProvider(providerCode);
        if (provider == null) {
            return OAuth2ClientAuthStatus.FAILED;
        }
        return provider.getAuthState(authStateId);
    }

    /**
     * 删除授权状态。
     *
     * @param authStateId 状态参数。
     */
    public static void invalidateAuthState(String authStateId) {
        if (StringUtils.isBlank(authStateId)) {
            return;
        }
        String providerCode = OAuth2StateId.parse(authStateId).getProviderCode();
        if (StringUtils.isBlank(providerCode)) {
            return;
        }
        OAuth2Provider provider = getProvider(providerCode);
        if (provider == null) {
            return;
        }
        provider.invalidateAuthState(authStateId);
    }

}