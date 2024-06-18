package uw.cache;

/**
 * 缓存失效监听器。
 *
 * @param <K>
 * @param <V>
 */
public interface CacheChangeNotifyListener<K, V> {

    /**
     * 响应消息。
     * @param key key
     * @param oldValue 旧数值
     * @param newValue 新数值
     */
    void onMessage(K key, V oldValue, V newValue);

}
