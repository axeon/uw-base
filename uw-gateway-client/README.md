[TOC]

## 简介

**uw-gateway-client** 是网关管理客户端 SDK，用于管理运营商（SAAS）的网关限速策略。

- **Maven 坐标**：`com.umtone:uw-gateway-client`
- **配置前缀**：`uw.gateway`
- **依赖**：`uw-auth-client`（提供带鉴权拦截器的共享 `RestClient`）

## 工作原理

通过 HTTP RPC 调用 gateway-center 的 `/rpc/service/*` 接口设置或清除限速。`GatewayClientHelper` 以静态工具方法对外暴露能力，启动时由 `UwGatewayAutoConfiguration` 通过依赖注入完成内部 `RestClient` 与配置的初始化。

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `uw.gateway.gateway-center-host` | `http://uw-gateway-center` | gateway-center 服务地址 |

## API 速查

> **包路径**：`uw.gateway.client.GatewayClientHelper`（全部静态方法）

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `updateSaasRateLimit(saasId, limitSeconds, limitRequests, limitBytes, expireDate, remark)` | `ResponseData<Void>` | 设置运营商限速 |
| `clearSaasRateLimit(saasId, remark)` | `ResponseData<Void>` | 清除运营商限速 |

**参数说明**（`updateSaasRateLimit`）：

| 参数 | 类型 | 说明 |
|------|------|------|
| `saasId` | `long` | 运营商 ID |
| `limitSeconds` | `int` | 限速统计窗口（秒） |
| `limitRequests` | `int` | 窗口内最大请求数 |
| `limitBytes` | `int` | 窗口内最大字节数 |
| `expireDate` | `Date` | 过期时间（必填，不可为 null） |
| `remark` | `String` | 备注信息 |

**返回语义**：`SUCCESS`=操作成功 / `ERROR`=操作失败。任何异常均被捕获并以 `ResponseData.errorMsg` 返回，不会抛出。

## 使用示例

```java
// 设置限速：每秒 100 请求、1MB，1年后过期
GatewayClientHelper.updateSaasRateLimit(saasId, 1, 100, 1024 * 1024,
    new Date(System.currentTimeMillis() + 365L * 24 * 3600 * 1000), "防止接口滥用");

// 设置带过期的限速（1小时）
GatewayClientHelper.updateSaasRateLimit(saasId, 60, 1000, 0,
    new Date(System.currentTimeMillis() + 3600_000L), "临时限速1小时");

// 清除限速
GatewayClientHelper.clearSaasRateLimit(saasId, "恢复正常访问");
```
