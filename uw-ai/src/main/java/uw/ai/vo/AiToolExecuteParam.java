package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AiTool执行参数。
 */
@Schema(title = "AiTool执行参数", description = "AiTool执行参数")
public class AiToolExecuteParam {

    @Schema(title = "工具id", description = "工具id", requiredMode = Schema.RequiredMode.REQUIRED)
    private long toolId;

    @Schema(title = "工具类", description = "工具类", requiredMode = Schema.RequiredMode.REQUIRED)
    private String toolClass;

    @Schema(title = "工具输入信息", description = "工具输入信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private String toolInput;

    public AiToolExecuteParam(long toolId, String toolClass, String toolInput) {
        this.toolId = toolId;
        this.toolClass = toolClass;
        this.toolInput = toolInput;
    }

    public AiToolExecuteParam() {
    }

    private AiToolExecuteParam(Builder builder) {
        setToolId(builder.toolId);
        setToolClass(builder.toolClass);
        setToolInput(builder.toolInput);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(AiToolExecuteParam copy) {
        Builder builder = new Builder();
        builder.toolId = copy.getToolId();
        builder.toolClass = copy.getToolClass();
        builder.toolInput = copy.getToolInput();
        return builder;
    }

    public long getToolId() {
        return toolId;
    }

    public void setToolId(long toolId) {
        this.toolId = toolId;
    }

    public String getToolClass() {
        return toolClass;
    }

    public void setToolClass(String toolClass) {
        this.toolClass = toolClass;
    }

    public String getToolInput() {
        return toolInput;
    }

    public void setToolInput(String toolInput) {
        this.toolInput = toolInput;
    }

    public static final class Builder {
        private long toolId;
        private String toolClass;
        private String toolInput;

        private Builder() {
        }

        public Builder toolId(long toolId) {
            this.toolId = toolId;
            return this;
        }

        public Builder toolClass(String toolClass) {
            this.toolClass = toolClass;
            return this;
        }

        public Builder toolInput(String toolInput) {
            this.toolInput = toolInput;
            return this;
        }

        public AiToolExecuteParam build() {
            return new AiToolExecuteParam(this);
        }
    }
}
