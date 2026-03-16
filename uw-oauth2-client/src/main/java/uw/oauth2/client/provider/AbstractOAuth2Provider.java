package uw.oauth2.client.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;
import uw.cache.GlobalCache;
import uw.cache.vo.CacheValueWrapper;
import uw.common.dto.ResponseData;
import uw.common.util.SnowflakeIdGenerator;
import uw.httpclient.http.HttpConfig;
import uw.httpclient.http.HttpData;
import uw.httpclient.http.HttpInterface;
import uw.httpclient.json.JsonInterfaceHelper;
import uw.httpclient.util.SSLContextUtils;
import uw.oauth2.client.conf.OAuth2ClientProperties;
import uw.oauth2.client.constant.OAuth2ClientAuthStatus;
import uw.oauth2.client.constant.OAuth2ClientAuthType;
import uw.oauth2.client.constant.OAuth2ClientResponseCode;
import uw.oauth2.client.vo.OAuth2StateId;
import uw.oauth2.client.vo.OAuth2Token;
import uw.oauth2.client.vo.OAuth2UserInfo;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 抽象OAuth2 Provider，实现通用的OAuth2流程。
 * 具体的Provider可以继承此类，只需要实现特定平台的差异化逻辑。
 */
public abstract class AbstractOAuth2Provider implements OAuth2Provider {

    /**
     * 状态ID缓存Key。
     */
    private static final String STATE_ID_KEY = "uw.oauth.state";

    /**
     * 状态ID有效期。
     */
    private static final long STATE_ID_EXPIRE_TIME = 300 * 1000;

    /**
     * 日志。
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * JSON接口帮助类。
     */
    protected final HttpInterface JSON_INTERFACE_HELPER = new JsonInterfaceHelper(HttpConfig.builder().connectTimeout(30000).readTimeout(30000).writeTimeout(30000).sslSocketFactory(SSLContextUtils.getTruestAllSocketFactory()).trustManager(SSLContextUtils.getTrustAllManager()).hostnameVerifier((s, sslSession) -> true).writeTimeout(60000).build());

    /**
     * Provider代码。
     */
    protected final String providerCode;

    /**
     * Provider配置。
     */
    protected final OAuth2ClientProperties.ProviderConfig providerConfig;

    /**
     * 重定向URI。
     */
    protected final String redirectUri;

    /**
     * 二维码URI。
     */
    protected final String qrcodeUri;

    /**
     * 构造函数
     *
     * @param providerCode   Provider名称
     * @param providerConfig Provider配置
     * @param redirectUri    重定向URI
     */
    protected AbstractOAuth2Provider(String providerCode, OAuth2ClientProperties.ProviderConfig providerConfig, String redirectUri, String qrcodeUri) {
        this.providerCode = providerCode;
        this.providerConfig = providerConfig;
        this.redirectUri = redirectUri;
        this.qrcodeUri = qrcodeUri;
    }

    /**
     * 获取Provider名称
     *
     * @return Provider名称
     */
    @Override
    public String getProviderCode() {
        return providerCode;
    }

