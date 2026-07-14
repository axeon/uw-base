package uw.nacos.config;

/**
 * uw-nacos-client 配置公共前缀常量。
 *
 * <p>config / discovery 等模块的配置项均挂载在固定前缀 {@link #DEFAULT_PREFIX} 下，
 * 与 {@code @ConfigurationProperties("spring.cloud.nacos.*")} 的绑定根保持一致。
 *
 */
public final class NacosPropertiesPrefixer {

	/**
	 * uw-nacos-client 固定公共前缀（不含尾部点号）。
	 */
	public static final String DEFAULT_PREFIX = "spring.cloud.nacos";

	/**
	 * 常量工具类禁止实例化。
	 */
	private NacosPropertiesPrefixer() {
	}

}
