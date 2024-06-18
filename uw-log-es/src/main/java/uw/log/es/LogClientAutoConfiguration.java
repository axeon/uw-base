package uw.log.es;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uw.log.es.service.LogService;

/**
 * 日志接口服务客户端自动配置类
 */
@Configuration
@EnableConfigurationProperties({LogClientProperties.class})
public class LogClientAutoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger( LogClientAutoConfiguration.class );
    /**
     * 应用名称
     */
    @Value("${spring.application.name}:${project.version}")
    private String appInfo;

    /**
     * 应用主机
     */
    @Value("${spring.cloud.nacos.discovery.ip}:${server.port}")
    private String appHost;


    private LogClient logClient;

    /**
     * 日志接口服务客户端
     *
     * @param logClientProperties
     * @return
     */
    @Bean
    public LogClient logClient(final LogClientProperties logClientProperties) {
        logClient = new LogClient( new LogService( logClientProperties, appInfo, appHost ) );
        return logClient;
    }

    @PreDestroy
    public void destroy() {
        logClient.destroy();
    }
}
