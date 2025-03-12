package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工具元数据。
 */
@Schema(title = "工具元数据", description = "工具元数据")
public class AiToolMeta {

    /**
     * ID
     */
    @Schema(title = "ID", description = "ID")
    private long id;

    /**
     * 应用名
     */
    @Schema(title = "应用名", description = "应用名")
    private String appName;

    /**
     * 工具类
     */
    @Schema(title = "工具类", description = "工具类")
    private String toolClass;

    /**
     * 工具版本
     */
    @Schema(title = "工具版本", description = "工具版本")
    private String toolVersion;

    /**
     * 工具名称
     */
    @Schema(title = "工具名称", description = "工具名称")
    private String toolName;

    /**
     * 工具描述
     */
    @Schema(title = "工具描述", description = "工具描述")
    private String toolDesc;

    /**
     * 工具输入参数
     */
    @Schema(title = "工具输入参数", description = "工具输入参数")
    private String toolInput;

    /**
     * 工具输出配置
     */
    @Schema(title = "工具输出配置", description = "工具输出配置")
    private String toolOutput;

    public AiToolMeta() {
    }

    public AiToolMeta(long id, String appName, String toolClass, String toolVersion, String toolName, String toolDesc, String toolInput, String toolOutput) {
        this.id = id;
        this.appName = appName;
        this.toolClass = toolClass;
        this.toolVersion = toolVersion;
        this.toolName = toolName;
        this.toolDesc = toolDesc;
        this.toolInput = toolInput;
        this.toolOutput = toolOutput;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getToolClass() {
        return toolClass;
    }

    public void setToolClass(String toolClass) {
        this.toolClass = toolClass;
    }

    public String getToolVersion() {
        return toolVersion;
    }

    public void setToolVersion(String toolVersion) {
        this.toolVersion = toolVersion;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolDesc() {
        return toolDesc;
    }

    public void setToolDesc(String toolDesc) {
        this.toolDesc = toolDesc;
    }

    public String getToolInput() {
        return toolInput;
    }

    public void setToolInput(String toolInput) {
        this.toolInput = toolInput;
    }

    public String getToolOutput() {
        return toolOutput;
    }

    public void setToolOutput(String toolOutput) {
        this.toolOutput = toolOutput;
    }
}
