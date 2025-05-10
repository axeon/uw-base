package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Scroll API result
 *
 * @since 2019-12-16
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "ScrollResponse", description = "Elasticsearch _scroll 接口返回结果")
public class ScrollResponse<T> extends SearchResponse<T> {

    /**
     * 分页游标
     */
    @Schema(title = "_scroll_id", description = "分页游标")
    @JsonProperty("_scroll_id")
    private String scrollId;

    public String getScrollId() {
        return this.scrollId;
    }

    public void setScrollId(String scrollId) {
        this.scrollId = scrollId;
    }
}
