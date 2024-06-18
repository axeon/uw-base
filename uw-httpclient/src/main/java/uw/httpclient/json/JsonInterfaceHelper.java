package uw.httpclient.json;

import uw.httpclient.http.HttpDataLogLevel;
import uw.httpclient.util.MediaTypes;
import uw.httpclient.http.*;

/**
 * Json接口帮助类。
 *
 * @since 2017/9/20
 */
public class JsonInterfaceHelper extends HttpInterface {

    /**
     * json转换器。
     */
    public static final ObjectMapper JSON_CONVERTER = new JsonObjectMapperImpl();


    /**
     * 默认构造器。
     */
    public JsonInterfaceHelper() {
        super( null, null, null, null, JSON_CONVERTER, MediaTypes.JSON_UTF8 );
    }

    /**
     * 使用httpConfig构造。
     * @param httpConfig HttpConfig配置参数。
     */
    public JsonInterfaceHelper(HttpConfig httpConfig) {
        super( httpConfig, null, null, null, JSON_CONVERTER, MediaTypes.JSON_UTF8 );
    }

    /**
     * 使用httpConfig和HttpDataClass进行构造。
     * @param httpConfig HttpConfig配置参数。
     * @param httpDataCls 指定HttpData实现类。
     */
    public JsonInterfaceHelper(HttpConfig httpConfig, Class<? extends HttpData> httpDataCls) {
        super( httpConfig, httpDataCls, null, null, JSON_CONVERTER, MediaTypes.JSON_UTF8 );
    }

    /**
     * 使用httpConfig和HttpDataClass、HttpDataLogLevel进行构造。
     * @param httpConfig HttpConfig配置参数。
     * @param httpDataCls 指定HttpData实现类。
     * @param httpDataLogLevel
     */
    public JsonInterfaceHelper(HttpConfig httpConfig, Class<? extends HttpData> httpDataCls, HttpDataLogLevel httpDataLogLevel) {
        super( httpConfig, httpDataCls, httpDataLogLevel, null, JSON_CONVERTER, MediaTypes.JSON_UTF8 );
    }

    /**
     * 完整构造器。
     * @param httpConfig HttpConfig配置参数。
     * @param httpDataCls 指定HttpData实现类。
     * @param httpDataLogLevel HttpDataLog级别。
     * @param httpDataProcessor HttpData数据处理器。
     */
    public JsonInterfaceHelper(HttpConfig httpConfig, Class<? extends HttpData> httpDataCls, HttpDataLogLevel httpDataLogLevel, HttpDataProcessor httpDataProcessor) {
        super( httpConfig, httpDataCls, httpDataLogLevel, httpDataProcessor, JSON_CONVERTER, MediaTypes.JSON_UTF8 );
    }

}
