package uw.httpclient.xml;

import uw.httpclient.http.*;
import uw.httpclient.util.MediaTypes;

/**
 * 基于 XML 的 HTTP 接口帮助类。
 * <p>
 * 继承 {@link HttpInterface}，绑定 XML 的 {@link DataObjectMapper}（{@link XmlObjectMapperImpl}）
 * 与 {@link MediaTypes#XML_UTF8} 默认媒体类型。请求体序列化与响应体反序列化均按 XML 处理。
 * <p>
 * 典型用法：
 * <pre>{@code
 * XmlInterfaceHelper helper = new XmlInterfaceHelper();
 * ResponseVo v = helper.getForEntity("https://api.example.com/data", ResponseVo.class).getValue();
 * }</pre>
 *
 * @since 2017/9/20
 */
public class XmlInterfaceHelper extends HttpInterface {

    /**
     * XML 转换器单例，基于 Jackson XML 的 {@link XmlObjectMapperImpl}。
     */
    public static final DataObjectMapper XML_CONVERTER = new XmlObjectMapperImpl();

    /**
     * 默认构造器，使用全局默认 OkHttpClient、默认日志类与级别。
     */
    public XmlInterfaceHelper() {
        super(null, null, null, null, XML_CONVERTER, MediaTypes.XML_UTF8);
    }

    /**
     * 使用指定 HttpConfig 构造。
     *
     * @param httpConfig HttpConfig 配置参数。
     */
    public XmlInterfaceHelper(HttpConfig httpConfig) {
        super(httpConfig, null, null, null, XML_CONVERTER, MediaTypes.XML_UTF8);
    }

    /**
     * 使用指定 HttpConfig 与自定义 HttpData 实现类构造。
     *
     * @param httpConfig  HttpConfig 配置参数。
     * @param httpDataCls 指定 HttpData 实现类，作为日志载体。
     */
    public XmlInterfaceHelper(HttpConfig httpConfig, Class<? extends HttpData> httpDataCls) {
        super(httpConfig, httpDataCls, null, null, XML_CONVERTER, MediaTypes.XML_UTF8);
    }

    /**
     * 使用指定 HttpConfig、HttpData 实现类与日志级别构造。
     *
     * @param httpConfig       HttpConfig 配置参数。
     * @param httpDataCls      指定 HttpData 实现类。
     * @param httpDataLogLevel HttpData 日志级别。
     */
    public XmlInterfaceHelper(HttpConfig httpConfig, Class<? extends HttpData> httpDataCls, HttpDataLogLevel httpDataLogLevel) {
        super(httpConfig, httpDataCls, httpDataLogLevel, null, XML_CONVERTER, MediaTypes.XML_UTF8);
    }

    /**
     * 完整构造器。
     *
     * @param httpConfig        HttpConfig 配置参数。
     * @param httpDataCls       指定 HttpData 实现类。
     * @param httpDataLogLevel  HttpData 日志级别。
     * @param httpDataProcessor HttpData 数据处理器（加解密/日志上报等）。
     */
    public XmlInterfaceHelper(HttpConfig httpConfig, Class<? extends HttpData> httpDataCls, HttpDataLogLevel httpDataLogLevel, HttpDataProcessor httpDataProcessor) {
        super(httpConfig, httpDataCls, httpDataLogLevel, httpDataProcessor, XML_CONVERTER, MediaTypes.XML_UTF8);
    }

}
