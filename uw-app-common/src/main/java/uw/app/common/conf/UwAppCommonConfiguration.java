package uw.app.common.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uw.app.common.service.SysCritLogStorageService;
import uw.auth.service.conf.AuthServiceAutoConfiguration;

/**
 * 启动配置。
 */
@Configuration
@AutoConfigureAfter(AuthServiceAutoConfiguration.class)
@EnableConfigurationProperties({UwAppCommonProperties.class})
public class UwAppCommonConfiguration {
    private static final Logger log = LoggerFactory.getLogger( UwAppCommonConfiguration.class );

    @Bean
    SysCritLogStorageService sysCritLogStorageService(UwAppCommonProperties uwAppBaseProperties) {
        return new SysCritLogStorageService(uwAppBaseProperties);
    }

}
