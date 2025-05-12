package uw.cache.loader;

import com.github.benmanes.caffeine.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.cache.CacheDataLoader;
import uw.cache.FusionCache;
import uw.cache.GlobalCache;
import uw.cache.vo.CacheProtectedValue;

/**
 * 全局融合缓存加载器。
 */
public class GlobalFusionCacheLoader<K, V> implements CacheLoader<K, V> {

    private static final Logger logger = LoggerFactory.getLogger(GlobalFusionCacheLoader.class);

    /**
     * 缓存配置。
     */
    private final FusionCache.Config cacheConfig;

    /**
     * 加载数据的函数。
     */
    private final CacheDataLoader<K, V> cacheDataLoader;

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
        try {
            // 加载数据。
            V value = GlobalCache.loadWithProtectedValue(cacheConfig.getCacheName(), key, cacheDataLoader, cacheConfig.getGlobalCacheExpireMillis(),
                    cacheConfig.getNullProtectMillis(), cacheConfig.getFailProtectMillis(), cacheConfig.getReloadIntervalMillis(), cacheConfig.getReloadMaxTimes());
            //此处通知invalidate缓存。
            if (cacheConfig.isAutoNotifyInvalidate()) {
                FusionCache.notifyInvalidate(cacheConfig.getCacheName(), key);
            }
            // 返回数据。
            return value;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return (V) new CacheProtectedValue(cacheConfig.getFailProtectMillis());
    }


}