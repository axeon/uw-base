package uw.task.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 队列任务延迟类型。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TypeRunnerDelay {


    /**
     * 非延迟任务类型。
     */
    OFF(0,"非延迟任务类型"),

    /**
     * 延迟任务类型。
     */
    ON(1,"延迟任务类型");

    private final int value;

    private final String label;

    TypeRunnerDelay(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 如果匹配不上，最后会返回OFF。
     *
     * @param value
     * @return
     */
    public static TypeRunnerDelay findByValue(int value) {
        for (TypeRunnerDelay state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return OFF;
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
        for (TypeRunnerDelay state : values()) {
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
