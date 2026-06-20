package uw.httpclient.json;

import uw.httpclient.http.*;
import uw.httpclient.util.MediaTypes;

/**
 * 基于 JSON 的 HTTP 接口帮助类。
 * <p>
 * 继承 {@link HttpInterface}，绑定 JSON 的 {@link DataObjectMapper}（{@link JsonObjectMapperImpl}）
 * 与 {@link MediaTypes#JSON_UTF8} 默认媒体类型。请求体序列化与响应体反序列化均按 JSON 处理。
 * <p>
 * 典型用法：
 * <pre>{@code
 * JsonInterfaceHelper helper = new JsonInterfaceHelper();
 * User u = helper.getForEntity("https://api.example.com/users/1", User.class).getValue();
 * }</pre>
 *
 * @since 2017/9/20
 */
public class JsonInterfaceHelper extends HttpInterface {

    /**
     * JSON 转换器单例，基于 Jackson 的 {@link JsonObjectMapperImpl}。
     */
    public static final DataObjectMapper JSON_CONVERTER = new JsonObjectMapperImpl();

    /**
     * 默认构造器，使用全局默认 OkHttpClient、默认日志类与级别。
     */
    public JsonInterfaceHelper() {
        super(null, null, null, null, JSON_CONVERTER, MediaTypes.JSON_UTF8);
    }

    /**
     * 使用指定 HttpConfig 构造。
     *
     * @param httpConfig HttpConfig 配置参数。
     */
    public JsonInterfaceHelper(HttpConfig httpConfig) {
        super(httpConfig, null, null, null, JSON_CONVERTER, MediaTypes.JSON_UTF8);
    }

    /**
     * 使用指定 HttpConfig 与自定义 HttpData 实现类构造。
     *
     * @param httpConfig  HttpConfig 配置参数。
     * @param httpDataCls 指定 HttpData 实现类，作为日志载体。
     */
    public JsonInterfaceHelper(HttpConfig httpConfig, Class<? extends HttpData> httpDataCls) {
        super(httpConfig, httpDataCls, null, null, JSON_CONVERTER, MediaTypes.JSON_UTF8);
    }

    /**
     * 使用指定 HttpConfig、HttpData 实现类与日志级别构造。
     *
     * @param httpConfig       HttpConfig 配置参数。
     * @param httpDataCls      指定 HttpData 实现类。
     * @param httpDataLogLevel HttpData 日志级别。
     */
    public JsonInterfaceHelper(HttpConfig httpConfig, Class<? extends HttpData> httpDataCls, HttpDataLogLevel httpDataLogLevel) {
        super(httpConfig, httpDataCls, httpDataLogLevel, null, JSON_CONVERTER, MediaTypes.JSON_UTF8);
    }

    /**
     * 完整构造器。
     *
     * @param httpConfig        HttpConfig 配置参数。
     * @param httpDataCls       指定 HttpData 实现类。
     * @param httpDataLogLevel  HttpData 日志级别。
     * @param httpDataProcessor HttpData 数据处理器（加解密/日志上报等）。
     */
    public JsonInterfaceHelper(HttpConfig httpConfig, Class<? extends HttpData> httpDataCls, HttpDataLogLevel httpDataLogLevel, HttpDataProcessor httpDataProcessor) {
        super(httpConfig, httpDataCls, httpDataLogLevel, httpDataProcessor, JSON_CONVERTER, MediaTypes.JSON_UTF8);
    }

}
