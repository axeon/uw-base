package uw.nacos.config.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import uw.nacos.config.utils.NacosConfigUtils;

import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import static uw.nacos.config.parser.AbstractPropertySourceLoader.DOT;

/**
 * Nacos 配置内容解析处理器（单例）。
 *
 * <p>根据配置内容扩展名，从一组显式有序的 {@link PropertySourceLoader} 中按扩展名匹配加载器，
 * 把远端字符串配置解析为 Spring {@link PropertySource}。加载器顺序为：Nacos 自有 loader 优先
 * （{@link NacosJsonPropertySourceLoader}、{@link NacosXmlPropertySourceLoader}），其后是 Spring Boot
 * 自带的 {@link PropertiesPropertySourceLoader} 与 {@link YamlPropertySourceLoader}——前者也声明
 * {@code xml}，靠列表顺序保证 Nacos XML loader 对 {@code .xml} 生效。对 {@code properties} 扩展名
 * 的内容会先把中文转成 Unicode 转义以避免 ISO-8859-1 乱码。
 *
 */
public final class NacosDataParserHandler {

	/**
	 * 默认扩展名。
	 */
	private static final String DEFAULT_EXTENSION = "properties";

	/**
	 * properties 扩展名常量。
	 */
	private static final String PROPERTIES_EXTENSION = "properties";

	/**
	 * 扩展名到加载器的映射（按显式有序 loader 列表 putIfAbsent，Nacos 自有 loader 优先）。
	 */
	private final Map<String, PropertySourceLoader> propertySourceLoaderMap;

	/**
	 * 私有构造：组装扩展名到 loader 的映射，Nacos 自有 loader 优先于 Spring Boot 自带 loader。
	 */
	private NacosDataParserHandler() {
		// 显式有序加载器列表：Nacos 自有 loader 在前，确保 NacosXml 对 .xml 生效
		// （Boot 的 PropertiesPropertySourceLoader 也声明 xml，靠先到先得让 NacosXml 赢）。
		List<PropertySourceLoader> loaders = Arrays.asList(
				new NacosJsonPropertySourceLoader(),
				new NacosXmlPropertySourceLoader(),
				new PropertiesPropertySourceLoader(),
				new YamlPropertySourceLoader());
		Map<String, PropertySourceLoader> map = new HashMap<>();
		for (PropertySourceLoader loader : loaders) {
			for (String ext : loader.getFileExtensions()) {
				map.putIfAbsent(ext, loader);
			}
		}
		this.propertySourceLoaderMap = map;
	}

	/**
	 * 解析 Nacos 配置内容。
	 * @param configName 配置名（含扩展名时可推断类型）
	 * @param configValue 配置内容
	 * @param extension 扩展名（为空时从 configName 推断）
	 * @return 解析得到的属性源列表
	 * @throws IOException 解析失败时抛出
	 */
	public List<PropertySource<?>> parseNacosData(String configName, String configValue,
			String extension) throws IOException {
		if (!StringUtils.hasLength(configValue)) {
			return Collections.emptyList();
		}
		if (!StringUtils.hasLength(extension)) {
			extension = this.getFileExtension(configName);
		}
		PropertySourceLoader propertySourceLoader = propertySourceLoaderMap
				.get(extension.toLowerCase(Locale.ROOT));
		if (propertySourceLoader == null) {
			return Collections.emptyList();
		}
		// properties 走 PropertiesPropertySourceLoader（ISO-8859-1），中文需先转 Unicode 转义避免乱码。
		byte[] bytes = PROPERTIES_EXTENSION.equalsIgnoreCase(extension)
				? NacosConfigUtils.selectiveConvertUnicode(configValue).getBytes()
				: configValue.getBytes();
		NacosByteArrayResource nacosByteArrayResource = new NacosByteArrayResource(bytes,
				configName);
		nacosByteArrayResource.setFilename(getFileName(configName, extension));
		List<PropertySource<?>> propertySourceList = propertySourceLoader.load(configName,
				nacosByteArrayResource);
		if (CollectionUtils.isEmpty(propertySourceList)) {
			return Collections.emptyList();
		}
		List<PropertySource<?>> result = new ArrayList<>(propertySourceList.size());
		for (PropertySource<?> propertySource : propertySourceList) {
			if (propertySource == null) {
				continue;
			}
			// loader 列表产物已是 OriginTrackedMapPropertySource；仅对未追踪 origin 的可枚举源做转换，避免剥离 origin。
			if (!(propertySource instanceof OriginTrackedMapPropertySource)
					&& propertySource instanceof EnumerablePropertySource enumerablePropertySource) {
				String[] propertyNames = enumerablePropertySource.getPropertyNames();
				if (propertyNames != null && propertyNames.length > 0) {
					Map<String, Object> map = new LinkedHashMap<>();
					for (String name : propertyNames) {
						map.put(name, propertySource.getProperty(name));
					}
					result.add(new OriginTrackedMapPropertySource(propertySource.getName(),
							map, true));
					continue;
				}
			}
			result.add(propertySource);
		}
		return result;
	}

	/**
	 * @param name 文件名
	 * @return 文件扩展名，无法获取时返回 {@link #DEFAULT_EXTENSION}
	 */
	private String getFileExtension(String name) {
		String ext = StringUtils.getFilenameExtension(name);
		return StringUtils.hasLength(ext) ? ext : DEFAULT_EXTENSION;
	}

	/**
	 * 组装真实文件名：name 已含正确扩展名时原样返回，否则拼接 extension。
	 * @param name 原始名
	 * @param extension 扩展名
	 * @return 真实文件名
	 */
	private String getFileName(String name, String extension) {
		if (!StringUtils.hasLength(extension)) {
			return name;
		}
		if (!StringUtils.hasLength(name)) {
			return extension;
		}
		int idx = name.lastIndexOf(DOT);
		if (idx > 0 && idx < name.length() - 1) {
			String ext = name.substring(idx + 1);
			if (extension.equalsIgnoreCase(ext)) {
				return name;
			}
		}
		return name + DOT + extension;
	}

	/**
	 * @return 单例实例
	 */
	public static NacosDataParserHandler getInstance() {
		return ParserHandler.HANDLER;
	}

	/**
	 * 单例持有者（懒加载）。
	 */
	private static final class ParserHandler {

		/**
		 * 单例实例。
		 */
		private static final NacosDataParserHandler HANDLER = new NacosDataParserHandler();

	}

}
