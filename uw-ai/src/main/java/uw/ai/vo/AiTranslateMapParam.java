package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.LinkedHashMap;

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

    public LinkedHashMap<String, String> getTextMap() {
        return textMap;
    }

    public void setTextMap(LinkedHashMap<String, String> textMap) {
        this.textMap = textMap;
    }
}
