package uw.auth.client.interceptor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import uw.auth.client.util.AuthClientTokenHelper;

import java.io.IOException;

/**
 * HTTP请求拦截器，用于添加token认证所需头信息
 */
public class TokenHeaderInterceptor implements ClientHttpRequestInterceptor {

    private final AuthClientTokenHelper authClientToken;

    public TokenHeaderInterceptor(final AuthClientTokenHelper authClientToken) {
        this.authClientToken = authClientToken;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = request.getHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + authClientToken.getToken());
        ClientHttpResponse response = execution.execute(request, body);
        //如果是无法验证的状态，则直接作废token。
        int code = response.getStatusCode().value();
        if (code == HttpStatus.UNAUTHORIZED.value() || code == HttpStatus.LOCKED.value()) {
            authClientToken.invalidate();
            //再次重试
            headers.set("Authorization", "Bearer " + authClientToken.getToken());
            response = execution.execute(request, body);
        }
        return response;
    }
}
