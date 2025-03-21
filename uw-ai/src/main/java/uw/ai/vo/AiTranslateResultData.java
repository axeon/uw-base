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
}

