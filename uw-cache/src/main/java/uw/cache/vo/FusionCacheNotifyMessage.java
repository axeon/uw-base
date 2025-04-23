package uw.cache.vo;

/**
 * 融合缓存通知消息。
 * 主要用于集群环境下，通知缓存失效。
 */
public class FusionCacheNotifyMessage {
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

    public FusionCacheNotifyMessage(String cacheName, int notifyType, Object cacheKey) {
        this.cacheName = cacheName;
        this.notifyType = notifyType;
        this.cacheKey = cacheKey;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder( "FusionCacheNotifyMessage{" );
        sb.append( "notifyType=" ).append( notifyType );
        sb.append( ", cacheName='" ).append( cacheName ).append( '\'' );
        sb.append( ", cacheKey=" ).append( cacheKey );
        sb.append( '}' );
        return sb.toString();
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
