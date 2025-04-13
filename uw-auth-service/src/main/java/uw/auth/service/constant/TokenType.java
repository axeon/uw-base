package uw.auth.service.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Token类型
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(title = "Token类型", description = "Token类型")
public enum TokenType {


    NONE(-1, "无Token"),
    /**
     * 临时Token。
     */
    TEMP(0, "临时Token"),

    /**
     * 正常Token。
     */
    COMMON(1, "标准Token"),

    /**
     * 超级Token。
     */
    SUDO(6, "超级Token");

    /**
     * 参数值
     */
    private final int value;

    /**
     * 参数信息。
     */
    private final String label;

    TokenType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
