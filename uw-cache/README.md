[TOC]

# 项目说明

uw-cache是一个基于caffeine和redis的缓存库。包含有cache,locker,counter三个常用组件。

- cache：缓存组件。包含两个实现：FusionCache融合本地和全局，性能更高；GlobalCache直接操作redis，不占用jvm内存，性能可能偏低。
- locker:  基于Redis实现的全局锁。
- counter: 计数器组件。包含两个实现：FusionCounter融合本地和全局的计数器，性能更高；GlobalCounter直接操作redis的计数器。
- hashSet: 基于redis set的便利实现。
- sortedSet: 基于redis zset的便利实现，可用于延迟任务实现。

# 基础配置

## maven引用

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-cache</artifactId>
    <version>${uw-cache.version}</version>
</dependency>
```

## yml配置

```yaml
uw:
  cache:
    redis:
      database: 9
      host: 192.168.88.21
      port: 6380
      password: redispasswd
      lettuce:
        pool:
          max-active: 100
          max-idle: 8
          max-wait: 5000ms
          min-idle: 1
      timeout: 30s
```

# FusionCache 融合缓存

- FusionCache是一种结合本地（Caffeine）缓存和全局（redis）的融合缓存实现。

- 使用了kryo进行序列化使来提高性能和降低内存使用。

- 使用了redis pub/sub进行缓存过期通知。


## 配置说明

FusionCache使用前需要调用config方法进行配置，否则无法正常使用。config方法一般建议放在类的构造器中，或者AutoConfiguration文件中，确保仅会调用一次。

config的第二个参数CacheDataLoader基于泛型的数据加载类。**泛型类型的设置必须精确，不能为接口类型必须为具体实现类（kryo反序列化的要求）。**

```java
FusionCache.config( fusionConfig, new CacheDataLoader<Integer, String>() {
    @Override
    public String load(Integer key) {
        return "hello " + key;
    }
} );
```

FusionCache.Config是配置参数类，支持基于builder的链式调用，如下：

```java
FusionCache.Config fusionConfig = FusionCache.Config.builder().cacheName( SaasInfo.class ).localCacheMaxNum( 1000 ).globalCacheExpireMillis( 86400_000L ).build();
```

同时提供了便利的构造器传值方式，如下：

```java
FusionCache.Config fusionConfig = new FusionCache.Config(SaasInfo.class, 1000, 86400_000L);
```

所有的配置参数说明如下：（重要的设置参数为cacheName,localCacheMaxNum,globalCacheExpireMillis）

```java
/**
 * 缓存名。
 */
private String cacheName;
/**
 * 本地缓存最大数量，默认10000。
 */
private int localCacheMaxNum = 10000;
/**
 * 本地缓存有效期毫秒数，默认0。
 * 设置为0的时候，表示永不过期。
 * 此参数严重影响缓存性能，降低超过200倍的性能，如非必要不要使用。
 */
private long localCacheExpireMillis = 0L;
/**
 * 全局缓存有效期毫秒数，默认为-1。
 * 设置为0的时候，表示永不过期。
 * 设置为-1的时候，表示不使用全局缓存。
 * 鉴于redis的特性，一般建议设置一个有效期，防止redis爆库。
 */
private long globalCacheExpireMillis = -1;
/**
 * 失败保护毫秒数，默认为60秒。
 * 当reload方法没有获得数据的时候，将会保护一段时间，防穿透。
 */
private long failProtectMillis = 60_000L;
/**
 * 重新加载数据的间隔毫秒数。
 * 默认为100ms，不建议低于50ms。
 */
private long reloadIntervalMillis = 100;
/**
 * 重新加载数据的最大次数。
 * 默认为10次。
 */
private int reloadMaxTimes = 10;

```

## 缓存使用

FusionCache的缓存使用有get,containsKey,size,invalidate方法。每个方法都有两种风格的参数。

1. cacheName+key参数对，其中cacheName为config方法中配置的缓存名称。

2. entityClass+key参数对。考虑到大部分缓存为实体类缓存，所以更建议使用entity.class参数的传值方式，语义明确，且编译器校验。

   

```java
/**
 * 从缓存中加载数据。
 *
 * @param entityClass 缓存对象类(主要用于构造cacheName)
 * @param key       缓存主键
 * @param <T>
 * @return
 */
