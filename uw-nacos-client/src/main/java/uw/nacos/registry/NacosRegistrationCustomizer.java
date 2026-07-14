package uw.nacos.registry;

/**
 * Nacos 注册信息定制器回调接口。
 *
 * <p>业务方可实现此接口，在 {@link NacosRegistration} 初始化阶段（{@link NacosRegistration#init()}）
 * 对注册元数据（如 metadata、权重、集群等）进行自定义加工。
 */
public interface NacosRegistrationCustomizer {

	/**
	 * 定制 Nacos 注册信息。
	 *
	 * @param registration 待定制的 Nacos 注册信息
	 */
	void customize(NacosRegistration registration);

}
