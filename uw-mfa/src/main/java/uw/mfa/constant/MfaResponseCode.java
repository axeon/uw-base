package uw.mfa.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import uw.common.response.ResponseCode;

/**
 * MFA模块响应码枚举。
 * <p>涵盖IP限制、Captcha、设备验证码、TOTP四类业务场景的错误码，</p>
 * <p>codePrefix为 {@code uw.mfa}，i18n资源文件为 {@code i18n/messages/uw_mfa}（basename 由 codePrefix 自动推导）。</p>
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum MfaResponseCode implements ResponseCode {

    // ip限制
    IP_AUTH_ERROR("你的IP[%s]不在授权IP范围内! "),

    IP_LIMIT_WARN("您的IP[%s]已经在[%s]分钟内连续[%s]次验证失败! "),

    IP_LIMIT_ERROR("您的IP[%s]已经在[%s]分钟内连续[%s]次验证失败! 请[%s]分钟后再试! "),

    // Captcha
    CAPTCHA_FEE_ERROR("欠费! "),

    CAPTCHA_SEND_LIMIT_ERROR("您的IP[%s]已经在[%s]分钟内连续[%s]次发送验证图片! 请[%s]分钟后再试! "),

    CAPTCHA_GENERATE_ERROR("图形验证码生成错误! "),

    CAPTCHA_LOST_ERROR("图形验证码信息丢失! "),

    CAPTCHA_VERIFY_ERROR("图形识别码验证错误! "),

    // 设备码
    DEVICE_CODE_FEE_ERROR("设备验证码发送欠费! "),

    DEVICE_CODE_SEND_LIMIT_ERROR("您的IP[%s]已经在[%s]分钟内连续[%s]次发送验证码! 请[%s]分钟后再试! "),

    DEVICE_CODE_VERIFY_LIMIT_ERROR("您的设备ID[%s]已经在[%s]分钟内连续[%s]次验证错误! 请[%s]分钟后再试! "),

    DEVICE_CODE_SEND_ERROR("设备验证码发送失败! "),

    DEVICE_CODE_LOST_ERROR("设备验证码信息丢失! "),

    DEVICE_CODE_VERIFY_ERROR("设备验证码校验错误! "),

    DEVICE_TYPE_ERROR("设备类型错误! "),

    // TOTP
    TOTP_SECRET_GEN_ERROR("TOTP密钥生成失败! %s"),

    TOTP_SECRET_MATCH_ERROR("TOTP密钥不匹配! "),

    TOTP_SECRET_LOST_ERROR("TOTP密钥信息丢失! "),

    TOTP_CODE_LOST_ERROR("TOTP验证码信息丢失! "),

    TOTP_CODE_VERIFY_ERROR("TOTP验证码校验错误! "),

    TOTP_RECOVERY_CODE_VERIFY_ERROR("TOTP恢复验证码校验错误! "),

    TOTP_VERIFY_LIMIT_ERROR("您的用户ID[%s]已经在[%s]分钟内连续[%s]次验证错误! 请[%s]分钟后再试! "),

    ;

    /**
     * 默认错误信息（支持 {@code %s} 占位符，由 {@code getMessage(params)} 格式化）。
     */
    private final String message;

    MfaResponseCode(String message) {
        this.message = message;
    }

    /**
     * 获取兜底响应消息（未经 i18n 与参数格式化）。
     *
     * @return 默认消息
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 获取响应码前缀，固定为 {@code uw.mfa}，同时作为 i18n basename（{@code i18n/messages/uw_mfa}）的推导依据。
     *
     * @return 响应码前缀
     */
    @Override
    public String codePrefix() {
        return "uw.mfa";
    }
}
