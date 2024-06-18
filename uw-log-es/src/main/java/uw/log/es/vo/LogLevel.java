package uw.log.es.vo;

/**
 * 日志级别。
 */
public enum LogLevel {

    /**
     * 什么都不记录
     */
    NONE(-1, "不记录"),

    /**
     * 记录日志
     */
    RECORD(0, "记录"),

    /**
     * 记录日志,含请求参数
     */
    RECORD_REQUEST(1, "记录请求"),

    /**
     * 记录日志,含返回参数
     */
    RECORD_RESPONSE(2, "记录返回结果"),

    /**
     * 记录全部信息。
     */
    RECORD_ALL(3, "记录全部信息");

    /**
     * 数值。
     */
    private final int value;

    /**
     * 描述。
     */
    private final String label;

    LogLevel(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
