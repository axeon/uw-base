[TOC]

## 简介

**uw-mydb-client** 是 MyDB 数据库运维中心的客户端 SDK，用于动态分配运营商（SAAS）数据节点，实现分库分表路由。

- **Maven 坐标**：`com.umtone:uw-mydb-client`
- **配置前缀**：`uw.mydb`
- **依赖**：`uw-auth-client`（提供带鉴权拦截器的共享 `RestClient`）

## 工作原理

通过 HTTP RPC 调用 mydb-center 的 `/rpc/app/assignSaasNode` 接口，为指定运营商在指定配置组下分配或复用一个数据节点。`MydbClientHelper` 以静态工具方法对外暴露能力，启动时由 `UwMydbAutoConfiguration` 通过依赖注入完成内部 `RestClient` 与配置的初始化。

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `uw.mydb.mydb-center-host` | `http://uw-mydb-center` | mydb-center 服务地址 |

## API 速查

> **包路径**：`uw.mydb.client.MydbClientHelper`（全部静态方法）

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `assignSaasNode(Serializable saasId)` | `ResponseData<DataNode>` | 默认配置组，自动分配节点 |
| `assignSaasNode(Serializable saasId, String preferNode)` | `ResponseData<DataNode>` | 默认配置组，指定偏好节点 |
| `assignSaasNode(String configKey, Serializable saasId, String preferNode)` | `ResponseData<DataNode>` | 指定配置组与偏好节点 |

**返回语义**：`SUCCESS`=新建节点 / `WARN`=节点已存在（`data` 仍可用） / `ERROR`=分配失败。任何异常均被捕获并以 `ResponseData.errorMsg` 返回，不会抛出。

## DataNode

> **包路径**：`uw.mydb.client.vo.DataNode`

通过「集群 ID + 库名」唯一确定一个数据节点，形如 `"clusterId.database"`。

| 字段 | 类型 | 说明 |
|------|------|------|
| `clusterId` | `long` | MySQL 集群 ID |
| `database` | `String` | MySQL 库名 |

构造方式：
- `new DataNode(long clusterId, String database)`
- `new DataNode("clusterId.database")`（含格式校验，非法抛 `IllegalArgumentException`）
- `toString()` 返回 `"clusterId.database"`

## 使用示例

```java
// 自动分配节点
ResponseData<DataNode> response = MydbClientHelper.assignSaasNode(saasId);
if (response.isSuccess()) {
    DataNode node = response.getData(); // 新建
} else if (response.isWarn()) {
    DataNode node = response.getData(); // 已存在，仍可使用
}

// 指定配置组与偏好节点
MydbClientHelper.assignSaasNode("cluster-a", saasId, "db_shard_01");
```
