package uw.cache;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.cache.constant.CacheNotifyType;
import uw.cache.loader.GlobalFusionCacheLoader;
import uw.cache.loader.LocalRetryCacheLoader;
import uw.cache.loader.NoneCacheLoader;
import uw.cache.vo.CacheValueWrapper;
import uw.cache.vo.FusionCacheNotifyMessage;
import uw.common.util.SnowflakeIdGenerator;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 基于 Caffeine 和 Redis 构建的复合（融合）缓存。
 * <p>
 * 本地 Caffeine 兜性能，Redis（{@link GlobalCache}）兜全局一致性，通过 Redis Pub/Sub（通道
 * {@link #FUSION_CACHE_NOTIFY_CHANNEL}）实现多实例间缓存失效/刷新同步。
 * <p>
 * 使用前必须调用 {@link #config} 注册缓存（建议放在 Helper 的 static 块中，仅调用一次，重复 config 会丢弃旧本地缓存）。
 * 提供两种参数风格：{@code (entityClass, key)} 与 {@code (cacheName, key)}，前者语义更明确、推荐使用。
 */
public class FusionCache {

    /**
     * Fusion缓存通知channel。
     */
    public static final String FUSION_CACHE_NOTIFY_CHANNEL = "UW_CACHE_NOTIFY_CHANNEL";


    /**
     * 最大重试次数。
     */
    private static final int MAX_RETRY_TIMES = 5;

    /**
     * 实例ID。
     */
    public static final long INSTANCE_ID = SnowflakeIdGenerator.getInstance().generateId();

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(FusionCache.class);

    /**
     * key是缓存名，value是CacheWrapper。
     */
    private static final ConcurrentMap<String, CacheWrapper> cacheWrapperMap = new ConcurrentHashMap<>();

    /**
     * 配置缓存。
     *
     * @param config 缓存配置
     */
    public static void config(Config config) {
        config(config, null, null);
    }

    /**
     * 配置缓存。
     *
     * @param config 缓存配置
     */
    public static void config(Config config, CacheDataLoader<?, ?> cacheDataLoader) {
        config(config, cacheDataLoader, null);
    }

    /**
     * 配置缓存。
     *
     * @param config 缓存配置
     */
    public static void config(Config config, CacheChangeNotifyListener<?, ?> cacheChangeNotifyListener) {
        config(config, null, cacheChangeNotifyListener);
    }

    /**
     * 配置缓存。
     *
     * @param config          缓存配置
     * @param cacheDataLoader 缓存数据加载器
     */
    public static void config(Config config, CacheDataLoader<?, ?> cacheDataLoader, CacheChangeNotifyListener<?, ?> cacheChangeNotifyListener) {
        Caffeine<?, ?> caffeine = Caffeine.newBuilder();
        if (config.getLocalCacheMaxNum() > 0) {
            caffeine.maximumSize(config.getLocalCacheMaxNum());
        }
        //设置失效监听
        config.cacheChangeNotifyListener = cacheChangeNotifyListener;
        CacheLoader cacheLoader = null;
        if (cacheDataLoader != null) {
            if (config.isGlobalCache()) {
                cacheLoader = new GlobalFusionCacheLoader<>(config, cacheDataLoader);
            } else {
                cacheLoader = new LocalRetryCacheLoader<>(config, cacheDataLoader);
            }
        } else {
            cacheLoader = new NoneCacheLoader<>();
        }

        LoadingCache<?, ?> caffeineCache = caffeine.build(cacheLoader);
        if (cacheWrapperMap.put(config.getCacheName(), new CacheWrapper(caffeineCache, config)) != null) {
            // 重复 config 会丢弃旧的本地 Caffeine 缓存实例，属于高风险操作（本地数据全部丢失），此处告警提示。
            log.warn("FusionCache[{}] 已存在配置，本次 config 将覆盖旧实例，旧本地缓存数据将被丢弃！", config.getCacheName());
        }
    }

    /**
     * 向缓存中存入数据。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     * @param value       数值
     */
    public static void put(Class<?> entityClass, Object key, Object value) {
        put(entityClass.getSimpleName(), key, value, 0, false);
    }

    /**
     * 向缓存中存入数据。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     * @param value     数值
     */
    public static void put(String cacheName, Object key, Object value) {
        put(cacheName, key, value, 0, false);
    }

    /**
     * 向缓存中存入数据。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     * @param value       数值
     * @param onlyLocal   只更新到本地缓存
     */
    public static void put(Class<?> entityClass, Object key, Object value, boolean onlyLocal) {
        put(entityClass.getSimpleName(), key, value, 0, onlyLocal);
    }

    /**
     * 向缓存中存入数据。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     * @param value     数值
     * @param onlyLocal 只更新到本地缓存
     */
    public static void put(String cacheName, Object key, Object value, boolean onlyLocal) {
        put(cacheName, key, value, 0, onlyLocal);
    }

    /**
     * 向缓存中存入数据。
     *
     * @param entityClass  缓存对象类(主要用于构造cacheName)
     * @param key          缓存主键
     * @param value        数值
     * @param expireMillis 缓存有效期毫秒数。
     */
    public static void put(Class<?> entityClass, Object key, Object value, long expireMillis) {
        put(entityClass.getSimpleName(), key, value, expireMillis, false);
    }

    /**
     * 向缓存中存入数据。
     *
     * @param cacheName    缓存名
     * @param key          缓存主键
     * @param value        数值
     * @param expireMillis 缓存有效期毫秒数。
     */
    public static void put(String cacheName, Object key, Object value, long expireMillis) {
        put(cacheName, key, value, expireMillis, false);
    }

    /**
     * 向缓存中存入数据。
     *
     * @param entityClass  缓存对象类(主要用于构造cacheName)
     * @param key          缓存主键
     * @param value        数值
     * @param expireMillis 缓存有效期毫秒数。
     * @param onlyLocal    只更新到本地缓存
     */
    public static void put(Class<?> entityClass, Object key, Object value, long expireMillis, boolean onlyLocal) {
        put(entityClass.getSimpleName(), key, value, expireMillis, onlyLocal);
    }

    /**
     * 向缓存中存入数据。
     *
     * @param cacheName    缓存名
     * @param key          缓存主键
     * @param value        数值
     * @param expireMillis 缓存有效期毫秒数。
     * @param onlyLocal    只更新到本地缓存
     */
    public static void put(String cacheName, Object key, Object value, long expireMillis, boolean onlyLocal) {
        CacheWrapper cacheWrapper = cacheWrapperMap.get(cacheName);
        if (cacheWrapper == null) {
            log.warn("FusionCache[{}] not config!!!", cacheName);
            return;
        }
        expireMillis = expireMillis > 0 ? expireMillis : cacheWrapper.config.getCacheExpireMillis();
        CacheValueWrapper<Object> valueWrapper = null;
        if (!onlyLocal && cacheWrapper.config.isGlobalCache()) {
            valueWrapper = GlobalCache.put(cacheName, key, value, expireMillis);
        } else {
            valueWrapper = new CacheValueWrapper<>(value, expireMillis);
        }
        cacheWrapper.cache.put(key, valueWrapper);
    }

    /**
     * 向缓存中存入数据。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param map         要存入的批量数据
     */
    public static void putAll(Class<?> entityClass, Map<Object, Object> map) {
        putAll(entityClass.getSimpleName(), map, 0, false);
    }

    /**
     * 向缓存中存入数据。
     *
     * @param cacheName 缓存名
     * @param map       要存入的批量数据
     */
    public static void putAll(String cacheName, Map<Object, Object> map) {
        putAll(cacheName, map, 0, false);
    }

    /**
     * 向缓存中存入数据。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param map         要存入的批量数据
     * @param onlyLocal   只更新到本地缓存
     */
    public static void putAll(Class<?> entityClass, Map<Object, Object> map, boolean onlyLocal) {
        putAll(entityClass.getSimpleName(), map, 0, onlyLocal);
    }

    /**
     * 向缓存中存入数据。
     *
     * @param cacheName 缓存名
     * @param map       要存入的批量数据
     * @param onlyLocal 只更新到本地缓存
     */
    public static void putAll(String cacheName, Map<Object, Object> map, boolean onlyLocal) {
        putAll(cacheName, map, 0, onlyLocal);
    }

    /**
     * 向缓存中存入数据。
     *
     * @param entityClass  缓存对象类(主要用于构造cacheName)
     * @param map          要存入的批量数据
     * @param expireMillis 缓存有效期毫秒数。
     */
    public static void putAll(Class<?> entityClass, Map<Object, Object> map, long expireMillis) {
        putAll(entityClass.getSimpleName(), map, expireMillis, false);
    }

    /**
     * 向缓存中存入数据。
     *
     * @param cacheName    缓存名
     * @param map          要存入的批量数据
     * @param expireMillis 缓存有效期毫秒数。
     */
    public static void putAll(String cacheName, Map<Object, Object> map, long expireMillis) {
        putAll(cacheName, map, expireMillis, false);
    }

    /**
     * 向缓存中批量存入数据。
     *
     * @param entityClass  缓存对象类(主要用于构造cacheName)
     * @param map          要存入的批量数据
     * @param expireMillis 缓存有效期毫秒数。
     * @param onlyLocal    只更新到本地缓存
     */
    public static void putAll(Class<?> entityClass, Map<Object, Object> map, long expireMillis, boolean onlyLocal) {
        putAll(entityClass.getSimpleName(), map, expireMillis, onlyLocal);
    }

    /**
     * 向缓存中批量存入数据。
     *
     * @param cacheName
     * @param map          要存入的批量数据
     * @param expireMillis 缓存有效期毫秒数。
     * @param onlyLocal    只更新到本地缓存
     */
    public static void putAll(String cacheName, Map<Object, Object> map, long expireMillis, boolean onlyLocal) {
        CacheWrapper cacheWrapper = cacheWrapperMap.get(cacheName);
        if (cacheWrapper == null) {
            log.warn("FusionCache[{}] not config!!!", cacheName);
            return;
        }
        expireMillis = expireMillis > 0 ? expireMillis : cacheWrapper.config.getCacheExpireMillis();
        CacheValueWrapper<Object> valueWrapper = null;
        for (Map.Entry<Object, Object> kv : map.entrySet()) {
            if (!onlyLocal && cacheWrapper.config.isGlobalCache()) {
                valueWrapper = GlobalCache.put(cacheName, kv.getKey(), kv.getValue(), expireMillis);
            } else {
                valueWrapper = new CacheValueWrapper<>(kv.getValue(), expireMillis);
            }
            cacheWrapper.cache.put(kv.getKey(), valueWrapper);
        }
    }

    /**
     * 从缓存中加载数据。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     * @param <T>         数据类型
     * @return 缓存数据，未配置缓存或重试耗尽仍过期返回 null
     */
    public static <T> T get(Class<?> entityClass, Object key) {
        return get(entityClass.getSimpleName(), key);
    }

    /**
     * 从缓存中加载数据。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     * @param <T>       数据类型
     * @return 缓存数据，未配置缓存或重试耗尽仍过期返回 null
     */
    public static <T> T get(String cacheName, Object key) {
        CacheWrapper cacheWrapper = cacheWrapperMap.get(cacheName);
        if (cacheWrapper == null) {
            log.warn("FusionCache[{}] not config!!!", cacheName);
            return null;
        }
        for (int i = 0; i <= MAX_RETRY_TIMES; i++) {
            CacheValueWrapper<T> valueWrapper = (CacheValueWrapper<T>) cacheWrapper.cache.get(key);
            if (valueWrapper == null) {
                return null;
            }
            if (!valueWrapper.checkExpired()) {
                return valueWrapper.getValue();
            }
            if (i == MAX_RETRY_TIMES) {
                log.warn("FusionCache[{}] key=[{}] still expired after {} retries, returning null", cacheName, key, MAX_RETRY_TIMES);
                return null;
            }
            // 重试路径：删除本地缓存以便 loader 重新加载；全局缓存场景下需同时删除 Redis 中已过期的 wrapper，
            // 否则 loader 会反复从 Redis 取到同一过期数据导致重试空转。
            // 此处直接操作缓存底座，绕过 invalidate() 以避免触发 cacheChangeNotifyListener（重试循环不需要监听副作用）和集群通知。
            if (cacheWrapper.config.isGlobalCache()) {
                GlobalCache.invalidate(cacheName, key);
            }
            cacheWrapper.cache.invalidate(key);
        }
        return null;
    }


    /**
     * 从缓存中加载ValueWrapper。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     * @param <T>         数据类型
     * @return 缓存值包装对象，未配置缓存返回 null
     */
    public static <T> CacheValueWrapper<T> getValueWrapper(Class<?> entityClass, Object key) {
        return getValueWrapper(entityClass.getSimpleName(), key);
    }

    /**
     * 从缓存中加载ValueWrapper。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     * @param <T>       数据类型
     * @return 缓存值包装对象，未配置缓存返回 null
     */
    public static <T> CacheValueWrapper<T> getValueWrapper(String cacheName, Object key) {
        CacheWrapper cacheWrapper = cacheWrapperMap.get(cacheName);
        if (cacheWrapper == null) {
            log.warn("FusionCache[{}] not config!!!", cacheName);
            return null;
        }
        CacheValueWrapper<T> valueWrapper = (CacheValueWrapper<T>) cacheWrapper.cache.get(key);
        return valueWrapper;
    }

    /**
     * 缓存中是否存在指定Key。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     * @return true 表示存在且未过期，未配置缓存返回 false
     */
    public static boolean containsKey(Class<?> entityClass, Object key) {
        return containsKey(entityClass.getSimpleName(), key);
    }

    /**
     * 缓存中是否存在指定Key。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     * @return true 表示存在且未过期，未配置缓存返回 false
     */
    public static boolean containsKey(String cacheName, Object key) {
        CacheWrapper cacheWrapper = cacheWrapperMap.get(cacheName);
        if (cacheWrapper == null) {
            log.warn("FusionCache[{}] not config!!!", cacheName);
            return false;
        }
        CacheValueWrapper valueWrapper = (CacheValueWrapper) cacheWrapper.cache.getIfPresent(key);
        return valueWrapper != null && valueWrapper.getValue() != null && !valueWrapper.checkExpired();
    }

    /**
     * 获取指定缓存本地大小。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @return 本地缓存条目数，未配置缓存返回 -1
     */
    public static long localCacheSize(Class<?> entityClass) {
        return localCacheSize(entityClass.getSimpleName());
    }

    /**
     * 获取指定缓存本地大小。
     *
     * @param cacheName 缓存名
     * @return 本地缓存条目数，未配置缓存返回 -1
     */
    public static long localCacheSize(String cacheName) {
        CacheWrapper cacheWrapper = cacheWrapperMap.get(cacheName);
        if (cacheWrapper == null) {
            log.warn("FusionCache[{}] not config!!!", cacheName);
            return -1;
        }
        return cacheWrapper.cache.estimatedSize();
    }

    /**
     * 以Map形式返回指定缓存内容。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @return 本地缓存视图，未配置缓存返回 null
     */
    public static ConcurrentMap<?, ?> localCacheMap(Class<?> entityClass) {
        return localCacheMap(entityClass.getSimpleName());
    }

    /**
     * 以Map形式返回指定缓存内容。
     *
     * @param cacheName 缓存名
     * @return 本地缓存视图，未配置缓存返回 null
     */
    public static ConcurrentMap<?, ?> localCacheMap(String cacheName) {
        CacheWrapper cacheWrapper = cacheWrapperMap.get(cacheName);
        if (cacheWrapper == null) {
            log.warn("FusionCache[{}] not config!!!", cacheName);
            return null;
        }
        return cacheWrapper.cache.asMap();
    }

    /**
     * 获取本地缓存。
     *
     * @param entityClass 缓存对象类
     * @return 本地 Caffeine 缓存实例，未配置缓存返回 null
     */
    public static LoadingCache<?, ?> getLocalCache(Class<?> entityClass) {
        return getLocalCache(entityClass.getSimpleName());
    }

    /**
     * 获取本地缓存。
     *
     * @param cacheName 缓存名
     * @return 本地 Caffeine 缓存实例，未配置缓存返回 null
     */
    private static LoadingCache<?, ?> getLocalCache(String cacheName) {
        CacheWrapper cacheWrapper = cacheWrapperMap.get(cacheName);
        if (cacheWrapper == null) {
            log.warn("FusionCache[{}] not config!!!", cacheName);
            return null;
        }
        return cacheWrapper.cache;
    }

    /**
     * 获取指定缓存统计信息。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @return Caffeine 命中统计，未配置缓存返回 null
     */
    public static CacheStats localStats(Class<?> entityClass) {
        return localStats(entityClass.getSimpleName());
    }

    /**
     * 获取指定缓存统计信息。
     *
     * @param cacheName 缓存名
     * @return Caffeine 命中统计，未配置缓存返回 null
     */
    public static CacheStats localStats(String cacheName) {
        CacheWrapper cacheWrapper = cacheWrapperMap.get(cacheName);
        if (cacheWrapper == null) {
            log.warn("FusionCache[{}] not config!!!", cacheName);
            return null;
        }
        return cacheWrapper.cache.stats();
    }

    /**
     * 是否包含某个cache。
     *
     * @param entityClass
     * @return
     */
    public static boolean existsCache(Class<?> entityClass) {
        return cacheWrapperMap.containsKey(entityClass.getSimpleName());
    }

    /**
     * 是否包含某个cache。
     *
     * @param cacheName
     * @return
     */
    public static boolean existsCache(String cacheName) {
        return cacheWrapperMap.containsKey(cacheName);
    }

    /**
     * 获取指定缓存key前缀的所有key。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param keyPrefix   key前缀
     * @return key集合，未配置缓存返回 null
     */
    public static Set<String> keys(Class<?> entityClass, String keyPrefix) {
        return keys(entityClass.getSimpleName(), keyPrefix);
    }

    /**
     * 获取指定缓存key前缀的所有key。
     *
     * @param cacheName 缓存名
     * @param keyPrefix key前缀
     * @return key集合，未配置缓存返回 null
     */
    public static Set<String> keys(String cacheName, String keyPrefix) {
        CacheWrapper cacheWrapper = cacheWrapperMap.get(cacheName);
        if (cacheWrapper == null) {
            log.warn("FusionCache[{}] not config!!!", cacheName);
            return null;
        }
        if (cacheWrapper.config.isGlobalCache()) {
            //全局缓存
            return GlobalCache.keys(cacheName, keyPrefix);
        } else {
            ConcurrentMap<?, ?> map = localCacheMap(cacheName);
            if (map == null) {
                return null;
            }
            if (StringUtils.isBlank(keyPrefix)) {
                return map.keySet().stream().map(String::valueOf).collect(Collectors.toSet());
            } else {
                return map.keySet().stream().map(String::valueOf).filter(key -> key.startsWith(keyPrefix)).collect(Collectors.toSet());
            }
        }
    }

    /**
     * 从缓存中删除一个对象。
     * 默认通知集群内其他主机。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     * @return true 表示执行成功，未配置缓存返回 false
     */
    public static boolean invalidate(Class<?> entityClass, Object key) {
        return invalidate(entityClass.getSimpleName(), key, true);
    }

    /**
     * 从缓存中删除一个对象。
     * 默认通知集群内其他主机。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     * @return true 表示执行成功，未配置缓存返回 false
     */
    public static boolean invalidate(String cacheName, Object key) {
        return invalidate(cacheName, key, true);
    }

    /**
     * 从缓存中删除一个对象。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     * @param notify      是否通知集群内其他主机。
     * @return true 表示执行成功，未配置缓存返回 false
     */
    public static boolean invalidate(Class<?> entityClass, Object key, boolean notify) {
        return invalidate(entityClass.getSimpleName(), key, notify);
    }

    /**
     * 从缓存中删除一个对象。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     * @param notify    是否通知集群内其他主机。
     * @return true 表示执行成功，未配置缓存返回 false
     */
    public static boolean invalidate(String cacheName, Object key, boolean notify) {
        CacheWrapper cacheWrapper = cacheWrapperMap.get(cacheName);
        if (cacheWrapper == null) {
            log.warn("FusionCache[{}] not config!!!", cacheName);
            return false;
        }
        //处理自身记录。
        LoadingCache cache = cacheWrapper.cache;
        Config config = cacheWrapper.config;
        if (cacheWrapper.config.isGlobalCache()) {
            //先删除redis缓存
            GlobalCache.invalidate(cacheName, key);
        }
        if (notify && config.isGlobalCache()) {
            //发布通知
            notifyInvalidate(cacheName, key);
        }
        if (key == null) {
            //拉出所有数据执行监听。
            Set<Map.Entry> kvSet = cache.asMap().entrySet();
            cache.invalidateAll();
            if (config.cacheChangeNotifyListener != null) {
                for (Map.Entry kv : kvSet) {
                    CacheValueWrapper oldValue = (CacheValueWrapper) kv.getValue();
                    try {
                        config.cacheChangeNotifyListener.onMessage(kv.getKey(), oldValue.getValue(), null);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }

                }
            }
        } else {
            CacheValueWrapper oldValue = (CacheValueWrapper) cache.getIfPresent(key);
            //对于没有的信息，不需要执行通知监听。
            if (oldValue != null) {
                cache.invalidate(key);
                if (config.cacheChangeNotifyListener != null) {
                    try {
                        config.cacheChangeNotifyListener.onMessage(key, oldValue.getValue(), null);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }

                }
            }
        }
        return true;
    }

    /**
     * 通知集群内其他主机缓存过期。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     * @return 接收到该通知的客户端数量
     */
    public static Long notifyInvalidate(Class<?> entityClass, Object key) {
        return notifyInvalidate(entityClass.getSimpleName(), key);
    }

    /**
     * 通知集群内其他主机缓存过期。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     * @return 接收到该通知的客户端数量
     */
    public static Long notifyInvalidate(String cacheName, Object key) {
        //发布通知
        return GlobalCache.notifyMsg(FUSION_CACHE_NOTIFY_CHANNEL, new FusionCacheNotifyMessage(INSTANCE_ID, cacheName, CacheNotifyType.INVALIDATE.getValue(), key));
    }

    /**
     * 通知集群内其他主机缓存刷新。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     * @return 接收到该通知的客户端数量
     */
    public static Long notifyRefresh(Class<?> entityClass, Object key) {
        return notifyRefresh(entityClass.getSimpleName(), key);
    }

    /**
     * 通知集群内其他主机缓存刷新。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     * @return 接收到该通知的客户端数量
     */
    public static Long notifyRefresh(String cacheName, Object key) {
        //发布通知
        return GlobalCache.notifyMsg(FUSION_CACHE_NOTIFY_CHANNEL, new FusionCacheNotifyMessage(INSTANCE_ID, cacheName, CacheNotifyType.REFRESH.getValue(), key));
    }

    /**
     * 刷新一个对象。
     * 默认通知集群内其他主机。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     * @return true 表示执行成功，未配置缓存返回 false
     */
    public static boolean refresh(Class<?> entityClass, Object key) {
        return refresh(entityClass.getSimpleName(), key, true);
    }

    /**
     * 刷新一个对象。
     * 默认通知集群内其他主机。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     * @return true 表示执行成功，未配置缓存返回 false
     */
    public static boolean refresh(String cacheName, Object key) {
        return refresh(cacheName, key, true);
    }

    /**
     * 刷新一个对象。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     * @param notify      是否通知集群内其他主机。
     * @return true 表示执行成功，未配置缓存返回 false
     */
    public static boolean refresh(Class<?> entityClass, Object key, boolean notify) {
        return refresh(entityClass.getSimpleName(), key, notify);
    }

    /**
     * 刷新一个对象。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     * @param notify    是否通知集群内其他主机。
     * @return true 表示执行成功，未配置缓存返回 false
     */
    public static boolean refresh(String cacheName, Object key, boolean notify) {
        CacheWrapper cacheWrapper = cacheWrapperMap.get(cacheName);
        if (cacheWrapper == null) {
            log.warn("FusionCache[{}] not config!!!", cacheName);
            return false;
        }
        //处理自身记录。
        LoadingCache cache = cacheWrapper.cache;
        Config config = cacheWrapper.config;

        if (cacheWrapper.config.isGlobalCache()) {
            //先删除redis缓存
            GlobalCache.invalidate(cacheName, key);
        }

        if (notify && config.isGlobalCache()) {
            //发布通知
            notifyRefresh(cacheName, key);
        }

        //先执行监听。
        if (key == null) {
            //拉出所有数据执行监听。
            Set<Map.Entry> kvSet = cache.asMap().entrySet();
            for (Map.Entry kv : kvSet) {
                cache.invalidate(kv.getKey());
                CacheValueWrapper oldValue = (CacheValueWrapper) kv.getValue();
                CacheValueWrapper newValue = (CacheValueWrapper) cache.get(kv.getKey());
                if (config.cacheChangeNotifyListener != null) {
                    try {
                        config.cacheChangeNotifyListener.onMessage(kv.getKey(), oldValue.getValue(), newValue.getValue());
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }

                }
            }
        } else {
            CacheValueWrapper oldValue = (CacheValueWrapper) cache.getIfPresent(key);
            if (oldValue != null) {
                cache.invalidate(key);
            }
            CacheValueWrapper newValue = (CacheValueWrapper) cache.get(key);
            if (config.cacheChangeNotifyListener != null && oldValue != null) {
                try {
                    config.cacheChangeNotifyListener.onMessage(key, oldValue.getValue(), newValue != null ? newValue.getValue() : null);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return true;
    }


    /**
     * 缓存对象包装类，封装 Caffeine 本地缓存实例与其配置。
     *
     * @param cache  本地 Caffeine LoadingCache
     * @param config 缓存配置
     */
    private record CacheWrapper(LoadingCache cache, Config config) {
    }

    /**
     * 缓存配置。
     */
    public static class Config {
        /**
         * 缓存名。
         */
        private String cacheName;
        /**
         * 缓存有效期毫秒数，默认 -1。
         * <p>
         * -1 或 0：永不过期（本地与 Redis 均不设过期）。
         * 大于 0：同时作为本地 wrapper 过期时间与 Redis TTL。
         * 注意：本地设过期会拖累 Caffeine 性能（劣化约 200 倍），高频缓存建议保持 -1。
         */
        private long cacheExpireMillis = -1;
        /**
         * 本地缓存最大数量，默认10000。
         * 设置为0，表示不限制数量。
         */
        private int localCacheMaxNum = 10000;
        /**
         * 空值保护毫秒数，默认为60秒。
         * 当reload方法获取null的时候，将会保护一段时间，防穿透。
         */
        private long nullProtectMillis = 60_000L;
        /**
         * 失败保护毫秒数，默认为60秒。
         * 当reload方法异常的时候，将会保护一段时间，防穿透。
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
         * 是否自动通知集群缓存失效。
         * 加载成功的缓存，将会通知集群缓存失效以便更新。
         * 默认不启用。
         */
        private boolean autoNotifyInvalidate = false;

        /**
         * 是否全局缓存。
         * 默认为true，表示全局缓存。
         * 如果为false，则表示本节点的缓存。
         */
        private boolean isGlobalCache = true;

        /**
         * 缓存变更监听器。
         */
        private CacheChangeNotifyListener cacheChangeNotifyListener;

        /**
         * 默认构造函数，配合 setter 链式配置使用。
         */
        public Config() {
        }

        /**
         * 常用构造器。
         *
         * @param entityClass      缓存对象类（取 simpleName 作为 cacheName）
         * @param localCacheMaxNum 本地缓存最大数量
         * @param cacheExpireMillis 缓存有效期毫秒数
         */
        public Config(Class<?> entityClass, int localCacheMaxNum, long cacheExpireMillis) {
            this.cacheName = entityClass.getSimpleName();
            this.localCacheMaxNum = localCacheMaxNum;
            this.cacheExpireMillis = cacheExpireMillis;
        }

        /**
         * 常用构造器。
         *
         * @param cacheName        缓存名
         * @param localCacheMaxNum 本地缓存最大数量
         * @param cacheExpireMillis 缓存有效期毫秒数
         */
        public Config(String cacheName, int localCacheMaxNum, long cacheExpireMillis) {
            this.cacheName = cacheName;
            this.localCacheMaxNum = localCacheMaxNum;
            this.cacheExpireMillis = cacheExpireMillis;
        }

        /**
         * 常用构造器。
         *
         * @param entityClass       缓存对象类（取 simpleName 作为 cacheName）
         * @param localCacheMaxNum  本地缓存最大数量
         * @param cacheExpireMillis 缓存有效期毫秒数
         * @param nullProtectMillis 空值保护毫秒数
         * @param failProtectMillis 失败保护毫秒数
         */
        public Config(Class<?> entityClass, int localCacheMaxNum, long cacheExpireMillis, long nullProtectMillis, long failProtectMillis) {
            this.cacheName = entityClass.getSimpleName();
            this.localCacheMaxNum = localCacheMaxNum;
            this.cacheExpireMillis = cacheExpireMillis;
            this.nullProtectMillis = nullProtectMillis;
            this.failProtectMillis = failProtectMillis;
        }

        /**
         * 常用构造器。
         *
         * @param cacheName         缓存名
         * @param localCacheMaxNum  本地缓存最大数量
         * @param cacheExpireMillis 缓存有效期毫秒数
         * @param nullProtectMillis 空值保护毫秒数
         * @param failProtectMillis 失败保护毫秒数
         */
        public Config(String cacheName, int localCacheMaxNum, long cacheExpireMillis, long nullProtectMillis, long failProtectMillis) {
            this.cacheName = cacheName;
            this.localCacheMaxNum = localCacheMaxNum;
            this.cacheExpireMillis = cacheExpireMillis;
            this.nullProtectMillis = nullProtectMillis;
            this.failProtectMillis = failProtectMillis;
        }

        private Config(Builder builder) {
            setCacheName(builder.cacheName);
            setLocalCacheMaxNum(builder.localCacheMaxNum);
            setCacheExpireMillis(builder.cacheExpireMillis);
            setNullProtectMillis(builder.nullProtectMillis);
            setFailProtectMillis(builder.failProtectMillis);
            setReloadIntervalMillis(builder.reloadIntervalMillis);
            setReloadMaxTimes(builder.reloadMaxTimes);
            setAutoNotifyInvalidate(builder.autoNotifyInvalidate);
            setGlobalCache(builder.isGlobalCache);
        }

        /**
         * builder构造器。
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * builder构造器。
         *
         * @param cacheName 缓存名
         * @return Builder 实例
         */
        public static Builder builder(String cacheName) {
            return new Builder().cacheName(cacheName);
        }

        /**
         * builder构造器。
         *
         * @param entityClass 缓存对象类（取 simpleName 作为 cacheName）
         * @return Builder 实例
         */
        public static Builder builder(Class<?> entityClass) {
            return new Builder().cacheName(entityClass.getSimpleName());
        }

        /**
         * builder构造器。
         *
         * @param cacheName         缓存名
         * @param localCacheMaxNum  本地缓存最大数量
         * @param cacheExpireMillis 缓存有效期毫秒数
         * @return Builder 实例
         */
        public static Builder builder(String cacheName, int localCacheMaxNum, long cacheExpireMillis) {
            return new Builder().cacheName(cacheName).localCacheMaxNum(localCacheMaxNum).cacheExpireMillis(cacheExpireMillis);
        }

        /**
         * builder构造器。
         *
         * @param entityClass       缓存对象类（取 simpleName 作为 cacheName）
         * @param localCacheMaxNum  本地缓存最大数量
         * @param cacheExpireMillis 缓存有效期毫秒数
         * @return Builder 实例
         */
        public static Builder builder(Class<?> entityClass, int localCacheMaxNum, long cacheExpireMillis) {
            return new Builder().cacheName(entityClass.getSimpleName()).localCacheMaxNum(localCacheMaxNum).cacheExpireMillis(cacheExpireMillis);
        }

        /**
         * 基于已有 Config 复制构造 Builder。
         *
         * @param copy 源配置
         * @return 复制了源配置的 Builder 实例
         */
        public static Builder builder(Config copy) {
            Builder builder = new Builder();
            builder.cacheName = copy.getCacheName();
            builder.localCacheMaxNum = copy.getLocalCacheMaxNum();
            builder.cacheExpireMillis = copy.getCacheExpireMillis();
            builder.nullProtectMillis = copy.getNullProtectMillis();
            builder.failProtectMillis = copy.getFailProtectMillis();
            builder.reloadIntervalMillis = copy.getReloadIntervalMillis();
            builder.reloadMaxTimes = copy.getReloadMaxTimes();
            builder.autoNotifyInvalidate = copy.isAutoNotifyInvalidate();
            builder.isGlobalCache = copy.isGlobalCache();
            return builder;
        }


        /**
         * 通过实体类设置缓存名（取 simpleName）。
         *
         * @param entityClass 缓存对象类
         */
        public void setEntityClass(Class<?> entityClass) {
            this.cacheName = entityClass.getSimpleName();
        }

        /**
         * 获取缓存名。
         *
         * @return 缓存名
         */
        public String getCacheName() {
            return cacheName;
        }

        /**
         * 设置缓存名。
         *
         * @param cacheName 缓存名
         */
        public void setCacheName(String cacheName) {
            this.cacheName = cacheName;
        }

        /**
         * 获取本地缓存最大数量。
         *
         * @return 本地缓存最大数量
         */
        public int getLocalCacheMaxNum() {
            return localCacheMaxNum;
        }

        /**
         * 设置本地缓存最大数量。
         *
         * @param localCacheMaxNum 本地缓存最大数量
         */
        public void setLocalCacheMaxNum(int localCacheMaxNum) {
            this.localCacheMaxNum = localCacheMaxNum;
        }

        /**
         * 获取缓存有效期毫秒数。
         *
         * @return 缓存有效期毫秒数，-1/0 表示永久
         */
        public long getCacheExpireMillis() {
            return cacheExpireMillis;
        }

        /**
         * 设置缓存有效期毫秒数。
         *
         * @param cacheExpireMillis 缓存有效期毫秒数，-1/0 表示永久
         */
        public void setCacheExpireMillis(long cacheExpireMillis) {
            this.cacheExpireMillis = cacheExpireMillis;
        }

        /**
         * 获取空值保护毫秒数。
         *
         * @return 空值保护毫秒数
         */
        public long getNullProtectMillis() {
            return nullProtectMillis;
        }

        /**
         * 设置空值保护毫秒数。
         *
         * @param nullProtectMillis 空值保护毫秒数
         */
        public void setNullProtectMillis(long nullProtectMillis) {
            this.nullProtectMillis = nullProtectMillis;
        }

        /**
         * 获取失败保护毫秒数。
         *
         * @return 失败保护毫秒数
         */
        public long getFailProtectMillis() {
            return failProtectMillis;
        }

        /**
         * 设置失败保护毫秒数。
         *
         * @param failProtectMillis 失败保护毫秒数
         */
        public void setFailProtectMillis(long failProtectMillis) {
            this.failProtectMillis = failProtectMillis;
        }

        /**
         * 获取重载间隔毫秒数。
         *
         * @return 重载间隔毫秒数
         */
        public long getReloadIntervalMillis() {
            return reloadIntervalMillis;
        }

        /**
         * 设置重载间隔毫秒数。
         *
         * @param reloadIntervalMillis 重载间隔毫秒数
         */
        public void setReloadIntervalMillis(long reloadIntervalMillis) {
            this.reloadIntervalMillis = reloadIntervalMillis;
        }

        /**
         * 获取重载最大次数。
         *
         * @return 重载最大次数
         */
        public int getReloadMaxTimes() {
            return reloadMaxTimes;
        }

        /**
         * 设置重载最大次数。
         *
         * @param reloadMaxTimes 重载最大次数
         */
        public void setReloadMaxTimes(int reloadMaxTimes) {
            this.reloadMaxTimes = reloadMaxTimes;
        }

        /**
         * 是否启用加载成功后自动通知集群失效。
         *
         * @return true 表示启用
         */
        public boolean isAutoNotifyInvalidate() {
            return autoNotifyInvalidate;
        }

        /**
         * 设置是否加载成功后自动通知集群失效。
         *
         * @param autoNotifyInvalidate true 表示启用
         */
        public void setAutoNotifyInvalidate(boolean autoNotifyInvalidate) {
            this.autoNotifyInvalidate = autoNotifyInvalidate;
        }

        /**
         * 是否启用全局（Redis）缓存。
         *
         * @return true 表示全局缓存，false 表示纯本地缓存
         */
        public boolean isGlobalCache() {
            return isGlobalCache;
        }

        /**
         * 设置是否启用全局（Redis）缓存。
         *
         * @param globalCache true 表示全局缓存
         */
        public void setGlobalCache(boolean globalCache) {
            isGlobalCache = globalCache;
        }

        public static final class Builder {

            /**
             * 缓存名。
             */
            private String cacheName;

            /**
             * 本地缓存最大数量，默认10000。
             */
            private int localCacheMaxNum = 10000;

            /**
             * 全局缓存有效期毫秒数，默认为-1。
             * 设置为0的时候，表示永不过期。
             * 设置为-1的时候，表示不使用全局缓存。
             * 鉴于redis的特性，一般建议设置一个有效期，防止redis爆库。
             */
            private long cacheExpireMillis = -1;
            /**
             * 空值保护毫秒数，默认为60秒。
             * 当reload方法获取null的时候，将会保护一段时间，防穿透。
             */
            private long nullProtectMillis = 60_000L;

            /**
             * 失败保护毫秒数，默认为60秒。
             * 当reload方法没有获取数据的时候，将会保护一段时间，防穿透。
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
             * 是否自动通知集群缓存失效。
             * 加载成功的缓存，将会通知集群缓存失效以便更新。
             * 默认不启用。
             */
            private boolean autoNotifyInvalidate = false;

            /**
             * 是否全局缓存。
             * 默认为true。
             */
            private boolean isGlobalCache = true;


            /**
             * 私有构造，通过 {@link Config#builder()} 获取实例。
             */
            private Builder() {
            }

            /**
             * 创建 Builder 实例。
             *
             * @return Builder 实例
             */
            public static Builder builder() {
                return new Builder();
            }

            /**
             * 通过实体类设置缓存名（取 simpleName）。
             *
             * @param entityClass 缓存对象类
             * @return 当前 Builder
             */
            public Builder entityClass(Class<?> entityClass) {
                this.cacheName = entityClass.getSimpleName();
                return this;
            }

            /**
             * 设置缓存名。
             *
             * @param cacheName 缓存名
             * @return 当前 Builder
             */
            public Builder cacheName(String cacheName) {
                this.cacheName = cacheName;
                return this;
            }

            /**
             * 设置本地缓存最大数量。
             *
             * @param localCacheMaxNum 本地缓存最大数量
             * @return 当前 Builder
             */
            public Builder localCacheMaxNum(int localCacheMaxNum) {
                this.localCacheMaxNum = localCacheMaxNum;
                return this;
            }

            /**
             * 设置缓存有效期毫秒数。
             *
             * @param cacheExpireMillis 缓存有效期毫秒数，-1/0 表示永久
             * @return 当前 Builder
             */
            public Builder cacheExpireMillis(long cacheExpireMillis) {
                this.cacheExpireMillis = cacheExpireMillis;
                return this;
            }

            /**
             * 设置空值保护毫秒数。
             *
             * @param nullProtectMillis 空值保护毫秒数
             * @return 当前 Builder
             */
            public Builder nullProtectMillis(long nullProtectMillis) {
                this.nullProtectMillis = nullProtectMillis;
                return this;
            }

            /**
             * 设置失败保护毫秒数。
             *
             * @param failProtectMillis 失败保护毫秒数
             * @return 当前 Builder
             */
            public Builder failProtectMillis(long failProtectMillis) {
                this.failProtectMillis = failProtectMillis;
                return this;
            }

            /**
             * 设置重载间隔毫秒数。
             *
             * @param reloadIntervalMillis 重载间隔毫秒数
             * @return 当前 Builder
             */
            public Builder reloadIntervalMillis(long reloadIntervalMillis) {
                this.reloadIntervalMillis = reloadIntervalMillis;
                return this;
            }

            /**
             * 设置重载最大次数。
             *
             * @param reloadMaxTimes 重载最大次数
             * @return 当前 Builder
             */
            public Builder reloadMaxTimes(int reloadMaxTimes) {
                this.reloadMaxTimes = reloadMaxTimes;
                return this;
            }

            /**
             * 设置是否加载成功后自动通知集群失效。
             *
             * @param autoNotifyInvalidate true 表示启用
             * @return 当前 Builder
             */
            public Builder autoNotifyInvalidate(boolean autoNotifyInvalidate) {
                this.autoNotifyInvalidate = autoNotifyInvalidate;
                return this;
            }

            /**
             * 设置是否启用全局（Redis）缓存。
             *
             * @param globalCache true 表示全局缓存
             * @return 当前 Builder
             */
            public Builder globalCache(boolean globalCache) {
                this.isGlobalCache = globalCache;
                return this;
            }

            /**
             * 构建不可变的 Config 实例。
             *
             * @return Config 实例
             */
            public Config build() {
                return new Config(this);
            }
        }
    }

}
