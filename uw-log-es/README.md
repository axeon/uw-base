[TOC]

# uw-log-es

Elasticsearch(下文简称es)是一个基于Lucene的搜索服务器,其强大的索引功能,自动支持分片、压缩、负载均衡、自动数据恢复,复制功能,可靠的集群功能,基本上是开箱即用。

#### 项目简介

uw-log-es的主要特性:

1. 使用uw-httpclient做REST构建查询,支持Http Basic 验证;
2. 支持多种方式批量写入,完全自动化构建日志内容;
3. 支持多种方式查询,并映自动射到日志对象;

#### 项目引入

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-log-es</artifactId>
    <version>${project.version}</version>
</dependency>
```

#### 项目配置

```yaml
uw:
  log:
    es:
      # 如果不配置 server 地址,将不会把日志发送到es服务端
      server: http://localhost:9200
      # 如果不配置用户名和密码,将不会有Http Basic验证头
      username: admin
      password: admin
      # READ_ONLY: 只读模式; READ_WRITE: 读写模式[会有后台线程开销]
      mode: READ_WRITE
      # 刷新Bucket时间毫秒数
      max-flush-in-milliseconds:
      # 允许最大Bucket 字节数
      max-bytes-of-batch: 5*1024*1024
      # 最大批量线程数
      max-batch-threads: 3
```

#### 基本使用

##### 初始化注册日志对象

```java
/**
 * uw-log-es配置,主要用来注册日志对象
 *
 * 
 * @since 2018-05-03
 */
@Service
public class MyLoginLogClient {
    
    
    public void initMyLoginlogClient(final LogClient logClient) {
        logClient.regLogObject(MscLoginLog.class);
    }
}
```

##### 日志写入

```java
public class DemoWriteLog {
    
    @org.springframework.beans.factory.annotation.Autowired
    private uw.log.es.LogClient logClient;
    
    /**
    * 写单条日志 
    */
    public void log() {
        MscLoginLog loginLog = new MscLoginLog();
        // 写日志...
        logClient.log(loginLog);
    }
    
    /**
    * 批写日志 
    */
    public void bulkLog() {
        List<MscLoginLog> dataLists = new ArrayList<MscLoginLog>();
        // 写日志...
        logClient.bulkLog(loginLog);
    }
}
```

##### 日志查询

在构建查询前,先选择你熟悉的查询方式,uw-log-es目前支持:

1. 简单查询日志（废弃）:

2. 转换SQL为DSL:

```java
public class LogClientDSLTest {
    @Test
    public void testSqlToDsl() throws Exception {
        System.out.println(logClient.translateSqlToDsl("select * from \\\"saas-hotel-task_20191217\\\" where level = 'ERROR'", 10, 10));
    }
}
```

3. DSL(Domain Specific Language)查询:

```java
public class LogClientDSLTest {
    /**
    * DSL查询,注意要带分页from,size参数。
    * PS: 对于在查询应用中,此方法查询要求先注册日志对象
    */
    @Test
        public void testDslSearch() throws Exception {
            String dsl = logClient.translateSqlToDsl("select * from \\\"saas-hotel-task_20191217\\\"", 10, 10);
            logClient.dslQuery(TaskRunnerLog.class, "uw.auth.server.vo.msc_action_log_20191217", dsl);
        }
}
```

4. SQL(Structured Query Language)查询 （未支持）:

5. Scroll API查询 :

```java
public class LogClientDSLTest {
    /**
    *  开始scroll查询
    * @throws Exception
    */
     @Test
    public void testScroll() throws Exception {
        String dsl = logClient.translateSqlToDsl("select * from \\\"saas-hotel-task_20191217\\\"", 0, 10);
        ScrollResponse<TaskRunnerLog> taskRunnerLogScrollResponse = logClient.scrollQueryOpen(TaskRunnerLog.class, "uw.auth.server.vo.msc_action_log_20191217", 60, dsl);
        System.out.println(taskRunnerLogScrollResponse);
    }
        
