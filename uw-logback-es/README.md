[TOC]

# uw-logback-es

项目主要是为了直接把日志发向es服务集群,而不再需要用logstash收集,方便优化应用程序日志

## 工作原理

- 业务线程在 `append()` 中把单条日志编码到临时 buffer,再加锁追加到全局 `okio.Buffer`(锁持有时间极短)。
- 后台监控线程 `ElasticsearchDaemonExporter`(默认 500ms 一轮)判断 flush 条件,满足时把 flush 任务提交到线程池。
- flush 触发条件二选一:buffer 达到 `maxKiloBytesOfBatch`(单位 KB),或距上次 flush 超过 `maxFlushInSeconds` 秒。
- flush 任务通过 ES `_bulk` 接口以 NDJSON 批量写入;HTTP 失败仅记录错误,本批数据会丢失(无重试/落盘)。
- 所有 JSON 字段值均经过转义,避免破坏 NDJSON 结构。

## 注意事项

- **HTTP 同步阻塞**:`HTTP_INTERFACE` 配置为信任全部证书 + 10s 超时 + 连接失败重试。ES 慢或不可达时,单次 flush 可能阻塞数十秒。
- **失败丢数据**:flush 失败(网络异常或非 200)仅通过 logback StatusManager 记录错误,本批数据无重试/落盘兜底,会被丢弃。
- **错误观测**:appender 自身的错误(配置缺失、flush 失败)写入 logback `StatusManager`,需在 logback 配置中加 `debug="true"` 或 `<statusListener>` 才能看到。
- **属性注入**:logback 按 setter 名注入,XML 标签名需与 `setXxx` 的 `xxx` 一致(如 `<esServer>` 对应 `setEsServer`),否则属性不会生效。

## 程序使用配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration>
<configuration>
    <springProfile name="default">
        <appender name="ES"
                  class="uw.logback.es.appender.ElasticSearchAppender">
            <!-- ES 服务地址(必填),如 http://localhost:9200 -->
            <esServer>http://localhost:9200</esServer>
            <!-- ES bulk 接口路径(可选,默认 /_bulk?filter_path=took,errors) -->
            <esBulk>/_bulk?filter_path=took,errors</esBulk>
            <!-- ES Basic 认证用户名(可选) -->
            <esUsername>elastic</esUsername>
            <!-- ES Basic 认证密码(可选) -->
            <esPassword>changeme</esPassword>
            <!-- 索引名(可选,为空时默认使用 appInfo) -->
            <esIndex>uw-auth-center2</esIndex>
            <!-- 索引后缀(可选),支持 FastDateFormat 时间格式,如 _yyyy-MM-dd,会拼成 索引名+后缀 -->
            <esIndexSuffix>_yyyy-MM-dd</esIndexSuffix>
            <!-- 应用名称(必填),写入日志的 appInfo 字段,esIndex 为空时同时作为索引名 -->
            <appInfo>uw-auth-center2</appInfo>
            <!-- 应用主机(可选),写入日志的 appHost 字段 -->
            <appHost>${host}</appHost>
            <!-- 堆栈输出最大深度(行数),小于 10 会被强制提升到 10,默认 20 -->
            <maxDepthPerThrowable>20</maxDepthPerThrowable>
            <!-- 需要折叠的堆栈类名前缀,逗号分隔,匹配的行会被合并计数后省略 -->
            <excludeThrowableKeys>java.base,org.spring,jakarta,org.apache,com.mysql,okhttp,com.fasterxml,uw.auth.service.filter</excludeThrowableKeys>
            <!-- 批量提交触发阈值,单位 KB:buffer 累积达到该 KB 数触发 flush,默认 8192(即 8MB) -->
            <maxKiloBytesOfBatch>8192</maxKiloBytesOfBatch>
            <!-- 定时刷新时间间隔,单位:秒,默认 10 -->
            <maxFlushInSeconds>10</maxFlushInSeconds>
            <!-- 批量提交最大线程数,默认 5 -->
            <maxBatchThreads>5</maxBatchThreads>
            <!-- 批量提交线程池队列容量,默认 20 -->
            <maxBatchQueueSize>20</maxBatchQueueSize>
            <!-- 开启 JMX 监控支持,默认 false -->
            <jmxMonitoring>true</jmxMonitoring>
        </appender>
        <root level="INFO">
            <appender-ref ref="ES"/>
        </root>
    </springProfile>
</configuration>
```

#### 配置项说明

| XML 标签 | 属性 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `esServer` | esServer | 是 | - | ES 服务地址 |
| `esBulk` | esBulk | 否 | `/_bulk?filter_path=took,errors` | ES bulk 接口路径 |
| `esUsername` | esUsername | 否 | - | Basic 认证用户名(与密码同时配置才生效) |
| `esPassword` | esPassword | 否 | - | Basic 认证密码 |
| `esIndex` | esIndex | 否 | =appInfo | 索引名 |
| `esIndexSuffix` | esIndexSuffix | 否 | - | 索引时间后缀,如 `_yyyy-MM-dd` |
| `appInfo` | appInfo | 是 | - | 应用名,写入 appInfo 字段;esIndex 为空时同时作索引名 |
| `appHost` | appHost | 否 | - | 主机名,写入 appHost 字段 |
| `maxDepthPerThrowable` | maxDepthPerThrowable | 否 | 20 | 异常堆栈输出最大深度,小于 10 提升至 10 |
| `excludeThrowableKeys` | excludeThrowableKeys | 否 | 见代码 | 折叠的堆栈类名前缀(逗号分隔) |
| `maxKiloBytesOfBatch` | maxKiloBytesOfBatch | 否 | 8192 (8MB) | buffer 达到此 KB 数触发 flush |
| `maxFlushInSeconds` | maxFlushInSeconds | 否 | 10 | 定时 flush 间隔,单位:秒 |
| `maxBatchThreads` | maxBatchThreads | 否 | 5 | 批量提交线程数 |
| `maxBatchQueueSize` | maxBatchQueueSize | 否 | 20 | 批量提交线程池队列容量 |
| `jmxMonitoring` | jmxMonitoring | 否 | false | 是否开启 JMX |

> 注:logback 通过 setter 名注入属性(标签名须与 `setXxx` 的 `xxx` 一致,如 `<esServer>` 对应 `setEsServer`)。
