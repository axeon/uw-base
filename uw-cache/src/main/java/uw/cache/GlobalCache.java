package uw.cache;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;
import uw.cache.util.KryoUtils;
import uw.cache.util.RedisKeyUtils;
import uw.cache.vo.CacheProtectedValue;

import java.lang.reflect.*;
import java.util.HashSet;
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
     * 返回空值的保护时间。
     */
    public static final long DEFAULT_NULL_PROTECT_MILLIS = 60_000L;

    /**
     * 加载失败的保护时间。
     */
    public static final long DEFAULT_FAIL_PROTECT_MILLIS = 60_000L;

    /**
     * redis前缀。
     */
    private static final String REDIS_PREFIX = "uw-cache:";

    private static final Logger logger = LoggerFactory.getLogger(GlobalCache.class);

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
     * @param entityClass  缓存对象类(主要用于构造cacheName)
     * @param key          主键
     * @param value        数据
     * @param expireMillis 有效期毫秒数。
     */
    public static <K, V> void put(Class<?> entityClass, K key, V value, long expireMillis) {
        put(entityClass.getSimpleName(), key, value, expireMillis);
    }

    /**
     * 向redis中存入缓存值。
     *
     * @param cacheName    缓存名
     * @param key          主键
     * @param value        数据
     * @param expireMillis 有效期毫秒数。
     * @param <V> 数据类型
     */
    public static <K, V> void put(String cacheName, K key, V value, long expireMillis) {
        String redisKey = RedisKeyUtils.buildTypeId(REDIS_PREFIX, cacheName, key);
        byte[] redisData = KryoUtils.serialize(value);
        if (expireMillis == 0) {
            opsForValue.set(redisKey, redisData);
        } else {
            opsForValue.set(redisKey, redisData, expireMillis, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 加jvm锁从redis中获取缓存值。
     * 获取不到时加jvm锁去执行函数获取。
     *
     * @param entityClass     缓存对象类(主要用于构造cacheName)
     * @param key             主键
     * @param cacheDataLoader 加载数据的函数
     * @param expireMillis    有效期毫秒数。
     * @param <V> 数据类型
     * @return
     */
    public static <K, V> V get(Class<?> entityClass, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis) {
        return get(entityClass.getSimpleName(), key, cacheDataLoader, expireMillis);
    }

    /**
     * 加jvm锁从redis中获取缓存值。
     * 获取不到时加jvm锁去执行函数获取。
     *
     * @param cacheName       缓存名
     * @param key             主键
     * @param cacheDataLoader 加载数据的函数
     * @param expireMillis    有效期毫秒数。
     * @param <V> 数据类型
     * @return
     */
    public static <K, V> V get(String cacheName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis) {
        return get(cacheName, key, cacheDataLoader, expireMillis, DEFAULT_NULL_PROTECT_MILLIS, DEFAULT_FAIL_PROTECT_MILLIS, DEFAULT_RELOAD_INTERVAL_MILLIS, DEFAULT_RELOAD_MAX_TIMES);
    }

    /**
     * 加jvm锁从redis中获取缓存值。
     * 获取不到时加jvm锁去执行函数获取。
     *
     * @param entityClass          缓存对象类(主要用于构造cacheName)
     * @param key                  主键
     * @param cacheDataLoader      加载数据的函数
     * @param expireMillis         有效期毫秒数。
     * @param failProtectMillis    失败保护毫秒数
     * @param reloadIntervalMillis 重载间隔毫秒数
     * @param reloadMaxTimes       重载次数
     * @return
     */
    public static <K, V> V get(Class<?> entityClass, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis, long nullProtectMillis, long failProtectMillis, long reloadIntervalMillis, int reloadMaxTimes) {
        return get(entityClass.getSimpleName(), key, cacheDataLoader, expireMillis, nullProtectMillis, failProtectMillis, reloadIntervalMillis, reloadMaxTimes);
    }

    /**
     * 加jvm锁从redis中获取缓存值。
     * 获取不到时加jvm锁去执行函数获取。
     * <p>
     * 对于失败保护，则返回CacheProtectedValue。
     *
     * @param cacheName            缓存名
     * @param key                  主键
     * @param cacheDataLoader      加载数据的函数
     * @param expireMillis         有效期毫秒数。
     * @param nullProtectMillis    空值保护毫秒数
     * @param failProtectMillis    失败保护毫秒数
     * @param reloadIntervalMillis 重载间隔毫秒数
     * @param reloadMaxTimes       重载次数
     * @return
     */
    public static <K, V> V get(String cacheName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis, long nullProtectMillis, long failProtectMillis, long reloadIntervalMillis, int reloadMaxTimes) {
        V value = loadWithProtectedValue(cacheName, key, cacheDataLoader, expireMillis, nullProtectMillis, failProtectMillis, reloadIntervalMillis, reloadMaxTimes);
        if (value instanceof CacheProtectedValue protectValue) {
            return null;
        }
        return value;
    }

    /**
     * 加jvm锁从redis中获取缓存值。
     * 获取不到时加jvm锁去执行函数获取。
     * <p>
     * 对于失败保护，则返回CacheProtectedValue。
     *
     * @param cacheName            缓存名
     * @param key                  主键
     * @param cacheDataLoader      加载数据的函数
     * @param expireMillis         有效期毫秒数。
     * @param nullProtectMillis    空值保护毫秒数
     * @param failProtectMillis    失败保护毫秒数
     * @param reloadIntervalMillis 重载间隔毫秒数
     * @param reloadMaxTimes       重载次数
     * @return
     */
    public static <K, V> V loadWithProtectedValue(String cacheName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis, long nullProtectMillis, long failProtectMillis, long reloadIntervalMillis, int reloadMaxTimes) {
        //组成真正的RedisKey
        String redisKey = RedisKeyUtils.buildTypeId(REDIS_PREFIX, cacheName, key);
        // 从redis中获取value.
        byte[] redisData = null;
        try {
            redisData = opsForValue.get(redisKey);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        //对象数值。
        V value = null;
        if (redisData != null) {
            if (redisData.length == 0) {
                //直接返回。
                return null;
            } else {
                try {
                    //正常数据
                    value = (V) KryoUtils.deserialize(redisData, type2Class(cacheDataLoader.getValueType()));
                    return value;
                } catch (Throwable e) {
                    //正常数据
                    value = (V) KryoUtils.deserialize(redisData, CacheProtectedValue.class);
                    return value;
                }
            }
        }
        // 没有则去执行获取方法
        synchronized (redisKey.intern()) {
            // 同一jvm中执行这个会被其他线程加锁阻塞 等待那个线程释放锁后 此线程进来 尝试再去get一下值 apply方法执行正常这里就会可以get到
            try {
                redisData = opsForValue.get(redisKey);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            if (redisData != null) {
                if (redisData.length == 0) {
                    //直接返回。
                    return null;
                } else {
                    //正常数据
                    try {
                        //正常数据
                        value = (V) KryoUtils.deserialize(redisData, type2Class(cacheDataLoader.getValueType()));
                        return value;
                    } catch (Throwable e) {
                        //正常数据
                        value = (V) KryoUtils.deserialize(redisData, CacheProtectedValue.class);
                        return value;
                    }
                }
            }
            // 假如还是没有get到值 则就是apply方法执行报错了 可以尝试继续执行 再放入redis
            for (int retryTimes = 0; retryTimes < reloadMaxTimes; retryTimes++) {
                try {
                    value = cacheDataLoader.load(key);
                    if (value == null) {
                        value = (V) new CacheProtectedValue(nullProtectMillis);
                        expireMillis = nullProtectMillis;
                    }
                    //正常执行就退出吧。
                    break;
                } catch (Throwable e) {
                    logger.error("Global数据加载失败! cacheName:{}, key:{}, retryTimes:{}, msg:{}", cacheName, key, retryTimes, e.getMessage(), e);
                }
                try {
                    Thread.sleep(reloadIntervalMillis);
                } catch (InterruptedException ignored) {
                }
            }
            // 如果此时还没有得到数值，说明加载彻底失败。
            if (value == null) {
                value = (V) new CacheProtectedValue(failProtectMillis);
                expireMillis = failProtectMillis;
            }
            //序列化写库。
            redisData = KryoUtils.serialize(value);
            try {
                if (expireMillis == 0) {
                    opsForValue.set(redisKey, redisData);
                } else {
                    opsForValue.set(redisKey, redisData, expireMillis, TimeUnit.MILLISECONDS);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        return value;
    }

    /**
     * 删除缓存中的数据。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key 缓存主键，null则全部清除
     * @return
     */
    public static boolean invalidate(Class<?> entityClass, Object key) {
        return invalidate(entityClass.getSimpleName(), key);
    }

    /**
     * 删除缓存中的数据。
     *
     * @param cacheName 缓存名
     * @param key 缓存主键，null则全部清除
     * @return
     */
    public static boolean invalidate(String cacheName, Object key) {
        //如果key是null，则清除全部。
        if (key == null) {
            cacheRedisTemplate.delete(keys(cacheName,null));
            return true;
        } else {
            String redisKey = RedisKeyUtils.buildTypeId(REDIS_PREFIX, cacheName, key);
            return Boolean.TRUE.equals(cacheRedisTemplate.delete(redisKey));
        }
    }

    /**
     * 删除缓存中指定前缀的Key。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param keyPrefix key前缀，请注意key最后不用加"*"
     * @return
     */
    public static boolean invalidateKeys(Class<?> entityClass, String keyPrefix) {
        return invalidate(entityClass.getSimpleName(), keyPrefix);
    }

    /**
     * 删除缓存中指定前缀的Key。
     *
     * @param cacheName 缓存名
     * @param keyPrefix key前缀，请注意key最后不用加"*",全部清除用*即可。
     * @return
     */
    public static boolean invalidatePrefix(String cacheName, String keyPrefix) {
        cacheRedisTemplate.delete(keys(cacheName,keyPrefix));
        return true;
    }

    /**
     * 加jvm锁从redis中获取缓存值。
     * 获取不到时加jvm锁去执行函数获取。
     *
     * @param entityClass       缓存对象类(主要用于构造cacheName)
     * @param key               主键
     * @param cacheDataLoader   加载数据的函数
     * @param expireMillis      有效期毫秒数。
     * @param failProtectMillis 失败保护毫秒数
     * @param <V>
     * @return
     */
    public static <K, V> V get(Class<?> entityClass, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis, long failProtectMillis) {
        return get(entityClass.getSimpleName(), key, cacheDataLoader, expireMillis, failProtectMillis);
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
     * @param <V> 数据类型
     * @return
     */
    public static <K, V> V get(String cacheName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis, long failProtectMillis) {
        return get(cacheName, key, cacheDataLoader, expireMillis, failProtectMillis, DEFAULT_NULL_PROTECT_MILLIS, DEFAULT_RELOAD_INTERVAL_MILLIS, DEFAULT_RELOAD_MAX_TIMES);
    }

    /**
     * pub 发布消息。
     *
     * @param channel 通道名。
     * @param message 消息。
     */
    public static Long notifyMsg(String channel, Object message) {
        return cacheRedisTemplate.convertAndSend(channel, KryoUtils.serialize(message));
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
            return Array.newInstance(type2Class(((GenericArrayType) type).getGenericComponentType()), 0).getClass(); // E.g. T[] -> T -> Object.class if <T> or Number.class
            // if <T extends Number & Comparable>
        } else if (type instanceof ParameterizedType) {
            return type2Class(((ParameterizedType) type).getRawType()); // Eg. List<T> would return List.class
        } else if (type instanceof TypeVariable) {
            Type[] bounds = ((TypeVariable<?>) type).getBounds();
            return bounds.length == 0 ? Object.class : type2Class(bounds[0]); // erasure is to the left-most bound.
        } else if (type instanceof WildcardType) {
            Type[] bounds = ((WildcardType) type).getUpperBounds();
            return bounds.length == 0 ? Object.class : type2Class(bounds[0]); // erasure is to the left-most upper bound.
        } else {
            // throw new UnsupportedOperationException( "cannot handle type class: " + type.getClass() );
            return Object.class;
        }
    }

    /**
     * 获取缓存中的所有key。
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param keyPrefix key前缀，请注意key最后不用加"*"
     * @return key集合
     */
    public static Set<String> keys(Class<?> entityClass, String keyPrefix) {
        return keys(entityClass.getSimpleName(), keyPrefix);
    }

    /**
     * 获取缓存中的所有key。
     *
     * @param cacheName 缓存名
     * @param keyPrefix key前缀，请注意key最后不用加"*",全部清除用*即可。
     * @return key集合
     */
    public static Set<String> keys(String cacheName, String keyPrefix) {
        if (StringUtils.isBlank(keyPrefix)) {
            keyPrefix = "*";
        } else {
            keyPrefix = keyPrefix + "*";
        }
        int redisPrefixLength = REDIS_PREFIX.length() + cacheName.length() + 1;
        String pattern = RedisKeyUtils.buildTypeId(REDIS_PREFIX, cacheName, keyPrefix);
        Set<String> keys = new HashSet<>();
        try (Cursor<String> cursor = cacheRedisTemplate.scan(ScanOptions.scanOptions().match(pattern).count(1000).build())) {
            cursor.forEachRemaining(key -> {
                keys.add(key.substring(redisPrefixLength));
            });
        }
        return keys;
    }

}
