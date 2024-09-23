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
     * 什么都不记录
     */
    NONE(-1, "不记录"),

    /**
     * 记录日志
     */
    BASE(0, "记录基本信息"),

    /**
     * 记录日志,含请求参数
     */
    REQUEST(1, "记录请求信息"),

    /**
     * 记录日志,含返回参数
     */
    RESPONSE(2, "记录返回结果"),

    /**
     * 记录全部信息。
     */
    ALL(3, "记录全部信息"),

    /**
     * 记录重要数据,同时记录请求响应数据到ES且写入数据库
     */
    CRIT(9, "记录全部数据,同时记录请求响应数据到ES且写入数据库");

    /**
     * 参数值
     */
    private final int value;

    /**
     * 参数信息。
     */
    private final String label;

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
