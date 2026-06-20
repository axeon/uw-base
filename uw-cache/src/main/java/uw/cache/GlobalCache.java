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
import uw.common.util.KryoUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * 全局缓存类，使用 Redis 实现。
 * <p>
 * 纯 Redis 缓存，不占用 JVM 内存，适合大对象、列表、低频访问场景。
 * {@link #loadValueWrapper} 通过锁条带化（1024 stripes）+ 双重检查防止缓存击穿，
 * loader 失败时走 nullProtect/failProtect 短时缓存空值防穿透。
 * 序列化采用 Kryo，value 由 dataCacheRedisTemplate（byte[]）承载。
 * Redis key 格式：{@code uw-cache:{cacheName}:{key}}。
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
     * 锁条带数组，用于替代 String.intern() + synchronized。
     * <p>
     * 之前使用 redisKey.intern() 作为锁对象，会将所有 key 放入 JVM 字符串常量池，
     * 当 key 包含用户ID等动态数据时常量池持续增长无法被 GC，最终导致 Metaspace OOM。
     * <p>
     * 使用固定大小的锁条带，同一 key 始终映射到同一把锁，不同 key 有约 1/1024 的概率碰撞，
     * 碰撞时仅串行等待不影响正确性。1024 把锁在 100 个并发 key 时的碰撞概率约 4.8%，
     * 在 50 个并发时约 1.2%，内存开销仅约 8KB。
     */
    private static final int LOCK_STRIPES = 1024;
    private static final Object[] LOCKS = new Object[LOCK_STRIPES];

    static {
        for (int i = 0; i < LOCK_STRIPES; i++) {
            LOCKS[i] = new Object();
        }
    }

    /**
     * 根据 key 的 hashCode 映射到固定的锁对象。
     * <p>
     * 注意：使用位掩码 {@code & 0x7FFFFFFF} 而非 {@code Math.abs}，
     * 因为 {@code Math.abs(Integer.MIN_VALUE)} 仍为负数，会导致负下标越界。
     */
    private static Object getLock(String key) {
        return LOCKS[(key.hashCode() & 0x7FFFFFFF) % LOCK_STRIPES];
    }

    /**
     * Redis 值操作器。
     * <p>
     * 统一使用 byte[] 作为 value 类型：业务对象经 Kryo 序列化为 byte[] 后存入，
     * 读出时再反序列化为具体类型。这样可以避免为每个业务类型单独实例化 RedisTemplate。
     */
    private static ValueOperations<String, byte[]> opsForValue;

    /**
     * Redis 操作模板（byte[] 值），提供 connectionFactory/scan/delete 等基础操作。
     */
    private static RedisTemplate<String, byte[]> cacheRedisTemplate;

    /**
     * 构造方法，由 Spring 注入 byte[] RedisTemplate 到 static 字段。
     *
     * @param cacheRedisTemplate byte[] 值 RedisTemplate
     */
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
        // 归一化TTL：负数或0表示永久有效，避免负数TTL导致Redis抛异常。
        long ttl = normalizeTtlMillis(expireMillis);
        // 本地 wrapper 的过期时间应使用与 Redis 一致的相对 TTL（增量），
        // 而非绝对时间戳，否则 CacheValueWrapper 构造会将其当作增量叠加到当前时间导致永不过期。
        CacheValueWrapper<V> valueWrapper = new CacheValueWrapper<>(value, ttl);
        byte[] redisData = KryoCacheUtils.serializeValueWrapper(valueWrapper);
        if (ttl == 0) {
            opsForValue.set(redisKey, redisData);
        } else {
            opsForValue.set(redisKey, redisData, ttl, TimeUnit.MILLISECONDS);
        }
        return valueWrapper;
    }

    /**
     * 从redis中获取缓存值。
     *
     * @param cacheName  缓存名
     * @param key        主键
     * @param valueClass 值类型
     * @return 缓存值包装对象，不存在或反序列化失败返回 null
     */
    public static <K, V> CacheValueWrapper<V> get(String cacheName, K key, Class<V> valueClass) {
        String redisKey = RedisKeyUtils.buildTypeId(REDIS_PREFIX, cacheName, key);
        byte[] redisData = opsForValue.get(redisKey);
        if (redisData == null) {
            return null;
        }
        return (CacheValueWrapper<V>) KryoCacheUtils.deserializeValueWrapper(redisData, KryoUtils.type2Class(valueClass));
    }

    /**
     * 从redis中获取缓存值。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         主键
     * @param valueClass  值类型
     * @param <V>         数据类型
     * @return 缓存值包装对象，不存在或反序列化失败返回 null
     */
    public static <K, V> CacheValueWrapper<V> get(Class<?> entityClass, K key, Class<V> valueClass) {
        return get(entityClass.getSimpleName(), key, valueClass);
    }

    /**
     * 判断缓存中是否存在指定key的缓存值。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         主键
     * @return true 表示存在
     */
    public static boolean containsKey(Class<?> entityClass, Object key) {
        return containsKey(entityClass.getSimpleName(), key);
    }

    /**
     * 判断缓存中是否存在指定key的缓存值。
     *
     * @param cacheName 缓存名
     * @param key       主键
     * @return true 表示存在
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
     * @return 缓存值，加载彻底失败返回 null
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
     * @return 缓存值，加载彻底失败返回 null
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
     * @return 缓存值，加载彻底失败返回 null
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
     * @return 缓存值，加载彻底失败返回 null
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
     * @return 缓存值包装对象，永不为 null（加载失败时返回保护期空值 wrapper）
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
     * @return 缓存值包装对象，永不为 null（加载失败时返回保护期空值 wrapper）
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
     * @return 缓存值包装对象，永不为 null（加载失败时返回保护期空值 wrapper）
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
     * @return 缓存值包装对象，永不为 null（加载失败时返回保护期空值 wrapper）
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
                valueWrapper = (CacheValueWrapper<V>) KryoCacheUtils.deserializeValueWrapper(redisData, KryoUtils.type2Class(cacheDataLoader.getValueType()));
                return valueWrapper;
            } catch (Throwable e) {
                logger.error("反序列化失败! key=[{}]", key, e);
            }
        }

        // 没有则去执行获取方法，使用锁条化替代 String.intern() 避免 Metaspace OOM
        synchronized (getLock(redisKey)) {
            // 同一jvm中执行这个会被其他线程加锁阻塞 等待那个线程释放锁后 此线程进来 尝试再去get一下值 apply方法执行正常这里就会可以get到
            try {
                redisData = opsForValue.get(redisKey);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            if (redisData != null && redisData.length > 0) {
                try {
                    //正常数据
                    valueWrapper = (CacheValueWrapper<V>) KryoCacheUtils.deserializeValueWrapper(redisData, KryoUtils.type2Class(cacheDataLoader.getValueType()));
                    return valueWrapper;
                } catch (Throwable e) {
                    logger.error("反序列化失败! key=[{}]", key, e);
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
                long ttl = normalizeTtlMillis(expireMillis);
                if (ttl == 0) {
                    opsForValue.set(redisKey, redisData);
                } else {
                    opsForValue.set(redisKey, redisData, ttl, TimeUnit.MILLISECONDS);
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
     * @return true 表示删除成功
     */
    public static boolean invalidate(Class<?> entityClass, Object key) {
        return invalidate(entityClass.getSimpleName(), key);
    }

    /**
     * 删除缓存中的数据。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键，null则全部清除
     * @return true 表示删除成功
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
     * @return true 表示删除成功
     */
    public static boolean invalidateKeys(Class<?> entityClass, String keyPrefix) {
        return invalidatePrefix(entityClass.getSimpleName(), keyPrefix);
    }

    /**
     * 删除缓存中指定前缀的Key。
     *
     * @param cacheName 缓存名
     * @param keyPrefix key前缀，请注意key最后不用加"*",全部清除用*即可。
     * @return true 表示删除成功
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
     * @param <V>               数据类型
     * @return 缓存值，加载彻底失败返回 null
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
     * @return 缓存值，加载彻底失败返回 null
     */
    public static <K, V> V get(String cacheName, K key, CacheDataLoader<K, V> cacheDataLoader, long expireMillis, long failProtectMillis) {
        return get(cacheName, key, cacheDataLoader, expireMillis, failProtectMillis, DEFAULT_NULL_PROTECT_MILLIS, DEFAULT_RELOAD_INTERVAL_MILLIS, DEFAULT_RELOAD_MAX_TIMES);
    }

    /**
     * pub 发布消息。
     *
     * @param channel 通道名。
     * @param message 消息。
     * @return 接收到该消息的客户端数量（Redis convertAndSend 返回值）
     */
    public static Long notifyMsg(String channel, Object message) {
        return cacheRedisTemplate.convertAndSend(channel, KryoUtils.serialize(message));
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

    /**
     * 归一化TTL毫秒数。
     * <p>
     * 由于 FusionCache.Config 中 cacheExpireMillis 默认值为 -1（表示未配置/永久），
     * 直接将 -1 传入 Redis 的 set(key, value, -1, MS) 会抛异常，因此此处统一将
     * 负数和 0 视为永久有效（返回 0），正数原样返回。
     *
     * @param expireMillis 原始过期毫秒数
     * @return 归一化后的过期毫秒数，0 表示永久有效
     */
    private static long normalizeTtlMillis(long expireMillis) {
        return expireMillis > 0 ? expireMillis : 0;
    }

}
