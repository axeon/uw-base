package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * 翻译列表参数。
 */
@Schema(title = "翻译列表参数", description = "翻译列表参数")
public class AiTranslateMapParam extends AiTranslateBaseParam {

    /**
     * 待翻译的文本Map。
     */
    @Schema(title = "待翻译的文本Map", description = "待翻译的文本Map。key是变量名，value是要翻译的文本")
    private LinkedHashMap<String,String> textMap;

    public AiTranslateMapParam() {
    }

    public AiTranslateMapParam(LinkedHashMap<String, String> textMap) {
        this.textMap = textMap;
    }

    private AiTranslateMapParam(Builder builder) {
        setConfigId(builder.configId);
        setSystemPrompt(builder.systemPrompt);
        setLangList(builder.langList);
        setTextMap(builder.textMap);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(AiTranslateMapParam copy) {
        Builder builder = new Builder();
        builder.configId = copy.getConfigId();
        builder.systemPrompt = copy.getSystemPrompt();
        builder.langList = copy.getLangList();
        builder.textMap = copy.getTextMap();
        return builder;
    }

    public LinkedHashMap<String, String> getTextMap() {
        return textMap;
    }

    public void setTextMap(LinkedHashMap<String, String> textMap) {
        this.textMap = textMap;
    }

    public static final class Builder {
        private long configId;
        private String systemPrompt;
        private List<String> langList;
        private LinkedHashMap<String, String> textMap;

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

        public Builder textMap(LinkedHashMap<String, String> textMap) {
            this.textMap = textMap;
            return this;
        }

        public AiTranslateMapParam build() {
            return new AiTranslateMapParam(this);
        }
    }
}
