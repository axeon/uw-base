package uw.nacos.discovery;

import java.util.Objects;
import java.util.Properties;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;

import static com.alibaba.nacos.api.NacosFactory.createNamingService;

/**
 * Nacos 服务管理器。
 *
 * <p>懒加载并持有 {@link NamingService}（用于服务注册、实例查询）实例，通过双重检查锁保证线程安全。
 * 同时缓存当前 {@link NacosDiscoveryProperties}，用于检测发现信息是否发生变更
 * （{@link #isNacosDiscoveryInfoChanged(NacosDiscoveryProperties)}）。
 */
public class NacosServiceManager {

	/** 缓存的 Nacos 服务发现配置属性，用于检测发现信息是否发生变更。 */
	private NacosDiscoveryProperties nacosDiscoveryProperties;

	/** 懒加载并持有的 NamingService 实例，volatile 配合双重检查锁保证可见性。 */
	private volatile NamingService namingService;

	/**
	 * 获取 NamingService，未创建时基于缓存配置懒加载。
	 *
	 * @return NamingService 实例
	 */
	public NamingService getNamingService() {
		if (Objects.isNull(this.namingService)) {
			buildNamingService(nacosDiscoveryProperties);
		}
		return namingService;
	}

	/**
	 * 判断缓存的发现配置与给定配置是否不一致（即是否发生变更）。
	 *
	 * @param currentNacosDiscoveryPropertiesCache 待比较的当前发现配置
	 * @return 不一致（已变更）返回 true，一致或尚未缓存返回 false
	 */
	public boolean isNacosDiscoveryInfoChanged(
			NacosDiscoveryProperties currentNacosDiscoveryPropertiesCache) {
		return !Objects.isNull(this.nacosDiscoveryProperties)
				&& !this.nacosDiscoveryProperties.equals(currentNacosDiscoveryPropertiesCache);
	}

	/**
	 * 双重检查锁创建并缓存 NamingService 实例。
	 *
	 * @param properties Nacos 连接属性
	 * @return NamingService 实例
	 */
	private NamingService buildNamingService(NacosDiscoveryProperties properties) {
		if (Objects.isNull(namingService)) {
			synchronized (NacosServiceManager.class) {
				if (Objects.isNull(namingService)) {
					// getNacosProperties() 放入锁内，避免并发首次访问时多线程各自构建 Properties + 全量扫描 Environment。
					namingService = createNewNamingService(properties.getNacosProperties());
				}
			}
		}
		return namingService;
	}

	/**
	 * 通过 {@code NacosFactory.createNamingService} 创建新的 NamingService 实例。
	 *
	 * @param properties Nacos 连接属性
	 * @return NamingService 实例
	 * @throws RuntimeException Nacos 创建异常包装
	 */
	private NamingService createNewNamingService(Properties properties) {
		try {
			return createNamingService(properties);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 关闭 NamingService 并置空，释放底层连接。
	 *
	 * @throws NacosException Nacos 关闭异常
	 */
	public void nacosServiceShutDown() throws NacosException {
		if (Objects.nonNull(this.namingService)) {
			this.namingService.shutDown();
			this.namingService = null;
		}
	}

	/**
	 * 设置 Nacos 服务发现配置属性，用于后续懒加载 NamingService 与变更检测。
	 *
	 * @param nacosDiscoveryProperties Nacos 服务发现配置属性
	 */
	public void setNacosDiscoveryProperties(NacosDiscoveryProperties nacosDiscoveryProperties) {
		this.nacosDiscoveryProperties = nacosDiscoveryProperties;
	}
}
