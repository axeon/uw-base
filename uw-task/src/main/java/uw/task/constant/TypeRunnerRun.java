package uw.task.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 队列任务运行类型。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TypeRunnerRun {


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


    private final int value;

    private final String label;

    TypeRunnerRun(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 如果匹配不上，最后会返回为止（UNKNOWN）
     *
     * @param value
     * @return
     */
    public static TypeRunnerRun findByValue(int value) {
        for (TypeRunnerRun state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return LOCAL;
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
        for (TypeRunnerRun state : values()) {
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
