package uw.cache.constant;

/**
 * 缓存通知类型
 */
public enum CacheNotifyType {

    /**
     * 作废。
     */
    INVALIDATE(0, "作废"),

    /**
     * 刷新。
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

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static CacheNotifyType findByValue(int value) {
        for (CacheNotifyType e : CacheNotifyType.values()) {
            if (value == e.value) {
                return e;
            }
        }
        return null;
    }
}
