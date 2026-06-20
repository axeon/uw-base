package uw.auth.client.conf;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HeaderElement;
import org.apache.hc.core5.http.message.BasicHeaderElementIterator;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import uw.auth.client.filter.AuthTokenHeaderFilter;
import uw.auth.client.interceptor.AuthTokenHeaderInterceptor;
import uw.auth.client.service.AuthClientTokenService;

import java.util.ArrayList;
import java.util.List;

/**
 * uw-auth-client 自动装配类。
 * <p>
 * 装配两类HTTP客户端，职责分明，避免循环依赖：
 * <ul>
 *   <li><b>baseAuthClientRestClient</b> — 仅带LoadBalancer拦截器，用于调认证中心登录/刷新，
 *       不注入{@link AuthTokenHeaderInterceptor}，否则会形成 login→getToken→login 死循环</li>
 *   <li><b>authRestClient</b> — 带LoadBalancer + {@link AuthTokenHeaderInterceptor}，
 *       供业务服务间调用时自动附加Bearer Token</li>
 * </ul>
 * <p>
 * 关键点：{@code @LoadBalanced}必须标注在{@link RestClient.Builder}类型的Bean上，
 * 因为{@code LoadBalancerRestClientBuilderBeanPostProcessor.isSupported()}只识别{@code RestClient.Builder}，
 * 标注在{@code RestClient}上无效，服务名无法解析为实际实例。
 * <p>
 * Spring Cloud 开关：{@link AuthClientProperties#isEnableSpringCloud()}（默认 true）控制是否标注
 * {@code @LoadBalanced}。由于 {@code @LoadBalanced} 是声明式注解、无法在运行时按条件去除，
 * 这里通过内嵌静态配置类 {@link CloudLoadBalancedConfig}（带 {@code @ConditionalOnProperty}）
 * 提供 LoadBalanced 的 Builder，{@link PlainBuilderConfig} 提供不带 LB 的 Builder。
 *
 * @see AuthClientProperties
 * @see AuthClientTokenService
 * @see AuthTokenHeaderInterceptor
 */
@AutoConfiguration
@EnableConfigurationProperties({AuthClientProperties.class})
public class AuthClientAutoConfiguration {

    /**
     * 认证Token管理服务，负责向认证中心登录、刷新Token，并为外部请求提供有效Token。
     *
     * @param authClientProperties 认证客户端配置（认证中心地址、登录凭证等）
     * @param restClient           认证中心内部调用的RestClient（无认证拦截器）
     * @return Token管理服务实例
     */
    @Bean
    public AuthClientTokenService authClientTokenService(final AuthClientProperties authClientProperties,
                                                         @Qualifier("baseAuthClientRestClient") final RestClient restClient) {
        return new AuthClientTokenService(authClientProperties, restClient);
    }

    /**
     * 认证中心内部调用的RestClient。
     * <p>
     * 由{@link AuthClientTokenService}持有，用于调用认证中心的登录和Token刷新接口。
     * 不带认证拦截器，避免 login→getToken→login 死循环。
     *
     * @param authClientHttpRequestFactory HTTP连接池工厂
     * @param restClientBuilder            已被LoadBalancer增强（或直连）的Builder
     * @return 不带认证拦截器的RestClient
     */
    @Bean("baseAuthClientRestClient")
    public RestClient baseAuthClientRestClient(final ClientHttpRequestFactory authClientHttpRequestFactory,
                                               @Qualifier("baseAuthClientRestClientBuilder") final RestClient.Builder restClientBuilder) {
        return restClientBuilder.requestFactory(authClientHttpRequestFactory).build();
    }

    /**
     * 带认证拦截器的RestClient，供业务服务间调用使用。
     * <p>
     * 同时具备：
     * <ul>
     *   <li>LoadBalancer拦截器（若启用 Spring Cloud）— 将服务名解析为实际实例IP</li>
     *   <li>{@link AuthTokenHeaderInterceptor} — 自动附加{@code Authorization: Bearer &lt;token&gt;}，
     *       收到401/498时自动刷新Token并重试</li>
     * </ul>
     * 标记为{@code @Primary}。
     *
     * @param authClientHttpRequestFactory HTTP连接池工厂
     * @param authClientTokenService       Token管理服务
     * @param restClientBuilder            已被LoadBalancer增强（或直连）的Builder
     * @return 带负载均衡和认证拦截器的RestClient
     */
    @Bean("authRestClient")
    @Primary
    public RestClient authRestClient(final ClientHttpRequestFactory authClientHttpRequestFactory,
                                     final AuthClientTokenService authClientTokenService,
                                     @Qualifier("authRestClientBuilder") final RestClient.Builder restClientBuilder) {
        return restClientBuilder
                .requestFactory(authClientHttpRequestFactory)
                .requestInterceptor(new AuthTokenHeaderInterceptor(authClientTokenService))
                .build();
    }

