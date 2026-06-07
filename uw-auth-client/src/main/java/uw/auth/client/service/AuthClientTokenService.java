package uw.auth.client.service;

import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uw.auth.client.conf.AuthClientProperties;
import uw.auth.client.constant.LoginType;
import uw.auth.client.vo.LoginRequest;
import uw.auth.client.vo.TokenResponse;
import uw.common.dto.ResponseData;
import uw.common.util.JsonUtils;
import uw.common.util.SystemClock;

import java.net.URI;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 服务间认证Token管理器。
 * <p>
 * 负责向认证中心获取和刷新Access Token，并通过{@code authRestTemplate} / {@code authWebClient}
 * 自动为每个请求注入Authorization头。
 * <p>
 * 并发模型：
 * <ul>
 *   <li>使用{@link ReentrantReadWriteLock}，token有效时多线程并发读（读锁），零阻塞</li>
 *   <li>token失效/过期时升级为写锁，仅一个线程执行网络刷新，其余线程等待后直接拿到新token</li>
 *   <li>字段均使用{@code volatile}修饰，保证写锁释放后读锁线程立即可见</li>
 * </ul>
 *
 * @see AuthClientProperties
 */
public class AuthClientTokenService {

    /**
     * 获取token的最大重试次数。
     */
    private static final int MAX_RETRY_TIMES = 10;

    private static final Logger log = LoggerFactory.getLogger(AuthClientTokenService.class);

    /**
     * 认证客户端配置，包含认证中心地址、登录凭证等信息。
     */
    private final AuthClientProperties authClientProperties;

    /**
     * 用于调用认证中心登录/刷新接口的HTTP客户端。
     */
    private final RestTemplate restTemplate;

