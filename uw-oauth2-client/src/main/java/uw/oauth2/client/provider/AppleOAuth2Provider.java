package uw.oauth2.client.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.apache.commons.lang3.StringUtils;
import uw.common.dto.ResponseData;
import uw.common.util.JsonUtils;
import uw.common.util.SystemClock;
import uw.oauth2.client.conf.OAuth2ClientProperties;
import uw.oauth2.client.constant.OAuth2ClientResponseCode;
import uw.oauth2.client.vo.OAuth2Token;
import uw.oauth2.client.vo.OAuth2UserInfo;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * 苹果OAuth2 Provider，处理苹果特有的Sign in with Apple流程
 */
public class AppleOAuth2Provider extends AbstractOAuth2Provider {

    /**
     * 自动生成clientSecret.
     */
    private volatile String CLIENT_SECRET;

    /**
     * 最后生成JWT令牌的时间
     */
    private volatile long CLIENT_SECRET_TIMESTAMP;


    /**
     * 构造函数
     *
     * @param providerCode   Provider名称
     * @param providerConfig Provider配置
     */
    public AppleOAuth2Provider(String providerCode, OAuth2ClientProperties.ProviderConfig providerConfig, String redirectUri, String qrcodeUri) {
        super(providerCode, providerConfig, redirectUri, qrcodeUri);
    }

    /**
     * 获取用户信息。
     * 直接返回不支持。
     *
     * @param oAuth2Token 访问令牌
     * @return 用户信息
     */
    @Override
    public ResponseData<OAuth2UserInfo> getUserInfo(OAuth2Token oAuth2Token) {
        return ResponseData.warnCode(OAuth2ClientResponseCode.NOT_SUPPORTED);
    }

    /**
     * 添加平台特定的参数
     */
    @Override
    protected void addAuthParam(Map<String, String> params) {
        // 确保response_type包含id_token
        params.put("response_type", "code id_token");
        // 设置response_mode
        params.put("response_mode", "form_post");
    }


    /**
     * 添加平台特定的参数。
     *
     * @param params 获取Token参数映射。
     */
    @Override
    protected void addTokenParam(Map<String, String> params) {
        String p8Key = providerConfig.getExtParam().get("p8Key");
        String teamId = providerConfig.getExtParam().get("teamId");
        String keyId = providerConfig.getExtParam().get("keyId");
        String clientId = providerConfig.getClientId();
        String clientSecret = genClientSecret(teamId, clientId, keyId, p8Key);
        params.put("client_secret", clientSecret);
    }

    /**
     * 解析令牌响应
     *
     * @param responseBody 响应体
     * @return OAuth2令牌
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
     * 解析用户信息响应
     *
     * @param responseBody 响应体
     * @return
     */
    @Override
    protected ResponseData<OAuth2UserInfo> parseUserInfoResponse(String responseBody) {
        Map<String, Object> userMap = JsonUtils.parse(responseBody, Map.class);
        return buildUserInfoFromMap(userMap);
    }

    /**
     * 从用户信息Map构建OAuth2UserInfo对象
     *
     * @param userMap 用户信息Map
     * @return OAuth2UserInfo对象
     */
    private ResponseData<OAuth2UserInfo> buildUserInfoFromMap(Map<String, Object> userMap) {
        OAuth2UserInfo userInfo = new OAuth2UserInfo();
        userInfo.setProviderCode(providerCode);

        // 从sub字段获取用户ID
        String userId = (String) userMap.get("sub");
        userInfo.setOpenId(userId);

        // 从name字段获取姓名
        if (userMap.containsKey("name")) {
            userInfo.setUsername((String) userMap.get("name"));
        } else {
            // 从given_name和family_name构建姓名
            StringBuilder name = new StringBuilder();
            if (userMap.containsKey("given_name")) {
                name.append(userMap.get("given_name"));
            }
            if (userMap.containsKey("family_name")) {
                if (name.length() > 0) {
                    name.append(" ");
                }
                name.append(userMap.get("family_name"));
            }
            if (name.length() > 0) {
                userInfo.setUsername(name.toString());
            }
        }

        // 从email字段获取邮箱
        userInfo.setEmail((String) userMap.get("email"));

        // 保存原始响应数据
        userInfo.setRawParams(userMap);

        return ResponseData.success(userInfo);
    }


    /**
     * 获取clientSecret.
     *
     * @param teamId   团队ID
     * @param clientId 客户端ID
     * @param keyId    密钥ID
     * @param p8Key    .p8私钥文件内容
     * @return JWT令牌
     */
    private String genClientSecret(String teamId, String clientId, String keyId, String p8Key) {
        if (CLIENT_SECRET == null || SystemClock.now() - CLIENT_SECRET_TIMESTAMP > 24 * 60 * 1000) {
            synchronized (this) {
                // 读取并解析.p8私钥文件
                ECPrivateKey privateKey = null;
                try {
                    p8Key = p8Key.replaceAll("-----BEGIN PRIVATE KEY-----", "").replaceAll("-----END PRIVATE KEY-----", "").replaceAll("\\s+", "");
                    byte[] keyBytes = Base64.getDecoder().decode(p8Key);
                    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
                    KeyFactory keyFactory = KeyFactory.getInstance("EC");
                    privateKey = (ECPrivateKey) keyFactory.generatePrivate(keySpec);
                } catch (Throwable e) {
                    logger.error("Failed to read P8 key: {}", e.getMessage());
                }
                if (privateKey == null) {
                    return null;
                }
                // 有效期设置为6个月（Apple允许的最大值）[^45^]
                Date nowDate = new Date(SystemClock.now());
                Date expiredDate = new Date(SystemClock.now() + 180 * 24 * 60 * 60 * 1000L);

                // 创建JWT token (client_secret)
                CLIENT_SECRET_TIMESTAMP = SystemClock.now();
                CLIENT_SECRET = JWT.create().withHeader(Map.of("kid", keyId))  // Key ID作为header
                        .withIssuer(teamId)                // iss: Team ID
                        .withSubject(clientId)             // sub: Service ID
                        .withAudience("https://appleid.apple.com") // aud
                        .withIssuedAt(nowDate)       // iat: 当前时间
                        .withExpiresAt(expiredDate) // exp: 过期时间
                        .sign(Algorithm.ECDSA256(null, privateKey));
            }
        }
        return CLIENT_SECRET;
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