public static <T> T get(Class entityClass, Object key);
```

```java
/**
 * 缓存中是否存在指定Key。
 *
 * @param entityClass 缓存对象类(主要用于构造cacheName)
 * @param key        缓存主键
 * @return
 */
public static boolean containsKey(Class entityClass, Object key) ;
```

```java
/**
 * 获得指定缓存大小。
 *
 * @param entityClass 缓存对象类(主要用于构造cacheName)
 * @return
 */
public static long size(Class entityClass);
```

```java
/**
 * 从缓存中删除一个对象。
 * 默认通知集群内其他主机。
 *
 * @param entityClass 缓存对象类(主要用于构造cacheName)
 * @param key        缓存主键
 */
public static boolean invalidate(Class entityClass, Object key);
```

## 缓存作废

FusionCache的缓存作废通过redis的pub/sub方法实现，可以对使用同一个缓存的多主机同步操作缓存作废。这个非常重要，也因为这个功能，我们不再建议使用Caffeine的缓存过期特性。

## 重要提示

1. kryo序列化不可传入接口类型（如List,Map,Set），必须传入具体实现类型（如ArrayList,LinkedHashMap,HashSet）。
2. 根据JMH实测数据，caffeine设定缓存过期时间后，性能劣化200倍，对大部分应用场景，建议不设定过期时间，仅设定redis过期时间即可。

## 压测数据

mbp16 m2max 32Gmem 20线程 预热10秒1次，压测10秒3次数据。

Benchmark                                   Mode  Cnt        Score         Error   Units
BenchmarkTest.testCaffeineCache            thrpt    3  2681789.453 ±  423455.867  ops/ms
BenchmarkTest.testCaffeineWithExpireCache  thrpt    3    16798.762 ±     371.330  ops/ms
BenchmarkTest.testCaffeineWithStats        thrpt    3  1330004.556 ± 1780012.722  ops/ms
BenchmarkTest.testFusionCache              thrpt    3  1372533.025 ±  727956.771  ops/ms
BenchmarkTest.testFusionLocalCache         thrpt    3  1404452.401 ±   27713.277  ops/ms
BenchmarkTest.testGlobalCache              thrpt    3        3.720 ±       1.078  ops/ms
BenchmarkTest.testMapCache                 thrpt    3  1953975.370 ±  183558.330  ops/ms

如果JDK21的ConcurrentHashMap性能是100%。

无设置caffeine性能为：162%。

带Expire设置的Caffeine性能为：0.8%。

带Stats设置的Caffeine性能为：68%

FusionCache的性能为：70%

屏蔽redis的FusionCache性能为：72%

GlobalCache的性能为：0.0002%

**Caffeine带Expire设置性能劣化200倍，所以如非必须不要设置Expire。**

**FusionCache实际性能应该和Caffeine一致，因为多了一层Map调用，所以性能差28%，但和带来的功能便利性相比，是划算的。**

# GlobalCache 全局缓存

GlobalCache是完全基于redis的缓存，如果数据访问量不大，那么可以考虑直接使用GlobalCache，可以减少不必要的jvm内存占用。

## 主要方法

GlobalCache主要方法有get, lockGet。

- get方法：使用jvm级别的锁，一般此方法已经足够用。

- lockGet方法，使用redis setnx锁，确定性更好，但性能更差。

## 参数风格

除此之外，也使用了类似FusionCache的两种风格的参数。

1. cacheName+key参数对，其中cacheName为config方法中配置的缓存名称。
2. entityClass+key参数对。考虑到大部分缓存为实体类缓存，所以更建议使用entity.class参数的传值方式，语义明确，且编译器校验。

在此基础上，还支持以下参数。

```java
/**
 * 缓存有效期毫秒数。
 * 设置为0的时候，表示永不过期。
 */