    /**
     * 读写锁，token有效时允许多线程并发读，失效/过期时写锁互斥刷新。
     */
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * 读锁引用，用于{@link #getToken()}中快速检查token有效性。
     */
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();

    /**
     * 写锁引用，用于{@link #invalidate()}和{@link #getTokenWithWrite()}中执行token刷新。
     */
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    /**
     * Access Token，由认证中心签发，用于服务间调用的身份凭证。
     */
    private volatile String token;

    /**
     * Refresh Token，用于在Access Token过期时向认证中心换取新Token。
     */
    private volatile String refreshToken;

    /**
     * Token过期时间戳（毫秒），已扣除{@code refreshAdvanceMillis}提前刷新缓冲。
     * 当{@code SystemClock.now() >= expiresAt}时判定为过期。
     */
    private volatile long expiresAt;

    /**
     * 连续获取token失败的累计次数，成功时重置为0。
     * 每累计10次输出一次错误日志。
     */
    private int retryTimes;

    /**
     * @param authClientProperties 认证客户端配置
     * @param restTemplate         用于调用认证中心的RestTemplate
     */
    public AuthClientTokenService(AuthClientProperties authClientProperties, RestTemplate restTemplate) {
        this.authClientProperties = authClientProperties;
        this.restTemplate = restTemplate;
    }

    /**
     * 获取有效的Access Token。
     * <p>
     * token有效时通过读锁并发返回；失效或过期时升级为写锁执行刷新。
     *
     * @return 有效的token，若刷新失败则返回null
     */
    public String getToken() {
        readLock.lock();
        try {
            if (isTokenValid()) {
                return token;
            }
        } finally {
            readLock.unlock();
        }
        return getTokenWithWrite();
    }

    /**
     * 将当前token标记为失效。
     * <p>
     * 下次调用{@link #getToken()}时将触发刷新。通常在收到401/498响应时由拦截器调用。
     */
    public void invalidate() {
        writeLock.lock();
        try {
            this.token = null;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 检查当前token是否有效（非空且未过期）。
     */
    private boolean isTokenValid() {
        return token != null && expiresAt > SystemClock.now();
    }

    /**
     * 写锁路径：获取token失败或过期时，在写锁保护下执行刷新。
     * <p>
     * 进入写锁后先double-check（可能其他线程已刷新成功），然后最多重试{@value #MAX_RETRY_TIMES}次。
     * 重试间隔1秒，避免认证中心不可用时密集请求。
     */
    private String getTokenWithWrite() {
        writeLock.lock();
        try {
            // double-check: 可能其他线程已经刷新好了
            if (isTokenValid()) {
                return token;
            }
            for (int i = 0; i < MAX_RETRY_TIMES; i++) {
                if (token == null) {
                    if (refreshToken == null) {
                        login();
                    } else {
                        refresh();
                    }
                } else if (expiresAt <= SystemClock.now()) {
                    token = null;
                    refresh();
                }
                if (token != null) {
                    return token;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            return token;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 账号密码登录，获取新的Access Token和Refresh Token。
     * <p>
     * 内含守卫检查：若token已有效则跳过。失败时清空refreshToken和expiresAt。
     */
    private void login() {
        if (retryTimes > 1 && retryTimes % 10 == 0) {
            log.error("!!!AuthClient获取token出错已超过{}次，请检查配置！", retryTimes);
        }
        if (StringUtils.isNotBlank(token)) {
            return;
        }
        retryTimes++;
        try {
            String loginUrl = authClientProperties.getAuthCenterHost() + authClientProperties.getLoginEntryPoint();
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setLoginType(LoginType.USER_PASS.getValue());
            loginRequest.setSaasId(authClientProperties.getSaasId());
            loginRequest.setLoginAgent(authClientProperties.getAppName() + ":" + authClientProperties.getAppVersion() + "/" + authClientProperties.getAppHost() + ":" + authClientProperties.getAppPort());
            loginRequest.setUserType(authClientProperties.getUserType());
            loginRequest.setLoginId(authClientProperties.getLoginId());
            loginRequest.setLoginPass(authClientProperties.getLoginPass());
            loginRequest.setLoginSecret(authClientProperties.getLoginSecret());
            loginRequest.setForceLogin(true);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String credentials = JsonUtils.toString(loginRequest);
            RequestEntity<String> requestEntity = new RequestEntity<>(credentials, headers, HttpMethod.POST, URI.create(loginUrl));
            ResponseEntity<ResponseData<List<TokenResponse>>> loginResponseEntity = restTemplate.exchange(requestEntity,
                    new ParameterizedTypeReference<ResponseData<List<TokenResponse>>>() {
                    });
            if (loginResponseEntity.getStatusCode().value() == HttpStatus.OK.value()) {
                ResponseData<List<TokenResponse>> loginResponse = loginResponseEntity.getBody();
                if (loginResponse != null && loginResponse.isSuccess()) {
                    List<TokenResponse> tokenList = loginResponse.getData();
                    if (tokenList != null && !tokenList.isEmpty()) {
                        TokenResponse tokenResponse = tokenList.getFirst();
                        if (tokenResponse != null) {
                            applyToken(tokenResponse.getToken(), tokenResponse.getRefreshToken(),
                                    tokenResponse.getCreateAt(), tokenResponse.getExpiresIn());
                        }
                    }
                }
            }
            if (StringUtils.isBlank(token)) {
                log.error("!!!AuthClient登录出错! response: {}", loginResponseEntity);
            }
        } catch (Throwable e) {
            log.error("!!!AuthClient登录出错! {}", e.getMessage(), e);
        }
        if (StringUtils.isBlank(token)) {
            refreshToken = null;
            expiresAt = 0;
        }
    }

    /**
     * 使用Refresh Token刷新Access Token。
     * <p>
     * 内含守卫检查：若token已有效则跳过。服务端可能同时轮换Refresh Token。
     * 失败时清空refreshToken和expiresAt，下次将触发全量login。
     */
    private void refresh() {
        if (retryTimes > 1 && retryTimes % 10 == 0) {
            log.error("!!!AuthClient获取token出错已超过{}次，请检查配置！", retryTimes);
        }
        if (StringUtils.isNotBlank(token)) {
            return;
        }
        retryTimes++;
        try {
            String refreshTokenUrl = authClientProperties.getAuthCenterHost() + authClientProperties.getRefreshEntryPoint();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("refreshToken", refreshToken);
            map.add("loginAgent",
                    authClientProperties.getAppName() + ":" + authClientProperties.getAppVersion() + "/" + authClientProperties.getAppHost() + ":" + authClientProperties.getAppPort());
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<ResponseData<TokenResponse>> responseEntity = restTemplate.exchange(refreshTokenUrl, HttpMethod.POST, request,
                    new ParameterizedTypeReference<ResponseData<TokenResponse>>() {
                    });
            if (responseEntity.getStatusCode().value() == HttpStatus.OK.value()) {
                ResponseData<TokenResponse> responseBody = responseEntity.getBody();
                if (responseBody != null && responseBody.isSuccess()) {
                    TokenResponse tokenResponse = responseBody.getData();
                    if (tokenResponse != null) {
                        applyToken(tokenResponse.getToken(), tokenResponse.getRefreshToken(),
                                tokenResponse.getCreateAt(), tokenResponse.getExpiresIn());
                    }
                }
            }
            if (StringUtils.isBlank(token)) {
                log.error("!!!AuthClient刷新token出错! response: {}", responseEntity);
            }
        } catch (Throwable e) {
            log.error("!!!AuthClient刷新token出错! {}", e.getMessage(), e);
        }
        if (StringUtils.isBlank(token)) {
            refreshToken = null;
            expiresAt = 0;
        }
    }

    /**
     * 应用服务端返回的token信息，更新本地缓存。
     * <p>
     * 过期时间 = createAt + expiresIn - refreshAdvanceMillis（提前刷新缓冲）。
     * 成功时重置retryTimes。
     */
    private void applyToken(String newToken, String newRefreshToken, long createAt, long expiresIn) {
        this.token = newToken;
        this.refreshToken = newRefreshToken;
        this.expiresAt = createAt - authClientProperties.getRefreshAdvanceMillis() + expiresIn;
        if (StringUtils.isNotBlank(token) && StringUtils.isNotBlank(refreshToken) && expiresAt > SystemClock.now()) {
            retryTimes = 0;
        }
    }

}
