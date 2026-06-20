package uw.cache.vo;

import uw.common.util.JsonUtils;

/**
 * 融合缓存通知消息。
 * <p>
 * 用于集群环境下，通过 Redis Pub/Sub（通道 {@code UW_CACHE_NOTIFY_CHANNEL}）通知各实例缓存失效或刷新。
 * 序列化采用 Kryo；{@code senderId} 为发送方实例的 SnowflakeId，接收方据此忽略自身发出的消息。
 *
 * @see uw.cache.FusionCache#notifyInvalidate
 * @see uw.cache.FusionCache#notifyRefresh
 */
public class FusionCacheNotifyMessage {

    /**
     * 消息发送者ID（SnowflakeId），接收方据此跳过自身发送的消息。
     */
    private long senderId;

    /**
     * 通知类型，取值见 {@link uw.cache.constant.CacheNotifyType}。
     */
    private int notifyType;

    /**
     * 缓存名称，与 FusionCache.config 注册的 cacheName 一致。
     */
    private String cacheName;

    /**
     * 缓存key，可为 null（表示全量失效/刷新）。
     */
    private Object cacheKey;

    /**
     * 默认构造函数，供 Kryo 反序列化使用。
     */
    public FusionCacheNotifyMessage() {
    }

    /**
     * 构造通知消息。
     *
     * @param senderId   发送者实例ID
     * @param cacheName  缓存名称
     * @param notifyType 通知类型，见 {@link uw.cache.constant.CacheNotifyType#getValue()}
     * @param cacheKey   缓存key，null 表示全量操作
     */
    public FusionCacheNotifyMessage(long senderId, String cacheName, int notifyType, Object cacheKey) {
        this.senderId = senderId;
        this.cacheName = cacheName;
        this.notifyType = notifyType;
        this.cacheKey = cacheKey;
    }

    /**
     * 输出消息的 JSON 表示，便于日志排查。
     *
     * @return JSON 字符串
     */
    @Override
    public String toString() {
        return JsonUtils.toString(this);
    }

    /**
     * 获取消息发送者ID。
     *
     * @return 发送者实例ID（SnowflakeId）
     */
    public long getSenderId() {
        return senderId;
    }

    /**
     * 设置消息发送者ID。
     *
     * @param senderId 发送者实例ID
     */
    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    /**
     * 获取通知类型。
     *
     * @return 通知类型值，见 {@link uw.cache.constant.CacheNotifyType#getValue()}
     */
    public int getNotifyType() {
        return notifyType;
    }

    /**
     * 设置通知类型。
     *
     * @param notifyType 通知类型值
     */
    public void setNotifyType(int notifyType) {
        this.notifyType = notifyType;
    }

    /**
     * 获取缓存名称。
     *
     * @return 缓存名称
     */
    public String getCacheName() {
        return cacheName;
    }

    /**
     * 设置缓存名称。
     *
     * @param cacheName 缓存名称
     */
    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    /**
     * 获取缓存key。
     *
     * @return 缓存key，null 表示全量操作
     */
    public Object getCacheKey() {
        return cacheKey;
    }

    /**
     * 设置缓存key。
     *
     * @param cacheKey 缓存key
     */
    public void setCacheKey(Object cacheKey) {
        this.cacheKey = cacheKey;
    }
}
