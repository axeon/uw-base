package uw.task.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 队列任务队列类型。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum RunnerQueueType {

    /**
     * 项目队列。
     */
    PROJECT(0,"项目队列"),

    /**
     * 项目优先级队列。
     */
    PROJECT_PRIORITY(1,"项目优先级队列"),

    /**
     * 任务组队列。
     */
    GROUP(2,"任务组队列"),

    /**
     * 任务组优先级队列。
     */
    GROUP_PRIORITY(3,"任务组优先级队列"),

    /**
     * 任务队列。
     */
    TASK(5,"任务队列");

    private final int value;

    private final String label;

    RunnerQueueType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 如果匹配不上，最后会返回NONE。
     *
     * @param value
     * @return
     */
    public static RunnerQueueType findByValue(int value) {
        for (RunnerQueueType state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return PROJECT;
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
        for (RunnerQueueType state : values()) {
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
