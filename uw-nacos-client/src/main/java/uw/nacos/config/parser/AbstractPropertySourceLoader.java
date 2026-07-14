package uw.nacos.config.parser;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * Nacos 专用的属性源加载器基类，实现 Spring Boot 的 {@link PropertySourceLoader}。
 *
 * <p>如需支持新的配置解析方式：1) 继承本类；2) 在 {@code spring.factories} 中注册
 * {@code org.springframework.boot.env.PropertySourceLoader=..}；3) 校验生效。注意配合
 * {@link NacosByteArrayResource} 使用，通过 {@link #canLoad(String, Resource)} 限定只处理
 * Nacos 资源，避免干扰其它加载器。
 *
 */
public abstract class AbstractPropertySourceLoader implements PropertySourceLoader {

	/**
	 * 符号：点号。
	 */
	static final String DOT = ".";

	/**
	 * 限定只加载 Nacos 专有资源（{@link NacosByteArrayResource}），防止干扰其它加载器，除非 reload 改变该行为。
	 * @param name 属性源根名
	 * @param resource 待加载资源
	 * @return 是否可加载该资源
	 */
	private boolean canLoad(String name, Resource resource) {
		return resource instanceof NacosByteArrayResource;
	}

	/**
	 * 加载资源为一个或多个属性源。单文档返回单源，多文档（如 yaml）每个文档一个源；非 Nacos 资源直接返回空列表。
	 * @param name 属性源根名
	 * @param resource 待加载资源
	 * @return 属性源列表
	 * @throws IOException 加载失败时抛出
	 */
	@Override
	public List<PropertySource<?>> load(String name, Resource resource)
			throws IOException {
		if (!canLoad(name, resource)) {
			return Collections.emptyList();
		}
		return this.doLoad(name, resource);
	}

	/**
	 * 子类实现的实际加载逻辑。
	 * @param name 属性源根名
	 * @param resource 待加载资源
	 * @return 属性源列表
	 * @throws IOException 加载失败时抛出
	 */
	protected abstract List<PropertySource<?>> doLoad(String name, Resource resource)
			throws IOException;

	/**
	 * 将嵌套 Map 扁平化为点号键名，便于 Spring 属性绑定。
	 * @param result 扁平化结果
	 * @param dataMap 待扁平化的 Map
	 * @param parentKey 父级键名
	 */
	protected void flattenedMap(Map<String, Object> result, Map<String, Object> dataMap,
			String parentKey) {
		if (dataMap == null || dataMap.isEmpty()) {
			return;
		}
		Set<Entry<String, Object>> entries = dataMap.entrySet();
		for (Iterator<Entry<String, Object>> iterator = entries.iterator(); iterator
				.hasNext();) {
			Map.Entry<String, Object> entry = iterator.next();
			String key = entry.getKey();
			Object value = entry.getValue();

			String fullKey = !StringUtils.hasLength(parentKey) ? key : key.startsWith("[")
					? parentKey.concat(key) : parentKey.concat(DOT).concat(key);

			if (value instanceof Map map) {
				flattenedMap(result, map, fullKey);
				continue;
			}
			else if (value instanceof Collection collection) {
				int count = 0;
				for (Object object : collection) {
					flattenedMap(result,
							Collections.singletonMap("[" + (count++) + "]", object),
							fullKey);
				}
				continue;
			}

			result.put(fullKey, value);
		}
	}

}
