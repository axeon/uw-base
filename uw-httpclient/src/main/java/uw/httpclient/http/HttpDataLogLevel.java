package uw.httpclient.http;

/**
 * HttpData日志级别。
 * 因为httpData必须返回response，所以只有RECORD_REQUEST有效。
 */
public enum HttpDataLogLevel {

    /**
     * 记录日志,含请求参数
     */
    RECORD_REQUEST( 1, "记录请求" ),

    /**
     * 默认记录输出。
     */
    RECORD_RESPONSE( 2, "记录输出" ),

    /**
     * 记录全部信息。
     */
    RECORD_ALL( 3, "记录全部信息" );

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
     * 是否记录请求信息。
     * @param logLevel
     * @return
     */
    public static boolean isRecordRequest(HttpDataLogLevel logLevel){
        if (logLevel==RECORD_REQUEST||logLevel==RECORD_ALL){
            return true;
        }else{
            return false;
        }
    }
}
