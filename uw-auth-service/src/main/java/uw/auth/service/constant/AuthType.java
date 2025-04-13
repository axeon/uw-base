package uw.auth.service.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 授权类型
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(title = "授权类型", description = "授权类型")
public enum AuthType {

    /**
     * 不验证鉴权。
     */
    NONE(0, "不验证"),

    /**
     * 临时授权
     */
    TEMP(1, "临时授权"),

    /**
     * 仅验证用户类型
     */
    USER(2, "仅验证用户类型"),

    /**
     * 验证用户类型和权限
     */
    PERM(3, "验证用户类型和权限"),

    /**
     * 超级用户权限
     */
    SUDO(6, "超级用户权限");

    /**
     * 参数值
     */
    private final int value;

    /**
     * 参数信息。
     */
    private final String label;

    AuthType(int value, String label) {
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
