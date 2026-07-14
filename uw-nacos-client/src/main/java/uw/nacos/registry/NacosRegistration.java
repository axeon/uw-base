package uw.nacos.registry;

import java.net.URI;
import java.util.List;
import java.util.Map;

import uw.nacos.discovery.NacosDiscoveryProperties;
import com.alibaba.nacos.api.naming.PreservedMetadataKeys;
import jakarta.annotation.PostConstruct;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ManagementServerPortUtils;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Nacos 服务注册信息，实现 Spring Cloud Commons 的 {@link Registration} 接口。
 *
 * <p>作为注册到 Nacos 的服务实例元信息载体，包装 {@link NacosDiscoveryProperties} 并在
 * {@link #init()} 阶段补充 management 端口、上下文路径、心跳参数等元数据。提供
 * serviceId、host、port、secure、metadata 等供 {@code NacosServiceRegistry} 构造 Nacos
 * {@code Instance} 使用。
 */
public class NacosRegistration implements Registration {

	/** management 端口元数据 key，对应 {@code management.server.port}。 */
	private static final String MANAGEMENT_PORT = "management.port";

	/** management 上下文路径元数据 key，对应 {@code management.server.servlet.context-path}。 */
	private static final String MANAGEMENT_CONTEXT_PATH = "management.context-path";

	/** management 地址元数据 key，对应 {@code management.server.address}。 */
	private static final String MANAGEMENT_ADDRESS = "management.address";

	/** management 端点基础路径元数据 key，对应 {@code management.endpoints.web.base-path}。 */
	private static final String MANAGEMENT_ENDPOINT_BASE_PATH = "management.endpoints.web.base-path";

	/** 注册信息定制器列表（可选），在 {@link #init()} 阶段依次执行。 */
	private final List<NacosRegistrationCustomizer> registrationCustomizers;

	/** 包装的 Nacos 服务发现配置属性，作为注册元信息的实际数据来源。 */
	private final NacosDiscoveryProperties nacosDiscoveryProperties;

	/** Spring 应用上下文，用于读取 Environment 中的 management 相关配置。 */
	private final ApplicationContext context;

	/**
	 * 构造 Nacos 注册信息载体。
	 *
	 * @param registrationCustomizers   注册信息定制器列表（可选）
	 * @param nacosDiscoveryProperties  Nacos 服务发现配置属性
	 * @param context                   Spring 应用上下文
	 */
	public NacosRegistration(List<NacosRegistrationCustomizer> registrationCustomizers,
			NacosDiscoveryProperties nacosDiscoveryProperties,
			ApplicationContext context) {
		this.registrationCustomizers = registrationCustomizers;
		this.nacosDiscoveryProperties = nacosDiscoveryProperties;
		this.context = context;
	}

	/**
	 * Bean 初始化回调，补充 management 端口/上下文路径/地址、心跳参数等元数据，并执行定制器。
	 */
	@PostConstruct
	public void init() {

		Map<String, String> metadata = nacosDiscoveryProperties.getMetadata();
		Environment env = context.getEnvironment();

		String endpointBasePath = env.getProperty(MANAGEMENT_ENDPOINT_BASE_PATH);
		if (StringUtils.hasLength(endpointBasePath)) {
			metadata.put(MANAGEMENT_ENDPOINT_BASE_PATH, endpointBasePath);
		}

		Integer managementPort = ManagementServerPortUtils.getPort(context);
		if (null != managementPort) {
			metadata.put(MANAGEMENT_PORT, managementPort.toString());
			String contextPath = env
					.getProperty("management.server.servlet.context-path");
			String address = env.getProperty("management.server.address");
			if (StringUtils.hasLength(contextPath)) {
				metadata.put(MANAGEMENT_CONTEXT_PATH, contextPath);
			}
			if (StringUtils.hasLength(address)) {
				metadata.put(MANAGEMENT_ADDRESS, address);
			}
		}

		if (null != nacosDiscoveryProperties.getHeartBeatInterval()) {
			metadata.put(PreservedMetadataKeys.HEART_BEAT_INTERVAL,
					nacosDiscoveryProperties.getHeartBeatInterval().toString());
		}
		if (null != nacosDiscoveryProperties.getHeartBeatTimeout()) {
			metadata.put(PreservedMetadataKeys.HEART_BEAT_TIMEOUT,
					nacosDiscoveryProperties.getHeartBeatTimeout().toString());
		}
		if (null != nacosDiscoveryProperties.getIpDeleteTimeout()) {
			metadata.put(PreservedMetadataKeys.IP_DELETE_TIMEOUT,
					nacosDiscoveryProperties.getIpDeleteTimeout().toString());
		}
		customize(registrationCustomizers);
	}

	/**
	 * 依次执行所有注册信息定制器，对当前注册信息进行自定义加工。
	 *
	 * @param registrationCustomizers 注册信息定制器列表
	 */
	private void customize(
			List<NacosRegistrationCustomizer> registrationCustomizers) {
		if (registrationCustomizers != null) {
			for (NacosRegistrationCustomizer customizer : registrationCustomizers) {
				customizer.customize(this);
			}
		}
	}

	/**
	 * @return 服务 id（service 名）
	 */
	@Override
	public String getServiceId() {
		return nacosDiscoveryProperties.getService();
	}

	/**
	 * @return 注册的实例 IP（host）
	 */
	@Override
	public String getHost() {
		return nacosDiscoveryProperties.getIp();
	}

	/**
	 * @return 注册的实例端口
	 */
	@Override
	public int getPort() {
		return nacosDiscoveryProperties.getPort();
	}

	/**
	 * @param port 注册端口
	 */
	public void setPort(int port) {
		this.nacosDiscoveryProperties.setPort(port);
	}

	/**
	 * @return 是否启用 HTTPS（secure）
	 */
	@Override
	public boolean isSecure() {
		return nacosDiscoveryProperties.isSecure();
	}

	/**
	 * @return 基于 host/port/secure 拼接的服务实例 URI
	 */
	@Override
	public URI getUri() {
		return ServiceInstance.createUri(this);
	}

	/**
	 * @return 注册元数据（含心跳参数、management 信息、Nacos 内置字段等）
	 */
	@Override
	public Map<String, String> getMetadata() {
		return nacosDiscoveryProperties.getMetadata();
	}

	/**
	 * @return 包装的 Nacos 服务发现配置属性
	 */
	public NacosDiscoveryProperties getNacosDiscoveryProperties() {
		return nacosDiscoveryProperties;
	}

	/**
	 * 返回注册信息的字符串表示，用户名与密码已脱敏。
	 * @return 脱敏后的注册信息字符串
	 */
	@Override
	public String toString() {
		return "NacosRegistration{" + "serviceId='" + nacosDiscoveryProperties.getService()
				+ '\'' + ", host='" + nacosDiscoveryProperties.getIp() + '\''
				+ ", port=" + nacosDiscoveryProperties.getPort()
				+ ", secure=" + nacosDiscoveryProperties.isSecure()
				+ ", group='" + nacosDiscoveryProperties.getGroup() + '\''
				+ ", username='" + mask(nacosDiscoveryProperties.getUsername()) + '\''
				+ ", password='" + mask(nacosDiscoveryProperties.getPassword()) + '\''
				+ '}';
	}

	/**
	 * 对非空敏感值脱敏为 {@code ******}，空值原样返回。
	 *
	 * @param value 待脱敏的原始值
	 * @return 脱敏后的字符串
	 */
	private static String mask(String value) {
		return (value == null || value.isEmpty()) ? value : "******";
	}

}
