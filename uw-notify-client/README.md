[TOC]

## 简介

**uw-notify-client** 是 Web 通知推送客户端 SDK，通过 HTTP RPC 调用 notify-center 向指定用户或运营商推送通知，notify-center 再经 SSE（Server-Sent Events）等实时通道下发至前端。

- **Maven 坐标**：`com.umtone:uw-notify-client`
- **配置前缀**：`uw.notify`
- **依赖**：`uw-auth-client`（提供带鉴权拦截器的共享 `RestClient`）

## 工作原理

通过 HTTP RPC 调用 notify-center 的 `/rpc/notify/pushNotify` 接口推送消息。`NotifyClientHelper` 以静态工具方法对外暴露能力，启动时由 `UwNotifyAutoConfiguration` 通过依赖注入完成内部 `RestClient` 与配置的初始化。

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `uw.notify.notify-center-host` | `http://uw-notify-center` | notify-center 服务地址 |

## API 速查

> **包路径**：`uw.notify.client.NotifyClientHelper`（全部静态方法）

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `pushNotify(WebNotifyMsg webNotifyMsg)` | `ResponseData<Void>` | 推送 Web 通知 |

**返回语义**：`SUCCESS`=推送成功 / `ERROR`=推送失败。任何异常均被捕获并以 `ResponseData.errorMsg` 返回，不会抛出。

## WebNotifyMsg

> **包路径**：`uw.notify.client.vo.WebNotifyMsg`

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | `long` | 用户 ID，`0` 表示对该运营商下所有用户广播 |
| `saasId` | `long` | 运营商编号，`0` 表示对所有运营商广播 |
| `notifyBody` | `NotifyBody` | 通知内容 |

### NotifyBody（内部类）

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | `String` | 通知类型（业务自定义，如 `"SYSTEM"`） |
| `subject` | `String` | 消息标题 |
| `content` | `String` | 消息正文 |
| `data` | `Object` | 附加数据，类型任意 |

构造方式：`new WebNotifyMsg(userId, saasId, body)`；`NotifyBody` 支持 `new NotifyBody(type, data)` 或 `new NotifyBody(type, subject, content, data)` 或无参构造 + setter。

## 使用示例

```java
// 定向推送
WebNotifyMsg.NotifyBody body = new WebNotifyMsg.NotifyBody();
body.setType("SYSTEM");
body.setSubject("标题");
body.setContent("内容");
body.setData(Map.of("ts", System.currentTimeMillis()));
NotifyClientHelper.pushNotify(new WebNotifyMsg(userId, saasId, body));

// 全运营商广播
NotifyClientHelper.pushNotify(new WebNotifyMsg(0L, 0L, new WebNotifyMsg.NotifyBody("SYSTEM", data)));
```
