package uw.task.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 队列任务运行类型。
 *
 * <p>对应 {@link uw.task.TaskData#getRunType()}，决定任务在本地执行还是经 RabbitMQ 远程执行：
 * <ul>
 *   <li>{@link #LOCAL}：本地同步执行，不经 MQ；</li>
 *   <li>{@link #GLOBAL}：全局异步执行，投递到 MQ 队列，无返回值；</li>
 *   <li>{@link #GLOBAL_RPC}：全局同步 RPC 执行，经 MQ 发送并等待结果返回；</li>
 *   <li>{@link #AUTO_RPC}：自动判定——本机存在匹配 runner 则本地执行，否则走 GLOBAL_RPC。</li>
 * </ul>
 * 序列化为 OBJECT 形态（含 value/label）。</p>
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum RunnerRunType {


    /**
     * 运行模式：本地运行
     */
    LOCAL(1, "本地运行"),

    /**
     * 运行模式：全局运行
     */
    GLOBAL(3, "全局运行"),

    /**
     * 运行模式：全局运行RPC返回结果
     */
    GLOBAL_RPC(5, "全局运行RPC"),

    /**
     * 运行模式：自动运行RPC返回结果，使用此模式，会自动选择本地还远程运行模式。
     */
    AUTO_RPC(6, "自动运行RPC");


    /**
     * 枚举数值，用于持久化与传输。
     */
    private final int value;

    /**
     * 枚举中文标签，用于展示。
     */
    private final String label;

    RunnerRunType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 按数值查找枚举；匹配不上时返回默认值 {@link #LOCAL}。
     *
     * @param value 数值
     * @return 对应的枚举，无匹配时返回 LOCAL
     */
    public static RunnerRunType findByValue(int value) {
        for (RunnerRunType state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return LOCAL;
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
        for (RunnerRunType state : values()) {
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
