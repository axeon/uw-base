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
     * rag知识库id列表。
     */
    @Schema(title = "rag知识库id列表", description = "rag知识库id列表", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private long[] ragLibIds;

    /**
     * 文件列表。
     */
    @Schema(title = "文件列表", description = "文件列表", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private MultipartFile[] fileList;

    public AiChatMsgParam() {
    }

    private AiChatMsgParam(Builder builder) {
        setSessionId(builder.sessionId);
        setUserPrompt(builder.userPrompt);
        setSystemPrompt(builder.systemPrompt);
        setToolList(builder.toolList);
        setToolContext(builder.toolContext);
        setRagLibIds(builder.ragLibIds);
        setFileList(builder.fileList);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(AiChatMsgParam copy) {
        Builder builder = new Builder();
        builder.sessionId = copy.getSessionId();
        builder.userPrompt = copy.getUserPrompt();
        builder.systemPrompt = copy.getSystemPrompt();
        builder.toolList = copy.getToolList();
        builder.toolContext = copy.getToolContext();
        builder.ragLibIds = copy.getRagLibIds();
        builder.fileList = copy.getFileList();
        return builder;
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

    public static final class Builder {
        private long sessionId;
        private String userPrompt;
        private String systemPrompt;
        private List<AiToolCallInfo> toolList;
        private Map<String, Object> toolContext;
        private long[] ragLibIds;
        private MultipartFile[] fileList;

        private Builder() {
        }

        public Builder sessionId(long sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder userPrompt(String userPrompt) {
            this.userPrompt = userPrompt;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder toolList(List<AiToolCallInfo> toolList) {
            this.toolList = toolList;
            return this;
        }

        public Builder toolContext(Map<String, Object> toolContext) {
            this.toolContext = toolContext;
            return this;
        }

        public Builder ragLibIds(long[] ragLibIds) {
            this.ragLibIds = ragLibIds;
            return this;
        }

        public Builder fileList(MultipartFile[] fileList) {
            this.fileList = fileList;
            return this;
        }

        public AiChatMsgParam build() {
            return new AiChatMsgParam(this);
        }
    }
}
