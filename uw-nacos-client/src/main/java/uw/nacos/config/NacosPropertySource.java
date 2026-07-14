package uw.nacos.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.CollectionUtils;

/**
 * Nacos 配置源，继承自 {@link MapPropertySource}。
 *
 * <p>在标准 Spring {@link PropertySource} 基础上附加 Nacos 语义信息（group、dataId、拉取时间戳、
 * 是否可动态刷新），是 ConfigData 加载与动态刷新流程中配置项的载体。
 *
 */
public class NacosPropertySource extends MapPropertySource {

	/**
	 * Nacos Group。
	 */
	private final String group;

	/**
	 * Nacos dataId。
	 */
	private final String dataId;

	/**
	 * 拉取该配置源的时间戳。
	 */
	private final Date timestamp;

	/**
	 * 该配置源是否支持动态刷新。
	 */
	private final boolean isRefreshable;

	/**
	 * 基于 group/dataId 与已组装好的 Map 构造配置源。
	 * @param group Nacos group
	 * @param dataId Nacos dataId
	 * @param source 底层属性 Map
	 * @param timestamp 拉取时间戳
	 * @param isRefreshable 是否可动态刷新
	 */
	NacosPropertySource(String group, String dataId, Map<String, Object> source,
			Date timestamp, boolean isRefreshable) {
		super(NacosPropertySourceRepository.getMapKey(dataId, group), source);
		this.group = group;
		this.dataId = dataId;
		this.timestamp = timestamp;
		this.isRefreshable = isRefreshable;
	}

	/**
	 * @param propertySources 底层属性源列表
	 * @param group Nacos group
	 * @param dataId Nacos dataId
	 * @param timestamp 拉取时间戳
	 * @param isRefreshable 是否可动态刷新
	 */
	public NacosPropertySource(List<PropertySource<?>> propertySources, String group,
			String dataId, Date timestamp, boolean isRefreshable) {
		this(group, dataId, getSourceMap(group, dataId, propertySources), timestamp,
				isRefreshable);
	}

	/**
	 * 将一组 PropertySource 合并为扁平 Map；多文档（如 yaml "---" 分段）会合并，
	 * 非 {@link MapPropertySource} 的源以 {@code dataId,group} 为 key 整体保留。
	 * @param group Nacos group
	 * @param dataId Nacos dataId
	 * @param propertySources 待合并的属性源列表
	 * @return 扁平化后的 Map
	 */
	private static Map<String, Object> getSourceMap(String group, String dataId,
			List<PropertySource<?>> propertySources) {
		if (CollectionUtils.isEmpty(propertySources)) {
			return Collections.emptyMap();
		}
		// If only one, return the internal element, otherwise wrap it.
		if (propertySources.size() == 1) {
			PropertySource propertySource = propertySources.get(0);
			if (propertySource != null && propertySource.getSource() instanceof Map source) {
				return source;
			}
		}

		Map<String, Object> sourceMap = new LinkedHashMap<>();
		List<PropertySource<?>> otherTypePropertySources = new ArrayList<>();
		for (PropertySource<?> propertySource : propertySources) {
			if (propertySource == null) {
				continue;
			}
			if (propertySource instanceof MapPropertySource mapPropertySource) {
				// If the Nacos configuration file uses "---" to separate property name,
				// propertySources will be multiple documents, and every document is a
				// map.
				// see org.springframework.boot.env.YamlPropertySourceLoader#load
				Map<String, Object> source = mapPropertySource.getSource();
				sourceMap.putAll(source);
			}
			else {
				otherTypePropertySources.add(propertySource);
			}
		}

		// Other property sources which is not instanceof MapPropertySource will be put as
		// it is,
		// and the internal elements cannot be directly retrieved,
		// so the user needs to implement the retrieval logic by himself
		if (!otherTypePropertySources.isEmpty()) {
			sourceMap.put(NacosPropertySourceRepository.getMapKey(dataId, group),
					otherTypePropertySources);
		}
		return sourceMap;
	}

	/**
	 * @return Nacos group
	 */
	public String getGroup() {
		return this.group;
	}

	/**
	 * @return Nacos dataId
	 */
	public String getDataId() {
		return dataId;
	}

	/**
	 * @return 拉取时间戳
	 */
	public Date getTimestamp() {
		return timestamp;
	}

	/**
	 * @return 是否可动态刷新
	 */
	public boolean isRefreshable() {
		return isRefreshable;
	}

}
