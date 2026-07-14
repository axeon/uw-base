package uw.nacos.discovery;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uw.nacos.common.context.support.PropertySourcesUtils;
import uw.nacos.config.NacosPropertiesPrefixer;
import com.alibaba.nacos.api.naming.PreservedMetadataKeys;
import com.alibaba.nacos.client.naming.utils.UtilAndComs;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import static com.alibaba.nacos.api.PropertyKeyConst.ACCESS_KEY;
import static com.alibaba.nacos.api.PropertyKeyConst.ENDPOINT;
import static com.alibaba.nacos.api.PropertyKeyConst.ENDPOINT_PORT;
import static com.alibaba.nacos.api.PropertyKeyConst.NAMESPACE;
import static com.alibaba.nacos.api.PropertyKeyConst.NAMING_LOAD_CACHE_AT_START;
import static com.alibaba.nacos.api.PropertyKeyConst.PASSWORD;
import static com.alibaba.nacos.api.PropertyKeyConst.SECRET_KEY;
import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static com.alibaba.nacos.api.PropertyKeyConst.USERNAME;

/**
 * Nacos 服务发现配置属性。
 *
 * <p>对应配置前缀 {@code spring.cloud.nacos.discovery}，封装 Nacos 注册中心地址、命名空间、
 * 鉴权、服务注册 IP/端口、权重、心跳、元数据等信息。在 Bean 初始化（{@link #init()}）阶段完成
 * IP 自动探测（支持 IPv4/IPv6）、环境变量回退（{@link #overrideFromEnv(Environment)}）以及
 * {@link NacosDiscoveryInfoChangedEvent} 发布。
 *
 * <p>{@link #getNacosProperties()} 将本配置转换为 Nacos client 原生的 {@link Properties}，
 * 供 {@link NacosServiceManager} 构建 NamingService 使用。
 */
@ConfigurationProperties("spring.cloud.nacos.discovery")
public class NacosDiscoveryProperties {

	/**
	 * 配置前缀常量：{@code spring.cloud.nacos.discovery}。
	 */
	private static final String PREFIX = "spring.cloud.nacos.discovery";

	/**
	 * 日志记录器。
	 */
	private static final Logger log = LoggerFactory
			.getLogger(NacosDiscoveryProperties.class);

	/**
	 * 用于将 kebab-case 配置键（形如 {@code heart-beat-interval}）转换为 camelCase
	 * （形如 {@code heartBeatInterval}）的正则：匹配连字符及其后第一个单词字符。
	 */
	private static final Pattern PATTERN = Pattern.compile("-(\\w)");

	/**
	 * IPv4 类型标识常量。
	 */
	private static final String IPV4 = "IPv4";

	/**
	 * IPv6 类型标识常量。
	 */
	private static final String IPV6 = "IPv6";

	/**
	 * Nacos 注册中心服务器地址，对应配置项 {@code spring.cloud.nacos.discovery.server-addr}。
	 */
	private String serverAddr;

	/**
	 * Nacos 鉴权用户名，对应配置项 {@code spring.cloud.nacos.discovery.username}。
	 */
	private String username;

	/**
	 * Nacos 鉴权密码，对应配置项 {@code spring.cloud.nacos.discovery.password}。
	 */
	private String password;

	/**
	 * 服务接入域名，通过该域名可动态获取 Nacos 服务器地址，对应配置项 {@code spring.cloud.nacos.discovery.endpoint}。
	 */
	private String endpoint;

	/**
	 * 命名空间，用于隔离不同环境的注册数据，对应配置项 {@code spring.cloud.nacos.discovery.namespace}。
	 */
	private String namespace;

	/**
	 * Nacos naming 日志文件名。
	 */
	private String logName;

	/**
	 * 注册到 Nacos 的服务名，对应配置项 {@code spring.cloud.nacos.discovery.service}，缺省回退到 {@code spring.application.name}。
	 */
	@Value("${spring.cloud.nacos.discovery.service:${spring.application.name:}}")
	private String service;

