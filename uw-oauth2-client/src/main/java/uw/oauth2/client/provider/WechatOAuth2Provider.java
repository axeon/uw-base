package uw.oauth2.client.provider;

import uw.common.response.ResponseData;
import uw.common.util.JsonUtils;
import uw.httpclient.http.HttpData;
import uw.oauth2.client.conf.OAuth2ClientProperties;
import uw.oauth2.client.constant.OAuth2ClientResponseCode;
import uw.oauth2.client.vo.OAuth2Token;
import uw.oauth2.client.vo.OAuth2UserInfo;

import java.util.Map;

/**
 * 微信OAuth2 Provider，处理微信特有的OAuth2流程。
 * <p>
 * 差异点：授权URL使用appid而非client_id、换取token使用appid+secret、
 * 获取用户信息以query参数携带access_token与openid、错误码通过errcode字段返回。
 *
 * @author axeon
 */
public class WechatOAuth2Provider extends AbstractOAuth2Provider {

    /**
     * 构造函数。
     *
     * @param providerCode   Provider名称
     * @param providerConfig Provider配置
     * @param redirectUri    重定向URI
     * @param qrcodeUri      二维码URI
     */
    public WechatOAuth2Provider(String providerCode, OAuth2ClientProperties.ProviderConfig providerConfig, String redirectUri, String qrcodeUri) {
        super(providerCode, providerConfig, redirectUri, qrcodeUri);
    }

    /**
     * 获取微信用户信息。
     * <p>
     * 微信要求以query参数携带access_token与openid请求用户信息接口。
     *
     * @param oAuth2Token 令牌（需含accessToken与openId）
     * @return 用户信息；HTTP或解析失败时返回对应错误码
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
     * 添加微信授权URL特有参数：将client_id替换为appid。
     *
     * @param params 参数映射
     */
    @Override
    protected void addAuthParam(Map<String, String> params) {
        params.remove("client_id");
        params.put("appid", providerConfig.getClientId());
    }

    /**
     * 添加微信换取Token特有参数：使用appid+secret替代client_id+client_secret，并移除redirect_uri。
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
     * 解析微信令牌响应。
     * <p>
     * 先检查errcode判断是否出错，再反序列化为OAuth2Token；微信不返回scope，使用配置的authScope。
     *
     * @param responseBody 响应体
     * @return 令牌对象；errcode非0时返回错误
     */
    @Override
    protected ResponseData<OAuth2Token> parseTokenResponse(String responseBody) {
        Map<String, Object> tokenMap = JsonUtils.parse(responseBody, Map.class);

        // 微信返回的错误处理
        if (tokenMap.containsKey("errcode") && !"0".equals(tokenMap.get("errcode").toString())) {
            return ResponseData.errorCode(OAuth2ClientResponseCode.HTTP_REQUEST_FAILED, "Wechat token error: " + tokenMap.get("errcode") + " - " + tokenMap.get("errmsg"));
        }

        // 直接用OAuth2Token反序列化，由Jackson处理expires_in的字符串/数字兼容。
        OAuth2Token token = JsonUtils.parse(responseBody, OAuth2Token.class);
        token.setTokenType("Bearer");
        // 微信不返回scope，使用配置的scope
        token.setScope(providerConfig.getAuthScope());

        // 微信返回的openid/unionid需要设置到token中。
        token.setOpenId(asString(tokenMap.get("openid")));
        token.setUnionId(asString(tokenMap.get("unionid")));

        // 保存原始响应数据，包括openid等
        token.setRawParams(tokenMap);

        return ResponseData.success(token);
    }

    /**
     * 解析微信用户信息响应。
     * <p>
     * 提取openid/nickname/headimgurl等字段，性别按微信规则（1男2女）映射，
     * 地区由country/province/city拼接（自动过滤null）。
     *
     * @param responseBody 响应体
     * @return 用户信息；errcode非0或解析失败时返回错误
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
            userInfo.setEmail(asString(userMap.get("email")));
            userInfo.setPhone(asString(userMap.get("phone")));
            // 拼接地区，过滤null避免出现"null null null"。
            userInfo.setArea(joinNonBlank(" ", userMap.get("country"), userMap.get("province"), userMap.get("city")));

            // 保存原始响应数据
            userInfo.setRawParams(userMap);

            return ResponseData.success(userInfo);
        } catch (Exception e) {
            logger.error("Failed to parse user info response for provider {}", providerCode, e);
            return ResponseData.errorCode(OAuth2ClientResponseCode.HTTP_REQUEST_FAILED, e.toString());
        }
    }

    /**
     * 安全转换为字符串，避免数字类型强转失败。
     */
    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    /**
     * 用分隔符拼接非空字符串，跳过null。
     */
    private static String joinNonBlank(String delimiter, Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object part : parts) {
            if (part == null) {
                continue;
            }
            String str = part.toString().trim();
            if (str.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(delimiter);
            }
            sb.append(str);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

}