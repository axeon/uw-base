# uw-httpclient

#### 目录

- [项目简介](#项目简介)
- [底层选型：为何选择 OkHttp](#底层选型为何选择-okhttp)
- [架构与模块](#架构与模块)
- [异常体系](#异常体系)
- [环境要求](#环境要求)
- [项目特性](#项目特性)
- [快速入门](#快速入门)
- [HttpConfig 配置](#httpconfig-配置)
- [获取响应头与请求耗时](#获取响应头与请求耗时)
- [HttpData 日志记录](#httpdata-日志记录)
- [HttpDataProcessor 数据处理器](#httpdataprocessor-数据处理器)
- [使用示例](#使用示例)
- [SSL 自签名证书](#ssl-自签名证书)
- [FAQ](#faq)

---

#### 项目简介

uw-httpclient 是针对接口业务需求设计的 HttpClient 库。
底层使用 OkHttp（JVM 版）实现，序列化使用 Jackson（支持 JSON / XML）。

> 底层选型说明见下方「底层选型：为何选择 OkHttp」章节。

#### 底层选型：为何选择 OkHttp

uw-httpclient 在底层 HTTP 引擎上选择了 **OkHttp（JVM 版）**，而非 Apache HttpClient / HC5 / JDK HttpClient。以下是经过评估后的决策依据：

| 维度 | OkHttp | Apache HC5 | JDK HttpClient |
|---|---|---|---|
| 同步 API 易用性 | Builder 链式、API 简洁 | 较繁琐，需手动管理 `HttpEntity` | 偏底层，无连接池快捷配置 |
| WebSocket | ✅ 原生（`newWebSocket`） | ❌ 经典 API 无 | ❌ 无 |
| 拦截器（应用/网络两层） | ✅ 原生 | ⚠️ 经典 API 无原生拦截器链 | ❌ 无 |
| 连接池 + 并发调度（Dispatcher） | ✅ 原生 `Dispatcher`（请求级 maxRequests） | 连接级池化，无请求级调度 | 连接级池化 |
| okio 集成（流式/批量场景） | ✅ 深度集成，支持 segment 共享零拷贝 | ❌ 需手写桥接 Entity，多一次拷贝 | ❌ |
| 重试/重定向/认证 follow-up | ✅ 原生，可观测 | 经典 API 能力较弱 | 部分 |
| 二进制兼容与升级平滑度 | 5.x 对 4.12 二进制兼容 | 5.x 有 breaking change | — |
| 维护活跃度 | Square 主导，持续迭代 | 维护中 | 随 JDK |

**核心决策因素**：

1. **WebSocket 原生支持**：本项目 `uw-ai-center` 的 DashScope 实时语音转写依赖 WebSocket，OkHttp 原生提供；HC5 / JDK HttpClient 均无，需引入额外组件。
2. **okio 零拷贝能力**：`uw-log-es` / `uw-logback-es` 批量日志上报用 `okio.Buffer` 攒批 + segment 共享直送 sink，省掉"攒批→序列化→复制成 byte[]"的拷贝与内存峰值。OkHttp 与 okio 深度集成是唯一能保留这一能力的方案，换 HC5 需手写桥接 Entity 且会多一次拷贝。
3. **拦截器与可观测性**：OkHttp 原生两层拦截器（应用层/网络层），配合本库的 `retryCount` 观测、`HttpDataProcessor` 机制，能力完整；HC5 经典 API 缺原生拦截器链。
4. **升级平滑**：OkHttp 5.x 对 4.12 的稳定 API 保持二进制兼容，包名 `okhttp3.*` 不变，下游零改动即可升级；HC5 的 4.x→5.x 有 breaking change。
5. **调度模型**：OkHttp `Dispatcher` 提供请求级 `maxRequests` / `maxRequestsPerHost` 并发上限，语义清晰；HC5 是连接级池化，无对等概念。

> 综上，OkHttp 在 WebSocket、流式性能、拦截器、升级平滑度上均有不可替代的优势，是本库的最佳选择。本库的对外 API 已尽量收敛对 `okhttp3.*` 类型的直接暴露（响应头通过中性 `Map<String,List<String>>` 表达），仅在 `requestForData(okhttp3.Request)`、`getOkHttpClient()`、`HttpDataProcessor` 等高级入口保留 OkHttp 类型作为"兜底方案"。

#### 架构与模块

```
uw-httpclient
├── http/                      核心
│   ├── HttpInterface          HTTP 请求抽象实现（基于 OkHttp 封装）
│   ├── HttpConfig             不可变配置（Builder 构建）
│   ├── HttpData / HttpDefaultData   请求/响应日志数据载体
│   ├── HttpEntity             封装 HttpData + 反序列化对象
│   ├── HttpDataProcessor      数据处理器（加解密/签名/日志上报）
│   ├── HttpDataLogLevel       日志级别（是否记录请求体）
│   └── DataObjectMapper       序列化抽象接口
├── json/                      JSON 实现
│   ├── JsonInterfaceHelper    JSON 入口（继承 HttpInterface）
│   └── JsonObjectMapperImpl   基于 Jackson 的 JSON DataObjectMapper
├── xml/                       XML 实现
│   ├── XmlInterfaceHelper     XML 入口
│   └── XmlObjectMapperImpl    基于 Jackson XML 的 DataObjectMapper
├── util/
│   ├── MediaTypes             常用 MediaType 常量
│   ├── BufferRequestBody       okio.Buffer → OkHttp RequestBody（零拷贝）
│   ├── HttpBasicAuthenticator  Basic 认证器（401 自动重试）
│   └── SSLContextUtils         信任全部证书（仅内网/测试）
├── exception/
│   ├── HttpRequestException   extends TaskPartnerException（网络/接口方错误）
│   └── DataMapperException    extends TaskDataException（序列化/数据错误）
└── uw.task.exception/         桥接副本（避免强依赖 uw-task，详见类 javadoc）
    ├── TaskDataException
    └── TaskPartnerException
```

#### 异常体系

本库所有异常均为 **RuntimeException（非受检）**，无需声明抛出，分两类，与 [uw-task](../uw-task) 调度框架的分类策略对齐：

```
RuntimeException
├── TaskDataException          数据/业务错误（内部原因）
│   └── DataMapperException    JSON/XML 序列化/反序列化失败
└── TaskPartnerException       接口方/网络错误（外部原因）
    └── HttpRequestException   连接超时、读写超时、SSL 握手失败、URL 非法等
```

- **`HttpRequestException`**：网络层错误（`IOException` 包裹），属"外部原因"，uw-task 通常会重试。
- **`DataMapperException`**：序列化/反序列化失败，属"内部数据原因"，uw-task 通常不重试（重试也是同样的数据错误）。
- `Processor` 抛出的 `DataMapperException` 会**直接冒泡**，不被本库吞掉或改写异常类型，交由上层（uw-task / 业务）按正确分类处理。

> `uw.task.exception` 包下的两个异常类是本库为避免强依赖 uw-task 而保留的**同名桥接副本**。运行在 uw-task 环境时，类加载器优先加载 uw-task 提供的同名类，异常分类语义生效；独立运行时回退到本地副本，仍可作为普通 RuntimeException 使用。

#### 环境要求

- **JDK 8+**
- **OkHttp 5.x（JVM 版）**：通过 `okhttp-jvm` artifact 引入（5.0+ 主 artifact 为空 jar，服务端须用 `okhttp-jvm`）
- **Jackson 2.x**（core / databind / annotations / dataformat-xml / datatype-jsr310）
- **okio 3.x**（随 OkHttp 传递，无需显式声明）



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
        // .defaultHeaders(defaultHeaderMap)   // 默认请求头，所有请求自动追加，业务同名头会覆盖
        // .cookieJar(cookieJar)               // Cookie 持久化（会话/登录态）
        // .addInterceptor(traceInterceptor)   // 应用拦截器（重试/重定向前介入）
        // .addNetworkInterceptor(netInterceptor) // 网络拦截器（重试/重定向后介入）
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
| defaultHeaders | `Map<String,String>` | 默认请求头，所有请求自动追加，业务侧同名头覆盖 |
| cookieJar | okhttp3.CookieJar | Cookie 持久化（会话/登录态），为 null 时 OkHttp 默认不持久化 |
| interceptors / networkInterceptors | `List<Interceptor>` | 应用 / 网络拦截器链，仅作用于本实例 |

> 每个 `HttpInterface` 实例使用**独立的 Dispatcher**，配置的 maxRequests / maxRequestsPerHost 仅作用于该实例，不会影响全局或其他实例。拦截器与 CookieJar 同理，仅作用于本实例派生出的 OkHttpClient。

##### 获取响应头与请求耗时

每次请求返回的 `HttpData`（`*ForData` 直接返回、`*ForEntity` 通过 `getHttpData()` 获取）携带完整的响应上下文：

| 方法 | 说明 |
|------|------|
| `getResponseHeaders()` | 完整响应头（`Map<String, List<String>>`，**大小写不敏感**，不可变快照；同名多值头如 `Set-Cookie` 用 List 表达） |
| `getResponseHeader(name)` | （`HttpDefaultData`）便捷取单个响应头首个值，大小写不敏感 |
| `getResponseMessage()` | HTTP 状态消息（reason phrase，如 "Not Found"） |
| `getElapsedMillis()` | 本次请求整体耗时（毫秒，基于 OkHttp `receivedResponseAtMillis - sentRequestAtMillis`，已含连接/重试/传输耗时） |
| `getRetryCount()` | 重试/重定向次数（除首次外的额外尝试次数，含连接失败重试与 follow-up）。`retryOnConnectionFailure=false` 时恒为 0；自动启用，无需配置 |
| `getStatusCode()` | HTTP 状态码 |

```java
HttpDefaultData data = http.getForData(url);
// 取全部响应头（大小写不敏感）
Map<String, List<String>> headers = data.getResponseHeaders();
String traceId = data.getResponseHeader("X-Trace-Id");
List<String> setCookies = data.getResponseHeaders().get("Set-Cookie");
long cost = data.getElapsedMillis();
```

> `responseHeaders` 参与 `HttpDefaultData.toString()` 的日志 JSON 序列化；如需排除可继承 `HttpDefaultData` 并在该字段加 `@JsonIgnore`。`responseBytes`（原始响应字节）则始终 `@JsonIgnore`，避免把二进制内容灌进日志。

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
    // 完整 Request 版本（含已合并的业务头/默认头、最终 URL），默认空实现，按需覆写
    // 注意：不含 OkHttp 网络层注入的 Host/Content-Length/Cookie 等，那些头只在网络拦截器里可见
    default void requestProcess(okhttp3.Request request) {}
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

**Q: 如何获取响应头（如 `X-Trace-Id`、`Set-Cookie`、`Location`）？**
A: 从 `HttpData` 取：`data.getResponseHeaders()` 返回大小写不敏感的 `Map<String, List<String>>`（同名多值头用 List 表达）；`HttpDefaultData` 还提供 `getResponseHeader(name)` 取单个首值。详见「获取响应头与请求耗时」章节。

**Q: `retryCount` 统计的是哪种重试？为什么 `retryOnConnectionFailure=false` 时也可能 >0？**
A: `retryCount = 物理网络请求次数 - 1`，由内部网络拦截器统计，**自动启用**。它包含两类：
1. 连接失败重试（仅 `retryOnConnectionFailure=true` 时发生）；
2. follow-up（重定向 301/302/307、401 认证挑战等），由 OkHttp 内部 `RetryAndFollowUpInterceptor` 处理，**与 `retryOnConnectionFailure` 无关**。

所以即便 `retryOnConnectionFailure=false`，只要发生了重定向跟随，`retryCount` 就会 >0。未配置 `HttpConfig`（走全局兜底 client）时 `retryCount` 恒为 0（该场景未注入计数拦截器）。

**Q: OkHttp 4.x 升级到 5.x 需要注意什么？**
A: 5.x 对 4.12 的稳定 API 保持**二进制兼容**，`okhttp3.*` 包名不变，下游代码零改动。唯一注意点：5.0+ 把单一 jar 拆成了 `okhttp-jvm`（服务端）和 `okhttp-android`，Maven Central 上的主 `okhttp` artifact 变为空 jar，**服务端必须用 `okhttp-jvm`**（本库已采用）。

**Q: 为什么 `uw-httpclient` 里会有 `uw.task.exception` 这个包？**
A: 这是避免强依赖 uw-task 的**同名桥接副本**。本库的 `DataMapperException` / `HttpRequestException` 分别继承 `TaskDataException` / `TaskPartnerException`，以便在 uw-task 环境被正确分类处理；但作为底层库不应强依赖 uw-task。运行时类加载器会优先加载 uw-task 的同名类，独立运行则回退到本地副本。详见对应类的 javadoc。

**Q: `HttpDataProcessor` 里两个 `requestProcess` 有什么区别？**
A: `requestProcess(String, Map, Map)` 只能拿到业务侧传入的原始参数（requestBody/formData/headers）；`requestProcess(okhttp3.Request)` 是默认空实现，覆写后可拿到**构建完成的 Request**（含已合并的业务头与 `defaultHeaders`、最终解析的 `HttpUrl`、method、body 引用）。两者都**不含** OkHttp 网络层注入的头（`Host`/`Content-Length`/`Connection`/`Cookie` 等），那些头只在**网络拦截器**里可见。
