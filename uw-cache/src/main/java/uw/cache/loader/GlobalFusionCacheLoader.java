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
 * <p>
 * 作为 Caffeine {@code CacheLoader} 的适配实现，当本地缓存未命中时，
 * 委托给 {@link GlobalCache#loadValueWrapper} 从 Redis 加载（含 JVM 锁防击穿、重试、空值/失败保护）。
 * 可选通过 {@code autoNotifyInvalidate} 在加载成功后通知集群其他实例失效旧值。
 *
 * @param <K> 缓存主键类型
 * @param <V> 缓存值类型
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

    /**
     * 构造加载器。
     *
     * @param cacheConfig     缓存配置
     * @param cacheDataLoader 数据加载函数
     */
    public GlobalFusionCacheLoader(FusionCache.Config cacheConfig, CacheDataLoader<K, V> cacheDataLoader) {
        this.cacheConfig = cacheConfig;
        this.cacheDataLoader = cacheDataLoader;
    }

    /**
     * 加载缓存值包装对象。
     * <p>
     * 内部调用 {@link GlobalCache#loadValueWrapper}，其已封装：Redis 读取、JVM 锁防击穿、
     * loader 重试、空值保护与失败保护。本方法仅兜底捕获非预期异常，此时返回 failProtect 时长的空 wrapper。
     *
     * @param key 缓存主键
     * @return 缓存值包装对象（永不为 null，加载失败时返回失败保护空值）
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
            // 正常情况下 loadValueWrapper 内部已吞掉 loader 异常并走 nullProtect 分支，
            // 此处仅兜底捕获非预期异常（如 Redis 写入失败向上冒泡），此时应使用 failProtectMillis 而非 nullProtectMillis。
            logger.error("GlobalFusionCacheLoader 加载异常! cacheName:{}, key:{}", cacheConfig.getCacheName(), key, e);
        }
        return new CacheValueWrapper<>(cacheConfig.getFailProtectMillis());
    }


}