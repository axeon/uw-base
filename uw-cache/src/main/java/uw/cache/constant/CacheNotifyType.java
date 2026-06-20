package uw.cache.constant;

/**
 * 缓存通知类型。
 * <p>
 * 定义 FusionCache 在集群间通过 Redis Pub/Sub 传播的缓存操作类型，
 * 接收方 {@link uw.cache.listener.FusionCacheNotifyListener} 据此分发处理。
 */
public enum CacheNotifyType {

    /**
     * 作废（失效）：通知接收方从本地缓存删除指定 key。
     */
    INVALIDATE(0, "作废"),

    /**
     * 刷新：通知接收方从本地缓存删除并立即重新加载指定 key。
     */
    REFRESH(1, "刷新");

    /**
     * 参数值
     */
    private final int value;

    /**
     * 参数信息。
     */
    private final String label;

    CacheNotifyType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 获取通知类型的数值。
     *
     * @return 通知类型数值
     */
    public int getValue() {
        return value;
    }

    /**
     * 获取通知类型的中文标签。
     *
     * @return 中文标签
     */
    public String getLabel() {
        return label;
    }

    /**
     * 根据数值查找对应的枚举。
     *
     * @param value 通知类型数值
     * @return 匹配的枚举，无匹配时返回 null
     */
    public static CacheNotifyType findByValue(int value) {
        for (CacheNotifyType e : CacheNotifyType.values()) {
            if (value == e.value) {
                return e;
            }
        }
        return null;
    }
}
