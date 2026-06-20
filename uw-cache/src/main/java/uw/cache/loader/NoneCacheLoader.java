package uw.cache.loader;

import com.github.benmanes.caffeine.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.cache.vo.CacheValueWrapper;

/**
 * 空缓存加载器。
 * <p>
 * 仅用于适配 Caffeine {@code LoadingCache} 的构造签名（build 需要 CacheLoader），
 * 用于 {@link uw.cache.FusionCache#config} 未提供 CacheDataLoader 的场景。
 * {@link #load} 恒返回 null，即此类缓存不会自动加载数据，只能通过 {@code put} 主动写入。
 *
 * @param <K> 缓存主键类型
 * @param <V> 缓存值类型
 */
public class NoneCacheLoader<K, V> implements CacheLoader<K, CacheValueWrapper<V>> {
    private static final Logger logger = LoggerFactory.getLogger(NoneCacheLoader.class);

    /**
     * 默认构造函数。
     */
    public NoneCacheLoader() {
    }

    /**
     * 恒返回 null，不执行任何加载。
     *
     * @param key 缓存主键
     * @return 始终为 null
     */
    @Override
    public CacheValueWrapper<V> load(K key) {
        return null;
    }
}
