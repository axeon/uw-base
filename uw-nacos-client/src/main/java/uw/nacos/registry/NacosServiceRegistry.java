package uw.nacos.registry;

import java.util.List;

import uw.nacos.discovery.NacosDiscoveryProperties;
import uw.nacos.discovery.NacosServiceManager;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.util.StringUtils;

import static org.springframework.util.ReflectionUtils.rethrowRuntimeException;

/**
 * 基于 Nacos 的 {@link ServiceRegistry} 实现。
 *
 * <p>实现 Spring Cloud Commons 的 {@link ServiceRegistry} 接口，负责将 {@link Registration}
 * （即 {@link NacosRegistration}）注册到 / 摘除自 Nacos 服务器，以及查询和设置实例状态。
 * 注册失败时根据 {@link NacosDiscoveryProperties#isFailFast()} 决定抛异常还是仅记录日志。
 * {@link #close()} 委托 {@link NacosServiceManager#nacosServiceShutDown()} 关闭底层连接。
 */
public class NacosServiceRegistry implements ServiceRegistry<Registration> {

	/** 实例状态：UP（上线）。 */
	private static final String STATUS_UP = "UP";

	/** 实例状态：DOWN（下线）。 */
	private static final String STATUS_DOWN = "DOWN";

	/** 日志记录器。 */
	private static final Logger log = LoggerFactory.getLogger(NacosServiceRegistry.class);

	/** Nacos 服务发现配置属性，提供 group、cluster、weight 等注册参数。 */
	private final NacosDiscoveryProperties nacosDiscoveryProperties;

	/** Nacos 服务管理器，负责获取 NamingService。 */
	private final NacosServiceManager nacosServiceManager;

	/**
	 * 构造 Nacos 服务注册器。
	 *
	 * @param nacosServiceManager     Nacos 服务管理器
	 * @param nacosDiscoveryProperties Nacos 服务发现配置属性
	 */
	public NacosServiceRegistry(NacosServiceManager nacosServiceManager,
			NacosDiscoveryProperties nacosDiscoveryProperties) {
		this.nacosDiscoveryProperties = nacosDiscoveryProperties;
		this.nacosServiceManager = nacosServiceManager;
	}

	/**
	 * 将注册信息注册到 Nacos 服务器。
	 * <p>serviceId 为空时跳过；注册失败时根据 {@code failFast} 决定抛异常或仅记录日志。
	 * @param registration 服务注册信息
	 */
	@Override
	public void register(Registration registration) {

		if (!StringUtils.hasLength(registration.getServiceId())) {
			log.warn("No service to register for nacos client...");
			return;
		}

		NamingService namingService = namingService();
		String serviceId = registration.getServiceId();
		String group = nacosDiscoveryProperties.getGroup();

		Instance instance = getNacosInstanceFromRegistration(registration);

		try {
			namingService.registerInstance(serviceId, group, instance);
			log.info("nacos registry, {} {} {}:{} register finished", group, serviceId,
					instance.getIp(), instance.getPort());
		}
		catch (Exception e) {
			if (nacosDiscoveryProperties.isFailFast()) {
				log.error("nacos registry, {} register failed...{},", serviceId,
						registration.toString(), e);
				rethrowRuntimeException(e);
			}
			else {
				log.warn("Failfast is false. {} register failed...{},", serviceId,
						registration.toString(), e);
			}
		}
	}

	/**
	 * 从 Nacos 服务器摘除注册实例，serviceId 为空或摘除失败时仅记录日志。
	 * @param registration 服务注册信息
	 */
	@Override
	public void deregister(Registration registration) {

		log.info("De-registering from Nacos Server now...");

		if (!StringUtils.hasLength(registration.getServiceId())) {
			log.warn("No dom to de-register for nacos client...");
			return;
		}

		NamingService namingService = namingService();
		String serviceId = registration.getServiceId();
		String group = nacosDiscoveryProperties.getGroup();

		try {
			namingService.deregisterInstance(serviceId, group, registration.getHost(),
					registration.getPort(), nacosDiscoveryProperties.getClusterName());
		}
		catch (Exception e) {
			log.error("ERR_NACOS_DEREGISTER, de-register failed...{},",
					registration.toString(), e);
		}

		log.info("De-registration finished.");
	}

	/**
	 * 关闭 NamingService 并释放底层连接，关闭异常仅记录日志。
	 */
	@Override
	public void close() {
		try {
			nacosServiceManager.nacosServiceShutDown();
		}
		catch (NacosException e) {
			log.error("Nacos namingService shutDown failed", e);
		}
	}

	/**
	 * 设置实例状态为 UP（重新注册）或 DOWN（摘除实例），其他状态值仅记录警告并跳过。
	 * @param registration 服务注册信息
	 * @param status       目标状态，仅支持 UP / DOWN
	 * @throws RuntimeException 切换状态失败时抛出
	 */
	@Override
	public void setStatus(Registration registration, String status) {

		if (!STATUS_UP.equalsIgnoreCase(status)
				&& !STATUS_DOWN.equalsIgnoreCase(status)) {
			log.warn("can't support status {},please choose UP or DOWN", status);
			return;
		}

		try {
			if (STATUS_DOWN.equalsIgnoreCase(status)) {
				deregister(registration);
			}
			else {
				register(registration);
			}
		}
		catch (Exception e) {
			throw new RuntimeException("update nacos instance status fail", e);
		}
	}

	/**
	 * 查询当前实例在 Nacos 的状态。
	 * <p>按 ip 与 port 匹配自身实例，返回 {@code UP}/{@code DOWN}；未匹配到或查询异常时返回 null。
	 * @param registration 服务注册信息
	 * @return 实例状态字符串，未找到时返回 null
	 */
	@Override
	public Object getStatus(Registration registration) {

		String serviceName = registration.getServiceId();
		String group = nacosDiscoveryProperties.getGroup();
		try {
			List<Instance> instances = namingService().getAllInstances(serviceName,
					group);
			for (Instance instance : instances) {
				if (instance.getIp().equalsIgnoreCase(nacosDiscoveryProperties.getIp())
						&& instance.getPort() == nacosDiscoveryProperties.getPort()) {
					return instance.isEnabled() ? STATUS_UP : STATUS_DOWN;
				}
			}
		}
		catch (Exception e) {
			log.error("get all instance of {} error,", serviceName, e);
		}
		return null;
	}

	/**
	 * 从注册信息构造 Nacos {@link Instance}，填充 ip、port、weight、cluster、enabled、metadata、ephemeral 等字段。
	 *
	 * @param registration 服务注册信息
	 * @return Nacos 实例
	 */
	private Instance getNacosInstanceFromRegistration(Registration registration) {
		Instance instance = new Instance();
		instance.setIp(registration.getHost());
		instance.setPort(registration.getPort());
		instance.setWeight(nacosDiscoveryProperties.getWeight());
		instance.setClusterName(nacosDiscoveryProperties.getClusterName());
		instance.setEnabled(nacosDiscoveryProperties.isInstanceEnabled());
		instance.setMetadata(registration.getMetadata());
		instance.setEphemeral(nacosDiscoveryProperties.isEphemeral());
		return instance;
	}

	/**
	 * 从服务管理器获取 NamingService 实例。
	 * @return NamingService 实例
	 */
	private NamingService namingService() {
		return nacosServiceManager.getNamingService();
	}

}
