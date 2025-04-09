[TOC]

# 简介

uw-dao包是一个封装数据库操作的类库，比hibernate效率高，比mybatis更简单，并一致化管理数据库连接池。

# 主要特性

1. 支持多数据库连接，支持mysql/oracle/sqlserver，支持基于表名的访问规则配置，便于分库分表。
2. 为了适配多数据库连接而改进的连接池，线程数少且节省资源，同时支持对于异常SQL的监控，便于整体控制数据库连接数。内测比druid更利索一些。
3. 非常类似hibernate的jpa的CRUD操作，以及非常类似mybatis的SQL映射实现，调用更加简单和直接。以上基于反射实现，已经使用缓存来保证效率了，木有泄漏。
4. 更直接和爽快的事务支持和批量更新支持，但是用起来要小心点哦，必须要用try.catch.finally规范处理异常。
5. 运维特性支持，可以监控每一条sql的执行情况，各种报表都可以做，比如slow-query，bad-query等等。。
6. 内部有一个CodeGen用于直接从数据库生成entity类，方便。

# maven引入

```
<dependency>
	<groupId>com.umtone</groupId>
	<artifactId>uw-dao</artifactId>
	<version>5.0.x</version>
</dependency>
```

# 配置文件

```yaml
uw:
  dao:
    # 连接池
    conn-pool:
      # 连接池列表，可以同时配置多个连接池，排在第一个的为默认连接池，名字为必须为default。
      default:
        driver: com.mysql.jdbc.Driver
        url: jdbc:mysql://localhost:3306/task?characterEncoding=utf-8&useSSL=false&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true
        username: root
        password: root
        # 测试sql，用于测试连接是否可用
        test-sql: select 1
        # 最小连接数
        min-conn: 1
        # 最大连接数
        max-conn: 10
        # 连接闲时超时秒数，默认值为60s
        conn-idle-timeout: 600
        #连接忙时超时秒数，默认值为60s
        conn-busy-timeout: 600
        # 连接最大寿命秒数，默认值为3600s 
        conn-max-age: 1800
    # 数据库访问连接路由配置，值为连接池名
    # all是所有访问方法，一般情况下会先匹配write/read方法，找不到的情况下才会匹配all方法
    # write是和写有关的方法，如insert,update,delete
    # read是和读有关的方法，如select
    conn-route:
      root:
        all: default
        write: default
        read: default
      # 路由列表
      list:
        # 用表名前缀来指定数据库连接池
        test_:
          all: test
          write: test
          read: test
    # 自动分表设置。
    table-sharding:
      task_runner_log:
        sharding-type: date
        sharding-rule: day
        auto-gen: true
    sql-stats:
      enable: true
      data-keep-days: 100
```

# 功能入口

```
//基于ResponseData返回数据，异常封装在ResponseData中。
DaoManager dao = DaoManager.getInstance();
//传统异常抛出的调用方式。
DaoFactory dao = DaoFactory.getInstance();
```
所有的数据库访问操作，都从dao开始。在不使用事务的情况下，dao线程安全，是可以共用的。
对新项目，建议使用DaoManager，这将会带来更好的安全性。

DaoFactory VS DaoManager
* DaoFactory 基于异常处理的传统调用办法，通过抛出TransactionException来处理异常。
* DaoManager 基于ResponseData来返回数据，将异常封装在ResponseData中，这将避免潜在攻击行为。

这导致了两种不同方式的调用代码。
* DaoFactory 可以一锅乱烩的方式在最外围层做异常处理，代码可能会简单。
* DaoManager 需要处理每个异常，但是可以通过Controller层做统一异常处理，代码会更简单。

基于DaoFactory的代码做DaoManager改造的注意点：
* 对大多数代码，可以简单的增加.getData()进行适配。因为上一级调用代码一般通过判断!=null来判断是否成功。
* DaoManager同时提供了getDaoFactory()接口直接获得DaoFactory。
* 对于controller，一般建议返回值为ResponseData，这样代码会更简单。


# 实体类操作

实体类操作一般会有多个参数重载，要求实体类必须是DataEntity类型。
可以指定连接名和表名，这样是为了提高灵活性。
对于大多数项目来说，可以通过ConnectionRouter来指定连接名。

