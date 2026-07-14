package uw.nacos.config.parser;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

/**
 * Nacos JSON 配置属性源加载器。
 *
 * <p>继承 {@link AbstractPropertySourceLoader}，处理扩展名为 {@code json} 的 Nacos 配置内容，
 * 支持注释（ALLOW_JAVA_COMMENTS），并扁平化嵌套结构；对以 {@code .value} 结尾的键做简化处理。
 *
 */
public class NacosJsonPropertySourceLoader extends AbstractPropertySourceLoader {

	/**
	 * value 键常量。
	 */
	private static final String VALUE = "value";

	/** 复用的 JsonMapper（线程安全），开启注释支持。 */
	private static final JsonMapper MAPPER = JsonMapper.builder()
			.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
			.build();

	/**
	 * @return 支持的文件扩展名（不含点号）
	 */
	@Override
	public String[] getFileExtensions() {
		return new String[] { "json" };
	}

	/**
	 * 加载 JSON 资源为属性源。
	 * @param name 属性源根名
	 * @param resource 待加载资源
	 * @return 属性源列表
	 * @throws IOException 加载失败时抛出
	 */
	@Override
	protected List<PropertySource<?>> doLoad(String name, Resource resource)
			throws IOException {
		Map<String, Object> result = new LinkedHashMap<>(32);
		Map<String, Object> nacosDataMap = MAPPER.readValue(resource.getInputStream(),
				LinkedHashMap.class);
		flattenedMap(result, nacosDataMap, null);
		return Collections.singletonList(
				new OriginTrackedMapPropertySource(name, this.reloadMap(result), true));

	}

	/**
	 * 对以 {@code .value} 结尾的键做简化：把 {@code a.b.value} 的值同时挂到 {@code a.b}。
	 * @param map 扁平化后的键值
	 * @return 处理后的键值
	 */
	protected Map<String, Object> reloadMap(Map<String, Object> map) {
		if (map == null || map.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, Object> result = new LinkedHashMap<>(map);
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String key = entry.getKey();
			if (key.contains(AbstractPropertySourceLoader.DOT)) {
				int idx = key.lastIndexOf(AbstractPropertySourceLoader.DOT);
				String suffix = key.substring(idx + 1);
				if (VALUE.equalsIgnoreCase(suffix)) {
					result.put(key.substring(0, idx), entry.getValue());
				}
			}
		}
		return result;
	}

}
