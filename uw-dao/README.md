# uw-dao

> 比 Hibernate 更高效，比 MyBatis 更简单的 Java 数据访问层类库

## 目录

- [1. 项目概述](#1-项目概述)
    - [1.1 简介](#11-简介)
    - [1.2 核心特性](#12-核心特性)
    - [1.3 架构总览](#13-架构总览)
- [2. 技术栈](#2-技术栈)
- [3. 安装与配置](#3-安装与配置)
    - [3.1 Maven 依赖](#31-maven-依赖)
    - [3.2 完整配置参考](#32-完整配置参考)
    - [3.3 配置项详解](#33-配置项详解)
- [4. 核心概念](#4-核心概念)
    - [4.1 DaoManager vs DaoFactory](#41-daomanager-vs-daofactory)
    - [4.2 实体类定义（DataEntity）](#42-实体类定义dataentity)
    - [4.3 注解说明](#43-注解说明)
- [5. CRUD 操作](#5-crud-操作)
    - [5.1 新增（save）](#51-新增save)
    - [5.2 查询（load）](#52-查询load)
    - [5.3 更新（update）](#53-更新update)
    - [5.4 删除（delete）](#54-删除delete)
- [6. 查询参数（QueryParam）](#6-查询参数queryparam)
    - [6.1 PageQueryParam 分页查询](#61-pagequaryparam-分页查询)
    - [6.2 QueryParam 条件查询](#62-queryparam-条件查询)
    - [6.3 @QueryMeta 注解表达式](#63-querymeta-注解表达式)
    - [6.4 排序支持](#64-排序支持)
- [7. 原生 SQL 操作](#7-原生-sql-操作)
    - [7.1 PageRowSet 结果集](#71-dataset-结果集)
    - [7.2 execute 执行更新](#72-execute-执行更新)
    - [7.3 queryForValue / queryForValueList](#73-queryforvalue--queryforvaluelist)
- [8. 事务管理](#8-事务管理)
- [9. 批量更新（BatchUpdateManager）](#9-批量更新batchupdatemanager)
- [10. 分布式序列（SequenceFactory）](#10-分布式序列sequencefactory)
- [11. 多数据源与路由](#11-多数据源与路由)
- [12. 分表（Table Sharding）](#12-分表table-sharding)
- [13. SQL 执行监控](#13-sql-执行监控)
- [14. 数据库表结构](#14-数据库表结构)
- [15. 最佳实践](#15-最佳实践)
- [16. 常见问题](#16-常见问题)

---

## 1. 项目概述

### 1.1 简介

`uw-dao` 是 `uw-base` 基础平台的数据访问层（DAL）类库，基于原生 JDBC 封装，提供 JPA 风格的 ORM 操作和原生 SQL
映射能力。以注解驱动 + 反射缓存为核心，兼顾开发效率和运行性能。

- **GroupId**: `com.umtone`
- **ArtifactId**: `uw-dao`
- **Java 版本**: 21
- **Spring Boot**: 自动配置（`AutoConfiguration`），零侵入集成

### 1.2 核心特性

| 特性               | 说明                                                                         |
|------------------|----------------------------------------------------------------------------|
| **注解驱动 ORM**     | `@TableMeta` / `@ColumnMeta` 定义实体映射，反射元数据全局缓存，无需 XML                       |
| **差量更新**         | 实体字段级变更追踪（`DataUpdateInfo`），UPDATE 时只更新已修改的字段                              |
| **多数据源路由**       | 按表名前缀路由到不同连接池，支持读写分离，轮询负载均衡                                                |
| **HikariCP 连接池** | 每个数据源独立 HikariCP 池，可配置最小/最大连接、超时、最大存活时间                                    |
| **多方言支持**        | 内置 MySQL / Oracle / SQL Server 分页方言，自动识别驱动类型                               |
| **自动分表**         | 按日期（天/月/年）或 ID 范围自动创建分片表，后台定时预创建                                           |
| **分布式序列**        | DB 乐观锁序列（`DaoSequenceFactory`）+ Redis `INCR` 高速序列（`FusionSequenceFactory`） |
| **事务管理**         | 轻量 `TransactionManager`，支持标准隔离级别                                           |
| **批量更新**         | `BatchUpdateManager` 复用 `PreparedStatement`，自动按批次提交                        |
| **SQL 执行监控**     | 慢 SQL 统计写入按天分片的 `dao_sql_stats_YYYYMMDD` 表，自动清理过期数据                        |
| **双入口设计**        | `DaoFactory`（抛异常风格）+ `DaoManager`（`ResponseData` 包装风格）                     |
| **国际化**          | 错误码消息支持 12 种语言（zh-CN/zh-TW/en/ja/ko/de/fr/es/it/pt/ar/ru）                  |

### 1.3 架构总览

```
调用层
  DaoManager (ResponseData 包装)   DaoFactory (抛出 TransactionException)
        └───────────────┬───────────────┘
                  DaoFactoryImpl
                  ┌──────┴──────┐
         EntityCommandImpl   SQLCommandImpl   BatchUpdateManagerImpl
                  └──────┬──────┘
             TransactionManagerImpl
                        │
              ConnectionManager (HikariCP)
                        │
              DaoConfigManager (路由表/分表配置)

后台服务 (DaoService, 单守护线程)
  ├── TableShardingTask    -- 每 3600s 预建分片表
  ├── StatsLogWriteTask    -- 每 30s 将慢 SQL 写入统计表
  └── StatsCleanDataTask   -- 每 86400s 清理过期统计表
```

---

## 2. 技术栈

| 技术/库                        | 版本  | 用途                                       |
|-----------------------------|-----|------------------------------------------|
| Java                        | 21  | 编程语言                                     |
| Spring Boot                 | 3.x | 自动配置、生命周期管理                              |
| HikariCP                    | —   | JDBC 连接池                                 |
| MySQL Connector/J           | —   | MySQL JDBC 驱动                            |
| Spring Data Redis / Lettuce | —   | FusionSequenceFactory Redis 支持（可选）       |
| Apache Commons Lang3        | —   | 字符串工具                                    |
| Apache Commons Pool2        | —   | Lettuce 连接池                              |
| Jackson                     | —   | 实体/结果 JSON 序列化                           |
| Swagger / OpenAPI 3         | —   | `@Schema` 注解支持                           |
| `uw-common`                 | —   | `ResponseData`、`JsonUtils`、`SystemClock` |
| JMH                         | —   | 微基准测试（test scope）                        |

---

## 3. 安装与配置

### 3.1 Maven 依赖

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-dao</artifactId>
    <version>${uw-dao.version}</version>
</dependency>
```

### 3.2 完整配置参考

```yaml
uw:
  dao:
    # 连接池配置
    conn-pool:
      # root 为默认连接池，所有 list 中未指定的参数继承 root
      root:
        driver: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai
        username: root
        password: secret
        test-sql: select 1
        min-conn: 1
        max-conn: 10
        conn-idle-timeout: 300      # 连接最大空闲时间（秒），默认 300
        conn-busy-timeout: 1800     # 连接最大占用时间（秒），默认 1800
        conn-max-age: 3600          # 连接最大存活时间（秒），默认 3600
      list:
        # 额外命名连接池，未填项继承 root 配置
        order-write:
          url: jdbc:mysql://order-db:3306/order?useSSL=false&serverTimezone=Asia/Shanghai
          max-conn: 20
        order-read:
          url: jdbc:mysql://order-db-replica:3306/order?useSSL=false&serverTimezone=Asia/Shanghai
          max-conn: 30

    # 数据源路由配置
    conn-route:
      # root 为默认路由（未匹配表前缀时使用）
      root:
        write-pools:
          - root                    # 写库连接池名（支持多个，轮询）
        read-pools:
          - root                    # 读库连接池名（支持多个，轮询）
      list:
        # key 为表名前缀
        order_:
          write-pools:
            - order-write
          read-pools:
            - order-read

    # 自动分表配置
    table-shard:
      # key 为基表名（不含后缀）
      task_runner_log:
        shard-type: date            # date | id
        shard-rule: day             # date: day/month/year；id: 每批次数量（整数）
        auto-gen: true              # 是否自动建表，默认 true
      order_item:
        shard-type: id
        shard-rule: 1000000         # 每 100 万 ID 一张表

    # SQL 执行统计配置
    sql-stats:
      enable: true
      sql-cost-min: 100             # 仅记录执行时间 >= 此值（毫秒）的 SQL，默认 100
      data-keep-days: 100           # 统计数据保留天数，默认 100

    # Redis 配置（可选，启用后激活 FusionSequenceFactory）
    redis:
      host: localhost
      port: 6379
      password: ""
      database: 0
      timeout: 3000
      lettuce:
        pool:
          min-idle: 1
          max-idle: 8
          max-active: 8
```

### 3.3 配置项详解

#### ConnPoolConfig（连接池）

| 属性                  | 默认值        | 说明          |
|---------------------|------------|-------------|
| `driver`            | —          | JDBC 驱动类名   |
| `url`               | —          | JDBC 连接 URL |
| `username`          | —          | 数据库用户名      |
| `password`          | —          | 数据库密码       |
| `test-sql`          | `select 1` | 连接健康检测 SQL  |
| `min-conn`          | `1`        | 最小连接数       |
| `max-conn`          | `1`        | 最大连接数       |
| `conn-idle-timeout` | `300`      | 空闲连接超时（秒）   |
| `conn-busy-timeout` | `1800`     | 繁忙连接超时（秒）   |
| `conn-max-age`      | `3600`     | 连接最大存活时间（秒） |

#### ConnRouteConfig（路由）

| 属性            | 说明            |
|---------------|---------------|
| `write-pools` | 写操作连接池名列表（轮询） |
| `read-pools`  | 读操作连接池名列表（轮询） |

路由匹配规则：`conn-route.list` 中的 key 作为**表名前缀**（如 `order_` 匹配 `order_2024`、`order_item` 等），未命中任何前缀时使用
`root` 路由。

#### TableShardConfig（分表）

| 属性           | 可选值                           | 说明              |
|--------------|-------------------------------|-----------------|
| `shard-type` | `date` / `id`                 | 分片类型            |
| `shard-rule` | `day` / `month` / `year` / 整数 | 日期粒度或每片 ID 数量   |
| `auto-gen`   | `true` / `false`              | 是否自动建表（默认 true） |

#### SqlStatsConfig（SQL 统计）

| 属性               | 默认值     | 说明           |
|------------------|---------|--------------|
| `enable`         | `false` | 是否启用 SQL 统计  |
| `sql-cost-min`   | `100`   | 慢 SQL 阈值（毫秒） |
| `data-keep-days` | `100`   | 统计数据保留天数     |

---

## 4. 核心概念

### 4.1 DaoManager vs DaoFactory

| 对比项     | `DaoManager`                                    | `DaoFactory`               |
|---------|-------------------------------------------------|----------------------------|
| 获取实例    | `DaoManager.getInstance()`                      | `DaoFactory.getInstance()` |
| 返回类型    | `ResponseData<T>`                               | 直接返回值                      |
| 异常处理    | 异常封装在 `ResponseData` 中                          | 抛出 `TransactionException`  |
| 更新 0 行时 | 返回 `ResponseData.warnCode(DATA_NOT_FOUND_WARN)` | 返回 `0`                     |
| 适用场景    | 新项目、Spring MVC 服务层                              | 需要细粒度异常控制的场景               |

> **注意**：每次调用 `getInstance()` 都会创建一个新的实例（非单例），每个实例持有独立的事务状态和批量更新状态，请勿将实例存储为
> Spring Bean 的成员变量用于共享。

```java
// DaoManager 风格（推荐）
DaoManager dao = DaoManager.getInstance();
ResponseData<User> resp = dao.load(User.class, userId);
if (resp.isSuccess()) {
    User user = resp.getData();
}

// DaoFactory 风格（传统）
DaoFactory dao = DaoFactory.getInstance();
try {
    User user = dao.load(User.class, userId);
} catch (TransactionException e) {
    // 处理异常
}
```

### 4.2 实体类定义（DataEntity）

所有用于 save / update / delete 操作的实体类必须：

1. 使用 `@TableMeta` 注解指定表名
2. 字段使用 `@ColumnMeta` 注解描述列元数据
3. 实现 `DataEntity` 接口的 5 个方法
4. 在 setter 中通过 `DataUpdateInfo.addUpdateInfo(...)` 记录字段变更（启用差量更新）
5. 声明 `transient boolean _IS_LOADED` 标志位（框架 load 后自动置 true，触发差量更新模式）

```java
@TableMeta(tableName = "sys_user", tableType = "table")
public class User implements DataEntity {

    // 加载标志，框架通过反射设置，勿手动修改
    private transient boolean _IS_LOADED;

    // 变更追踪对象，transient 防止序列化
    private transient DataUpdateInfo _UPDATED_INFO;

    @ColumnMeta(columnName = "id", primaryKey = true, autoIncrement = false)
    private long id;

    @ColumnMeta(columnName = "username", dataSize = 50, nullable = false)
    private String username;

    @ColumnMeta(columnName = "email", dataSize = 100)
    private String email;

    @ColumnMeta(columnName = "create_date")
    private Date createDate;

    @ColumnMeta(columnName = "state")
    private int state;

    // ---------- DataEntity 接口实现 ----------

    @Override
    public String ENTITY_TABLE() { return "sys_user"; }

    @Override
    public String ENTITY_NAME() { return "用户"; }

    @Override
    public Serializable ENTITY_ID() { return id; }

    @Override
    public DataUpdateInfo GET_UPDATED_INFO() { return _UPDATED_INFO; }

    @Override
    public void CLEAR_UPDATED_INFO() { _UPDATED_INFO = null; }

    // ---------- Getter / Setter（Setter 中记录变更） ----------

    public long getId() { return id; }

    public User setId(long id) {
        // 初始赋值不追踪（_IS_LOADED=false 时）
        this._UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "id", this.id, id, _IS_LOADED);
        this.id = id;
        return this;
    }

    public String getUsername() { return username; }

    public User setUsername(String username) {
        this._UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "username", this.username, username, _IS_LOADED);
        this.username = username;
        return this;
    }

    // ... 其他 getter/setter 同理
}
```

> **差量更新原理**：调用 `load()` 后，框架将 `_IS_LOADED` 置为 `true`。此后每次调用 setter，变更记录到 `DataUpdateInfo`。执行
`update()` 时，框架仅对有变更的字段生成 `SET col=?`，大幅减少无效更新。

### 4.3 注解说明

#### @TableMeta（类级别）

```java
@TableMeta(
    tableName = "sys_user",   // 数据库表名（必填）
    tableType = "table",      // "table"（默认）或 "view"
    sql = ""                  // 自定义 SELECT 前缀，覆盖默认的 "select * from tableName"
)
```

#### @ColumnMeta（字段级别）

```java
@ColumnMeta(
    columnName    = "id",     // 数据库列名（默认等于字段名）
    dataType      = "long",   // Java 类型提示（可选，用于文档）
    dataSize      = 0,        // 最大长度（0 表示不限）
    primaryKey    = false,    // 是否主键
    nullable      = true,     // 是否可为 null
    autoIncrement = false     // 是否自增主键（自增主键 save 后自动回填）
)
```

#### @QueryMeta（QueryParam 子类字段/方法级别）

```java
@QueryMeta(expr = "username like ?")
private String username;
```

详见 [第 6 节](#6-查询参数queryparam)。

---

## 5. CRUD 操作

### 5.1 新增（save）

```java
DaoManager dao = DaoManager.getInstance();

// 保存单个实体（使用实体 @TableMeta 中的表名）
User user = new User();
user.setId(SequenceFactory.getSequenceId(User.class));
user.setUsername("alice");
user.setCreateDate(new Date());
ResponseData<User> resp = dao.save(user);

// 保存到指定表（分表场景）
String tableName = ShardingTableUtils.getTableNameByDate("sys_user", new Date());
ResponseData<User> resp2 = dao.save(user, tableName);

// 批量保存（单次 INSERT INTO ... VALUES (...),(...),...）
List<User> users = buildUserList();
ResponseData<List<User>> batchResp = dao.save(users);
```

### 5.2 查询（load）

```java
// 按主键加载（加载成功后 _IS_LOADED 自动置 true，后续 update 启用差量更新）
ResponseData<User> resp = dao.load(User.class, userId);

// 加载指定分表中的数据
ResponseData<User> resp2 = dao.load(User.class, "sys_user_20240101", userId);
```

**分页列表查询**：

```java
// 使用 PageQueryParam
UserQueryParam param = new UserQueryParam();
param.setUsername("alice");   // 对应 @QueryMeta(expr="username like ?") 字段
param.PAGE(1).RESULT_NUM(20);

ResponseData<PageList<User>> resp = dao.list(User.class, param);
PageList<User> list = resp.getData();
// list.size()      -- 当前页记录数
// list.sizeAll()   -- 总记录数（REQUEST_TYPE=2 时有值）
// list.pageCount() -- 总页数
// list.results()   -- 记录列表

// 使用原生 SQL 列表查询
ResponseData<PageList<User>> resp2 = dao.list(
    User.class,
    "select * from sys_user where state = ?",
    new Object[]{1},
    0, 20, true   // startIndex, resultNum, autoCount
);
```

### 5.3 更新（update）

```java
// 先 load 再 update，自动差量更新
ResponseData<User> loadResp = dao.load(User.class, userId);
User user = loadResp.getData();
user.setUsername("bob");      // 仅修改 username 字段
user.setEmail("bob@a.com");   // 仅修改 email 字段
// 生成 SQL：UPDATE sys_user SET username=?, email=? WHERE id=?
ResponseData<Integer> resp = dao.update(user);

// 按条件批量更新（QueryParam 生成 WHERE 子句）
User updateEntity = new User();
updateEntity.setState(0);
UserQueryParam param = new UserQueryParam();
param.setState(1);   // @QueryMeta(expr="state=?")
ResponseData<Integer> resp2 = dao.update(updateEntity, param);
```

### 5.4 删除（delete）

```java
// 按主键删除（实体需已设置 ID）
ResponseData<Integer> resp = dao.delete(user);

// 按条件删除
UserQueryParam param = new UserQueryParam();
param.setState(-1);
ResponseData<Integer> resp2 = dao.delete(User.class, param);

// 删除指定分表中的数据
ResponseData<Integer> resp3 = dao.delete(user, "sys_user_20240101");
```

---

## 6. 查询参数（QueryParam）

### 6.1 PageQueryParam 分页查询

`PageQueryParam` 继承 `QueryParam`，添加分页控制：

| 属性             | HTTP 参数别名 | 说明                              | 默认值  |
|----------------|-----------|---------------------------------|------|
| `PAGE`         | `$pg`     | 当前页码（从 1 开始）                    | `1`  |
| `RESULT_NUM`   | `$rn`     | 每页记录数（最大 10000）                 | `10` |
| `START_INDEX`  | `$si`     | 起始偏移（优先级高于 PAGE）                | `0`  |
| `REQUEST_TYPE` | `$rt`     | `0`=仅计数, `1`=仅数据（默认）, `2`=数据+计数 | `1`  |

```java
PageQueryParam param = new PageQueryParam();
param.PAGE(2).RESULT_NUM(20).REQUEST_TYPE(PageQueryParam.REQUEST_ALL);

// 等价 HTTP 参数绑定（Spring MVC / Jackson）：
// ?$pg=2&$rn=20&$rt=2
```

### 6.2 QueryParam 条件查询

自定义查询参数类继承 `PageQueryParam`（或 `QueryParam`），字段加 `@QueryMeta` 注解：

```java
public class UserQueryParam extends PageQueryParam<UserQueryParam> {

    @Schema(description = "用户名（模糊匹配）")
    @QueryMeta(expr = "username like ?")
    private String username;

    @Schema(description = "用户状态")
    @QueryMeta(expr = "state=?")
    private Integer state;

    @Schema(description = "注册开始时间")
    @QueryMeta(expr = "create_date>=?")
    private Date createDateBegin;

    @Schema(description = "注册结束时间")
    @QueryMeta(expr = "create_date<=?")
    private Date createDateEnd;

    // getter/setter ...
}
```

使用：

```java
UserQueryParam param = new UserQueryParam();
param.setUsername("ali");    // 生成 and username like ?，参数 = "ali"
param.setState(1);           // 生成 and state=?，参数 = 1
// 未设置的字段（null）自动跳过

// 附加原始条件（不带参数）
param.ADD_EXT_COND_SQL("create_date is not null");

// 附加参数化条件
param.ADD_EXT_COND("create_date>=?", startDate);
param.ADD_EXT_COND_PARAM("state", 1);  // 等价 state=?

ResponseData<PageList<User>> resp = dao.list(User.class, param);
```

### 6.3 @QueryMeta 注解表达式

| 表达式示例                        | 说明                    | 字段类型                |
|------------------------------|-----------------------|---------------------|
| `"col=?"`                    | 等值匹配                  | 任意                  |
| `"col like ?"`               | 模糊匹配                  | String              |
| `"col>=?"`                   | 范围比较                  | 数值/日期               |
| `"col in (?)"`               | IN 查询                 | 数组 / List           |
| `"col between ? and ?"`      | 区间查询                  | 长度为 2 的数组           |
| `"(c1 like ? or c2 like ?)"` | 多列 OR 匹配              | String（值同时绑定到两个占位符） |
| `"state>=0"`                 | 无占位符（仅判断字段非 null 时激活） | 任意                  |

> `like` 表达式有最小长度保护：`LIKE_QUERY_PARAM_MIN_LEN`（默认 3 字符），短于此长度时该条件被跳过，可通过
`param.LIKE_QUERY_PARAM_MIN_LEN(1)` 调整。

### 6.4 排序支持

子类通过覆盖 `ALLOWED_SORT_PROPERTY()` 定义允许排序的列（防 SQL 注入）：

```java
public class UserQueryParam extends PageQueryParam<UserQueryParam> {

    @Override
    public Map<String, String> ALLOWED_SORT_PROPERTY() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("createDate", "create_date");   // Java属性名 -> 列名
        map.put("id", "id");
        return map;
    }
}

// 使用
param.ADD_SORT("createDate", QueryParam.SORT_DESC)
     .ADD_SORT("id", QueryParam.SORT_ASC);
// 生成：order by create_date desc, id asc

// HTTP 参数绑定：?$sn=createDate,id&$st=2,1
```

---

## 7. 原生 SQL 操作

### 7.1 PageRowSet 结果集

`PageRowSet` 是游标式结果集，所有行以 `List<Object[]>` 形式存储在内存中：

```java
DaoFactory dao = DaoFactory.getInstance();

PageRowSet ds = dao.queryForPageRowSet(
    "select id, username, state from sys_user where state=?",
    new Object[]{1},
    0, 100, true   // startIndex, resultNum, autoCount
);

// 遍历
while (ds.next()) {
    long id       = ds.getLong("id");
    String name   = ds.getString("username");
    int state     = ds.getInt("state");
}

// 转换为强类型列表
PageList<UserVO> list = ds.map(row -> {
    UserVO vo = new UserVO();
    vo.setId(row.getLong("id"));
    vo.setUsername(row.getString("username"));
    return vo;
});

// 元数据
String[] cols = ds.getColumnNames();
```

**PageRowSet 支持的类型转换方法**：`getBoolean`, `getInt`, `getLong`, `getDouble`, `getFloat`, `getString`, `getBigInteger`,
`getDecimal`, `getBytes`, `getDate`，均支持按列名或按列索引（0-based）访问。

### 7.2 executeCommand 执行更新

```java
int rows = dao.execute(
    "update sys_user set state=? where id=?",
    new Object[]{0, userId}
);
```

### 7.3 queryForValue / queryForValueList

```java
// 查询单个值
Long count = dao.queryForValue(Long.class,
    "select count(*) from sys_user where state=?",
    new Object[]{1}
);

// 查询单列列表
List<Long> ids = dao.queryForValueList(Long.class,
    "select id from sys_user where state=?",
    new Object[]{1}
);
```

---

## 8. 事务管理

`uw-dao` 提供轻量级 `TransactionManager`，与 Spring 事务体系**独立**：

```java
DaoFactory dao = DaoFactory.getInstance();
TransactionManager tx = dao.beginTransaction();
try {
    dao.save(order);
    dao.save(orderItem);
    tx.commit();
} catch (Exception e) {
    tx.rollback();
    throw e;
}
```

设置隔离级别：

```java
TransactionManager tx = dao.beginTransaction();
tx.setTransactionIsolation(TransactionManager.TRANSACTION_READ_COMMITTED);
```

**支持的隔离级别常量**：

| 常量                             | 说明    |
|--------------------------------|-------|
| `TRANSACTION_NONE`             | 不支持事务 |
| `TRANSACTION_READ_UNCOMMITTED` | 读未提交  |
| `TRANSACTION_READ_COMMITTED`   | 读已提交  |
| `TRANSACTION_REPEATABLE_READ`  | 可重复读  |
| `TRANSACTION_SERIALIZABLE`     | 串行化   |

> `DaoManager` 同样支持通过 `getDaoFactory().beginTransaction()` 获取 `TransactionManager`。

---

## 9. 批量更新（BatchUpdateManager）

`BatchUpdateManager` 在内部复用 `PreparedStatement`，达到 `batchSize` 时自动触发 `executeBatch()`：

```java
DaoFactory dao = DaoFactory.getInstance();
BatchUpdateManager batch = dao.beginBatchUpdate();
batch.setBatchSize(500);   // 每 500 条提交一次，默认 100

for (LogRecord record : records) {
    // 与普通 DAO 操作混用，框架自动判断是否在批量模式
    dao.save(record);
}

// 提交剩余未满批次的数据
Map<String, List<Integer>> result = batch.submit();
// result: SQL -> 各批次受影响行数列表
```

---

## 10. 分布式序列（SequenceFactory）

`SequenceFactory` 统一入口，根据是否配置 Redis 自动选择实现：

```java
// 获取下一个序列 ID（为实体类生成，seqName = 类全限定名）
long id = SequenceFactory.getSequenceId(User.class);

// 使用自定义序列名
long orderId = SequenceFactory.getSequenceId("order_seq");

// 查看当前 ID（不消耗序列）
long current = SequenceFactory.getCurrentId("order_seq");

// 重置序列
SequenceFactory.resetSequenceId("order_seq", 1000000L);
```

### 两种实现对比

| 特性   | `DaoSequenceFactory`（DB）   | `FusionSequenceFactory`（Redis+DB） |
|------|----------------------------|-----------------------------------|
| 依赖   | 仅数据库                       | 数据库 + Redis                       |
| 吞吐量  | 约 `incrementNum × 100` TPS | 约 `DB吞吐 × 10000` TPS              |
| 持久化  | 全量持久化到 `sys_seq` 表         | Redis `INCR` + 定期同步到 DB           |
| 适用场景 | 中低并发                       | 高并发 ID 生成                         |

### 依赖的数据库表

```sql
-- 序列表（需手动创建，参见 database/init.sql）
CREATE TABLE sys_seq (
    seq_name      VARCHAR(100) NOT NULL PRIMARY KEY COMMENT '序列名称',
    seq_id        BIGINT       NOT NULL DEFAULT 0   COMMENT '当前序列值',
    increment_num INT          NOT NULL DEFAULT 100 COMMENT '每次预取数量',
    create_date   DATETIME     NOT NULL             COMMENT '创建时间',
    modify_date   DATETIME     NOT NULL             COMMENT '修改时间'
);
```

### FusionSequenceFactory 随机池（可选）

适用于用户 ID 等需要随机分布的场景：

```java
// 从 Redis SET 池中随机取一个 ID
long userId = FusionSequenceFactory.getRandomSequenceIdFromPool("user_id_pool");

// 归还未使用的 ID
FusionSequenceFactory.restoreSequenceIdToPool("user_id_pool", userId);
```

---

## 11. 多数据源与路由

框架根据 SQL 中的**表名前缀**自动路由到对应连接池：

```
SQL: SELECT * FROM order_2024 WHERE ...
→ 表名: order_2024  →  前缀匹配: order_  →  连接池: order-read（轮询）
```

**读写分离规则**：

- `SELECT` → 读池（`read-pools`）
- `INSERT` / `UPDATE` / `DELETE` / `REPLACE` / `MERGE` / `CREATE` → 写池（`write-pools`）

**手动指定连接名**（绕过自动路由）：

```java
// 所有 DaoFactory/DaoManager 方法均有 connName 前缀重载
dao.load("order-write", Order.class, orderId);
dao.execute("order-write", "update order_2024 set ...", params);
```

---

## 12. 分表（Table Sharding）

### 按日期分表

```yaml
uw:
  dao:
    table-shard:
      log_access:
        shard-type: date
        shard-rule: day    # 生成 log_access_20240101, log_access_20240102 ...
```

```java
// 获取当天分表名
String table = ShardingTableUtils.getTableNameByDate("log_access", new Date());
// 结果: "log_access_20240101"

// 按日期查询
dao.list(AccessLog.class, table, param);
```

### 按 ID 分表

```yaml
uw:
  dao:
    table-shard:
      user_profile:
        shard-type: id
        shard-rule: 1000000   # 每 100 万一张表
```

```java
long id = SequenceFactory.getSequenceId(UserProfile.class);
String table = ShardingTableUtils.getTableNameById("user_profile", id);
// id=1500000 → "user_profile_1"

dao.save(profile, table);
```

### 自动建表

框架后台 `TableShardingTask` 每小时运行一次，自动：

1. 检查当前分片表和下一分片表是否存在
2. 若不存在，执行 `SHOW CREATE TABLE 基表` 并创建同结构的分片表

**前提**：基表必须存在（框架不会创建基表本身）。

---

## 13. SQL 执行监控

启用后，框架记录每条 SQL 的连接名、执行时间、影响行数、异常信息等，每 30 秒批量写入按天分片的统计表：

```yaml
uw:
  dao:
    sql-stats:
      enable: true
      sql-cost-min: 100      # 只记录 >= 100ms 的 SQL
      data-keep-days: 30     # 保留 30 天
```

统计数据写入表 `dao_sql_stats_YYYYMMDD`（自动按天分表），自动清理超出 `data-keep-days` 的旧表。

**在代码中启用 SQL 追踪**（调试用）：

```java
DaoFactory dao = DaoFactory.getInstance();
dao.enableSqlExecuteStats();

// ... 执行 DAO 操作 ...

List<SqlExecuteStats> statsList = dao.getSqlExecuteStatsList();
for (SqlExecuteStats stats : statsList) {
    System.out.println(stats.genFullSqlInfo());   // 完整 SQL（参数已替换）
    System.out.println("耗时: " + stats.getAllMillis() + "ms");
}
dao.disableSqlExecuteStats();
```

`SqlExecuteStats` 关键字段：

| 字段           | 说明              |
|--------------|-----------------|
| `connName`   | 使用的连接池名         |
| `sql`        | 带 `?` 占位符的 SQL  |
| `paramList`  | 参数值数组           |
| `connMillis` | 获取连接耗时（ms）      |
| `dbMillis`   | SQL 执行耗时（ms）    |
| `allMillis`  | 总耗时（ms）         |
| `rowNum`     | 影响/返回行数         |
| `exception`  | 异常信息（无异常为 null） |
| `actionDate` | 执行时间            |

---

## 14. 数据库表结构

### 序列表 sys_seq

```sql
CREATE TABLE `sys_seq` (
  `seq_name`      VARCHAR(100) NOT NULL COMMENT '序列名称',
  `seq_id`        BIGINT       NOT NULL DEFAULT 0 COMMENT '当前序列值',
  `increment_num` INT          NOT NULL DEFAULT 100 COMMENT '每次预取步长',
  `create_date`   DATETIME     NOT NULL COMMENT '创建时间',
  `modify_date`   DATETIME     NOT NULL COMMENT '修改时间',
  PRIMARY KEY (`seq_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分布式序列表';
```

### SQL 统计表 dao_sql_stats_YYYYMMDD

框架自动创建，表结构（按天分片）：

```sql
CREATE TABLE `dao_sql_stats_20240101` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `conn_name`    VARCHAR(100) NOT NULL COMMENT '连接池名',
  `sql_info`     TEXT         NOT NULL COMMENT 'SQL语句',
  `param_info`   TEXT                  COMMENT '参数信息',
  `exception`    TEXT                  COMMENT '异常信息',
  `row_num`      INT          NOT NULL DEFAULT 0 COMMENT '影响行数',
  `conn_millis`  BIGINT       NOT NULL DEFAULT 0 COMMENT '获取连接耗时(ms)',
  `db_millis`    BIGINT       NOT NULL DEFAULT 0 COMMENT '执行耗时(ms)',
  `all_millis`   BIGINT       NOT NULL DEFAULT 0 COMMENT '总耗时(ms)',
  `action_date`  DATETIME     NOT NULL COMMENT '执行时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

完整初始化脚本见 `database/init.sql`。

---

## 15. 最佳实践

### 实体类规范

1. 主键使用 `long` 类型，配合 `SequenceFactory.getSequenceId()` 生成全局唯一 ID
2. `_IS_LOADED` 和 `_UPDATED_INFO` 声明为 `transient`，避免被序列化
3. setter 方法中始终调用 `DataUpdateInfo.addUpdateInfo(..., _IS_LOADED)` 以启用差量更新
4. 实体类仅包含与数据库字段对应的属性，不放业务逻辑

### 查询规范

1. 列表查询始终传入 `startIndex` 和 `resultNum`，避免全表扫描导致 OOM
2. `REQUEST_TYPE` 默认为 `REQUEST_DATA（1）`，仅在需要总数时设为 `REQUEST_ALL（2）`
3. `like` 查询建议设置合理的 `LIKE_QUERY_PARAM_MIN_LEN`，防止短词导致全表扫描
4. 定义 `ALLOWED_SORT_PROPERTY()` 白名单，防止排序字段注入

### 分表规范

1. 基表须手动创建好（含索引），框架只负责克隆结构创建分片表
2. 跨分片查询需要在业务层按时间/ID 范围逐表查询后合并
3. 分片表名通过 `ShardingTableUtils` 工具方法生成，保持一致性

### 序列规范

1. 生产环境配置 Redis 以启用 `FusionSequenceFactory`，提升 ID 生成吞吐量
2. `sys_seq` 表的 `increment_num` 调大（如 1000）可减少 DB 访问频次
3. 不要将序列 ID 用作分页的 `page offset`，ID 不一定连续

### 事务规范

1. 事务范围尽量小，避免长事务持有连接
2. 事务内操作同一数据源，跨源操作请使用分布式事务框架（如 Seata）
3. `DaoFactory`/`DaoManager` 实例不要跨线程共享

---

## 16. 常见问题

### Q1：DaoManager.load() 返回的 ResponseData.getData() 为 null？

`DaoManager` 不会因为查询无结果而返回 `warn`，结果为 null 时 `getData()` 返回 null。更新操作返回 0 行时才触发
`DATA_NOT_FOUND_WARN`。建议：

```java
ResponseData<User> resp = dao.load(User.class, id);
if (resp.isSuccess() && resp.getData() != null) {
    // 正常处理
}
```

### Q2：多次调用 update() 后发现 SQL 只更新了部分字段？

这是差量更新的正常行为。若实体不是通过 `load()` 获取的（`_IS_LOADED=false`），则 `update()` 会更新所有 `@ColumnMeta`
字段（非主键）。若是通过 `load()` 获取的，只有 setter 被调用过的字段才会出现在 UPDATE 语句中。

### Q3：如何配置读写分离？

```yaml
uw:
  dao:
    conn-pool:
      root:
        url: jdbc:mysql://master:3306/db
      list:
        db-read:
          url: jdbc:mysql://replica:3306/db
    conn-route:
      root:
        write-pools: [root]
        read-pools: [db-read]
```

### Q4：如何查询跨多个分片表的数据？

框架不支持自动跨表联合查询，需在业务层手动处理：

```java
// 按日期范围逐表查询
LocalDate start = LocalDate.of(2024, 1, 1);
LocalDate end   = LocalDate.of(2024, 3, 31);
List<LogRecord> all = new ArrayList<>();

for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
    String table = ShardingTableUtils.getTableNameByDate("log_access", d);
    ResponseData<PageList<LogRecord>> resp = dao.list(LogRecord.class, table, param);
    if (resp.isSuccess()) {
        all.addAll(resp.getData().results());
    }
}
```

### Q5：enableSqlExecuteStats() 对性能有影响吗？

有轻微影响：每次 SQL 执行都会创建 `SqlExecuteStats` 对象并追加到列表。建议仅在开发/调试阶段使用，生产环境通过
`sql-stats.enable=true` + `sql-cost-min` 阈值来只记录慢 SQL。

### Q6：FusionSequenceFactory 在 Redis 故障时怎么办？

Redis 故障时 `FusionSequenceFactory` 的 `INCR` 操作会抛出异常，序列生成失败。建议：

1. 配置 Redis Sentinel 或 Cluster 保障高可用
2. 业务层对序列生成做重试和降级处理

### Q7：Oracle / SQL Server 如何接入？

无需修改代码，框架根据 `driver` 类名自动识别方言并切换分页实现：

```yaml
uw:
  dao:
    conn-pool:
      root:
        driver: oracle.jdbc.driver.OracleDriver
        url: jdbc:oracle:thin:@localhost:1521:ORCL
```

---

## 许可证

[Apache License 2.0](../LICENSE)
