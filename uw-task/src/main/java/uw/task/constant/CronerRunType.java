package uw.task.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 定时任务运行类型。
 *
 * <p>决定同一个定时任务配置在多实例（多主机）环境下如何被调度执行：
 * <ul>
 *   <li>{@link #ANYWAY}：所有匹配 runTarget 的主机都会执行，不做全局唯一性约束；</li>
 *   <li>{@link #SINGLETON}：全局唯一执行，由 {@link uw.task.util.TaskGlobalLocker} 选出的 Leader
 *       主机独占运行，其余主机直接跳过。</li>
 * </ul>
 * 序列化为 OBJECT 形态（含 value/label），便于前端展示与服务端存储。</p>
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum CronerRunType {

    /**
     * 到处运行：所有匹配 runTarget 的主机都执行，不做全局唯一约束。
     */
    ANYWAY(0, "到处运行"),
    /**
     * 单例运行：仅由全局 Leader 主机执行，其余主机跳过。
     */
    SINGLETON(1, "单例运行");


    /**
     * 枚举数值，用于持久化与传输。
     */
    private final int value;

    /**
     * 枚举中文标签，用于展示。
     */
    private final String label;

    CronerRunType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 按数值查找枚举；匹配不上时返回默认值 {@link #SINGLETON}。
     *
     * @param value 数值
     * @return 对应的枚举，无匹配时返回 SINGLETON
     */
    public static CronerRunType findByValue(int value) {
        for (CronerRunType state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return SINGLETON;
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
        for (CronerRunType state : values()) {
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
