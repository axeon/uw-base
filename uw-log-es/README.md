[TOC]

# uw-log-es

基于 [uw-httpclient](../uw-httpclient) 封装的 Elasticsearch 日志客户端，提供日志的**批量写入**与 **DSL / SQL / Scroll 查询**能力，并自动映射到日志对象。

> Elasticsearch（下文简称 es）是基于 Lucene 的搜索服务器，天然支持分片、压缩、负载均衡与自动恢复，基本开箱即用。

## 项目简介

主要特性：

1. 基于 uw-httpclient 构建 REST 请求，支持 Http Basic 认证；
2. 后台守护线程 + buffer 聚合的**批量写入**，按时间阈值或字节阈值自动 flush；
3. 支持 **DSL 查询**、**SQL 转 DSL**、**Scroll 游标查询**，结果自动反序列化为日志对象；
4. 提供聚合结果转换与分页映射工具方法。

## 项目引入

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-log-es</artifactId>
    <version>${project.version}</version>
</dependency>
```

引入后由 `LogClientAutoConfiguration` 自动装配 `LogClient` 单例，业务侧通过 `LogClient.getInstance()` 或注入获取。

## 项目配置

配置前缀 `uw.log.es`：

```yaml
uw:
  log:
    es:
      # ES 集群 HTTP REST 地址；不配置 server 时不会写入日志，仅保留查询能力
      server: http://localhost:9200
      # Http Basic 认证（username 与 password 同时配置时生效）
      username: admin
      password: admin
      # READ_ONLY: 只读模式（不起后台写入线程）; READ_WRITE: 读写模式
      mode: READ_WRITE
      # 是否用应用信息覆写日志体中的 appInfo/appHost
      app-info-overwrite: true
      # 后台批量提交的刷新间隔（秒），默认 10
      max-flush-in-seconds: 10
      # 触发立即提交的 buffer 阈值（KB），默认 8192（即 8MB）
      max-kilo-bytes-of-batch: 8192
      # 批量提交线程池最大线程数，默认 5
      max-batch-threads: 5
      # 批量提交线程池队列容量，默认 20
      max-batch-queue-size: 20
      # ES 连接/读/写超时（毫秒），默认 30000
      connect-timeout: 30000
      read-timeout: 30000
      write-timeout: 30000
      # bulk api 路径（相对 server）
      es-bulk: /_bulk?filter_path=took,errors
```

> 单位提醒：`max-kilo-bytes-of-batch` 为 **KB**，`max-flush-in-seconds` 为 **秒**。

`appInfo` 取自 `${spring.application.name}:${project.version}`，`appHost` 取自 `${spring.cloud.nacos.discovery.ip}:${server.port}`，均有默认值，未接入 nacos 也不会启动报错。

## 基本使用

### 1. 定义日志对象

日志对象须继承 `LogBaseVo`，公共字段（`@timestamp` / `appInfo` / `appHost` / `logLevel`）由基类提供，写入时自动补齐。

```java
public class MscLoginLog extends LogBaseVo {
    private long userId;
    private String loginIp;
    private int status;
    // getter/setter ...
}
```

`logLevel` 参考 `uw.log.es.vo.LogLevel`：`NONE(-1)` 不记录、`BASE(0)` 基本信息……值越大记录信息越多。`LogClient.log()` 仅在 `logLevel > NONE` 时才写入。

### 2. 注册日志类型

在应用启动期（`static` 块或 `@PostConstruct`）注册，索引名默认按类名 lower_underscore 推导，也可自定义或按时间分索引：

```java
@Service
public class LogClientInitializer {
    @PostConstruct
    public void init() {
        // 方式一：索引名按类名推导
        LogClient.getInstance().regLogObject(MscLoginLog.class);
        // 方式二：自定义索引名
        LogClient.getInstance().regLogObjectWithIndexName(MscLoginLog.class, "msc_login_log");
        // 方式三：按时间分索引（写入追加 yyyyMM 后缀，查询用 原名_* 通配）
        LogClient.getInstance().regLogObjectWithIndexPattern(MscLoginLog.class, "yyyyMM");
    }
}
```

### 3. 写入日志

```java
@Autowired
private LogClient logClient;

public void log() {
    MscLoginLog loginLog = new MscLoginLog();
    loginLog.setLogLevel(LogLevel.BASE.getValue());
    loginLog.setUserId(10001L);
    loginLog.setLoginIp("10.8.8.89");
    logClient.log(loginLog);          // 单条写入，经 buffer 聚合后批量提交
}

