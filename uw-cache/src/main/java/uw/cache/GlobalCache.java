package uw.cache;

import org.checkerframework.checker.units.qual.K;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import uw.cache.util.KryoUtils;
import uw.cache.util.RedisKeyUtils;

import java.lang.reflect.*;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 全局缓存类，使用redis实现。
 */
public class GlobalCache {

    /**
     * 加载数据的重试次数。
     */
    public static final int DEFAULT_RELOAD_MAX_TIMES = 10;

    /**
     * 加载数据的重试等待间隔。
     */
    public static final long DEFAULT_RELOAD_INTERVAL_MILLIS = 200L;

    /**
     * 等待锁的重试次数。
     */
    public static final int DEFAULT_LOCK_WAIT_MAX_TIMES = 10;

    /**
     * 等待锁的重试等待间隔。
     */
    public static final long DEFAULT_LOCK_WAIT_INTERVAL_MILLIS = 200L;

    /**
     * 加载失败的保护时间。
     */
    public static final long DEFAULT_FAIL_PROTECT_MILLIS = 60_000L;

    /**
     * redis前缀。
     */
    private static final String REDIS_PREFIX = "uw-cache:";

    private static final Logger logger = LoggerFactory.getLogger( GlobalCache.class );

    /**
     * 加载失败的MAGIC数据，防止重复请求。
     */
    private static final byte[] FAIL_MAGIC_DATA = new byte[0];

    /**
     * 这里使用ValueOperations<String, String>是有原因的 虽然<K, V>可以泛型 但是泛型也存在问题 使用时必须实例化 也就是说每个使用的地方得ValueOperations<类, 类>实例化RedisTemplate
     * 所以这里选择统一转换String去序列化、反序列化 再转为对应类型的对象
     */
    private static ValueOperations<String, byte[]> opsForValue;

    private static RedisTemplate<String, byte[]> cacheRedisTemplate;

    public GlobalCache(RedisTemplate<String, byte[]> cacheRedisTemplate) {
        GlobalCache.opsForValue = cacheRedisTemplate.opsForValue();
        GlobalCache.cacheRedisTemplate = cacheRedisTemplate;
    }

    /**
     * 向redis中存入缓存值。
     *
     * @param entityName   缓存对象类(主要用于构造cacheName)
     * @param key          主键
     * @param value        数据
     * @param expireMillis 有效期毫秒数。
     */
    public static <K, V> void put(Class entityName, K key, V value, long expireMillis) {
        put( entityName.getSimpleName(), key, value, expireMillis );
    }

    /**
     * 向redis中存入缓存值。
     *
     * @param cacheName    缓存名
     * @param key          主键
     * @param value        数据
     * @param expireMillis 有效期毫秒数。
     * @param <V>
     * @return
     */
    public static <K, V> void put(String cacheName, K key, V value, long expireMillis) {
        String redisKey = RedisKeyUtils.buildTypeId( REDIS_PREFIX, cacheName, key );
        byte[] redisData = KryoUtils.serialize( value );
        if (expireMillis == 0) {
            opsForValue.set( redisKey, redisData );
        } else {
            opsForValue.set( redisKey, redisData, expireMillis, TimeUnit.MILLISECONDS );
        }
    }

    /**
     * 加jvm锁从redis中获取缓存值。
     * 获取不到时加jvm锁去执行函数获取。
     *
     * @param entityName      缓存对象类(主要用于构造cacheName)
     * @param key             主键
     * @param cacheDataLoader 加载数据的函数
     * @param expireMillis    有效期毫秒数。
     * @param <V>
     * @return
     */
    public static <K, V> V get(Class entityName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis) {
        return get( entityName.getSimpleName(), key, cacheDataLoader, expireMillis );
    }

    /**
     * 加jvm锁从redis中获取缓存值。
     * 获取不到时加jvm锁去执行函数获取。
     *
     * @param cacheName       缓存名
     * @param key             主键
     * @param cacheDataLoader 加载数据的函数
     * @param expireMillis    有效期毫秒数。
     * @param <V>
     * @return
     */
    public static <K, V> V get(String cacheName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis) {
        return get( cacheName, key, cacheDataLoader, expireMillis, DEFAULT_FAIL_PROTECT_MILLIS, DEFAULT_RELOAD_INTERVAL_MILLIS, DEFAULT_RELOAD_MAX_TIMES );
    }

