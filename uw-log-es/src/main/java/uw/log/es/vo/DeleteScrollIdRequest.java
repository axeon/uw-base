package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 删除滚动ID的请求参数.
 * @since 2019-12-16
 **/
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "DeleteScrollIdRequest", description = "删除滚动ID的请求参数")
public class DeleteScrollIdRequest {

    @Schema(title = "滚动ID", description = "滚动ID")
    @JsonProperty("_scroll_id")
    private String scrollId;

    public DeleteScrollIdRequest(String scrollId) {
        this.scrollId = scrollId;
    }

    public String getScrollId() {
        return scrollId;
    }

    public void setScrollId(String scrollId) {
        this.scrollId = scrollId;
    }
}
