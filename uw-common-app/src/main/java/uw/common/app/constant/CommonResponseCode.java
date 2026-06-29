package uw.common.app.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import uw.common.response.ResponseCode;

/**
 * 通用返回代码。
 * <p>
 * 实现 {@link ResponseCode} 接口，codePrefix 为 {@code uw.common}，对应完整响应码形如 {@code uw.common.entity.not.found.error}。
 * 国际化资源位于 {@code i18n/messages/uw_common}（basename 由 codePrefix 按 {@code 点转下划线} 规范自动推导），支持多语言。
 * 序列化为 JSON 时以对象形态输出（包含 code 与 message）。
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
     * 默认错误信息（未命中 i18n 时的兜底文案）。
     */
    private final String message;

    /**
     * 构造响应码。
     *
     * @param message 默认错误信息
     */
    CommonResponseCode(String message) {
        this.message = message;
    }

    /**
     * 获取兜底错误信息（未经 i18n 解析），供 {@link ResponseCode#getLocalizedMessage(Object...)} 作为兜底使用。
     *
     * @return 默认错误信息
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 获取响应码前缀，固定为 {@code uw.common}，与短码拼接构成完整响应码，
     * 同时作为 i18n basename（{@code i18n/messages/uw_common}）的推导依据。
     *
     * @return 响应码前缀
     */
    @Override
    public String codePrefix() {
        return "uw.common";
    }
}