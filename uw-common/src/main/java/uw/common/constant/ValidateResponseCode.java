package uw.common.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import uw.common.response.ResponseCode;

/**
 * Schema 数据校验返回代码。
 * <p>
 * 实现 {@link ResponseCode} 接口，codePrefix 为 {@code uw.validate}，对应完整响应码形如 {@code uw.validate.not.null}。
 * 国际化资源位于 {@code i18n/messages/uw_validate}（basename 由 codePrefix 按 {@code 点转下划线} 规范自动推导），
 * 由 {@link uw.common.helper.SchemaValidateHelper} 与 {@link uw.common.helper.JsonConfigHelper} 在校验失败时引用。
 * 序列化为 JSON 时以对象形态输出。
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
     * 默认错误信息（未命中 i18n 时的兜底文案）。
     */
    private final String message;

    /**
     * 构造校验响应码。
     *
     * @param message 默认错误信息
     */
    ValidateResponseCode(String message) {
        this.message = message;
    }

    /**
     * 获取默认错误信息（兜底文案，未经 i18n 解析）。
     * <p>
     * 供 {@link ResponseCode#getLocalizedMessage(Object...)} 作为兜底使用。
     * </p>
     *
     * @return 默认错误信息
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 获取响应码前缀，固定为 {@code uw.validate}，与短码拼接构成完整响应码，
     * 同时作为 i18n basename（{@code i18n/messages/uw_validate}）的推导依据。
     *
     * @return 响应码前缀
     */
    @Override
    public String codePrefix() {
        return "uw.validate";
    }
}
