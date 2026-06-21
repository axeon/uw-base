package uw.oauth2.client.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.apache.commons.lang3.StringUtils;
import uw.common.response.ResponseData;
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
 * 苹果OAuth2 Provider，处理苹果特有的Sign in with Apple流程。
 * <p>
 * 差异点：换取token需用p8私钥动态生成JWT形式的client_secret（有效期6个月，缓存30天）、
 * 授权response_type为 {@code code id_token} 且response_mode为form_post、
 * 用户信息直接从id_token解析（不提供独立的用户信息接口）。
 * 所需扩展参数：extParam.teamId、extParam.keyId、extParam.p8Key。
 *
 * @author axeon
 */
public class AppleOAuth2Provider extends AbstractOAuth2Provider {

    /**
     * 自动生成的client_secret（JWT形式），缓存复用。
     */
    private volatile String CLIENT_SECRET;

    /**
     * 最后生成client_secret的时间戳，用于判断缓存是否到期（30天刷新）。
     */
    private volatile long CLIENT_SECRET_TIMESTAMP;


    /**
     * 构造函数。
     *
     * @param providerCode   Provider名称
     * @param providerConfig Provider配置（需在extParam配置teamId/keyId/p8Key）
     * @param redirectUri    重定向URI
     * @param qrcodeUri      二维码URI
     */
    public AppleOAuth2Provider(String providerCode, OAuth2ClientProperties.ProviderConfig providerConfig, String redirectUri, String qrcodeUri) {
        super(providerCode, providerConfig, redirectUri, qrcodeUri);
    }

    /**
     * 获取用户信息。
     * <p>
     * Apple不提供独立的用户信息接口，直接返回NOT_SUPPORTED警告，
     * 调用方应使用 {@link OAuth2Token} 中由id_token解析出的字段。
     *
     * @param oAuth2Token 访问令牌
     * @return 始终返回NOT_SUPPORTED警告
     */
    @Override
    public ResponseData<OAuth2UserInfo> getUserInfo(OAuth2Token oAuth2Token) {
        return ResponseData.warnCode(OAuth2ClientResponseCode.NOT_SUPPORTED);
    }

    /**
     * 添加Apple授权URL特有参数：response_type设为 {@code code id_token}，response_mode设为form_post。
     *
     * @param params 参数映射
     */
    @Override
    protected void addAuthParam(Map<String, String> params) {
        // 确保response_type包含id_token
        params.put("response_type", "code id_token");
        // 设置response_mode
        params.put("response_mode", "form_post");
    }


    /**
     * 添加Apple换取Token特有参数：用p8私钥动态生成JWT形式的client_secret。
     *
     * @param params 获取Token参数映射
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
     * 解析Apple令牌响应，并从id_token中提取openId/username/email等用户信息。
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
                token.setEmail((String) idTokenMap.get("email"));
                token.setPhone((String) idTokenMap.get("phone"));
                token.setAvatar(getAvatarFromMap(idTokenMap));
            }
        }
        return ResponseData.success(token);
    }

    /**
     * 解析Apple用户信息响应（实际由id_token解析，此方法作为接口对齐保留）。
     *
     * @param responseBody 响应体
     * @return 用户信息
     */
    @Override
    protected ResponseData<OAuth2UserInfo> parseUserInfoResponse(String responseBody) {
        Map<String, Object> userMap = JsonUtils.parse(responseBody, Map.class);
        return buildUserInfoFromMap(userMap);
    }

    /**
     * 从用户信息Map构建OAuth2UserInfo对象。
     * <p>
     * 用户名优先取name，其次由given_name与family_name拼接。
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
     * 获取Apple client_secret（JWT形式）。
     * <p>
     * 使用p8私钥签发，有效期6个月（Apple允许的最大值），缓存30天后自动刷新。
     * 采用双重检查锁保证多线程下只生成一次；任一入参缺失时抛IllegalArgumentException。
     *
     * @param teamId   团队ID（extParam.teamId）
     * @param clientId 客户端ID（即Service ID）
     * @param keyId    密钥ID（extParam.keyId）
     * @param p8Key    .p8私钥内容（extParam.p8Key）
     * @return JWT形式的client_secret
     */
    private String genClientSecret(String teamId, String clientId, String keyId, String p8Key) {
        // 入参校验，避免后续NPE且给出清晰错误。
        if (StringUtils.isBlank(teamId) || StringUtils.isBlank(clientId) || StringUtils.isBlank(keyId) || StringUtils.isBlank(p8Key)) {
            throw new IllegalArgumentException("Apple provider requires extParam: teamId, keyId, p8Key and a non-empty clientId");
        }
        if (CLIENT_SECRET == null || SystemClock.now() - CLIENT_SECRET_TIMESTAMP > 30L * 24 * 60 * 60 * 1000) {
            synchronized (this) {
                // 双重检查，避免多线程重复生成并互相覆盖。
                if (CLIENT_SECRET != null && SystemClock.now() - CLIENT_SECRET_TIMESTAMP <= 30L * 24 * 60 * 60 * 1000) {
                    return CLIENT_SECRET;
                }
                // 读取并解析.p8私钥文件
                ECPrivateKey privateKey;
                try {
                    p8Key = p8Key.replaceAll("-----BEGIN PRIVATE KEY-----", "").replaceAll("-----END PRIVATE KEY-----", "").replaceAll("\\s+", "");
                    byte[] keyBytes = Base64.getDecoder().decode(p8Key);
                    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
                    KeyFactory keyFactory = KeyFactory.getInstance("EC");
                    privateKey = (ECPrivateKey) keyFactory.generatePrivate(keySpec);
                } catch (Throwable e) {
                    logger.error("Failed to read P8 key: {}", e.getMessage());
                    throw new RuntimeException("Failed to parse Apple P8 private key", e);
                }
                // 有效期设置为6个月（Apple允许的最大值）
                Date nowDate = new Date(SystemClock.now());
                Date expiredDate = new Date(SystemClock.now() + 180 * 24 * 60 * 60 * 1000L);

                // 创建JWT token (client_secret)
                CLIENT_SECRET = JWT.create().withHeader(Map.of("kid", keyId))  // Key ID作为header
                        .withIssuer(teamId)                // iss: Team ID
                        .withSubject(clientId)             // sub: Service ID
                        .withAudience("https://appleid.apple.com") // aud
                        .withIssuedAt(nowDate)       // iat: 当前时间
                        .withExpiresAt(expiredDate) // exp: 过期时间
                        .sign(Algorithm.ECDSA256(null, privateKey));
                // 仅在生成成功后更新时间戳，避免失败时缓存空值。
                CLIENT_SECRET_TIMESTAMP = SystemClock.now();
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