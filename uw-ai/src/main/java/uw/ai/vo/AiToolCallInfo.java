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

    public AiToolCallInfo() {
    }

    public AiToolCallInfo(String toolCode, boolean returnDirect) {
        this.toolCode = toolCode;
        this.returnDirect = returnDirect;
    }

    private AiToolCallInfo(Builder builder) {
        setToolCode(builder.toolCode);
        setReturnDirect(builder.returnDirect);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(AiToolCallInfo copy) {
        Builder builder = new Builder();
        builder.toolCode = copy.getToolCode();
        builder.returnDirect = copy.isReturnDirect();
        return builder;
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

    public static final class Builder {
        private String toolCode;
        private boolean returnDirect;

        private Builder() {
        }

        public Builder toolCode(String toolCode) {
            this.toolCode = toolCode;
            return this;
        }

        public Builder returnDirect(boolean returnDirect) {
            this.returnDirect = returnDirect;
            return this;
        }

        public AiToolCallInfo build() {
            return new AiToolCallInfo(this);
        }
    }
}
