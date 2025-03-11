package uw.ai.vo;

/**
 * AiChat生成参数。
 */
public class AiChatGenerateParam {
    /**
     * saasId
     */
    private long saasId;

    /**
     * 用户Id
     */
    private long userId;

    /**
     * 用户类型
     */
    private int userType;

    /**
     * 用户信息
     */
    private String userInfo;

    /**
     * 配置Id
     */
    private long configId;

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
    private String[] toolInfo;
}
