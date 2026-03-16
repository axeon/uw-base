package uw.oauth2.client.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.context.support.ResourceBundleMessageSource;
import uw.common.dto.ResponseCode;
import uw.common.util.EnumUtils;

/**
 * OAuth2错误码枚举，定义清晰的错误码体系
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
     * 无效的授权码
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
     * 国际化信息MESSAGE_SOURCE。
     */
    private static final ResourceBundleMessageSource MESSAGE_SOURCE = new ResourceBundleMessageSource() {{
        setBasename( "i18n/messages/uw_oauth2_client" );
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


    /**
     * 构造函数
     *
     * @param message 错误信息
     */
    OAuth2ClientResponseCode(String message) {
        this.code = EnumUtils.enumNameToDotCase( this.name() );
        this.message = message;
    }

    /**
     * 获取配置前缀.
     *
     * @return
     */
    @Override
    public String codePrefix() {
        return "uw.oauth2.client";
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
    public ResourceBundleMessageSource messageSource() {
        return MESSAGE_SOURCE;
    }

}