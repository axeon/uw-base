package uw.logback.es.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 应用日志Vo
 *
 *
 * @since 2018-07-28
 */
public class AppLogVo {
    /**
     * 记录时间,方便kibana分析
     */
    @JsonProperty("@timestamp")
    private String timestamp;

    /**
     * 应用信息
     */
    @JsonProperty("appInfo")
    private String appInfo;

    /**
     * 应用主机
     */
    @JsonProperty("appHost")
    private String appHost;

    /**
     * 记录日志的级别。
     */
    @JsonProperty("level")
    private String level;

    /**
     * logger。
     */
    @JsonProperty("logger")
    private String logger;

    /**
     * 输出日志的线程。
     */
    @JsonProperty("thread")
    private String thread;

    /**
     * 日志消息。
     */
    @JsonProperty("message")
    private String message;

    /**
     * 输出日志的线程。
     */
    @JsonProperty("stack_trace")
    private String stackTrace;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getAppInfo() {
        return appInfo;
    }

    public void setAppInfo(String appInfo) {
        this.appInfo = appInfo;
    }

    public String getAppHost() {
        return appHost;
    }

    public void setAppHost(String appHost) {
        this.appHost = appHost;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
}
