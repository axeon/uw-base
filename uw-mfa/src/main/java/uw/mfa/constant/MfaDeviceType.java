package uw.mfa.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * MFA设备类型枚举。
 * <p>定义设备验证码的投递渠道与TOTP验证方式，值用于Redis Key前缀与业务路由。</p>
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(title = "mfa设备类型", description = "mfa设备类型")
public enum MfaDeviceType {

    /**
     * TOTP恢复码登录（一次性恢复码，用于TOTP密钥丢失时登录）。
     */
    TOTP_RECOVERY_CODE(20, "TOTP恢复码登录"),

    /**
     * TOTP验证码登录（基于时间的一次性密码，由Authenticator App生成）。
     */
    TOTP_CODE(21, "TOTP验证码登录"),

    /**
     * Email验证码登录（验证码通过邮件投递）。
     */
    EMAIL_CODE(22, "Email验证码登录"),

    /**
     * 手机验证码登录（验证码通过短信投递）。
     */
    MOBILE_CODE(23, "手机验证码登录");

    /**
     * 类型参数值，用于Redis Key前缀区分与业务路由。
     */
    private final int value;

    /**
     * 类型展示名称。
     */
    private final String label;

    MfaDeviceType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 检查类型值是否合法。
     *
     * @param value 类型值
     * @return 合法返回true，否则false
     */
    public static boolean checkTypeValid(int value) {
        return valueOf(value) != null;
    }

    /**
     * 获取所有设备类型集合。
     *
     * @return 不可变类型集合
     */
    public static Set<MfaDeviceType> types() {
        return Set.of(MfaDeviceType.values());
    }


    /**
     * 获取TOTP验证码登录类型集合（仅TOTP_CODE）。
     *
     * @return 不可变类型集合
     */
    public static Set<MfaDeviceType> totpCodeTypes() {
        return Set.of(TOTP_CODE);
    }

    /**
     * 获取TOTP登录类型集合（包含TOTP_CODE与TOTP_RECOVERY_CODE）。
     *
     * @return 不可变类型集合
     */
    public static Set<MfaDeviceType> totpTypes() {
        return Set.of(TOTP_CODE, TOTP_RECOVERY_CODE);
    }


    /**
     * 根据值获取类型。
     *
     * @param value 类型值
     * @return 匹配的类型，无匹配返回null
     */
    public static MfaDeviceType valueOf(int value) {
        for (MfaDeviceType type : MfaDeviceType.values()) {
            if (value == type.value) {
                return type;
            }
        }
        return null;
    }


    /**
     * 获取类型展示名称。
     *
     * @return 展示名称
     */
    public String getLabel() {
        return label;
    }

    /**
     * 获取类型参数值。
     *
     * @return 类型值
     */
    public int getValue() {
        return value;
    }
}
