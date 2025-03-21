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

    public List<String> getTextList() {
        return textList;
    }

    public void setTextList(List<String> textList) {
        this.textList = textList;
    }
}
