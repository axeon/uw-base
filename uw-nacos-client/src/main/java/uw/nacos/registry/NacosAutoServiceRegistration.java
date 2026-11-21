package uw.nacos.registry;

import uw.nacos.discovery.NacosDiscoveryInfoChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.serviceregistry.AbstractAutoServiceRegistration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Nacos 自动服务注册生命周期管理。
 *
 * <p>继承 Spring Cloud Commons 的 {@link AbstractAutoServiceRegistration}，在应用启动时自动将
 * {@link NacosRegistration} 注册到 Nacos（通过 {@link NacosServiceRegistry}），在关闭时自动摘除。
 * 监听 {@link NacosDiscoveryInfoChangedEvent}，发现信息变更时先 {@code stop()} 再 {@code start()}
 * 重新完成注册。{@code start()}/{@code stop()} 行为由父类提供。
 * </p>
 *
 * <p><b>上游弃用说明</b>：父类 {@code AbstractAutoServiceRegistration}（spring-cloud-commons 5.x）整体标记
 * forRemoval，{@code getPort()}/{@code getConfiguration()} 等成员均已弃用且无替代 API——涉及方法局部
 * {@code @SuppressWarnings("deprecation")} 压制并留此说明，待官方给出正式替代后整体迁移。</p>
 */
public class NacosAutoServiceRegistration
		extends AbstractAutoServiceRegistration<Registration> {

	/** 日志记录器。 */
	private static final Logger log = LoggerFactory
			.getLogger(NacosAutoServiceRegistration.class);

	/** 当前应用的 Nacos 注册信息。 */
	private NacosRegistration registration;

	/**
	 * 构造 Nacos 自动服务注册生命周期管理器。
	 *
	 * @param context                          Spring 应用上下文
	 * @param serviceRegistry                  服务注册器
	 * @param autoServiceRegistrationProperties 自动注册属性
	 * @param registration                     Nacos 注册信息
	 */
	public NacosAutoServiceRegistration(ApplicationContext context,
			ServiceRegistry<Registration> serviceRegistry,
			AutoServiceRegistrationProperties autoServiceRegistrationProperties,
			NacosRegistration registration) {
		super(context, serviceRegistry, autoServiceRegistrationProperties);
		this.registration = registration;
	}

	/**
	 * 返回待注册的 Nacos 注册信息，并校验端口合法。
	 * @return Nacos 注册信息
	 */
	@Override
	protected NacosRegistration getRegistration() {
		if (this.registration.getPort() < 0 && this.getPort().get() > 0) {
			this.registration.setPort(this.getPort().get());
		}
		Assert.isTrue(this.registration.getPort() > 0, "service.port has not been set");
		return this.registration;
	}

	/**
	 * 返回 management 注册信息，本项目不单独注册 management 实例，固定返回 null。
	 * @return 固定为 null
	 */
	@Override
	protected NacosRegistration getManagementRegistration() {
		return null;
	}

	/**
	 * 执行服务注册，注册开关关闭或端口非法时跳过。
	 */
	@Override
	protected void register() {
		if (!this.registration.getNacosDiscoveryProperties().isRegisterEnabled()) {
			log.debug("Registration disabled.");
			return;
		}
		if (this.registration.getPort() < 0) {
			this.registration.setPort(getPort().get());
		}
		super.register();
	}

	/**
	 * 执行 management 服务注册，注册开关关闭时跳过。
	 */
	@Override
	protected void registerManagement() {
		if (!this.registration.getNacosDiscoveryProperties().isRegisterEnabled()) {
			return;
		}
		super.registerManagement();

	}

	/**
	 * @return 配置对象（即 Nacos 服务发现配置属性），供父类诊断/展示使用
	 */
	@SuppressWarnings("deprecation")
	@Override
	protected Object getConfiguration() {
		return this.registration.getNacosDiscoveryProperties();
	}

	/**
	 * @return 自动注册是否启用，由 {@code register-enabled} 配置项决定
	 */
	@Override
	protected boolean isEnabled() {
		return this.registration.getNacosDiscoveryProperties().isRegisterEnabled();
	}

	/**
	 * @return 应用名，优先使用 Nacos 配置的 service 名，未设置时回退到父类实现
	 */
	@Override
	@SuppressWarnings("deprecation")
	protected String getAppName() {
		String appName = registration.getNacosDiscoveryProperties().getService();
		return StringUtils.hasLength(appName) ? appName : super.getAppName();
	}

	/**
	 * 监听发现信息变更事件，触发重新注册。
	 *
	 * @param event Nacos 发现信息变更事件
	 */
	@EventListener
	public void onNacosDiscoveryInfoChangedEvent(NacosDiscoveryInfoChangedEvent event) {
		restart();
	}

	/**
	 * 先摘除实例（stop）再重新注册（start），用于发现信息变更后的重新注册。
	 */
	private void restart() {
		this.stop();
		this.start();
	}
}
