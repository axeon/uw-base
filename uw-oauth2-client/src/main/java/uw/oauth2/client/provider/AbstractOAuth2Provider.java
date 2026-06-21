package uw.oauth2.client.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;
import uw.cache.GlobalCache;
import uw.cache.vo.CacheValueWrapper;
import uw.common.response.ResponseData;
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
 * <p>
 * 具体的Provider可以继承此类，只需要实现特定平台的差异化逻辑（覆盖
 * {@link #addAuthParam}、{@link #addTokenParam}、{@link #parseTokenResponse}、
 * {@link #parseUserInfoResponse} 等钩子方法）。
 * <p>
 * 授权状态（stateId）通过 {@link GlobalCache} 统一存储，有效期300秒，用于扫码轮询与CSRF防护。
 *
 * @author axeon
 */
public abstract class AbstractOAuth2Provider implements OAuth2Provider {

    /**
     * 状态ID在GlobalCache中的缓存Key（命名空间）。
     */
    private static final String STATE_ID_KEY = "uw.oauth.state";

    /**
     * 状态ID有效期（毫秒），默认5分钟。
     */
    private static final long STATE_ID_EXPIRE_TIME = 300 * 1000;

    /**
     * 日志。
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * JSON接口帮助类，各Provider共享一个HTTP客户端实例（30s连接/读取，60s写入，信任全部证书）。
     */
    protected final HttpInterface JSON_INTERFACE_HELPER = new JsonInterfaceHelper(HttpConfig.builder().connectTimeout(30000).readTimeout(30000).writeTimeout(60000).sslSocketFactory(SSLContextUtils.getTruestAllSocketFactory()).trustManager(SSLContextUtils.getTrustAllManager()).hostnameVerifier((s, sslSession) -> true).build());

    /**
     * Provider代码（如google、wechat等）。
     */
    protected final String providerCode;

    /**
     * Provider配置（clientId/secret及各平台URL）。
     */
    protected final OAuth2ClientProperties.ProviderConfig providerConfig;

    /**
     * 重定向URI，接收第三方授权回调。
     */
    protected final String redirectUri;

    /**
     * 二维码URI（扫码登录入口，必须以"/"结尾）。
     */
    protected final String qrcodeUri;

    /**
     * 构造函数。
     *
     * @param providerCode   Provider名称
     * @param providerConfig Provider配置
     * @param redirectUri    重定向URI
     * @param qrcodeUri      二维码URI
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
     * 构建授权URL，引导用户跳转到第三方授权页面。
     * <p>
     * 当 {@code authStateId} 为空时自动生成新的状态ID（授权类型为auth），
     * 并将state置为SCANNED状态存入缓存，同时拼装client_id、redirect_uri、
     * response_type、scope、state等标准参数。
     *
     * @param authStateId 状态ID，为空时自动生成
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
     * 生成扫码登录二维码URL。
     * <p>
     * 内部生成授权类型为qrcode的状态ID并置为WAITING状态，
     * 返回的URL供前端渲染成二维码，用户扫码后由手机端走网页授权流程。
     *
     * @return 二维码URL（qrcodeUri + authStateId）
     */
    @Override
    public String buildQrCode() {
        String authStateId = generateStateId(OAuth2ClientAuthType.QRCODE.toString().toLowerCase());
        setAuthState(authStateId, OAuth2ClientAuthStatus.WAITING);
        return this.qrcodeUri + authStateId;
    }

    /**
     * 使用授权码换取访问令牌。
     * <p>
     * 先校验 {@code authStateId} 必须处于SCANNED状态（CSRF防护），
     * 校验通过后向tokenUri发起表单POST请求；HTTP状态码非200或抛异常时清理state，
     * 成功时将state推进到CONFIRMED并解析响应。
     *
     * @param authCode    授权码
     * @param authStateId 状态参数
     * @param extParam    扩展参数（按Provider差异化使用，可为null）
     * @return Token响应；state无效、HTTP失败时返回对应错误码
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
                // 换Token失败，清理state避免被重复利用。
                invalidateAuthState(authStateId);
                return ResponseData.errorCode(OAuth2ClientResponseCode.INVALID_HTTP_CODE, httpData.getStatusCode(), httpData.getResponseData());
            }
            setAuthState(authStateId, OAuth2ClientAuthStatus.CONFIRMED);
            return parseTokenResponse(httpData.getResponseData());
        } catch (Exception e) {
            // 异常时同样清理state。
            invalidateAuthState(authStateId);
            return ResponseData.errorCode(OAuth2ClientResponseCode.HTTP_REQUEST_FAILED, e.toString());
        }
    }

    /**
     * 使用访问令牌获取用户信息。
     * <p>
     * 以Bearer方式携带accessToken请求userInfoUri，HTTP状态码非200时返回错误。
     *
     * @param oAuth2Token 访问令牌
     * @return 用户信息；HTTP失败时返回对应错误码
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
     * 生成全局唯一的状态ID。
     * <p>
     * 由 providerCode、authType 和 Snowflake顺序ID 组成。
     *
     * @param authType 认证类型（auth/qrcode）
     * @return 状态ID字符串
     */
    protected String generateStateId(String authType) {
        // 生成唯一状态ID。
        return new OAuth2StateId(providerCode, authType, String.valueOf(SnowflakeIdGenerator.getInstance().generateId())).toString();
    }

    /**
     * 将状态ID与其当前授权状态写入GlobalCache，有效期5分钟。
     *
     * @param authStateId 状态ID
     * @param authStatus  授权状态
     */
    protected void setAuthState(String authStateId, OAuth2ClientAuthStatus authStatus) {
        GlobalCache.put(STATE_ID_KEY, authStateId, authStatus.name(), STATE_ID_EXPIRE_TIME);
    }

    /**
     * 检查状态ID是否处于指定状态。
     *
     * @param authStateId 状态ID
     * @param authStatus  期望的授权状态
     * @return 当前状态与期望一致时返回true
     */
    protected boolean checkAuthState(String authStateId, OAuth2ClientAuthStatus authStatus) {
        return getAuthState(authStateId) == authStatus;
    }

    /**
     * 获取状态ID对应的授权状态。
     * <p>
     * state不存在返回EXPIRED；缓存值非法（脏数据或枚举改名）时容错返回FAILED并打印告警。
     *
     * @param authStateId 状态ID
     * @return 授权状态
     */
    @Override
    public OAuth2ClientAuthStatus getAuthState(String authStateId) {
        CacheValueWrapper<String> cacheValueWrapper = GlobalCache.get(STATE_ID_KEY, authStateId, String.class);
        if (cacheValueWrapper == null) {
            return OAuth2ClientAuthStatus.EXPIRED;
        }
        try {
            return OAuth2ClientAuthStatus.valueOf(cacheValueWrapper.getValue());
        } catch (IllegalArgumentException e) {
            // 缓存值为非法枚举（脏数据或版本升级枚举改名），按失败处理。
            logger.warn("Invalid auth state value [{}] for stateId [{}], treating as FAILED", cacheValueWrapper.getValue(), authStateId);
            return OAuth2ClientAuthStatus.FAILED;
        }
    }

    /**
     * 删除状态ID，使其立即失效。
     *
     * @param authStateId 状态ID
     */
    @Override
    public void invalidateAuthState(String authStateId) {
        GlobalCache.invalidate(STATE_ID_KEY, authStateId);
    }

    /**
     * 解析ID令牌（OpenID Connect的id_token）。
     * <p>
     * <b>注意：</b>本方法仅做JWT解码提取sub/iss等声明，<b>不校验签名</b>。
     * 若上层依赖openId做账号绑定或登录，调用方必须自行用Provider公钥验签后才能信任返回的openId。
     *
     * @param idToken ID令牌
     * @return 解析结果Map（含openId、issuer及各声明）；idToken为空时返回null
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
     * 添加授权URL的平台特有参数（钩子方法）。
     * <p>
     * 默认实现为空，子类可覆盖以增删标准参数（如微信把client_id改为appid）。
     *
     * @param params 参数映射
     */
    protected void addAuthParam(Map<String, String> params) {
    }

    /**
     * 添加获取Token的平台特有参数（钩子方法）。
     * <p>
     * 默认实现为空，子类可覆盖以增删标准参数。
     *
     * @param params 获取Token参数映射
     */
    protected void addTokenParam(Map<String, String> params) {
    }

    /**
     * 解析令牌响应（由子类实现具体平台的解析逻辑）。
     *
     * @param responseBody 响应体
     * @return OAuth2令牌
     */
    protected abstract ResponseData<OAuth2Token> parseTokenResponse(String responseBody);

    /**
     * 解析用户信息响应（由子类实现具体平台的解析逻辑）。
     *
     * @param responseBody 响应体
     * @return OAuth2用户信息
     */
    protected abstract ResponseData<OAuth2UserInfo> parseUserInfoResponse(String responseBody);
}