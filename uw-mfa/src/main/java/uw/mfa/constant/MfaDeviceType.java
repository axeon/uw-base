package uw.mfa.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * mfa设备类型
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(title = "mfa设备类型", description = "mfa设备类型")
public enum MfaDeviceType {

    /**
     * Email验证码登录
     */
    EMAIL( 22, "Email验证码登录" ),

    /**
     * 手机号验证码登录
     */
    MOBILE( 23, "手机号验证码登录" );

    /**
     * 参数值
     */
    private int value;

    /**
     * 参数信息。
     */
    private String label;

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
        for (MfaDeviceType type : MfaDeviceType.values()) {
            if (value == type.value) {
                return true;
            }
        }
        return false;
    }


    public String getLabel() {
        return label;
    }

    public int getValue() {
        return value;
    }
}
