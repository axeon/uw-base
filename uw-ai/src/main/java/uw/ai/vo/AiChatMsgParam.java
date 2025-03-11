package uw.ai.vo;

import java.util.List;

/**
 * AiChat生成参数。
 */
public class AiChatMsgParam {

    /**
     * sessionId
     */
    private long sessionId;

    /**
     * 用户输入
     */
    private String userPrompt;

    /**
     * 系统提示
     */
    private String systemPrompt;

    /**
     * 工具信息。
     */
    private List<AiToolCallInfo> toolList;

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
}
