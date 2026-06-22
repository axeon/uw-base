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
     * HttpConfig 新增项（默认头/CookieJar/拦截器）应正确承载，且拷贝 Builder 保留。
     */
    @Test
    public void testHttpConfigExtendedOptions() {
        java.util.Map<String, String> defaults = new HashMap<>();
        defaults.put("X-Default", "dv");
        okhttp3.Interceptor app = chain -> chain.proceed(chain.request());
        okhttp3.Interceptor net = chain -> chain.proceed(chain.request());
        okhttp3.CookieJar jar = new okhttp3.CookieJar() {
            @Override public void saveFromResponse(okhttp3.HttpUrl url, java.util.List<okhttp3.Cookie> cookies) {}
            @Override public java.util.List<okhttp3.Cookie> loadForRequest(okhttp3.HttpUrl url) { return java.util.Collections.emptyList(); }
        };
        HttpConfig config = HttpConfig.builder()
                .defaultHeaders(defaults)
                .cookieJar(jar)
                .addInterceptor(app)
                .addNetworkInterceptor(net)
                .build();
        assertEquals(defaults, config.getDefaultHeaders());
        assertSame(jar, config.getCookieJar());
        assertEquals(1, config.getInterceptors().size());
        assertSame(app, config.getInterceptors().get(0));
        assertEquals(1, config.getNetworkInterceptors().size());
        assertSame(net, config.getNetworkInterceptors().get(0));
        // 拦截器列表不可变
        try {
            config.getInterceptors().add(app);
            fail("interceptors list should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // 期望不可变
        }
        // 拷贝 Builder 保留全部新项
        HttpConfig copy = HttpConfig.builder(config).build();
        assertEquals(config.getDefaultHeaders(), copy.getDefaultHeaders());
        assertSame(config.getCookieJar(), copy.getCookieJar());
        assertEquals(config.getInterceptors(), copy.getInterceptors());
        assertEquals(config.getNetworkInterceptors(), copy.getNetworkInterceptors());
    }

    /**
     * HttpConfig 新增项默认值：未设置时 defaultHeaders/cookieJar 为 null，拦截器列表为空（非 null）。
     */
    @Test
    public void testHttpConfigExtendedDefaults() {
        HttpConfig config = HttpConfig.builder().build();
        assertNull(config.getDefaultHeaders());
        assertNull(config.getCookieJar());
        assertNotNull(config.getInterceptors());
        assertTrue(config.getInterceptors().isEmpty());
        assertNotNull(config.getNetworkInterceptors());
        assertTrue(config.getNetworkInterceptors().isEmpty());
    }

    /**
     * HttpConfig 的 defaultHeaders/interceptors 不可变：构建后修改原 Map/List 不影响配置，
     * 且返回的视图不可修改（HttpConfig 契约承诺"不可变配置对象"）。
     */
    @Test
    public void testHttpConfigImmutability() {
        java.util.Map<String, String> headers = new HashMap<>();
        headers.put("X-A", "1");
        java.util.List<okhttp3.Interceptor> list = new java.util.ArrayList<>();
        okhttp3.Interceptor it = chain -> chain.proceed(chain.request());
        list.add(it);
        HttpConfig config = HttpConfig.builder().defaultHeaders(headers).interceptors(list).build();
        // 修改原始集合不影响配置（防御性拷贝）
        headers.put("X-B", "2");
        list.clear();
        assertEquals(1, config.getDefaultHeaders().size());
        assertEquals("1", config.getDefaultHeaders().get("X-A"));
        assertEquals(1, config.getInterceptors().size());
        // 返回视图不可修改
        try {
            config.getDefaultHeaders().put("X-C", "3");
            fail("defaultHeaders should be unmodifiable");
        } catch (UnsupportedOperationException expected) { /* 期望 */ }
        try {
            config.getInterceptors().clear();
            fail("interceptors should be unmodifiable");
        } catch (UnsupportedOperationException expected) { /* 期望 */ }
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

    /**
     * HttpDefaultData 新增响应上下文字段的 setter/getter：responseMessage / elapsedMillis / retryCount。
     */
    @Test
    public void testHttpDefaultDataNewFields() {
        uw.httpclient.http.HttpDefaultData data = new uw.httpclient.http.HttpDefaultData();
        // 默认值
        assertEquals(-1L, data.getElapsedMillis());
        assertEquals(0, data.getRetryCount());
        assertNull(data.getResponseMessage());
        // setter/getter 往返
        data.setResponseMessage("Not Found");
        assertEquals("Not Found", data.getResponseMessage());
        data.setElapsedMillis(123L);
        assertEquals(123L, data.getElapsedMillis());
        data.setRetryCount(3);
        assertEquals(3, data.getRetryCount());
    }

    /**
     * HttpDefaultData.getResponseHeader 便捷取值：大小写不敏感、缺失返回 null。
     */
    @Test
    public void testHttpDefaultDataGetResponseHeader() {
        uw.httpclient.http.HttpDefaultData data = new uw.httpclient.http.HttpDefaultData();
        // 无响应头时返回 null，不 NPE
        assertNull(data.getResponseHeader("X-Any"));
        assertNull(data.getResponseHeader(null));
        // 构造大小写不敏感的多值头
        java.util.Map<String, java.util.List<String>> headers = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("X-Trace-Id", java.util.Collections.singletonList("t-1"));
        headers.put("Set-Cookie", java.util.Arrays.asList("a=1", "b=2"));
        data.setResponseHeaders(headers);
        // 大小写不敏感取单值（首个）
        assertEquals("t-1", data.getResponseHeader("x-trace-id"));
        assertEquals("t-1", data.getResponseHeader("X-TRACE-ID"));
        // 多值头取首个
        assertEquals("a=1", data.getResponseHeader("Set-Cookie"));
        // 缺失返回 null
        assertNull(data.getResponseHeader("Not-Exist"));
    }
}
