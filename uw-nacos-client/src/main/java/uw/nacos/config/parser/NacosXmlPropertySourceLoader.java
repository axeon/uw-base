package uw.nacos.config.parser;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * Nacos XML 配置属性源加载器。
 *
 * <p>覆盖默认的 {@link PropertiesPropertySourceLoader} 对 XML 的处理：后者对 {@code DOCTYPE} 做严格
 * 校验，难以自定义配置内容。本加载器将 XML 解析为扁平 Map 后包装为属性源；在
 * {@link NacosDataParserHandler} 的显式加载器列表中排在 Boot 自带 loader 之前，确保对 {@code .xml} 生效。
 *
 */
public class NacosXmlPropertySourceLoader extends AbstractPropertySourceLoader {

	/** 复用的 DocumentBuilderFactory（线程安全）；DocumentBuilder 本身非线程安全，每次新建。 */
	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory
			.newInstance();

	/**
	 * @return 支持的文件扩展名（不含点号）
	 */
	@Override
	public String[] getFileExtensions() {
		return new String[] { "xml" };
	}

	/**
	 * 加载 XML 资源为属性源。
	 * @param name 属性源根名
	 * @param resource 待加载资源
	 * @return 属性源列表
	 * @throws IOException 加载失败时抛出
	 */
	@Override
	protected List<PropertySource<?>> doLoad(String name, Resource resource)
			throws IOException {
		Map<String, Object> nacosDataMap = parseXml2Map(resource);
		return Collections.singletonList(
				new OriginTrackedMapPropertySource(name, nacosDataMap, true));

	}

	/**
	 * 将 XML 资源解析为扁平 Map。
	 * @param resource XML 资源
	 * @return 扁平 Map
	 * @throws IOException 解析失败时抛出
	 */
	private Map<String, Object> parseXml2Map(Resource resource) throws IOException {
		Map<String, Object> map = new LinkedHashMap<>(32);
		try {
			DocumentBuilder documentBuilder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
			Document document = documentBuilder.parse(resource.getInputStream());
			if (null == document) {
				return null;
			}
			parseNodeList(document.getChildNodes(), map, "");
		}
		catch (Exception e) {
			throw new IOException("The xml content parse error.", e.getCause());
		}
		return map;
	}

	/**
	 * 递归遍历节点列表，按 element/属性/文本填充 Map：element 递归子节点、属性追加到 parentKey、文本挂 parentKey。
	 * @param nodeList 节点列表
	 * @param map 结果 Map
	 * @param parentKey 父级键名
	 */
	private void parseNodeList(NodeList nodeList, Map<String, Object> map,
			String parentKey) {
		if (nodeList == null || nodeList.getLength() < 1) {
			return;
		}
		parentKey = parentKey == null ? "" : parentKey;
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			String value = node.getNodeValue();
			value = value == null ? "" : value.trim();
			String name = node.getNodeName();
			name = name == null ? "" : name.trim();

			if (!StringUtils.hasLength(name)) {
				continue;
			}

			String key = !StringUtils.hasLength(parentKey) ? name : parentKey + AbstractPropertySourceLoader.DOT + name;
			NamedNodeMap nodeMap = node.getAttributes();
			parseNodeAttr(nodeMap, map, key);
			if (node.getNodeType() == Node.ELEMENT_NODE && node.hasChildNodes()) {
				parseNodeList(node.getChildNodes(), map, key);
				continue;
			}
			if (value.length() < 1) {
				continue;
			}
			map.put(parentKey, value);
		}
	}

	/**
	 * 把节点属性展开为 {@code parentKey.attrName} 形式写入 Map。
	 * @param nodeMap 属性映射
	 * @param map 结果 Map
	 * @param parentKey 父级键名
	 */
	private void parseNodeAttr(NamedNodeMap nodeMap, Map<String, Object> map,
			String parentKey) {
		if (null == nodeMap || nodeMap.getLength() < 1) {
			return;
		}
		for (int i = 0; i < nodeMap.getLength(); i++) {
			Node node = nodeMap.item(i);
			if (null == node) {
				continue;
			}
			if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
				if (!StringUtils.hasLength(node.getNodeName())) {
					continue;
				}
				if (!StringUtils.hasLength(node.getNodeValue())) {
					continue;
				}
				map.put(String.join(AbstractPropertySourceLoader.DOT, parentKey, node.getNodeName()),
						node.getNodeValue());
			}
		}
	}

}
