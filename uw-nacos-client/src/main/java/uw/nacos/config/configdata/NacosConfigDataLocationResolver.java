package uw.nacos.config.configdata;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uw.nacos.config.NacosConfigManager;
import uw.nacos.config.NacosConfigProperties;
import uw.nacos.config.NacosPropertiesPrefixer;

import org.springframework.boot.bootstrap.BootstrapRegistry;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import static uw.nacos.config.configdata.NacosConfigDataResource.NacosItemConfig;

/**
 * Nacos ConfigData location 解析器，实现 Spring Boot 的 {@link ConfigDataLocationResolver}。
 *
 * <p>解析 {@code spring.config.import=nacos:...} 中的 {@code nacos:} 前缀位置，将其转换为
 * {@link NacosConfigDataResource}。ConfigData location 的前缀 {@code nacos:} 是 URI scheme
 * （不是配置前缀），保持不变；配置项前缀固定为 {@code spring.cloud.nacos.config.*}。
 *
 * @since 2021.0.1.0
 */
public class NacosConfigDataLocationResolver
		implements ConfigDataLocationResolver<NacosConfigDataResource>, Ordered {
	/**
	 * ConfigData location 的前缀（{@code spring.config.import} 的 URI scheme），固定为 {@code nacos:}。
	 */
	public static final String PREFIX = "nacos:";
	/**
	 * URI query 参数中的 group 键名。
	 */
	private static final String GROUP = "group";

	/**
	 * {@code spring.config.import} 属性 key。
	 */
	private static final String SPRING_CONFIG_IMPORT_PROPERTIES = "spring.config.import";

	/**
	 * URI query 参数中的刷新开关键名。
	 */
	private static final String REFRESH_ENABLED = "refreshEnabled";
	/**
	 * URI query 参数中的优先级键名（local/remote）。
	 */
	private static final String PREFERENCE = "preference";
	/**
	 * @param logFactory 延迟日志工厂（Boot 注入；本解析器不直接记日志，仅满足 ConfigDataLocationResolver 构造约定）
	 */
	public NacosConfigDataLocationResolver(DeferredLogFactory logFactory) {
	}

	/**
	 * @return 解析器顺序（-1，优先于默认 resolver）
	 */
	@Override
	public int getOrder() {
		return -1;
	}

	/**
	 * 优先从 BootstrapRegistry 复用已绑定的属性，否则按公共前缀+config 子前缀从 Binder 绑定。
	 * @param context 解析上下文
	 * @return 绑定后的 NacosConfigProperties
	 */
	private NacosConfigProperties loadProperties(
			ConfigDataLocationResolverContext context) {
		Binder binder = context.getBinder();
		BindHandler bindHandler = getBindHandler(context);

		NacosConfigProperties nacosConfigProperties;
		if (context.getBootstrapContext().isRegistered(NacosConfigProperties.class)) {
			nacosConfigProperties = context.getBootstrapContext()
					.get(NacosConfigProperties.class);
		}
		else {
			String nacosPrefix = NacosPropertiesPrefixer.DEFAULT_PREFIX;

			String nacosConfigPrefix = nacosPrefix + ".config";

			nacosConfigProperties = binder
					.bind(nacosPrefix, Bindable.of(NacosConfigDataLoadProperties.class),
							bindHandler)
					.map(properties -> binder
							.bind(nacosConfigPrefix,
									Bindable.ofInstance(properties), bindHandler)
							.orElse(properties))
					.orElseGet(() -> binder
							.bind(nacosConfigPrefix,
									Bindable.of(NacosConfigDataLoadProperties.class), bindHandler)
							.orElseGet(NacosConfigDataLoadProperties::new));
		}

		return nacosConfigProperties;
	}

	/**
	 * 从 BootstrapRegistry 获取 BindHandler，未注册则返回 null。
	 * @param context 解析上下文
	 * @return BindHandler 或 null
	 */
	private BindHandler getBindHandler(ConfigDataLocationResolverContext context) {
		return context.getBootstrapContext().getOrElse(BindHandler.class, null);
	}

	/**
	 * 判断该 location 是否可被本解析器处理：必须以 {@code nacos:} 开头且 nacos config 已启用。
	 * @param context 解析上下文
	 * @param location 待判定位置
	 * @return 是否可解析
	 */
	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context,
			ConfigDataLocation location) {
		if (!location.hasPrefix(PREFIX)) {
			return false;
		}
		return context.getBinder()
				.bind(NacosPropertiesPrefixer.DEFAULT_PREFIX + ".config.enabled", Boolean.class)
				.orElse(true);
	}

	/**
	 * 不支持无 profile 解析（始终返回空列表），Nacos 配置统一走
	 * {@link #resolveProfileSpecific(ConfigDataLocationResolverContext, ConfigDataLocation, Profiles)}。
	 * @param context 解析上下文
	 * @param location 待解析位置
	 * @return 空列表
	 * @throws ConfigDataLocationNotFoundException 位置缺失
	 * @throws ConfigDataResourceNotFoundException 资源缺失
	 */
	@Override
	public List<NacosConfigDataResource> resolve(
			ConfigDataLocationResolverContext context, ConfigDataLocation location)
			throws ConfigDataLocationNotFoundException,
			ConfigDataResourceNotFoundException {
		return Collections.emptyList();
	}

	/**
	 * 解析 profile 专属的 Nacos ConfigData 资源：绑定属性、注册 ConfigService/ConfigManager 到
	 * BootstrapRegistry，并基于 location URI 构造 {@link NacosConfigDataResource}。
	 * @param resolverContext 解析上下文
	 * @param location 待解析位置
	 * @param profiles 激活 profiles
	 * @return NacosConfigDataResource 列表
	 * @throws ConfigDataLocationNotFoundException 位置缺失
	 */
	@Override
	public List<NacosConfigDataResource> resolveProfileSpecific(
			ConfigDataLocationResolverContext resolverContext,
			ConfigDataLocation location, Profiles profiles)
			throws ConfigDataLocationNotFoundException {
		NacosConfigProperties properties = loadProperties(resolverContext);

		ConfigurableBootstrapContext bootstrapContext = resolverContext
				.getBootstrapContext();

		bootstrapContext.registerIfAbsent(NacosConfigProperties.class,
				BootstrapRegistry.InstanceSupplier.of(properties));

		registerConfigManager(properties, bootstrapContext, resolverContext);

		return loadConfigDataResources(location, profiles, properties);
	}

	/**
	 * 从 location URI 解析出 group/dataId/suffix/refresh/preference，构造单个 NacosConfigDataResource。
	 * @param location ConfigData 位置
	 * @param profiles 激活 profiles
	 * @param properties Nacos 配置属性
	 * @return 单元素资源列表
	 */
	private List<NacosConfigDataResource> loadConfigDataResources(
			ConfigDataLocation location, Profiles profiles,
			NacosConfigProperties properties) {
		URI uri = getUri(location, properties);
		Map<String, String> queryMap = getQueryMap(uri);
		String dataId = dataIdFor(uri);

		if (!StringUtils.hasText(dataId)) {
			throw new IllegalArgumentException("dataId must be specified");
		}

		return Collections.singletonList(new NacosConfigDataResource(properties,
				location.isOptional(), profiles,
				new NacosItemConfig().setGroup(groupFor(queryMap, properties))
						.setDataId(dataId).setSuffix(suffixFor(dataId, properties))
						.setRefreshEnabled(refreshEnabledFor(queryMap, properties))
						.setPreference(preferenceFor(queryMap))));
	}

	/**
	 * 从 query 参数取 preference（local/remote），未指定返回 null。
	 * @param queryMap URI query 参数
	 * @return preference 或 null
	 */
	private String preferenceFor(Map<String, String> queryMap) {
		return queryMap.get(PREFERENCE);
	}

	/**
	 * 基于 server-addr 与 location 路径拼接 URI。
	 * @param location ConfigData 位置
	 * @param properties Nacos 配置属性
	 * @return 解析后的 URI
	 */
	private URI getUri(ConfigDataLocation location, NacosConfigProperties properties) {
		String path = location.getNonPrefixedValue(PREFIX);
		if (!StringUtils.hasText(path)) {
			path = "/";
		}
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		String uri = properties.getServerAddr() + path;
		return getUri(uri);
	}

	/**
	 * 在 BootstrapRegistry 注册 NacosConfigManager 单例（仅当 spring.config.import 非空且未注册时）。
	 * @param properties Nacos 配置属性
	 * @param bootstrapContext 引导上下文
	 * @param resolverContext 解析上下文
	 */
	private void registerConfigManager(NacosConfigProperties properties,
			ConfigurableBootstrapContext bootstrapContext,
			ConfigDataLocationResolverContext resolverContext) {
		List<?> springConfigImportProperties = resolverContext.getBinder()
				.bind(SPRING_CONFIG_IMPORT_PROPERTIES, List.class).get();
		if (!springConfigImportProperties.isEmpty() && !bootstrapContext.isRegistered(NacosConfigManager.class)) {
			bootstrapContext.register(NacosConfigManager.class,
					BootstrapRegistry.InstanceSupplier.of(NacosConfigManager.getInstance(properties)));
		}
	}

	/**
	 * 将字符串 URI 标准化（补默认 http scheme）并解析为 URI 对象。
	 * @param uris URI 字符串
	 * @return URI 对象
	 * @throws IllegalArgumentException URI 非法时抛出
	 */
	private URI getUri(String uris) {
		if (!uris.startsWith("http://") && !uris.startsWith("https://")) {
			uris = "http://" + uris;
		}
		URI uri;
		try {
			uri = new URI(uris);
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("illegal URI: " + uris);
		}
		return uri;
	}

	/**
	 * 取 group：query 参数优先，缺失回退到 properties 默认 group。
	 * @param queryMap URI query 参数
	 * @param properties Nacos 配置属性
	 * @return group
	 */
	private String groupFor(Map<String, String> queryMap, NacosConfigProperties properties) {
		return queryMap.getOrDefault(GROUP, properties.getGroup());
	}

	/**
	 * 解析 URI query 串为键值 Map。
	 * @param uri URI
	 * @return query 参数 Map（无 query 时返回空 Map）
	 */
	private Map<String, String> getQueryMap(URI uri) {
		String query = uri.getQuery();
		if (!StringUtils.hasText(query)) {
			return Collections.emptyMap();
		}
		Map<String, String> result = new HashMap<>(4);
		for (String entry : query.split("&")) {
			String[] kv = entry.split("=", 2);
			if (kv.length == 2) {
				result.put(kv[0], kv[1]);
			}
		}
		return result;
	}

	/**
	 * 推断 dataId 后缀：dataId 含点号则取末段，否则回退到 properties.fileExtension。
	 * @param uri URI
	 * @param properties Nacos 配置属性
	 * @return 文件后缀
	 */
	private String suffixFor(String dataId, NacosConfigProperties properties) {
		String ext = StringUtils.getFilenameExtension(dataId);
		return StringUtils.hasText(ext) ? ext : properties.getFileExtension();
	}

	/**
	 * 取刷新开关：query 参数优先，缺失回退到 properties.refreshEnabled。
	 * @param queryMap URI query 参数
	 * @param properties Nacos 配置属性
	 * @return 是否启用刷新
	 */
	private boolean refreshEnabledFor(Map<String, String> queryMap, NacosConfigProperties properties) {
		String refresh = queryMap.get(REFRESH_ENABLED);
		return refresh != null ? Boolean.parseBoolean(refresh) : properties.isRefreshEnabled();
	}

	/**
	 * 从 URI path 段取 dataId（取第一段，多段或空视为非法）。
	 * @param uri URI
	 * @return dataId
	 * @throws IllegalArgumentException dataId 非法时抛出
	 */
	private String dataIdFor(URI uri) {
		String path = uri.getPath();
		// notice '/'
		if (path == null || path.length() <= 1) {
			return "";
		}
		String[] parts = path.substring(1).split("/");
		if (parts.length != 1) {
			throw new IllegalArgumentException("illegal dataId");
		}
		return parts[0];
	}

}
