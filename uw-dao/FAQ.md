# uw-dao 常见问题（FAQ）

> 本文档与 `uw-dao` 源码对齐，反映最新行为。

#### 1. 概括一下 uw-dao 的主要功能和特性。

uw-dao 是一个基于原生 JDBC 封装的数据访问层类库，比 Hibernate 更高效，比 MyBatis 更简单，并统一管理数据库连接池。

1. **注解驱动 ORM**：`@TableMeta` / `@ColumnMeta` 定义实体映射，反射元数据全局缓存，JPA 风格的 CRUD。
2. **差量更新**：实体字段级变更追踪（`DataUpdateInfo` + `_IS_LOADED` 标志），UPDATE 时只更新已修改字段。
3. **多数据源路由**：按表名前缀路由到不同连接池，支持读写分离（SELECT→读池，写操作→写池），轮询负载均衡。
4. **HikariCP 连接池**：每数据源独立池，连接超时/空闲/寿命有安全上下限，避免数据库宕机卡死。
5. **多方言支持**：内置 MySQL / Oracle / SQL Server / PostgreSQL 分页方言，按驱动类名自动识别。
6. **自动分表**：按日期（天/月/年）或 ID 范围自动创建分片表，后台定时预创建。
7. **分布式序列**：DB 乐观锁序列（`DaoSequenceFactory`）+ Redis 段缓存高速序列（`FusionSequenceFactory`），Redis 故障自动降级。
8. **事务与批量更新**：轻量 `TransactionManager`（可设隔离级别）；`BatchUpdateManager` 复用 PreparedStatement。
9. **SQL 执行监控**：慢 SQL 按天分表写入 `dao_sql_stats_YYYYMMDD`，自动清理过期数据。
10. **双入口**：`DaoManager`（推荐，ResponseData 包装）+ `DaoFactory`（传统，抛异常风格，不推荐新代码）。

#### 2. uw-dao 的自动分表是根据什么进行分表？该如何配置？

支持按**日期**或 **ID 范围**分表，后台每小时预建当天与次日的分片表。

```yaml
uw:
  dao:
    table-shard:
      # key 为基表名（基表须手动建好，框架只克隆结构创建分片表）
      task_runner_log:
        shard-type: date       # date | id
        shard-rule: day        # date: day/month/year；id: 每片ID数量（整数）
        auto-gen: true         # 是否自动建表，默认 true
      order_item:
        shard-type: id
        shard-rule: 1000000    # 每 100 万 ID 一张表
```

代码中用 `ShardingTableUtils.getTableNameByDate(baseName, date)` 或 `getTableNameById(baseName, id)` 生成目标表名。

#### 3. uw-dao 的数据库访问入口是什么？获取到的实例是否线程安全、能否共用？

```java
DaoManager dao = DaoManager.getInstance();          // 推荐，返回 ResponseData
DaoFactory dao2 = DaoFactory.getInstance();         // 传统入口，不推荐新代码使用，抛 TransactionException
```

> 新代码统一用 `DaoManager`。事务/批量更新已由 DaoManager 直接委派
> （`dao.beginTransaction()` / `dao.beginBatchUpdate()`），无需经 DaoFactory。

`getInstance()` **每次返回新实例**，每个实例持有独立的事务状态和批量更新状态。

- **不使用事务/批量时**：实例可方法内创建即用即弃，逻辑上无状态冲突。
- **不可共用场景**：实例**不要**存为 Spring Bean 的成员变量跨线程共享，尤其涉及 `beginTransaction()` / `beginBatchUpdate()` 时——事务连接绑定在实例内部，跨线程会串用。

#### 4. 配置多个数据库连接时，不指定连接名的情况下，框架如何决定用哪个连接？

两种方式自动路由：

1. **按表名前缀**（`conn-route.list` 的 key 作为表名前缀匹配）。
2. **按 SQL 语句探测**（从 `FROM` / `INTO` / `UPDATE` 提取表名再走前缀匹配）。

```yaml
uw:
  dao:
    conn-route:
      root:
        write-pools: [root]
        read-pools: [root]
      list:
        order_:
          write-pools: [order-write]
          read-pools: [order-read]
```

