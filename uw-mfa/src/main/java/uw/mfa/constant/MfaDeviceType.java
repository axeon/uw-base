package uw.mfa.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * mfa设备类型
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(title = "mfa设备类型", description = "mfa设备类型")
public enum MfaDeviceType {

    /**
     * TOTP恢复码登录
     */
    TOTP_RECOVERY_CODE( 20, "TOTP恢复码登录" ),

    /**
     * TOTP验证码登录
     */
    TOTP_CODE( 21, "TOTP验证码登录" ),

    /**
     * Email验证码登录
     */
    EMAIL_CODE( 22, "Email验证码登录" ),

    /**
     * 手机验证码登录
     */
    MOBILE_CODE( 23, "手机验证码登录" );

    /**
     * 参数值
     */
    private final int value;

    /**
     * 参数信息。
     */
    private final String label;

    MfaDeviceType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 检查类型值是否合法。
     *
     * @param value
     * @return
     */
    public static boolean checkTypeValid(int value) {
        return valueOf( value ) != null;
    }

    /**
     * 获取所有类型。
     *
     * @return
     */
    public static Set<MfaDeviceType> types() {
        return Set.of(MfaDeviceType.values());
    }


    /**
     * 获取TOTP验证码登录类型。
     * @return
     */
    public static Set<MfaDeviceType> totpCodeTypes() {
        return Set.of(TOTP_CODE);
    }

    /**
     * 获取TOTP登录类型。
     * @return
     */
    public static Set<MfaDeviceType> totpTypes() {
        return Set.of(TOTP_CODE, TOTP_RECOVERY_CODE);
    }


    /**
     * 根据值获取类型。
     *
     * @param value
     * @return
     */
    public static MfaDeviceType valueOf(int value) {
        for (MfaDeviceType type : MfaDeviceType.values()) {
            if (value == type.value) {
                return type;
            }
        }
        return null;
    }


    public String getLabel() {
        return label;
    }

    public int getValue() {
        return value;
    }
}
