# uw-httpclient

#### 项目简介

uw-httpclient 是针对接口业务需求设计的 HttpClient 库。
当前底层使用 OkHttp 实现，序列化使用 Jackson（支持 JSON / XML），将来可切换为 AHC、httpclient5。

#### 项目特性

1. 完整支持 GET / POST / PUT / PATCH / DELETE 各种方法；
2. 请求、返回对象序列化集成到类库中，并可自由定义 `DataObjectMapper`；
3. 支持 JSON / XML 格式接口（`JsonInterfaceHelper` / `XmlInterfaceHelper`）；
4. 支持完整的 HTTP 请求日志（`HttpData`），并可自定义日志类与数据处理器（`HttpDataProcessor`）；
5. 支持表单（Form）、请求体（Body）、文件上传（multipart FormFile）与文件下载。

#### 快速入门

##### 添加 Maven dependency

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-httpclient</artifactId>
    <version>${uw-httpclient.version}</version>
</dependency>
```

##### 常用入口

- `JsonInterfaceHelper`：基于 JSON 的接口帮助入口。
- `XmlInterfaceHelper`：基于 XML 的接口帮助入口。

两者本质都是通过构建特定参数的 `HttpInterface` 来实现，初始化方式类似。下面以 `JsonInterfaceHelper` 为例：

```java
// 最简用法
JsonInterfaceHelper helper = new JsonInterfaceHelper();

// 带配置
JsonInterfaceHelper helper = new JsonInterfaceHelper(
        HttpConfig.builder()
                .connectTimeout(5000)
                .readTimeout(30000)
                .writeTimeout(10000)
                .retryOnConnectionFailure(false)
                .build());
```

完整构造器（JSON 与 XML 同构）：

```java
/**
 * 完整构造器。
 * @param httpConfig        HttpConfig 配置参数（可为 null，使用全局默认 client）。
 * @param httpDataCls       指定 HttpData 实现类（自定义日志类，可为 null，默认 HttpDefaultData）。
 * @param httpDataLogLevel  HttpData 日志级别（可为 null，默认 RECORD_RESPONSE）。
 * @param httpDataProcessor HttpData 数据处理器（加解密 / 日志上报等，可为 null）。
 */
public JsonInterfaceHelper(HttpConfig httpConfig,
                           Class<? extends HttpData> httpDataCls,
                           HttpDataLogLevel httpDataLogLevel,
                           HttpDataProcessor httpDataProcessor) {
    super(httpConfig, httpDataCls, httpDataLogLevel, httpDataProcessor,
          JsonInterfaceHelper.JSON_CONVERTER, MediaTypes.JSON_UTF8);
}
```

##### 方法命名规律

```
{httpMethod}{请求体形式}{返回形式}
  httpMethod:    get / post / put / patch / delete
  请求体形式:    Form(表单) | Body(请求体) | FormFile(含文件上传) | 无(get/delete)
  返回形式:      ForData(返回 HttpData) | ForEntity(返回 HttpEntity，含反序列化对象)
