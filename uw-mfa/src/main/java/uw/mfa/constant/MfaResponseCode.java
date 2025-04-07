package uw.mfa.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.context.support.ResourceBundleMessageSource;
import uw.common.dto.ResponseCode;
import uw.common.util.EnumUtils;

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
    CAPTCHA_FEE_ERROR( "欠费! " ),

    CAPTCHA_SEND_LIMIT_ERROR( "您的IP[%s]已经在[%s]分钟内连续[%s]次发送验证图片! 请[%s]分钟后再试! " ),

    CAPTCHA_GENERATE_ERROR( "图形验证码生成错误! " ),

    CAPTCHA_LOST_ERROR( "图形验证码信息丢失! " ),

    CAPTCHA_VERIFY_ERROR( "图形识别码验证错误! " ),

    // 设备码
    DEVICE_CODE_FEE_ERROR( "运营商欠费! " ),

    DEVICE_CODE_SEND_LIMIT_ERROR( "您的IP[%s]已经在[%s]分钟内连续[%s]次发送验证码! 请[%s]分钟后再试! " ),

    DEVICE_CODE_SEND_ERROR( "设备认证码发送失败! " ),

    DEVICE_CODE_LOST_ERROR( "设备认证码信息丢失! " ),

    DEVICE_CODE_VERIFY_ERROR( "设备认证码验证错误! " ),

    DEVICE_TYPE_ERROR( "设备类型错误! " ),

    // TOTP
    TOTP_SECRET_GEN_ERROR( "TOTP密钥生成失败! " ),

    TOTP_SECRET_MATCH_ERROR( "TOTP密钥不匹配! " ),

    TOTP_SECRET_LOST_ERROR( "TOTP密钥信息丢失! " ),

    TOTP_CODE_LOST_ERROR( "TOTP认证码信息丢失! " ),

    TOTP_CODE_VERIFY_ERROR( "TOTP认证码验证错误! " ),

    TOTP_RECOVERY_CODE_VERIFY_ERROR( "TOTP恢复认证码验证错误! " ),

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
     * 响应码。
     */
    private final String code;

    /**
     * 错误信息。
     */
    private final String message;

    MfaResponseCode(String message) {
        this.code = EnumUtils.enumNameToDotCase( this.name() );
        this.message = message;
    }

    /**
     * 获取配置前缀.
     *
     * @return
     */
    @Override
    public String configPrefix() {
        return "uw.mfa";
    }

    /**
     * 获取响应码
     *
     * @return
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * 获取响应消息.
     *
     * @return
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 获取消息源.
     *
     * @return
     */
    @Override
    public ResourceBundleMessageSource getMessageSource() {
        return MESSAGE_SOURCE;
    }


}
