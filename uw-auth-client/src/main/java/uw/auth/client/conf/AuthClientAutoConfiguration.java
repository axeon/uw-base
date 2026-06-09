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
 * 自动装配类。
 */
@Configuration
@EnableConfigurationProperties({AuthClientProperties.class})
public class AuthClientAutoConfiguration {

    private final List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();

    public AuthClientAutoConfiguration() {
        messageConverters.add(new ByteArrayHttpMessageConverter());
        messageConverters.add(new StringHttpMessageConverter());
        messageConverters.add(new ResourceHttpMessageConverter());
        messageConverters.add(new SourceHttpMessageConverter<>());
        messageConverters.add(new AllEncompassingFormHttpMessageConverter());
        messageConverters.add(new MappingJackson2HttpMessageConverter());
    }

    /**
     * auth-client自用的RestClient（不带认证拦截器）。
     */
    @Bean("baseAuthClientRestClient")
    @LoadBalanced
    @ConditionalOnProperty(prefix = "uw.auth.client", name = "enable-spring-cloud", havingValue = "true", matchIfMissing = true)
    public RestClient scBaseAuthClientRestClient(final ClientHttpRequestFactory authClientHttpRequestFactory) {
        return RestClient.builder()
                .requestFactory(authClientHttpRequestFactory)
                .messageConverters(messageConverters)
                .build();
    }

    /**
     * client自用的RestClient（不带认证拦截器）。
     */
    @Bean("baseAuthClientRestClient")
    @ConditionalOnProperty(prefix = "uw.auth.client", name = "enable-spring-cloud", havingValue = "false")
    public RestClient baseAuthClientRestClient(final ClientHttpRequestFactory authClientHttpRequestFactory) {
        return RestClient.builder()
                .requestFactory(authClientHttpRequestFactory)
                .messageConverters(messageConverters)
                .build();
    }

    @Bean
    public AuthClientTokenService authClientTokenHelper(final AuthClientProperties authClientProperties, @Qualifier("baseAuthClientRestClient") final RestClient restClient) {
        return new AuthClientTokenService(authClientProperties, restClient);
    }

    /**
     * 负载均衡的RestClient（带认证拦截器）。
     */
    @Bean("authRestClient")
    @LoadBalanced
    @Primary
    @ConditionalOnProperty(prefix = "uw.auth.client", name = "enable-spring-cloud", havingValue = "true", matchIfMissing = true)
    public RestClient lbAuthRestClient(final ClientHttpRequestFactory authClientHttpRequestFactory, final AuthClientTokenService authClientTokenService) {
        return RestClient.builder()
                .requestFactory(authClientHttpRequestFactory)
                .requestInterceptor(new AuthTokenHeaderInterceptor(authClientTokenService))
                .messageConverters(messageConverters)
                .build();
    }

    /**
     * 非Spring cloud环境下的RestClient（带认证拦截器）。
     */
    @Bean("authRestClient")
    @Primary
    @ConditionalOnProperty(prefix = "uw.auth.client", name = "enable-spring-cloud", havingValue = "false")
    public RestClient authRestClient(final ClientHttpRequestFactory authClientHttpRequestFactory, final AuthClientTokenService authClientTokenService) {
        return RestClient.builder()
                .requestFactory(authClientHttpRequestFactory)
                .requestInterceptor(new AuthTokenHeaderInterceptor(authClientTokenService))
                .messageConverters(messageConverters)
                .build();
    }

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientloadBalancedBuilder() {
        return WebClient.builder();
    }

    @Bean("authWebClient")
    @Primary
    public WebClient authWebClient(WebClient.Builder webClientloadBalancedBuilder, final AuthClientTokenService authClientTokenService) {
        return webClientloadBalancedBuilder
                .filter(new AuthTokenHeaderFilter(authClientTokenService))
                .build();
    }

    /**
     * Http Client连接池配置。
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
}
