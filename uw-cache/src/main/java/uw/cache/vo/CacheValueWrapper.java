package uw.cache.vo;

import uw.common.util.SystemClock;

/**
 * 缓存数据包装器。
 * <p>
 * 封装业务值与过期时间，配合 {@link uw.cache.FusionCache} / {@link uw.cache.GlobalCache} 使用。
 * 序列化协议由 {@link uw.cache.util.KryoCacheUtils} 定义：先写 8 字节 expiredAt，再写业务值。
 *
 * @param <T> 业务值类型（必须是 Kryo 可序列化的具体类）
 */
public class CacheValueWrapper<T> {

    /**
     * 业务数据。
     */
    private T value;
    /**
     * 过期时间的绝对时间戳（毫秒）。
     * <p>
     * 0：未设置过期（永不过期）。
     * >0：过期时刻 = 构造时 {@code SystemClock.now() + expiredMillis}。
     */
    private long expiredAt;

    /**
     * 默认构造函数，供 Kryo 反序列化使用。
     */
    public CacheValueWrapper() {
    }

    /**
     * 构造函数，同时设置值与相对过期时间。
     *
     * @param value         业务值
     * @param expiredMillis 过期毫秒数，>0 时计算绝对过期时间戳；<=0 表示永不过期
     */
    public CacheValueWrapper(T value, long expiredMillis) {
        this.value = value;
        if (expiredMillis > 0) {
            this.expiredAt = SystemClock.now() + expiredMillis;
        }
    }

    /**
     * 构造函数，仅设置值，永不过期。
     *
     * @param value 业务值
     */
    public CacheValueWrapper(T value) {
        this.value = value;
        this.expiredAt = -1;
    }

    /**
     * 构造函数，仅设置过期时间，值为 null（用于空值保护/失败保护场景）。
     *
     * @param expiredMillis 过期毫秒数，>0 时计算绝对过期时间戳；<=0 表示永不过期
     */
    public CacheValueWrapper(long expiredMillis) {
        if (expiredMillis > 0) {
            this.expiredAt = SystemClock.now() + expiredMillis;
        }
    }

    /**
     * 获取业务数据。
     *
     * @return 业务值，可能为 null
     */
    public T getValue() {
        return value;
    }

    /**
     * 获取过期时间的绝对时间戳。
     *
     * @return 过期时间戳（毫秒），0/-1 表示永不过期
     */
    public long getExpiredAt() {
        return expiredAt;
    }

    /**
     * 设置业务数据（供 Kryo 反序列化使用）。
     *
     * @param value 业务值
     */
    public void setValue(T value) {
        this.value = value;
    }

    /**
     * 设置过期时间的绝对时间戳（供 Kryo 反序列化使用）。
     *
     * @param expiredAt 过期时间戳（毫秒）
     */
    public void setExpiredAt(long expiredAt) {
        this.expiredAt = expiredAt;
    }

    /**
     * 判断当前包装对象是否已过期。
     * <p>
     * 仅当 expiredAt > 0 且当前时间超过 expiredAt 时返回 true。
     *
     * @return true 表示已过期，false 表示未过期或永不过期
     */
    public boolean checkExpired() {
        return expiredAt > 0 && SystemClock.now() > expiredAt;
    }

}