public void bulkLog() {
    List<MscLoginLog> list = new ArrayList<>();
    // 填充列表 ...
    logClient.bulkLog(list);          // 批量写入
}
```

### 4. 查询日志

#### DSL 查询

```java
String dsl = "{\"query\":{\"term\":{\"userId\":10001}},\"from\":0,\"size\":10}";
SearchResponse<MscLoginLog> resp = logClient.dslQuery(MscLoginLog.class, dsl);
// resp.getHitResponse().getHits() 取命中
```

#### SQL 转 DSL

将 SQL 交给 ES 的 `_sql/translate` 翻译为 DSL，再自行或交由 `dslQuery` 执行。表名（索引名）需用 `getQuotedQueryIndexName` 转义；SQL **不可包含 limit**。

```java
String index = logClient.getQuotedQueryIndexName(MscLoginLog.class);
String dsl = logClient.translateSqlToDsl(
        "select * from " + index + " where status = 1", 0, 10, true);
// startIndex=0 不附加 from；resultNum=10 拼接 limit 10；trueCount 附加 track_total_hits:true
SearchResponse<MscLoginLog> resp = logClient.dslQuery(
        MscLoginLog.class, logClient.getQueryIndexName(MscLoginLog.class), dsl);
```

#### Scroll 游标查询（大数据量导出）

> 注意：scroll 的 DSL **不能包含 from 节点**；使用完毕**必须关闭** scroll 以释放 ES 资源。

```java
String index = logClient.getQueryIndexName(MscLoginLog.class);
String dsl = "{\"query\":{\"match_all\":{}},\"size\":1000}";
ScrollResponse<MscLoginLog> scroll = logClient.scrollQueryOpen(MscLoginLog.class, index, 60, dsl);
try {
    while (scroll != null && scroll.getHitResponse() != null
            && !scroll.getHitResponse().getHits().isEmpty()) {
        for (SearchResponse.Hit<MscLoginLog> hit : scroll.getHitResponse().getHits()) {
            // 处理 hit.getSource()
        }
        scroll = logClient.scrollQueryNext(MscLoginLog.class, index, scroll.getScrollId(), 60);
    }
} finally {
    if (scroll != null) {
        logClient.scrollQueryClose(scroll.getScrollId(), index);
    }
}
```

### 5. 分页查询

ES 查询默认仅返回 10 条，分页查询可通过 `from` / `size` 控制，并用工具方法转换为 `PageList`：

```java
public PageList<MscLoginLog> list(int page, int resultNum) {
    int startIndex = (page - 1) * resultNum;
    String index = logClient.getQuotedQueryIndexName(MscLoginLog.class);
    String dsl = logClient.translateSqlToDsl(
            "select * from " + index + " where loginDate > 1524666600000",
            startIndex, resultNum, true);
    SearchResponse<MscLoginLog> resp = logClient.dslQuery(
            MscLoginLog.class, logClient.getQueryIndexName(MscLoginLog.class), dsl);
    return LogClient.mapQueryResponseToPageList(resp, startIndex, resultNum);
}
```

### 6. 聚合查询

```java
String dsl = "{\"size\":0,\"aggs\":{\"by_status\":{\"terms\":{\"field\":\"status\"}}}}";
SearchResponse<MscLoginLog> resp = logClient.dslQuery(MscLoginLog.class, dsl);
// 拉平为 聚合名+桶key -> 文档数
Map<String, Double> flat = LogClient.convertAggBucketFlatMap(resp.getAggregations());
// 或保留桶结构：聚合名 -> [{name, count, 子聚合名:值}]
Map<String, List<Map<String, Object>>> buckets = LogClient.convertAggBucketListMap(resp.getAggregations());
```

## 常见问题

**Q: 日志没有被记录到 ES？**
- 是否在启动期调用 `LogClient.regLogObject` 注册了日志类型？
- `mode` 是否为 `READ_WRITE`（`READ_ONLY` 不写入）？
- `logLevel` 是否大于 `NONE(-1)`？
- `server` 是否已配置？

**Q: 写日志失败 401？**
- ES 服务端开启了 Http Basic 验证，需同时配置 `username` 与 `password`。

**Q: scroll 查询报错？**
- scroll 的 DSL 不能含 `from` 节点；且使用完务必 `scrollQueryClose`。

**Q: SQL 转 DSL 报错？**
- SQL 不可包含 `limit`（由 `resultNum` 控制）；表名需用 `getQuotedQueryIndexName` 转义。

## 学习建议

ES 版本迭代很快，建议直接参考 [官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)。中文翻译可能与最新版本有出入，但基本够用。

## 更新历史

见 [CHANGELOG.md](CHANGELOG.md)。
