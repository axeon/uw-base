package uw.task.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 队列任务限速类型。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TaskRateLimitType {

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
     * 限速类型：全局主机HOST限速。
     * <p>限流 key 不含 taskClass，<b>跨任务共享</b>：同一主机上所有配置此类型的任务共享配额池。</p>
     */
    GLOBAL_HOST(4, "全局主机HOST限速"),

    /**
     * 限速类型：全局TAG限速。
     * <p>限流 key 不含 taskClass，<b>跨任务共享</b>：rateLimitTag 相同的不同任务共享配额池。
     * 适用于多个任务对接同一第三方接口、需共享 QPS 上限的场景。</p>
     */
    GLOBAL_TAG(5, "全局TAG限速"),

    /**
     * 限速类型：全局TASK限速。
     * <p>限流 key 含 taskClass，<b>按任务隔离</b>：每个任务独享配额池。</p>
     */
    GLOBAL_TASK(6, "全局TASK限速"),

    /**
     * 限速类型：全局TAG+HOST限速。
     * <p>限流 key 不含 taskClass，<b>跨任务共享</b>：rateLimitTag 与主机共同确定配额池。</p>
     */
    GLOBAL_TAG_HOST(7, "全局TAG+HOST限速"),

    /**
     * 限速类型：全局TASK+HOST限速
     */
    GLOBAL_TASK_HOST(8, "全局TASK+HOST限速"),

    /**
     * 限速类型：全局TASK+TAG限速
     */
    GLOBAL_TASK_TAG(9, "全局TASK+TAG限速"),

    /**
     * 限速类型：全局TASK+TAG+HOST限速
     */
    GLOBAL_TASK_TAG_HOST(10, "全局TASK+TAG+HOST限速");

    /**
     * 枚举数值，用于持久化与传输。
     */
    private final int value;

    /**
     * 枚举中文标签，用于展示。
     */
    private final String label;

    /**

     * 构建枚举项。

     *

     * @param value 枚举数值

     * @param label 枚举中文标签

     */

    TaskRateLimitType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 按数值查找枚举；匹配不上时返回默认值 {@link #NONE}。
     *
     * @param value 数值
     * @return 对应的枚举，无匹配时返回 NONE
     */
    public static TaskRateLimitType findByValue(int value) {
        for (TaskRateLimitType state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return NONE;
    }

    /**
     * 返回枚举数值。
     *
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
        for (TaskRateLimitType state : values()) {
            if (state.value == name) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回枚举中文标签。
     *
     * @return 枚举中文标签
     */
    public String getLabel() {
        return label;
    }
}
