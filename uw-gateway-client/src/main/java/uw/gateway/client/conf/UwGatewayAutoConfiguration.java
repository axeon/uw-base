package uw.gateway.client.conf;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import uw.gateway.client.GatewayClientHelper;

/**
 * 启动自动配置。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({UwGatewayProperties.class})
public class UwGatewayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GatewayClientHelper GatewayClientHelper(UwGatewayProperties uwGatewayProperties, RestTemplate authRestTemplate) {
        return new GatewayClientHelper( uwGatewayProperties, authRestTemplate );
    }
}
