package uw.task.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 队列任务限速类型。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TypeRunnerRateLimit {

    /**
     * 限速类型：不限速
     */
    NONE(0, "不限速"),

    /**
     * 限速类型：本地进程限速
     */
    LOCAL(1, "本地进程限速"),

    /**
     * 限速类型：本地TASK限速
     */
    LOCAL_TASK(2, "本地TASK限速"),

    /**
     * 限速类型：本地TASK+TAG限速
     */
    LOCAL_TASK_TAG(3, "本地TASK+TAG限速"),

    /**
     * 限速类型：全局主机HOST限速
     */
    GLOBAL_HOST(4, "全局主机HOST限速"),

    /**
     * 限速类型：全局TAG限速
     */
    GLOBAL_TAG(5, "全局TAG限速"),

    /**
     * 限速类型：全局TASK限速
     */
    GLOBAL_TASK(6, "全局TASK限速"),

    /**
     * 限速类型：全局TAG+HOST限速
     */
    GLOBAL_TAG_HOST(7, "全局TAG+HOST限速"),

    /**
     * 限速类型：全局TASK+IP限速
     */
    GLOBAL_TASK_HOST(8, "全局TASK+IP限速"),

    /**
     * 限速类型：全局TASK+TAG限速
     */
    GLOBAL_TASK_TAG(9, "全局TASK+TAG限速"),

    /**
     * 限速类型：全局TASK+TAG+IP限速
     */
    GLOBAL_TASK_TAG_HOST(10, "全局TASK+TAG+IP限速");


    private final int value;

    private final String label;

    TypeRunnerRateLimit(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 如果匹配不上，最后会返回NONE。
     *
     * @param value
     * @return
     */
    public static TypeRunnerRateLimit findByValue(int value) {
        for (TypeRunnerRateLimit state : values()) {
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
        for (TypeRunnerRateLimit state : values()) {
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