读写规则：`SELECT`→读池；`INSERT/UPDATE/DELETE/REPLACE/MERGE/CREATE`→写池。未配 read-pools 时读操作自动回退 write-pools。未命中任何前缀用 root 路由。手动指定可传 connName 首参：`dao.load("order-write", Order.class, id)`。

#### 5. SequenceFactory 的 incrementNum 有什么作用？越大越好吗？

`incrementNum` 是序列预取步长：每次从 DB 申请一个步长段的号缓存到本地，减少 DB 访问次数。

- `incrementNum` 越大，DB 操作开销越小，发号吞吐越高（`tps ≈ incrementNum × 100`）。
- 插入频繁的表，`increment_num >= 100` 可显著提升性能。
- **代价**：步长越大，服务刚占用序列就宕机/重启，会留下更大空档（号段丢失，非重复）。

`increment_num` 通过 `sys_seq` 表调整，或管理后台。FusionSequenceFactory（Redis）使用固定的 POOL_SIZE=10000 段缓存，吞吐约为 DB 实现的百倍以上。

#### 6. PageList 和 PageRowSet 的使用场景差别？

| 对比项 | `PageList<T>` | `PageRowSet` |
|--------|---------------|--------------|
| 类型 | 强类型实体列表 | 弱类型行列集合（`List<Object[]>`） |
| 映射 | 自动反射映射到实体 | 手动 `row.getLong("col")` 取值 |
| 性能 | 更优（直接映射） | 略低（多一次 Object[] 中转） |
| 适用 | 查询映射到实体类 | 联表/动态列/无需实体的灵活查询 |

优先用 `PageList`（`dao.list` / `dao.queryForList`），仅当结果无法映射到单一实体时用 `PageRowSet`（`dao.queryForRowSet`）。

#### 7. PageQueryParam 和 QueryParam 有什么区别？

- **`QueryParam<P>`**：查询参数基类，支持 `@QueryMeta` 条件、排序、附加 where。无分页。
- **`PageQueryParam`**：继承 `QueryParam`，增加分页控制。

PageQueryParam 分页字段（HTTP 参数别名）：

| 字段 | 别名 | 默认 | 说明 |
|------|------|------|------|
| `PAGE` | `$pg` | 1 | 页码（从1起） |
| `RESULT_NUM` | `$rn` | 10 | 每页条数 |
| `START_INDEX` | `$si` | 0 | 起始偏移（优先于 PAGE） |
| `REQUEST_TYPE` | `$rt` | 1 | 0=仅计数, 1=仅数据, 2=数据+计数 |

排序常量 `SORT_NONE=0 / SORT_ASC=1 / SORT_DESC=2`，通过 `ADD_SORT(name, type)` 或 HTTP `$sn/$st` 设置。

#### 8. 假设有一个 user 表，如何编写按 userName 模糊查询 + age 范围查询的分页参数类？

```java
public class UserAgeQueryParam extends PageQueryParam<UserAgeQueryParam> {

    @QueryMeta(expr = "user_name like ?")
    private String userName;

    // between 需长度2数组，两个占位符分别绑定 begin/end
    @QueryMeta(expr = "age between ? and ?")
    private Integer[] age;

    @Override
    public Map<String, String> ALLOWED_SORT_PROPERTY() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("id", "id");
        return map;
    }
    // getter/setter ...
}
```

`@ColumnMeta.columnName` 大小写不敏感，框架内部统一小写匹配。

#### 9. 集合字段用 @QueryMeta 做 IN 查询时占位符怎么写？

```java
@QueryMeta(expr = "name in (?)")
private List<String> name;   // 或 String[] / Integer[]
```

框架会按集合大小自动展开占位符（`?` → `?,?,?`）并绑定每个元素。`between ? and ?` 用长度2数组，同一值绑多占位符也支持。

#### 10. 如何正确使用批量写入？如何优化速度？

⚠️ **批量更新必须在事务中运行**。完整步骤：

1. MySQL 连接 URL 必须加 `rewriteBatchedStatements=true`。
2. `DaoManager.getInstance()` 获取实例（事务/批量状态绑定在实例上，勿跨线程共用）。
3. `dao.beginTransaction()` 开启事务（DaoManager 已委派 DaoFactory 的事务 API）。
4. `dao.beginBatchUpdate()` 开启批量更新，`setBatchSize(N)` 设定每批大小（默认 100）。
5. 循环调用 `dao.save/update/delete`，框架自动判批量模式走 `addBatch`，满 batchSize 自动 `executeBatch`。
6. `batch.submit()` 提交剩余批次，返回 `Map<SQL, List<行数>>`。
7. `tx.commit()` 提交事务；异常走 `tx.rollback()`。

