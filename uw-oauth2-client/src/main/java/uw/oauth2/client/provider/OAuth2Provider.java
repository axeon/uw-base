package uw.oauth2.client.provider;

import uw.common.response.ResponseData;
import uw.oauth2.client.conf.OAuth2ClientProperties;
import uw.oauth2.client.constant.OAuth2ClientAuthStatus;
import uw.oauth2.client.vo.OAuth2Token;
import uw.oauth2.client.vo.OAuth2UserInfo;

import java.util.Map;

/**
 * OAuth2 Provider接口，定义第三方登录平台的核心功能。
 * <p>
 * 所有第三方登录平台都需要实现此接口；通用流程已由
 * {@link AbstractOAuth2Provider} 实现，新平台通常继承该抽象类并覆盖差异化钩子即可。
 *
 * @author axeon
 */
public interface OAuth2Provider {

    /**
     * 获取Provider标识。
     *
     * @return Provider标识
     */
    String getProviderCode();

    /**
     * 获取Provider配置信息。
     *
     * @return 配置信息
     */
    OAuth2ClientProperties.ProviderConfig getProviderConfig();

    /**
     * 构建授权请求URL，引导用户跳转到第三方授权页面。
     *
     * @param authStateId 状态ID（为空时由实现自动生成）
     * @return 授权URL
     */
    String buildAuthUrl(String authStateId);

    /**
     * 构建扫码登录二维码URL。
     *
     * @return 二维码URL
     */
    String buildQrCode();

    /**
     * 使用授权码换取访问令牌。
     *
     * @param authCode    授权码
     * @param authStateId 授权状态（用于CSRF校验）
     * @param extParam    额外参数（可为null）
     * @return 访问令牌
     */
    ResponseData<OAuth2Token> getToken(String authCode, String authStateId, Map<String, String> extParam);

    /**
     * 使用访问令牌获取用户信息。
     *
     * @param oAuth2Token 访问令牌
     * @return 用户信息；不支持时返回NOT_SUPPORTED警告
     */
    ResponseData<OAuth2UserInfo> getUserInfo(OAuth2Token oAuth2Token);

    /**
     * 获取授权状态（用于扫码轮询）。
     *
     * @param authStateId 状态ID
     * @return 授权状态
     */
    OAuth2ClientAuthStatus getAuthState(String authStateId);

    /**
     * 删除授权状态，使其立即失效。
     *
     * @param authStateId 状态ID
     */
    void invalidateAuthState(String authStateId);
}