	/**
	 * 服务实例权重，值越大被分配到的流量越多，对应配置项 {@code spring.cloud.nacos.discovery.weight}。
	 */
	private float weight = 1;

	/**
	 * Nacos 集群名称，对应配置项 {@code spring.cloud.nacos.discovery.cluster-name}。
	 */
	private String clusterName;

	/**
	 * Nacos 分组名，对应配置项 {@code spring.cloud.nacos.discovery.group}，默认 {@code DEFAULT_GROUP}。
	 */
	private String group = "DEFAULT_GROUP";

	/**
	 * 应用启动时是否从本地缓存加载 naming 数据，对应配置项 {@code spring.cloud.nacos.discovery.naming-load-cache-at-start}。
	 */
	private String namingLoadCacheAtStart = "false";

	/**
	 * 注册时附加的扩展元数据，对应配置项 {@code spring.cloud.nacos.discovery.metadata}。
	 */
	private Map<String, String> metadata = new HashMap<>();

	/**
	 * 是否启用注册，设为 false 表示只订阅不注册，对应配置项 {@code spring.cloud.nacos.discovery.register-enabled}。
	 */
	private boolean registerEnabled = true;

	/**
	 * 注册的 IP 地址，未设置时自动探测，对应配置项 {@code spring.cloud.nacos.discovery.ip}。
	 */
	private String ip;

	/**
	 * 指定注册使用的网卡名称，对应配置项 {@code spring.cloud.nacos.discovery.network-interface}。
	 */
	private String networkInterface = "";

	/**
	 * IP 类型选择（IPv4 或 IPv6），未设置时默认 IPv4；选 IPv6 但无可用地址时自动回退 IPv4。
	 * 对应配置项 {@code spring.cloud.nacos.discovery.ip-type}。
	 */
	private String ipType;

	/**
	 * 注册的端口，未设置时自动探测，对应配置项 {@code spring.cloud.nacos.discovery.port}。
	 */
	private int port = -1;

	/**
	 * 服务是否为 HTTPS（安全）服务，对应配置项 {@code spring.cloud.nacos.discovery.secure}。
	 */
	private boolean secure = false;

	/**
	 * 命名空间的 access key（用于阿里云鉴权），对应配置项 {@code spring.cloud.nacos.discovery.access-key}。
	 */
	private String accessKey;

	/**
	 * 命名空间的 secret key（用于阿里云鉴权），对应配置项 {@code spring.cloud.nacos.discovery.secret-key}。
	 */
	private String secretKey;

	/**
	 * 心跳间隔（毫秒），对应配置项 {@code spring.cloud.nacos.discovery.heart-beat-interval}。
	 */
	private Integer heartBeatInterval;

	/**
	 * 心跳超时时间（毫秒），对应配置项 {@code spring.cloud.nacos.discovery.heart-beat-timeout}。
	 */
	private Integer heartBeatTimeout;

	/**
	 * IP 摘除超时时间（毫秒），对应配置项 {@code spring.cloud.nacos.discovery.ip-delete-timeout}。
	 */
	private Integer ipDeleteTimeout;

	/**
	 * 注册的实例是否可接收请求，默认 true，对应配置项 {@code spring.cloud.nacos.discovery.instance-enabled}。
	 */
	private boolean instanceEnabled = true;

	/**
	 * 注册的实例是否为临时实例，默认 true，对应配置项 {@code spring.cloud.nacos.discovery.ephemeral}。
	 */
	private boolean ephemeral = true;

	/**
	 * 注册失败是否快速失败（true 抛异常，false 仅记录日志），默认 true，对应配置项 {@code spring.cloud.nacos.discovery.fail-fast}。
	 */
	private boolean failFast = true;

	/**
	 * 优雅停机等待时间（毫秒），默认 10s。Spring Boot 关闭钩子触发后先从 Nacos 摘除实例，
	 * 再等待该时长后让 Tomcat 拒绝请求。对应配置项 {@code spring.cloud.nacos.discovery.graceful-shutdown-wait-time}。
	 */
	private Integer gracefulShutdownWaitTime = 10 * 1000;