    /**
     * 加jvm锁从redis中获取缓存值。
     * 获取不到时加jvm锁去执行函数获取。
     *
     * @param entityName           缓存对象类(主要用于构造cacheName)
     * @param key                  主键
     * @param cacheDataLoader      加载数据的函数
     * @param expireMillis         有效期毫秒数。
     * @param failProtectMillis    失败保护毫秒数
     * @param reloadIntervalMillis 重载间隔毫秒数
     * @param reloadMaxTimes       重载次数
     * @return
     */
    public static <K, V> V get(Class entityName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis, long failProtectMillis, long reloadIntervalMillis,
                               int reloadMaxTimes) {
        return get( entityName.getSimpleName(), key, cacheDataLoader, expireMillis, failProtectMillis, reloadIntervalMillis, reloadMaxTimes );
    }

    /**
     * 加jvm锁从redis中获取缓存值。
     * 获取不到时加jvm锁去执行函数获取。
     *
     * @param cacheName            缓存名
     * @param key                  主键
     * @param cacheDataLoader      加载数据的函数
     * @param expireMillis         有效期毫秒数。
     * @param failProtectMillis    失败保护毫秒数
     * @param reloadIntervalMillis 重载间隔毫秒数
     * @param reloadMaxTimes       重载次数
     * @return
     */
    public static <K, V> V get(String cacheName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis, long failProtectMillis, long reloadIntervalMillis,
                               int reloadMaxTimes) {
        //组成真正的RedisKey
        String redisKey = RedisKeyUtils.buildTypeId( REDIS_PREFIX, cacheName, key );
        // 从redis中获取value.
        byte[] redisData = opsForValue.get( redisKey );
        //对象数值。
        V value = null;
        if (redisData != null) {
            if (redisData.length == 0) {
                //直接返回。
                return null;
            } else {
                //正常数据
                value = (V) KryoUtils.deserialize( redisData, type2Class( cacheDataLoader.getValueType() ) );
                if (value != null) {
                    return value;
                }
            }
        }
        // 没有则去执行获取方法
        synchronized (redisKey.intern()) {
            // 同一jvm中执行这个会被其他线程加锁阻塞 等待那个线程释放锁后 此线程进来 尝试再去get一下值 apply方法执行正常这里就会可以get到
            redisData = opsForValue.get( redisKey );
            if (redisData != null) {
                if (redisData.length == 0) {
                    //直接返回。
                    return null;
                } else {
                    //正常数据
                    value = (V) KryoUtils.deserialize( redisData, type2Class( cacheDataLoader.getValueType() ) );
                    if (value != null) {
                        return value;
                    }
                }
            }
            // 假如还是没有get到值 则就是apply方法执行报错了 可以尝试继续执行 再放入redis
            value = tryLoadData( cacheName, key, cacheDataLoader, reloadIntervalMillis, reloadMaxTimes );
            if (value == null) {
                //设置失败保护，防止重复请求。
                opsForValue.set( redisKey, FAIL_MAGIC_DATA, failProtectMillis, TimeUnit.MILLISECONDS );
            } else {
                redisData = KryoUtils.serialize( value );
                if (expireMillis == 0) {
                    opsForValue.set( redisKey, redisData );
                } else {
                    opsForValue.set( redisKey, redisData, expireMillis, TimeUnit.MILLISECONDS );
                }
            }
            return value;
        }
    }

    /**
     * 删除缓存中的数据。
     *
     * @param entityName 缓存对象类(主要用于构造cacheName)
     * @param key
     * @return
     */
    public static boolean invalidate(Class entityName, Object key) {
        return invalidate( entityName.getSimpleName(), key );
    }

    /**
     * 删除缓存中的数据。
     *
     * @param cacheName
     * @param key
     * @return
     */
    public static boolean invalidate(String cacheName, Object key) {
        //如果key是null，则清除全部。
        if (key == null) {
            key = "*";
            //组成真正的RedisKey
            String redisKey = RedisKeyUtils.buildTypeId( REDIS_PREFIX, cacheName, key );
            Set<String> keys = cacheRedisTemplate.keys( redisKey );
            cacheRedisTemplate.delete( keys );
            return true;
        } else {
            String redisKey = RedisKeyUtils.buildTypeId( REDIS_PREFIX, cacheName, key );
            return Boolean.TRUE.equals( cacheRedisTemplate.delete( redisKey ) );
        }
    }

    /**
     * 加jvm锁从redis中获取缓存值。
     * 获取不到时加jvm锁去执行函数获取。
     *
     * @param entityName        缓存对象类(主要用于构造cacheName)
     * @param key               主键
     * @param cacheDataLoader   加载数据的函数
     * @param expireMillis      有效期毫秒数。
     * @param failProtectMillis 失败保护毫秒数
     * @param <V>
     * @return
     */
    public static <K, V> V get(Class entityName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis, long failProtectMillis) {
        return get( entityName.getSimpleName(), key, cacheDataLoader, expireMillis, failProtectMillis );
    }

