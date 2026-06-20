# uw-auth-client

> 内部 RPC 鉴权客户端 —— Spring Boot Auto-Configuration Starter

为服务间调用自动管理 **JWT Bearer Token** 的获取、刷新与注入，业务方注入客户端即可发起带鉴权的 RPC，
无需手动处理登录、续期、401/498 重试。同时提供同步（`RestClient`）与响应式（`WebClient`）两套客户端，
RestClient 路径底层基于 **Apache HttpClient 5** 连接池。

---

## 特性

- **自动鉴权**：每次请求自动附加 `Authorization: Bearer <token>`，对业务零侵入。
- **自动续期**：Token 即将过期时提前刷新（默认提前 5 分钟）；收到 `401`/`498` 时自动作废并重试一次。
- **并发安全**：读写锁 + `volatile`，Token 有效期读锁并发零阻塞，刷新在写锁内串行、靠 double-check 避免重复登录。
- **双客户端**：`RestClient`（同步）与 `WebClient`（响应式）。
- **双部署形态**：支持 Spring Cloud（`@LoadBalanced` + Nacos 服务名）与直连两种模式，由配置切换。
- **安全脱敏**：`TokenResponse.toString()` 对凭证脱敏，失败日志只打印 `code/msg`，不泄漏到日志文件。

---

## 目录

