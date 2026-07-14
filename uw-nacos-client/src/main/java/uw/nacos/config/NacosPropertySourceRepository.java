package uw.nacos.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nacos 配置源全局仓库。
 *
 * <p>以 {@code dataId,group} 复合键缓存所有已加载的 {@link NacosPropertySource}，供刷新监听器按
 * dataId/group 定位需要重新监听的配置项。
 *
 */
public final class NacosPropertySourceRepository {

	/**
	 * 配置源仓库：dataId+group 复合键到配置源的并发映射。
	 */
	private final static ConcurrentHashMap<String, NacosPropertySource> NACOS_PROPERTY_SOURCE_REPOSITORY = new ConcurrentHashMap<>();

	/**
	 * 工具类禁止实例化。
	 */
	private NacosPropertySourceRepository() {

	}

	/**
	 * @return 当前已加载的全部 Nacos 配置源
	 */
	public static List<NacosPropertySource> getAll() {
		return new ArrayList<>(NACOS_PROPERTY_SOURCE_REPOSITORY.values());
	}

	/**
	 * 以 dataId+group 复合键登记配置源（若已存在则不覆盖）。
	 * @param nacosPropertySource 待登记的配置源
	 */
	public static void collectNacosPropertySource(
			NacosPropertySource nacosPropertySource) {
		NACOS_PROPERTY_SOURCE_REPOSITORY
				.putIfAbsent(getMapKey(nacosPropertySource.getDataId(),
						nacosPropertySource.getGroup()), nacosPropertySource);
	}

	/**
	 * @param dataId 配置 dataId
	 * @param group 配置 group
	 * @return dataId 与 group 拼接出的仓库键
	 */
	public static String getMapKey(String dataId, String group) {
		return dataId + NacosConfigProperties.COMMAS + group;
	}

}
