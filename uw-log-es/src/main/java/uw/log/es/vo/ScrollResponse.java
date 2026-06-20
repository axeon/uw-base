package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Elasticsearch scroll API 返回结果，在 {@link SearchResponse} 基础上追加游标 {@code _scroll_id}。
 * <p>调用方应保留 {@code scrollId} 用于 {@code scrollQueryNext} 拉取下一批，并在结束时 {@code scrollQueryClose}。
 *
 * @since 2019-12-16
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "ScrollResponse", description = "Elasticsearch _scroll 接口返回结果")
public class ScrollResponse<T> extends SearchResponse<T> {

    /**
     * 分页游标，用于获取下一批数据。
     */
    @Schema(title = "_scroll_id", description = "分页游标")
    @JsonProperty("_scroll_id")
    private String scrollId;

    /**
     * 获取分页游标。
     *
     * @return scrollId
     */
    public String getScrollId() {
        return this.scrollId;
    }

    /**
     * 设置分页游标。
     *
     * @param scrollId 游标
     */
    public void setScrollId(String scrollId) {
        this.scrollId = scrollId;
    }
}
