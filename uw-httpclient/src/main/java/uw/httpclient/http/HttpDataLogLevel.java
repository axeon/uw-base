package uw.httpclient.http;

/**
 * HttpData日志级别。
 * <p>
 * 注意：响应数据始终会被记录到HttpData中，本级别仅控制是否额外记录请求体数据。
 * - RECORD_RESPONSE：默认级别，仅记录响应，不记录请求体。
 * - RECORD_REQUEST：记录响应，并额外记录请求体。
 * - RECORD_ALL：等同于RECORD_REQUEST，记录响应与请求体。
 */
public enum HttpDataLogLevel {

    /**
     * 记录响应，并额外记录请求体。
     */
    RECORD_REQUEST(1, "记录请求"),

    /**
     * 默认记录输出，仅记录响应。
     */
    RECORD_RESPONSE(2, "记录输出"),

    /**
     * 记录全部信息（响应与请求体）。
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

    HttpDataLogLevel(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 是否记录请求体信息。
     *
     * @param logLevel 日志级别。
     * @return 为 RECORD_REQUEST 或 RECORD_ALL 时返回 true，否则 false。
     */
    public static boolean isRecordRequest(HttpDataLogLevel logLevel) {
        return logLevel == RECORD_REQUEST || logLevel == RECORD_ALL;
    }
}
