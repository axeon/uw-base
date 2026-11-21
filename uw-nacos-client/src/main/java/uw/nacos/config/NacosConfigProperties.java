package uw.nacos.config;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uw.nacos.common.context.support.PropertySourcesUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import static com.alibaba.nacos.api.PropertyKeyConst.ACCESS_KEY;
import static com.alibaba.nacos.api.PropertyKeyConst.CONFIG_LONG_POLL_TIMEOUT;
import static com.alibaba.nacos.api.PropertyKeyConst.CONFIG_RETRY_TIME;
import static com.alibaba.nacos.api.PropertyKeyConst.ENABLE_REMOTE_SYNC_CONFIG;
import static com.alibaba.nacos.api.PropertyKeyConst.ENCODE;
import static com.alibaba.nacos.api.PropertyKeyConst.ENDPOINT;
import static com.alibaba.nacos.api.PropertyKeyConst.ENDPOINT_PORT;
import static com.alibaba.nacos.api.PropertyKeyConst.MAX_RETRY;
import static com.alibaba.nacos.api.PropertyKeyConst.NAMESPACE;
import static com.alibaba.nacos.api.PropertyKeyConst.PASSWORD;
import static com.alibaba.nacos.api.PropertyKeyConst.SECRET_KEY;
import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static com.alibaba.nacos.api.PropertyKeyConst.USERNAME;

/**
 * Nacos 配置中心客户端属性绑定类。
 *
 * <p>绑定配置前缀 {@code spring.cloud.nacos.config}（公共前缀 {@code spring.cloud.nacos}），
 * 封装 Nacos Server 连接信息、鉴权信息与监听/刷新开关。基于这些属性构建 {@code ConfigService}
 * 并驱动 ConfigData 加载与动态刷新。多 dataId 场景统一通过 {@code spring.config.import} 声明，
 * 不再支持 SCA 的 shared-configs / extension-configs。
 *
 */
@ConfigurationProperties("spring.cloud.nacos.config")
public class NacosConfigProperties {

	/**
	 * dataId 与 group 复合键的分隔符常量：逗号（见 NacosPropertySourceRepository.getMapKey）。
	 */
	public static final String COMMAS = ",";

	/**
	 * Nacos 默认命名空间。
	 */
	private static final String DEFAULT_NAMESPACE = "public";

	/**
	 * Nacos 默认服务地址（host:port）。
	 */
	private static final String DEFAULT_ADDRESS = "127.0.0.1:8848";

	/**
	 * kebab-case 转驼峰用的正则：匹配横线加一个字母字符，捕获字母用于大写替换。
	 */
	private static final Pattern PATTERN = Pattern.compile("-(\\w)");

	/**
	 * 日志器。
	 */
	private static final Logger log = LoggerFactory
			.getLogger(NacosConfigProperties.class);

	/**
	 * Spring 环境，用于在 {@link #overrideFromEnv()} 中回退读取公共前缀下的连接/鉴权配置。
	 */
	@Autowired
	@JsonIgnore
	private Environment environment;
	/**
	 * Nacos Server 地址，例如 {@code 127.0.0.1:8848}。对应配置项
	 * {@code spring.cloud.nacos.config.server-addr}。
	 */
	private String serverAddr;
	/**
	 * Nacos 鉴权用户名。
	 */
	private String username;
	/**
	 * Nacos 鉴权密码。
	 */
	private String password;
	/**
	 * 配置内容的字符编码。
	 */
	private String encode;
	/**
	 * 配置所属分组，作为 dataId 的元信息，默认 {@code DEFAULT_GROUP}。
	 */
	private String group = "DEFAULT_GROUP";
	/**
	 * dataId 前缀，拼接 profile 与 fileExtension 后构成完整 dataId。
	 */
	private String prefix;
	/**
	 * dataId 的后缀，即配置文件扩展名，默认 {@code properties}。
	 */
	private String fileExtension = "properties";
	/**
	 * 从 Nacos 拉取配置的超时时间（毫秒），默认 3000。
	 */
	private int timeout = 3000;
	/**
	 * 服务端重连最大容忍次数。
	 */
	private String maxRetry;
	/**
	 * 长轮询超时时间。
	 */
	private String configLongPollTimeout;
	/**
	 * 拉取配置失败后的重试时间。
	 */
	private String configRetryTime;
	/**
	 * 是否在启动时主动同步远端配置并注册监听器；开启会有额外网络开销，推荐直接使用
	 * {@code ConfigService.getConfigAndSignListener}。
	 */
	private boolean enableRemoteSyncConfig = false;
	/**
	 * Nacos 接入点（ endpoint ），一个服务域名，通过它可以动态获取 server 地址。
	 */
	private String endpoint;
	/**
	 * 命名空间，用于隔离不同环境的配置。
	 */
	private String namespace;
	/**
	 * 命名空间的 accessKey。
	 */
	private String accessKey;
	/**
	 * 命名空间的 secretKey。
	 */
	private String secretKey;
	/**
	 * Nacos 集群名。
	 */
	private String clusterName;
	/**
	 * 配置刷新总开关，默认开启（true）。
	 */
	private boolean refreshEnabled = true;

