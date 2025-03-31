package uw.auth.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HeaderElement;
import org.apache.hc.core5.http.message.BasicHeaderElementIterator;
import org.apache.hc.core5.util.TimeValue;
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
import org.springframework.web.client.RestTemplate;
import uw.auth.client.interceptor.TokenHeaderInterceptor;
import uw.auth.client.util.AuthClientTokenHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 自动装配类。
 */
@Configuration
@EnableConfigurationProperties({AuthClientProperties.class})
public class AuthClientAutoConfiguration {

    private final List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();

    /**
     * 初始化Message Converters
     */
    public AuthClientAutoConfiguration() {
        messageConverters.add(new ByteArrayHttpMessageConverter());
        messageConverters.add(new StringHttpMessageConverter());
        messageConverters.add(new ResourceHttpMessageConverter());
        messageConverters.add(new SourceHttpMessageConverter<>());
        messageConverters.add(new AllEncompassingFormHttpMessageConverter());
        messageConverters.add(new MappingJackson2HttpMessageConverter());
    }


    @Bean("baseAuthClientRestTemplate")
    @LoadBalanced
    @ConditionalOnProperty(prefix = "uw.auth.client", name = "enable-spring-cloud", havingValue = "true", matchIfMissing = true)
    public RestTemplate scBaseAuthClientRestTemplate(final ClientHttpRequestFactory clientHttpRequestFactory) {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);
        restTemplate.setMessageConverters(messageConverters);
        return restTemplate;
    }

    @Bean("baseAuthClientRestTemplate")
    @ConditionalOnProperty(prefix = "uw.auth.client", name = "enable-spring-cloud", havingValue = "false")
    public RestTemplate baseAuthClientRestTemplate(final ClientHttpRequestFactory clientHttpRequestFactory) {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);
        restTemplate.setMessageConverters(messageConverters);
        return restTemplate;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public AuthClientTokenHelper authClientToken(final AuthClientProperties authClientProperties, final ObjectMapper objectMapper,
                                                 @Qualifier("baseAuthClientRestTemplate") final RestTemplate restTemplate) {
        return new AuthClientTokenHelper(authClientProperties, objectMapper, restTemplate);
    }

    @Bean
    public TokenHeaderInterceptor tokenHeaderInterceptor(final AuthClientTokenHelper authClientToken) {
        return new TokenHeaderInterceptor(authClientToken);
    }

    @LoadBalanced
    @Bean("tokenRestTemplate")
    @Primary
    @ConditionalOnProperty(prefix = "uw.auth.client", name = "enable-spring-cloud", havingValue = "true", matchIfMissing = true)
    public RestTemplate lbTokenRestTemplate(final ClientHttpRequestFactory clientHttpRequestFactory, final TokenHeaderInterceptor tokenHeaderInterceptor) {
        RestTemplate tokenRestTemplate = new RestTemplate(clientHttpRequestFactory);
        tokenRestTemplate.setInterceptors(Collections.singletonList(tokenHeaderInterceptor));
        tokenRestTemplate.setMessageConverters(messageConverters);
        return tokenRestTemplate;
    }

    @Bean("tokenRestTemplate")
    @Primary
    @ConditionalOnProperty(prefix = "uw.auth.client", name = "enable-spring-cloud", havingValue = "false")
    public RestTemplate tokenRestTemplate(final ClientHttpRequestFactory clientHttpRequestFactory, final TokenHeaderInterceptor tokenHeaderInterceptor) {
        RestTemplate tokenRestTemplate = new RestTemplate(clientHttpRequestFactory);
        tokenRestTemplate.setInterceptors(Collections.singletonList(tokenHeaderInterceptor));
        tokenRestTemplate.setMessageConverters(messageConverters);
        return tokenRestTemplate;
    }

    /**
     * Http Client连接池配置
     *
     * @param authClientProperties
     * @return
     */
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory(final AuthClientProperties authClientProperties) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(authClientProperties.getHttpPool().getMaxTotal());
        connectionManager.setDefaultMaxPerRoute(authClientProperties.getHttpPool().getDefaultMaxPerRoute());
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(authClientProperties.getHttpPool().getConnectTimeout(), TimeUnit.MILLISECONDS)
                .setConnectionRequestTimeout(authClientProperties.getHttpPool().getConnectionRequestTimeout(), TimeUnit.MILLISECONDS)
                .build();
        CloseableHttpClient httpClient = HttpClientBuilder.create().setConnectionManager(connectionManager).setDefaultRequestConfig(config).setKeepAliveStrategy((response,
                                                                                                                                                                  context) -> {
            BasicHeaderElementIterator iterator = new BasicHeaderElementIterator(response.headerIterator("Keep-Alive"));
            while (iterator.hasNext()) {
                HeaderElement he = iterator.next();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && param.equalsIgnoreCase("timeout")) {
                    return TimeValue.of(Long.parseLong(value) * 1000, TimeUnit.MILLISECONDS);
                }
            }
            // return authClientProperties.getHttpPool().getKeepAliveTimeIfNotPresent();
            return TimeValue.of(authClientProperties.getHttpPool().getKeepAliveTimeIfNotPresent(), TimeUnit.MILLISECONDS);

        }).build();
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