## 插入记录

```java
    /**
 * 保存一个Entity实例，等效于insert。
 * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
 * @param entity 要更新的对象
 * @param tableName 指定表名
 * @return
 * @throws TransactionException
 */
public abstract<T extends DataEntity> T save(String connName,T entity,String tableName)
        throws TransactionException;
```

## 批量插入记录

```java
/**
 * 保存多个Entity实例，等效于insert values()().
 * 但是注意my-db分表使用第一个()的路由规则 以及sqlserver数据库values()参数或者说总字段数量最多2000个限制
 * @param <T>       映射的类型
 * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
 * @param entityList 要保存的Entity实例集合
 * @param tableName 指定表名
 * @return Entity实例集合
 * @throws TransactionException 事务异常
 */
public abstract<T extends DataEntity> List<T> save(String connName,List<T> entityList,String tableName)
        throws TransactionException;
```

## 修改记录

```java
    /**
 * 根据主键更新一个Entity实例，等效于update。
 * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
 * @param entity 要更新的对象
 * @param tableName 指定表名
 * @return
 * @throws TransactionException
 */
public abstract<T extends DataEntity> int update(String connName,T entity,String tableName)
        throws TransactionException;
```

## 删除记录

```java
    /**
 * 根据主键删除一个Entity实例，等效于delete。
 * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
 * @param entity 要更新的对象
 * @param tableName 指定表名
 * @return
 * @throws TransactionException
 */
public abstract<T extends DataEntity> int delete(String connName,T entity,String tableName)
        throws TransactionException;
```

## 载入记录

```java
    /**
 * 根据指定的主键ID载入一个Entity实例。
 * @param cls 要映射的对象类型
 * @param tableName 指定表名
 * @param id 主键数值
 * @return
 * @throws TransactionException
 */
public abstract<T> Optional<T> load(Class<T> cls,String tableName,Serializable id)throws TransactionException;
```

## 列表查询

```java
    /**
 * 根据指定的映射类型，返回一个DataList列表。
 * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
 * @param cls 要映射的对象类型
 * @param selectSql 查询的SQL
 * @param paramList 查询SQL的绑定参数
 * @param startIndex 开始位置，默认为0
 * @param resultNum 结果集大小，默认为0，获取全部数据
 * @param autoCount 是否统计全部数据（用于分页算法），默认为false。
 * @return
 * @throws TransactionException
 */
public abstract<T> DataList<T> list(String connName,Class<T> cls,String selectSql,Object[]paramList,
        int startIndex,int resultNum,boolean autoCount)throws TransactionException;
```

# QueryParam详解

QueryParam: 查询参数基类。
PageQueryParam: 带分页参数的基类。

QueryParam通过注解来自动生成sql。

通过uw-code-center可以根据库表结构自动生成QueryParam的dto类，作为代码基可以做针对业务需求的修改。
代码生成的规则如下：
1.对于Date类型，生成DateRange的查询参数，支持基于between的范围查询。
2.对于state字段，生成state,states(支持in查询)，stateGte(大于等于)，stateLte(小于等于)的方法。
3.对于Saas专用版，自动屏蔽SaasId字段。

## QueryParam的注解使用。

0.基本匹配。
```java
/**
 * 应用状态1: 上线; 0: 下线 -1:删除
 */
@QueryMeta(expr = "state=?")
@Schema(title = "应用状态1: 上线; 0: 下线 -1:删除", description = "应用状态1: 上线; 0: 下线 -1:删除")
private Integer state;

```

1.基于数值展开类型。解析器将会针对占位符数量对参数值展开。
```java
/**
 * 应用名称
 */
@QueryMeta(expr = "(app_name like ? or app_desc like ?)")
@Schema(title = "应用名称", description = "应用名称")
private String appInfo;
```

2.基于占位符展开类型。解析器将会针对数组大小对占位符展开。
```java
/**
* 应用状态1: 上线; 0: 下线 -1:删除
*/
@QueryMeta(expr = "state in (?)")
@Schema(title = "应用状态1: 上线; 0: 下线 -1:删除", description = "应用状态1: 上线; 0: 下线 -1:删除")
private Integer[] states;
```

