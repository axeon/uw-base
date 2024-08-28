package uw.cache;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.cache.constant.CacheNotifyType;
import uw.cache.loader.GlobalFusionCacheLoader;
import uw.cache.loader.LocalRetryCacheLoader;
import uw.cache.loader.NoneCacheLoader;
import uw.cache.vo.CacheProtectedValue;
import uw.cache.vo.FusionCacheNotifyMessage;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于caffeine和redis构建的复合缓存，可以大幅度提升效率。
 */
public class FusionCache {

    /**
     * Fusion缓存通知channel。
     */
    public static final String FUSION_CACHE_NOTIFY_CHANNEL = "UW_CACHE_NOTIFY_CHANNEL";

    private static final Logger log = LoggerFactory.getLogger( FusionCache.class );

    /**
     * key是缓存名，value是Cache实例。
     */
    private static ConcurrentMap<String, LoadingCache> cacheMap = new ConcurrentHashMap<>();

    /**
     * key是缓存名，value是Config。
     */
    private static ConcurrentMap<String, Config> configMap = new ConcurrentHashMap<>();

    /**
     * 配置缓存。
     *
     * @param config 缓存配置
     */
    public static void config(Config config) {
        config( config, null, null );
    }

    /**
     * 配置缓存。
     *
     * @param config 缓存配置
     */
    public static void config(Config config, CacheDataLoader cacheDataLoader) {
        config( config, cacheDataLoader, null );
    }

    /**
     * 配置缓存。
     *
     * @param config 缓存配置
     */
    public static void config(Config config, CacheChangeNotifyListener cacheChangeNotifyListener) {
        config( config, null, cacheChangeNotifyListener );
    }

    /**
     * 配置缓存。
     *
     * @param config          缓存配置
     * @param cacheDataLoader 缓存数据加载器
     */
    public static void config(Config config, CacheDataLoader cacheDataLoader, CacheChangeNotifyListener cacheChangeNotifyListener) {
        Caffeine caffeine = Caffeine.newBuilder();
        if (config.getLocalCacheMaxNum() > 0) {
            caffeine.maximumSize( config.getLocalCacheMaxNum() );
        }
        //缓存过期设置特别影响性能，所以能不设置就不要设置了。
        if (config.getLocalCacheExpireMillis() > 0) {
            caffeine.expireAfterWrite( config.getLocalCacheExpireMillis(), TimeUnit.MILLISECONDS );
        }
        //设置失效监听
        config.cacheChangeNotifyListener = cacheChangeNotifyListener;
        CacheLoader cacheLoader = null;
        if (cacheDataLoader != null) {
            if (config.getGlobalCacheExpireMillis() > -1) {
                cacheLoader = new GlobalFusionCacheLoader( config, cacheDataLoader );
            } else {
                cacheLoader = new LocalRetryCacheLoader( config, cacheDataLoader );
            }
        } else {
            cacheLoader = new NoneCacheLoader();
        }

        LoadingCache caffeineCache = caffeine.build( cacheLoader );
        cacheMap.put( config.getCacheName(), caffeineCache );
        configMap.put( config.getCacheName(), config );
    }

    /**
     * 向缓存中存入数据。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     * @param value       数值
     * @return
     */
    public static void put(Class entityClass, Object key, Object value) {
        put( entityClass.getSimpleName(), key, value, false );
    }

    /**
     * 向缓存中存入数据。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     * @param value       数值
     * @param onlyLocal   只更新到本地缓存
     * @return
     */
    public static void put(Class entityClass, Object key, Object value, boolean onlyLocal) {
        put( entityClass.getSimpleName(), key, value, onlyLocal );
    }

    /**
     * 向缓存中存入数据。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     * @param value     数值
     * @return
     */
    public static void put(String cacheName, Object key, Object value) {
        put( cacheName, key, value, false );
    }

    /**
     * 向缓存中存入数据。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     * @param value     数值
     * @param onlyLocal 只更新到本地缓存
     * @return
     */
    public static void put(String cacheName, Object key, Object value, boolean onlyLocal) {
        Config config = configMap.get( cacheName );
        if (!onlyLocal && config != null && config.getGlobalCacheExpireMillis() >= 0) {
            GlobalCache.put( cacheName, key, value, config.getGlobalCacheExpireMillis() );
        }
        LoadingCache cache = getLocalCache( cacheName );
        if (cache != null) {
            cache.put( key, value );
        }
    }

