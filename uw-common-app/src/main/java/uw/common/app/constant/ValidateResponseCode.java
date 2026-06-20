package uw.common.app.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import uw.common.response.ResponseCode;
import uw.common.util.EnumUtils;

/**
 * Schema 数据校验返回代码。
 * <p>
 * 实现 {@link ResponseCode} 接口，codePrefix 为 {@code uw.validate}，对应完整响应码形如 {@code uw.validate.not.null}。
 * 国际化资源位于 {@code i18n/messages/uw_validate}，由 {@link uw.common.app.helper.SchemaValidateHelper} 与
 * {@link uw.common.app.helper.JsonConfigHelper} 在校验失败时引用。序列化为 JSON 时以对象形态输出。
 * </p>
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ValidateResponseCode implements ResponseCode {
    /** 值为 null（违反必填约束）。 */
    NOT_NULL("不能为NULL"),
    /** 值为空字符串或空白（违反非空约束）。 */
    NOT_EMPTY("不能为空"),
    /** 数值小于 minimum。 */
    VALUE_TOO_SMALL("不能小于最小值"),
    /** 数值大于 maximum。 */
    VALUE_TOO_LARGE("不能大于最大值"),
    /** 字符串长度小于 minLength。 */
    LENGTH_TOO_SHORT("不能小于最小长度"),
    /** 字符串长度大于 maxLength。 */
    LENGTH_TOO_LONG("不能大于最大长度"),
    /** 数据格式错误（如数值/日期/Map 解析失败、枚举值不在可选集合）。 */
    DATA_FORMAT_ERROR("数据格式错误"),
    /** 正则校验格式错误。 */
    REGEX_FORMAT_ERROR("正则校验格式错误"),
    ;

    /**
     * 国际化信息 MESSAGE_SOURCE。
     * 基于 {@code i18n/messages/uw_validate} 资源包，UTF-8 编码，默认开启缓存。
     */
    private static final ResourceBundleMessageSource MESSAGE_SOURCE = new ResourceBundleMessageSource() {{
        setBasename("i18n/messages/uw_validate");
        setDefaultEncoding("UTF-8");
    }};

    /**
     * 响应码（由枚举名转换为点分小写形式，如 {@code not.null}）。
     */
    private final String code;
    /**
     * 默认错误信息（未命中 i18n 时的兜底文案）。
     */
    private final String message;

    /**
     * 构造校验响应码。
     *
     * @param message 默认错误信息
     */
    ValidateResponseCode(String message) {
        this.code = EnumUtils.enumNameToDotCase(this.name());
        this.message = message;
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

    /**
     * 获取响应码前缀，固定为 {@code uw.validate}，与 code 拼接构成完整响应码。
     *
     * @return 响应码前缀
     */
    @Override
    public String codePrefix() {
        return "uw.validate";
    }
}