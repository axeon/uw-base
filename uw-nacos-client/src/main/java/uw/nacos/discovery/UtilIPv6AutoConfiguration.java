package uw.nacos.discovery;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * IPv6 工具自动配置类。
 *
 * <p>在 Spring Cloud 通用发现能力与 Nacos 发现开关均启用时，注册 {@link InetIPv6Utils}
 * Bean，供 {@link NacosDiscoveryProperties} 在初始化阶段探测本机 IPv6 地址使用。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
@ConditionalOnNacosDiscoveryEnabled
public class UtilIPv6AutoConfiguration {

	/**
	 * 注册 IPv6 地址探测工具 Bean。
	 *
	 * @param properties Spring Cloud 通用网络工具配置
	 * @return IPv6 地址探测工具实例
	 */
	@Bean
	@ConditionalOnMissingBean
	public InetIPv6Utils inetIPv6Utils(InetUtilsProperties properties) {
		return new InetIPv6Utils(properties);
	}

}
