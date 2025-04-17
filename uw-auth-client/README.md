# uw-auth-client

uw-auth验证客户端，主要用于内部RPC调用的鉴权。

## Maven引入

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-auth-client</artifactId>
    <version>${project.version}</version>
</dependency>
```

## 关键配置

```yaml
uw:
  auth:
    client:
      # 如果是Spring Cloud应用，则开启Spring Cloud支持，可以获取添加@LoadBalanced注解后的restTemplate：
      auth-center-host: http://localhost:9999
      username: username
      password: password
      http-pool:
        max-total: 1000
        default-max-per-route: 1000
        connect-timeout: 1000
        connection-request-timeout: 1000
        socket-timeout: 1000
        keep-alive-time-if-not-present: 0
```

## 使用RestTemplate

使用`@Qualifier("authRestTemplate")`来注入restTemplate：

```java
@Component
public class MyApp {
    @Autowired
    @Qualifier("authRestTemplate")
    private RestTemplate authRestTemplate;
    
    User me = restTemplate.getForObject("http://localhost:8080/protected/profile", User.class);
}
```
