package uw.tinyurl.client.conf;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import uw.tinyurl.client.TinyurlClientHelper;

/**
 * 启动自动配置。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({UwTinyurlProperties.class})
public class UwTinyurlAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TinyurlClientHelper tinyurlClientHelper(UwTinyurlProperties uwTinyurlProperties, RestTemplate tokenRestTemplate) {
        return new TinyurlClientHelper( uwTinyurlProperties, tokenRestTemplate );
    }
}
