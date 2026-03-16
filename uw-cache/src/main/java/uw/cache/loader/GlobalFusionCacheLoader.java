package uw.cache.loader;

import com.github.benmanes.caffeine.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.cache.CacheDataLoader;
import uw.cache.FusionCache;
import uw.cache.GlobalCache;
import uw.cache.vo.CacheValueWrapper;

/**
 * 全局融合缓存加载器。
 */
public class GlobalFusionCacheLoader<K, V> implements CacheLoader<K, CacheValueWrapper<V>> {

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
    public CacheValueWrapper<V> load(K key) {
        try {
            // 加载数据。
            CacheValueWrapper<V> valueWrapper = GlobalCache.loadValueWrapper(cacheConfig.getCacheName(), key, cacheDataLoader, cacheConfig.getCacheExpireMillis(),
                    cacheConfig.getNullProtectMillis(), cacheConfig.getFailProtectMillis(), cacheConfig.getReloadIntervalMillis(), cacheConfig.getReloadMaxTimes());
            //此处通知invalidate缓存。
            if (cacheConfig.isAutoNotifyInvalidate()) {
                FusionCache.notifyInvalidate(cacheConfig.getCacheName(), key);
            }
            // 返回数据。
            return valueWrapper;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return new CacheValueWrapper<>(cacheConfig.getNullProtectMillis());
    }


}