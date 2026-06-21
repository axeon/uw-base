package uw.oauth2.client;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.response.ResponseData;
import uw.oauth2.client.conf.OAuth2ClientProperties;
import uw.oauth2.client.constant.OAuth2ClientAuthStatus;
import uw.oauth2.client.constant.OAuth2ClientResponseCode;
import uw.oauth2.client.provider.*;
import uw.oauth2.client.vo.OAuth2StateId;
import uw.oauth2.client.vo.OAuth2Token;
import uw.oauth2.client.vo.OAuth2UserInfo;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth2服务类，对外提供统一的OAuth2认证服务。
 * <p>
 * 管理所有的OAuth2 Provider，处理认证流程，包括：
 * <ul>
 *     <li>构建授权URL与扫码二维码URL；</li>
 *     <li>使用授权码换取访问令牌；</li>
 *     <li>使用访问令牌获取三方用户信息；</li>
 *     <li>维护授权状态（stateId）的生命周期，用于扫码轮询与CSRF防护。</li>
 * </ul>
 * 该类所有核心方法均为静态方法，配置与Provider注册表在构造时一次性初始化后驻留于静态字段，
 * 业务侧可直接以 {@code OAuth2ClientHelper.xxx()} 方式调用，无需注入Bean。
 *
 * @author axeon
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
     * 注册自定义Provider，用于在运行时动态接入新的第三方登录平台。
     *
     * @param providerCode Provider名称
     * @param provider     Provider实例
     */
    public static void registerProvider(String providerCode, OAuth2Provider provider) {
        providerMap.put(providerCode, provider);
    }

    /**
     * 获取Provider。
     *
     * @param providerCode Provider名称
     * @return Provider实例；未注册时返回null
     */
    public static OAuth2Provider getProvider(String providerCode) {
        return providerMap.get(providerCode);
    }

    /**
     * 获取Provider实例Map。
     *
     * @return Provider实例Map（只读视图）。
     */
    public static Map<String, OAuth2Provider> getProviderMap() {
        return Collections.unmodifiableMap(providerMap);
    }

    /**
     * 获取Provider配置Map。
     *
     * @return Provider配置Map（只读视图）。
     */
    public static Map<String, OAuth2ClientProperties.ProviderConfig> getConfigMap() {
        return Collections.unmodifiableMap(properties.getProviders());
    }

    /**
     * 构建授权URL。
     * <p>
     * 若 {@code providerCode} 为空，则尝试从 {@code authStateId} 中解析出Provider。
     * 当 {@code authStateId} 为空时，内部会自动生成一个新的状态ID（授权类型为auth）。
     *
     * @param providerCode Provider名称，为空时从 authStateId 解析
     * @param authStateId  验证状态ID，为空时自动生成
     * @return 授权URL；Provider无效时返回 INVALID_PROVIDER 错误
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
     * 构建二维码URL（扫码登录入口）。
     * <p>
     * 内部会生成一个新的状态ID（授权类型为qrcode）并置为WAITING状态，
     * 返回的URL供前端渲染成二维码，用户扫码后由手机端完成授权。
     *
     * @param providerCode Provider名称
     * @return 二维码URL；Provider无效时返回 INVALID_PROVIDER 错误
     */
    public static ResponseData<String> buildQrCode(String providerCode) {
        OAuth2Provider provider = getProvider(providerCode);
        if (provider == null) {
            return ResponseData.errorCode(OAuth2ClientResponseCode.INVALID_PROVIDER);
        }
        return ResponseData.success(provider.buildQrCode());
    }

    /**
     * 处理授权回调，使用授权码换取访问令牌。
     * <p>
     * 调用前会校验 {@code authStateId} 必须处于 SCANNED 状态，校验通过后置为 CONFIRMED；
     * 若换Token失败（HTTP异常或状态码非200）则清理state，避免被重复利用。
     *
     * @param providerCode Provider名称，为空时从 authStateId 解析
     * @param authCode     授权码（第三方回调返回的code）
     * @param authStateId  状态参数（授权URL携带的state）
     * @param extParam     扩展参数，按Provider差异化使用（可为null）
     * @return 访问令牌信息；state无效、Provider无效或HTTP失败时返回对应错误码
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
     * <p>
     * 部分Provider（如Apple）不支持独立的用户信息接口，会返回 NOT_SUPPORTED 警告，
     * 调用方应回退使用 {@link OAuth2Token} 中已解析的字段（openId/email等）。
     *
     * @param providerCode Provider名称
     * @param oAuth2Token  访问令牌（由 {@link #getToken} 获取）
     * @return 用户信息；Provider无效返回错误，不支持时返回 NOT_SUPPORTED 警告
     */
    public static ResponseData<OAuth2UserInfo> getUserInfo(String providerCode, OAuth2Token oAuth2Token) {
        OAuth2Provider provider = getProvider(providerCode);
        if (provider == null) {
            return ResponseData.errorCode(OAuth2ClientResponseCode.INVALID_PROVIDER);
        }
        return provider.getUserInfo(oAuth2Token);
    }

    /**
     * 获取授权状态（用于扫码登录轮询）。
     *
     * @param authStateId 状态参数
     * @return 授权状态；authStateId为空或Provider无效时返回 FAILED，state不存在时返回 EXPIRED
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
     * 删除授权状态，使其立即失效。
     * <p>
     * 通常在授权流程结束（登录成功或失败）后调用，清理GlobalCache中的state记录。
     *
     * @param authStateId 状态参数；为空或无法解析Provider时直接忽略
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