    /**
     * 向缓存中批量存入数据。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param map         要存入的批量数据
     */
    public static void putAll(Class entityClass, Map<Object, Object> map) {
        putAll( entityClass.getSimpleName(), map, false );
    }

    /**
     * 向缓存中批量存入数据。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param map         要存入的批量数据
     * @param onlyLocal   只更新到本地缓存
     */
    public static void putAll(Class entityClass, Map<Object, Object> map, boolean onlyLocal) {
        putAll( entityClass.getSimpleName(), map, onlyLocal );
    }

    /**
     * 向缓存中批量存入数据。
     *
     * @param cacheName
     * @param map       要存入的批量数据
     */
    public static void putAll(String cacheName, Map<Object, Object> map) {
        putAll( cacheName, map, false );
    }

    /**
     * 向缓存中批量存入数据。
     *
     * @param cacheName
     * @param map       要存入的批量数据
     * @param onlyLocal 只更新到本地缓存
     */
    public static void putAll(String cacheName, Map<Object, Object> map, boolean onlyLocal) {
        Config config = configMap.get( cacheName );
        if (!onlyLocal && config != null && config.getGlobalCacheExpireMillis() >= 0) {
            for (Map.Entry<Object, Object> kv : map.entrySet()) {
                GlobalCache.put( cacheName, kv.getKey(), kv.getValue(), config.getGlobalCacheExpireMillis() );
            }
        }
        LoadingCache cache = getLocalCache( cacheName );
        if (cache != null) {
            cache.putAll( map );
        }
    }

    /**
     * 从缓存中加载数据。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     * @param <T>
     * @return
     */
    public static <T> T get(Class entityClass, Object key) {
        return get( entityClass.getSimpleName(), key );
    }

    /**
     * 从缓存中加载数据。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     * @param <T>
     * @return
     */
    public static <T> T get(String cacheName, Object key) {
        LoadingCache cache = getLocalCache( cacheName );
        Object value = cache.get( key );
        if (value instanceof CacheProtectedValue failProtectValue) {
            if (failProtectValue.isExpired()) {
                //此处必须全局通知。
                invalidate( cacheName, key, true );
                return get( cacheName, key );
            } else {
                return null;
            }
        } else {
            return (T) value;
        }
    }


    /**
     * 缓存中是否存在指定Key。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     * @return
     */
    public static boolean containsKey(Class entityClass, Object key) {
        return containsKey( entityClass.getSimpleName(), key );
    }

    /**
     * 缓存中是否存在指定Key。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     * @return
     */
    public static boolean containsKey(String cacheName, Object key) {
        return get( cacheName, key ) != null;
    }

    /**
     * 获得指定缓存大小。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @return
     */
    public static long size(Class entityClass) {
        return size( entityClass.getSimpleName() );
    }

    /**
     * 获得指定缓存大小。
     *
     * @param cacheName 缓存名
     * @return
     */
    public static long size(String cacheName) {
        LoadingCache cache = getLocalCache( cacheName );
        return cache.estimatedSize();
    }

    /**
     * 以Map形式返回指定缓存内容。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @return
     */
    public static ConcurrentMap asMap(Class entityClass) {
        return asMap( entityClass.getSimpleName() );
    }

    /**
     * 以Map形式返回指定缓存内容。
     *
     * @param cacheName 缓存名
     * @return
     */
    public static ConcurrentMap asMap(String cacheName) {
        LoadingCache cache = getLocalCache( cacheName );
        return cache.asMap();
    }

    /**
     * 获得缓存。
     *
     * @param cacheName
     * @return
     */
    public static LoadingCache getLocalCache(String cacheName) {
        LoadingCache cache = cacheMap.get( cacheName );
        if (cache == null) {
            log.warn( "FusionCache[{}] not config!!!", cacheName );
            return null;
        }
        return cache;
    }

    /**
     * 获得缓存。
     *
     * @param entityClass
     * @return
     */
    public static LoadingCache getLocalCache(Class entityClass) {
        return getLocalCache( entityClass.getSimpleName() );
    }

    /**
     * 获得指定缓存统计信息。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @return
     */
    public static CacheStats stats(Class entityClass) {
        return stats( entityClass.getSimpleName() );
    }

    /**
     * 获得指定缓存统计信息。
     *
     * @param cacheName 缓存名
     * @return
     */
    public static CacheStats stats(String cacheName) {
        LoadingCache cache = getLocalCache( cacheName );
        return cache.stats();
    }

