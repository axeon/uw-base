package uw.auth.service.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 非法 Token 类型枚举。
 * <p>
 * 标识 Token 失效的成因，由 auth-center 下发，{@code AuthServiceHelper.invalidToken}
 * 会据此将 Token 从本地缓存清除并加入黑名单。
 *
 * @author axeon
 * @see uw.auth.service.AuthServiceHelper#invalidToken
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(title = "非法token类型", description = "非法token类型")
public enum TokenInvalidType {

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

    TokenInvalidType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 根据value获取enum。
     *
     * @param value 非法类型数值
     * @return 匹配的 TokenInvalidType，未匹配时返回 {@link #INVALID}
     */
    public static TokenInvalidType findByValue(int value) {
        for (TokenInvalidType type : TokenInvalidType.values()) {
            if (value == type.value) {
                return type;
            }
        }
        return INVALID;
    }

    /**
     * @return 非法类型数值
     */
    public int getValue() {
        return value;
    }

    /**
     * @return 非法类型显示名称
     */
    public String getLabel() {
        return label;
    }
}
