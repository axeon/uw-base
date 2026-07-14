package uw.nacos.discovery;

import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nacos 服务管理器自动配置类。
 *
 * <p>在 Spring Cloud 通用发现能力与 Nacos 发现开关均启用时，注册 {@link NacosServiceManager}
 * Bean，负责懒加载并持有 Nacos {@code NamingService} 实例。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
@ConditionalOnNacosDiscoveryEnabled
public class NacosServiceAutoConfiguration {

	/**
	 * 注册 Nacos 服务管理器 Bean。
	 *
	 * @return Nacos 服务管理器实例
	 */
	@Bean
	public NacosServiceManager nacosServiceManager() {
		return new NacosServiceManager();
	}

}
