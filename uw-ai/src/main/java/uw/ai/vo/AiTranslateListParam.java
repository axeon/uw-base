package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

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

    public AiTranslateListParam(List<String> textList) {
        this.textList = textList;
    }

    private AiTranslateListParam(Builder builder) {
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
        private long configId;
        private String systemPrompt;
        private List<String> langList;
        private List<String> textList;

        private Builder() {
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
