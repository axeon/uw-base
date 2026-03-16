package uw.cache.vo;

import uw.common.util.SystemClock;

/**
 * 缓存数据包装器。
 * 主要是封装过期时间。
 */
public class CacheValueWrapper<T> {

    /**
     * 数据。
     */
    public T value;
    /**
     * 过期时间的毫秒数。
     */
    private long expiredAt;

    public CacheValueWrapper() {
    }

    /**
     * 构造函数。
     *
     * @param value
     * @param expiredMillis
     */
    public CacheValueWrapper(T value, long expiredMillis) {
        this.value = value;
        if (expiredMillis > 0) {
            this.expiredAt = SystemClock.now() + expiredMillis;
        }
    }

    /**
     * 构造函数。
     *
     * @param value
     */
    public CacheValueWrapper(T value) {
        this.value = value;
        this.expiredAt = -1;
    }

    /**
     * 构造函数。
     *
     * @param expiredMillis
     */
    public CacheValueWrapper(long expiredMillis) {
        if (expiredMillis > 0) {
            this.expiredAt = SystemClock.now() + expiredMillis;
        }
    }

    /**
     * 获取数据。
     *
     * @return
     */
    public T getValue() {
        return value;
    }

    /**
     * 获取过期时间。
     *
     * @return
     */
    public long getExpiredAt() {
        return expiredAt;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public void setExpiredAt(long expiredAt) {
        this.expiredAt = expiredAt;
    }

    /**
     * 是否已过期。
     *
     * @return
     */
    public boolean checkExpired() {
        return expiredAt > 0 && SystemClock.now() > expiredAt;
    }

}