1. [Maven 引入](#maven-引入)
2. [依赖与环境要求](#依赖与环境要求)
3. [配置项](#配置项)
4. [注入 Bean](#注入-bean)
5. [使用 RestClient](#使用-restclient)
6. [使用 WebClient](#使用-webclient)
7. [Spring Cloud 模式](#spring-cloud-模式)
8. [Token 生命周期](#token-生命周期)
9. [错误处理与重试](#错误处理与重试)
10. [FAQ / 注意事项](#faq--注意事项)
11. [架构说明（供 AI 阅读）](#架构说明供-ai-阅读)

---

## Maven 引入

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-auth-client</artifactId>
    <version>${uw-auth-client.version}</version>
</dependency>
```

---

## 依赖与环境要求

| 依赖                         | 要求                        | 说明                                              |
|----------------------------|---------------------------|-------------------------------------------------|
| JDK                        | 25+                       | 与 uw-base 对齐                                   |
| Spring Boot                | 3.5.x                     | 使用 `@AutoConfiguration` 与 `RestClient`         |
| Spring Cloud（可选）           | 2025.x（如 2025.0.3）        | 仅当 `enable-spring-cloud=true`（默认）时需要，提供服务名解析   |
| Apache HttpClient 5        | 随 Boot 版本                 | RestClient 路径的底层连接池                             |
| uw-common                 | 同基线版本                     | 提供 `ResponseData` / `SystemClock`              |

> 本 Starter 引入了 `spring-boot-starter-webflux` 以提供 `WebClient`。在 Servlet 宿主应用中，
> `AuthClientExcludeAutoConfigProcessor` 会自动排除 `ClientHttpConnectorAutoConfiguration`，
> 避免与 Spring Boot 的响应式连接器自动配置冲突（详见 [Spring Cloud 模式](#spring-cloud-模式)）。

---

## 配置项

配置前缀：`uw.auth.client`

```yaml
uw:
  auth:
    client:
      # 认证中心地址，默认 http://uw-auth-center（Spring Cloud 服务名）
      auth-center-host: http://uw-auth-center

      # 登录接口路径，默认 /auth/login
      login-entry-point: /auth/login

      # 刷新 Token 接口路径，默认 /auth/refreshToken
      refresh-entry-point: /auth/refreshToken

      # 登录账号（loginId）
      login-id: your-service-account

      # 登录密码（明文）
      login-pass: your-password

      # 登录密钥（加密密码，优先于 login-pass）
      login-secret:

      # 用户类型，默认 10（RPC 服务账号）
      user-type: 10

      # SaaS ID，默认 0
      saas-id: 0

      # 是否启用 Spring Cloud（LoadBalanced），默认 true
      enable-spring-cloud: true

      # Token 提前刷新缓冲（ms），默认 300000（提前 5 分钟判定过期）
      refresh-advance-millis: 300000

      # Apache HttpClient 5 连接池配置（仅作用于 RestClient 路径）
      http-pool:
        max-total: 3000                       # 最大连接数
        default-max-per-route: 300            # 每路由最大连接数
        connect-timeout: 30000                # 连接超时（ms）
        connection-request-timeout: 180000    # 从连接池获取连接超时（ms）
        socket-timeout: 30000                 # 读取超时（ms）
        keep-alive-time-if-not-present: 0     # 服务端未返回 Keep-Alive 时的保活时间（ms），0 表示不保活
```

### 属性一览

| 属性                                     | 类型      | 默认值                    | 说明                                                                 |
|----------------------------------------|---------|------------------------|--------------------------------------------------------------------|
| `auth-center-host`                     | String  | `http://uw-auth-center` | 认证中心地址。Cloud 模式为服务名；直连模式为完整 URL                                   |
| `login-entry-point`                    | String  | `/auth/login`          | 登录接口路径                                                             |
| `refresh-entry-point`                  | String  | `/auth/refreshToken`   | 刷新 Token 接口路径                                                      |
| `login-id`                             | String  | -                      | 登录账号                                                               |
| `login-pass`                           | String  | -                      | 登录明文密码                                                             |
| `login-secret`                         | String  | -                      | 登录加密密码，非空时优先于 `login-pass`                                         |
| `user-type`                            | int     | `10`                   | 用户类型，10 = RPC 服务账号                                                 |
| `saas-id`                              | long    | `0`                    | SaaS ID                                                            |
| `enable-spring-cloud`                  | boolean | `true`                 | 是否为 Builder 标注 `@LoadBalanced`，启用服务名解析                             |
| `refresh-advance-millis`               | long    | `300000`               | Token 提前刷新缓冲（ms）                                                   |
| `http-pool.max-total`                  | int     | `3000`                 | 连接池最大连接数                                                           |
| `http-pool.default-max-per-route`      | int     | `300`                  | 每路由最大连接数                                                           |
| `http-pool.connect-timeout`            | int     | `30000`                | 连接超时（ms）                                                           |
| `http-pool.connection-request-timeout` | int     | `180000`               | 从连接池获取连接超时（ms）                                                     |
| `http-pool.socket-timeout`             | int     | `30000`                | 读取超时（ms）                                                           |
| `http-pool.keep-alive-time-if-not-present` | int     | `0`                    | 服务端未返回 Keep-Alive 时的保活时间（ms），0 不保活                                 |

> `appName`、`appVersion`、`appHost`、`appPort` 由 `${project.name}`、`${project.version}`、
> `${spring.cloud.nacos.discovery.ip:}`、`${server.port:8080}` 自动注入，用于构造 `loginAgent` 字段，
> 均带默认值（缺失不会导致启动失败），无需手动配置。

---

## 注入 Bean

Auto-Configuration 注册的可用 Bean：

| Bean 名称 / 类型                                              | 说明                                                                 |
|------------------------------------------------------------|--------------------------------------------------------------------|
| `authRestClient` (`RestClient`)                            | **主要使用**。每次请求自动注入 `Authorization: Bearer <token>`，401/498 时自动刷新后重试一次。`@Primary`。 |
| `authWebClient` (`WebClient`)                              | **主要使用**。响应式版本，功能同上。`@Primary`。                                     |
| `baseAuthClientRestClient` (`RestClient`)                  | 内部使用，无鉴权拦截器，专用于向认证中心发起登录/刷新请求。                                     |
| `authClientTokenService` (`AuthClientTokenService`)        | Token 管理器，可直接注入调用 `getToken()` 获取当前有效 Token。                       |
| `authClientHttpRequestFactory` (`ClientHttpRequestFactory`) | Apache HttpClient 5 连接池工厂，被上述 RestClient 共用。                    |

> 内部 Builder Bean：`baseAuthClientRestClientBuilder`、`authRestClientBuilder`、`authWebClientBuilder`，
> 根据是否启用 Spring Cloud 自动标注 `@LoadBalanced`。若需自定义，可在宿主工程覆盖同名 Builder Bean。

---

## 使用 RestClient

```java
@Component
public class MyService {

    @Autowired
    @Qualifier("authRestClient")
    private RestClient authRestClient;

    public UserInfo getUser(long userId) {
        return authRestClient.get()
            .uri("http://uw-user-center/user/{id}", userId)
            .retrieve()
            .body(UserInfo.class);
    }
}
```

- 无需手动设置 `Authorization` 请求头。
- 收到 `401 Unauthorized` 或 `498 Token Expired` 时，会自动 invalidate 并重新获取 Token，然后重试本次请求一次。
- 由于 `authRestClient` 标记为 `@Primary`，按类型注入 `RestClient` 也会命中它。

---

## 使用 WebClient

```java
@Component
public class MyReactiveService {

    @Autowired
    @Qualifier("authWebClient")
    private WebClient authWebClient;

    public Mono<UserInfo> getUser(long userId) {
        return authWebClient.get()
            .uri("http://uw-user-center/user/{id}", userId)
            .retrieve()
            .bodyToMono(UserInfo.class);
    }
}
```

- 同样自动注入 Token，收到 `401`/`498` 时自动重试一次。
- Token 获取延迟到订阅时（`Mono.defer`），不在组装阶段阻塞调度线程。
- 注意：WebClient 底层使用 Reactor Netty 响应式连接器，与 RestClient 的 Apache HttpClient5 连接池相互独立，
  `http-pool` 配置不影响 WebClient。如需自定义响应式连接行为，请覆盖 `authWebClientBuilder` 或提供 `ClientHttpConnector`。

---

## Spring Cloud 模式

通过 `uw.auth.client.enable-spring-cloud`（默认 `true`）控制：

| 模式            | Builder 行为                                                                 | 适用场景                  |
|---------------|----------------------------------------------------------------------------|-----------------------|
| `true`（默认）    | `baseAuthClientRestClientBuilder` / `authRestClientBuilder` / `authWebClientBuilder` 均加 `@LoadBalanced`，支持 Nacos 服务名解析 | Spring Cloud / Nacos 环境 |
| `false`       | 不加 `@LoadBalanced`，使用直连 URL，需配置完整 host（`auth-center-host`）                  | 无 Spring Cloud 环境     |

> 实现说明：因 `@LoadBalanced` 是声明式注解、无法在运行时按条件去除，本库通过内嵌的
> `CloudLoadBalancedConfig` / `PlainBuilderConfig` 两个配置类（带 `@ConditionalOnProperty`）
> 向同名 Builder Bean 槽贡献实例。Spring Cloud 的 BPP 通过 `findAnnotationOnBean` 按 Bean 名识别注解，
> 与定义所在的配置类无关，因此该写法可被正确增强。

> **WebFlux 冲突规避**：`AuthClientExcludeAutoConfigProcessor`（通过 `META-INF/spring.factories`
> 注册为 `EnvironmentPostProcessor`）会在 bootstrap 早期将 `ClientHttpConnectorAutoConfiguration`
> 加入 `spring.autoconfigure.exclude`，避免 Spring Boot WebFlux 自动配置与本库的 WebClient 配置冲突。
> 它会合并宿主已配置的 exclude 并去重，不会覆盖宿主既有配置。

---

## Token 生命周期

```
调用 getToken()
  └─ token 有效（非空 && 未过期）            →  直接返回（读锁并发）
  └─ token == null && refreshToken == null →  login()（用户名密码登录）
  └─ token == null && refreshToken != null →  refresh()（刷新 Token）
  └─ token != null && 已过期                 →  作废后 refresh()

最大重试次数：10 次，每次间隔 1 秒
login() / refresh() 均在写锁内串行执行，防止并发重复登录
expiresAt = createAt + expiresIn - refreshAdvanceMillis
```

### 并发模型

使用 `ReentrantReadWriteLock`：

- **Token 有效**：走读锁，多线程并发返回，零阻塞。
- **Token 失效/过期**：升级为写锁。多个线程同时发现失效时会排队抢写锁，但**只有第一个线程执行网络刷新**；
  其余线程拿到写锁后通过 double-check 直接复用已刷新的新 Token，不会重复登录。
- 字段以 `volatile` 修饰，读写锁互斥保证可见性。

### 登录请求（`LoginRequest`）关键字段

| 字段            | 值                                            |
|---------------|----------------------------------------------|
| `loginType`   | `LoginType.USER_PASS` (1)                    |
| `loginId`     | 配置的 `login-id`                               |
| `loginPass`   | 配置的 `login-pass`                             |
| `loginSecret` | 配置的 `login-secret`（优先）                       |
| `userType`    | 配置的 `user-type`（默认 10）                       |
| `saasId`      | 配置的 `saas-id`（默认 0）                          |
| `loginAgent`  | `{appName}:{appVersion}/{appHost}:{appPort}` |
| `forceLogin`  | `true`                                       |

### Token 刷新（POST form）关键字段

| 字段             | 值                    |
|----------------|----------------------|
| `refreshToken` | 上次登录返回的 refreshToken |
| `loginAgent`   | 同上                   |

---

## 错误处理与重试

- **Token 获取重试**：`getToken()` 在写锁内最多重试 10 次，每次间隔 1 秒，避免认证中心不可用时密集请求。
  连续失败累计每达 10 次输出一次告警日志（`!!!AuthClient获取token出错已超过N次，请检查配置！`）。
- **请求级重试**：业务请求收到 `401`/`498` 时，拦截器/过滤器会作废 Token、重新获取并**重试本次请求一次**（非无限重试）。
  若重试后仍失败，按原始错误向上抛出。
- **失败降级**：`login()` / `refresh()` 失败会清空 `refreshToken` 和 `expiresAt`，下次 `getToken()` 将触发全量 login。
- **日志安全**：登录/刷新失败日志仅打印响应的 `code/msg`；`TokenResponse.toString()` 对 `token` / `refreshToken` 脱敏
  （保留前 4 后 4），凭证不会泄漏到日志文件。

---

## FAQ / 注意事项

**Q：直连模式下 `auth-center-host` 怎么配？**
A：关闭 `enable-spring-cloud`（设为 `false`），并将 `auth-center-host` 配置为完整地址，如 `http://10.0.0.5:8080`。

**Q：为什么 WebClient 不受 `http-pool` 控制？**
A：RestClient 用的是 Apache HttpClient5 的同步 `CloseableHttpClient`，而 WebClient 底层是 Reactor Netty 的响应式连接器，
两者连接池相互独立，无法共享。如需自定义 WebClient 连接行为，请覆盖 `authWebClientBuilder` 或提供 `ClientHttpConnector` Bean。

**Q：`LoginType` 反序列化要注意什么？**
A：`LoginType` 标注了 `@JsonFormat(shape = OBJECT)`，序列化为 `{"value":1,"label":"..."}`。该注解下 Jackson 反序列化
**仅支持对象形态**（`{"value":1}`），裸数字或字符串无法直接反序列化。本模块仅序列化输出 `LoginType`，反序列化由消费端负责；
若消费端需要按 value 反序列化，请自行注册对应的反序列化器。

**Q：启动时报 `project.name` / `project.version` 找不到？**
A：这两个属性带有默认值（`unknown`），正常不会报错。若仍报错，通常是宿主工程的 `spring-boot-starter-parent`
未正确传递 `project.*` 属性，可在 `application.yml` 中显式补充 `project.name` / `project.version`。

---

## 架构说明（供 AI 阅读）

### 模块结构

```
uw.auth.client
├── conf/
│   ├── AuthClientAutoConfiguration     # @AutoConfiguration，注册所有 Bean；内嵌 CloudLoadBalancedConfig / PlainBuilderConfig
│   ├── AuthClientExcludeAutoConfigProcessor  # EnvironmentPostProcessor（spring.factories 注册），排除 WebFlux 冲突配置
│   └── AuthClientProperties            # @ConfigurationProperties(prefix="uw.auth.client")
├── constant/
│   ├── AuthClientConstants             # HTTP 状态码常量（200/401/402/403/426/498/503）
│   └── LoginType                       # 登录类型枚举（value/label），含分类工具方法
├── filter/
│   └── AuthTokenHeaderFilter           # WebClient ExchangeFilterFunction，注入 Token + 401/498 重试（Mono.defer 延迟取 token）
├── service/
│   └── AuthClientTokenService          # Token 状态机：读写锁 + volatile，login/refresh/invalidate
├── interceptor/
│   └── AuthTokenHeaderInterceptor      # RestClient ClientHttpRequestInterceptor，注入 Token + 401/498 重试
└── vo/
    ├── LoginRequest                    # 登录请求 VO
    └── TokenResponse                   # Token 响应 VO（toString 对 token 脱敏）
```

### 关键设计点

1. **双 RestClient**：`baseAuthClientRestClient`（无拦截器）专用于向认证中心通信，`authRestClient`（有拦截器）供业务方使用，
   避免 `login → getToken → login` 循环依赖。

2. **自定义 HTTP 状态码 498**：认证中心使用 498 表示 Token 已过期（区别于标准 401 未携带 Token），拦截器/过滤器对两者均处理。

3. **Apache HttpClient 5**：底层使用 `httpclient5` + `PoolingHttpClientConnectionManager`，高并发场景下连接池参数需按实际 RPC 规模调整。

4. **WebFlux 冲突规避**：引入 `spring-boot-starter-webflux` 但同时是 Servlet 应用时，
   `AuthClientExcludeAutoConfigProcessor` 自动排除 `ClientHttpConnectorAutoConfiguration`。

5. **Spring Cloud 开关**：`enable-spring-cloud` 通过 `@ConditionalOnProperty` 在两个内嵌配置类间切换 Builder 是否标注
   `@LoadBalanced`，支持无 Spring Cloud 环境的直连模式。

6. **LoginType 枚举**：序列化为 `{value, label}` JSON 对象（`@JsonFormat(shape=OBJECT)`）。该注解下 Jackson 反序列化仅支持对象形态，
   本模块仅序列化输出，反序列化由消费端负责。提供 `isInputType`/`isPassType`/`isCodeType`/`isOAuthType` 等分类判断方法。
