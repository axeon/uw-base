package uw.task.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 任务日志记录类型。
 *
 * <p>对应 {@code TaskRunnerConfig.logLevel}，控制每次任务执行后写入日志的详细程度：
 * 从 {@link #NONE}（不记录）到 {@link #RECORD_ALL}（记录入参与返回）逐级递增。
 * 序列化为 OBJECT 形态（含 value/label）。</p>
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TaskLogRecordType {


    /**
     * 什么都不记录
     */
    NONE(-1, "不记录日志"),

    /**
     * 记录日志
     */
    RECORD(0, "记录日志"),

    /**
     * 记录日志,含请求参数
     */
    RECORD_TASK_PARAM(1, "记录日志,含请求参数"),

    /**
     * 记录日志,含返回参数
     */
    RECORD_RESULT_DATA(2, "记录日志,含返回参数"),

    /**
     * 记录全部日志
     */
    RECORD_ALL(3, "记录全部日志");

    /**
     * 枚举数值，用于持久化与传输。
     */
    private final int value;

    /**
     * 枚举中文标签，用于展示。
     */
    private final String label;

    TaskLogRecordType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 按数值查找枚举；匹配不上时返回默认值 {@link #NONE}。
     *
     * @param value 数值
     * @return 对应的枚举，无匹配时返回 NONE
     */
    public static TaskLogRecordType findByValue(int value) {
        for (TaskLogRecordType state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return NONE;
    }

    /**
     * @return 枚举数值
     */
    public int getValue() {
        return value;
    }

    /**
     * 判断给定数值是否是有效的枚举值。
     *
     * @param name 待校验的数值
     * @return 有效返回 true，否则 false
     */
    public static boolean isEffective(int name) {
        for (TaskLogRecordType state : values()) {
            if (state.value == name) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return 枚举中文标签
     */
    public String getLabel() {
        return label;
    }
}