    /**
     * 加jvm锁从redis中获取缓存值。
     * 获取不到时加jvm锁去执行函数获取。
     *
     * @param cacheName         缓存名
     * @param key               缓存主键
     * @param cacheDataLoader   加载数据函数
     * @param expireMillis      有效期毫秒数。
     * @param failProtectMillis 失败保护毫秒数
     * @param <V>
     * @return
     */
    public static <K, V> V get(String cacheName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis, long failProtectMillis) {
        return get( cacheName, key, cacheDataLoader, expireMillis, failProtectMillis, DEFAULT_RELOAD_INTERVAL_MILLIS, DEFAULT_RELOAD_MAX_TIMES );
    }

    /**
     * 从redis中获取缓存值。
     * 获取不到时加redis锁去执行函数获取。
     *
     * @param entityName      缓存对象类(主要用于构造cacheName)
     * @param key             缓存主键
     * @param cacheDataLoader 加载数据函数
     * @param expireMillis    有效期毫秒数。
     * @return
     */
    public static <V> V lockGet(Class entityName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis) {
        return lockGet( entityName.getSimpleName(), key, cacheDataLoader, expireMillis );
    }

    /**
     * 从redis中获取缓存值。
     * 获取不到时加redis锁去执行函数获取。
     *
     * @param cacheName       缓存名
     * @param key             缓存主键
     * @param cacheDataLoader 加载数据函数
     * @param expireMillis    有效期毫秒数。
     * @return
     */
    public static <V> V lockGet(String cacheName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis) {
        return lockGet( cacheName, key, cacheDataLoader, expireMillis, DEFAULT_FAIL_PROTECT_MILLIS, DEFAULT_RELOAD_INTERVAL_MILLIS, DEFAULT_RELOAD_MAX_TIMES,
                DEFAULT_LOCK_WAIT_INTERVAL_MILLIS, DEFAULT_LOCK_WAIT_MAX_TIMES );
    }

    /**
     * 从redis中获取缓存值。
     * 获取不到时加redis锁去执行函数获取。
     *
     * @param entityName             缓存对象类(主要用于构造cacheName)
     * @param key                    缓存主键
     * @param cacheDataLoader        加载数据函数
     * @param expireMillis           有效期毫秒数。
     * @param failProtectMillis
     * @param reloadIntervalMillis
     * @param reloadMaxTimes
     * @param lockWaitIntervalMillis
     * @param lockWaitMaxTimes
     * @return
     */
    public static <V> V lockGet(Class entityName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis, long failProtectMillis, long reloadIntervalMillis,
                                int reloadMaxTimes, long lockWaitIntervalMillis, int lockWaitMaxTimes) {
        return lockGet( entityName.getSimpleName(), key, cacheDataLoader, expireMillis, failProtectMillis, reloadIntervalMillis, reloadMaxTimes, lockWaitIntervalMillis,
                lockWaitMaxTimes );
    }

    /**
     * 从redis中获取缓存值。
     * 获取不到时加redis锁去执行函数获取。
     *
     * @param cacheName              缓存名
     * @param key                    缓存主键
     * @param cacheDataLoader        加载数据函数
     * @param expireMillis           有效期毫秒数。
     * @param failProtectMillis
     * @param reloadIntervalMillis
     * @param reloadMaxTimes
     * @param lockWaitIntervalMillis
     * @param lockWaitMaxTimes
     * @return
     */
    public static <V> V lockGet(String cacheName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis, long failProtectMillis, long reloadIntervalMillis,
                                int reloadMaxTimes, long lockWaitIntervalMillis, int lockWaitMaxTimes) {
        //组成真正的RedisKey
        String redisKey = RedisKeyUtils.buildTypeId( REDIS_PREFIX, cacheName, key );
        // 从redis中获取value.
        byte[] redisData = opsForValue.get( redisKey );
        //对象数值。
        V value = null;
        if (redisData != null) {
            if (redisData.length == 0) {
                //直接返回。
                return null;
            } else {
                //正常数据
                value = (V) KryoUtils.deserialize( redisData, type2Class( cacheDataLoader.getValueType() ) );
                if (value != null) {
                    return value;
                }
            }
        }
        //尝试加锁。
        long lockerStamp = GlobalLocker.tryLock( cacheName, key, lockWaitIntervalMillis );
        if (lockerStamp > 0) {
            // 假如还是没有get到值 则就是apply方法执行报错了 可以尝试继续执行 再放入redis
            value = tryLoadData( cacheName, key, cacheDataLoader, reloadIntervalMillis, reloadMaxTimes );
            if (value == null) {
                //设置失败保护，防止重复请求。
                opsForValue.set( redisKey, FAIL_MAGIC_DATA, failProtectMillis, TimeUnit.MILLISECONDS );
            } else {
                redisData = KryoUtils.serialize( value );
                if (expireMillis == 0) {
                    opsForValue.set( redisKey, redisData );
                } else {
                    opsForValue.set( redisKey, redisData, expireMillis, TimeUnit.MILLISECONDS );
                }
            }
            GlobalLocker.unlock( cacheName, key, lockerStamp );
            return value;
        } else {
            //自旋重试。
            for (int retryTimes = 0; retryTimes < lockWaitMaxTimes; retryTimes++) {
                redisData = opsForValue.get( redisKey );
                if (redisData != null) {
                    if (redisData.length == 0) {
                        //直接返回。
                        return null;
                    } else {
                        //正常数据
                        value = (V) KryoUtils.deserialize( redisData, type2Class( cacheDataLoader.getValueType() ) );
                        if (value != null) {
                            return value;
                        }
                    }
                }
                try {
                    Thread.sleep( lockWaitIntervalMillis );
                } catch (InterruptedException e) {
                }

            }
            //超过自旋重试次数，直接返回null。
            return null;
        }

    }

