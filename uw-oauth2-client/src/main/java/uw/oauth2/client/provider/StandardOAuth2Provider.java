package uw.oauth2.client.provider;

import org.apache.commons.lang3.StringUtils;
import uw.common.dto.ResponseData;
import uw.common.util.JsonUtils;
import uw.oauth2.client.conf.OAuth2ClientProperties;
import uw.oauth2.client.vo.OAuth2Token;
import uw.oauth2.client.vo.OAuth2UserInfo;

import java.util.Map;

/**
 * 标准OAuth2 Provider，用于处理符合OAuth2标准的平台。
 * 如Google、Apple、GitHub等。
 */
public class StandardOAuth2Provider extends AbstractOAuth2Provider {

    /**
     * 构造函数。
     *
     * @param providerCode   Provider名称
     * @param providerConfig Provider配置
     */
    public StandardOAuth2Provider(String providerCode, OAuth2ClientProperties.ProviderConfig providerConfig, String redirectUri, String qrcodeUri) {
        super(providerCode, providerConfig, redirectUri, qrcodeUri);
    }

    /**
     * 解析令牌响应。
     *
     * @param responseBody 响应体
     * @return 令牌对象
     */
    @Override
    protected ResponseData<OAuth2Token> parseTokenResponse(String responseBody) {
        // 直接使用OAuth2Token解析令牌响应，消除中间转换步骤
        OAuth2Token token = JsonUtils.parse(responseBody, OAuth2Token.class);
        // 保存原始响应数据
        token.setRawParams(JsonUtils.parse(responseBody, Map.class));
        // 尝试解析idToken。
        if (StringUtils.isNotBlank(token.getIdToken())) {
            Map<String, Object> idTokenMap = parseIdToken(token.getIdToken());
            if (idTokenMap != null) {
                token.setOpenId(getUserIdFromMap(idTokenMap));
                token.setUsername(getUsernameFromMap(idTokenMap));
                token.setEmail(String.valueOf(idTokenMap.get("email")));
                token.setPhone(String.valueOf(idTokenMap.get("phone")));
                token.setAvatar(getAvatarFromMap(idTokenMap));
            }
        }
        return ResponseData.success(token);
    }

    /**
     * 解析用户信息响应。
     *
     * @param responseBody 响应体
     * @return 用户信息对象
     */
    @Override
    protected ResponseData<OAuth2UserInfo> parseUserInfoResponse(String responseBody) {
        // 读取原始Map数据，用于处理多字段映射
        Map<String, Object> userMap = JsonUtils.parse(responseBody, Map.class);

        // 直接使用OAuth2UserInfo解析用户信息响应
        OAuth2UserInfo userInfo = new OAuth2UserInfo();

        userInfo.setProviderCode(providerCode);

        // 处理用户ID映射，支持多种字段名
        String userId = getUserIdFromMap(userMap);
        userInfo.setOpenId(userId);
        // 设置用户名
        userInfo.setUsername(getUsernameFromMap(userMap));
        // 处理头像映射，支持多种字段名
        userInfo.setAvatar(getAvatarFromMap(userMap));
        // 保存原始用户信息
        userInfo.setRawParams(userMap);
        return ResponseData.success(userInfo);
    }

    /**
     * 从用户信息Map中获取用户ID，支持多种字段名。
     *
     * @param userMap 用户信息Map
     * @return 用户ID
     */
    private String getUserIdFromMap(Map<String, Object> userMap) {
        // 支持多种用户ID字段名
        String userId = (String) userMap.get("openId");
        if (userId == null) userId = (String) userMap.get("sub");
        if (userId == null) userId = (String) userMap.get("id");
        if (userId == null) userId = (String) userMap.get("openid");
        if (userId == null) userId = (String) userMap.get("uid");
        if (userId == null) userId = (String) userMap.get("login");
        return userId;
    }

    /**
     * 从用户信息Map中获取昵称，支持多种字段名。
     *
     * @param userMap 用户信息Map
     * @return 昵称
     */
    private String getUsernameFromMap(Map<String, Object> userMap) {
        // 支持多种昵称字段名
        String nickname = (String) userMap.get("nickname");
        if (nickname == null) nickname = (String) userMap.get("name");
        if (nickname == null) nickname = (String) userMap.get("screen_name");
        if (nickname == null) nickname = (String) userMap.get("login");
        return nickname;
    }

    /**
     * 从用户信息Map中获取头像URL，支持多种字段名。
     *
     * @param userMap 用户信息Map
     * @return 头像URL
     */
    private String getAvatarFromMap(Map<String, Object> userMap) {
        // 支持多种头像字段名
        String avatar = (String) userMap.get("picture");
        if (avatar == null) avatar = (String) userMap.get("avatar_url");
        if (avatar == null) avatar = (String) userMap.get("headimgurl");
        if (avatar == null) avatar = (String) userMap.get("avatar");
        return avatar;
    }

}