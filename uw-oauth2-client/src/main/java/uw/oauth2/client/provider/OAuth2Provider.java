package uw.oauth2.client.provider;

import uw.common.dto.ResponseData;
import uw.oauth2.client.conf.OAuth2ClientProperties;
import uw.oauth2.client.constant.OAuth2ClientAuthStatus;
import uw.oauth2.client.vo.OAuth2Token;
import uw.oauth2.client.vo.OAuth2UserInfo;

import java.util.Map;

/**
 * OAuth2 Provider接口，定义第三方登录平台的核心功能。
 * 所有第三方登录平台都需要实现此接口。
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
     * 构建授权请求URL。
     *
     * @param authStateId 状态ID
     * @return 授权URL
     */
    String buildAuthUrl(String authStateId);

    /**
     * 构建二维码.
     *
     * @return 二维码信息
     */
    String buildQrCode();

    /**
     * 获取访问令牌
     *
     * @param authCode    授权码
     * @param authStateId 授权状态
     * @param extParam    额外参数
     * @return 访问令牌
     */
    ResponseData<OAuth2Token> getToken(String authCode, String authStateId, Map<String, String> extParam);

    /**
     * 使用访问令牌获取用户信息
     *
     * @param oAuth2Token 访问令牌
     * @return 用户信息
     */
    ResponseData<OAuth2UserInfo> getUserInfo(OAuth2Token oAuth2Token);

    /**
     * 获取授权状态
     *
     * @param authStateId 授权状态
     * @return 授权状态
     */
    OAuth2ClientAuthStatus getAuthState(String authStateId);

    /**
     * 删除授权状态。
     *
     * @param authStateId
     */
    void invalidateAuthState(String authStateId);
}