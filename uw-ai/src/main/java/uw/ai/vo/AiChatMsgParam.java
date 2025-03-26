package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * AiChat聊天消息参数。
 */
@Schema(title = "AiChat聊天消息参数", description = "AiChat聊天消息参数")
public class AiChatMsgParam {

    /**
     * sessionId
     */
    @Schema(title = "sessionId", description = "sessionId", requiredMode = Schema.RequiredMode.REQUIRED)
    private long sessionId;

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
     * 文件列表。
     */
    @Schema(title = "文件列表", description = "文件列表", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private MultipartFile[] fileList;

    public AiChatMsgParam() {
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
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

    public MultipartFile[] getFileList() {
        return fileList;
    }

    public void setFileList(MultipartFile[] fileList) {
        this.fileList = fileList;
    }
}
