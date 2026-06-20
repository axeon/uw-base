package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 删除滚动ID的响应结果。
 * <pre>
 * {
 *   "succeeded": true,
 *   "num_freed": 3
 * }
 * </pre>
 *
 * @since 2019-12-16
 **/
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "DeleteScrollResponse", description = "删除滚动ID的响应结果")
public class DeleteScrollResponse {

    /**
     * 是否成功删除。
     */
    @Schema(title = "succeeded", description = "是否成功")
    @JsonProperty("succeeded")
    private boolean succeeded;

    /**
     * 释放的滚动上下文数量。
     */
    @Schema(title = "numFreed", description = "删除的滚动ID数量")
    @JsonProperty("num_freed")
    private int numFreed;


    /**
     * 是否成功删除。
     *
     * @return 成功返回 true
     */
    public boolean isSucceeded() {
        return succeeded;
    }

    public void setSucceeded(boolean succeeded) {
        this.succeeded = succeeded;
    }

    /**
     * 获取释放的滚动上下文数量。
     *
     * @return 释放数量
     */
    public int getNumFreed() {
        return numFreed;
    }

    public void setNumFreed(int numFreed) {
        this.numFreed = numFreed;
    }
}
