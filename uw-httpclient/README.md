# uw-httpclient

#### 项目简介
uw-httpclient针对接口业务需求设计的一个HttpClient库。
当前使用okhttp作为底层实现，将来可以切换为AHC、httpclient5。

#### 项目特性

1. 完整支持get/post/put/delete各种方法；
2. 将请求、返回对象序列化集成到类库中，并可自由定义；
3. 支持JSON/XML格式接口；
4. 支持完整的http请求日志，并可定义日志类，方便接口日志记录；
5. 变通支持上传、下载文件。

#### 快速入门

##### 添加maven dependency

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-httpclient</artifactId>
    <version>${version}</version>
</dependency>
```

##### 常用入口
- JsonInterfaceHelper 基于JSON的接口帮助入口。

- XmlInterfaceHelper 基于XML的接口帮助入口。

两者初始化方法类似，本质都是通过构建特定参数的HttpInterface来实现。
下面以JsonInterfaceHelper为例，介绍完整构造方法。

```java
/**
 * 完整构造器。
 * @param httpConfig HttpConfig配置参数。
 * @param httpDataCls 指定HttpData实现类。
 * @param httpDataLogLevel HttpDataLog级别。
 * @param httpDataFilter HttpData数据处理器。
 */
public JsonInterfaceHelper(HttpConfig httpConfig, Class<? extends HttpData> httpDataCls, HttpDataLogLevel httpDataLogLevel, HttpDataProcessor httpDataFilter) {
    super( httpConfig, httpDataCls, httpDataLogLevel, httpDataFilter, JsonUtils, MediaTypes.JSON_UTF8 );
}
```

##### HttpConfig配置
HttpConfig提供了常用的配置参数，并支持通过Builder方式进行构建。
常用的参数说明如下：

```java
/**
 * 连接超时时间 - 毫秒。
 */
private final long connectTimeout;

/**
 * 读超时时间 - 毫秒。
 */
private final long readTimeout;

/**
 * 写超时时间 - 毫秒。
 */
private final long writeTimeout;

/**
 * 当一个连接失败时,配置此值可以进行重试。
 */
private final boolean retryOnConnectionFailure;

/**
 * 每主机最大并发请求数。
 */
private final int maxRequestsPerHost;

/**
 * 全局最大并发请求数。
 */
private final int maxRequests;

/**
 * 连接池最大空闲连接数。
 */
private final int maxIdleConnections;

/**
 * 连接池中空闲连接存活时间毫秒数。
 */
private final long keepAliveTimeout;
```
##### HttpData 日志记录
- uw-httpclient使用HttpData对象来记录请求->结果日志。可以记录包括请求地址，请求方法，请求时间，请求内容，返回状态码，返回结果等相关数据。
- 为了减少无异议的copy，开发者可以自定义HttpData类型的实现类直接作为日志类使用，类库提供了HttpDefaultData的默认实现。
- 为了兼容二进制数据和文本数据，HttpData中优先设置ResponseBytes字段，推荐当访问ResponseData的时候才转化为String类型，避免不必要的转换。
- HttpDataLogLevel可以控制日志记录的范围，可以设定是否记录RequestData，默认不记录请求数据。

##### HttpDataProcessor 数据处理器
- HttpDataFilter用于处理请求数据和返回数据。

- 在特定的场景下，请求数据和返回数据需要加解密，此时可以HttpDataFilter中进行处理，让代码更整洁。
- 另外，最重要的场景是：可以在processLog中处理日志发送。

##### 主要方法介绍
- getForData 用于get方法获取HttpData，同时也可以用于下载二进制数据，包括图片和文件。

- getForEntity 用于get方法获取HttpEntity，同时包含HttpData和序列化后的对象。
- postFormForData 用于post formData方法获取HttpData，同时也可以用于下载二进制数据，包括图片和文件。
- postBodyForData 用于post requestBody方法获取HttpData，同时也可以用于下载二进制数据，包括图片和文件。
- postFormForEntity 用于post formData方法获取HttpEntity，同时包含HttpData和序列化后的对象。
- postBodyForEntity 用于post requestBody方法获取HttpEntity，同时包含HttpData和序列化后的对象。
- putFormForData 用于put formData方法获取HttpData，同时也可以用于下载二进制数据，包括图片和文件。
- putBodyForData 用于put requestBody方法获取HttpData，同时也可以用于下载二进制数据，包括图片和文件。
- putFormForEntity 用于put formData方法获取HttpEntity，同时包含HttpData和序列化后的对象。
- putBodyForEntity 用于put requestBody方法获取HttpEntity，同时包含HttpData和序列化后的对象。
- deleteForData 用于delete方法获取HttpData，同时也可以用于下载二进制数据，包括图片和文件。
- deleteForEntity 用于delete方法获取HttpEntity，同时包含HttpData和序列化后的对象。

除此之外，还提供了使用okhttp request对象的基础方法。

- requestForData，可以传入自定义的request对象返回HttpData，来提高代码的灵活度，可以使用此方法来上传数据。
- requestForEntity，可以传入自定义的request对象返回HttpEntity，来提高代码的灵活度，可以使用此方法来上传数据。

如果还不能达成目的，类库还暴露了okHttpClient方法。

- getOkHttpClient，通过此方法，可以自由操作okHttpClient。

##### FAQ

Q: 多次new OkHttpClient会不会带来效率问题？

A: 不会，因为OkHttpClient 对Java 底层的Connection是共用的,并于OkHttpClient的静态代码块中加载，加之外层的ConnectionPool使用了一个专门线
程(cleanupRunnable)去自动判定连接是否达到池的存活时间(keepAliveDuration)和空闲连接(idleConnections)数量的连接进行清理，减少长时间对底层
Socket句柄的占用而带来的各种问题。

Q: ObjectMapper是否能处理不加注解的Vo类？

A: 目前uw-httpclient采用jackson作为json和xml的序列化和反序列化库,没有添加jackson支持的注解能否转换成功取决于jackson对json或者xml处理的
默认的规则,建议使用uw-code-center生成相关的Java Vo实体类。

Q: 如何处理内部自签名的ssl访问问题？
A: 初始化代码中加入以下代码：
.trustManager( SSLContextUtils.getTrustAllManager() )
.sslSocketFactory( SSLContextUtils.getTruestAllSocketFactory())
.hostnameVerifier( (host, session) -> true )