package uw.common.app.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import uw.common.dto.ResponseCode;
import uw.common.util.EnumUtils;

/**
 * schema校验返回代码。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ValidateResponseCode implements ResponseCode {
    NOT_NULL( "不能为NULL" ),
    NOT_EMPTY( "不能为空" ),
    VALUE_TOO_SMALL( "不能小于最小值" ),
    VALUE_TOO_LARGE( "不能大于最大值" ),
    LENGTH_TOO_SHORT( "不能小于最小长度" ),
    LENGTH_TOO_LONG( "不能大于最大长度" ),
    DATA_FORMAT_ERROR( "数据格式错误" ),
    REGEX_FORMAT_ERROR( "正则校验格式错误" ),
    ;

    /**
     * 国际化信息MESSAGE_SOURCE。
     */
    private static final ResourceBundleMessageSource MESSAGE_SOURCE = new ResourceBundleMessageSource() {{
        setBasename( "i18n/messages/uw_validate" );
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

    ValidateResponseCode(String message) {
        this.code = EnumUtils.enumNameToDotCase( this.name() );
        this.message = message;
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
     * 获取错误信息
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

    /**
     * 获取配置前缀.
     *
     * @return
     */
    @Override
    public String codePrefix() {
        return "uw.validate";
    }
}