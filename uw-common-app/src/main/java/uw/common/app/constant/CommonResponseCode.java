package uw.common.app.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import uw.common.response.ResponseCode;
import uw.common.util.EnumUtils;

/**
 * 通用返回代码。
 * <p>
 * 实现 {@link ResponseCode} 接口，codePrefix 为 {@code uw.common}，对应完整响应码形如 {@code uw.common.entity.not.found.error}。
 * 国际化资源位于 {@code i18n/messages/uw_common}，支持多语言。序列化为 JSON 时以对象形态输出（包含 code 与 message）。
 * </p>
 * 使用示例：
 * <pre>{@code
 * return ResponseData.warnCode(CommonResponseCode.ENTITY_NOT_FOUND_ERROR);
 * }</pre>
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum CommonResponseCode implements ResponseCode {

    /** 数据列表查询失败。 */
    ENTITY_LIST_ERROR("数据列表失败"),
    /** 数据加载（单条）失败。 */
    ENTITY_LOAD_ERROR("数据加载失败"),
    /** 数据保存（新增）失败。 */
    ENTITY_SAVE_ERROR("数据保存失败"),
    /** 数据更新失败。 */
    ENTITY_UPDATE_ERROR("数据更新失败"),
    /** 数据删除失败。 */
    ENTITY_DELETE_ERROR("数据删除失败"),
    /** 数据已存在（唯一性冲突）。 */
    ENTITY_EXISTS_ERROR("数据已存在"),
    /** 数据未找到。 */
    ENTITY_NOT_FOUND_ERROR("数据未找到"),
    /** 数据状态错误（如状态机非法迁移）。 */
    ENTITY_STATE_ERROR("数据状态错误"),
    ;

    /**
     * 国际化信息 MESSAGE_SOURCE。
     * 基于 {@code i18n/messages/uw_common} 资源包，UTF-8 编码，默认开启缓存。
     */
    private static final ResourceBundleMessageSource MESSAGE_SOURCE = new ResourceBundleMessageSource() {{
        setBasename("i18n/messages/uw_common");
        setDefaultEncoding("UTF-8");
    }};

    /**
     * 响应码（由枚举名转换为点分小写形式，如 {@code entity.not.found.error}）。
     */
    private final String code;
    /**
     * 默认错误信息（未命中 i18n 时的兜底文案）。
     */
    private final String message;

    /**
     * 构造响应码。
     *
     * @param message 默认错误信息
     */
    CommonResponseCode(String message) {
        this.code = EnumUtils.enumNameToDotCase(this.name());
        this.message = message;
    }

    /**
     * 获取响应码前缀，固定为 {@code uw.common}，与 code 拼接构成完整响应码。
     *
     * @return 响应码前缀
     */
    @Override
    public String codePrefix() {
        return "uw.common";
    }

    /**
     * 获取响应码（点分小写形式）。
     *
     * @return 响应码
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * 获取默认错误信息（未命中 i18n 时使用）。
     *
     * @return 默认错误信息
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 获取国际化消息源，用于按 Locale 解析本地化文案。
     *
     * @return 消息源
     */
    @Override
    public MessageSource messageSource() {
        return MESSAGE_SOURCE;
    }


}