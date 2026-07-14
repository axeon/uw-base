package uw.nacos.config.configdata;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import uw.nacos.config.NacosConfigProperties;

/**
 * 用于 ConfigData 阶段初始化 Nacos {@code ConfigService} 的属性绑定类。
 *
 * <p>继承 {@link NacosConfigProperties}，额外承载 {@code spring.cloud.nacos.config} 下以
 * Map 形式声明的扁平配置项，覆盖父类的 {@link #enrichNacosConfigProperties(Properties)}，
 * 将这些键补充到构造 ConfigService 的 Properties 中。
 */
public class NacosConfigDataLoadProperties extends NacosConfigProperties {
	/**
	 * {@code spring.cloud.nacos.config} 下以 Map 形式声明的扁平配置项。
	 */
	private Map<String, String> config = new HashMap<>();

	/**
	 * 用 {@link #config} Map 中的扁平键补齐 Properties（kebab-case 转驼峰后 putIfAbsent）。
	 * @param nacosConfigProperties 待补齐的 Properties
	 */
	@Override
	protected void enrichNacosConfigProperties(Properties nacosConfigProperties) {
		config.forEach((k, v) -> nacosConfigProperties.putIfAbsent(resolveKey(k),
				String.valueOf(v)));
	}

	/**
	 * @param config 扁平配置项 Map
	 */
	void setConfig(Map<String, String> config) {
		this.config = config;
	}
}
