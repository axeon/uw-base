# uw-auth-client

内部 RPC 鉴权客户端。作为 Spring Boot Auto-Configuration Starter，自动管理 JWT Bearer Token 的获取、刷新与注入，支持
`RestTemplate` 和 `WebClient` 两种 HTTP 客户端。

---

## 目录

1. [Maven 引入](#maven-引入)
2. [配置项](#配置项)
3. [注入 Bean](#注入-bean)
4. [使用 RestTemplate](#使用-resttemplate)
5. [使用 WebClient](#使用-webclient)
6. [Spring Cloud 模式](#spring-cloud-模式)
7. [Token 生命周期](#token-生命周期)
8. [架构说明（供 AI 阅读）](#架构说明供-ai-阅读)

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

      # Apache HttpClient 5 连接池配置
      http-pool:
        max-total: 100000                    # 最大连接数
        default-max-per-route: 10000         # 每路由最大连接数
        connect-timeout: 30000               # 连接超时（ms）
        connection-request-timeout: 180000   # 从连接池获取连接超时（ms）
        socket-timeout: 30000                # 读取超时（ms）
        keep-alive-time-if-not-present: 0    # 服务端未返回 Keep-Alive 时的保活时间（ms），0 表示不保活
```

> `appName`、`appVersion`、`appHost`、`appPort` 由 `${project.name}`、`${project.version}`、
`${spring.cloud.nacos.discovery.ip:}`、`${server.port:8080}` 自动注入，用于构造 `loginAgent` 字段，无需手动配置。

---

## 注入 Bean

Auto-Configuration 注册的可用 Bean：

| Bean 名称 / 类型                                                | 说明                                                                               |
|-------------------------------------------------------------|----------------------------------------------------------------------------------|
| `authRestTemplate` (`RestTemplate`)                         | **主要使用**。每次请求自动注入 `Authorization: Bearer <token>`，401/498 时自动刷新后重试一次。`@Primary`。 |
| `authWebClient` (`WebClient`)                               | **主要使用**。响应式版本，功能与上同。`@Primary`。                                                 |
| `baseAuthClientRestTemplate` (`RestTemplate`)               | 内部使用，无鉴权拦截器，专用于向认证中心发起登录/刷新请求。                                                   |
| `authClientTokenService` (`AuthClientTokenService`)         | Token 管理器，可直接注入调用 `getToken()` 获取当前有效 Token。                                     |
| `authClientHttpRequestFactory` (`ClientHttpRequestFactory`) | Apache HttpClient 5 连接池工厂，被上述 RestTemplate 共用。                                   |

---

## 使用 RestTemplate

```java
@Component
public class MyService {

    @Autowired
    @Qualifier("authRestTemplate")
    private RestTemplate authRestTemplate;

    public UserInfo getUser(long userId) {
        return authRestTemplate.getForObject(
            "http://uw-user-center/user/{id}", UserInfo.class, userId);
    }
}
```

- 无需手动设置 `Authorization` 请求头。
- 收到 `401 Unauthorized` 或 `498 Token Expired` 时，会自动 invalidate 并重新获取 Token，然后重试本次请求一次。

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

- 同样自动注入 Token，收到 `401` 时自动重试一次。

---

## Spring Cloud 模式

通过 `uw.auth.client.enable-spring-cloud`（默认 `true`）控制：

| 模式         | Bean 特性                                                                                | 适用场景                    |
|------------|----------------------------------------------------------------------------------------|-------------------------|
| `true`（默认） | `authRestTemplate` 和 `baseAuthClientRestTemplate` 均加 `@LoadBalanced` 注解，支持 Nacos 服务名解析 | Spring Cloud / Nacos 环境 |
| `false`    | 不加 `@LoadBalanced`，使用直连 URL                                                            | 无 Spring Cloud 环境       |

> `AuthClientExcludeAutoConfigProcessor` 会在环境后处理阶段，将 `ClientHttpConnectorAutoConfiguration` 加入
`spring.autoconfigure.exclude`，避免 Spring Boot WebFlux 自动配置与本库的 WebClient 配置冲突。

---

## Token 生命周期

```
首次调用 getToken()
  └─ token == null && refreshToken == null  →  login()（用户名密码登录）
  └─ token == null && refreshToken != null  →  refresh()（刷新 Token）
  └─ token != null && 已过期（提前 5 分钟）  →  invalidate() + refresh()
  └─ token 有效  →  直接返回

最大重试次数：10 次，每次间隔 1 秒
login() / refresh() 均为 synchronized，防止并发重复登录
expiresAt = createAt + expiresIn - 300_000（提前 5 分钟过期）
```

**登录请求（`LoginRequest`）关键字段：**

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

**Token 刷新（POST form）关键字段：**

| 字段             | 值                    |
|----------------|----------------------|
| `refreshToken` | 上次登录返回的 refreshToken |
| `loginAgent`   | 同上                   |

---

## 架构说明（供 AI 阅读）

### 模块结构

```
uw.auth.client
├── conf/
│   ├── AuthClientAutoConfiguration     # 注册所有 Bean 的 @Configuration 类
│   ├── AuthClientExcludeAutoConfigProcessor  # EnvironmentPostProcessor，排除 WebFlux 冲突配置
│   └── AuthClientProperties            # @ConfigurationProperties(prefix="uw.auth.client")
├── constant/
│   ├── AuthClientConstants             # HTTP 状态码常量（200/401/402/403/426/498/503）
│   └── LoginType                       # 登录类型枚举（value/label），含分类工具方法
├── filter/
│   └── AuthTokenHeaderFilter           # WebClient ExchangeFilterFunction，注入 Token + 401 重试
├── service/
│   └── AuthClientTokenService           # Token 状态机：持有 token/refreshToken/expiresAt，login/refresh/invalidate
├── interceptor/
│   └── AuthTokenHeaderInterceptor      # RestTemplate ClientHttpRequestInterceptor，注入 Token + 401/498 重试
└── vo/
    ├── LoginRequest                    # 登录请求 VO
    └── TokenResponse                   # Token 响应 VO（含用户信息、token、refreshToken、expiresIn、createAt）
```

### 关键设计点

1. **双 RestTemplate**：`baseAuthClientRestTemplate`（无拦截器）专用于向认证中心通信，`authRestTemplate`（有拦截器）供业务方使用，避免循环依赖。

2. **自定义 HTTP 状态码 498**：认证中心使用 498 表示 Token 已过期（区别于标准 401 未携带 Token），拦截器/过滤器对两者均处理。

3. **Apache HttpClient 5**：底层使用 `httpclient5` + `PoolingHttpClientConnectionManager`，高并发场景下连接池参数需按实际
   RPC 规模调整。

4. **WebFlux 冲突规避**：引入 `spring-boot-starter-webflux` 但同时是 Servlet 应用时，
   `AuthClientExcludeAutoConfigProcessor` 自动排除 `ClientHttpConnectorAutoConfiguration`。

5. **LoginType 枚举**：序列化为 `{value, label}` JSON 对象（`@JsonFormat(shape=OBJECT)`），提供 `isInputType`/`isPassType`/
   `isCodeType`/`isOAuthType` 等分类判断方法。
