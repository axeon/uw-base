package uw.auth.service.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 授权类型枚举。
 * <p>
 * 决定 {@code @MscPermDeclare.auth()} 对应的鉴权严格程度：从 {@link #NONE} 不校验，
 * 到 {@link #USER} 仅校验用户类型，再到 {@link #PERM}/{@link #SUDO} 需校验权限集合。
 *
 * @author axeon
 * @see uw.auth.service.service.MscAuthPermService#hasPerm
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

    /**
     * @return 授权类型数值
     */
    public int getValue() {
        return value;
    }

    /**
     * @return 授权类型显示名称
     */
    public String getLabel() {
        return label;
    }
}
