package uw.nacos.discovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

/**
 * Nacos 服务发现查询封装。
 *
 * <p>持有一份 {@link NacosDiscoveryProperties} 与 {@link NacosServiceManager}，对外提供按
 * serviceId 查询健康实例列表（{@link #getInstances(String)}）和查询全部服务名
 * （{@link #getServices()}）的能力，内部委托给 Nacos {@link NamingService}。同时提供将
 * Nacos {@link Instance} 转换为 Spring Cloud {@link ServiceInstance}（{@link DefaultServiceInstance}）
 * 的静态工具方法。
 */
public class NacosServiceDiscovery {

	/** Nacos 服务发现配置属性，提供 group 等查询参数。 */
	private NacosDiscoveryProperties discoveryProperties;

	/** Nacos 服务管理器，负责懒加载 NamingService。 */
	private NacosServiceManager nacosServiceManager;

	/**
	 * 构造 Nacos 服务发现查询封装。
	 *
	 * @param discoveryProperties  Nacos 服务发现配置属性
	 * @param nacosServiceManager  Nacos 服务管理器
	 */
	public NacosServiceDiscovery(NacosDiscoveryProperties discoveryProperties,
			NacosServiceManager nacosServiceManager) {
		this.discoveryProperties = discoveryProperties;
		this.nacosServiceManager = nacosServiceManager;
	}

	/**
	 * 查询指定服务的全部健康实例。
	 *
	 * @param serviceId 服务 id
	 * @return 服务实例列表
	 * @throws NacosException Nacos 调用异常
	 */
	public List<ServiceInstance> getInstances(String serviceId) throws NacosException {
		String group = discoveryProperties.getGroup();
		List<Instance> instances = namingService().selectInstances(serviceId, group,
				true);
		return hostToServiceInstanceList(instances, serviceId);
	}

	/**
	 * 查询全部服务名列表。
	 *
	 * @return 服务名列表
	 * @throws NacosException Nacos 调用异常
	 */
	public List<String> getServices() throws NacosException {
		String group = discoveryProperties.getGroup();
		ListView<String> services = namingService().getServicesOfServer(1,
				Integer.MAX_VALUE, group);
		return services.getData();
	}

	/**
	 * 将 Nacos 实例列表批量转换为 Spring Cloud {@link ServiceInstance} 列表。
	 *
	 * @param instances Nacos 实例列表
	 * @param serviceId 服务 id
	 * @return 服务实例列表
	 */
	public static List<ServiceInstance> hostToServiceInstanceList(
			List<Instance> instances, String serviceId) {
		List<ServiceInstance> result = new ArrayList<>(instances.size());
		for (Instance instance : instances) {
			ServiceInstance serviceInstance = hostToServiceInstance(instance, serviceId);
			if (serviceInstance != null) {
				result.add(serviceInstance);
			}
		}
		return result;
	}

	/**
	 * 将单个 Nacos {@link Instance} 转换为 {@link DefaultServiceInstance}，未启用或不健康的实例返回 null。
	 *
	 * @param instance  Nacos 实例
	 * @param serviceId 服务 id
	 * @return 服务实例，实例不可用时返回 null
	 */
	public static ServiceInstance hostToServiceInstance(Instance instance,
			String serviceId) {
		if (instance == null || !instance.isEnabled() || !instance.isHealthy()) {
			return null;
		}
		Map<String, String> instanceMetadata = instance.getMetadata();
		Map<String, String> metadata = new HashMap<>(
				8 + (instanceMetadata == null ? 0 : instanceMetadata.size()));
		metadata.put("nacos.instanceId", instance.getInstanceId());
		metadata.put("nacos.weight", String.valueOf(instance.getWeight()));
		metadata.put("nacos.healthy", String.valueOf(instance.isHealthy()));
		metadata.put("nacos.cluster", String.valueOf(instance.getClusterName()));
		if (instanceMetadata != null) {
			metadata.putAll(instanceMetadata);
		}
		metadata.put("nacos.ephemeral", String.valueOf(instance.isEphemeral()));
		String secureValue = metadata.get("secure");
		boolean secure = secureValue != null && Boolean.parseBoolean(secureValue);
		return new DefaultServiceInstance(instance.getInstanceId(), serviceId,
				instance.getIp(), instance.getPort(), secure, metadata);
	}

	/**
	 * 从服务管理器获取 NamingService 实例。
	 * @return NamingService 实例
	 */
	private NamingService namingService() {
		return nacosServiceManager.getNamingService();
	}

}
