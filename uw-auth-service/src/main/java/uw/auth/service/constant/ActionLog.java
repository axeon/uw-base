package uw.auth.service.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 应用Http接口操作日志记录类型
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(title = "操作日志类型", description = "操作日志类型")
public enum ActionLog {
    /**
     * 不记录
     */
    NONE(0, "不记录"),

    /**
     * 记录请求参数
     */
    REQUEST(1, "记录请求数据"),

    /**
     * 记录响应参数,当数据量巨大时会有性能问题,不建议记录
     */
    RESPONSE(2, "记录响应数据"),

    /**
     * 记录请求参数和响应参数
     */
    ALL(3, "记录请求响应数据"),

    /**
     * 记录重要数据,同时记录请求响应数据到ES且写入数据库
     */
    CRIT(4, "记录重要数据,同时记录请求响应数据到ES且写入数据库");

    /**
     * 参数值
     */
    private int value;

    /**
     * 参数信息。
     */
    private String label;

    ActionLog(int value, String label) {
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
