package uw.task.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 任务日志记录类型。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TaskLogRecordType {


    /**
     * 什么都不记录
     */
    NONE(-1,"不记录日志"),

    /**
     * 记录日志
     */
    RECORD(0,"记录日志"),

    /**
     * 记录日志,含请求参数
     */
    RECORD_TASK_PARAM(1,"记录日志,含请求参数"),

    /**
     * 记录日志,含返回参数
     */
    RECORD_RESULT_DATA(2,"记录日志,含返回参数"),

    /**
     * 记录全部日志
     */
    RECORD_ALL(3,"记录全部日志");

    private final int value;

    private final String label;

    TaskLogRecordType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 如果匹配不上，最后会返回NONE。
     *
     * @param value
     * @return
     */
    public static TaskLogRecordType findByValue(int value) {
        for (TaskLogRecordType state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return NONE;
    }

    public int getValue() {
        return value;
    }

    /**
     * 返回改状态码是否有效
     *
     * @param name
     * @return
     */
    public static boolean isEffective(int name) {
        for (TaskLogRecordType state : values()) {
            if (state.value == name) {
                return true;
            }
        }
        return false;
    }

    public String getLabel() {
        return label;
    }
}
