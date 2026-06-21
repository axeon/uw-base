# uw-gateway-client

网关管理客户端 SDK。供业务方（如运营商计费、风控系统）通过 RPC 管理运营商（SAAS）的网关限速策略。

- **Maven 坐标**：`com.umtone:uw-gateway-client`
- **配置前缀**：`uw.gateway`
- **依赖**：`uw-auth-client`（提供带鉴权拦截器的共享 `RestClient`）

## 工作原理

通过 HTTP RPC 调用 [uw-gateway-center](../../uw-gateway-center) 的 `/rpc/service/*` 接口设置或清除限速。`GatewayClientHelper` 以**静态工具方法**对外暴露能力；启动时由 `UwGatewayAutoConfiguration` 通过依赖注入完成内部 `RestClient` 与配置的初始化，业务方无需持有 Bean 实例即可调用。

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `uw.gateway.gateway-center-host` | `http://uw-gateway-center` | gateway-center 服务地址 |

## API 速查

> **包路径**：`uw.gateway.client.GatewayClientHelper`（全部静态方法）

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `updateSaasRateLimit(SaasRateLimitParam)` | `ResponseData<Void>` | 设置运营商限速 |
| `clearSaasRateLimit(saasId, remark)` | `ResponseData<Void>` | 清除运营商限速 |

### SaasRateLimitParam 字段

通过 `SaasRateLimitParam.builder()` 链式构造（包路径 `uw.gateway.client.vo.SaasRateLimitParam`）：

| 字段 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `saasId` | `long` | — | 运营商 ID（必填，>0） |
| `limitType` | `int` | — | 限速类型，对应 `MscAclRateLimitType`（必填） |
| `userType` | `int` | `-1` | 仅按用户维度限速时使用 |
| `userId` | `long` | `-1` | 仅按用户 ID 维度限速时使用 |
| `limitSeconds` | `int` | — | 限速统计窗口（秒） |
| `limitRequests` | `int` | — | 窗口内最大请求数 |
| `limitBytes` | `int` | — | 窗口内最大字节数 |
| `expireDate` | `Date` | — | 过期时间（必填，晚于当前） |
| `remark` | `String` | — | 备注（必填） |

**limitType 合法值**（对应 `MscAclRateLimitType`）：`-1` 不限速 / `0` IP / `1` SAAS_LEVEL / `2` USER_TYPE / `3` USER_ID / `11` SAAS_LEVEL_URI / `12` USER_TYPE_URI / `13` USER_ID_URI。

**返回语义**：`SUCCESS`=操作成功 / `ERROR`=操作失败。任何异常均被捕获并以 `ResponseData.errorMsg` 返回，不会抛出。业务级校验（limitType 合法性、用户维度字段等）由 gateway-center 侧统一执行。

## 使用示例

```java
// 按 IP 限速：每 10 秒 1000 请求、500KB，1 年后过期
GatewayClientHelper.updateSaasRateLimit(
    SaasRateLimitParam.builder()
        .saasId(saasId)
        .limitType(0)        // 0=IP限速（裸值，对应服务端 MscAclRateLimitType.IP）
        .limitSeconds(10)
        .limitRequests(1000)
        .limitBytes(500_000)
        .expireDate(new Date(System.currentTimeMillis() + 365L * 24 * 3600 * 1000))
        .remark("防止接口滥用")
        .build());

// 按用户 ID 限速：针对特定用户，1 小时
GatewayClientHelper.updateSaasRateLimit(
    SaasRateLimitParam.builder()
        .saasId(saasId)
        .limitType(3)        // 3=USER_ID限速（对应服务端 MscAclRateLimitType.USER_ID）
        .userType(11)        // 用户类型，按业务侧 UserType 取值
        .userId(targetUserId)
        .limitSeconds(60)
        .limitRequests(100)
        .expireDate(new Date(System.currentTimeMillis() + 3600_000L))
        .remark("临时限速1小时")
        .build());

// 清除限速
GatewayClientHelper.clearSaasRateLimit(saasId, "恢复正常访问");
```

## 相关模块

- [uw-gateway-center](../../uw-gateway-center)：消费本 SDK 调用的服务端，提供 `/rpc/service/*` 接口。
- [uw-gateway](../../uw-gateway)：实际执行限速的网关本体。
