package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @since 2019-12-16
 **/
public class DeleteScrollIdRequest {

    public DeleteScrollIdRequest(String scrollId) {
        this.scrollId = scrollId;
    }

    private String scrollId;

    @JsonProperty("_scroll_id")
    public String getScrollId() {
        return scrollId;
    }

    public void setScrollId(String scrollId) {
        this.scrollId = scrollId;
    }
}
