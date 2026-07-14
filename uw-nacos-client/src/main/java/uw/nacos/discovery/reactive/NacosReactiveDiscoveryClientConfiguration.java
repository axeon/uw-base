package uw.nacos.discovery.reactive;

import uw.nacos.discovery.ConditionalOnNacosDiscoveryEnabled;
import uw.nacos.discovery.NacosDiscoveryAutoConfiguration;
import uw.nacos.discovery.NacosServiceDiscovery;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnReactiveDiscoveryEnabled;
import org.springframework.cloud.client.ReactiveCommonsClientAutoConfiguration;
import org.springframework.cloud.client.discovery.composite.reactive.ReactiveCompositeDiscoveryClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nacos 反应式发现客户端自动配置类。
 *
 * <p>在响应式发现能力启用（{@link ConditionalOnReactiveDiscoveryEnabled}）且 Nacos 发现开关启用时，
 * 注册 {@link NacosReactiveDiscoveryClient} 作为 Spring Cloud Commons
 * {@link org.springframework.cloud.client.discovery.ReactiveDiscoveryClient} 的实现。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
@ConditionalOnReactiveDiscoveryEnabled
@ConditionalOnNacosDiscoveryEnabled
@AutoConfigureAfter({ NacosDiscoveryAutoConfiguration.class,
		ReactiveCompositeDiscoveryClientAutoConfiguration.class })
@AutoConfigureBefore({ ReactiveCommonsClientAutoConfiguration.class })
public class NacosReactiveDiscoveryClientConfiguration {

	/**
	 * 注册 Nacos 反应式发现客户端。
	 *
	 * @param nacosServiceDiscovery Nacos 服务发现查询 Bean
	 * @return Nacos 反应式发现客户端实例
	 */
	@Bean
	@ConditionalOnMissingBean
	public NacosReactiveDiscoveryClient nacosReactiveDiscoveryClient(
			NacosServiceDiscovery nacosServiceDiscovery) {
		return new NacosReactiveDiscoveryClient(nacosServiceDiscovery);
	}

}