```

- `ForData` 返回 `HttpData`，不做响应反序列化，**二进制友好**（适合下载）。
- `ForEntity` 返回 `HttpEntity<HttpData, T>`，含反序列化后的对象，取 `.getValue()`。
- 每个 `ForEntity` 方法都提供 `Class<T>` / `TypeReference<T>` / `JavaType` 三套响应类型重载，每套再各有一个带 `headers` 的重载。

##### 主要方法

- `getForData` / `getForEntity`：GET。`getForData` 也可用于下载二进制数据（图片、文件）。
- `postFormForData` / `postFormForEntity`：POST 表单（application/x-www-form-urlencoded）。
- `postBodyForData` / `postBodyForEntity`：POST 请求体（按配置的 mediaType 序列化，默认 JSON）。
- `postFormFileForData` / `postFormFileForEntity`：POST multipart，**文件上传**（fileData 值支持 `byte[]` / `File` / 其它）。
- `putFormForData` / `putBodyForData` / `putFormForEntity` / `putBodyForEntity`：PUT。
- `patchFormForData` / `patchBodyForData` / `patchFormForEntity` / `patchBodyForEntity`：PATCH。
- `deleteForData` / `deleteForEntity`：DELETE（无 body，可带 queryParam）。

此外提供基于 OkHttp 原生 `Request` 的基础方法，用于更灵活的场景：

- `requestForData(Request)` / `requestForEntity(Request, Class)`：传入自定义 Request。
- `getOkHttpClient()`：直接获取底层 OkHttpClient，可自由操作（兜底方案）。

##### HttpConfig 配置

`HttpConfig` 提供常用配置参数，通过 Builder 构建：

```java
HttpConfig config = HttpConfig.builder()
        .connectTimeout(5000)        // 连接超时（毫秒）
        .readTimeout(30000)          // 读超时（毫秒）
        .writeTimeout(10000)         // 写超时（毫秒）
        .retryOnConnectionFailure(false) // 连接失败是否重试
        .maxRequests(64)             // 全局最大并发
        .maxRequestsPerHost(32)      // 每主机最大并发
        .maxIdleConnections(50)      // 连接池最大空闲连接
        .keepAliveTimeout(300000)    // 空闲连接存活时间（毫秒）
        // .sslSocketFactory(...) / .trustManager(...) / .hostnameVerifier(...)
        .build();
```

| 属性 | 类型 | 说明 |
|------|------|------|
| connectTimeout | long | 连接超时（毫秒） |
| readTimeout | long | 读超时（毫秒） |
| writeTimeout | long | 写超时（毫秒） |
| retryOnConnectionFailure | boolean | 连接失败重试（幂等敏感接口建议 false） |
| maxRequests / maxRequestsPerHost | int | 全局 / 每主机最大并发 |
| maxIdleConnections / keepAliveTimeout | int / long | 连接池空闲连接数 / 存活时间 |
| sslSocketFactory / trustManager / hostnameVerifier | — | SSL 配置 |

> 每个 `HttpInterface` 实例使用**独立的 Dispatcher**，配置的 maxRequests / maxRequestsPerHost 仅作用于该实例，不会影响全局或其他实例。

##### HttpData 日志记录

- uw-httpclient 使用 `HttpData` 对象记录「请求 → 结果」日志，含请求地址、方法、时间、请求内容、返回状态码、返回结果等。
- 为减少无意义的 copy，开发者可自定义 `HttpData` 实现类（继承 `HttpDefaultData`）直接作为日志类使用。
- 为兼容二进制与文本数据，`HttpData` 优先设置 `responseBytes`，推荐在访问 `getResponseData()` 时才懒转换为字符串（UTF-8），避免不必要的转换。
- `HttpDataLogLevel` 控制日志范围：**响应始终记录**，是否额外记录请求体取决于级别（`RECORD_RESPONSE` 默认不记录，`RECORD_REQUEST` / `RECORD_ALL` 记录）。

##### HttpDataProcessor 数据处理器

`HttpDataProcessor` 用于处理请求数据与返回数据，典型场景：

- 加解密、签名验证：在 `requestProcess` / `responseProcess` 中统一处理，让业务代码更整洁。
- 日志上报：在 `postProcess` 中将 `HttpData` 发送到远端日志系统。

```java
public interface HttpDataProcessor<D extends HttpData, T> {
    void requestProcess(String requestBody, Map<String,String> formData, Map<String,String> headers);
    void responseProcess(D httpData, Headers headers);
    void postProcess(D httpData, T t);
}
```

##### 使用示例

```java
import com.fasterxml.jackson.core.type.TypeReference;
import uw.httpclient.json.JsonInterfaceHelper;
import uw.httpclient.http.*;

import java.io.File;
import java.util.*;

