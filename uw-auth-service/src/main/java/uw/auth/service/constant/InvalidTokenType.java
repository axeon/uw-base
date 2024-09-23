package uw.auth.service.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 非法token类型。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(title = "非法token类型", description = "非法token类型")
public enum InvalidTokenType {

    /**
     * 无效token。
     */
    INVALID(0, "无效token"),

    /**
     * 用户登出。
     */
    LOGOUT(1, "用户登出"),

    /**
     * 重复登录
     */
    LOGIN_DOUBLE(2, "重复登录"),

    /**
     * 刷新作废
     */
    REFRESH(3, "刷新作废"),

    /**
     * 踢出用户
     */
    KICK_OUT(4, "踢出用户");


    /**
     * 参数值
     */
    private final int value;

    /**
     * 参数信息。
     */
    private final String label;

    InvalidTokenType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 根据value获得enum。
     *
     * @param value
     * @return
     */
    public static InvalidTokenType findByValue(int value) {
        for (InvalidTokenType type : InvalidTokenType.values()) {
            if (value == type.value) {
                return type;
            }
        }
        return INVALID;
    }

    public static void main(String[] args) {
        System.out.println(InvalidTokenType.findByValue(1));
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
