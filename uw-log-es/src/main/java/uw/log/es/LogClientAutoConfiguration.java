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
 * 日志接口服务客户端自动配置类。
 * <p>启用 {@link LogClientProperties} 配置绑定，并以 Bean 形式装配 {@link LogClient}
 * （内部持有 {@link LogService}）。容器销毁时通过 {@link PreDestroy} 关闭后台写入链路。
 */
@Configuration
@EnableConfigurationProperties({LogClientProperties.class})
public class LogClientAutoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(LogClientAutoConfiguration.class);
    /**
     * 应用名称（应用名:版本，用于覆写日志体 appInfo）
     */
    @Value("${spring.application.name:unknown}:${project.version:unknown}")
    private String appInfo;

    /**
     * 应用主机（ip:port，用于覆写日志体 appHost）
     */
    @Value("${spring.cloud.nacos.discovery.ip:localhost}:${server.port:0}")
    private String appHost;

    /**
     * 日志接口服务客户端。
     */
    private LogClient logClient;

    /**
     * 创建并登记全局单例 {@link LogClient}。
     *
     * @param logClientProperties 配置属性
     * @return 日志客户端实例
     */
    @Bean
    public LogClient logClient(final LogClientProperties logClientProperties) {
        logClient = new LogClient(new LogService(logClientProperties, appInfo, appHost));
        return logClient;
    }

    /**
     * 容器销毁时关闭写日志系统，flush 残留 buffer。
     */
    @PreDestroy
    public void destroy() {
        logClient.destroy();
    }
}
