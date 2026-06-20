package uw.cache.loader;

import com.github.benmanes.caffeine.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.cache.CacheDataLoader;
import uw.cache.FusionCache;
import uw.cache.vo.CacheValueWrapper;

/**
 * 本地重试缓存加载器。
 * <p>
 * 作为 Caffeine {@code CacheLoader} 的适配实现，适用于 {@code isGlobalCache=false} 的纯本地缓存场景。
 * 当本地缓存未命中时，直接调用 {@link CacheDataLoader#load} 加载数据，
 * 失败时按 {@code reloadIntervalMillis} 间隔重试最多 {@code reloadMaxTimes} 次。
 * <ul>
 *   <li>loader 返回 null：返回 nullProtect 时长的空值 wrapper（防穿透）。</li>
 *   <li>重试耗尽仍失败：返回 failProtect 时长的空值 wrapper（防穿透）。</li>
 * </ul>
 *
 * @param <K> 缓存主键类型
 * @param <V> 缓存值类型
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

    /**
     * 构造加载器。
     *
     * @param cacheConfig     缓存配置
     * @param cacheDataLoader 数据加载函数
     */
    public LocalRetryCacheLoader(FusionCache.Config cacheConfig, CacheDataLoader cacheDataLoader) {
        this.cacheConfig = cacheConfig;
        this.cacheDataLoader = cacheDataLoader;
    }

    /**
     * 加载缓存值包装对象（带重试）。
     *
     * @param key 缓存主键
     * @return 缓存值包装对象（永不为 null，加载失败或返回 null 时返回保护期空值 wrapper）
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
