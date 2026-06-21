[TOC]

## 简介

**uw-tinyurl-client** 是短链接生成客户端 SDK，支持密语保护与过期时间。

- **Maven 坐标**：`com.umtone:uw-tinyurl-client`
- **配置前缀**：`uw.tinyurl`
- **依赖**：`uw-auth-client`（提供带鉴权拦截器的共享 `RestClient`）

## 工作原理

通过 HTTP RPC 调用 tinyurl-center 的 `/rpc/tinyurl/generate` 接口生成长链接对应的短链码。`TinyurlClientHelper` 以静态工具方法对外暴露能力，启动时由 `UwTinyurlAutoConfiguration` 通过依赖注入完成内部 `RestClient` 与配置的初始化。

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `uw.tinyurl.tinyurl-center-host` | `http://uw-tinyurl-center` | tinyurl-center 服务地址 |

## API 速查

> **包路径**：`uw.tinyurl.client.TinyurlClientHelper`（全部静态方法）

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `generate(TinyurlParam param)` | `ResponseData<String>` | 生成短链，`data` 为短链码 |

**返回语义**：`SUCCESS`=生成成功，`data` 为短链码 / `ERROR`=生成失败。任何异常均被捕获并以 `ResponseData.errorMsg` 返回，不会抛出。

## TinyurlParam

> **包路径**：`uw.tinyurl.client.vo.TinyurlParam`（推荐 Builder 模式）

| 字段 | 类型 | 说明 |
|------|------|------|
| `saasId` | `long` | 运营商 ID |
| `objectType` | `String` | 对象类型（分类统计用，如 `"LINK"`） |
| `objectId` | `long` | 对象 ID |
| `url` | `String` | 原始长 URL（必填） |
| `secretTips` | `String` | 密语提示 |
| `secretData` | `String` | 密语（访问时需输入） |
| `expireDate` | `Date` | 过期时间，`null` 表示永不过期 |

## 使用示例

```java
// 普通短链
TinyurlParam param = TinyurlParam.builder()
    .saasId(saasId).objectType("LINK").url(longUrl).build();
ResponseData<String> resp = TinyurlClientHelper.generate(param);
String code = resp.isSuccess() ? resp.getData() : null;

// 带密语与过期时间
TinyurlParam secret = TinyurlParam.builder()
    .saasId(saasId).objectType("SECRET_LINK").url(longUrl)
    .secretTips("请输入访问密码").secretData("pwd123")
    .expireDate(new Date(System.currentTimeMillis() + 7L * 24 * 3600 * 1000))
    .build();
TinyurlClientHelper.generate(secret);
```
