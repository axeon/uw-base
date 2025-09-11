package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * 翻译结果数据。
 */
@Schema(title = "翻译结果数据", description = "翻译结果数据")
public class AiTranslateResultData {

    /**
     * 目标语言。
     */
    @Schema(title = "目标语言", description = "目标语言")
    private String lang;

    /**
     * 翻译结果。
     */
    @Schema(title = "翻译结果", description = "翻译结果。key是源语言，value是翻译结果。")
    private Map<String, String> resultMap;

    public AiTranslateResultData() {
    }

    public AiTranslateResultData(String lang, Map<String, String> resultMap) {
        this.lang = lang;
        this.resultMap = resultMap;
    }

    private AiTranslateResultData(Builder builder) {
        setLang(builder.lang);
        setResultMap(builder.resultMap);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(AiTranslateResultData copy) {
        Builder builder = new Builder();
        builder.lang = copy.getLang();
        builder.resultMap = copy.getResultMap();
        return builder;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public Map<String, String> getResultMap() {
        return resultMap;
    }

    public void setResultMap(Map<String, String> resultMap) {
        this.resultMap = resultMap;
    }

    public static final class Builder {
        private String lang;
        private Map<String, String> resultMap;

        private Builder() {
        }

        public Builder lang(String lang) {
            this.lang = lang;
            return this;
        }

        public Builder resultMap(Map<String, String> resultMap) {
            this.resultMap = resultMap;
            return this;
        }

        public AiTranslateResultData build() {
            return new AiTranslateResultData(this);
        }
    }
}

