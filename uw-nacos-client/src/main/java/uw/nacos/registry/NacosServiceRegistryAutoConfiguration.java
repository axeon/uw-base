package uw.nacos.registry;

import java.util.List;

import uw.nacos.discovery.ConditionalOnNacosDiscoveryEnabled;
import uw.nacos.discovery.NacosDiscoveryProperties;
import uw.nacos.discovery.NacosServiceManager;
import uw.nacos.discovery.NacosDiscoveryAutoConfiguration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationAutoConfiguration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationConfiguration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nacos 服务注册自动配置类。
 *
 * <p>在 Nacos 发现开关启用且 {@code spring.cloud.service-registry.auto-registration.enabled}
 * （缺省 true）启用时，装配服务注册核心组件：
 * {@link NacosServiceRegistry}（注册器）、{@link NacosRegistration}（注册信息）、
 * {@link NacosAutoServiceRegistration}（自动注册生命周期）以及
 * {@link NacosGracefulShutdownDelegate}（优雅停机委托）。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
@ConditionalOnNacosDiscoveryEnabled
@ConditionalOnProperty(value = "spring.cloud.service-registry.auto-registration.enabled",
		matchIfMissing = true)
@AutoConfigureAfter({ AutoServiceRegistrationConfiguration.class,
		AutoServiceRegistrationAutoConfiguration.class,
		NacosDiscoveryAutoConfiguration.class })
public class NacosServiceRegistryAutoConfiguration {

	/**
	 * 注册 Nacos 服务注册器 Bean。
	 *
	 * @param nacosServiceManager     Nacos 服务管理器
	 * @param nacosDiscoveryProperties Nacos 服务发现配置属性
	 * @return Nacos 服务注册器实例
	 */
	@Bean
	public NacosServiceRegistry nacosServiceRegistry(
			NacosServiceManager nacosServiceManager,
			NacosDiscoveryProperties nacosDiscoveryProperties) {
		return new NacosServiceRegistry(nacosServiceManager, nacosDiscoveryProperties);
	}

	/**
	 * 注册 Nacos 服务注册信息 Bean。
	 *
	 * @param registrationCustomizers 注册信息定制器列表（可选）
	 * @param nacosDiscoveryProperties Nacos 服务发现配置属性
	 * @param context                  Spring 应用上下文
	 * @return Nacos 注册信息实例
	 */
	@Bean
	@ConditionalOnBean(AutoServiceRegistrationProperties.class)
	public NacosRegistration nacosRegistration(
			ObjectProvider<List<NacosRegistrationCustomizer>> registrationCustomizers,
			NacosDiscoveryProperties nacosDiscoveryProperties,
			ApplicationContext context) {
		return new NacosRegistration(registrationCustomizers.getIfAvailable(),
				nacosDiscoveryProperties, context);
	}

	/**
	 * 注册 Nacos 自动服务注册生命周期 Bean。
	 *
	 * @param context                          Spring 应用上下文
	 * @param registry                         Nacos 服务注册器
	 * @param autoServiceRegistrationProperties 自动注册属性
	 * @param registration                     Nacos 注册信息
	 * @return Nacos 自动服务注册实例
	 */
	@Bean
	@ConditionalOnBean(AutoServiceRegistrationProperties.class)
	public NacosAutoServiceRegistration nacosAutoServiceRegistration(
			ApplicationContext context,
			NacosServiceRegistry registry,
			AutoServiceRegistrationProperties autoServiceRegistrationProperties,
			NacosRegistration registration) {
		return new NacosAutoServiceRegistration(context, registry,
				autoServiceRegistrationProperties, registration);
	}

	/**
	 * 注册 Nacos 优雅停机委托 Bean。
	 *
	 * @param nacosAutoServiceRegistration Nacos 自动服务注册实例
	 * @param nacosDiscoveryProperties     Nacos 服务发现配置属性
	 * @return Nacos 优雅停机委托实例
	 */
	@Bean
	@ConditionalOnBean(NacosAutoServiceRegistration.class)
	public NacosGracefulShutdownDelegate nacosGracefulShutdownDelegate(
			NacosAutoServiceRegistration nacosAutoServiceRegistration,
			NacosDiscoveryProperties nacosDiscoveryProperties) {
		return new NacosGracefulShutdownDelegate(nacosAutoServiceRegistration, nacosDiscoveryProperties);
	}
}
