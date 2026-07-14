package uw.nacos.config.parser;

import org.springframework.core.io.ByteArrayResource;

/**
 * Nacos 专有字节数组资源，继承 Spring 的 {@link ByteArrayResource}。
 *
 * <p>重写 {@link #getFilename()} 使其携带真实文件名（含扩展名），供
 * {@link org.springframework.boot.env.PropertySourceLoader} 按扩展名选择解析器；用作
 * {@link AbstractPropertySourceLoader#canLoad(String, org.springframework.core.io.Resource)}
 * 的类型标识，将 Nacos 配置与本地文件加载器隔离开。
 *
 */
public class NacosByteArrayResource extends ByteArrayResource {

	/**
	 * 真实文件名（含扩展名）。
	 */
	private String filename;

	/**
	 * @param byteArray 待包装的字节数组
	 * @param description 字节数组来源描述
	 */
	public NacosByteArrayResource(byte[] byteArray, String description) {
		super(byteArray, description);
	}

	/**
	 * @param filename 真实文件名（含扩展名）
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * 返回真实文件名；未设置时回退到资源描述。
	 * @return 文件名
	 */
	@Override
	public String getFilename() {
		return null == this.filename ? this.getDescription() : this.filename;
	}

}
