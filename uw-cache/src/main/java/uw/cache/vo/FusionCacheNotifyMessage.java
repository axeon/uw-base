package uw.cache.vo;

import java.util.StringJoiner;

/**
 * 融合缓存通知消息。
 * 主要用于集群环境下，通知缓存失效。
 */
public class FusionCacheNotifyMessage {

    /** 缓存名称 */
    private String cacheName;

    /**
     * 通知类型
     */
    private int notifyType;

    /** 缓存key */
    private Object key;

    public FusionCacheNotifyMessage() {
    }

    public FusionCacheNotifyMessage(String cacheName, int notifyType, Object key) {
        this.cacheName = cacheName;
        this.notifyType = notifyType;
        this.key = key;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public int getNotifyType() {
        return notifyType;
    }

    public void setNotifyType(int notifyType) {
        this.notifyType = notifyType;
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return new StringJoiner( ", ", FusionCacheNotifyMessage.class.getSimpleName() + "[", "]" )
                .add( "cacheName='" + cacheName + "'" )
                .add( "notifyType=" + notifyType )
                .add( "key=" + key )
                .toString();
    }
}
