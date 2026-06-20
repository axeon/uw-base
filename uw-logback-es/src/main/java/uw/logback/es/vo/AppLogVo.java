package uw.logback.es.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 应用日志视图对象。
 * <p>
 * 描述写入 ES 的单条日志文档结构，字段名通过 {@link JsonProperty} 映射到 ES 文档字段。
 * 注意：当前 appender 实际通过手写 NDJSON 编码（见 ElasticSearchAppender#fillBuffer），
 * 本 VO 主要作为字段约定/契约文档保留。
 *
 * @since 2018-07-28
 */
public class AppLogVo {
    /**
     * 记录时间，ISO 格式，方便 Kibana 按时间分析。
     */
    @JsonProperty("@timestamp")
    private String timestamp;

    /**
     * 应用名称（对应配置 appInfo）。
     */
    @JsonProperty("appInfo")
    private String appInfo;

    /**
     * 应用主机标识（对应配置 appHost）。
     */
    @JsonProperty("appHost")
    private String appHost;

    /**
     * 日志级别（如 INFO/WARN/ERROR）。
     */
    @JsonProperty("level")
    private String level;

    /**
     * logger 名称（通常是类全名）。
     */
    @JsonProperty("logger")
    private String logger;

    /**
     * 产生日志的线程名。
     */
    @JsonProperty("thread")
    private String thread;

    /**
     * 日志消息正文（已格式化的消息）。
     */
    @JsonProperty("message")
    private String message;

    /**
     * 异常堆栈文本（仅在事件携带异常时存在）。
     */
    @JsonProperty("stack_trace")
    private String stackTrace;

    /**
     * @return 记录时间
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp 记录时间
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return 应用名称
     */
    public String getAppInfo() {
        return appInfo;
    }

    /**
     * @param appInfo 应用名称
     */
    public void setAppInfo(String appInfo) {
        this.appInfo = appInfo;
    }

    /**
     * @return 应用主机标识
     */
    public String getAppHost() {
        return appHost;
    }

    /**
     * @param appHost 应用主机标识
     */
    public void setAppHost(String appHost) {
        this.appHost = appHost;
    }

    /**
     * @return 日志级别
     */
    public String getLevel() {
        return level;
    }

    /**
     * @param level 日志级别
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * @return logger 名称
     */
    public String getLogger() {
        return logger;
    }

    /**
     * @param logger logger 名称
     */
    public void setLogger(String logger) {
        this.logger = logger;
    }

    /**
     * @return 线程名
     */
    public String getThread() {
        return thread;
    }

    /**
     * @param thread 线程名
     */
    public void setThread(String thread) {
        this.thread = thread;
    }

    /**
     * @return 日志消息正文
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message 日志消息正文
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return 异常堆栈文本
     */
    public String getStackTrace() {
        return stackTrace;
    }

    /**
     * @param stackTrace 异常堆栈文本
     */
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
}
