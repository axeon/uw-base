package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 删除滚动ID的响应结果
 * {
 * "succeeded": true,
 * "num_freed": 3
 * }
 *
 * @since 2019-12-16
 **/
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "DeleteScrollResponse", description = "删除滚动ID的响应结果")
public class DeleteScrollResponse {

    @Schema(title = "succeeded", description = "是否成功")
    @JsonProperty("succeeded")
    private boolean succeeded;

    @Schema(title = "numFreed", description = "删除的滚动ID数量")
    @JsonProperty("num_freed")
    private int numFreed;


    public boolean isSucceeded() {
        return succeeded;
    }

    public boolean getSucceeded() {
        return succeeded;
    }

    public void setSucceeded(boolean succeeded) {
        this.succeeded = succeeded;
    }

    public int getNumFreed() {
        return numFreed;
    }

    public void setNumFreed(int numFreed) {
        this.numFreed = numFreed;
    }
}
