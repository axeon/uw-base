package uw.mfa.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import uw.common.dto.ResponseCode;

/**
 * 登录错误信息。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum MfaResponseCode implements ResponseCode {

    // ip限制
    IP_AUTH_ERROR( "你的IP[%s]不在授权IP范围内! " ),

    IP_LIMIT_WARN( "您的IP[%s]已经在[%s]分钟内连续[%s]次登录失败! " ),

    IP_LIMIT_ERROR( "您的IP[%s]已经在[%s]分钟内连续[%s]次登录失败! 请[%s]分钟后再试! " ),

    // Captcha
    CAPTCHA_LOST_FEE( "欠费! " ),

    CAPTCHA_SEND_LIMIT( "您的IP[%s]已经在[%s]分钟内连续[%s]次发送验证图片! 请[%s]分钟后再试! " ),

    CAPTCHA_GENERATE_FAIL( "图形验证码生成错误! " ),

    CAPTCHA_LOST( "图形验证码信息丢失! " ),

    CAPTCHA_VERIFY_FAIL( "图形识别码验证错误! " ),

    // 设备码
    DEVICE_CODE_LOST_FEE( "运营商欠费! " ),

    DEVICE_CODE_SEND_LIMIT( "您的IP[%s]已经在[%s]分钟内连续[%s]次发送验证码! 请[%s]分钟后再试! " ),

    DEVICE_CODE_SEND_FAIL( "设备认证码发送失败! " ),

    DEVICE_CODE_LOST( "设备认证码信息丢失! " ),

    DEVICE_CODE_VERIFY_FAIL( "设备认证码验证错误! " ),

    DEVICE_TYPE_ERROR( "设备类型错误! " ),

    // TOTP
    TOTP_SECRET_GEN_ERROR( "TOTP密钥生成失败! " ),

    TOTP_SECRET_MATCH_FAIL( "TOTP密钥不匹配! " ),

    TOTP_SECRET_LOST( "TOTP密钥信息丢失! " ),

    TOTP_CODE_LOST( "TOTP认证码信息丢失! " ),

    TOTP_CODE_VERIFY_FAIL( "TOTP认证码验证错误! " ),

    TOTP_RECOVERY_CODE_VERIFY_FAIL( "TOTP恢复认证码验证错误! " ),

    ;
    /**
     * 国际化信息MESSAGE_SOURCE。
     */
    private static final ResourceBundleMessageSource MESSAGE_SOURCE = new ResourceBundleMessageSource() {{
        setBasename( "i18n/messages/uw_mfa" );
        setDefaultEncoding( "UTF-8" );
        setCacheSeconds( 0 );
    }};

    /**
     * 错误信息。
     */
    private String message;

    MfaResponseCode(String message) {
        this.message = message;
    }


    /**
     * 获取响应码
     *
     * @return
     */
    @Override
    public String getCode() {
        return name();
    }

    /**
     * 获取响应消息
     *
     * @param params
     * @return
     */
    @Override
    public String getMessage(Object... params) {
        return String.format( message, params );
    }

    /**
     * 获取国际化消息
     *
     * @param params
     * @return
     */
    @Override
    public String getI18Message(Object... params) {
        return MESSAGE_SOURCE.getMessage( name(), params, LocaleContextHolder.getLocale() );
    }

    /**
     * 获取响应消息
     *
     * @return
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 获取国际化消息
     *
     * @return
     */
    @Override
    public String getI18Message() {
        return ResponseCode.super.getI18Message();
    }
}