    /**
    *  接着scroll查询
    */
     @Test
    public void testScrollNext() {
        ScrollResponse<TaskRunnerLog> taskRunnerLogScrollResponse = logClient.scrollQueryNext(TaskRunnerLog.class, index, "DXF1ZXJ5QW5kRmV0Y2gBAAAAAAAFqj0WbTg3Z180Q1FRUHEwelZjbUI0NmROQQ==", 60);
        System.out.println(taskRunnerLogScrollResponse);

    }
    
    
    /**
    * 关闭scroll查询
     */
    @Test
    public void deleteScroll() {
        DeleteScrollResponse deleteScrollResponse = logClient.scrollQueryClose("DnF1ZXJ5VGhlbkZldGNoBAAAAAAABXUDFm04N2dfNENRUVBxMHpWY21CNDZkTkEAAAAAAAV1BBZtODdnXzRDUVFQcTB6VmNtQjQ2ZE5BAAAAAAAFdQUWbTg3Z180Q1FRUHEwelZjbUI0NmROQQAAAAAABXUZFm04N2dfNENRUVBxMHpWY21CNDZkTkE="), index;
        System.out.println(deleteScrollResponse);
    }
    
        
}
```

##### 日志分页查询

es本身对分页查询支持良好,使用简单查询和DSL查询默认是取10条

项目中常见的分页查询是前端的列表查询,可以使用带SearchResponse后缀的方法将数据查出来,然后使用[uw-dao](http://192.168.88.88:10080/uw/uw-dao "数据库操作的类库")
提供的DataList工具类进行包装即可,比如

```java
public class DemoPaginationQuery {

    /**
     * SQL分页查询示例
     */
    public DataList<MscLoginLog> list(@RequestParam(name = "page", defaultValue = "1") int page,
                                      @RequestParam(name = "resultNum", defaultValue = "10") int resultNum) {
        String dsl = logClient.translateSqlToDsl(
                "select * from " + logClient.getLogObjectIndex(MscLoginLog.class) + " where loginDate > 1524666600000 ", (page - 1) * resultNum, resultNum);
        SearchResponse<MscLoginLog> response = logClient.dslQuery(MscLoginLog.class, logClient.getLogObjectIndex(MscLoginLog.class), dsl);
        // 组装分页参数
        if (response != null && response.getHisResponse() != null) {
            int total = response.getHisResponse().getTotal().getValue();
            List<SearchResponse.Hits<TaskRunnerLog>> hitsList = response.getHisResponse().getHits();
            if (!hitsList.isEmpty()) {
                List<TaskRunnerLog> dataList = Lists.newArrayList();
                for (SearchResponse.Hits<TaskRunnerLog> hits : hitsList) {
                    dataList.add(hits.getSource());
                }
                return new DataList<TaskRunnerLog>(dataList, startIndex, resultNum, total);
            }
        }
    }
}
```

##### 学习的es的建议

因为es版本目前迭代非常快。。。不要上百度搜文档了,搜出来的可能解决不了你的问题(因为版本不一致)
,建议参考[官方文档](https://www.elastic.co/guide/en/elasticsearch/guide/current/index.html "Elasticsearch: The Definitive Guide"),[中文版](https://github.com/elasticsearch-cn/elasticsearch-definitive-guide "Elasticsearch: The Definitive Guide")
,中文文档翻译可能与最新版的原文档有出入,但是基本够用。

#### 常见问题

1.Q: 日志没有被记录到es?

A: 是否在uw-log-es初始化时调用LogClient.regLogObject方法注册日志对象

2.Q: uw-log-es写日志失败401?

A: uw-log-es在es服务端配置了Http Basic验证,需要配置用户名和密码。

#### 2023/3/16 依赖升级

| 库                                  | 升级前版本  | 升级后版本 |
|------------------------------------|--------|-------|
| uw-httpclient                      | 1.0.30 | 2.0.1 |
| 删除commons-lang3，依赖迁移到uw-httpclient |        |       |
| 删除jackson，依赖迁移到uw-httpclient       |        |       |

单元测试ES遇到的一些问题解决思路

https://blog.csdn.net/weixin_38650077/article/details/129116034

http改为https才可以

单元测试

遇到这种报错

```
SunCertPathBuilderException: unable to find valid certification path to requested target
```

修改config目录下的[elasticsearch](https://so.csdn.net/so/search?q=elasticsearch&spm=1001.2101.3001.7020)
.yml文件中xpack.security.http.ssl.enable:true 为false（修改后只能用http）

```yml
# Enable encryption for HTTP API client connections, such as Kibana, Logstash, and Agents
xpack.security.http.ssl:
  enabled: false
  keystore.path: certs/http.p12
```

