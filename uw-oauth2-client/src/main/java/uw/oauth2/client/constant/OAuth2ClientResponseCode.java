package uw.oauth2.client.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import uw.common.response.ResponseCode;

/**
 * OAuth2错误码枚举，定义清晰的错误码体系。
 * <p>
 * 支持国际化（资源文件位于 i18n/messages/uw_oauth2_client*.properties，basename 由 codePrefix 自动推导），
 * 序列化为JSON时输出为对象形态（含code与message）。
 *
 * @author axeon
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum OAuth2ClientResponseCode implements ResponseCode {
    /**
     * 成功
     */
    SUCCESS("成功"),

    /**
     * 不支持
     */
    NOT_SUPPORTED("不支持"),

    /**
     * 无效的供应商
     */
    INVALID_PROVIDER("无效的供应商"),

    /**
     * 无效的状态ID
     */
    INVALID_STATE_ID("无效的状态ID"),

    /**
     * 无效的HTTP状态码
     */
    INVALID_HTTP_CODE("无效的HTTP状态码：%s，错误：%s"),

    /**
     * HTTP请求失败
     */
    HTTP_REQUEST_FAILED("HTTP请求失败，错误：%s"),

    /**
     * 服务端错误
     */
    SERVER_ERROR("Server internal error"),

    /**
     * 未知错误
     */
    UNKNOWN_ERROR("Unknown error");

    /**
     * 错误信息。
     */
    private final String message;


    /**
     * 构造函数
     *
     * @param message 错误信息
     */
    OAuth2ClientResponseCode(String message) {
        this.message = message;
    }

    /**
     * 获取兜底响应消息（未经 i18n 解析）。
     *
     * @return 错误信息
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 获取响应码前缀，固定为 {@code uw.oauth2.client}，同时作为 i18n basename
     * （{@code i18n/messages/uw_oauth2_client}）的推导依据。
     *
     * @return 响应码前缀
     */
    @Override
    public String codePrefix() {
        return "uw.oauth2.client";
    }

}