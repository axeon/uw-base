package uw.nacos.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nacos 配置中心客户端管理器（单例）。
 *
 * <p>持有全局唯一的 {@link ConfigService} 与 {@link NacosConfigProperties}，供 ConfigData 加载器、
 * 刷新监听器等复用。{@code ConfigService} 懒创建：首次调用 {@link #getConfigService()} 时基于
 * {@link NacosConfigProperties#assembleConfigServiceProperties()} 构造，双重检查锁 + volatile
 * 保证并发首次访问只创建一个实例。
 *
 */
public class NacosConfigManager {

	/**
	 * 日志器。
	 */
	private static final Logger log = LoggerFactory.getLogger(NacosConfigManager.class);

	/**
	 * 全局唯一的 ConfigService，volatile + 双重检查锁保证安全发布。
	 */
	private static volatile ConfigService service;

	/**
	 * 管理器单例。
	 */
	private static volatile NacosConfigManager INSTANCE;

	/**
	 * 当前管理器持有的 Nacos 配置属性。
	 */
	private NacosConfigProperties nacosConfigProperties;

	/**
	 * @param nacosConfigProperties Nacos 配置属性
	 */
	public NacosConfigManager(NacosConfigProperties nacosConfigProperties) {
		this.nacosConfigProperties = nacosConfigProperties;
	}

	/**
	 * @return 已创建的单例（可能为 null）
	 */
	public static NacosConfigManager getInstance() {
		return INSTANCE;
	}

	/**
	 * 双重检查锁获取/创建单例，并按需初始化 ConfigService。
	 * @param properties Nacos 配置属性
	 * @return 单例实例
	 */
	public static NacosConfigManager getInstance(NacosConfigProperties properties) {
		if (INSTANCE != null) {
			return INSTANCE;
		}
		synchronized (NacosConfigManager.class) {
			if (INSTANCE == null) {
				INSTANCE = new NacosConfigManager(properties);
				INSTANCE.createConfigService(properties);
			}
		}
		return INSTANCE;
	}

	/**
	 * 创建 ConfigService。调用方需持 {@code NacosConfigManager.class} 锁。
	 */
	private ConfigService createConfigService(
			NacosConfigProperties nacosConfigProperties) {
		try {
			if (service == null) {
				service = NacosFactory.createConfigService(
						nacosConfigProperties.assembleConfigServiceProperties());
			}
		}
		catch (NacosException e) {
			log.error(e.getMessage());
			throw new NacosConnectionFailureException(
					nacosConfigProperties.getServerAddr(), e.getMessage(), e);
		}
		return service;
	}

	/**
	 * @return 全局 ConfigService，不存在则加锁懒创建
	 */
	public ConfigService getConfigService() {
		if (service == null) {
			synchronized (NacosConfigManager.class) {
				if (service == null) {
					createConfigService(this.nacosConfigProperties);
				}
			}
		}
		return service;
	}

	/**
	 * @return 当前管理器持有的 Nacos 配置属性
	 */
	public NacosConfigProperties getNacosConfigProperties() {
		return nacosConfigProperties;
	}

}
