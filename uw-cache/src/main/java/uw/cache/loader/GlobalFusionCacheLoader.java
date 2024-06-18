package uw.cache.loader;

import com.github.benmanes.caffeine.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.cache.CacheDataLoader;
import uw.cache.FusionCache;
import uw.cache.GlobalCache;
import uw.cache.vo.FailProtectValue;

/**
 * 全局融合缓存加载器。
 */
public class GlobalFusionCacheLoader<K, V> implements CacheLoader<K, V> {

    private static final Logger logger = LoggerFactory.getLogger( GlobalFusionCacheLoader.class );

    /**
     * 缓存配置。
     */
    private FusionCache.Config cacheConfig;

    /**
     * 加载数据的函数。
     */
    private CacheDataLoader<K, V> cacheDataLoader;

    public GlobalFusionCacheLoader(FusionCache.Config cacheConfig, CacheDataLoader<K, V> cacheDataLoader) {
        this.cacheConfig = cacheConfig;
        this.cacheDataLoader = cacheDataLoader;
    }

    /**
     * load
     *
     * @param key
     * @return
     * @throws Exception
     */
    @Override
    public V load(K key) {
        V value = GlobalCache.get( cacheConfig.getCacheName(), key, cacheDataLoader, cacheConfig.getGlobalCacheExpireMillis(), cacheConfig.getFailProtectMillis(),
                cacheConfig.getReloadIntervalMillis(), cacheConfig.getReloadMaxTimes() );
        if (value == null) {
            value = (V) new FailProtectValue( cacheConfig.getFailProtectMillis() );
        }
        return value;
    }


}