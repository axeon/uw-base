package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * AiChat生成参数。
 */
@Schema(title = "AiChat生成参数", description = "AiChat生成参数")
public class AiChatGenerateParam {

    /**
     * 配置Id
     */
    @Schema(title = "配置Id", description = "配置Id", requiredMode = Schema.RequiredMode.REQUIRED)
    private long configId;

    /**
     * 用户输入
     */
    @Schema(title = "用户输入", description = "用户输入", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userPrompt;

    /**
     * 系统提示
     */
    @Schema(title = "系统提示", description = "系统提示", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String systemPrompt;

    /**
     * 工具信息。
     */
    @Schema(title = "工具列表", description = "工具列表", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<AiToolCallInfo> toolList;

    /**
     * 工具上下文。
     */
    @Schema(title = "工具上下文", description = "工具上下文", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Map<String,Object> toolContext;

    /**
     * rag知识库id列表。
     */
    @Schema(title = "rag知识库id列表", description = "rag知识库id列表", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private long[] ragLibIds;

    /**
     * 文件列表。
     */
    @Schema(title = "文件列表", description = "文件列表", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private MultipartFile[] fileList;

    public AiChatGenerateParam() {
    }

    public long getConfigId() {
        return configId;
    }

    public void setConfigId(long configId) {
        this.configId = configId;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public List<AiToolCallInfo> getToolList() {
        return toolList;
    }

    public void setToolList(List<AiToolCallInfo> toolList) {
        this.toolList = toolList;
    }

    public Map<String, Object> getToolContext() {
        return toolContext;
    }

    public void setToolContext(Map<String, Object> toolContext) {
        this.toolContext = toolContext;
    }

    public long[] getRagLibIds() {
        return ragLibIds;
    }

    public void setRagLibIds(long[] ragLibIds) {
        this.ragLibIds = ragLibIds;
    }

    public MultipartFile[] getFileList() {
        return fileList;
    }

    public void setFileList(MultipartFile[] fileList) {
        this.fileList = fileList;
    }
}
