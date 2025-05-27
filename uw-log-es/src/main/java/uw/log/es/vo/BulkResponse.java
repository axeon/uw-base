package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 批量操作的响应结果。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "BulkResponse", description = "批量操作的响应结果")
public class BulkResponse {
    /**
     * 是否在批量操作过程中出现错误
     */
    @JsonProperty("errors")
    @Schema(title = "errors", description = "是否在批量操作过程中出现错误")
    private boolean errors;
    /**
     * 整个操作的耗时（毫秒）
     */
    @JsonProperty("took")
    @Schema(title = "took", description = "整个操作的耗时（毫秒）")
    private long took;

    public boolean isErrors() {
        return errors;
    }

    public void setErrors(boolean errors) {
        this.errors = errors;
    }

    public long getTook() {
        return took;
    }

    public void setTook(long took) {
        this.took = took;
    }
}
