package uw.common.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 全局公共状态
 **/
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(title = "全局公共状态", description = "全局公共状态")
public enum StateCommon {

    DELETED( -1, "标记删除" ),
    DISABLED( 0, "禁用状态" ),
    ENABLED( 1, "启用状态" );

    private final int value;

    private final String label;

    StateCommon(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 如果匹配不上，最后会返回删除（DELETED）
     *
     * @param value
     * @return
     */
    public static StateCommon findByValue(int value) {
        for (StateCommon state : values()) {
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
