package uw.cache;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;
import uw.cache.util.KryoCacheUtils;
import uw.cache.util.RedisKeyUtils;
import uw.cache.vo.CacheValueWrapper;

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
    public static <K, V> CacheValueWrapper<V> put(Class<?> entityClass, K key, V value, long expireMillis) {
        return put(entityClass.getSimpleName(), key, value, expireMillis);
    }

    /**
     * 向redis中存入缓存值。
     *
     * @param cacheName    缓存名
     * @param key          主键
     * @param value        数据
     * @param expireMillis 有效期毫秒数。
     * @param <V>          数据类型
     */
    public static <K, V> CacheValueWrapper<V> put(String cacheName, K key, V value, long expireMillis) {
        String redisKey = RedisKeyUtils.buildTypeId(REDIS_PREFIX, cacheName, key);
        CacheValueWrapper<V> valueWrapper = new CacheValueWrapper<>(value, System.currentTimeMillis());
        byte[] redisData = KryoCacheUtils.serializeValueWrapper(valueWrapper);
        if (expireMillis == 0) {
            opsForValue.set(redisKey, redisData);
        } else {
            opsForValue.set(redisKey, redisData, expireMillis, TimeUnit.MILLISECONDS);
        }
        return valueWrapper;
    }

    /**
     * 从redis中获取缓存值。
     *
     * @param cacheName
     * @param key
     * @param valueClass
     * @return
     */
    public static <K, V> CacheValueWrapper<V> get(String cacheName, K key, Class<V> valueClass) {
        String redisKey = RedisKeyUtils.buildTypeId(REDIS_PREFIX, cacheName, key);
        byte[] redisData = opsForValue.get(redisKey);
        if (redisData == null) {
            return null;
        }
        return (CacheValueWrapper<V>) KryoCacheUtils.deserializeValueWrapper(redisData, KryoCacheUtils.type2Class(valueClass));
    }

    /**
     * 从redis中获取缓存值。
     *
     * @param entityClass
     * @param key
     * @param valueClass
     * @param <V>
     * @return
     */
    public static <K, V> CacheValueWrapper<V> get(Class<?> entityClass, K key, Class<V> valueClass) {
        return get(entityClass.getSimpleName(), key, valueClass);
    }

    /**
     * 判断缓存中是否存在指定key的缓存值。
     *
     * @param entityClass
     * @param key
     * @return
     */
    public static boolean containsKey(Class<?> entityClass, Object key) {
        return containsKey(entityClass.getSimpleName(), key);
    }

    /**
     * 判断缓存中是否存在指定key的缓存值。
     *
     * @param cacheName 缓存名
     * @param key       主键
     * @return
     */
    public static boolean containsKey(String cacheName, Object key) {
        String redisKey = RedisKeyUtils.buildTypeId(REDIS_PREFIX, cacheName, key);
        return cacheRedisTemplate.hasKey(redisKey);
    }

    /**
     * 加jvm锁从redis中获取缓存值。
     * 获取不到时加jvm锁去执行函数获取。
     *
     * @param entityClass     缓存对象类(主要用于构造cacheName)
     * @param key             主键
     * @param cacheDataLoader 加载数据的函数
     * @param expireMillis    有效期毫秒数。
     * @param <V>             数据类型
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
     * @param <V>             数据类型
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
        // 多次加载，防止超过有效期。
        for (int i = 0; i < reloadMaxTimes; i++) {
            CacheValueWrapper<V> valueWrapper = loadValueWrapper(cacheName, key, cacheDataLoader, expireMillis, nullProtectMillis, failProtectMillis, reloadIntervalMillis, reloadMaxTimes);
            if (valueWrapper == null) {
                return null;
            }
            if (valueWrapper.checkExpired()) {
                invalidate(cacheName, key);
            } else {
                return valueWrapper.getValue();
            }
        }
        return null;
    }


    /**
     * 加jvm锁从redis中获取缓存值。
     * 获取不到时加jvm锁去执行函数获取。
     *
     * @param entityClass     缓存对象类(主要用于构造cacheName)
     * @param key             主键
     * @param cacheDataLoader 加载数据的函数
     * @param expireMillis    有效期毫秒数。
     * @param <V>             数据类型
     * @return
     */
    public static <K, V> CacheValueWrapper<V> loadValueWrapper(Class<?> entityClass, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis) {
        return loadValueWrapper(entityClass.getSimpleName(), key, cacheDataLoader, expireMillis);
    }

    /**
     * 加jvm锁从redis中获取缓存值。
     * 获取不到时加jvm锁去执行函数获取。
     *
     * @param cacheName       缓存名
     * @param key             主键
     * @param cacheDataLoader 加载数据的函数
     * @param expireMillis    有效期毫秒数。
     * @param <V>             数据类型
     * @return
     */
    public static <K, V> CacheValueWrapper<V> loadValueWrapper(String cacheName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis) {
        return loadValueWrapper(cacheName, key, cacheDataLoader, expireMillis, DEFAULT_NULL_PROTECT_MILLIS, DEFAULT_FAIL_PROTECT_MILLIS, DEFAULT_RELOAD_INTERVAL_MILLIS, DEFAULT_RELOAD_MAX_TIMES);
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
    public static <K, V> CacheValueWrapper<V> loadValueWrapper(Class<?> entityClass, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis, long nullProtectMillis, long failProtectMillis, long reloadIntervalMillis, int reloadMaxTimes) {
        return loadValueWrapper(entityClass.getSimpleName(), key, cacheDataLoader, expireMillis, nullProtectMillis, failProtectMillis, reloadIntervalMillis, reloadMaxTimes);
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
    public static <K, V> CacheValueWrapper<V> loadValueWrapper(String cacheName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis, long nullProtectMillis, long failProtectMillis, long reloadIntervalMillis, int reloadMaxTimes) {
        //组成真正的RedisKey
        String redisKey = RedisKeyUtils.buildTypeId(REDIS_PREFIX, cacheName, key);
        // 从redis中获取value.
        byte[] redisData = null;
        try {
            redisData = opsForValue.get(redisKey);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        CacheValueWrapper<V> valueWrapper = null;
        //正常数据
        if (redisData != null && redisData.length > 0) {
            try {
                //正常数据
                valueWrapper = (CacheValueWrapper<V>) KryoCacheUtils.deserializeValueWrapper(redisData, KryoCacheUtils.type2Class(cacheDataLoader.getValueType()));
                return valueWrapper;
            } catch (Throwable e) {
                e.printStackTrace();
                logger.error("反序列化失败！key=[{}]", key);
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
            if (redisData != null && redisData.length > 0) {
                try {
                    //正常数据
                    valueWrapper = (CacheValueWrapper<V>) KryoCacheUtils.deserializeValueWrapper(redisData, KryoCacheUtils.type2Class(cacheDataLoader.getValueType()));
                    return valueWrapper;
                } catch (Throwable e) {
                    e.printStackTrace();
                    logger.error("反序列化失败！key=[{}]", key);
                }
            }
            // 假如还是没有get到值 则就是apply方法执行报错了 可以尝试继续执行 再放入redis
            V data = null;
            for (int retryTimes = 0; retryTimes < reloadMaxTimes; retryTimes++) {
                try {
                    data = cacheDataLoader.load(key);
                    if (cacheDataLoader.getExpireMillis() > 0L) {
                        expireMillis = cacheDataLoader.getExpireMillis();
                    }
                    if (data == null) {
                        expireMillis = DEFAULT_NULL_PROTECT_MILLIS;
                    }
                    valueWrapper = new CacheValueWrapper<>(data, expireMillis);
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
            if (data == null) {
                expireMillis = DEFAULT_NULL_PROTECT_MILLIS;
                valueWrapper = new CacheValueWrapper<>(data, expireMillis);
            }
            //序列化写库。
            redisData = KryoCacheUtils.serializeValueWrapper(valueWrapper);
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

        return valueWrapper;
    }

    /**
     * 删除缓存中的数据。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键，null则全部清除
     * @return
     */
    public static boolean invalidate(Class<?> entityClass, Object key) {
        return invalidate(entityClass.getSimpleName(), key);
    }

    /**
     * 删除缓存中的数据。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键，null则全部清除
     * @return
     */
    public static boolean invalidate(String cacheName, Object key) {
        //如果key是null，则清除全部。
        if (key == null) {
            cacheRedisTemplate.delete(keys(cacheName, null));
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
     * @param keyPrefix   key前缀，请注意key最后不用加"*"
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
        cacheRedisTemplate.delete(keys(cacheName, keyPrefix));
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
     * @param <V>               数据类型
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
        return cacheRedisTemplate.convertAndSend(channel, KryoCacheUtils.serialize(message));
    }

    /**
     * 获取缓存中的所有key。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param keyPrefix   key前缀，请注意key最后不用加"*"
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
