package uw.oauth2.client.provider;

import uw.common.dto.ResponseData;
import uw.common.util.JsonUtils;
import uw.httpclient.http.HttpData;
import uw.oauth2.client.conf.OAuth2ClientProperties;
import uw.oauth2.client.constant.OAuth2ClientResponseCode;
import uw.oauth2.client.vo.OAuth2Token;
import uw.oauth2.client.vo.OAuth2UserInfo;

import java.util.Map;

/**
 * 微信OAuth2 Provider，处理微信特有的OAuth2流程。
 */
public class WechatOAuth2Provider extends AbstractOAuth2Provider {

    /**
     * 构造函数
     *
     * @param providerCode   Provider名称。
     * @param providerConfig Provider配置。
     */
    public WechatOAuth2Provider(String providerCode, OAuth2ClientProperties.ProviderConfig providerConfig, String redirectUri, String qrcodeUri) {
        super(providerCode, providerConfig, redirectUri, qrcodeUri);
    }

    /**
     * 获取用户信息
     *
     * @param oAuth2Token 令牌
     * @return
     */
    @Override
    public ResponseData<OAuth2UserInfo> getUserInfo(OAuth2Token oAuth2Token) {
        try {
            // 微信获取用户信息需要使用access_token和openid
            String userInfoUrl = providerConfig.getUserInfoUri() + "?access_token=" + oAuth2Token.getAccessToken() + "&openid=" + oAuth2Token.getOpenId();

            // 使用GET方法获取用户信息。
            HttpData httpData = JSON_INTERFACE_HELPER.getForData(userInfoUrl);
            if (httpData.getStatusCode() != 200) {
                return ResponseData.errorCode(OAuth2ClientResponseCode.INVALID_HTTP_CODE, httpData.getStatusCode(), httpData.getResponseData());
            }

            String responseBody = httpData.getResponseData();

            // 解析用户信息
            return parseUserInfoResponse(responseBody);
        } catch (Exception e) {
            logger.error("Failed to get user info for provider {}", providerCode, e);
            return ResponseData.errorCode(OAuth2ClientResponseCode.HTTP_REQUEST_FAILED, e.toString());
        }
    }

    /**
     * 添加微信平台特有的参数
     *
     * @param params 参数映射
     */
    @Override
    protected void addAuthParam(Map<String, String> params) {
        params.remove("client_id");
        params.put("appid", providerConfig.getClientId());
    }

    /**
     * 添加微信平台特有的参数
     *
     * @param params 参数映射
     */
    @Override
    protected void addTokenParam(Map<String, String> params) {
        params.remove("client_secret");
        params.remove("client_id");
        params.remove("redirect_uri");
        params.put("appid", providerConfig.getClientId());
        params.put("secret", providerConfig.getClientSecret());
    }

    /**
     * 解析令牌响应
     *
     * @param responseBody 响应体
     * @return
     */
    @Override
    protected ResponseData<OAuth2Token> parseTokenResponse(String responseBody) {
        Map<String, Object> tokenMap = JsonUtils.parse(responseBody, Map.class);

        // 微信返回的错误处理
        if (tokenMap.containsKey("errcode") && !"0".equals(tokenMap.get("errcode").toString())) {
            return ResponseData.errorCode(OAuth2ClientResponseCode.HTTP_REQUEST_FAILED, "Wechat token error: " + tokenMap.get("errcode") + " - " + tokenMap.get("errmsg"));
        }

        OAuth2Token token = new OAuth2Token();
        token.setAccessToken((String) tokenMap.get("access_token"));
        token.setRefreshToken((String) tokenMap.get("refresh_token"));
        token.setTokenType("Bearer");
        token.setExpiresIn(((Number) tokenMap.getOrDefault("expires_in", 0)).longValue());
        // 微信不返回scope，使用配置的scope
        token.setScope(providerConfig.getAuthScope());

        // 微信返回的openid需要设置到token中。
        token.setOpenId((String) tokenMap.get("openid"));
        token.setUnionId((String) tokenMap.get("unionid"));

        // 保存原始响应数据，包括openid等
        token.setRawParams(tokenMap);

        return ResponseData.success(token);
    }

    /**
     * 解析用户信息响应
     *
     * @param responseBody 响应体
     * @return
     */
    @Override
    protected ResponseData<OAuth2UserInfo> parseUserInfoResponse(String responseBody) {
        try {
            Map<String, Object> userMap = JsonUtils.parse(responseBody, Map.class);

            // 微信返回的错误处理
            if (userMap.containsKey("errcode") && !"0".equals(userMap.get("errcode").toString())) {
                return ResponseData.errorCode(OAuth2ClientResponseCode.HTTP_REQUEST_FAILED, "Wechat user info error: " + userMap.get("errcode") + " - " + userMap.get("errmsg"));
            }

            OAuth2UserInfo userInfo = new OAuth2UserInfo();
            userInfo.setProviderCode(providerCode);
            userInfo.setOpenId((String) userMap.get("openid"));
            userInfo.setUnionId((String) userMap.get("unionid"));
            userInfo.setUsername((String) userMap.get("nickname"));
            userInfo.setAvatar((String) userMap.get("headimgurl"));

            // 微信性别：0-未知，1-男，2-女
            String gender = "unknown";
            if ("1".equals(String.valueOf(userMap.get("sex")))) {
                gender = "male";
            } else if ("2".equals(String.valueOf(userMap.get("sex")))) {
                gender = "female";
            }
            userInfo.setGender(gender);

            // 设置其他可能的字段
            userInfo.setEmail((String) userMap.get("email"));
            userInfo.setPhone((String) userMap.get("phone"));
            userInfo.setArea(userMap.get("country") + " " + userMap.get("province") + " " + userMap.get("city"));

            // 保存原始响应数据
            userInfo.setRawParams(userMap);

            return ResponseData.success(userInfo);
        } catch (Exception e) {
            logger.error("Failed to parse user info response for provider {}", providerCode, e);
            return ResponseData.errorCode(OAuth2ClientResponseCode.HTTP_REQUEST_FAILED, e.toString());
        }
    }

}