package uw.mfa.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 登录错误信息。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum MfaCodeType {

    // ip限制
    IP_AUTH_ERROR( "MFA-0010", "你的IP[%s]不在授权IP范围内! " ),

    IP_LIMIT_WARN( "MFA-0011", "您的IP[%s]已经在[%s]分钟内连续[%s]次登录失败! " ),

    IP_LIMIT_ERROR( "MFA-0012", "您的IP[%s]已经在[%s]分钟内连续[%s]次登录失败! 请[%s]分钟后再试! " ),

    // Captcha
    CAPTCHA_LOST_FEE( "MFA-0020", "欠费! " ),

    CAPTCHA_SEND_LIMIT( "MFA-0021", "您的IP[%s]已经在[%s]分钟内连续[%s]次发送验证图片! 请[%s]分钟后再试! " ),

    CAPTCHA_GENERATE_FAIL( "MFA-0022", "图形验证码生成错误! " ),

    CAPTCHA_LOST( "MFA-0023", "图形验证码信息丢失! " ),

    CAPTCHA_VERIFY_FAIL( "MFA-0024", "图形识别码验证错误! " ),

    // 设备码
    DEVICE_CODE_LOST_FEE( "MFA-0030", "欠费! " ),

    DEVICE_CODE_SEND_LIMIT( "MFA-0031", "您的IP[%s]已经在[%s]分钟内连续[%s]次发送验证码! 请[%s]分钟后再试! " ),

    DEVICE_CODE_SEND_FAIL( "MFA-0032", "设备认证码发送失败! " ),

    DEVICE_CODE_LOST( "MFA-0033", "设备认证码信息丢失! " ),

    DEVICE_CODE_VERIFY_FAIL( "MFA-0034", "设备认证码验证错误! " ),

    DEVICE_TYPE_ERROR( "MFA-0035", "设备类型错误! " ),

    // TOTP
    TOTP_SECRET_GEN_ERROR( "MFA-0050", "TOTP密钥生成失败! " ),

    TOTP_SECRET_MATCH_FAIL( "MFA-0051", "TOTP密钥不匹配! " ),

    TOTP_SECRET_LOST( "MFA-0052", "TOTP密钥信息丢失! " ),

    TOTP_CODE_LOST( "MFA-0053", "TOTP认证码信息丢失! " ),

    TOTP_CODE_VERIFY_FAIL( "MFA-0054", "TOTP认证码验证错误! " ),

    TOTP_RECOVERY_CODE_VERIFY_FAIL( "MFA-0055", "TOTP恢复认证码验证错误! " ),

    ;
    /**
     * 错误代码。
     */
    private String code;

    /**
     * 错误信息。
     */
    private String message;

    MfaCodeType(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static MfaCodeType findByCode(String code) {
        for (MfaCodeType e : MfaCodeType.values()) {
            if (code.equals( e.code )) {
                return e;
            }
        }
        return null;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
