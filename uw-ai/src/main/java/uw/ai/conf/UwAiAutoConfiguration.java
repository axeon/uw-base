package uw.ai.conf;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import uw.ai.AiClientHelper;

/**
 * 启动自动配置。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({UwAiProperties.class})
public class UwAiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AiClientHelper notifyClientHelper(UwAiProperties uwNotifyProperties, RestTemplate tokenRestTemplate) {
        return new AiClientHelper( uwNotifyProperties, tokenRestTemplate );
    }
}
