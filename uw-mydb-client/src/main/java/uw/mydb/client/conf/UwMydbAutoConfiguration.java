package uw.mydb.client.conf;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import uw.mydb.client.MydbClient;

/**
 * 启动自动配置。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({UwMydbClientProperties.class})
public class UwMydbAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MydbClient mydbClient(UwMydbClientProperties uwNotifyProperties, RestTemplate tokenRestTemplate) {
        return new MydbClient( uwNotifyProperties, tokenRestTemplate );
    }
}
