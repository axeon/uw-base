package uw.task.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 队列任务队列类型。
 *
 * <p>决定任务投递到的目标队列粒度，影响队列名生成（见 {@code TaskMetaInfoManager.getQueueNameByConfig}）：
 * <ul>
 *   <li>{@link #PROJECT} / {@link #PROJECT_PRIORITY}：项目级共享队列，同一项目所有任务共用；</li>
 *   <li>{@link #GROUP} / {@link #GROUP_PRIORITY}：任务组级队列，按 taskClass 所在包隔离；</li>
 *   <li>{@link #TASK}：任务级队列，每个 taskClass+taskTag 独立队列。</li>
 * </ul>
 * 带 PRIORITY 的类型使用优先级队列配置。序列化为 OBJECT 形态（含 value/label）。</p>
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum RunnerQueueType {

    /**
     * 项目队列。
     */
    PROJECT(0, "项目队列"),

    /**
     * 项目优先级队列。
     */
    PROJECT_PRIORITY(1, "项目优先级队列"),

    /**
     * 任务组队列。
     */
    GROUP(2, "任务组队列"),

    /**
     * 任务组优先级队列。
     */
    GROUP_PRIORITY(3, "任务组优先级队列"),

    /**
     * 任务队列。
     */
    TASK(5, "任务队列");

    /**
     * 枚举数值，用于持久化与传输。
     */
    private final int value;

    /**
     * 枚举中文标签，用于展示。
     */
    private final String label;

    RunnerQueueType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 按数值查找枚举；匹配不上时返回默认值 {@link #PROJECT}。
     *
     * @param value 数值
     * @return 对应的枚举，无匹配时返回 PROJECT
     */
    public static RunnerQueueType findByValue(int value) {
        for (RunnerQueueType state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return PROJECT;
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
        for (RunnerQueueType state : values()) {
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
