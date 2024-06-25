package uw.cache.vo;

/**
 * 失败保护数值。
 * 原本可以在expiry中设置null类型的保护时间，caffeine开expiry检测后性能下降200倍。
 * 遂使用特定FailProtectValue设置expired方式来规避此性能问题。
 */
public class CacheProtectedValue {

    /**
     * 过期时间。
     */
    private long expiredMillis;

    public CacheProtectedValue() {
    }

    public CacheProtectedValue(long ttl) {
        expiredMillis = System.currentTimeMillis() + ttl;
    }

    /**
     * 是否已过期。
     *
     * @return
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiredMillis;
    }
}
