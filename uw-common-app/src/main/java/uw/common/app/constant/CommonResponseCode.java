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

    ENTITY_LIST_ERROR( "数据列表失败" ),
    ENTITY_LOAD_ERROR( "数据加载失败" ),
    ENTITY_SAVE_ERROR( "数据保存失败" ),
    ENTITY_UPDATE_ERROR( "数据更新失败" ),
    ENTITY_DELETE_ERROR( "数据删除失败" ),
    ENTITY_EXISTS_ERROR( "数据已存在" ),
    ENTITY_NOT_FOUND_ERROR( "数据未找到" ),
    ENTITY_STATE_ERROR( "数据状态错误" ),
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
    private final String message;

    CommonResponseCode(String message) {
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
        return message;
    }

    /**
     * 获取消息源.
     *
     * @return
     */
    @Override
    public MessageSource messageSource() {
        return MESSAGE_SOURCE;
    }


}