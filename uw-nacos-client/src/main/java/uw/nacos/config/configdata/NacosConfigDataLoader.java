package uw.nacos.config.configdata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import uw.nacos.config.NacosConfigManager;
import uw.nacos.config.NacosConfigProperties;
import uw.nacos.config.NacosPropertiesPrefixer;
import uw.nacos.config.NacosPropertySource;
import uw.nacos.config.NacosPropertySourceRepository;
import uw.nacos.config.parser.NacosDataParserHandler;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.apache.commons.logging.Log;

import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.PropertySource;

import static uw.nacos.config.configdata.ConfigPreference.LOCAL;
import static uw.nacos.config.configdata.ConfigPreference.REMOTE;
import static uw.nacos.config.configdata.NacosConfigDataResource.NacosItemConfig;
import static org.springframework.boot.context.config.ConfigData.Option;

/**
 * Nacos ConfigData 加载器，实现 Spring Boot 的 {@link ConfigDataLoader}。
 *
 * <p>在 ConfigData 机制下，根据 {@link NacosConfigDataResource} 从 Nacos 拉取配置内容，
 * 解析为 {@link PropertySource} 列表并包装为 {@link ConfigData}。同时根据
 * {@code spring.cloud.nacos.config.preference}（{@link ConfigPreference}）决定是否将远端配置
 * 标记为 PROFILE_SPECIFIC 以提升优先级。
 *
 * @since 2021.0.1.0
 */
public class NacosConfigDataLoader implements ConfigDataLoader<NacosConfigDataResource> {

	/**
	 * 延迟日志（ConfigData 早期阶段使用 DeferredLog）。
	 */
	private final Log log;

	/**
	 * @param logFactory 延迟日志工厂（ConfigData 早期阶段使用 DeferredLog）
	 */
	public NacosConfigDataLoader(DeferredLogFactory logFactory) {
		this.log = logFactory.getLog(getClass());
	}

	/**
	 * 从 Nacos 拉取配置、解析为属性源、登记到仓库，并按优先级包装为 ConfigData。
	 * @param context 加载上下文
	 * @param resource 待加载资源
	 * @return ConfigData（失败且非 optional 时抛异常；optional 失败返回 null）
	 * @throws ConfigDataResourceNotFoundException 非可选资源加载失败时抛出
	 */
	@Override
	public ConfigData load(ConfigDataLoaderContext context,
			NacosConfigDataResource resource) {
		try {
			ConfigService configService = getBean(context, NacosConfigManager.class)
					.getConfigService();
			NacosConfigProperties properties = getBean(context,
					NacosConfigProperties.class);

			NacosItemConfig config = resource.getConfig();
			// pull config from nacos
			List<PropertySource<?>> propertySources = pullConfig(configService,
					config.getGroup(), config.getDataId(), config.getSuffix(),
					properties.getTimeout());

			NacosPropertySource propertySource = new NacosPropertySource(propertySources,
					config.getGroup(), config.getDataId(), new Date(),
					config.isRefreshEnabled());

			NacosPropertySourceRepository.collectNacosPropertySource(propertySource);

			return new ConfigData(propertySources, getOptions(context, resource));
		}
		catch (Exception e) {
			log.error("Error getting properties from nacos: " + resource, e);
			if (!resource.isOptional()) {
				throw new ConfigDataResourceNotFoundException(resource, e);
			}
		}
		return null;
	}

	/**
	 * 计算 ConfigData 选项：固定 IGNORE_IMPORTS/IGNORE_PROFILES；preference 为 REMOTE 时附加
	 * PROFILE_SPECIFIC 以提升优先级。
	 * @param context 加载上下文
	 * @param resource 待加载资源
	 * @return 选项数组
	 */
	private Option[] getOptions(ConfigDataLoaderContext context,
			NacosConfigDataResource resource) {
		List<Option> options = new ArrayList<>();
		options.add(Option.IGNORE_IMPORTS);
		options.add(Option.IGNORE_PROFILES);
		if (getPreference(context, resource) == REMOTE) {
			// mark it as 'PROFILE_SPECIFIC' config, it has higher priority,
			// will override the none profile specific config.
			options.add(Option.PROFILE_SPECIFIC);
		}
		return options.toArray(new Option[0]);
	}

	/**
	 * 解析生效的配置优先级：binder 绑定全局 preference，resource 上的 per-config preference 优先覆盖。
	 * @param context 加载上下文
	 * @param resource 待加载资源
	 * @return 生效的 ConfigPreference（默认 LOCAL）
	 */
	private ConfigPreference getPreference(ConfigDataLoaderContext context,
			NacosConfigDataResource resource) {
		Binder binder = context.getBootstrapContext().get(Binder.class);

		ConfigPreference preference = binder
				.bind(NacosPropertiesPrefixer.DEFAULT_PREFIX + ".config.preference", ConfigPreference.class)
				.orElse(LOCAL);
		String specificPreference = resource.getConfig().getPreference();
		if (specificPreference != null) {
			try {
				preference = ConfigPreference.valueOf(specificPreference.toUpperCase(Locale.ROOT));
			}
			catch (IllegalArgumentException ignore) {
				// illegal preference value, just ignore.
				log.error(String.format(
						"illegal preference value: %s, using default preference: %s",
						specificPreference, preference));
			}
		}
		return preference;
	}

	/**
	 * 调用 ConfigService.getConfig 拉取配置原文，并交由 {@link NacosDataParserHandler} 按后缀解析。
	 * @param configService Nacos 配置服务
	 * @param group 配置 group
	 * @param dataId 配置 dataId
	 * @param suffix 文件后缀
	 * @param timeout 拉取超时（毫秒）
	 * @return 解析后的属性源列表
	 * @throws NacosException Nacos 客户端异常
	 * @throws IOException 解析失败
	 */
	private List<PropertySource<?>> pullConfig(ConfigService configService, String group,
			String dataId, String suffix, long timeout)
			throws NacosException, IOException {
		String config = configService.getConfig(dataId, group, timeout);
		logLoadInfo(group, dataId, config);
		String configName = group + "@" + dataId;
		return NacosDataParserHandler.getInstance().parseNacosData(configName, config, suffix);
	}

	/**
	 * 输出拉取结果日志：成功/空内容/DEBUG 级原文。
	 * @param group 配置 group
	 * @param dataId 配置 dataId
	 * @param config 配置原文（可能为 null）
	 */
	private void logLoadInfo(String group, String dataId, String config) {
		if (config != null) {
			if (log.isInfoEnabled()) {
				log.info(String.format(
						"[Nacos Config] Load config[dataId=%s, group=%s] success", dataId,
						group));
			}
		}
		else if (log.isWarnEnabled()) {
			log.warn(String.format("[Nacos Config] config[dataId=%s, group=%s] is empty",
					dataId, group));
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format(
					"[Nacos Config] config[dataId=%s, group=%s] content: \n%s", dataId,
					group, config));
		}
	}

	/**
	 * 从 BootstrapRegistry 取 Bean，未注册返回 null。
	 * @param <T> Bean 类型
	 * @param context 加载上下文
	 * @param type Bean 类型
	 * @return Bean 或 null
	 */
	private <T> T getBean(ConfigDataLoaderContext context, Class<T> type) {
		if (context.getBootstrapContext().isRegistered(type)) {
			return context.getBootstrapContext().get(type);
		}
		return null;
	}

}
