#### 1.uw-httpclient用什么作为http客户端？它的优点是什么？

- 转为业务系统优化，高性能、低消耗。
- 统一接口，方便调用。
- 方便业务系统记录日志。

#### 2.JsonInterfaceHelper和XmlInterfaceHelper有什么作用，使用场景是什么？

Http接口的大部分实现均位于HttpInterface,而接口工具类JsonInterfaceHelper和XmlInterfaceHelper只是向它们提供HttpHelper和分别处理Json格式和Xml格式的ObjectMapper，即屏蔽了HttpInterface的实现，又实现了对代码的复用,方便重构和修改。根据请求响应的内容格式决定使用JsonInterfaceHelper还是XmlInterfaceHelper。

#### 3.HttpEntity类有什么作用？为什么使用它？

是返回结果的封装类，一些请求方法用它来作为返回类型。作为返回类，可以获取比较完整和方便的整个请求信息：响应的Response，请求的Request，响应内容ResponseBody，以及通过设置泛型对响应内容进行类型转换后的value。

#### 4.如果出现了MapperException异常，可能是因为什么原因？

类型转换时出现的报错，可能是响应内容的格式与HttpInterface实现类支持的格式不一致，或者选择了错误要转换成的类。

#### 5.HttpConfig中可以进行什么配置？

- 连接超时时间connectTimeout，读超时时间readTimeout，写超时时间writeTimeout。
- 连接池相关配置：包括最大空闲大小，keepAlive时长。
- 连接数相关配置：包括全局连接数，单主机连接数。
- 安全配置：SSLSocketFactory，X509证书管理器X509TrustManager，主机名验证器HostnameVerifier。
- 是否重试retryOnConnectionFailure。

#### 6.如果要请求一个返回结果是JSON类型的接口，请求超时时间设置为3秒，该如何获取HttpInterface？

```java
HttpInterface httpInterface =
                new JsonInterfaceHelper(
                        new HttpConfig.Builder().connectTimeout(3000).build());
```

#### 7.如何用uw-httpclient获取一个url为xxx远程请求的ProductVo对象?

```java
ProductVo product = httpInterface.getForEntity("xxx", ProductVo.class);
```

#### 8.HttpConfig的retryOnConnectionFailure属性有什么作用和影响？

retryOnConnectionFailure设置为true将会开启请求失败重试。如果你的业务是有严格幂等要求的,重试可能会出现严重问题(比如重复调用下单),应将此值配为false，让程序自己处理错误问题。