	/**
	 * IPv6 地址探测工具，用于在未显式指定 IP 时查找本机 IPv6 地址。
	 */
	@Autowired
	private InetIPv6Utils inetIPv6Utils;

	/**
	 * Spring Cloud 提供的网络工具，用于探测本机非回环 IPv4 地址。
	 */
	@Autowired
	private InetUtils inetUtils;

	/**
	 * Spring 环境，用于在 {@link #overrideFromEnv(Environment)} 中解析配置占位符。
	 */
	@Autowired
	private Environment environment;

	/**
	 * Nacos 服务管理器，负责构建/管理 NamingService 并比对配置变更。
	 */
	@Autowired
	private NacosServiceManager nacosServiceManager;

	/**
	 * Spring 事件发布器，用于在初始化阶段发布 {@link NacosDiscoveryInfoChangedEvent}。
	 */
	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;

	/**
	 * Bean 初始化入口。填充注册来源元数据、规整 serverAddr/endpoint/namespace/logName、
	 * 按需自动探测注册 IP（支持指定网卡或 IPv4/IPv6 选择），随后回退解析 Environment 配置，
	 * 若配置发生变更则发布 {@link NacosDiscoveryInfoChangedEvent}，并最终将本配置注册到
	 * {@link NacosServiceManager}。
	 *
	 * @throws Exception 当指定网卡不存在、网卡上无可用 IP 或 ipType 取值非法时抛出
	 */
	@PostConstruct
	public void init() throws Exception {

		metadata.put(PreservedMetadataKeys.REGISTER_SOURCE, "SPRING_CLOUD");
		if (secure) {
			metadata.put("secure", "true");
		}

		serverAddr = Objects.toString(serverAddr, "");
		if (serverAddr.endsWith("/")) {
			serverAddr = serverAddr.substring(0, serverAddr.length() - 1);
		}
		endpoint = Objects.toString(endpoint, "");
		namespace = Objects.toString(namespace, "");
		logName = Objects.toString(logName, "");

		if (!StringUtils.hasLength(ip)) {
			// traversing network interfaces if didn't specify an interface
			if (!StringUtils.hasLength(networkInterface)) {
				if (ipType == null) {
					ip = inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
					String ipv6Addr = inetIPv6Utils.findIPv6Address();
					if (ipv6Addr != null) {
						metadata.put(IPV6, ipv6Addr);
					}
				}
				else if (IPV4.equalsIgnoreCase(ipType)) {
					ip = inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
				}
				else if (IPV6.equalsIgnoreCase(ipType)) {
					ip = inetIPv6Utils.findIPv6Address();
					if (!StringUtils.hasLength(ip)) {
						log.warn("There is no available IPv6 found. Spring Cloud Alibaba will automatically find IPv4.");
						ip = inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
					}
				}
				else {
					throw new IllegalArgumentException(
							"please checking the type of IP " + ipType);
				}
			}
			else {
				NetworkInterface netInterface = NetworkInterface
						.getByName(networkInterface);
				if (null == netInterface) {
					throw new IllegalArgumentException(
							"no such interface " + networkInterface);
				}

				Enumeration<InetAddress> inetAddress = netInterface.getInetAddresses();
				while (inetAddress.hasMoreElements()) {
					InetAddress currentAddress = inetAddress.nextElement();
					if ((currentAddress instanceof Inet4Address
							|| currentAddress instanceof Inet6Address)
							&& !currentAddress.isLoopbackAddress()) {
						ip = currentAddress.getHostAddress();
						break;
					}
				}

				if (!StringUtils.hasLength(ip)) {
					throw new RuntimeException("cannot find available ip from"
							+ " network interface " + networkInterface);
				}

			}
		}

		this.overrideFromEnv(environment);
		if (nacosServiceManager.isNacosDiscoveryInfoChanged(this)) {
			applicationEventPublisher
					.publishEvent(new NacosDiscoveryInfoChangedEvent(this));
		}
		nacosServiceManager.setNacosDiscoveryProperties(this);
	}

