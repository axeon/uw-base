package uw.httpclient.http.json;

import org.junit.Test;
import uw.httpclient.http.HttpConfig;
import uw.httpclient.http.HttpData;
import uw.httpclient.http.HttpDataLogLevel;
import uw.httpclient.http.HttpInterface;
import uw.httpclient.http.json.vo.TestVo;
import uw.httpclient.json.JsonInterfaceHelper;
import uw.httpclient.json.JsonObjectMapperImpl;
import uw.httpclient.util.MediaTypes;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * JsonInterfaceHelper测试类。
 * 本测试不依赖外部网络，仅验证可纯本地运行的逻辑路径：序列化/反序列化、配置、日志级别等。
 *
 * @since 2018-03-01
 */
public class JsonInterfaceTest {

    private final JsonObjectMapperImpl converter = new JsonObjectMapperImpl();

    /**
     * JSON转换器应正确序列化/反序列化对象。
     */
    @Test
    public void testConverterRoundTrip() {
        TestVo vo = new TestVo();
        vo.setName("test");
        vo.setAge(18);
        vo.setAddress("planet");
        String json = converter.toString(vo);
        assertTrue(json.contains("\"name\":\"test\""));
        assertTrue(json.contains("\"age\":18"));
        TestVo parsed = converter.parse(json, TestVo.class);
        assertEquals("test", parsed.getName());
        assertEquals(18, parsed.getAge());
        assertEquals("planet", parsed.getAddress());
    }

    /**
     * String类型应直接透传，不走ObjectMapper。
     */
    @Test
    public void testConverterParseString() {
        String raw = "{\"a\":1}";
        assertSame(raw, converter.parse(raw, String.class));
    }

    /**
     * null内容解析应返回null，而非NPE。
     */
    @Test
    public void testConverterParseNullSafe() {
        assertNull(converter.parse(null, TestVo.class));
    }

    /**
     * Map应正确序列化为JSON。
     */
    @Test
    public void testConverterMapToString() {
        Map<String, String> formData = new HashMap<>();
        formData.put("username", "test");
        formData.put("password", "123");
        String json = converter.toString(formData);
        assertTrue(json.contains("\"username\":\"test\""));
        assertTrue(json.contains("\"password\":\"123\""));
    }

    /**
     * HttpConfig Builder应正确承载各配置项。
     */
    @Test
    public void testHttpConfigBuilder() {
        HttpConfig config = HttpConfig.builder()
                .connectTimeout(1000)
                .readTimeout(2000)
                .writeTimeout(3000)
                .maxRequests(128)
                .maxRequestsPerHost(32)
                .maxIdleConnections(10)
                .keepAliveTimeout(60000)
                .retryOnConnectionFailure(false)
                .build();
        assertEquals(1000L, config.getConnectTimeout());
        assertEquals(2000L, config.getReadTimeout());
        assertEquals(3000L, config.getWriteTimeout());
        assertEquals(128, config.getMaxRequests());
        assertEquals(32, config.getMaxRequestsPerHost());
        assertEquals(10, config.getMaxIdleConnections());
        assertEquals(60000L, config.getKeepAliveTimeout());
    }

    /**
     * HttpConfig基于已有实例的拷贝Builder应完全一致。
     */
    @Test
    public void testHttpConfigCopyBuilder() {
        HttpConfig origin = HttpConfig.builder()
                .connectTimeout(1000)
                .readTimeout(2000)
                .writeTimeout(3000)
                .maxRequests(128)
                .maxRequestsPerHost(32)
                .maxIdleConnections(10)
                .keepAliveTimeout(60000)
                .retryOnConnectionFailure(true)
                .build();
        HttpConfig copy = HttpConfig.builder(origin).build();
        assertEquals(origin.getConnectTimeout(), copy.getConnectTimeout());
        assertEquals(origin.getReadTimeout(), copy.getReadTimeout());
        assertEquals(origin.getWriteTimeout(), copy.getWriteTimeout());
        assertEquals(origin.getMaxRequests(), copy.getMaxRequests());
        assertEquals(origin.getMaxRequestsPerHost(), copy.getMaxRequestsPerHost());
        assertEquals(origin.getMaxIdleConnections(), copy.getMaxIdleConnections());
        assertEquals(origin.getKeepAliveTimeout(), copy.getKeepAliveTimeout());
        assertEquals(origin.isRetryOnConnectionFailure(), copy.isRetryOnConnectionFailure());
    }

    /**
     * JsonInterfaceHelper默认构造应使用JSON_CONVERTER与JSON_UTF8。
     */
    @Test
    public void testJsonHelperDefaults() {
        HttpInterface helper = new JsonInterfaceHelper();
        assertSame(JsonInterfaceHelper.JSON_CONVERTER, helper.getObjectMapper());
        assertEquals(MediaTypes.JSON_UTF8, helper.getMediaType());
    }

    /**
     * 非法url应抛出HttpRequestException（而非RuntimeException）。
     */
    @Test
    public void testInvalidUrlThrowsHttpRequestException() {
        HttpInterface helper = new JsonInterfaceHelper();
        try {
            helper.getForData("not-a-valid-url-://!!!");
            fail("expected HttpRequestException for invalid url");
        } catch (uw.httpclient.exception.HttpRequestException e) {
            // 期望异常类型，通过
            assertNotNull(e.getMessage());
        }
    }

    /**
     * HttpDataLogLevel.isRecordRequest的语义。
     */
    @Test
    public void testHttpDataLogLevel() {
        assertTrue(HttpDataLogLevel.isRecordRequest(HttpDataLogLevel.RECORD_REQUEST));
        assertTrue(HttpDataLogLevel.isRecordRequest(HttpDataLogLevel.RECORD_ALL));
        assertTrue(!HttpDataLogLevel.isRecordRequest(HttpDataLogLevel.RECORD_RESPONSE));
    }

    /**
     * HttpDefaultData.getResponseData在没有responseBytes时应返回null。
     */
    @Test
    public void testHttpDefaultDataEmptyResponse() {
        uw.httpclient.http.HttpDefaultData data = new uw.httpclient.http.HttpDefaultData();
        assertNull(data.getResponseData());
        assertNull(data.getResponseBytes());
        data.setResponseBytes("hello".getBytes());
        assertEquals("hello", data.getResponseData());
    }
}
