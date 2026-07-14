package uw.nacos.discovery;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * 基于 Nacos 的阻塞式 {@link DiscoveryClient} 实现。
 *
 * <p>实现 Spring Cloud Commons 的 {@link DiscoveryClient} 接口，将服务实例查询和服务列表查询
 * 委托给 {@link NacosServiceDiscovery}（内部调用 Nacos {@code NamingService}）。
 */
public class NacosDiscoveryClient implements DiscoveryClient {

	/** 日志记录器。 */
	private static final Logger log = LoggerFactory.getLogger(NacosDiscoveryClient.class);

	/**
	 * Nacos Discovery Client 描述信息。
	 */
	private static final String DESCRIPTION = "Spring Cloud Nacos Discovery Client";

	/** 委托的 Nacos 服务发现查询封装，负责实际调用 Nacos NamingService。 */
	private NacosServiceDiscovery serviceDiscovery;

	/**
	 * 构造阻塞式 Nacos 发现客户端。
	 *
	 * @param nacosServiceDiscovery 委托的 Nacos 服务发现查询封装
	 */
	public NacosDiscoveryClient(NacosServiceDiscovery nacosServiceDiscovery) {
		this.serviceDiscovery = nacosServiceDiscovery;
	}

	/**
	 * 返回该发现客户端的描述信息。
	 * @return 客户端描述字符串
	 */
	@Override
	public String description() {
		return DESCRIPTION;
	}

	/**
	 * 查询指定服务的全部健康实例列表。
	 * @param serviceId 服务 id
	 * @return 服务实例列表
	 * @throws RuntimeException 查询失败时抛出
	 */
	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		try {
			return serviceDiscovery.getInstances(serviceId);
		}
		catch (Exception e) {
			throw new RuntimeException(
					"Can not get hosts from nacos server. serviceId: " + serviceId, e);
		}
	}

	/**
	 * 查询全部已注册的服务名列表，查询失败时返回空列表而非抛异常。
	 * @return 服务名列表，异常时返回 {@link Collections#emptyList()}
	 */
	@Override
	public List<String> getServices() {
		try {
			return serviceDiscovery.getServices();
		}
		catch (Exception e) {
			log.error("get service name from nacos server failed.", e);
			return Collections.emptyList();
		}
	}

}
