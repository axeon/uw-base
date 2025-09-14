package uw.auth.client.interceptor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import uw.auth.client.constant.AuthClientConstants;
import uw.auth.client.helper.AuthClientTokenHelper;

import java.io.IOException;

/**
 * HTTP请求拦截器，用于添加token认证所需头信息。
 * 主要用于RestTemplate。
 */
public class AuthTokenHeaderInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 认证token头名称
     */
    public static final String AUTH_TOKEN_HEADER_NAME = "Authorization";

    /**
     * 认证token头值前缀
     */
    public static final String AUTH_TOKEN_HEADER_VALUE_PREFIX = "Bearer ";

    /**
     * 认证token工具类
     */
    private final AuthClientTokenHelper authClientTokenHelper;

    /**
     * 构造函数
     *
     * @param authClientTokenHelper 认证token工具类
     */
    public AuthTokenHeaderInterceptor(final AuthClientTokenHelper authClientTokenHelper) {
        this.authClientTokenHelper = authClientTokenHelper;
    }

    /**
     * 拦截处理
     *
     * @param request  请求
     * @param body     请求体
     * @param execution 执行器
     * @return 响应
     * @throws IOException 异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = request.getHeaders();
        if (headers.getContentType() == null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        headers.set(AUTH_TOKEN_HEADER_NAME, AUTH_TOKEN_HEADER_VALUE_PREFIX + authClientTokenHelper.getToken());
        ClientHttpResponse response = execution.execute(request, body);
        //如果是无法验证的状态，则直接作废token。
        int code = response.getStatusCode().value();
        if (code == AuthClientConstants.HTTP_UNAUTHORIZED_CODE || code == AuthClientConstants.HTTP_TOKEN_EXPIRED_CODE) {
            authClientTokenHelper.invalidate();
            //再次重试
            headers.set(AUTH_TOKEN_HEADER_NAME, AUTH_TOKEN_HEADER_VALUE_PREFIX + authClientTokenHelper.getToken());
            response = execution.execute(request, body);
        }
        return response;
    }
}
