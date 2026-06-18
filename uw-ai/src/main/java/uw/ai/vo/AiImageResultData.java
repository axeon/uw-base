package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * AI图片生成结果数据。
 */
@Schema(title = "AI图片生成结果数据", description = "AI图片生成结果数据")
public class AiImageResultData {

    /**
     * 会话ID
     */
    @Schema(title = "会话ID", description = "会话ID")
    private long sessionId;

    /**
     * 图片URL列表
     */
    @Schema(title = "图片URL列表", description = "图片URL列表")
    private List<String> imageUrlList;


    public AiImageResultData() {
    }

    public List<String> getImageUrlList() {
        return imageUrlList;
    }

    public void setImageUrlList(List<String> imageUrlList) {
        this.imageUrlList = imageUrlList;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }
}
