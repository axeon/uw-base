package uw.task.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 任务运行状态。
 *
 * <p>对应 {@link uw.task.TaskData#getState()} 与日志实体的状态字段。框架根据执行结果（成功 / 各类异常）
 * 自动设置：{@link #SUCCESS} 表示成功；{@code FAIL_*} 系列表示不同类型的失败，其中
 * {@link #FAIL_PARTNER} 与 {@link #FAIL_CONFIG} 可按配置触发重试，{@link #FAIL_PROGRAM} 与
 * {@link #FAIL_DATA} 不重试。序列化为 OBJECT 形态（含 value/label）便于前端展示。</p>
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum RunStateTask {


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


    /**
     * 枚举数值，用于持久化与传输。
     */
    private final int value;

    /**
     * 枚举中文标签，用于展示。
     */
    private final String label;

    RunStateTask(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 按数值查找枚举；匹配不上时返回默认值 {@link #UNKNOWN}。
     *
     * @param value 数值
     * @return 对应的枚举，无匹配时返回 UNKNOWN
     */
    public static RunStateTask findByValue(int value) {
        for (RunStateTask state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return UNKNOWN;
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
        for (RunStateTask state : values()) {
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
