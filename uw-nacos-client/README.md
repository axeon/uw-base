[TOC]

## 简介

**uw-nacos-client** 是 fork 自 Spring Cloud Alibaba（SCA）2025.1.x 的 Nacos 客户端 starter，**彻底脱离 `com.alibaba.cloud` 依赖**，仅适配 Spring Cloud 官方抽象。提供三大能力：

- **配置中心**：通过 `spring.config.import` 拉取远端配置，支持动态刷新
- **服务注册**：应用启动自动注册到 Nacos，关闭自动摘除
- **服务发现**：阻塞式 + 响应式（WebFlux/gateway）双客户端，配合 LoadBalancer 即可负载均衡调用

- **Maven 坐标**：`com.umtone:uw-nacos-client`
- **配置前缀**：`spring.cloud.nacos`（与 SCA 完全一致，迁移零成本）
- **基线版本**：Spring Boot 4.x / Spring Cloud 2025.1.x / nacos-client 3.2.2
- **加载方式**：ConfigData（`spring.config.import`），不支持 bootstrap 模式

## 引用方法

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-nacos-client</artifactId>
</dependency>
```

版本由 `uw-base` 父 BOM 统一管理，无需指定。模块会自动传递 Spring Cloud 与 nacos-client 依赖；WebFlux 应用需自行引入 `spring-boot-starter-webflux`（响应式发现依赖的 reactor-core 由其自带）。

## 标准配置文件

一份生产环境可直接使用的 `application.yml`，可以通过环境变量系统运行期间动态加载数值。

```yaml
project:
  name: @project.name@
  version: @project.version@

spring:
  application:
    name: ${project.name}
  config:
    import:
      - "optional:nacos:${spring.application.name}"
  cloud:
    nacos:
      server-addr: ${NACOS_SERVER}
      username: ${NACOS_USERNAME}
      password: ${NACOS_PASSWORD}
      config:
        namespace: ${NACOS_NAMESPACE}
        file-extension: yaml
      discovery:
        namespace: ${spring.cloud.nacos.config.namespace}
        service: ${spring.application.name}
        ip: ${APP_HOST:}
        register-enabled: true
```

### `spring.config.import` 的 `nacos:` URI 语法

```
[optional:]nacos:<dataId>[?group=<group>&refreshEnabled=<true|false>&preference=<local|remote>]
```

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `dataId`（URI 路径） | 必填 | 配置 dataId；后缀决定解析方式（`properties`/`yaml`/`yml`/`xml`/`json`） |
| `group` | `spring.cloud.nacos.config.group` | 配置分组 |
| `refreshEnabled` | `spring.cloud.nacos.config.refresh-enabled` | 该 dataId 是否参与动态刷新 |
| `preference` | `local` | `remote` 时远端配置优先级高于本地同名配置 |
| `optional:` 前缀 | — | 配置缺失时不阻断启动；不加则缺失即报错 |

- `server-addr` / `username` / `password` 等连接信息统一在 `spring.cloud.nacos` 下声明，无需在 URI 重复。
- 多份配置按 `spring.config.import` 列表顺序加载，后加载者优先级更高（Spring Boot 标准行为）。

## 工作原理

### 配置加载

应用启动时，`spring.config.import` 中的每条 `nacos:` 会被解析为一次远端拉取：按 dataId 后缀选择解析器（properties/yaml/xml/json），解析后注入 Spring `Environment`，与本地 `application.yml` 合并。

### 动态刷新

配置在 Nacos 控制台被修改后，本模块监听到变更，直接发布 Spring Cloud 标准的 `RefreshEvent`，由 `ContextRefresher` 触发配置重新加载：

- `@ConfigurationProperties` Bean 全量重新绑定
- `@RefreshScope` Bean 在下次访问时重建

刷新链路完全走 Spring Cloud 标准机制，未引入额外刷新引擎。某 dataId 是否参与刷新由其 `refreshEnabled` 控制。

### 服务注册与发现

- **注册**：Web 服务启动后自动将本实例（IP/端口/元数据）注册到 Nacos，应用关闭时自动摘除。通过 `spring.cloud.nacos.discovery.register-enabled=false` 可关闭注册（仅订阅）。
- **发现**：阻塞式应用用 `DiscoveryClient`，WebFlux/gateway 用响应式客户端（阻塞调用调度到独立线程池，不阻塞事件循环）。配合 Spring Cloud LoadBalancer，即可对 `@LoadBalanced` 的 `RestClient` / `WebClient` / `RestTemplate` 按服务名负载均衡调用。

## 配置项参考

### 公共前缀 `spring.cloud.nacos`

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server-addr` | `127.0.0.1:8848` | Nacos Server 地址，config/discovery 共用 |
| `username` | - | 鉴权用户名 |
| `password` | - | 鉴权密码 |

