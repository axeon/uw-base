package uw.nacos.discovery;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nacos 服务发现自动配置类。
 *
 * <p>在 Spring Cloud 通用发现能力（{@link ConditionalOnDiscoveryEnabled}）与 Nacos 发现开关
 * （{@link ConditionalOnNacosDiscoveryEnabled}）均启用时，装配
 * {@link NacosDiscoveryProperties} 与 {@link NacosServiceDiscovery} 两个核心 Bean。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
@ConditionalOnNacosDiscoveryEnabled
@AutoConfigureAfter({ NacosServiceAutoConfiguration.class, UtilIPv6AutoConfiguration.class })
public class NacosDiscoveryAutoConfiguration {

	/**
	 * 注册 Nacos 服务发现配置属性 Bean。
	 *
	 * @return Nacos 服务发现配置属性实例
	 */
	@Bean
	@ConditionalOnMissingBean
	public NacosDiscoveryProperties nacosProperties() {
		return new NacosDiscoveryProperties();
	}

	/**
	 * 注册 Nacos 服务发现查询 Bean，封装对 Nacos NamingService 的服务实例/服务名查询。
	 *
	 * @param discoveryProperties Nacos 服务发现配置属性
	 * @param nacosServiceManager Nacos 服务管理器
	 * @return Nacos 服务发现查询实例
	 */
	@Bean
	@ConditionalOnMissingBean
	public NacosServiceDiscovery nacosServiceDiscovery(
			NacosDiscoveryProperties discoveryProperties,
			NacosServiceManager nacosServiceManager) {
		return new NacosServiceDiscovery(discoveryProperties, nacosServiceManager);
	}

}
