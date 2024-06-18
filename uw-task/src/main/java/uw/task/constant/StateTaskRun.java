package uw.task.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 任务运行状态。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum StateTaskRun {


    /**
     * 任务状态:未设置
     */
    UNKNOWN(0, "未设置"),

    /**
     * 任务状态:成功
     */
    SUCCESS(1, "成功"),

    /**
     * 任务状态:程序错误
     */
    FAIL_PROGRAM(2, "程序错误"),

    /**
     * 任务状态:配置错误，如超过流量限制
     */
    FAIL_CONFIG(3, "配置错误"),

    /**
     * 任务状态:第三方接口错误
     */
    FAIL_PARTNER(4, "三方接口错误"),

    /**
     * 任务状态:数据错误
     */
    FAIL_DATA(5, "数据错误");


    private final int value;

    private final String label;

    StateTaskRun(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 如果匹配不上，最后会返回为止（UNKNOWN）
     *
     * @param value
     * @return
     */
    public static StateTaskRun findByValue(int value) {
        for (StateTaskRun state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return UNKNOWN;
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
        for (StateTaskRun state : values()) {
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
