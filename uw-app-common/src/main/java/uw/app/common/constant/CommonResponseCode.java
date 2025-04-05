package uw.app.common.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import uw.common.dto.ResponseCode;

/**
 * 通用返回代码。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum CommonResponseCode implements ResponseCode {

    COMMON_SAVE_ERROR( "数据创建失败！" ),
    COMMON_UPDATE_ERROR( "数据更新失败！" ),
    COMMON_EXIST_ERROR( "数据已存在！" ),
    COMMON_NOT_FOUND_ERROR( "数据未找到！" ),
    COMMON_STATE_ERROR( "数据状态错误！" ),
    ;

    /**
     * 国际化信息MESSAGE_SOURCE。
     */
    private static final ResourceBundleMessageSource MESSAGE_SOURCE = new ResourceBundleMessageSource() {{
        setBasename( "i18n/messages/uw_common" );
        setDefaultEncoding( "UTF-8" );
        setCacheSeconds( 0 );
    }};

    /**
     * 错误信息。
     */
    private final String message;

    CommonResponseCode(String message) {
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

