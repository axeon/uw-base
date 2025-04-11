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
 * HTTP请求拦截器，用于添加token认证所需头信息
 */
public class AuthTokenHeaderInterceptor implements ClientHttpRequestInterceptor {

    private final AuthClientTokenHelper authClientTokenHelper;

    public AuthTokenHeaderInterceptor(final AuthClientTokenHelper authClientTokenHelper) {
        this.authClientTokenHelper = authClientTokenHelper;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = request.getHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + authClientTokenHelper.getToken());
        ClientHttpResponse response = execution.execute(request, body);
        //如果是无法验证的状态，则直接作废token。
        int code = response.getStatusCode().value();
        if (code == AuthClientConstants.HTTP_UNAUTHORIZED_CODE || code == AuthClientConstants.HTTP_TOKEN_EXPIRED_CODE) {
            authClientTokenHelper.invalidate();
            //再次重试
            headers.set("Authorization", "Bearer " + authClientTokenHelper.getToken());
            response = execution.execute(request, body);
        }
        return response;
    }
}
