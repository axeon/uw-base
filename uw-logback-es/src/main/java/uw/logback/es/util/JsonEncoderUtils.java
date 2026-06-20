package uw.logback.es.util;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import org.apache.commons.lang3.time.FastDateFormat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 日志 NDJSON 编码工具。
 * <p>
 * 提供 JSON 字符串转义、统一换行符、时间格式化等 appender 编码所需的公共常量与方法。
 */
public class JsonEncoderUtils {

    /**
     * 日志编码字符集，固定 UTF-8。
     */
    public static final Charset LOG_CHARSET = StandardCharsets.UTF_8;

    /**
     * ES {@code @timestamp} 字段使用的时间格式（ISO8601 带毫秒与时区）。
     */
    public static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

    /**
     * 平台换行符，用于 NDJSON 行分隔。
     */
    private static final String LINE_SEPARATOR = System.lineSeparator();

    /**
     * NDJSON 行分隔字节。
     */
    public static final byte[] LINE_SEPARATOR_BYTES = LINE_SEPARATOR.getBytes(JsonEncoderUtils.LOG_CHARSET);

    /**
     * 对字符串做 JSON 字符串内容转义（不含外层引号）。
     * <p>
     * 与手写的 {@code "..."} 外层引号配合使用：本方法只转义内容中的引号、反斜杠与控制字符。
     *
     * @param input 原始字符串，为 null 时返回字面 "null"
     * @return 转义后的字符串内容
     */
    public static String escapeJSON(String input) {
        if (input == null) {
            return "null";
        } else {
            return new String(JsonStringEncoder.getInstance().quoteAsString(input));
        }
    }

}
