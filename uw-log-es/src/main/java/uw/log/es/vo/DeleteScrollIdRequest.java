package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 删除滚动ID的请求参数。
 * <p>对应 ES 关闭 scroll 请求体中的 {@code _scroll_id} 字段。
 *
 * @since 2019-12-16
 **/
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "DeleteScrollIdRequest", description = "删除滚动ID的请求参数")
public class DeleteScrollIdRequest {

    /**
     * 待删除的滚动ID。
     */
    @Schema(title = "滚动ID", description = "滚动ID")
    @JsonProperty("_scroll_id")
    private String scrollId;

    /**
     * 构造请求参数。
     *
     * @param scrollId 滚动ID
     */
    public DeleteScrollIdRequest(String scrollId) {
        this.scrollId = scrollId;
    }

    /**
     * 获取滚动ID。
     *
     * @return 滚动ID
     */
    public String getScrollId() {
        return scrollId;
    }

    /**
     * 设置滚动ID。
     *
     * @param scrollId 滚动ID
     */
    public void setScrollId(String scrollId) {
        this.scrollId = scrollId;
    }
}
