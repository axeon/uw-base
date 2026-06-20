package uw.httpclient.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uw.httpclient.json.JsonInterfaceHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * HttpInterface真实HTTP回归测试。
 * 使用JDK自带HttpServer起本地echo服务，验证GET/POST/PUT/PATCH/DELETE/Form/Body等全链路，
 * 专门用于确保HttpInterface重构（抽取fillRequestMeta/fillResponse等）未引入回归。
 * 不依赖外部网络与MockWebServer。
 */
public class HttpInterfaceHttpTest {

    private static HttpServer server;
    private static String base;
    private static final AtomicReference<String> lastMethod = new AtomicReference<>();
    private static final AtomicReference<String> lastPath = new AtomicReference<>();
    private static final AtomicReference<String> lastQuery = new AtomicReference<>();
    private static final AtomicReference<String> lastBody = new AtomicReference<>();
    private static final AtomicReference<String> lastContentType = new AtomicReference<>();
    private static final AtomicReference<String> lastAuthHeader = new AtomicReference<>();
    private static final AtomicInteger callCount = new AtomicInteger(0);

    @BeforeClass
    public static void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", new EchoHandler());
        server.start();
        int port = server.getAddress().getPort();
        base = "http://127.0.0.1:" + port;
    }

    @AfterClass
    public static void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    static class EchoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            callCount.incrementAndGet();
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getQuery());
            lastContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            lastAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] reqBytes = exchange.getRequestBody().readAllBytes();
            lastBody.set(reqBytes.length == 0 ? null : new String(reqBytes, StandardCharsets.UTF_8));
            // 响应体回显方法+path，便于断言
            String resp = "{\"method\":\"" + exchange.getRequestMethod() + "\",\"path\":\"" + exchange.getRequestURI().getPath() + "\"}";
            byte[] data = resp.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }
    }

    private JsonInterfaceHelper helper() {
        return new JsonInterfaceHelper(HttpConfig.builder()
                .connectTimeout(3000).readTimeout(3000).writeTimeout(3000).build());
    }

    @Test
    public void testGetForData() {
        HttpData d = helper().getForData(base + "/get1");
        assertEquals(200, d.getStatusCode());
        assertEquals("GET", d.getRequestMethod());
        assertEquals("/get1", lastPath.get());
        assertNotNull(d.getResponseBytes());
        assertTrue(new String(d.getResponseBytes(), StandardCharsets.UTF_8).contains("\"method\":\"GET\""));
        assertNotNull(d.getResponseDate());
        assertNotNull(d.getRequestDate());
    }

    @Test
    public void testGetWithQueryParam() {
        Map<String, String> q = new HashMap<>();
        q.put("k1", "v1");
        q.put("k2", "v2");
        HttpData d = helper().getForData(base + "/getq", q);
        assertEquals(200, d.getStatusCode());
        String query = lastQuery.get();
        assertTrue(query.contains("k1=v1"));
        assertTrue(query.contains("k2=v2"));
    }

    @Test
    public void testGetWithHeaders() {
        Map<String, String> h = new HashMap<>();
        h.put("X-Test", "abc");
        HttpData d = helper().getForData(base + "/geth", h, null);
        assertEquals(200, d.getStatusCode());
        assertTrue(d.getRequestHeader().contains("X-Test: abc"));
    }

    @Test
    public void testGetForEntity() {
        HttpEntity<? extends HttpData, EchoVo> e = helper().getForEntity(base + "/ge", EchoVo.class);
        assertEquals(200, e.getHttpData().getStatusCode());
        assertEquals("GET", e.getValue().getMethod());
        assertEquals("/ge", e.getValue().getPath());
    }

    @Test
    public void testPostFormForData() {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("a", "1");
        form.put("b", "2");
        HttpData d = helper().postFormForData(base + "/pf", form);
        assertEquals(200, d.getStatusCode());
        assertEquals("POST", lastMethod.get());
        // form内容应以urlencoded发送
        assertNotNull(lastBody.get());
        assertTrue(lastBody.get().contains("a=1"));
        assertTrue(lastBody.get().contains("b=2"));
    }

    @Test
    public void testPostBodyForData() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "test");
        body.put("age", 18);
        HttpData d = helper().postBodyForData(base + "/pb", body);
        assertEquals(200, d.getStatusCode());
        assertEquals("POST", lastMethod.get());
        assertNotNull(lastBody.get());
        assertTrue(lastBody.get().contains("\"name\":\"test\""));
        assertTrue(lastContentType.get().contains("application/json"));
    }

    @Test
    public void testPostBodyForEntity() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("x", 1);
        HttpEntity<? extends HttpData, EchoVo> e = helper().postBodyForEntity(base + "/pbe", EchoVo.class, body);
        assertEquals(200, e.getHttpData().getStatusCode());
        assertEquals("POST", e.getValue().getMethod());
        assertEquals("/pbe", e.getValue().getPath());
    }

    @Test
    public void testPutFormForData() {
        Map<String, String> form = new HashMap<>();
        form.put("k", "v");
        HttpData d = helper().putFormForData(base + "/putf", form);
        assertEquals(200, d.getStatusCode());
        assertEquals("PUT", lastMethod.get());
    }

    @Test
    public void testPutBodyForData() {
        HttpData d = helper().putBodyForData(base + "/putb", "hello");
        assertEquals(200, d.getStatusCode());
        assertEquals("PUT", lastMethod.get());
        assertEquals("hello", lastBody.get());
    }

    @Test
    public void testPatchFormForData() {
        Map<String, String> form = new HashMap<>();
        form.put("k", "v");
        HttpData d = helper().patchFormForData(base + "/patchf", form);
        assertEquals(200, d.getStatusCode());
        assertEquals("PATCH", lastMethod.get());
    }

    @Test
    public void testPatchBodyForData() {
        HttpData d = helper().patchBodyForData(base + "/patchb", "hello");
        assertEquals(200, d.getStatusCode());
        assertEquals("PATCH", lastMethod.get());
        assertEquals("hello", lastBody.get());
    }

    @Test
    public void testDeleteForData() {
        HttpData d = helper().deleteForData(base + "/del");
        assertEquals(200, d.getStatusCode());
        assertEquals("DELETE", lastMethod.get());
    }

    @Test
    public void testDeleteWithQueryForEntity() {
        Map<String, String> q = new HashMap<>();
        q.put("id", "9");
        HttpEntity<? extends HttpData, EchoVo> e = helper().deleteForEntity(base + "/dele", EchoVo.class, q);
        assertEquals(200, e.getHttpData().getStatusCode());
        assertEquals("DELETE", e.getValue().getMethod());
        assertTrue(lastQuery.get().contains("id=9"));
    }

    /**
     * requestForData路径（自定义Request）：验证填充逻辑不抛异常。
     */
    @Test
    public void testRequestForData() {
        okhttp3.Request req = new okhttp3.Request.Builder().url(base + "/req").get().build();
        HttpData d = helper().requestForData(req);
        assertEquals(200, d.getStatusCode());
        assertEquals("GET", d.getRequestMethod());
    }

    /**
     * HttpDataLogLevel=RECORD_REQUEST时，postBody应记录请求体与大小。
     */
    @Test
    public void testRecordRequestBody() {
        JsonInterfaceHelper h = new JsonInterfaceHelper(
                HttpConfig.builder().connectTimeout(3000).readTimeout(3000).writeTimeout(3000).build(),
                null, HttpDataLogLevel.RECORD_REQUEST, null);
        HttpData d = h.postBodyForData(base + "/rec", "{\"a\":1}");
        assertEquals(200, d.getStatusCode());
        assertNotNull(d.getRequestData());
        assertTrue(d.getRequestData().contains("\"a\":1"));
        assertTrue(d.getRequestSize() > 0);
    }

    /**
     * 默认RECORD_RESPONSE级别，不记录请求体。
     */
    @Test
    public void testNotRecordRequestBodyByDefault() {
        HttpData d = helper().postBodyForData(base + "/norec", "{\"a\":1}");
        assertEquals(200, d.getStatusCode());
        assertNull(d.getRequestData());
        assertEquals(0, d.getRequestSize());
    }

    /**
     * 非法url应抛HttpRequestException。
     */
    @Test
    public void testInvalidUrlThrowsHttpRequestException() {
        try {
            helper().getForData("not-a-valid-url-://!!!");
            org.junit.Assert.fail("expected HttpRequestException");
        } catch (uw.httpclient.exception.HttpRequestException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * 连接不可达应抛HttpRequestException。
     */
    @Test
    public void testConnectionFailureThrowsHttpRequestException() {
        // 1号端口通常无服务，触发连接失败
        JsonInterfaceHelper h = new JsonInterfaceHelper(
                HttpConfig.builder().connectTimeout(1000).readTimeout(1000).writeTimeout(1000).build());
        try {
            h.getForData("http://127.0.0.1:1/unreachable");
            org.junit.Assert.fail("expected HttpRequestException");
        } catch (uw.httpclient.exception.HttpRequestException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * HttpDataProcessor回调验证：responseProcess/postProcess被调用。
     */
    @Test
    public void testProcessorInvoked() {
        final AtomicInteger respProc = new AtomicInteger();
        final AtomicInteger postProc = new AtomicInteger();
        HttpDataProcessor<HttpData, Object> proc = new HttpDataProcessor<HttpData, Object>() {
            @Override
            public void requestProcess(String requestBody, Map<String, String> formData, Map<String, String> headers) {
            }

            @Override
            public void responseProcess(HttpData httpData, okhttp3.Headers headers) {
                respProc.incrementAndGet();
            }

            @Override
            public void postProcess(HttpData httpData, Object o) {
                postProc.incrementAndGet();
            }
        };
        JsonInterfaceHelper h = new JsonInterfaceHelper(
                HttpConfig.builder().connectTimeout(3000).readTimeout(3000).writeTimeout(3000).build(),
                null, null, proc);
        HttpEntity<? extends HttpData, EchoVo> e = h.getForEntity(base + "/proc", EchoVo.class);
        assertEquals(200, e.getHttpData().getStatusCode());
        assertEquals(1, respProc.get());
        assertEquals(1, postProc.get());
    }

    /**
     * 验证dispatcher隔离：配置maxRequests不应影响全局globalOkHttpClient的dispatcher。
     */
    @Test
    public void testDispatcherIsolation() {
        JsonInterfaceHelper h = new JsonInterfaceHelper(
                HttpConfig.builder().maxRequests(42).maxRequestsPerHost(7).build());
        okhttp3.Dispatcher d = h.getOkHttpClient().dispatcher();
        assertEquals(42, d.getMaxRequests());
        assertEquals(7, d.getMaxRequestsPerHost());
        // globalOkHttpClient的dispatcher默认maxRequests=64, maxRequestsPerHost=5
        // 但globalOkHttpClient是private，通过另建一个无config的helper间接验证其dispatcher未被污染
        JsonInterfaceHelper h2 = new JsonInterfaceHelper();
        okhttp3.Dispatcher d2 = h2.getOkHttpClient().dispatcher();
        assertEquals(64, d2.getMaxRequests());
        assertEquals(5, d2.getMaxRequestsPerHost());
    }

    public static class EchoVo {
        private String method;
        private String path;

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }
}
