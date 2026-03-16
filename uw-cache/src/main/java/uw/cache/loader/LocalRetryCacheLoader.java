package uw.cache.loader;

import com.github.benmanes.caffeine.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.cache.CacheDataLoader;
import uw.cache.FusionCache;
import uw.cache.vo.CacheValueWrapper;

/**
 * 重试缓存Loader
 * 主要在load失败后重试指定次数,如果加载仍然失败将抛出运行时异常
 */
public class LocalRetryCacheLoader<K, V> implements CacheLoader<K, CacheValueWrapper<V>> {

    private static final Logger logger = LoggerFactory.getLogger(LocalRetryCacheLoader.class);
    /**
     * 缓存配置。
     */
    private final FusionCache.Config cacheConfig;

    /**
     * 加载数据的函数。
     */
    private final CacheDataLoader<K, V> cacheDataLoader;

    public LocalRetryCacheLoader(FusionCache.Config cacheConfig, CacheDataLoader cacheDataLoader) {
        this.cacheConfig = cacheConfig;
        this.cacheDataLoader = cacheDataLoader;
    }

    /**
     * 加载数据。
     *
     * @param key
     * @return
     * @throws Exception
     */
    @Override
    public CacheValueWrapper<V> load(K key) {
        int retryTimes = 0;
        do {
            try {
                V value = cacheDataLoader.load(key);
                if (value == null) {
                    return new CacheValueWrapper<>(cacheConfig.getNullProtectMillis());
                }
                // 获取缓存的过期时间
                long expiredMillis = cacheDataLoader.getExpireMillis();
                if (expiredMillis <= 0) {
                    expiredMillis = cacheConfig.getCacheExpireMillis();
                }
                //此处通知invalidate缓存。
                if (cacheConfig.isAutoNotifyInvalidate()) {
                    FusionCache.notifyInvalidate(cacheConfig.getCacheName(), key);
                }
                return new CacheValueWrapper<>(value, expiredMillis);
            } catch (Throwable e) {
                logger.error("Local数据加载失败! cacheName:{}, key:{}, retryTimes:{}, msg:{}", cacheConfig.getCacheName(), key, retryTimes, e.getMessage(), e);
            }
            try {
                Thread.sleep(cacheConfig.getReloadIntervalMillis());
            } catch (InterruptedException ignored) {
            }
            retryTimes++;
        } while (retryTimes < cacheConfig.getReloadMaxTimes());
        return new CacheValueWrapper(cacheConfig.getFailProtectMillis());
    }

}
