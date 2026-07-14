package uw.nacos.discovery;

import org.springframework.context.ApplicationEvent;

/**
 * Nacos 服务发现信息变更事件。
 *
 * <p>当 {@link NacosDiscoveryProperties} 在初始化过程中检测到与服务管理器中缓存的旧配置不一致
 * （{@link NacosServiceManager#isNacosDiscoveryInfoChanged}）时发布此事件，
 * 监听方（如 {@code NacosAutoServiceRegistration}）据此重新注册服务实例。
 */
public class NacosDiscoveryInfoChangedEvent extends ApplicationEvent {

	/**
	 * 构造事件。
	 *
	 * @param nacosDiscoveryProperties 发生变更的 Nacos 服务发现配置属性（同时作为事件源）
	 */
	public NacosDiscoveryInfoChangedEvent(
			NacosDiscoveryProperties nacosDiscoveryProperties) {
		super(nacosDiscoveryProperties);
	}

	/**
	 * 返回事件源，即发生变更的 Nacos 服务发现配置属性。
	 * @return 发生变更的 Nacos 服务发现配置属性
	 */
	@Override
	public NacosDiscoveryProperties getSource() {
		return (NacosDiscoveryProperties) super.getSource();
	}

}
