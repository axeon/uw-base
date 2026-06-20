package uw.auth.service.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Token 类型枚举。
 * <p>
 * 表示 Token 的安全等级：{@link #TEMP} 临时 Token、{@link #COMMON} 标准 Token、
 * {@link #SUDO} 超级 Token。鉴权时按 {@code tokenType >= 阈值} 的方式判定，
 * 因此值越大权限越强。
 *
 * @author axeon
 * @see uw.auth.service.service.MscAuthPermService#hasPerm
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

    /**
     * @return Token 类型数值
     */
    public int getValue() {
        return value;
    }

    /**
     * @return Token 类型显示名称
     */
    public String getLabel() {
        return label;
    }
}
