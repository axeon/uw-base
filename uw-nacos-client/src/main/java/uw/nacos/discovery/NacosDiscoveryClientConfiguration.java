package uw.nacos.discovery;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.cloud.client.CommonsClientAutoConfiguration;
import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nacos 阻塞式发现客户端自动配置类。
 *
 * <p>在阻塞式发现能力启用时，注册 {@link NacosDiscoveryClient} 作为 Spring Cloud Commons
 * {@link DiscoveryClient} 的实现。注意：项目未启用 {@code NacosWatch}（Spring Cloud Alibaba
 * 2025.1.x 默认也不再启用）。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
@ConditionalOnBlockingDiscoveryEnabled
@ConditionalOnNacosDiscoveryEnabled
@AutoConfigureBefore({ SimpleDiscoveryClientAutoConfiguration.class,
		CommonsClientAutoConfiguration.class })
@AutoConfigureAfter(NacosDiscoveryAutoConfiguration.class)
public class NacosDiscoveryClientConfiguration {

	/**
	 * 注册 Nacos 阻塞式发现客户端。
	 *
	 * @param nacosServiceDiscovery Nacos 服务发现查询 Bean
	 * @return Nacos 发现客户端实例
	 */
	@Bean
	public DiscoveryClient nacosDiscoveryClient(
			NacosServiceDiscovery nacosServiceDiscovery) {
		return new NacosDiscoveryClient(nacosServiceDiscovery);
	}

}
