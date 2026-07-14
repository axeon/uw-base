package uw.nacos.config.refresh;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import uw.nacos.config.NacosConfigManager;
import uw.nacos.config.NacosPropertySource;
import uw.nacos.config.NacosPropertySourceRepository;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractSharedListener;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;

/**
 * Nacos 配置上下文刷新器。
 *
 * <p>应用启动（{@link ApplicationReadyEvent}）后，为所有标记可刷新的 {@link NacosPropertySource}
 * 向 Nacos 注册监听器；远端配置变更时直接发布 Spring Cloud 标准 {@link RefreshEvent}，由
 * {@code ContextRefresher} 驱动 ConfigData 重新加载，完成 {@code @ConfigurationProperties}
 * 与 {@code @RefreshScope} 的刷新。
 *
 */
public class NacosContextRefresher
		implements ApplicationListener<ApplicationReadyEvent>, ApplicationContextAware {

	/**
	 * 日志器。
	 */
	private final static Logger log = LoggerFactory
			.getLogger(NacosContextRefresher.class);

	/**
	 * 是否启用配置刷新（构造期一次性确定）。
	 */
	private final boolean isRefreshEnabled;
	/**
	 * Nacos 配置服务，懒初始化。
	 */
	private volatile ConfigService configService;

	/**
	 * Nacos 配置管理器。
	 */
	private final NacosConfigManager configManager;

	/**
	 * Spring 应用上下文（用于发布 RefreshEvent）。
	 */
	private volatile ApplicationContext applicationContext;

	/**
	 * 是否已就绪标志，CAS 防止多 ApplicationContext 重复注册。
	 */
	private final AtomicBoolean ready = new AtomicBoolean(false);

	/**
	 * dataId+group 复合键到 Nacos Listener 的映射，便于幂等注册。
	 */
	private final Map<String, Listener> listenerMap = new ConcurrentHashMap<>(16);

	/**
	 * @param nacosConfigManager Nacos 配置管理器
	 */
	public NacosContextRefresher(NacosConfigManager nacosConfigManager) {
		this.configManager = nacosConfigManager;
		this.isRefreshEnabled = nacosConfigManager.getNacosConfigProperties()
				.isRefreshEnabled();
	}

	/**
	 * 应用就绪后注册 Nacos 监听器（CAS 保证只注册一次）。
	 * @param event 应用就绪事件
	 */
	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		// many Spring context
		if (this.ready.compareAndSet(false, true)) {
			this.registerNacosListenersForApplications();
		}
	}

	/**
	 * 注入应用上下文。
	 * @param applicationContext 应用上下文
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * 为所有可刷新的配置源注册 Nacos 监听器。
	 */
	private void registerNacosListenersForApplications() {
		if (isRefreshEnabled()) {
			for (NacosPropertySource propertySource : NacosPropertySourceRepository
					.getAll()) {
				if (!propertySource.isRefreshable()) {
					continue;
				}
				String dataId = propertySource.getDataId();
				registerNacosListener(propertySource.getGroup(), dataId);
			}
		}
	}

	/**
	 * 为指定 group/dataId 注册 Nacos 配置变更监听器：变更时发布 RefreshEvent 触发 Spring Cloud 刷新。
	 * @param groupKey 配置 group
	 * @param dataKey 配置 dataId
	 */
	private void registerNacosListener(final String groupKey, final String dataKey) {
		String key = NacosPropertySourceRepository.getMapKey(dataKey, groupKey);
		Listener listener = listenerMap.computeIfAbsent(key,
				lst -> new AbstractSharedListener() {
					/**
					 * 收到 Nacos 配置变更回调：直接发布 Spring Cloud 标准 RefreshEvent，
					 * 由 ContextRefresher 触发 ConfigData 重新 import 完成刷新。
					 * @param dataId 变更的 dataId
					 * @param group 变更的 group
					 * @param configInfo 最新配置原文
					 */
					@Override
					public void innerReceive(String dataId, String group,
							String configInfo) {
						log.info("[Nacos Config] Receive Nacos config change: dataId={}, group={}", dataKey,
								groupKey);
						// 远端变更直接发布 Spring Cloud 标准 RefreshEvent，
						// 由 ContextRefresher 触发 ConfigData 重新 import 完成刷新。
						applicationContext.publishEvent(
								new RefreshEvent(this, null, "Refresh Nacos config"));
					}
				});
		try {
			if (configService == null) {
				configService = configManager.getConfigService();
			}
			configService.addListener(dataKey, groupKey, listener);
			log.info("[Nacos Config] Listening config: dataId={}, group={}", dataKey,
					groupKey);
		}
		catch (NacosException e) {
			log.warn("register fail for nacos listener, dataId=[{}], group=[{}]", dataKey,
					groupKey, e);
		}
	}

	/**
	 * @return 是否启用配置刷新（构造期由 NacosConfigProperties 一次性确定）
	 */
	public boolean isRefreshEnabled() {
		return isRefreshEnabled;
	}

}
