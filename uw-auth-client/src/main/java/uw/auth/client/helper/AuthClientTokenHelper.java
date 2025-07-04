package uw.auth.client.helper;

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

/**
 * Token工具类
 */
public class AuthClientTokenHelper {

    /**
     * 重试次数。
     */
    private static final int MAX_RETRY_TIMES = 10;

    /**
     * 日志记录。
     */
    private static final Logger log = LoggerFactory.getLogger(AuthClientTokenHelper.class);

    /**
     * 认证客户端配置。
     */
    private final AuthClientProperties authClientProperties;

    /**
     * RestTemplate。
     */
    private final RestTemplate restTemplate;

    /**
     * Access_Token
     */
    private String token;

    /**
     * Refresh_Token
     */
    private String refreshToken;

    /**
     * Token过期时间,单位为毫秒
     */
    private long expiresAt;

    /**
     * 出错重试次数。
     */
    private int retryTimes;


    public AuthClientTokenHelper(AuthClientProperties authClientProperties, RestTemplate restTemplate) {
        this.authClientProperties = authClientProperties;
        this.restTemplate = restTemplate;
    }

    /**
     * 获取token。
     * 1.如果发现登录状态错误，先尝试刷新token。
     *
     * @return token
     */
    public String getToken() {
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            if (token == null) {
                if (refreshToken == null) {
                    login();
                } else {
                    refresh();
                }
            } else {
                //即将过期的，进入刷新token。
                if (expiresAt <= SystemClock.now()) {
                    invalidate();
                    refresh();
                }
            }
            //如果token=null，进入重试。
            if (token == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }
        return token;
    }

    /**
     * 作废token。
     */
    public void invalidate() {
        this.token = null;
    }

    /**
     * 账号密码登录认证。
     */
    private synchronized void login() {
        if (retryTimes > 1 && retryTimes % 10 == 0) {
            log.error("!!!AuthClient获取token出错已超过{}次，请检查配置！", retryTimes);
        }
        //如果状态正常了，不再执行，防止重复执行。
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
                if (loginResponse != null) {
                    if (loginResponse.isSuccess()) {
                        List<TokenResponse> tokenList = loginResponse.getData();
                        if (tokenList != null && !tokenList.isEmpty()) {
                            TokenResponse tokenResponse = tokenList.getFirst();
                            if (tokenResponse != null) {
                                token = tokenResponse.getToken();
                                refreshToken = tokenResponse.getRefreshToken();
                                long expiresIn = tokenResponse.getExpiresIn();
                                long createAt = tokenResponse.getCreateAt();
                                // 预留5分钟缓冲。
                                expiresAt = (createAt - 300_000L) + expiresIn;
                                if (StringUtils.isNotBlank(token) && StringUtils.isNotBlank(refreshToken) && expiresAt > SystemClock.now()) {
                                    retryTimes = 0;
                                }
                            }
                        }
                    }
                }
            }
            if (StringUtils.isBlank(token)) {
                log.error("!!!AuthClient登录出错! response: {}", loginResponseEntity.toString());
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
     * 刷新token操作。
     */
    private synchronized void refresh() {
        if (retryTimes > 1 && retryTimes % 10 == 0) {
            log.error("!!!AuthClient获取token出错已超过{}次，请检查配置！", retryTimes);
        }
        //如果状态正常了，不再执行，防止重复执行。
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
            //刷新token成功以后，更新token
            if (responseEntity.getStatusCode().value() == HttpStatus.OK.value()) {
                ResponseData<TokenResponse> responseBody = responseEntity.getBody();
                if (responseBody != null && responseBody.isSuccess()) {
                    TokenResponse tokenResponse = responseBody.getData();
                    if (tokenResponse != null) {
                        token = tokenResponse.getToken();
                        long expiresIn = tokenResponse.getExpiresIn();
                        long createAt = tokenResponse.getCreateAt();
                        // 预留5分钟缓冲。
                        expiresAt = (createAt - 300_000L) + expiresIn;
                        if (StringUtils.isNotBlank(token) && expiresAt > SystemClock.now()) {
                            retryTimes = 0;
                        }
                    }
                }
            }
            if (StringUtils.isBlank(token)) {
                log.error("!!!AuthClient刷新token出错! response: {}", responseEntity.toString());
            }
        } catch (Throwable e) {
            log.error("!!!AuthClient刷新token出错! {}", e.getMessage(), e);
        }
        if (StringUtils.isBlank(token)) {
            refreshToken = null;
            expiresAt = 0;
        }
    }

}