	/**
	 * @return 服务接入域名
	 */
	public String getEndpoint() {
		return endpoint;
	}

	/**
	 * @param endpoint 服务接入域名
	 */
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	/**
	 * @return 命名空间
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * @param namespace 命名空间
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * @return Nacos naming 日志文件名
	 */
	public String getLogName() {
		return logName;
	}

	/**
	 * @param logName Nacos naming 日志文件名
	 */
	public void setLogName(String logName) {
		this.logName = logName;
	}

	/**
	 * @return 服务实例权重
	 */
	public float getWeight() {
		return weight;
	}

	/**
	 * @param weight 服务实例权重
	 */
	public void setWeight(float weight) {
		this.weight = weight;
	}

	/**
	 * @return Nacos 集群名称
	 */
	public String getClusterName() {
		return clusterName;
	}

	/**
	 * @param clusterName Nacos 集群名称
	 */
	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	/**
	 * @return 注册到 Nacos 的服务名
	 */
	public String getService() {
		return service;
	}

	/**
	 * @param service 注册到 Nacos 的服务名
	 */
	public void setService(String service) {
		this.service = service;
	}

	/**
	 * @return 是否启用注册（false 表示只订阅不注册）
	 */
	public boolean isRegisterEnabled() {
		return registerEnabled;
	}

	/**
	 * @param registerEnabled 是否启用注册
	 */
	public void setRegisterEnabled(boolean registerEnabled) {
		this.registerEnabled = registerEnabled;
	}

	/**
	 * @return 注册的 IP 地址
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * @param ip 注册的 IP 地址
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}

	/**
	 * @return IP 类型选择（IPv4 或 IPv6）
	 */
	public String getIpType() {
		return ipType;
	}

	/**
	 * @param ipType IP 类型选择（IPv4 或 IPv6）
	 */
	public void setIpType(String ipType) {
		this.ipType = ipType;
	}

	/**
	 * @return 指定注册使用的网卡名称
	 */
	public String getNetworkInterface() {
		return networkInterface;
	}

	/**
	 * @param networkInterface 指定注册使用的网卡名称
	 */
	public void setNetworkInterface(String networkInterface) {
		this.networkInterface = networkInterface;
	}

	/**
	 * @return 注册的端口
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port 注册的端口
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return 服务是否为 HTTPS（安全）服务
	 */
	public boolean isSecure() {
		return secure;
	}

	/**
	 * @param secure 服务是否为 HTTPS（安全）服务
	 */
	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	/**
	 * @return 注册时附加的扩展元数据
	 */
	public Map<String, String> getMetadata() {
		return metadata;
	}

