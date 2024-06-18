package uw.task.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 定时任务运行类型。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TypeCronerRun {

    /**
     * 直接运行模式。
     */
    ANYWAY(0,"到处运行"),
    /**
     * 运行在全局单例模式下。
     */
    SINGLETON(1,"单例运行");



    private final int value;

    private final String label;

    TypeCronerRun(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 如果匹配不上，最后会返回为止（UNKNOWN）
     *
     * @param value
     * @return
     */
    public static TypeCronerRun findByValue(int value) {
        for (TypeCronerRun state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return SINGLETON;
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
        for (TypeCronerRun state : values()) {
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
