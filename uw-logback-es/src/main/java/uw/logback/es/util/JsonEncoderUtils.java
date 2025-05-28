package uw.logback.es.util;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import org.apache.commons.lang3.time.FastDateFormat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 编码工具类
 */
public class JsonEncoderUtils {

    /**
     * 日志编码
     */
    public static final Charset LOG_CHARSET = StandardCharsets.UTF_8;
    /**
     * 时间格式化器
     */
    public static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
    /**
     * 换行符
     */
    private static final String LINE_SEPARATOR = System.lineSeparator();
    /**
     * 换行符字节
     */
    public static final byte[] LINE_SEPARATOR_BYTES = LINE_SEPARATOR.getBytes(JsonEncoderUtils.LOG_CHARSET);

    /**
     * 转义引号。
     *
     * @param input
     * @return
     */
    public static String escapeJSON(String input) {
        if (input == null) {
            return "null";
        } else {
            return new String(JsonStringEncoder.getInstance().quoteAsString(input));
        }
    }

}
