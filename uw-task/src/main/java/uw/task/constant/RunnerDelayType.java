package uw.task.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 队列任务延迟类型。
 *
 * <p>决定队列任务是否启用延迟投递：
 * <ul>
 *   <li>{@link #OFF}：不启用延迟队列，任务直接进入业务队列；</li>
 *   <li>{@link #ON}：启用延迟队列（基于 RabbitMQ 死信队列实现），
 *       任务先进入 TTL 队列，到期后转发到业务队列。注意：死信队列实现下，
 *       长延时任务可能阻塞短延时任务（见 README 说明）。</li>
 * </ul>
 * 序列化为 OBJECT 形态（含 value/label）。</p>
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum RunnerDelayType {


    /**
     * 非延迟任务类型。
     */
    OFF(0, "非延迟任务类型"),

    /**
     * 延迟任务类型。
     */
    ON(1, "延迟任务类型");

    /**
     * 枚举数值，用于持久化与传输。
     */
    private final int value;

    /**
     * 枚举中文标签，用于展示。
     */
    private final String label;

    RunnerDelayType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 按数值查找枚举；匹配不上时返回默认值 {@link #OFF}。
     *
     * @param value 数值
     * @return 对应的枚举，无匹配时返回 OFF
     */
    public static RunnerDelayType findByValue(int value) {
        for (RunnerDelayType state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return OFF;
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
        for (RunnerDelayType state : values()) {
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
