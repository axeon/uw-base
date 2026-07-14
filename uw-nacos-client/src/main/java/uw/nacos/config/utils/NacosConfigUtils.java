package uw.nacos.config.utils;

/**
 * Nacos 配置工具类。
 *
 * <p>提供配置内容字符编码转换辅助方法，主要解决 {@code PropertiesPropertySourceLoader} 使用
 * ISO-8859-1 导致中文乱码的问题：将非基础拉丁字符转成反斜杠-u 加四位十六进制的 Unicode 转义形式。
 *
 */
public final class NacosConfigUtils {

	/** Unicode 转义用十六进制字符表。 */
	private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

	/**
	 * 工具类禁止实例化。
	 */
	private NacosConfigUtils() {
	}

	/**
	 * 将配置内容中的非基础拉丁字符转换为反斜杠-u 加四位十六进制的 Unicode 转义形式。
	 * @param configValue 原始配置内容
	 * @return 转换后的字符串
	 */
	public static String selectiveConvertUnicode(String configValue) {
		StringBuilder sb = new StringBuilder(configValue.length());
		for (int i = 0; i < configValue.length(); i++) {
			char aChar = configValue.charAt(i);
			if (isBaseLetter(aChar)) {
				sb.append(aChar);
			}
			else {
				sb.append("\\u");
				sb.append(HEX_CHARS[(aChar >> 12) & 0xF]);
				sb.append(HEX_CHARS[(aChar >> 8) & 0xF]);
				sb.append(HEX_CHARS[(aChar >> 4) & 0xF]);
				sb.append(HEX_CHARS[aChar & 0xF]);
			}
		}
		return sb.toString();
	}

	/**
	 * @param ch 字符
	 * @return 是否为基本拉丁字符或空白字符
	 */
	private static boolean isBaseLetter(char ch) {
		Character.UnicodeBlock ub = Character.UnicodeBlock.of(ch);
		return ub == Character.UnicodeBlock.BASIC_LATIN
				|| (ch <= 0x7F && Character.isWhitespace(ch));
	}

}