    /**
     * 构建授权URL
     *
     * @param authStateId 状态ID
     * @return 授权URL
     */
    @Override
    public String buildAuthUrl(String authStateId) {
        if (StringUtils.isBlank(authStateId)) {
            return buildAuthUrl(generateStateId(OAuth2ClientAuthType.AUTH.toString().toLowerCase()));
        }
        Map<String, String> params = new HashMap<>();
        params.put("client_id", providerConfig.getClientId());
        params.put("redirect_uri", redirectUri);
        params.put("response_type", "code");
        if (providerConfig.getAuthScope() != null) {
            params.put("scope", providerConfig.getAuthScope());
        }
        params.put("state", authStateId);
        setAuthState(authStateId, OAuth2ClientAuthStatus.SCANNED);
        // 添加平台特定参数
        addAuthParam(params);
        // 构建URL。
        String queryString = params.entrySet().stream().map(entry -> entry.getKey() + "=" + UriUtils.encode(entry.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
        return providerConfig.getAuthUri() + "?" + queryString;
    }

    /**
     * 生成二维码
     *
     * @return 二维码信息
     */
    @Override
    public String buildQrCode() {
        String authStateId = generateStateId(OAuth2ClientAuthType.QRCODE.toString().toLowerCase());
        setAuthState(authStateId, OAuth2ClientAuthStatus.WAITING);
        return this.qrcodeUri + authStateId;
    }

    /**
     * 获取Token。
     *
     * @return Token响应。
     */
    @Override
    public ResponseData<OAuth2Token> getToken(String authCode, String authStateId, Map<String, String> extParam) {
        // 检查authStateId。
        if (!checkAuthState(authStateId, OAuth2ClientAuthStatus.SCANNED)) {
            return ResponseData.errorCode(OAuth2ClientResponseCode.INVALID_STATE_ID);
        }
        Map<String, String> params = new HashMap<>();
        params.put("client_id", providerConfig.getClientId());
        params.put("client_secret", providerConfig.getClientSecret());
        params.put("redirect_uri", redirectUri);
        params.put("grant_type", "authorization_code");
        params.put("code", authCode);
        addTokenParam(params);
        try {
            HttpData httpData = JSON_INTERFACE_HELPER.postFormForData(providerConfig.getTokenUri(), params);
            if (httpData.getStatusCode() != 200) {
                return ResponseData.errorCode(OAuth2ClientResponseCode.INVALID_HTTP_CODE, httpData.getStatusCode(), httpData.getResponseData());
            }
            setAuthState(authStateId, OAuth2ClientAuthStatus.CONFIRMED);
            return parseTokenResponse(httpData.getResponseData());
        } catch (Exception e) {
            return ResponseData.errorCode(OAuth2ClientResponseCode.HTTP_REQUEST_FAILED, e.toString());
        }
    }

    /**
     * 使用访问令牌获取用户信息
     *
     * @param oAuth2Token 访问令牌
     * @return 用户信息
     */
    public ResponseData<OAuth2UserInfo> getUserInfo(OAuth2Token oAuth2Token) {
        // 构建请求头
        Map<String, String> headers = Map.of("Authorization", "Bearer " + oAuth2Token.getAccessToken(), "Accept", "application/json");
        try {
            HttpData httpData = JSON_INTERFACE_HELPER.getForData(providerConfig.getUserInfoUri(), headers, null);
            if (httpData.getStatusCode() != 200) {
                return ResponseData.errorCode(OAuth2ClientResponseCode.INVALID_HTTP_CODE, httpData.getStatusCode(), httpData.getErrorInfo());
            }
            // 解析用户信息
            return parseUserInfoResponse(httpData.getResponseData());
        } catch (Exception e) {
            return ResponseData.errorCode(OAuth2ClientResponseCode.HTTP_REQUEST_FAILED, e.toString());
        }
    }

    /**
     * 获取配置信息
     *
     * @return 配置信息
     */
    @Override
    public OAuth2ClientProperties.ProviderConfig getProviderConfig() {
        return providerConfig;
    }

    /**
     * 生成唯一的状态ID。
     *
     * @return 生成唯一的状态ID。
     */
    protected String generateStateId(String authType) {
        // 生成唯一状态ID。
        return new OAuth2StateId(providerCode, authType, String.valueOf(SnowflakeIdGenerator.getInstance().generateId())).toString();
    }

    /**
     * 设置状态ID。
     */
    protected void setAuthState(String authStateId, OAuth2ClientAuthStatus authStatus) {
        GlobalCache.put(STATE_ID_KEY, authStateId, authStatus.name(), STATE_ID_EXPIRE_TIME);
    }

    /**
     * 检查状态ID是否处在某个状态上。
     */
    protected boolean checkAuthState(String authStateId, OAuth2ClientAuthStatus authStatus) {
        return getAuthState(authStateId) == authStatus;
    }

    /**
     * 获取状态ID。
     */
    @Override
    public OAuth2ClientAuthStatus getAuthState(String authStateId) {
        CacheValueWrapper<String> cacheValueWrapper = GlobalCache.get(STATE_ID_KEY, authStateId, String.class);
        if (cacheValueWrapper == null) {
            return OAuth2ClientAuthStatus.EXPIRED;
        } else {
            return OAuth2ClientAuthStatus.valueOf(cacheValueWrapper.getValue());
        }
    }

    /**
     * 删除状态ID。
     *
     * @param authStateId 状态ID。
     */
    @Override
    public void invalidateAuthState(String authStateId) {
        GlobalCache.invalidate(STATE_ID_KEY, authStateId);
    }

    /**
     * 解析ID令牌。
     *
     * @param idToken ID令牌。
     * @return 解析结果
     */
    protected Map<String, Object> parseIdToken(String idToken) {
        if (StringUtils.isBlank(idToken)) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        //尝试解析idToken
        DecodedJWT jwt = JWT.decode(idToken);
        map.put("openId", jwt.getSubject());
        map.put("issuer", jwt.getIssuer());
        jwt.getClaims().forEach((k, v) -> {
            map.put(k, v.asString());
        });
        return map;
    }

    /**
     * 添加授权URL参数。
     *
     * @param params 参数映射
     */
    protected void addAuthParam(Map<String, String> params) {
    }

    /**
     * 添加获取Token参数。
     *
     * @param params 获取Token参数映射。
     */
    protected void addTokenParam(Map<String, String> params) {
    }

    /**
     * 解析令牌响应。
     *
     * @param responseBody 响应体
     * @return OAuth2令牌
     */
    protected abstract ResponseData<OAuth2Token> parseTokenResponse(String responseBody);

    /**
     * 解析用户信息响应。
     *
     * @param responseBody 响应体
     * @return OAuth2用户信息
     */
    protected abstract ResponseData<OAuth2UserInfo> parseUserInfoResponse(String responseBody);
}