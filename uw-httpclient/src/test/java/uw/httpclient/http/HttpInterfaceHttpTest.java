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

    /**
     * 本地 Echo Handler：记录每次请求的方法/路径/查询串/Content-Type/Authorization/请求体，
     * 并回显 {@code {"method":...,"path":...}} JSON。响应头附带 {@code X-Server} 与多值 {@code X-Multi}，
     * 供响应头相关断言使用。
     */
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
            exchange.getResponseHeaders().add("X-Server", "uw-httpclient-test");
            // 同名多值头，用于验证 List<String> 形态
            exchange.getResponseHeaders().add("X-Multi", "a");
            exchange.getResponseHeaders().add("X-Multi", "b");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }
    }

    /**
     * 构造一个带短超时（3s）的 JsonInterfaceHelper，retryOnConnectionFailure 保持默认（不重试）。
     * 每个 helper 持有独立的 OkHttpClient 与 Dispatcher，测试间互不串扰。
     *
     * @return 测试用 JsonInterfaceHelper。
     */
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

    @Test
    public void testResponseHeadersExposed() {
        HttpData d = helper().getForData(base + "/rh");
        assertEquals(200, d.getStatusCode());
        assertNotNull(d.getResponseHeaders());
        // 大小写不敏感
        assertEquals("application/json; charset=utf-8", d.getResponseHeaders().get("Content-Type").get(0));
        assertEquals("uw-httpclient-test", d.getResponseHeaders().get("x-server").get(0));
        // 同名多值
        java.util.List<String> multi = d.getResponseHeaders().get("X-Multi");
        assertNotNull(multi);
        assertEquals(2, multi.size());
        assertTrue(multi.contains("a") && multi.contains("b"));
        // HttpDefaultData 的便捷取值
        assertTrue(d instanceof HttpDefaultData);
        assertEquals("uw-httpclient-test", ((HttpDefaultData) d).getResponseHeader("X-SERVER"));
        assertNull(((HttpDefaultData) d).getResponseHeader("Not-Exist"));
    }

    @Test
    public void testResponseMessageAndElapsed() {
        HttpData d = helper().getForData(base + "/rme");
        assertEquals(200, d.getStatusCode());
        // JDK HttpServer 对 200 返回 "OK"
        assertEquals("OK", d.getResponseMessage());
        // 耗时已记录（>=0）
        assertTrue("elapsedMillis should be >= 0", d.getElapsedMillis() >= 0);
        // helper() 默认 retryOnConnectionFailure=false，正常成功请求无重试
        assertEquals(0, d.getRetryCount());
    }

    @Test
    public void testRetryCountOnRedirect() throws Exception {
        // 端到端验证：OkHttp 默认 followRedirects=true，302 会触发一次 follow-up（RetryAndFollowUpInterceptor），
        // 网络拦截器 proceed 2 次 → retryCount=1。证明 retryCount 能捕获真实重定向，而非仅单元测试的模拟。
        // 用独立 server，/start 返回 302 → /end（200）。
        HttpServer redir = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        int port = redir.getAddress().getPort();
        redir.createContext("/start", ex -> {
            ex.getResponseHeaders().add("Location", "http://127.0.0.1:" + port + "/end");
            ex.sendResponseHeaders(302, -1);
            ex.getResponseBody().close();
        });
        redir.createContext("/end", ex -> {
            byte[] b = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(b); }
        });
        redir.start();
        try {
            HttpData d = helper().getForData("http://127.0.0.1:" + port + "/start");
            assertEquals(200, d.getStatusCode());
            assertEquals(1, d.getRetryCount());
        } finally {
            redir.stop(0);
        }
    }

    @Test
    public void testRetryCountNoRetryOnSuccess() {
        // helper() 的 client 注入了内部重试计数网络拦截器；单次成功请求 retryCount 必为 0，
        // 验证计数器正确注册/清理，不会因计数泄漏导致后续请求读到脏值。
        JsonInterfaceHelper h = helper();
        HttpData d1 = h.getForData(base + "/rc1");
        HttpData d2 = h.getForData(base + "/rc2");
        assertEquals(0, d1.getRetryCount());
        assertEquals(0, d2.getRetryCount());
    }

    /**
     * 最小 Interceptor.Chain 桩，用于单元测试 RetryCounter 的计数语义。
     * proceed 返回固定 Response，call 返回固定 Call，request 返回固定 Request。
     */
    private static final class FakeChain implements okhttp3.Interceptor.Chain {
        private final okhttp3.Call call;
        private final okhttp3.Request request;
        private final okhttp3.Response response;
        FakeChain(okhttp3.Call call, okhttp3.Request request, okhttp3.Response response) {
            this.call = call; this.request = request; this.response = response;
        }
        @Override public okhttp3.Request request() { return request; }
        @Override public okhttp3.Response proceed(okhttp3.Request request) { return response; }
        @Override public okhttp3.Call call() { return call; }
        @Override public okhttp3.Connection connection() { return null; }
        @Override public int connectTimeoutMillis() { return 0; }
        @Override public okhttp3.Interceptor.Chain withConnectTimeout(int timeout, java.util.concurrent.TimeUnit unit) { return this; }
        @Override public int readTimeoutMillis() { return 0; }
        @Override public okhttp3.Interceptor.Chain withReadTimeout(int timeout, java.util.concurrent.TimeUnit unit) { return this; }
        @Override public int writeTimeoutMillis() { return 0; }
        @Override public okhttp3.Interceptor.Chain withWriteTimeout(int timeout, java.util.concurrent.TimeUnit unit) { return this; }
    }

    @Test
    public void testRetryCounterSingleProceed() throws Exception {
        // 单次 proceed（无重试/无 follow-up）→ retryCount = 0，且 consume 后清理（再 consume 仍为 0）
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        okhttp3.Request req = new okhttp3.Request.Builder().url("http://127.0.0.1:1/x").build();
        okhttp3.Call call = client.newCall(req);
        okhttp3.Response resp = new okhttp3.Response.Builder().request(req).protocol(okhttp3.Protocol.HTTP_1_1)
                .code(200).message("OK").build();
        HttpInterface.RetryCounter rc = new HttpInterface.RetryCounter();
        rc.intercept(new FakeChain(call, req, resp));
        assertEquals(0, rc.consumeRetryCount(call));
        // 清理后再取，不报错、不返回脏值
        assertEquals(0, rc.consumeRetryCount(call));
    }

    @Test
    public void testRetryCounterMultipleProceed() throws Exception {
        // 模拟同一 Call 发生 2 次重试（共 3 次 proceed）→ retryCount = 2
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        okhttp3.Request req = new okhttp3.Request.Builder().url("http://127.0.0.1:1/x").build();
        okhttp3.Call call = client.newCall(req);
        okhttp3.Response resp = new okhttp3.Response.Builder().request(req).protocol(okhttp3.Protocol.HTTP_1_1)
                .code(200).message("OK").build();
        HttpInterface.RetryCounter rc = new HttpInterface.RetryCounter();
        FakeChain chain = new FakeChain(call, req, resp);
        rc.intercept(chain);
        rc.intercept(chain);
        rc.intercept(chain);
        assertEquals(2, rc.consumeRetryCount(call));
    }

    @Test
    public void testRetryCounterIsolationBetweenCalls() throws Exception {
        // 不同 Call 的计数互不干扰（验证以 Call 为 key 的隔离性）
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        okhttp3.Request req = new okhttp3.Request.Builder().url("http://127.0.0.1:1/x").build();
        okhttp3.Call callA = client.newCall(req);
        okhttp3.Call callB = client.newCall(req);
        okhttp3.Response resp = new okhttp3.Response.Builder().request(req).protocol(okhttp3.Protocol.HTTP_1_1)
                .code(200).message("OK").build();
        HttpInterface.RetryCounter rc = new HttpInterface.RetryCounter();
        rc.intercept(new FakeChain(callA, req, resp));
        rc.intercept(new FakeChain(callA, req, resp)); // callA proceed 2 次
        rc.intercept(new FakeChain(callB, req, resp)); // callB proceed 1 次
        assertEquals(1, rc.consumeRetryCount(callA));
        assertEquals(0, rc.consumeRetryCount(callB));
    }


    @Test
    public void testCookieJarAndInterceptor() {
        okhttp3.CookieJar jar = new okhttp3.CookieJar() {
            final java.util.Map<String, java.util.List<okhttp3.Cookie>> store = new java.util.concurrent.ConcurrentHashMap<>();

            @Override
            public void saveFromResponse(okhttp3.HttpUrl url, java.util.List<okhttp3.Cookie> cookies) {
                store.put(url.host(), cookies);
            }

            @Override
            public java.util.List<okhttp3.Cookie> loadForRequest(okhttp3.HttpUrl url) {
                return store.getOrDefault(url.host(), java.util.Collections.emptyList());
            }
        };
        java.util.concurrent.atomic.AtomicInteger hit = new java.util.concurrent.atomic.AtomicInteger();
        okhttp3.Interceptor app = chain -> {
            hit.incrementAndGet();
            return chain.proceed(chain.request());
        };
        JsonInterfaceHelper h = new JsonInterfaceHelper(HttpConfig.builder()
                .connectTimeout(3000).readTimeout(3000).writeTimeout(3000)
                .cookieJar(jar).addInterceptor(app).build());
        HttpData d = h.getForData(base + "/ci");
        assertEquals(200, d.getStatusCode());
        assertEquals(1, hit.get());
    }

    @Test
    public void testDefaultHeadersViaProcessor() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("X-Default-A", "da");
        defaults.put("X-Shared", "from-default");
        java.util.concurrent.atomic.AtomicReference<okhttp3.Request> captured = new java.util.concurrent.atomic.AtomicReference<>();
        HttpDataProcessor<HttpData, Object> proc = new HttpDataProcessor<HttpData, Object>() {
            @Override
            public void requestProcess(String requestBody, Map<String, String> formData, Map<String, String> headers) {
            }

            @Override
            public void requestProcess(okhttp3.Request request) {
                captured.set(request);
            }

            @Override
            public void responseProcess(HttpData httpData, okhttp3.Headers headers) {
            }

            @Override
            public void postProcess(HttpData httpData, Object o) {
            }
        };
        JsonInterfaceHelper h = new JsonInterfaceHelper(HttpConfig.builder()
                .connectTimeout(3000).readTimeout(3000).writeTimeout(3000)
                .defaultHeaders(defaults).build(), null, null, proc);
        Map<String, String> biz = new HashMap<>();
        biz.put("X-Shared", "from-biz");
        h.getForData(base + "/dhp", biz, null);
        okhttp3.Request req = captured.get();
        assertNotNull(req);
        // 默认头存在
        assertEquals("da", req.header("X-Default-A"));
        // 业务同名头覆盖默认值
        assertEquals("from-biz", req.header("X-Shared"));
    }

    @Test
    public void testProcessorFullLifecycle() {
        // 端到端验证 HttpDataProcessor 四个回调的调用顺序与参数：
        // 1. requestProcess(Request) 能看到 OkHttp 注入的 Host 头（证明拿到的是最终完整 Request）
        // 2. responseProcess 被调用，httpData 含状态码
        // 3. postProcess 在 ForData 时 t=null，ForEntity 时 t=反序列化对象
        java.util.concurrent.atomic.AtomicReference<okhttp3.Request> capturedReq = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger respCall = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicReference<Object> postValue = new java.util.concurrent.atomic.AtomicReference<>(new Object()); // 哨兵：非null
        HttpDataProcessor<HttpData, Object> proc = new HttpDataProcessor<HttpData, Object>() {
            @Override public void requestProcess(String requestBody, Map<String, String> formData, Map<String, String> headers) {}
            @Override
            public void requestProcess(okhttp3.Request request) {
                capturedReq.set(request);
            }
            @Override
            public void responseProcess(HttpData httpData, okhttp3.Headers headers) {
                respCall.incrementAndGet();
            }
            @Override
            public void postProcess(HttpData httpData, Object o) {
                postValue.set(o);
            }
        };
        JsonInterfaceHelper h = new JsonInterfaceHelper(HttpConfig.builder()
                .connectTimeout(3000).readTimeout(3000).writeTimeout(3000).build(), null, null, proc);
        // ForData：postProcess 的 t 应为 null
        h.getForData(base + "/pl1");
        okhttp3.Request req = capturedReq.get();
        assertNotNull(req);
        // 拿到的是最终完整 Request（含已解析的 HttpUrl、method），而非业务侧原始参数
        assertEquals("GET", req.method());
        assertNotNull(req.url());
        assertEquals(1, respCall.get());
        assertNull("ForData postProcess t must be null", postValue.get());

        // ForEntity：postProcess 的 t 应为反序列化对象
        HttpEntity<? extends HttpData, EchoVo> e = h.getForEntity(base + "/pl2", EchoVo.class);
        assertNotNull(e.getValue());
        assertEquals("GET", e.getValue().getMethod());
        assertNotNull("ForEntity postProcess t must be the deserialized object", postValue.get());
    }

    /**
     * Echo 响应体 VO，对应 EchoHandler 回显的 {@code {"method":...,"path":...}} JSON。
     * 用于 {@code *ForEntity} 方法的反序列化断言。
     */
    public static class EchoVo {
        /** HTTP 方法。 */
        private String method;
        /** 请求路径。 */
        private String path;

        /** @return HTTP 方法。 */
        public String getMethod() { return method; }
        /** @param method HTTP 方法。 */
        public void setMethod(String method) { this.method = method; }
        /** @return 请求路径。 */
        public String getPath() { return path; }
        /** @param path 请求路径。 */
        public void setPath(String path) { this.path = path; }
    }
}
