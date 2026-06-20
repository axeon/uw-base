package uw.common.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.common.response.ResponseCode;
import uw.common.util.JsonUtils;

/**
 * 数据校验结果。
 * <p>
 * 由 {@link uw.common.app.helper.SchemaValidateHelper} 与 {@link uw.common.app.helper.JsonConfigHelper}
 * 在校验失败时产生，描述具体字段的错误码、错误信息及参考数据（如数值边界）。
 * </p>
 */
public class ValidateResult {

    /**
     * 校验失败的字段名。
     */
    @Schema(title = "属性名", description = "属性名")
    private final String name;

    /**
     * 校验失败的字段描述（取自 @Schema 的 title/description）。
     */
    @Schema(title = "属性信息", description = "属性信息")
    private final String title;

    /**
     * 完整错误码（含 codePrefix，如 uw.validate.not.null）。
     */
    @Schema(title = "错误码", description = "错误码")
    private final String errorCode;

    /**
     * 国际化错误信息（按当前 Locale 解析）。
     */
    @Schema(title = "错误信息", description = "错误信息")
    private final String errorMsg;

    /**
     * 参考数据（如最小值/最大值/正则表达式等，用于辅助定位问题）。
     */
    @Schema(title = "参考数据", description = "参考数据")
    private final String refData;

    /**
     * 构造校验结果。
     *
     * @param name         字段名
     * @param title        字段描述
     * @param responseCode 响应码（用于派生 errorCode 与本地化 errorMsg）
     * @param refData      参考数据
     */
    public ValidateResult(String name, String title, ResponseCode responseCode, String refData) {
        this.name = name;
        this.title = title;
        this.errorCode = responseCode.getFullCode();
        this.errorMsg = responseCode.getLocalizedMessage();
        this.refData = refData;
    }

    /**
     * JSON 序列化输出。
     *
     * @return JSON 字符串
     */
    @Override
    public String toString() {
        return JsonUtils.toString(this);
    }

    /**
     * 获取字段名。
     *
     * @return 字段名
     */
    public String getName() {
        return name;
    }

    /**
     * 获取字段描述。
     *
     * @return 字段描述
     */
    public String getTitle() {
        return title;
    }

    /**
     * 获取完整错误码。
     *
     * @return 完整错误码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 获取国际化错误信息。
     *
     * @return 错误信息
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * 获取参考数据。
     *
     * @return 参考数据
     */
    public String getRefData() {
        return refData;
    }
}