	/**
	 * 初始化回调：从环境变量回退填充未显式设置的连接/鉴权属性。
	 */
	@PostConstruct
	public void init() {
		this.overrideFromEnv();
	}

	/**
	 * 从公共前缀（{@link NacosPropertiesPrefixer#DEFAULT_PREFIX}）下回退填充尚未显式设置的
	 * serverAddr / username / password，便于仅在 {@code spring.cloud.nacos} 一级声明这些公共字段。
	 */
	private void overrideFromEnv() {
		if (environment == null) {
			return;
		}

		String prefix = NacosPropertiesPrefixer.DEFAULT_PREFIX;

		if (!StringUtils.hasLength(this.getServerAddr())) {
			String serverAddr = environment
					.resolvePlaceholders("${" + prefix + ".config.server-addr:}");
			if (!StringUtils.hasLength(serverAddr)) {
				serverAddr = environment.resolvePlaceholders(
						"${" + prefix + ".server-addr:127.0.0.1:8848}");
			}
			this.setServerAddr(serverAddr);
		}
		if (!StringUtils.hasLength(this.getUsername())) {
			this.setUsername(
					environment.resolvePlaceholders("${" + prefix + ".username:}"));
		}
		if (!StringUtils.hasLength(this.getPassword())) {
			this.setPassword(
					environment.resolvePlaceholders("${" + prefix + ".password:}"));
		}
	}

	/**
	 * @return Nacos Server 地址
	 */
	public String getServerAddr() {
		return serverAddr;
	}

	/**
	 * @param serverAddr Nacos Server 地址
	 */
	public void setServerAddr(String serverAddr) {
		this.serverAddr = serverAddr;
	}

	/**
	 * @return 鉴权用户名
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username 鉴权用户名
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return 鉴权密码
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password 鉴权密码
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return dataId 前缀
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * @param prefix dataId 前缀
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * @return 配置文件扩展名
	 */
	public String getFileExtension() {
		return fileExtension;
	}

