package uw.app.common.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 全局公共状态
 **/
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(title = "全局公共状态", description = "全局公共状态")
public enum CommonState {

    DELETED( -1, "标记删除" ),
    DISABLED( 0, "禁用状态" ),
    ENABLED( 1, "启用状态" );

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
     * 如果匹配不上，最后会返回删除（DELETED）
     *
     * @param value
     * @return
     */
    public static CommonState valueOf(int value) {
        for (CommonState state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return DELETED;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
