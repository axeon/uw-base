package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.auth.service.AuthServiceHelper;

/**
 * AI图片生成参数。
 */
@Schema(title = "AI图片生成参数", description = "AI图片生成参数")
public class AiImageGenerateParam {

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
     * 会话ID，若大于0则保存到指定会话，否则自动创建新会话
     */
    @Schema(title = "会话ID", description = "会话ID，若大于0则保存到指定会话，否则自动创建新会话")
    private long sessionId;

    /**
     * 图片提示词
     */
    @Schema(title = "图片提示词", description = "图片提示词", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userPrompt;

    public AiImageGenerateParam() {
    }

    private AiImageGenerateParam(Builder builder) {
        setSaasId(builder.saasId);
        setUserId(builder.userId);
        setUserType(builder.userType);
        setUserInfo(builder.userInfo);
        setConfigId(builder.configId);
        setSessionId(builder.sessionId);
        setUserPrompt(builder.prompt);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(AiImageGenerateParam copy) {
        Builder builder = new Builder();
        builder.saasId = copy.getSaasId();
        builder.userId = copy.getUserId();
        builder.userType = copy.getUserType();
        builder.userInfo = copy.getUserInfo();
        builder.configId = copy.getConfigId();
        builder.sessionId = copy.getSessionId();
        builder.prompt = copy.getUserPrompt();
        return builder;
    }

    /**
     * 绑定授权信息。
     */
    public void bindAuthInfo() {
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

    public static final class Builder {
        private long saasId;
        private long userId;
        private int userType;
        private String userInfo;
        private long configId;
        private long sessionId;
        private String prompt;

        private Builder() {
        }

        /**
         * 绑定授权信息。
         */
        public Builder bindAuthInfo() {
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

        public Builder sessionId(long sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public AiImageGenerateParam build() {
            return new AiImageGenerateParam(this);
        }
    }
}
