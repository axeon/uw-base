package uw.common.app.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import uw.common.dto.ResponseCode;
import uw.common.util.EnumUtils;

/**
 * 通用返回代码。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum CommonResponseCode implements ResponseCode {

    ENTITY_LIST_ERROR( "entity.list.error" ),
    ENTITY_LOAD_ERROR( "entity.load.error" ),
    ENTITY_SAVE_ERROR( "entity.save.error" ),
    ENTITY_UPDATE_ERROR( "entity.update.error" ),
    ENTITY_DELETE_ERROR( "entity.delete.error" ),
    ENTITY_EXISTS_ERROR( "entity.exists.error" ),
    ENTITY_NOT_FOUND_ERROR( "entity.not.found.error" ),
    ENTITY_STATE_ERROR( "entity.state.error" ),
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
     * 响应码。
     */
    private final String code;
    /**
     * 错误信息。
     */
    private final String messageKey;

    CommonResponseCode(String messageKey) {
        this.code = EnumUtils.enumNameToDotCase( this.name() );
        this.messageKey = messageKey;
    }

    /**
     * 获取配置前缀.
     *
     * @return
     */
    @Override
    public String codePrefix() {
        return "uw.common";
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
        return getMessageSource().getMessage(messageKey, null, messageKey, null);
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