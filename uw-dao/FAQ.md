#### 1.概括一下uw-dao的主要功能和特性。

 uw-dao包是一个封装数据库操作的类库，比hibernate效率高，比mybatis更简单，并一致化管理数据库连接池。 

1. 支持多数据库连接，支持mysql/oracle/sqlserver，支持基于表名的访问规则配置，便于分库分表。
2. 为了适配多数据库连接而改进的连接池，线程数少且节省资源，同时支持对于异常SQL的监控，便于整体控制数据库连接数。
3. 非常类似hibernate的jpa的CRUD操作，以及非常类似mybatis的SQL映射实现，调用更加简单和直接。以上基于反射实现，已经使用缓存来保证效率了，木有泄漏。
4. 更直接和爽快的事务支持和批量更新支持，但是用起来要小心点，必须要用try.catch.finally规范处理异常。
5. 运维特性支持，可以监控每一条sql的执行情况，各种报表都可以做，比如slow-query，bad-query等等。
6. 内部有一个CodeGen用于直接从数据库生成entity类，方便。

#### 2.uw-dao的自动分表是根据什么进行分表？该如何配置？

根据日期分表。

```yml
# 自动分表设置。
table-sharding:
  # 表名，可以配置多个
  task_runner_log:
    # 分片类型。 当前仅支持date类型.
    sharding-type: date
    # 分片规则。当前仅支持day,month,year类型.
    sharding-rule: day
    # 是否自动建表
    auto-gen: true
```

#### 3.uw-dao的所有数据库访问功能入口是什么？获取到的dao实例是否线程安全，是否可以共用？什么场景下是不可共用的？

```java
 DAOFactory dao = DAOFactory.getInstance();
```

dao实例是线程安全的。

在不使用事务的情况下，dao是可以共用的。

#### 4.如果配置多个数据库连接的情况下，在不指定的连接名的情况下，如何配置访问特定表的时候使用哪个连接？

在不指定的连接名的情况下，根据conn-route配置的表名前缀，根据表名或者分析sql语句确定使用哪个连接。

```yml
conn-route:
  # 用表名前缀来指定数据库连接池
  test_:
    # 全权限连接
    all: test
    # 写入连接
    write: test
    # 读连接
    read: test
```

#### 5.SequenceFactory的参数incrementNum有什么作用？incrementNum越大越好吗？

incrementNum是序列增量数，从数据库获取序列的时候会先占用该数量的序列，减少操作数据库的数量。incrementNum越大，数据库操作开销越小，对于插入频繁的表，使increment的数值>=100，可以显著提高sequence性能。但如果incrementNum较大，服务刚占用了较多序列就宕机或者重启，就会导致序列之间出现较大空档。increment数值的调整通过管理后台或数据库操作。

incrementNum和tps的关系公式为 tps = incrementNum*(30~50)。

#### 6.uw-dao中DataList和DataSet类的使用场景差别？

DataSet数据列表。相比较DataList列表，这不是一个强类型列表，但是更加灵活。

DataList性能上优于DataSet，优先使用。

DataSet用于兼容代码，类似mybatis的ResultSet，性能略低于DataList。

#### 7.PageQueryParam类有什么作用？和QueryParam有什么区别？

PageQueryParam定义了分页查询参数，实现了父类QueryParam，用于前端传递参数进行分页查询；dao框架会根据entityCls中TableMeta注解的sql属性和QueryPageParam中属性的QueryMeta信息来自动生成sql和分页参数。
其中属性page为查询的页码，resultNum为每页条数，startIndex为起始位置，sortName为排序字段，sortType为排序类型（0:不排序, 1:顺序, 2:倒序），requestType为请求类型，决定了返回内容（0:仅分页信息, 1:仅数据, 2:全部）

QueryParam查询参数的接口类，主要用于标识子类为查询参数，不具备PageQueryParam的分页功能。

#### 8.假如有一个user表，如何编写一个可以根据userName模糊查询和根据age范围查询的分页查询参数类？

```java
public class userNameAgeQueryParam implements PageQueryParam {

        @QueryMeta(expr = "userName like ?")
        private String name;

        @QueryMeta(expr = "age between ? and ?")
        private Integer[] age;

        //getter,setter

    }
```

#### 9.在集合类型的字段上使用@QueryMeta注解，如果sql中要使用in的时候该如何写占位符？

```java
@QueryMeta(expr = "name in (?)")
List<String> name;
```

#### 10.如何正确的使用批量写入？如何优化写入速度？
- mysql连接必须设置rewriteBatchedStatements=true，否则无法生效。
- 必须开启一个新的DaoFactory实例，不可公用其他DaoFactory实例。
- 使用dao的beginTransaction()方法获取TransactionManager实例，开启事务；
- 使用dao的beginBatchUpdate()方法开启批量更新，获取BatchUpdateManager实例；
- 此时执行save/update.delete方法，相当于addBatch操作；
- 使用BatchUpdateManager的submit()方法关闭批量更新。
- 使用TransactionManager的commit()方法来提交事务。
- 异常中调用TransactionManager的rollback()方法来回退事务。

优化
- 通过BatchUpdateManager的setBatchsize接口设置合理的批量更新数量，控制数据库操作次数以优化性能。
- 在uw-mydb-proxy下，批量写入功能是无法启用的。

**代码示例如下：**

```java
//获取dao实例
DaoFactory batchDao = DaoFactory.getInstance();
//开启事务
TransactionManager transactionManager = batchDao.beginTransaction();
//开启批量写入
BatchUpdateManager batchUpdateManager = batchDao.beginBatchUpdate();
try{
    //执行save方法
    for (EntityA entityA : entityList) {
        batchDao.save(entityA);
    }
    batchUpdateManager.submit();
    transactionManager.commit();
}catch (Exception e){
    transactionManager.rollback();
    e.printStackTrace();
}
```

#### 11.如何开启使用sql调试和在线sql统计分析功能？如何在日志中输出当前执行的sql?

```yml
#统计sql执行信息，包括参数，返回信息，执行时间等,表名为dao_sql_stats开头，此表被自动配置为按日分表
#开启sql统计
sql-stats:
  #是否统计，默认是false.
  enable: true
  #sql执行最小毫秒数
  sql_cost_min: 100
  #保存时间，默认是100天.
  data-keep-days: 100
```

#### 12.dao中queryForSingleObject和queryForSingleValue方法有什么区别？什么时候使用queryForSingleValue更好？

queryForSingleObject查询单个对象（单行数据）。

queryForSingleValue查询单个基本数值（单个字段），要映射的基础类型，如int.class,long.class,String.class,Date.class。

在程序内部调用查询某个字段用于计算的时候，使用queryForSingleValue直接获取基本类型进行计算，减少包装类开销。

#### 13.关于使用原生类型和包装类型的区别？为什么在entityBean和dtoBean中分别使用了原生类型和包装类型？

- 原生类型比包装类型占用的内存更小。

- 原生类型的运算比包装类型更高效。

- 原生类值相等一定相等，包装类值相等不一定相等。

- 包装类可以为null，原生类型不能为null。

- 包装类可以用于泛型，原生类型不能。

- 在entityBean中使用原生类型是为了减小内存占用，避免包装类计算时候的装箱拆箱等性能损耗，提高计算速度。dtoBean中使用包装类型，是因为包装类可以为null，可以用于判空，传递空值。 