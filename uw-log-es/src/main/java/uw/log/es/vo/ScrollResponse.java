package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Scroll API result
 *
 * 
 * @since 2019-12-16
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScrollResponse<T> extends SearchResponse<T> {

    /**
     * 分页游标
     */
    private String scrollId;


    public void setScrollId(String scrollId) {
        this.scrollId = scrollId;
    }

    @JsonProperty("_scroll_id")
    public String getScrollId() {
        return this.scrollId;
    }
}