    /**
     * 是否包含某个cache。
     *
     * @param entityClass
     * @return
     */
    public static boolean existsCache(Class entityClass) {
        return cacheMap.containsKey( entityClass.getSimpleName() );
    }

    /**
     * 是否包含某个cache。
     *
     * @param cacheName
     * @return
     */
    public static boolean existsCache(String cacheName) {
        return cacheMap.containsKey( cacheName );
    }

    /**
     * 从缓存中删除一个对象。
     * 默认通知集群内其他主机。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     */
    public static boolean invalidate(Class entityClass, Object key) {
        return invalidate( entityClass.getSimpleName(), key, true );
    }

    /**
     * 从缓存中删除一个对象。
     * 默认通知集群内其他主机。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     */
    public static boolean invalidate(String cacheName, Object key) {
        return invalidate( cacheName, key, true );
    }

    /**
     * 从缓存中删除一个对象。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     * @param notify      是否通知集群内其他主机。
     */
    public static boolean invalidate(Class entityClass, Object key, boolean notify) {
        return invalidate( entityClass.getSimpleName(), key, notify );
    }

    /**
     * 从缓存中删除一个对象。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     * @param notify    是否通知集群内其他主机。
     */
    public static boolean invalidate(String cacheName, Object key, boolean notify) {
        if (notify) {
            //先删除redis缓存
            GlobalCache.invalidate( cacheName, key );
            //发布通知
            GlobalCache.notifyMsg( FUSION_CACHE_NOTIFY_CHANNEL, new FusionCacheNotifyMessage( cacheName, CacheNotifyType.INVALIDATE.getValue(), key ) );
        }
        //处理自身记录。
        LoadingCache cache = cacheMap.get( cacheName );
        Config config = configMap.get( cacheName );
        if (cache == null || config == null) {
            log.warn( "FusionCache[{}] not config!!!", cacheName );
            return false;
        }
        if (key == null) {
            //拉出所有数据执行监听。
            Set<Map.Entry> kvSet = cache.asMap().entrySet();
            cache.invalidateAll();
            if (config.cacheChangeNotifyListener != null) {
                for (Map.Entry kv : kvSet) {
                    Object oldValue = kv.getValue();
                    if (oldValue instanceof CacheProtectedValue) {
                        oldValue = null;
                    }
                    try {
                        config.cacheChangeNotifyListener.onMessage( kv.getKey(), oldValue, null );
                    } catch (Exception e) {
                        log.error( e.getMessage(), e );
                    }

                }
            }
        } else {
            Object oldValue = cache.getIfPresent( key );
            //对于没有的信息，不需要执行通知监听。
            if (oldValue != null) {
                cache.invalidate( key );
                if (config.cacheChangeNotifyListener != null) {
                    if (oldValue instanceof CacheProtectedValue) {
                        oldValue = null;
                    }
                    try {
                        config.cacheChangeNotifyListener.onMessage( key, oldValue, null );
                    } catch (Exception e) {
                        log.error( e.getMessage(), e );
                    }

                }
            }
        }
        return true;
    }


    /**
     * 刷新一个对象。
     * 默认通知集群内其他主机。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     */
    public static boolean refresh(Class entityClass, Object key) {
        return refresh( entityClass.getSimpleName(), key, true );
    }

    /**
     * 刷新一个对象。
     * 默认通知集群内其他主机。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     */
    public static boolean refresh(String cacheName, Object key) {
        return refresh( cacheName, key, true );
    }

    /**
     * 刷新一个对象。
     *
     * @param entityClass 缓存对象类(主要用于构造cacheName)
     * @param key         缓存主键
     * @param notify      是否通知集群内其他主机。
     */
    public static boolean refresh(Class entityClass, Object key, boolean notify) {
        return refresh( entityClass.getSimpleName(), key, notify );
    }

