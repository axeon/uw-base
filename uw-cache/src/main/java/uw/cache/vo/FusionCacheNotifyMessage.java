package uw.cache.vo;

import uw.common.util.JsonUtils;

/**
 * 融合缓存通知消息。
 * 主要用于集群环境下，通知缓存失效。
 */
public class FusionCacheNotifyMessage {

    /**
     * 消息发送者ID
     */
    private long senderId;

    /**
     * 通知类型
     */
    private int notifyType;

    /**
     * 缓存名称
     */
    private String cacheName;

    /**
     * 缓存key
     */
    private Object cacheKey;

    public FusionCacheNotifyMessage() {
    }

    public FusionCacheNotifyMessage(long senderId, String cacheName, int notifyType, Object cacheKey) {
        this.senderId = senderId;
        this.cacheName = cacheName;
        this.notifyType = notifyType;
        this.cacheKey = cacheKey;
    }

    @Override
    public String toString() {
        return JsonUtils.toString( this);
    }

    public long getSenderId() {
        return senderId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public int getNotifyType() {
        return notifyType;
    }

    public void setNotifyType(int notifyType) {
        this.notifyType = notifyType;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public Object getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(Object cacheKey) {
        this.cacheKey = cacheKey;
    }
}
