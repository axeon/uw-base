package uw.auth.client.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uw.auth.client.AuthClientProperties;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Token工具类
 */
public class AuthClientToken {

    /**
     * 重试次数。
     */
    private static final int MAX_RETRY_TIMES = 10;

    private static final Logger log = LoggerFactory.getLogger( AuthClientToken.class );

    private final AuthClientProperties authClientProperties;

    private final ObjectMapper objectMapper;

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


    public AuthClientToken(AuthClientProperties authClientProperties, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.authClientProperties = authClientProperties;
        this.objectMapper = objectMapper;
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
                if (expiresAt <= System.currentTimeMillis()) {
                    invalidate();
                    refresh();
                }
            }
            //如果token=null，进入重试。
            if (token == null) {
                try {
                    Thread.sleep( 1000 );
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
            log.error( "!!!AuthClient获取token出错已超过{}次，请检查配置！", retryTimes );
        }
        //如果状态正常了，不再执行，防止重复执行。
        if (StringUtils.isNotBlank( token )) {
            return;
        }
        retryTimes++;
        try {
            String loginUrl = authClientProperties.getAuthCenterHost() + authClientProperties.getLoginEntryPoint();
            Map<String, String> credentialsMap = new HashMap<>( 6 );
            credentialsMap.put( "loginType", "1" );
            credentialsMap.put( "loginAgent",
                    authClientProperties.getAppName() + ":" + authClientProperties.getAppVersion() + "/" + authClientProperties.getAppHost() + ":" + authClientProperties.getAppPort() );
            credentialsMap.put( "loginId", authClientProperties.getLoginId() );
            credentialsMap.put( "loginPass", authClientProperties.getLoginPass() );
            credentialsMap.put( "loginSecret", authClientProperties.getLoginSecret() );
            credentialsMap.put( "userType", String.valueOf( authClientProperties.getUserType() ) );
            credentialsMap.put( "forceLogin", "true");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType( MediaType.APPLICATION_JSON );
            String credentials = objectMapper.writeValueAsString( credentialsMap );
            RequestEntity<String> requestEntity = new RequestEntity<>( credentials, headers, HttpMethod.POST, URI.create( loginUrl ) );
            ResponseEntity<Map> loginResponse = restTemplate.exchange( requestEntity, Map.class );
            if (loginResponse.getStatusCode().value() == HttpStatus.OK.value()) {
                Map responseBody = loginResponse.getBody();
                if (responseBody != null) {
                    if ("success".equals( responseBody.get( "state" ) )) {
                        List tokenList = (List) responseBody.get( "data" );
                        if (tokenList != null && !tokenList.isEmpty()) {
                            Map tokenMap = (Map) tokenList.get( 0 );
                            if (tokenMap != null && tokenMap.get( "token" ) != null) {
                                token = String.valueOf( tokenMap.get( "token" ) );
                                refreshToken = String.valueOf( tokenMap.get( "refreshToken" ) );
                                long expiresIn = Long.parseLong( String.valueOf( tokenMap.get( "expiresIn" ) ) );
                                long createAt = Long.parseLong( String.valueOf( tokenMap.get( "createAt" ) ) );
                                // 预留5分钟缓冲。
                                expiresAt = (createAt - 300_000L) + expiresIn;
                                if (StringUtils.isNotBlank( token ) && StringUtils.isNotBlank( refreshToken ) && expiresAt > System.currentTimeMillis()) {
                                    retryTimes = 0;
                                }
                            }
                        }
                    }
                }
            }
            if (StringUtils.isBlank( token )) {
                log.error( "!!!AuthClient登录出错! response: {}", loginResponse.toString() );
            }
        } catch (Throwable e) {
            log.error( "!!!AuthClient登录出错! {}", e.getMessage(), e );
        }
        if (StringUtils.isBlank( token )) {
            refreshToken = null;
            expiresAt = 0;
        }
    }

    /**
     * 刷新token操作。
     */
    private synchronized void refresh() {
        if (retryTimes > 1 && retryTimes % 10 == 0) {
            log.error( "!!!AuthClient获取token出错已超过{}次，请检查配置！", retryTimes );
        }
        //如果状态正常了，不再执行，防止重复执行。
        if (StringUtils.isNotBlank( token )) {
            return;
        }
        retryTimes++;
        try {
            String refreshTokenUrl = authClientProperties.getAuthCenterHost() + authClientProperties.getRefreshEntryPoint();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType( MediaType.APPLICATION_FORM_URLENCODED );
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add( "refreshToken", refreshToken );
            map.add( "loginAgent",
                    authClientProperties.getAppName() + ":" + authClientProperties.getAppVersion() + "/" + authClientProperties.getAppHost() + ":" + authClientProperties.getAppPort() );
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>( map, headers );
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity( refreshTokenUrl, request, Map.class );
            //刷新token成功以后，更新token
            if (responseEntity.getStatusCode().value() == HttpStatus.OK.value()) {
                Map responseBody = responseEntity.getBody();
                if (responseBody != null) {
                    if ("success".equals( responseBody.get( "state" ) )) {
                        Map tokenMap = (Map) responseBody.get( "data" );
                        if (tokenMap != null && tokenMap.get( "token" ) != null) {
                            token = String.valueOf( tokenMap.get( "token" ) );
                            long expiresIn = Long.parseLong( String.valueOf( tokenMap.get( "expiresIn" ) ) );
                            long createAt = Long.parseLong( String.valueOf( tokenMap.get( "createAt" ) ) );
                            // 预留5分钟缓冲。
                            expiresAt = (createAt - 300_000L) + expiresIn;
                            if (StringUtils.isNotBlank( token ) && expiresAt > System.currentTimeMillis()) {
                                retryTimes = 0;
                            }
                        }
                    }
                }
            }
            if (StringUtils.isBlank( token )) {
                log.error( "!!!AuthClient刷新token出错! response: {}", responseEntity.toString() );
            }
        } catch (Throwable e) {
            log.error( "!!!AuthClient刷新token出错! {}", e.getMessage(), e );
        }
        if (StringUtils.isBlank( token )) {
            refreshToken = null;
            expiresAt = 0;
        }
    }

}
