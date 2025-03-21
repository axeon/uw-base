package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 翻译列表基类。
 */
@Schema(title = "翻译列表基类", description = "翻译列表基类")
public abstract class AiTranslateBaseParam {

    /**
     * 配置ID。
     */
    @Schema(title = "配置ID", description = "配置ID")
    private long configId;

    /**
     * 系统提示。
     */
    @Schema(title = "系统提示", description = "系统提示")
    private String systemPrompt;

    /**
     * 目标语言。
     */
    @Schema(title = "目标语言列表", description = "目标语言列表")
    private List<String> langList;

    public long getConfigId() {
        return configId;
    }

    public void setConfigId(long configId) {
        this.configId = configId;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public List<String> getLangList() {
        return langList;
    }

    public void setLangList(List<String> langList) {
        this.langList = langList;
    }
}
