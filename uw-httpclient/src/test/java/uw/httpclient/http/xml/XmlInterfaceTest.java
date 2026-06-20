package uw.httpclient.http.xml;

import org.junit.Test;
import uw.httpclient.http.HttpInterface;
import uw.httpclient.util.MediaTypes;
import uw.httpclient.xml.XmlInterfaceHelper;
import uw.httpclient.xml.XmlObjectMapperImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * XmlInterfaceHelper测试类。
 * 本测试不依赖外部网络，仅验证可纯本地运行的逻辑路径。
 *
 * @since 2017/12/13
 */
public class XmlInterfaceTest {

    private final XmlObjectMapperImpl converter = new XmlObjectMapperImpl();

    /**
     * XML转换器应能序列化简单对象（不断言具体XML结构，因受命名空间影响）。
     */
    @Test
    public void testConverterSerialize() {
        uw.httpclient.http.xml.vo.SessionVo vo = new uw.httpclient.http.xml.vo.SessionVo();
        vo.setSessionId("s-123");
        String xml = converter.toString(vo);
        assertEquals(true, xml != null && xml.length() > 0);
    }

    /**
     * String类型应直接透传。
     */
    @Test
    public void testConverterParseString() {
        String raw = "<a>1</a>";
        assertSame(raw, converter.parse(raw, String.class));
    }

    /**
     * null内容解析应返回null。
     */
    @Test
    public void testConverterParseNullSafe() {
        assertNull(converter.parse(null, uw.httpclient.http.xml.vo.SessionVo.class));
    }

    /**
     * XmlInterfaceHelper默认构造应使用XML_CONVERTER与XML_UTF8。
     */
    @Test
    public void testXmlHelperDefaults() {
        HttpInterface helper = new XmlInterfaceHelper();
        assertSame(XmlInterfaceHelper.XML_CONVERTER, helper.getObjectMapper());
        assertEquals(MediaTypes.XML_UTF8, helper.getMediaType());
    }
}