private long expireMillis = -1;
/**
 * 失败保护毫秒数，默认为60秒。
 * 当reload方法没有获得数据的时候，将会保护一段时间，防穿透。
 */
private long failProtectMillis = 60_000L;
/**
 * 重新加载数据的间隔毫秒数。
 * 默认为100ms，不建议低于50ms。
 */
private long reloadIntervalMillis = 100;
/**
 * 重新加载数据的最大次数。
 * 默认为10次。
 */
private int reloadMaxTimes = 10;
/**
 * 全局缓存锁定等待的毫秒数。
 * 默认为100ms，不建议低于50ms。
 */
private long globalLockWaitIntervalMillis = 100;
/**
 * 全局缓存锁定等待的重试次数。
 * 默认为10次。
 */
private int globalLockWaitMaxTimes = 10;
```

# GlobalLocker 全局锁

GlobalLocker基于Redis的setnx提供全局锁，其重要的参数就是锁定时间，超过锁定时间该锁将会自动生效。
在使用GlobalLocker的时候就应当特别注意，执行时间不可以超过其锁定时间，否则可能导致并发执行，导致锁失效。

除此之外，也使用了类似FusionCache的两种风格的参数。

1. lockerType+lockerId参数对，锁类型+锁id。
2. entityType+lockerId参数对。考虑到大部分锁为实体类型锁，所以更建议使用entity.class参数的传值方式，语义明确，且编译器校验。

**推荐使用方法如下：**

```java
long stamp = 0;
try {
    stamp = GlobalLocker.tryLock( StatsService.class, "statsTask", 120_000L );
    if (stamp > 0) {
				//此处写业务逻辑。
    }
} catch (Throwable e) {
    log.error( "MscStats save exception: " + e.getMessage(), e );
} finally {
    GlobalLocker.unlock( StatsService.class, "statsTask", stamp );
}
```

**API定义如下：**

## 尝试加锁 tryLock

```java
/**
 * 尝试加锁。
 *
 * @param lockerType     锁类型，一般可以是类名。
 * @param lockerId       锁id。
 * @param lockTimeMillis 锁住时间: 单位为毫秒,根据实际情况置锁,防死锁，也要防锁不住。
 * @return stamp，如果stamp>0则锁定成功。
 */
public static long tryLock(Class entityType, Object lockerId, long lockTimeMillis);
```

## 保持锁 keepLock

保持锁的功能在执行阶段，发现超过了锁的有效期，可以使用保持锁方法来延续锁有效期。

```java
/**
 * 保持锁定。
 * 如果执行中发现设定锁的时间不足，则可以通过keepLock保持锁。
 * @param entityType     entity类型(主要用于构造lockerType)。
 * @param lockerId       锁id。
 * @param stamp          锁stamp。
 * @param lockTimeMillis 锁住时间: 单位为毫秒,根据实际情况置锁,防止死锁
 * @return 是否锁定成功
 */
public static boolean keepLock(Class entityType, Object lockerId, long stamp, long lockTimeMillis);
```

## 解除锁 unlock

在程序执行完成后，可以调用解锁，这将会及时释放锁。

```java
/**
 * 解锁。
 *
 * @param entityType entity类型(主要用于构造lockerType)。
 * @param lockerId   锁id。
 * @param stamp      锁stamp。
 * @return 是否解锁成功
 */
public static boolean unlock(Class entityType, Object lockerId, long stamp);
```

# FusionCounter 融合计数器

FusionCounter基于本地HashMap和全局Redis的实现的融合计数器。

## 配置说明

FusionCounter使用config方法进行配置，一般来说，无特别要求不配置也可以，计数器初次调用会进行默认配置。

```java
/**
 * 初始化计数器。
 *
 * @param entityType       entity类型(主要用于构造counterType)。
 * @param syncGlobalMillis 同步全局间隔毫秒数
 * @param writeBackMillis  回写数据库间隔毫秒数
 * @param callbackConsumer 回写函数，可以在此函数中写入数据库。
 */
