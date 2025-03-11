package uw.ai.vo;

/**
 * AiTool调用信息。
 */
public class AiToolCallInfo {

    /**
     * 工具代码。
     */
    private String toolCode;

    /**
     * 是否直接返回结果。
     */
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