### 配置中心 `spring.cloud.nacos.config`

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `enabled` | `true` | 是否启用配置中心 |
| `namespace` | （不设=public） | 命名空间 |
| `group` | `DEFAULT_GROUP` | 默认分组（`spring.config.import` 未指定 group 时用） |
| `file-extension` | `properties` | 默认后缀（dataId 无后缀时用） |
| `refresh-enabled` | `true` | 动态刷新总开关 |
| `timeout` | `3000` | 拉取配置超时（ms） |
| `import-check.enabled` | `true` | 启动校验 `spring.config.import` 是否含 `nacos:`，缺失则启动失败并给出修复指引 |
| `access-key` / `secret-key` | - | 鉴权密钥（自建 Nacos 2.x 鉴权） |
| `endpoint` | - | 接入点域名（动态获取 server 地址） |

### 服务发现 `spring.cloud.nacos.discovery`

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `enabled` | `true` | 是否启用服务发现 |
| `namespace` | public | 命名空间 |
| `group` | `DEFAULT_GROUP` | 分组 |
| `service` | `${spring.application.name}` | 注册的服务名 |
| `ip` | 自动探测 | 注册 IP，未设置时按 `ip-type` 探测 |
| `port` | 自动探测 | 注册端口 |
| `ip-type` | `IPv4` | `IPv4` / `IPv6`，IPv6 无可用地址时回退 IPv4 |
| `network-interface` | - | 指定网卡名 |
| `weight` | `1` | 实例权重 |
| `cluster-name` | - | 集群名 |
| `metadata` | - | 扩展元数据（Map） |
| `register-enabled` | `true` | 是否注册本实例（false=只订阅） |
| `ephemeral` | `true` | 是否临时实例 |
| `secure` | `false` | 是否 HTTPS 服务 |
| `fail-fast` | `true` | 注册失败是否抛异常（false 仅记日志） |
| `heart-beat-interval` / `heart-beat-timeout` / `ip-delete-timeout` | - | 心跳参数（ms） |
| `naming-load-cache-at-start` | `false` | 启动时是否从本地缓存加载 naming 数据 |
| `graceful-shutdown-wait-time` | `10000` | 优雅停机等待（ms，先摘实例再排空请求） |

## 从 SCA 迁移

配置前缀保持 `spring.cloud.nacos` 不变，迁移步骤：

1. pom 去掉 `spring-cloud-starter-alibaba-nacos-config` / `-nacos-discovery`，换成 `uw-nacos-client`
2. `bootstrap.yml` 内容合并进 `application.yml`
3. 把原 `shared-configs` / `extension-configs` 改写为 `spring.config.import` 列多条 `nacos:`

相对 SCA 裁剪的能力（均为 SCA 可选特性，当前项目零使用，迁移零影响）：

| SCA 写法 | 替代方案 |
|----------|----------|
| `spring-cloud-starter-bootstrap`（bootstrap 模式） | ConfigData（`spring.config.import`） |
| `shared-configs` / `extension-configs` | `spring.config.import` 列多条 `nacos:` |
| `refresh-behavior=specific_bean`（按键精确刷新） | Spring Cloud 默认全量 rebind |
| `failure-tolerance-enabled`（发现容错降级） | Nacos 异常直接抛出，由 LB/调用方处理 |
| `@NacosConfigListener` 注解 | 统一走 `RefreshEvent` / `@RefreshScope` |
