package uw.app.common.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uw.app.common.service.SysCritLogStorageService;
import uw.auth.service.conf.AuthServiceAutoConfiguration;
import uw.auth.service.log.AuthCriticalLogStorage;

/**
 * 启动配置。
 */
@Configuration
@AutoConfigureAfter(AuthServiceAutoConfiguration.class)
@EnableConfigurationProperties({AppCommonProperties.class})
public class AppCommonAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger( AppCommonAutoConfiguration.class );

    @Bean
    @Primary
    public AuthCriticalLogStorage SysCritLogStorageService(AppCommonProperties uwAppBaseProperties) {
        log.info( "init SysCritLogStorageService!" );
        return new SysCritLogStorageService(uwAppBaseProperties);
    }

}