    /**
     * 从redis中获取缓存值。
     * 获取不到时加redis锁去执行函数获取。
     *
     * @param entityName        缓存对象类(主要用于构造cacheName)
     * @param key               缓存主键
     * @param cacheDataLoader   加载数据函数
     * @param expireMillis      有效期毫秒数。
     * @param failProtectMillis
     * @return
     */
    public static <V> V lockGet(Class entityName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis, long failProtectMillis) {
        return lockGet( entityName.getSimpleName(), key, cacheDataLoader, expireMillis, failProtectMillis );
    }

    /**
     * 从redis中获取缓存值。
     * 获取不到时加redis锁去执行函数获取。
     *
     * @param cacheName         缓存名
     * @param key               缓存主键
     * @param cacheDataLoader   加载函数
     * @param expireMillis      有效期毫秒数。
     * @param failProtectMillis
     * @return
     */
    public static <V> V lockGet(String cacheName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis, long failProtectMillis) {
        return lockGet( cacheName, key, cacheDataLoader, expireMillis, failProtectMillis, DEFAULT_RELOAD_INTERVAL_MILLIS, DEFAULT_RELOAD_MAX_TIMES,
                DEFAULT_LOCK_WAIT_INTERVAL_MILLIS, DEFAULT_LOCK_WAIT_MAX_TIMES );
    }

    /**
     * pub 发布消息。
     *
     * @param channel 通道名。
     * @param message 消息。
     */
    public static void notifyMsg(String channel, Object message) {
        cacheRedisTemplate.convertAndSend( channel, KryoUtils.serialize( message ) );
    }

    /**
     * 重试加载。
     * 如果function返回null，或者多次重试失败，将会写入保护数值，防止穿透。
     *
     * @param <V>
     * @param key
     * @param dataLoader
     * @param reloadIntervalMillis 重试间隔毫秒数
     * @param reloadMaxTimes       最大重试次数
     * @return
     */
    private static <K, V> V tryLoadData(String cacheName, K key, CacheDataLoader<K, V> dataLoader, long reloadIntervalMillis, int reloadMaxTimes) {
        for (int retryTimes = 0; retryTimes < reloadMaxTimes; retryTimes++) {
            try {
                return dataLoader.load( key );
            } catch (Throwable e) {
                logger.error( "Global数据加载失败! cacheName:{}, key:{}, retryTimes:{}, msg:{}", cacheName, key, retryTimes, e.getMessage(), e );
            }
            try {
                Thread.sleep( reloadIntervalMillis );
            } catch (InterruptedException e) {
            }
        }
        return null;
    }

    /**
     * 把type类型转为class类型。
     *
     * @param type
     * @return
     */
    private static Class<?> type2Class(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof GenericArrayType) {
            // having to create an array instance to get the class is kinda nasty
            // but apparently this is a current limitation of java-reflection concerning array classes.
            return Array.newInstance( type2Class( ((GenericArrayType) type).getGenericComponentType() ), 0 ).getClass(); // E.g. T[] -> T -> Object.class if <T> or Number.class
            // if <T extends Number & Comparable>
        } else if (type instanceof ParameterizedType) {
            return type2Class( ((ParameterizedType) type).getRawType() ); // Eg. List<T> would return List.class
        } else if (type instanceof TypeVariable) {
            Type[] bounds = ((TypeVariable<?>) type).getBounds();
            return bounds.length == 0 ? Object.class : type2Class( bounds[0] ); // erasure is to the left-most bound.
        } else if (type instanceof WildcardType) {
            Type[] bounds = ((WildcardType) type).getUpperBounds();
            return bounds.length == 0 ? Object.class : type2Class( bounds[0] ); // erasure is to the left-most upper bound.
        } else {
            // throw new UnsupportedOperationException( "cannot handle type class: " + type.getClass() );
            return Object.class;
        }
    }

}
