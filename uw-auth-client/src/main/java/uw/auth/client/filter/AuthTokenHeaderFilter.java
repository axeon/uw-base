package uw.auth.client.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import uw.auth.client.helper.AuthClientTokenHelper;

/**
 * 添加token到header。
 * 主要用于webClient。
 *
 */
public class AuthTokenHeaderFilter implements ExchangeFilterFunction {
    private static final Logger log = LoggerFactory.getLogger(AuthTokenHeaderFilter.class);

    /**
     * token header
     */
    public static final String AUTH_TOKEN_HEADER_NAME = "Authorization";
    /**
     * token header value prefix
     */
    public static final String AUTH_TOKEN_HEADER_VALUE_PREFIX = "Bearer ";

    /**
     * token helper
     */
    private final AuthClientTokenHelper authClientTokenHelper;

    /**
     * 构造函数
     *
     * @param authClientTokenHelper
     */
    public AuthTokenHeaderFilter(final AuthClientTokenHelper authClientTokenHelper) {
        this.authClientTokenHelper = authClientTokenHelper;
    }

    /**
     * 拦截器
     *
     * @param request
     * @param next
     * @return
     */
    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        // 1. 第一次请求
        return exchange(request, next, authClientTokenHelper.getToken())
                .flatMap(resp -> {
                    if (resp.statusCode() == HttpStatus.UNAUTHORIZED) {
                        log.warn("token 失效，触发刷新；uri={}", request.url());
                        return exchange(request, next, authClientTokenHelper.getToken());
                    }
                    return Mono.just(resp);
                });
    }

    /**
     * 尝试刷新token
     *
     * @param req
     * @param next
     * @param token
     * @return
     */
    private Mono<ClientResponse> exchange(ClientRequest req, ExchangeFunction next, String token) {
        ClientRequest mutated = ClientRequest.from(req)
                .headers(h -> h.set(AUTH_TOKEN_HEADER_NAME, AUTH_TOKEN_HEADER_VALUE_PREFIX + token))
                .build();
        return next.exchange(mutated);
    }
}
