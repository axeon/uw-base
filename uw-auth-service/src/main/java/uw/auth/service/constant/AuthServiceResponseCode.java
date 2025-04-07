package uw.auth.service.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import uw.common.dto.ResponseCode;
import uw.common.util.EnumUtils;

/**
 * uw-service响应码信息。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum AuthServiceResponseCode implements ResponseCode {

    AUTH_SERVICE_LOADING_WARN("auth-service正在启动中！"),

    TOKEN_HEADER_ERROR("Access Token[%s] Header错误！"),

    TOKEN_ILLEGAL_ERROR("Access Token[%s]非法！"),

    TOKEN_DECODE_ERROR("Access Token[%s]解密异常！原因: %s"),

    TOKEN_INFO_MISMATCH_ERROR("Access Token[%s]用户类型、用户ID或SAAS_ID无效！"),

    TOKEN_INVALID_ERROR("Access Token[%s]非法！原因：%s"),

    TOKEN_EXPIRED_ERROR("Access Token[%s]已过期！"),
    ;

    /**
     * 国际化信息MESSAGE_SOURCE。
     */
    private static final ResourceBundleMessageSource MESSAGE_SOURCE = new ResourceBundleMessageSource() {{
        setBasename( "i18n/messages/uw_auth_center" );
        setDefaultEncoding( "UTF-8" );
    }};

    private final String code;

    /**
     * 错误信息。
     */
    private final String message;

    AuthServiceResponseCode(String message) {
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
        return "uw.auth.service";
    }

    /**
     * 获得错误代码。
     *
     * @return
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * 获得错误信息。
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
    public MessageSource getMessageSource() {
        return MESSAGE_SOURCE;
    }


}
