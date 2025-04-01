package uw.httpclient.xml;

import uw.httpclient.http.HttpDataLogLevel;
import uw.httpclient.util.MediaTypes;
import uw.httpclient.http.*;

/**
 * Xml接口帮助类。
 *
 * @since 2017/9/20
 */
public class XmlInterfaceHelper extends HttpInterface {

    /**
     * xml转换器。
     */
    public static final DataObjectMapper XML_CONVERTER = new XmlObjectMapperImpl();

    /**
     * 默认构造器。
     */
    public XmlInterfaceHelper() {
        super( null, null, null, null, XML_CONVERTER, MediaTypes.XML_UTF8 );
    }

    /**
     * 使用httpConfig构造。
     * @param httpConfig HttpConfig配置参数。
     */
    public XmlInterfaceHelper(HttpConfig httpConfig) {
        super( httpConfig, null, null, null, XML_CONVERTER, MediaTypes.XML_UTF8 );
    }

    /**
     * 使用httpConfig和HttpDataClass进行构造。
     * @param httpConfig HttpConfig配置参数。
     * @param httpDataCls 指定HttpData实现类。
     */
    public XmlInterfaceHelper(HttpConfig httpConfig, Class<? extends HttpData> httpDataCls) {
        super( httpConfig, httpDataCls, null, null, XML_CONVERTER, MediaTypes.XML_UTF8 );
    }

    /**
     * 使用httpConfig和HttpDataClass、HttpDataLogLevel进行构造。
     * @param httpConfig HttpConfig配置参数。
     * @param httpDataCls 指定HttpData实现类。
     * @param httpDataLogLevel
     */
    public XmlInterfaceHelper(HttpConfig httpConfig, Class<? extends HttpData> httpDataCls, HttpDataLogLevel httpDataLogLevel) {
        super( httpConfig, httpDataCls, httpDataLogLevel, null, XML_CONVERTER, MediaTypes.XML_UTF8 );
    }
    /**
     * 完整构造器。
     * @param httpConfig HttpConfig配置参数。
     * @param httpDataCls 指定HttpData实现类。
     * @param httpDataLogLevel HttpDataLog级别。
     * @param httpDataProcessor HttpData数据处理器。
     */
    public XmlInterfaceHelper(HttpConfig httpConfig, Class<? extends HttpData> httpDataCls, HttpDataLogLevel httpDataLogLevel, HttpDataProcessor httpDataProcessor) {
        super( httpConfig, httpDataCls, httpDataLogLevel, httpDataProcessor, XML_CONVERTER, MediaTypes.XML_UTF8 );
    }

}