public static void config(final Class entityType, final long syncGlobalMillis, final long writeBackMillis, final BiConsumer<Object, Long> callbackConsumer);
```

## 参数风格

使用了类似FusionCache的两种风格的参数。

1. counterType+counterId参数对，counter类型+counterid。

2. entityType+lockerId参数对。考虑到大部分计数为实体类型，所以更建议使用entity.class参数的传值方式，语义明确，且编译器校验。


## 主要方法

```java
/**
 * 增加计数。
 *
 * @param entityType entity类型(主要用于构造counterType)。
 * @param counterId  计数器ID
 */
public static long increment(Class entityType, Object counterId)
```

```java
/**
 * 减少计数。
 *
 * @param entityType entity类型(主要用于构造counterType)。
 * @param counterId  计数器ID
 */
public static long decrement(Class entityType, Object counterId) 
```

```java
/**
 * 一般用于计数器初始设置。
 * 设置初始值后，将会自动同步redis数值。
 *
 * @param entityType entity类型(主要用于构造counterType)。
 * @param counterId  计数器ID
 * @param initNum    初始值。
 */
public static LocalCounter init(Class entityType, Object counterId, long initNum) 
```

```java
/**
 * 获得计数器数值。
 *
 * @param entityType entity类型(主要用于构造counterType)。
 * @param counterId  计数器ID
 * @return 计数数值。
 */
public static long get(Class entityType, Object counterId)
```

```java
/**
 * 删除计数器。
 *
 * @param entityType entity类型(主要用于构造counterType)。
 * @param counterId  计数器ID
 * @return 是否成功
 */
public static boolean delete(Class entityType, Object counterId)
```

```java
/**
 * 获取数值后删除计数器。
 *
 * @param entityType entity类型(主要用于构造counterType)。
 * @param counterId  计数器ID
 * @return 计数数值
 */
public static long getAndDelete(Class entityType, Object counterId) 
```

# GlobalCounter 全局计数器

GlobalCounter基于Redis的实现的全局计数器。

## 参数风格

使用了类似FusionCache的两种风格的参数。

1. counterType+counterId参数对，counter类型+counterid。

2. entityType+lockerId参数对。考虑到大部分计数为实体类型，所以更建议使用entity.class参数的传值方式，语义明确，且编译器校验。

## **主要方法**

```java
/**
 * 增加计数。
 *
 * @param entityType   entity类型(主要用于构造counterType)。
 * @param counterId    计数器ID
 * @param incrementNum 增加的计数。
 * @return 计数数值
 */
public static long increment(Class entityType, Object counterId, long incrementNum);
```

   

```java
/**
 * 减少计数。
 *
 * @param entityType   entity类型(主要用于构造counterType)。
 * @param counterId    计数器ID
 * @param decrementNum 减少的计数。
 * @return 计数数值
 */
public static long decrement(Class entityType, Object counterId, long decrementNum) 
```

```java
/**
 * 设置计数器数值。
 *
 * @param entityType entity类型(主要用于构造counterType)。
 * @param counterId  计数器ID
 * @param num        数值
 */
public static void set(Class entityType, Object counterId, long num)
```

```java
/**
 * 如果没有，则设置计数器数值。
 * 一般用于设置初始数值。
 *
 * @param entityType entity类型(主要用于构造counterType)。
 * @param counterId  计数器ID
 * @param num        数值
 */
public static void setIfAbsent(Class entityType, Object counterId, long num) 
```

```java
/**
 * 获得计数器数值。
 *
 * @param entityType entity类型(主要用于构造counterType)。
 * @param counterId  计数器ID
 * @return 计数数值
 */
public static long get(Class entityType, Object counterId) 
```

```java
/**
 * 删除计数器。
 *
 * @param entityType entity类型(主要用于构造counterType)。
 * @param counterId  计数器ID
 * @return 是否成功
 */
public static boolean delete(Class entityType, Object counterId)
```

```java
/**
 * 获取数值后删除计数器。
 *
 * @param entityType entity类型(主要用于构造counterType)。
 * @param counterId  计数器ID
 * @return 计数数值
 */
public static long getAndDelete(Class entityType, Object counterId)
```
