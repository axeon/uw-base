package uw.nacos.config.configdata;

/**
 * 配置优先级枚举。
 *
 * <p>当配置了 profile 专属配置时，本地配置会覆盖远端，因为本地是 profile 专属、优先级更高。
 * 为给远端配置一次"获胜"的机会，可把远端配置视为 profile 专属，使其排在 profile 专属同级 import
 * 之后，最终覆盖本地 profile 专属配置。
 *
 * @since 2021.0.4.0
 */
public enum ConfigPreference {
	/**
	 * 优先本地配置。
	 */
	LOCAL,
	/**
	 * 优先远端配置。
	 */
	REMOTE
}
