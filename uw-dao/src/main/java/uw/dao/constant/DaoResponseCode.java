package uw.dao.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import uw.common.response.ResponseCode;

/**
 * 通用返回代码。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum DaoResponseCode implements ResponseCode {

    TRANSACTION_ERROR("数据库操作执行失败！"),
    DATA_NOT_FOUND_WARN("数据未找到！"),
    ;

    /**
     * 错误信息。
     */
    private final String message;

    DaoResponseCode(String message) {
        this.message = message;
    }

    /**
     * 获取兜底错误信息（未经 i18n 解析）。
     *
     * @return 错误信息
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 获取配置前缀，固定为 {@code uw.dao}，同时作为 i18n basename（{@code i18n/messages/uw_dao}）的推导依据。
     *
     * @return 响应码前缀
     */
    @Override
    public String codePrefix() {
        return "uw.dao";
    }
}

