package uw.mydb.client.conf;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;
import uw.mydb.client.MydbClientHelper;

/**
 * 启动自动配置。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({UwMydbClientProperties.class})
public class UwMydbAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MydbClientHelper mydbClientHelper(UwMydbClientProperties uwMydbClientProperties, RestClient authRestClient) {
        return new MydbClientHelper(uwMydbClientProperties, authRestClient);
    }
}