	/**
	 * @param metadata 注册时附加的扩展元数据
	 */
	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}

	/**
	 * @return Nacos 注册中心服务器地址
	 */
	public String getServerAddr() {
		return serverAddr;
	}

	/**
	 * @param serverAddr Nacos 注册中心服务器地址
	 */
	public void setServerAddr(String serverAddr) {
		this.serverAddr = serverAddr;
	}

	/**
	 * @return 命名空间的 access key
	 */
	public String getAccessKey() {
		return accessKey;
	}

	/**
	 * @param accessKey 命名空间的 access key
	 */
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	/**
	 * @return 命名空间的 secret key
	 */
	public String getSecretKey() {
		return secretKey;
	}

	/**
	 * @param secretKey 命名空间的 secret key
	 */
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	/**
	 * @return 心跳间隔（毫秒）
	 */
	public Integer getHeartBeatInterval() {
		return heartBeatInterval;
	}

	/**
	 * @param heartBeatInterval 心跳间隔（毫秒）
	 */
	public void setHeartBeatInterval(Integer heartBeatInterval) {
		this.heartBeatInterval = heartBeatInterval;
	}

	/**
	 * @return 心跳超时时间（毫秒）
	 */
	public Integer getHeartBeatTimeout() {
		return heartBeatTimeout;
	}

	/**
	 * @param heartBeatTimeout 心跳超时时间（毫秒）
	 */
	public void setHeartBeatTimeout(Integer heartBeatTimeout) {
		this.heartBeatTimeout = heartBeatTimeout;
	}

	/**
	 * @return IP 摘除超时时间（毫秒）
	 */
	public Integer getIpDeleteTimeout() {
		return ipDeleteTimeout;
	}

	/**
	 * @param ipDeleteTimeout IP 摘除超时时间（毫秒）
	 */
	public void setIpDeleteTimeout(Integer ipDeleteTimeout) {
		this.ipDeleteTimeout = ipDeleteTimeout;
	}

	/**
	 * @return 应用启动时是否从本地缓存加载 naming 数据
	 */
	public String getNamingLoadCacheAtStart() {
		return namingLoadCacheAtStart;
	}

	/**
	 * @param namingLoadCacheAtStart 应用启动时是否从本地缓存加载 naming 数据
	 */
	public void setNamingLoadCacheAtStart(String namingLoadCacheAtStart) {
		this.namingLoadCacheAtStart = namingLoadCacheAtStart;
	}

	/**
	 * @return Nacos 分组名
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * @param group Nacos 分组名
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * @return Nacos 鉴权用户名
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username Nacos 鉴权用户名
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return Nacos 鉴权密码
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password Nacos 鉴权密码
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return 注册的实例是否可接收请求
	 */
	public boolean isInstanceEnabled() {
		return instanceEnabled;
	}

	/**
	 * @param instanceEnabled 注册的实例是否可接收请求
	 */
	public void setInstanceEnabled(boolean instanceEnabled) {
		this.instanceEnabled = instanceEnabled;
	}

	/**
	 * @return 注册的实例是否为临时实例
	 */
	public boolean isEphemeral() {
		return ephemeral;
	}

	/**
	 * @param ephemeral 注册的实例是否为临时实例
	 */
	public void setEphemeral(boolean ephemeral) {
		this.ephemeral = ephemeral;
	}

	/**
	 * @return 注册失败是否快速失败
	 */
	public boolean isFailFast() {
		return failFast;
	}

	/**
	 * @param failFast 注册失败是否快速失败
	 */
	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}

	/**
	 * @return 优雅停机等待时间（毫秒）
	 */
	public Integer getGracefulShutdownWaitTime() {
		return gracefulShutdownWaitTime;
	}

	/**
	 * @param gracefulShutdownWaitTime 优雅停机等待时间（毫秒）
	 */
	public void setGracefulShutdownWaitTime(Integer gracefulShutdownWaitTime) {
		this.gracefulShutdownWaitTime = gracefulShutdownWaitTime;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>基于全部业务字段（含权重、注册开关、端口、安全标志、实例开关、临时标志、快速失败标志，
	 * 以及 serverAddr/username/password/endpoint/namespace/logName/service/clusterName/group/
	 * namingLoadCacheAtStart/metadata/ip/networkInterface/accessKey/secretKey/各超时时间）判定相等。
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		NacosDiscoveryProperties that = (NacosDiscoveryProperties) o;
		return Float.compare(that.weight, weight) == 0
				&& registerEnabled == that.registerEnabled && port == that.port
				&& secure == that.secure && instanceEnabled == that.instanceEnabled
				&& ephemeral == that.ephemeral
				&& Objects.equals(serverAddr, that.serverAddr)
				&& Objects.equals(username, that.username)
				&& Objects.equals(password, that.password)
				&& Objects.equals(endpoint, that.endpoint)
				&& Objects.equals(namespace, that.namespace)
				&& Objects.equals(logName, that.logName)
				&& Objects.equals(service, that.service)
				&& Objects.equals(clusterName, that.clusterName)
				&& Objects.equals(group, that.group)
				&& Objects.equals(namingLoadCacheAtStart, that.namingLoadCacheAtStart)
				&& Objects.equals(metadata, that.metadata) && Objects.equals(ip, that.ip)
				&& Objects.equals(networkInterface, that.networkInterface)
				&& Objects.equals(accessKey, that.accessKey)
				&& Objects.equals(secretKey, that.secretKey)
				&& Objects.equals(heartBeatInterval, that.heartBeatInterval)
				&& Objects.equals(heartBeatTimeout, that.heartBeatTimeout)
				&& Objects.equals(failFast, that.failFast)
				&& Objects.equals(ipDeleteTimeout, that.ipDeleteTimeout);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>使用全部业务字段计算哈希值，与 {@link #equals(Object)} 保持一致。
	 */
	@Override
	public int hashCode() {
		return Objects.hash(serverAddr, username, password, endpoint, namespace,
				logName, service, weight, clusterName, group,
				namingLoadCacheAtStart, metadata, registerEnabled, ip, networkInterface,
				port, secure, accessKey, secretKey, heartBeatInterval, heartBeatTimeout,
				ipDeleteTimeout, instanceEnabled, ephemeral, failFast);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>返回包含全部业务字段的可读字符串（注意：其中会包含 username/password/accessKey/secretKey
	 * 等敏感信息，日志输出时需谨慎）。
	 */
	@Override
	public String toString() {
		return "NacosDiscoveryProperties{" + "serverAddr='" + serverAddr + '\''
				+ ", username='" + username + '\'' + ", password='" + password + '\''
				+ ", endpoint='" + endpoint + '\'' + ", namespace='" + namespace + '\''
				+ ", logName='" + logName + '\'' + ", service='" + service + '\''
				+ ", weight=" + weight + ", clusterName='" + clusterName + '\''
				+ ", group='" + group + '\'' + ", namingLoadCacheAtStart='" + namingLoadCacheAtStart + '\''
				+ ", metadata=" + metadata + ", registerEnabled=" + registerEnabled
				+ ", ip='" + ip + '\'' + ", networkInterface='" + networkInterface + '\''
				+ ", port=" + port + ", secure=" + secure + ", accessKey='" + accessKey
				+ '\'' + ", secretKey='" + secretKey + '\'' + ", heartBeatInterval="
				+ heartBeatInterval + ", heartBeatTimeout=" + heartBeatTimeout
				+ ", ipDeleteTimeout=" + ipDeleteTimeout + ", instanceEnabled="
				+ instanceEnabled + ", ephemeral=" + ephemeral + ", failFast=" + failFast + '}';
	}

	/**
	 * 从 Spring {@link Environment} 中回退填充未显式设置的配置项。
	 *
	 * <p>逐项检查 serverAddr、namespace、accessKey、secretKey、logName、clusterName、
	 * endpoint、group、username、password，若当前值为空则尝试从环境变量解析对应占位符。
	 * 公共回退前缀为 {@code spring.cloud.nacos}，发现专属前缀为 {@code spring.cloud.nacos.discovery}。
	 *
	 * @param env Spring 环境
	 */
	public void overrideFromEnv(Environment env) {
		String discoveryPrefix = PREFIX;
		String nacosPrefix = NacosPropertiesPrefixer.DEFAULT_PREFIX;

		if (!StringUtils.hasLength(this.getServerAddr())) {
			String serverAddr = env
					.resolvePlaceholders("${" + discoveryPrefix + ".server-addr:}");
			if (!StringUtils.hasLength(serverAddr)) {
				serverAddr = env.resolvePlaceholders(
						"${" + nacosPrefix + ".server-addr:127.0.0.1:8848}");
			}
			this.setServerAddr(serverAddr);
		}
		if (!StringUtils.hasLength(this.getNamespace())) {
			this.setNamespace(env
					.resolvePlaceholders("${" + discoveryPrefix + ".namespace:}"));
		}
		if (!StringUtils.hasLength(this.getAccessKey())) {
			this.setAccessKey(env
					.resolvePlaceholders("${" + discoveryPrefix + ".access-key:}"));
		}
		if (!StringUtils.hasLength(this.getSecretKey())) {
			this.setSecretKey(env
					.resolvePlaceholders("${" + discoveryPrefix + ".secret-key:}"));
		}
		if (!StringUtils.hasLength(this.getLogName())) {
			this.setLogName(
					env.resolvePlaceholders("${" + discoveryPrefix + ".log-name:}"));
		}
		if (!StringUtils.hasLength(this.getClusterName())) {
			this.setClusterName(env.resolvePlaceholders(
					"${" + discoveryPrefix + ".cluster-name:}"));
		}
		if (!StringUtils.hasLength(this.getEndpoint())) {
			this.setEndpoint(
					env.resolvePlaceholders("${" + discoveryPrefix + ".endpoint:}"));
		}
		if (!StringUtils.hasLength(this.getGroup())) {
			this.setGroup(
					env.resolvePlaceholders("${" + discoveryPrefix + ".group:}"));
		}
		if (!StringUtils.hasLength(this.getUsername())) {
			this.setUsername(env.resolvePlaceholders("${" + nacosPrefix + ".username:}"));
		}
		if (!StringUtils.hasLength(this.getPassword())) {
			this.setPassword(env.resolvePlaceholders("${" + nacosPrefix + ".password:}"));
		}
	}

	/**
	 * 将本配置转换为 Nacos client 原生的 {@link Properties}，供构建 NamingService 使用。
	 *
	 * @return Nacos client 原生配置属性
	 */
	public Properties getNacosProperties() {
		Properties properties = new Properties();
		properties.put(SERVER_ADDR, serverAddr);
		properties.put(USERNAME, Objects.toString(username, ""));
		properties.put(PASSWORD, Objects.toString(password, ""));
		properties.put(NAMESPACE, namespace);
		properties.put(UtilAndComs.NACOS_NAMING_LOG_NAME, logName);

		if (endpoint.contains(":")) {
			int index = endpoint.indexOf(":");
			properties.put(ENDPOINT, endpoint.substring(0, index));
			properties.put(ENDPOINT_PORT, endpoint.substring(index + 1));
		}
		else {
			properties.put(ENDPOINT, endpoint);
		}

		properties.put(ACCESS_KEY, accessKey);
		properties.put(SECRET_KEY, secretKey);
		properties.put(NAMING_LOAD_CACHE_AT_START, namingLoadCacheAtStart);

		enrichNacosDiscoveryProperties(properties);
		return properties;
	}

	/**
	 * 将 Environment 中 {@code spring.cloud.nacos.discovery} 前缀下的全部子属性（键名经
	 * {@link #resolveKey(String)} 转 camelCase 后）以 {@code putIfAbsent} 方式补充到给定的
	 * Nacos 原生 {@link Properties} 中，避免覆盖已显式设置的标准键。
	 *
	 * @param nacosDiscoveryProperties 待补充的 Nacos 原生配置
	 */
	private void enrichNacosDiscoveryProperties(Properties nacosDiscoveryProperties) {
		Map<String, Object> properties = PropertySourcesUtils
				.getSubProperties((org.springframework.core.env.ConfigurableEnvironment) environment, PREFIX);
		properties.forEach((k, v) -> nacosDiscoveryProperties.putIfAbsent(resolveKey(k),
				String.valueOf(v)));
	}

	/**
	 * 将 kebab-case 配置键转换为 camelCase，例如 {@code heart-beat-interval} →
	 * {@code heartBeatInterval}，以适配 Nacos client 原生 Properties 的键命名约定。
	 *
	 * @param key 原始配置键（kebab-case）
	 * @return 转换后的配置键（camelCase）
	 */
	private String resolveKey(String key) {
		Matcher matcher = PATTERN.matcher(key);
		StringBuilder sb = new StringBuilder();
		while (matcher.find()) {
			matcher.appendReplacement(sb, matcher.group(1).toUpperCase(Locale.ROOT));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

}
