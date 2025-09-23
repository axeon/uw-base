package uw.common.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.common.dto.ResponseCode;
import uw.common.util.JsonUtils;

/**
 * 检查结果。
 */
public class ValidateResult {

    /**
     * 属性名。
     */
    @Schema(title = "属性名", description = "属性名")
    private final String name;

    /**
     * 属性信息。
     */
    @Schema(title = "属性信息", description = "属性信息")
    private final String title;

    /**
     * 错误码。
     */
    @Schema(title = "错误码", description = "错误码")
    private final String errorCode;

    /**
     * 错误信息。
     */
    @Schema(title = "错误信息", description = "错误信息")
    private final String errorMsg;

    /**
     * 参考数据。
     */
    @Schema(title = "参考数据", description = "参考数据")
    private final String refData;

    public ValidateResult(String name, String title, ResponseCode responseCode, String refData) {
        this.name = name;
        this.title = title;
        this.errorCode = responseCode.getFullCode();
        this.errorMsg = responseCode.getLocalizedMessage();
        this.refData = refData;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(this);
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public String getRefData() {
        return refData;
    }
}