    /**
     * 带认证过滤器的WebClient，供响应式场景的服务间调用使用。
     * <p>
     * 注意：WebClient 底层使用 Reactor Netty 的响应式连接器，与 RestClient 使用的阻塞式 Apache HttpClient5
     * 连接池相互独立（Apache HttpClient5 的同步 {@code CloseableHttpClient} 与异步
     * {@code CloseableHttpAsyncClient} 不共享连接池），无法复用 {@link #authClientHttpRequestFactory}。
     * 如需调整响应式连接行为，请在宿主工程自定义 {@code WebClient.Builder} 或
     * {@code ClientHttpConnector} Bean。
     *
     * @param webClientBuilder       已被LoadBalancer增强（或直连）的WebClient.Builder
     * @param authClientTokenService Token管理服务
     * @return 带负载均衡和认证过滤器的WebClient
     */
    @Bean("authWebClient")
    @Primary
    public WebClient authWebClient(@Qualifier("authWebClientBuilder") WebClient.Builder webClientBuilder,
                                   final AuthClientTokenService authClientTokenService) {
        return webClientBuilder.filter(new AuthTokenHeaderFilter(authClientTokenService)).build();
    }

    /**
     * 基于Apache HttpClient5的HTTP连接池工厂。
     * <p>
     * 配置项来自{@link AuthClientProperties.HttpPool}，包括：
     * <ul>
     *   <li>连接池大小、每路由最大连接数</li>
     *   <li>连接超时、Socket超时、获取连接超时</li>
     *   <li>Keep-Alive策略：优先使用服务端指定的timeout，否则使用配置的默认保活时间</li>
     * </ul>
     *
     * @param authClientProperties 认证客户端配置
     * @return 配置好连接池的ClientHttpRequestFactory
     */
    @Bean
    public ClientHttpRequestFactory authClientHttpRequestFactory(final AuthClientProperties authClientProperties) {
        AuthClientProperties.HttpPool pool = authClientProperties.getHttpPool();

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(pool.getMaxTotal())
                .setMaxConnPerRoute(pool.getDefaultMaxPerRoute())
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(pool.getConnectTimeout()))
                        .setSocketTimeout(Timeout.ofMilliseconds(pool.getSocketTimeout()))
                        .setValidateAfterInactivity(TimeValue.ofSeconds(30))
                        .build())
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(pool.getConnectionRequestTimeout()))
                .build();

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setKeepAliveStrategy((response, context) -> {
                    BasicHeaderElementIterator iterator = new BasicHeaderElementIterator(response.headerIterator("Keep-Alive"));
                    while (iterator.hasNext()) {
                        HeaderElement he = iterator.next();
                        String param = he.getName();
                        String value = he.getValue();
                        if (value != null && param.equalsIgnoreCase("timeout")) {
                            return TimeValue.ofSeconds(Long.parseLong(value));
                        }
                    }
                    return pool.getKeepAliveTimeIfNotPresent() > 0
                            ? TimeValue.ofMilliseconds(pool.getKeepAliveTimeIfNotPresent())
                            : TimeValue.ZERO_MILLISECONDS;
                })
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    /**
     * 构造统一的消息转换器列表。每次调用返回独立 List，避免多个 Builder 共享同一可变集合。
     */
    private static List<HttpMessageConverter<?>> buildMessageConverters() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        converters.add(new ByteArrayHttpMessageConverter());
        converters.add(new StringHttpMessageConverter());
        converters.add(new ResourceHttpMessageConverter());
        converters.add(new SourceHttpMessageConverter<>());
        converters.add(new AllEncompassingFormHttpMessageConverter());
        converters.add(new MappingJackson2HttpMessageConverter());
        return converters;
    }

    /**
     * Spring Cloud（LoadBalanced）模式：Builder 标注 {@code @LoadBalanced}。
     * 仅当 {@code uw.auth.client.enable-spring-cloud} 为 true（默认）时生效。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = "uw.auth.client", name = "enable-spring-cloud", havingValue = "true", matchIfMissing = true)
    static class CloudLoadBalancedConfig {

        @Bean("baseAuthClientRestClientBuilder")
        @LoadBalanced
        public RestClient.Builder baseAuthClientRestClientBuilder() {
            return RestClient.builder().messageConverters(buildMessageConverters());
        }

        @Bean("authRestClientBuilder")
        @LoadBalanced
        public RestClient.Builder authRestClientBuilder() {
            return RestClient.builder().messageConverters(buildMessageConverters());
        }

        @Bean("authWebClientBuilder")
        @LoadBalanced
        public WebClient.Builder authWebClientBuilder() {
            return WebClient.builder();
        }
    }

    /**
     * 非 Spring Cloud 模式：Builder 不标注 {@code @LoadBalanced}，使用直连 URL。
     * 仅当 {@code uw.auth.client.enable-spring-cloud} 为 false 时生效。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = "uw.auth.client", name = "enable-spring-cloud", havingValue = "false")
    static class PlainBuilderConfig {

        @Bean("baseAuthClientRestClientBuilder")
        public RestClient.Builder baseAuthClientRestClientBuilder() {
            return RestClient.builder().messageConverters(buildMessageConverters());
        }

        @Bean("authRestClientBuilder")
        public RestClient.Builder authRestClientBuilder() {
            return RestClient.builder().messageConverters(buildMessageConverters());
        }

        @Bean("authWebClientBuilder")
        public WebClient.Builder authWebClientBuilder() {
            return WebClient.builder();
        }
    }
}
