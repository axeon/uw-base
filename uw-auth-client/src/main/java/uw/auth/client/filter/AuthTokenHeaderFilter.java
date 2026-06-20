package uw.auth.client.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import uw.auth.client.constant.AuthClientConstants;
import uw.auth.client.service.AuthClientTokenService;

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
    private final AuthClientTokenService authClientTokenService;

    /**
     * 构造函数
     *
     * @param authClientTokenService
     */
    public AuthTokenHeaderFilter(final AuthClientTokenService authClientTokenService) {
        this.authClientTokenService = authClientTokenService;
    }

    /**
     * 拦截器。
     * <p>
     * 使用 {@link Mono#defer(java.util.function.Supplier)} 延迟到订阅时才获取 token，
     * 避免在组装阶段（可能处于响应式调度线程）同步阻塞地访问 token 服务。
     * 收到 401/498 时作废 token 并重试一次。
     *
     * @param request
     * @param next
     * @return
     */
    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return Mono.defer(() -> exchange(request, next, authClientTokenService.getToken()))
                .flatMap(resp -> {
                    int code = resp.statusCode().value();
                    if (code == AuthClientConstants.HTTP_UNAUTHORIZED_CODE || code == AuthClientConstants.HTTP_TOKEN_EXPIRED_CODE) {
                        log.warn("token 失效，触发刷新；uri={}", request.url());
                        authClientTokenService.invalidate();
                        return exchange(request, next, authClientTokenService.getToken());
                    }
                    return Mono.just(resp);
                });
    }

    /**
     * 用指定 token 构造带 Authorization 头的请求并发起交换。
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