	/**
	 * @param fileExtension 配置文件扩展名
	 */
	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}

	/**
	 * @return 配置分组
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * @param group 配置分组
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * @return 拉取配置超时时间（毫秒）
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * @param timeout 拉取配置超时时间（毫秒）
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * @return 服务端重连最大容忍次数
	 */
	public String getMaxRetry() {
		return maxRetry;
	}

	/**
	 * @param maxRetry 服务端重连最大容忍次数
	 */
	public void setMaxRetry(String maxRetry) {
		this.maxRetry = maxRetry;
	}

	/**
	 * @return 长轮询超时时间
	 */
	public String getConfigLongPollTimeout() {
		return configLongPollTimeout;
	}

	/**
	 * @param configLongPollTimeout 长轮询超时时间
	 */
	public void setConfigLongPollTimeout(String configLongPollTimeout) {
		this.configLongPollTimeout = configLongPollTimeout;
	}

	/**
	 * @return 拉取配置失败后的重试时间
	 */
	public String getConfigRetryTime() {
		return configRetryTime;
	}

	/**
	 * @param configRetryTime 拉取配置失败后的重试时间
	 */
	public void setConfigRetryTime(String configRetryTime) {
		this.configRetryTime = configRetryTime;
	}

	/**
	 * @return 是否启用远端配置同步
	 */
	public boolean getEnableRemoteSyncConfig() {
		return enableRemoteSyncConfig;
	}

	/**
	 * @param enableRemoteSyncConfig 是否启用远端配置同步
	 */
	public void setEnableRemoteSyncConfig(boolean enableRemoteSyncConfig) {
		this.enableRemoteSyncConfig = enableRemoteSyncConfig;
	}

	/**
	 * @return Nacos 接入点
	 */
	public String getEndpoint() {
		return endpoint;
	}

	/**
	 * @param endpoint Nacos 接入点
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
	 * @return 命名空间 accessKey
	 */
	public String getAccessKey() {
		return accessKey;
	}

	/**
	 * @param accessKey 命名空间 accessKey
	 */
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	/**
	 * @return 命名空间 secretKey
	 */
	public String getSecretKey() {
		return secretKey;
	}

	/**
	 * @param secretKey 命名空间 secretKey
	 */
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	/**
	 * @return 配置内容字符编码
	 */
	public String getEncode() {
		return encode;
	}

	/**
	 * @param encode 配置内容字符编码
	 */
	public void setEncode(String encode) {
		this.encode = encode;
	}

	/**
	 * @return Nacos 集群名
	 */
	public String getClusterName() {
		return clusterName;
	}

	/**
	 * @param clusterName Nacos 集群名
	 */
	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	/**
	 * @return 是否启用配置刷新
	 */
	public boolean isRefreshEnabled() {
		return refreshEnabled;
	}

	/**
	 * @param refreshEnabled 是否启用配置刷新
	 */
	public void setRefreshEnabled(boolean refreshEnabled) {
		this.refreshEnabled = refreshEnabled;
	}

	/**
	 * 组装构造 ConfigService 所需的 Properties。将当前属性对象映射为 Nacos client
	 * 识别的键值（参见 {@code com.alibaba.nacos.api.PropertyKeyConst}）。
	 * @return 用于创建 ConfigService 的 Properties
	 */
	public Properties assembleConfigServiceProperties() {
		Properties properties = new Properties();
		properties.put(SERVER_ADDR, Objects.toString(this.serverAddr, ""));
		properties.put(USERNAME, Objects.toString(this.username, ""));
		properties.put(PASSWORD, Objects.toString(this.password, ""));
		properties.put(ENCODE, Objects.toString(this.encode, ""));
		properties.put(NAMESPACE, this.resolveNamespace());
		properties.put(ACCESS_KEY, Objects.toString(this.accessKey, ""));
		properties.put(SECRET_KEY, Objects.toString(this.secretKey, ""));
		properties.put("clusterName", Objects.toString(this.clusterName, ""));
		properties.put(MAX_RETRY, Objects.toString(this.maxRetry, ""));
		properties.put(CONFIG_LONG_POLL_TIMEOUT,
				Objects.toString(this.configLongPollTimeout, ""));
		properties.put(CONFIG_RETRY_TIME, Objects.toString(this.configRetryTime, ""));
		properties.put(ENABLE_REMOTE_SYNC_CONFIG,
				Objects.toString(this.enableRemoteSyncConfig, ""));
		String endpoint = Objects.toString(this.endpoint, "");
		if (endpoint.contains(":")) {
			int index = endpoint.indexOf(":");
			properties.put(ENDPOINT, endpoint.substring(0, index));
			properties.put(ENDPOINT_PORT, endpoint.substring(index + 1));
		}
		else {
			properties.put(ENDPOINT, endpoint);
		}

		enrichNacosConfigProperties(properties);

		// set default value when serverAddr and endpoint is empty
		if (!StringUtils.hasLength(this.serverAddr) && !StringUtils.hasLength(this.endpoint)) {
			properties.put(SERVER_ADDR, DEFAULT_ADDRESS);
		}

		return properties;
	}

	/**
	 * 解析命名空间：Nacos 的 public 命名空间在协议层用空串表示。
	 * @return 实际下发给 Nacos client 的命名空间
	 */
	private String resolveNamespace() {
		if (DEFAULT_NAMESPACE.equals(this.namespace)) {
			log.info("set nacos config namespace 'public' to ''");
			return "";
		}
		else {
			return Objects.toString(this.namespace, "");
		}
	}

	/**
	 * 用公共前缀下尚未被显式设置过的 config 子键补齐 Properties。
	 * @param nacosConfigProperties 待补齐的 Properties
	 */
	protected void enrichNacosConfigProperties(Properties nacosConfigProperties) {
		if (environment == null) {
			return;
		}
		String prefix = NacosPropertiesPrefixer.DEFAULT_PREFIX;

		Map<String, Object> properties = PropertySourcesUtils
				.getSubProperties((ConfigurableEnvironment) environment, prefix + ".config");
		properties.forEach((k, v) -> nacosConfigProperties.putIfAbsent(resolveKey(k),
				String.valueOf(v)));
	}

	/**
	 * 将 kebab-case 的键名转成驼峰，匹配 Nacos client PropertyKeyConst 的命名风格。
	 * @param key 原始键名
	 * @return 转换后的驼峰键名
	 */
	protected String resolveKey(String key) {
		Matcher matcher = PATTERN.matcher(key);
		StringBuilder sb = new StringBuilder();
		while (matcher.find()) {
			matcher.appendReplacement(sb, matcher.group(1).toUpperCase(Locale.ROOT));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	/**
	 * 在日志中屏蔽敏感字段，避免凭据泄露。
	 * @param value 原始值
	 * @return 屏蔽后的值
	 */
	private static String mask(String value) {
		return (value == null || value.isEmpty()) ? value : "******";
	}

	/**
	 * 摘要输出，敏感字段（accessKey / secretKey）做屏蔽处理。
	 * @return 摘要字符串
	 */
	@Override
	public String toString() {
		return "NacosConfigProperties{"
				+ "serverAddr='" + serverAddr + '\''
				+ ", encode='" + encode + '\''
				+ ", group='" + group + '\''
				+ ", prefix='" + prefix + '\''
				+ ", fileExtension='" + fileExtension + '\''
				+ ", timeout=" + timeout
				+ ", maxRetry='" + maxRetry + '\''
				+ ", configLongPollTimeout='" + configLongPollTimeout + '\''
				+ ", configRetryTime='" + configRetryTime + '\''
				+ ", enableRemoteSyncConfig=" + enableRemoteSyncConfig
				+ ", endpoint='" + endpoint + '\''
				+ ", namespace='" + namespace + '\''
				+ ", accessKey='" + mask(accessKey) + '\''
				+ ", secretKey='" + mask(secretKey) + '\''
				+ ", clusterName='" + clusterName + '\''
				+ ", refreshEnabled=" + refreshEnabled
				+ '}';
	}

}
