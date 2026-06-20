package uw.common.app.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 全局公共状态枚举。
 * <p>
 * 统一约定实体状态值：删除(-1) / 禁用(0) / 启用(1)。
 * 序列化为 JSON 时以对象形态输出（包含 value 与 label 字段）。
 * </p>
 * 使用示例：
 * <pre>{@code
 * entity.setState(CommonState.ENABLED.getValue());
 * if (user.getState() == CommonState.DISABLED.getValue()) { ... }
 * }</pre>
 **/
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(title = "全局公共状态", description = "全局公共状态")
public enum CommonState {

    /** 标记删除（软删除），不可见且不参与业务。 */
    DELETED(-1, "标记删除"),
    /** 禁用状态，数据保留但功能不可用。 */
    DISABLED(0, "禁用状态"),
    /** 启用状态，正常可用。 */
    ENABLED(1, "启用状态");

    /**
     * 状态值
     */
    private final int value;

    /**
     * 状态描述
     */
    private final String label;

    CommonState(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 根据状态值解析枚举。
     * <p>
     * 若传入值匹配不上任何枚举常量，将返回 {@link #DELETED}（保守策略，未知状态视为不可见）。
     * </p>
     *
     * @param value 状态值
     * @return 匹配的枚举常量；匹配不上时返回 {@link #DELETED}
     */
    public static CommonState valueOf(int value) {
        for (CommonState state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return DELETED;
    }

    /**
     * 获取状态值。
     *
     * @return 状态值（-1/0/1）
     */
    public int getValue() {
        return value;
    }

    /**
     * 获取状态描述。
     *
     * @return 状态描述文本
     */
    public String getLabel() {
        return label;
    }
}
