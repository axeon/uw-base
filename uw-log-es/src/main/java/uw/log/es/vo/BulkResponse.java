package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Elasticsearch bulk api 批量操作的响应结果。
 * <p>用于在 {@code processLogBuffer()} 提交后判断整体是否出错。
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

    /**
     * 是否在批量操作中出现错误。
     *
     * @return 出错返回 true
     */
    public boolean isErrors() {
        return errors;
    }

    public void setErrors(boolean errors) {
        this.errors = errors;
    }

    /**
     * 获取整个操作的耗时。
     *
     * @return 耗时（毫秒）
     */
    public long getTook() {
        return took;
    }

    public void setTook(long took) {
        this.took = took;
    }
}
