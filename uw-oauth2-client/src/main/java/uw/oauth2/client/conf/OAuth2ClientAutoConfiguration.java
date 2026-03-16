package uw.oauth2.client.conf;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import uw.oauth2.client.OAuth2ClientHelper;

/**
 * 启动自动配置。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({OAuth2ClientProperties.class})
public class OAuth2ClientAutoConfiguration {

    /**
     * 创建OAuth2客户端帮助类
     *
     * @param oauth2ClientProperties OAuth2客户端配置
     * @return OAuth2客户端帮助类
     */
    @Bean
    @ConditionalOnMissingBean
    public OAuth2ClientHelper oAuth2ClientHelper(OAuth2ClientProperties oauth2ClientProperties) {
        return new OAuth2ClientHelper(oauth2ClientProperties);
    }

}