3.数值不参与计算。
```java
/**
* 应用状态1: 上线; 0: 下线 -1:删除
*/
@QueryMeta(expr = "state>=0")
@Schema(title = "应用状态 ", description = "应用状态")
private Boolean stateOn;
```

## 针对like查询的限定。
当触发限定后，将自动转换like为=，并去除参数值中的%。
LIKE_QUERY_ENABLE: 是否开启LIKE查询，默认打开。
LIKE_QUERY_PARAM_MIN_LEN: like参数数值最小长度，默认为5。

## 附加参数。
QueryParam支持链式调用。

ADD_SELECT_SQL(String sql) 可以重载where之前的select * from table语句，如果不指定，则通过EntityBean的注解提取Select语句。
ADD_EXT_WHERE_SQL(String whereSql) 添加附加的条件sql，可以多次添加。
ADD_EXT_PARAM(String paramCond, Object paramValue) 添加额外的参数对，paramValue支持传入数组和List。

# 一般SQL操作

## 执行返回DataSet（多行多列）的查询

```java
    /**
 * 返回一个DataSet数据列表。
 * 相比较DataList列表，这不是一个强类型列表，但是更加灵活。
 * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
 * @param selectSql 查询的SQL
 * @param paramList 查询SQL的绑定参数
 * @param startIndex 开始位置，默认为0
 * @param resultNum 结果集大小，默认为0，获取全部数据
 * @param autoCount 是否统计全部数据（用于分页算法），默认为false。
 * @return
 * @throws TransactionException
 */
public abstract DataSet queryForDataSet(String connName,String selectSql,Object[]paramList,int startIndex,
        int resultNum,boolean autoCount)throws TransactionException;
```

## 执行返回List（多行单列）的查询

```java
    /**
 * 查询单个基本数值列表（多行单个字段）。
 * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
 * @param cls 要映射的基础类型，如int.class,long.class,String.class,Date.class
 * @param selectSql 查询的SQL
 * @param paramList 查询SQL的参数
 * @return
 * @throws TransactionException
 */
public abstract<T> List<T> queryForSingleList(String connName,Class<T> cls,String sql,Object...paramList)
        throws TransactionException;
```

## 执行返回单个基本数值的查询

```java
    /**
 * 查询单个基本数值（单个字段）。
 * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
 * @param cls 要映射的基础类型，如int.class,long.class,String.class,Date.class
 * @param sql 查询的SQL
 * @param paramList 查询SQL的参数
 * @return
 * @throws TransactionException
 */
public abstract<T> Optional<T> queryForSingleValue(String connName,Class<T> cls,String sql,Object...paramList)
        throws TransactionException;
```

## 执行任意sql语句

```java
    /**
 * 执行一条SQL语句。
 * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
 * @param sql 查询的SQL
 * @param paramList 查询SQL的参数
 * @return 影响的行数
 * @throws TransactionException
 */
public abstract int executeCommand(String connName,String sql,Object...paramList)throws TransactionException;
```

# DataList VS DataSet

* DataList优于DataSet，优先使用;
* DataSet用于兼容代码，性能略低于DataList，内存占用大。

# DaoSequenceFactory VS FusionSequenceFactory。
SequenceFactory工厂类通过配置文件动态决定使用 DaoSequenceFactory，还是 FusionSequenceFactory。
1. FusionSequenceFactory 可以获得连续的Sequence数值，DaoSequenceFactory 集群环境下是不连续的。
2. FusionSequenceFactory 默认配置下性能是 DaoSequenceFactory 的100、倍。
3. DaoSequenceFactory的incrementNum=100的时候和FusionSequenceFactory性能平衡点，超过100则性能大于FusionSequenceFactory。
4. FusionSequenceFactory 和 DaoSequenceFactory 不可以混用，超过200线程下可能会出现ID重复，如果解决此问题将会大大降低性能。
5. ResetSequenceId方法要非常谨慎使用，最好提前设置，否则可能引发可能的ID重复问题。
