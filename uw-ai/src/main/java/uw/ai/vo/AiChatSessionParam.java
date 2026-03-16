package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.auth.service.AuthServiceHelper;

import java.util.List;

/**
 * AiChat session初始化参数。
 */
@Schema(title = "AiChat session初始化参数", description = "AiChat session初始化参数")
public class AiChatSessionParam {

    /**
     * SaasId
     */
    @Schema(title = "saasId", description = "saasId")
    private long saasId;

    /**
     * UserId
     */
    @Schema(title = "userId", description = "userId")
    private long userId;

    /**
     * UserType
     */
    @Schema(title = "userType", description = "userType")
    private int userType;

    /**
     * UserInfo
     */
    @Schema(title = "userInfo", description = "userInfo")
    private String userInfo;

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
     * 窗口大小。
     */
    @Schema(title = "窗口大小", description = "窗口大小", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private int windowSize;

    /**
     * 工具信息。
     */
    @Schema(title = "工具列表", description = "工具列表", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<AiToolCallInfo> toolList;

    /**
     * rag知识库id列表。
     */
    @Schema(title = "rag知识库id列表", description = "rag知识库id列表", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private long[] ragLibIds;


    public AiChatSessionParam() {
    }

    private AiChatSessionParam(Builder builder) {
        setSaasId(builder.saasId);
        setUserId(builder.userId);
        setUserType(builder.userType);
        setUserInfo(builder.userInfo);
        setConfigId(builder.configId);
        setUserPrompt(builder.userPrompt);
        setSystemPrompt(builder.systemPrompt);
        setWindowSize(builder.windowSize);
        setToolList(builder.toolList);
        setRagLibIds(builder.ragLibIds);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(AiChatSessionParam copy) {
        Builder builder = new Builder();
        builder.saasId = copy.getSaasId();
        builder.userId = copy.getUserId();
        builder.userType = copy.getUserType();
        builder.userInfo = copy.getUserInfo();
        builder.configId = copy.getConfigId();
        builder.userPrompt = copy.getUserPrompt();
        builder.systemPrompt = copy.getSystemPrompt();
        builder.windowSize = copy.getWindowSize();
        builder.toolList = copy.getToolList();
        builder.ragLibIds = copy.getRagLibIds();
        return builder;
    }

    /**
     * 绑定授权信息。
     */
    public void bindAuthInfo(){
        this.saasId = AuthServiceHelper.getSaasId();
        this.userId = AuthServiceHelper.getUserId();
        this.userType = AuthServiceHelper.getUserType();
        this.userInfo = AuthServiceHelper.getUserName();
    }

    public long getSaasId() {
        return saasId;
    }

    public void setSaasId(long saasId) {
        this.saasId = saasId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
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

    public int getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    public List<AiToolCallInfo> getToolList() {
        return toolList;
    }

    public void setToolList(List<AiToolCallInfo> toolList) {
        this.toolList = toolList;
    }

    public long[] getRagLibIds() {
        return ragLibIds;
    }

    public void setRagLibIds(long[] ragLibIds) {
        this.ragLibIds = ragLibIds;
    }

    public static final class Builder {
        private long saasId;
        private long userId;
        private int userType;
        private String userInfo;
        private long configId;
        private String userPrompt;
        private String systemPrompt;
        private int windowSize;
        private List<AiToolCallInfo> toolList;
        private long[] ragLibIds;

        private Builder() {
        }

        /**
         * 绑定授权信息。
         */
        public Builder bindAuthInfo(){
            this.saasId = AuthServiceHelper.getSaasId();
            this.userId = AuthServiceHelper.getUserId();
            this.userType = AuthServiceHelper.getUserType();
            this.userInfo = AuthServiceHelper.getUserName();
            return this;
        }

        public Builder saasId(long saasId) {
            this.saasId = saasId;
            return this;
        }

        public Builder userId(long userId) {
            this.userId = userId;
            return this;
        }

        public Builder userType(int userType) {
            this.userType = userType;
            return this;
        }

        public Builder userInfo(String userInfo) {
            this.userInfo = userInfo;
            return this;
        }

        public Builder configId(long configId) {
            this.configId = configId;
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

        public Builder windowSize(int windowSize) {
            this.windowSize = windowSize;
            return this;
        }

        public Builder toolList(List<AiToolCallInfo> toolList) {
            this.toolList = toolList;
            return this;
        }

        public Builder ragLibIds(long[] ragLibIds) {
            this.ragLibIds = ragLibIds;
            return this;
        }

        public AiChatSessionParam build() {
            return new AiChatSessionParam(this);
        }
    }
}