    /**
     * 刷新一个对象。
     *
     * @param cacheName 缓存名
     * @param key       缓存主键
     * @param notify    是否通知集群内其他主机。
     */
    public static boolean refresh(String cacheName, Object key, boolean notify) {
        if (notify) {
            //先删除redis缓存
            GlobalCache.invalidate( cacheName, key );
            //发布通知
            GlobalCache.notifyMsg( FUSION_CACHE_NOTIFY_CHANNEL, new FusionCacheNotifyMessage( cacheName, CacheNotifyType.REFRESH.getValue(), key ) );
        }
        //处理自身记录。
        LoadingCache cache = cacheMap.get( cacheName );
        Config config = configMap.get( cacheName );
        if (cache == null || config == null) {
            log.warn( "FusionCache[{}] not config!!!", cacheName );
            return false;
        }
        //先执行监听。
        if (key == null) {
            //拉出所有数据执行监听。
            Set<Map.Entry> kvSet = cache.asMap().entrySet();
            for (Map.Entry kv : kvSet) {
                cache.invalidate( kv.getKey() );
                Object oldValue = kv.getValue();
                Object newValue = cache.get( kv.getKey() );
                if (config.cacheChangeNotifyListener != null) {
                    if (oldValue instanceof CacheProtectedValue) {
                        oldValue = null;
                    }
                    if (newValue instanceof CacheProtectedValue) {
                        newValue = null;
                    }
                    try {
                        config.cacheChangeNotifyListener.onMessage( kv.getKey(), oldValue, newValue );
                    } catch (Exception e) {
                        log.error( e.getMessage(), e );
                    }

                }
            }
        } else {
            Object oldValue = cache.getIfPresent( key );
            if (oldValue != null) {
                cache.invalidate( key );
            }
            Object newValue = cache.get( key );
            if (config.cacheChangeNotifyListener != null) {
                if (oldValue instanceof CacheProtectedValue) {
                    oldValue = null;
                }
                if (newValue instanceof CacheProtectedValue) {
                    newValue = null;
                }
                try {
                    config.cacheChangeNotifyListener.onMessage( key, oldValue, newValue );
                } catch (Exception e) {
                    log.error( e.getMessage(), e );
                }
            }
        }
        return true;
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
         * 本地缓存最大数量，默认10000。
         * 设置为0，表示不限制数量。
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
         * 空值保护毫秒数，默认为60秒。
         * 当reload方法获得null的时候，将会保护一段时间，防穿透。
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
         * 缓存作废监听器。
         */
        private CacheChangeNotifyListener cacheChangeNotifyListener;

        public Config() {
        }

        /**
         * 常用构造器。
         *
         * @param entityClass
         * @param localCacheMaxNum
         * @param globalCacheExpireMillis
         */
        public Config(Class entityClass, int localCacheMaxNum, long globalCacheExpireMillis) {
            this.cacheName = entityClass.getSimpleName();
            this.localCacheMaxNum = localCacheMaxNum;
            this.globalCacheExpireMillis = globalCacheExpireMillis;
        }

        /**
         * 常用构造器。
         *
         * @param cacheName
         * @param localCacheMaxNum
         * @param globalCacheExpireMillis
         */
        public Config(String cacheName, int localCacheMaxNum, long globalCacheExpireMillis) {
            this.cacheName = cacheName;
            this.localCacheMaxNum = localCacheMaxNum;
            this.globalCacheExpireMillis = globalCacheExpireMillis;
        }

        /**
         * 常用构造器。
         *
         * @param entityClass
         * @param localCacheMaxNum
         * @param globalCacheExpireMillis
         */
        public Config(Class entityClass, int localCacheMaxNum, long globalCacheExpireMillis, long nullProtectMillis, long failProtectMillis) {
            this.cacheName = entityClass.getSimpleName();
            this.localCacheMaxNum = localCacheMaxNum;
            this.globalCacheExpireMillis = globalCacheExpireMillis;
            this.nullProtectMillis = nullProtectMillis;
            this.failProtectMillis = failProtectMillis;
        }

        /**
         * 常用构造器。
         *
         * @param cacheName
         * @param localCacheMaxNum
         * @param globalCacheExpireMillis
         */
        public Config(String cacheName, int localCacheMaxNum, long globalCacheExpireMillis, long nullProtectMillis, long failProtectMillis) {
            this.cacheName = cacheName;
            this.localCacheMaxNum = localCacheMaxNum;
            this.globalCacheExpireMillis = globalCacheExpireMillis;
            this.nullProtectMillis = nullProtectMillis;
            this.failProtectMillis = failProtectMillis;
        }


        private Config(Builder builder) {
            setCacheName( builder.cacheName );
            setLocalCacheMaxNum( builder.localCacheMaxNum );
            setLocalCacheExpireMillis( builder.localCacheExpireMillis );
            setGlobalCacheExpireMillis( builder.globalCacheExpireMillis );
            setNullProtectMillis( builder.nullProtectMillis );
            setFailProtectMillis( builder.failProtectMillis );
            setReloadIntervalMillis( builder.reloadIntervalMillis );
            setReloadMaxTimes( builder.reloadMaxTimes );
        }

        public static Builder builder() {
            return new Builder();
        }

        public static Builder builder(Config copy) {
            Builder builder = new Builder();
            builder.cacheName = copy.getCacheName();
            builder.localCacheMaxNum = copy.getLocalCacheMaxNum();
            builder.localCacheExpireMillis = copy.getLocalCacheExpireMillis();
            builder.globalCacheExpireMillis = copy.getGlobalCacheExpireMillis();
            builder.nullProtectMillis = copy.getNullProtectMillis();
            builder.failProtectMillis = copy.getFailProtectMillis();
            builder.reloadIntervalMillis = copy.getReloadIntervalMillis();
            builder.reloadMaxTimes = copy.getReloadMaxTimes();
            return builder;
        }


        public void setEntityClass(Class entityClass) {
            this.cacheName = entityClass.getSimpleName();
        }

        public String getCacheName() {
            return cacheName;
        }

        public void setCacheName(String cacheName) {
            this.cacheName = cacheName;
        }

        public int getLocalCacheMaxNum() {
            return localCacheMaxNum;
        }

        public void setLocalCacheMaxNum(int localCacheMaxNum) {
            this.localCacheMaxNum = localCacheMaxNum;
        }

        public long getLocalCacheExpireMillis() {
            return localCacheExpireMillis;
        }

        public void setLocalCacheExpireMillis(long localCacheExpireMillis) {
            this.localCacheExpireMillis = localCacheExpireMillis;
        }

        public long getGlobalCacheExpireMillis() {
            return globalCacheExpireMillis;
        }

        public void setGlobalCacheExpireMillis(long globalCacheExpireMillis) {
            this.globalCacheExpireMillis = globalCacheExpireMillis;
        }

        public long getNullProtectMillis() {
            return nullProtectMillis;
        }

        public void setNullProtectMillis(long nullProtectMillis) {
            this.nullProtectMillis = nullProtectMillis;
        }

        public long getFailProtectMillis() {
            return failProtectMillis;
        }

        public void setFailProtectMillis(long failProtectMillis) {
            this.failProtectMillis = failProtectMillis;
        }

        public long getReloadIntervalMillis() {
            return reloadIntervalMillis;
        }

        public void setReloadIntervalMillis(long reloadIntervalMillis) {
            this.reloadIntervalMillis = reloadIntervalMillis;
        }

        public int getReloadMaxTimes() {
            return reloadMaxTimes;
        }

        public void setReloadMaxTimes(int reloadMaxTimes) {
            this.reloadMaxTimes = reloadMaxTimes;
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
             * 本地缓存有效期毫秒数，默认0。
             * 设置为0的时候，表示永不过期。
             * 设置为-1的时候，表示不使用全局缓存。
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
             * 空值保护毫秒数，默认为60秒。
             * 当reload方法获得null的时候，将会保护一段时间，防穿透。
             */
            private long nullProtectMillis = 60_000L;

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

            private Builder() {
            }

            public static Builder builder() {
                return new Builder();
            }

            public Builder entityClass(Class entityClass) {
                this.cacheName = entityClass.getSimpleName();
                return this;
            }

            public Builder cacheName(String cacheName) {
                this.cacheName = cacheName;
                return this;
            }

            public Builder localCacheMaxNum(int localCacheMaxNum) {
                this.localCacheMaxNum = localCacheMaxNum;
                return this;
            }

            public Builder localCacheExpireMillis(long localCacheExpireMillis) {
                this.localCacheExpireMillis = localCacheExpireMillis;
                return this;
            }

            public Builder globalCacheExpireMillis(long globalCacheExpireMillis) {
                this.globalCacheExpireMillis = globalCacheExpireMillis;
                return this;
            }

            public Builder nullProtectMillis(long nullProtectMillis) {
                this.nullProtectMillis = nullProtectMillis;
                return this;
            }

            public Builder failProtectMillis(long failProtectMillis) {
                this.failProtectMillis = failProtectMillis;
                return this;
            }

            public Builder reloadIntervalMillis(long reloadIntervalMillis) {
                this.reloadIntervalMillis = reloadIntervalMillis;
                return this;
            }

            public Builder reloadMaxTimes(int reloadMaxTimes) {
                this.reloadMaxTimes = reloadMaxTimes;
                return this;
            }

            public Config build() {
                return new Config( this );
            }
        }
    }

}
