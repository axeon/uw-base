package uw.notify.client.conf;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import uw.notify.client.NotifyClientHelper;

/**
 * 启动自动配置。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({UwNotifyProperties.class})
public class UwNotifyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NotifyClientHelper notifyClientHelper(UwNotifyProperties uwNotifyProperties, RestTemplate authRestTemplate) {
        return new NotifyClientHelper( uwNotifyProperties, authRestTemplate );
    }
}