public class HttpHelper {
    private static final JsonInterfaceHelper http = new JsonInterfaceHelper(
            HttpConfig.builder()
                    .connectTimeout(5000).readTimeout(30000).writeTimeout(10000)
                    .retryOnConnectionFailure(false)
                    .build());

    // GET → 对象
    public static User getUser(long userId) {
        return http.getForEntity("https://api.example.com/users/" + userId, User.class).getValue();
    }

    // GET → 泛型列表 + 查询参数
    public static List<User> listUsers(String keyword, int page) {
        Map<String, String> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("page", String.valueOf(page));
        return http.getForEntity("https://api.example.com/users",
                new TypeReference<List<User>>() {}, params).getValue();
    }

    // POST JSON body
    public static User createUser(CreateUserRequest req) {
        return http.postBodyForEntity("https://api.example.com/users", User.class, req).getValue();
    }

    // POST 表单
    public static Token login(String user, String pass) {
        Map<String, String> form = new HashMap<>();
        form.put("username", user);
        form.put("password", pass);
        return http.postFormForEntity("https://api.example.com/login", Token.class, form).getValue();
    }

    // 自定义 Header
    public static User requestWithHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token123");
        return http.getForEntity("https://api.example.com/users/1",
                User.class, headers, null).getValue();
    }

    // 文件上传（multipart）
    public static UploadResult upload(File file) {
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("file", file);   // 值支持 byte[] / File / 其它(toString)
        return http.postFormFileForEntity("https://api.example.com/upload",
                UploadResult.class, null, fileData).getValue();
    }

    // 文件下载
    public static byte[] download(String fileUrl) {
        return http.getForData(fileUrl).getResponseBytes();
    }
}
```

##### SSL 自签名证书

```java
HttpConfig config = HttpConfig.builder()
        .sslSocketFactory(SSLContextUtils.getTruestAllSocketFactory())
        .trustManager(SSLContextUtils.getTrustAllManager())
        .hostnameVerifier((hostName, session) -> true)
        .build();
JsonInterfaceHelper helper = new JsonInterfaceHelper(config);
```

> ⚠️ 信任全部证书仅适用于内网自签名 / 测试环境，生产环境须使用受信任证书库。

#### FAQ

**Q: 多次 new OkHttpClient 会不会带来效率问题？**
A: 不会。OkHttpClient 对底层连接是共用的，其 `ConnectionPool` 使用专门线程（cleanupRunnable）自动判定并清理达到存活时间（keepAliveDuration）或超出空闲连接数（idleConnections）的连接，减少长时间占用 Socket 句柄带来的问题。本库内部通过全局 `OkHttpClient.newBuilder()` 派生实例配置，连接池共享。

**Q: ObjectMapper 是否能处理不加注解的 VO 类？**
A: 本库采用 Jackson 作为 JSON / XML 的序列化与反序列化库，已关闭 `FAIL_ON_UNKNOWN_PROPERTIES`（未知属性不报错）。不加 Jackson 注解能否转换成功取决于 Jackson 的默认规则，建议使用 uw-code-center 生成相关的 Java VO 实体类。

**Q: 如何处理内部自签名的 SSL 访问问题？**
A: 构造 `HttpConfig` 时配置信任全部证书（见上文「SSL 自签名证书」章节）。

**Q: `retryOnConnectionFailure` 有什么影响？**
A: 设为 true 将开启请求失败重试。如果业务有严格幂等要求（如重复调用下单会出问题），应设为 false，让程序自己处理错误。

**Q: 如何拿反序列化后的响应对象？**
A: 用 `*ForEntity` 方法，返回 `HttpEntity`，通过 `getValue()` 获取（注意不是 `getBody()`）。

**Q: 方法名记不住怎么办？**
A: 遵循 `{method}{Form|Body|FormFile}{ForData|ForEntity}` 规律：表单用 `Form`，请求体用 `Body`，含文件上传用 `FormFile`；要反序列化用 `ForEntity`，否则用 `ForData`。
