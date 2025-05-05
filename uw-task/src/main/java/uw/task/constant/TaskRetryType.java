package uw.task.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 任务重试类型。
 *
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TaskRetryType {

    /**
     * 自动重试[为了兼容,默认开启重试]
     */
    AUTO(0, "自动重试"),
    /**
     * 用户手工重试
     */
    MANUAL(1, "手工重试");

    private final int value;

    private final String label;

    TaskRetryType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 如果匹配不上,返回自动。
     *
     * @param value
     * @return
     */
    public static TaskRetryType findByValue(int value) {
        for (TaskRetryType state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return AUTO;
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
        for (TaskRetryType state : values()) {
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
