package uw.nacos.common.context.support;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;

import static java.util.Collections.unmodifiableMap;

/**
 * 属性源（{@link PropertySource}）工具类，提供按前缀提取子属性集合能力。
 *
 * <p>用于从 Spring {@link org.springframework.core.env.Environment} 中按指定前缀聚合配置项，
 * 是 {@code @ConfigurationProperties} 绑定之外的补充读取手段（例如动态读取 Nacos discovery 配置）。
 */
public final class PropertySourcesUtils {

	/**
	 * 工具类禁止实例化。
	 */
	private PropertySourcesUtils() {
	}

	/**
	 * 从 {@link ConfigurableEnvironment} 中提取指定前缀下的子属性集合。
	 *
	 * <p>同名属性只取第一个出现的（优先级最高的）属性源中的值；字符串值会解析占位符。
	 *
	 * @param environment {@link ConfigurableEnvironment}
	 * @param prefix      属性名前缀
	 * @return 以去掉前缀后的属性名为 key 的属性 map（不可变）
	 */
	public static Map<String, Object> getSubProperties(ConfigurableEnvironment environment, String prefix) {
		String normalizedPrefix = prefix.endsWith(".") ? prefix : prefix + ".";
		PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(
				environment.getPropertySources());
		Map<String, Object> subProperties = new LinkedHashMap<>();
		for (PropertySource<?> source : environment.getPropertySources()) {
			String[] propertyNames = source instanceof EnumerablePropertySource
					? ((EnumerablePropertySource<?>) source).getPropertyNames() : null;
			if (propertyNames == null) {
				continue;
			}
			for (String name : propertyNames) {
				if (name.startsWith(normalizedPrefix)) {
					String subName = name.substring(normalizedPrefix.length());
					if (!subProperties.containsKey(subName)) { // take first one
						Object value = source.getProperty(name);
						if (value instanceof String) {
							// Resolve placeholder
							value = resolver.resolvePlaceholders((String) value);
						}
						subProperties.put(subName, value);
					}
				}
			}
		}
		return unmodifiableMap(subProperties);
	}

}
