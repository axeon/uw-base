package uw.cache;

/**
 * 缓存变更通知监听器。
 * <p>
 * 当 FusionCache 发生 invalidate（失效）或 refresh（刷新）时，若 Config 配置了监听器，
 * 将回调 {@link #onMessage} 通知业务方处理新旧值差异（如清理关联缓存、推送事件等）。
 * <p>
 * 注意：回调在执行 invalidate/refresh 的线程中同步执行，实现方应避免耗时操作，否则会阻塞缓存主流程。
 *
 * @param <K> 缓存主键类型
 * @param <V> 缓存值类型
 */
public interface CacheChangeNotifyListener<K, V> {

    /**
     * 响应缓存变更消息。
     *
     * @param key      缓存主键
     * @param oldValue 变更前的旧数值，invalidate 场景为被清除的值，可能为 null
     * @param newValue 变更后的新数值，invalidate 场景固定为 null，refresh 场景为重新加载后的值
     */
    void onMessage(K key, V oldValue, V newValue);

}
