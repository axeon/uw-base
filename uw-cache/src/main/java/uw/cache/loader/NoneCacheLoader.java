package uw.cache.loader;

import com.github.benmanes.caffeine.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 空CacheLoader，主要是为了适配LoadingCache。。
 * 主要在load失败后重试指定次数,如果加载仍然失败将抛出运行时异常
 */
public class NoneCacheLoader<K, V> implements CacheLoader<K, V> {
    private static final Logger logger = LoggerFactory.getLogger( NoneCacheLoader.class );

    public NoneCacheLoader() {
    }

    /**
     * 加载数据。
     *
     * @param key
     * @return
     * @throws Exception
     */
    @Override
    public V load(K key) {
        return null;
    }
}
