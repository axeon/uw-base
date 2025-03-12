package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AiTool调用信息。
 */
@Schema(title = "AiTool调用信息", description = "AiTool调用信息")
public class AiToolCallInfo {

    /**
     * 工具代码。
     */
    @Schema(title = "工具代码", description = "工具代码",requiredMode = Schema.RequiredMode.REQUIRED)
    private String toolCode;

    /**
     * 是否直接返回结果。
     */
    @Schema(title = "是否直接返回", description = "是否直接返回",requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean returnDirect;

    public AiToolCallInfo(String toolCode, boolean returnDirect) {
        this.toolCode = toolCode;
        this.returnDirect = returnDirect;
    }

    public String getToolCode() {
        return toolCode;
    }

    public void setToolCode(String toolCode) {
        this.toolCode = toolCode;
    }

    public boolean isReturnDirect() {
        return returnDirect;
    }

    public void setReturnDirect(boolean returnDirect) {
        this.returnDirect = returnDirect;
    }
}
