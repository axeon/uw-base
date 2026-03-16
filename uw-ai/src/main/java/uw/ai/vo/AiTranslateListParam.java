package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.auth.service.AuthServiceHelper;

import java.util.List;

/**
 * 翻译列表参数。
 */
@Schema(title = "翻译列表参数", description = "翻译列表参数")
public class AiTranslateListParam extends AiTranslateBaseParam {

    /**
     * 待翻译的文本列表。
     */
    @Schema(title = "待翻译的文本列表", description = "待翻译的文本列表")
    private List<String> textList;

    public AiTranslateListParam() {
    }

    private AiTranslateListParam(Builder builder) {
        setSaasId(builder.saasId);
        setUserId(builder.userId);
        setUserType(builder.userType);
        setUserInfo(builder.userInfo);
        setConfigId(builder.configId);
        setSystemPrompt(builder.systemPrompt);
        setLangList(builder.langList);
        setTextList(builder.textList);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(AiTranslateListParam copy) {
        Builder builder = new Builder();
        builder.saasId = copy.getSaasId();
        builder.userId = copy.getUserId();
        builder.userType = copy.getUserType();
        builder.userInfo = copy.getUserInfo();
        builder.configId = copy.getConfigId();
        builder.systemPrompt = copy.getSystemPrompt();
        builder.langList = copy.getLangList();
        builder.textList = copy.getTextList();
        return builder;
    }


    public List<String> getTextList() {
        return textList;
    }

    public void setTextList(List<String> textList) {
        this.textList = textList;
    }

    public static final class Builder {
        private long saasId;
        private long userId;
        private int userType;
        private String userInfo;
        private long configId;
        private String systemPrompt;
        private List<String> langList;
        private List<String> textList;

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

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder langList(List<String> langList) {
            this.langList = langList;
            return this;
        }

        public Builder textList(List<String> textList) {
            this.textList = textList;
            return this;
        }

        public AiTranslateListParam build() {
            return new AiTranslateListParam(this);
        }
    }
}
