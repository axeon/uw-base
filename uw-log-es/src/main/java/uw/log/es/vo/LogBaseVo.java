package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 日志基类Vo。
 * <p>所有写入 Elasticsearch 的日志对象应继承此类，公共字段由写入链路自动填充：
 * {@code timestamp} 在写入时补齐、{@code appInfo}/{@code appHost} 在启用 appInfoOverwrite 时覆写。
 *
 * @since 2018-07-28
 */
public abstract class LogBaseVo {

    /**
     * 记录时间（ISO8601），映射为 ES 的 {@code @timestamp} 字段，方便 kibana 分析。
     */
    @JsonProperty("@timestamp")
    @Schema(title = "记录时间", description = "记录时间")
    protected String timestamp;

    /**
     * 应用信息（应用名:版本）
     */
    @Schema(title = "应用信息", description = "应用信息")
    protected String appInfo;

    /**
     * 应用主机（ip:port）
     */
    @Schema(title = "应用主机", description = "应用主机")
    protected String appHost;

    /**
     * 记录日志的级别，参见 {@link LogLevel}。{@code <=NONE} 时单条 log() 不写入。
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
