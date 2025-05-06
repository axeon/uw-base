package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 日志基类Vo
 *
 * 
 * @since 2018-07-28
 */
public abstract class LogBaseVo {

    /**
     * 记录时间,方便kibana分析
     */
    @JsonProperty("@timestamp")
    @Schema(title = "记录时间", description = "记录时间")
    protected String timestamp;

    /**
     * 应用信息
     */
    @Schema(title = "应用信息", description = "应用信息")
    protected String appInfo;

    /**
     * 应用主机
     */
    @Schema(title = "应用主机", description = "应用主机")
    protected String appHost;

    /**
     * 记录日志的级别。
     */
    @Schema(title = "记录日志的级别", description = "记录日志的级别")
    protected int logLevel;

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

    public int getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }
}
