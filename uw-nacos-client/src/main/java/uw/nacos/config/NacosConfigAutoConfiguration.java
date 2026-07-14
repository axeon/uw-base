package uw.nacos.config;

import uw.nacos.config.refresh.NacosContextRefresher;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nacos 配置中心自动装配类。
 *
 * <p>当 {@code spring.cloud.nacos.config.enabled}（默认 true）开启时，装配
 * {@link NacosConfigProperties}、{@link NacosConfigManager} 与 {@link NacosContextRefresher}。
 * 本项目不使用 SCA 的 @NacosConfigListener 注解驱动，统一走 ConfigData 加载 + RefreshEvent 刷新。
 *
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.cloud.nacos.config.enabled", matchIfMissing = true)
public class NacosConfigAutoConfiguration {

	/**
	 * 装配 {@link NacosConfigProperties}，复用 ConfigData 阶段已在 BootstrapRegistry 注册的实例。
	 * @return NacosConfigProperties 实例
	 */
	@Bean
	@ConditionalOnMissingBean(value = NacosConfigProperties.class, search = SearchStrategy.CURRENT)
	public NacosConfigProperties nacosConfigProperties() {
		// this should never happen except for some unit tests
		if (NacosConfigManager.getInstance() == null) {
			return new NacosConfigProperties();
		}
		return NacosConfigManager.getInstance().getNacosConfigProperties();
	}

	/**
	 * @param nacosConfigProperties Nacos 配置属性
	 * @return NacosConfigManager 单例
	 */
	@Bean
	public NacosConfigManager nacosConfigManager(NacosConfigProperties nacosConfigProperties) {
		return NacosConfigManager.getInstance(nacosConfigProperties);
	}

	/**
	 * @param nacosConfigManager Nacos 配置管理器
	 * @return Nacos 配置上下文刷新器
	 */
	@Bean
	public NacosContextRefresher nacosContextRefresher(NacosConfigManager nacosConfigManager) {
		return new NacosContextRefresher(nacosConfigManager);
	}

}
