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

    /**
     * 负载均衡的RestTemplate
     * @param clientHttpRequestFactory
     * @return
     */
    @LoadBalanced
    @Bean("authRestTemplate")
    @Primary
    @ConditionalOnProperty(prefix = "uw.auth.client", name = "enable-spring-cloud", havingValue = "true", matchIfMissing = true)
    public RestTemplate lbAuthRestTemplate(final AuthClientProperties authClientProperties,final ClientHttpRequestFactory clientHttpRequestFactory) {
        RestTemplate commonRestTemplate = new RestTemplate(clientHttpRequestFactory);
        commonRestTemplate.setMessageConverters(messageConverters);
        RestTemplate authRestTemplate = new RestTemplate(clientHttpRequestFactory);
        authRestTemplate.setInterceptors(Collections.singletonList(new TokenHeaderInterceptor(new AuthClientTokenHelper(authClientProperties, commonRestTemplate))));
        authRestTemplate.setMessageConverters(messageConverters);
        return authRestTemplate;
    }

    /**
     * 非Spring cloud环境下的RestTemplate
     *
     * @param clientHttpRequestFactory
     * @return
     */
    @Bean("authRestTemplate")
    @Primary
    @ConditionalOnProperty(prefix = "uw.auth.client", name = "enable-spring-cloud", havingValue = "false")
    public RestTemplate authRestTemplate(final AuthClientProperties authClientProperties,final ClientHttpRequestFactory clientHttpRequestFactory) {
        RestTemplate commonRestTemplate = new RestTemplate(clientHttpRequestFactory);
        commonRestTemplate.setMessageConverters(messageConverters);
        RestTemplate authRestTemplate = new RestTemplate(clientHttpRequestFactory);
        authRestTemplate.setInterceptors(Collections.singletonList(new TokenHeaderInterceptor(new AuthClientTokenHelper(authClientProperties, commonRestTemplate))));
        authRestTemplate.setMessageConverters(messageConverters);
        return authRestTemplate;
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