```java
DaoManager dao = DaoManager.getInstance();
TransactionManager tx = dao.beginTransaction();
BatchUpdateManager batch = dao.beginBatchUpdate();
batch.setBatchSize(500);
try {
    for (User u : userList) {
        dao.save(u);
    }
    batch.submit();
    tx.commit();
} catch (Exception e) {
    tx.rollback();
    throw e;
}
```

**优化**：合理 `setBatchSize`（500~1000）减少 executeBatch 次数；`rewriteBatchedStatements=true` 让驱动合并语句。`submit()` 后 batchSize 重置为默认 100，连续批量需重新设置。

#### 11. 如何开启 SQL 统计与慢查询分析？如何在日志输出执行 SQL？

```yaml
uw:
  dao:
    sql-stats:
      enable: true          # 默认 false
      sql-cost-min: 100     # 仅记录执行 >=100ms 的 SQL
      data-keep-days: 100   # 按天分表，自动清理超期表
```

启用后每条慢 SQL 写入 `dao_sql_stats_YYYYMMDD`，后台每30s批量写、每天清理超期分表。

代码内临时调试（不影响统计表）：

```java
DaoManager dao = DaoManager.getInstance();
dao.enableSqlExecuteStats();
// ... 执行 DAO 操作 ...
dao.getSqlExecuteStatsList().forEach(s -> System.out.println(s.genFullSqlInfo()));
dao.disableSqlExecuteStats();
```

#### 12. queryForObject 和 queryForValue 有什么区别？

- **`queryForObject(Class<T>, sql, params)`**：查询单个**对象**（单行映射到实体类）。
- **`queryForValue(Class<T>, sql, params)`**：查询单个**标量值**（单行单字段，T 为 `int/long/String/Date` 等基本或包装类型）。

查某个字段用于计算时用 `queryForValue`，减少实体映射开销。注意：包装类型（`Integer/Long`）在数据库列为 NULL 时返回 `null`；基本类型（`int.class`）返回 0（无法区分"真0"与"NULL"），查可空列建议用包装类型。

#### 13. 原生类型与包装类型在 entity 和 dto 中的选择？

- 原生类型（`long/int`）：内存更小，无装箱开销，运算更快，但**不能为 null**。
- 包装类型（`Long/Integer`）：可为 null（用于判空、可空列），支持泛型，但有装箱开销。

**实体字段（entity）**：主键、状态码等必有值的字段用原生类型减小内存；允许为空的字段（如 `finishDate`）用包装类型，框架会正确处理 null 读写（save 时 setObject null，load 时按 wasNull 置 null）。

**DTO 字段**：通常用包装类型，便于区分"未传值(null)"与"默认值(0)"。

#### 14. FusionSequenceFactory 在 Redis 故障时会怎样？

框架按"是否配置 Redis 且 Redis 可用"自动选择发号实现：

- Redis 可用 → `FusionSequenceFactory`（Redis 段缓存，高吞吐）。
- 未配置 Redis / Redis 初始化失败 / 句柄未就绪 → 自动降级到 `DaoSequenceFactory`（DB 乐观锁）。

降级保证全局发号不因 Redis 故障中断。运行期 Redis 瞬时抖动由 Fusion 内部重试（指数退避，最多50次）兜底；持续不可达建议配置 Redis Sentinel/Cluster 保障高可用。

#### 15. 为什么 load() 后再 update() 只更新了部分字段？

这是差量更新的设计：`load()` 后框架将实体 `_IS_LOADED` 置 true，此后只有被 setter 调用过的字段进入 UPDATE 的 SET 子句，未改字段不入 SQL。

- 通过 `load` 获取的实体：只更新 setter 触发过的字段。
- 直接 `new` 出的实体（`_IS_LOADED=false`）：update 会写入全部非主键字段。

若需强制更新某字段为 null，确保该字段 setter 被调用过即可（差量记录 oldValue≠newValue）。
