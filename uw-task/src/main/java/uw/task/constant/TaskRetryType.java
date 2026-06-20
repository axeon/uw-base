package uw.task.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 任务重试类型。
 *
 * <p>对应 {@link uw.task.TaskData#getRetryType()}，决定任务失败后是否由框架自动重试：
 * <ul>
 *   <li>{@link #AUTO}：自动重试（默认），失败后按 retryTimes 配置由框架自动重新派发；</li>
 *   <li>{@link #MANUAL}：仅人工重试，框架不做自动重试。</li>
 * </ul>
 * 序列化为 OBJECT 形态（含 value/label）。</p>
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

    /**
     * 枚举数值，用于持久化与传输。
     */
    private final int value;

    /**
     * 枚举中文标签，用于展示。
     */
    private final String label;

    TaskRetryType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 按数值查找枚举；匹配不上时返回默认值 {@link #AUTO}。
     *
     * @param value 数值
     * @return 对应的枚举，无匹配时返回 AUTO
     */
    public static TaskRetryType findByValue(int value) {
        for (TaskRetryType state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return AUTO;
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
        for (TaskRetryType state : values()) {
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
