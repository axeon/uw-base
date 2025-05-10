package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch _search 接口返回结果的 VO 类
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "SearchResponse", description = "Elasticsearch _search 接口返回结果")
public class SearchResponse<T> {

    @JsonProperty("took")
    @Schema(title = "took", description = "请求执行所花费的时间，单位为毫秒")
    private long took;

    @JsonProperty("timed_out")
    @Schema(title = "timedOut", description = "请求是否超时")
    private boolean timedOut;

    @JsonProperty("_shards")
    @Schema(title = "_shards", description = "分片信息")
    private Shards shards;

    @JsonProperty("hits")
    @Schema(title = "hits", description = "搜索结果")
    private HitResponse<T> hitResponse = new HitResponse<>();

    @JsonProperty("aggregations")
    @Schema(title = "aggregations", description = "聚合结果")
    private Map<String, Aggregation> aggregations;

    public long getTook() {
        return took;
    }

    public void setTook(long took) {
        this.took = took;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    public Shards getShards() {
        return shards;
    }

    public void setShards(Shards shards) {
        this.shards = shards;
    }

    public HitResponse<T> getHitResponse() {
        return hitResponse;
    }

    public void setHitResponse(HitResponse<T> hitResponse) {
        this.hitResponse = hitResponse;
    }

    public Map<String, Aggregation> getAggregations() {
        return aggregations;
    }

    public void setAggregations(Map<String, Aggregation> aggregations) {
        this.aggregations = aggregations;
    }

    /**
     * 分片信息类
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(title = "Shards", description = "分片信息")
    public static class Shards {

        @JsonProperty("total")
        @Schema(title = "total", description = "总分片数")
        private int total;

        @JsonProperty("successful")
        @Schema(title = "successful", description = "成功分片数")
        private int successful;

        @JsonProperty("skipped")
        @Schema(title = "skipped", description = "跳过分片数")
        private int skipped;

        @JsonProperty("failed")
        @Schema(title = "failed", description = "失败分片数")
        private int failed;

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getSuccessful() {
            return successful;
        }

        public void setSuccessful(int successful) {
            this.successful = successful;
        }

        public int getSkipped() {
            return skipped;
        }

        public void setSkipped(int skipped) {
            this.skipped = skipped;
        }

        public int getFailed() {
            return failed;
        }

        public void setFailed(int failed) {
            this.failed = failed;
        }
    }

    /**
     * 搜索和主体类
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(title = "Hits", description = "搜索结果集合")
    public static class HitResponse<T> {

        @JsonProperty("total")
        @Schema(title = "total", description = "匹配文档的总数信息")
        private Total total;

        @JsonProperty("max_score")
        @Schema(title = "maxScore", description = "所有匹配文档中的最大分数")
        private Float maxScore;

        @JsonProperty("hits")
        @Schema(title = "hits", description = "匹配文档数组")
        private List<Hit<T>> hits = new ArrayList<>();

        public Total getTotal() {
            return total;
        }

        public void setTotal(Total total) {
            this.total = total;
        }

        public Float getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(Float maxScore) {
            this.maxScore = maxScore;
        }

        public List<Hit<T>> getHits() {
            return this.hits;
        }

        public void setHits(List<Hit<T>> hits) {
            this.hits = hits;
        }
    }

    /**
     * 文档总数类
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(title = "Total", description = "匹配文档的总数信息")
    public static class Total {

        @JsonProperty("value")
        @Schema(title = "value", description = "文档总数")
        private int value;

        @JsonProperty("relation")
        @Schema(title = "relation", description = "计数是否准确（eq 表示准确，gte 表示大于或等于）")
        private String relation;

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public String getRelation() {
            return relation;
        }

        public void setRelation(String relation) {
            this.relation = relation;
        }
    }

    /**
     * 单个匹配文档类
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(title = "Hit", description = "单个匹配文档")
    public static class Hit<T> {

        @JsonProperty("_index")
        @Schema(title = "_index", description = "文档所属的索引名")
        private String index;

        @JsonProperty("_type")
        @Schema(title = "_type", description = "文档类型")
        private String type;

        @JsonProperty("_id")
        @Schema(title = "_id", description = "文档 ID")
        private String id;

        @JsonProperty("_score")
        @Schema(title = "_score", description = "文档相关性分数")
        private Float score;

        @JsonProperty("_source")
        @Schema(title = "_source", description = "文档源内容")
        private T source;

        @JsonProperty("fields")
        @Schema(title = "fields", description = "文档字段内容")
        private Map<String, Object> fields;

        @JsonProperty("highlight")
        @Schema(title = "highlight", description = "文档高亮内容")
        private Map<String, List<String>> highlight;

        public String getIndex() {
            return index;
        }

        public void setIndex(String index) {
            this.index = index;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Float getScore() {
            return score;
        }

        public void setScore(Float score) {
            this.score = score;
        }

        public T getSource() {
            return source;
        }

        public void setSource(T source) {
            this.source = source;
        }
    }

    /**
     * 聚合结果基类
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(title = "Aggregation", description = "聚合结果")
    public static class Aggregation {

        @JsonProperty("doc_count_error_upper_bound")
        @Schema(title = "docCountErrorUpperBound", description = "文档计数错误上限")
        private int docCountErrorUpperBound;

        @JsonProperty("sum_other_doc_count")
        @Schema(title = "sumOtherDocCount", description = "其他文档计数")
        private int sumOtherDocCount;

        @JsonProperty("buckets")
        @Schema(title = "buckets", description = "桶数组，包含聚合分组信息")
        private List<Bucket> buckets;

        @JsonProperty("value")
        @Schema(title = "value", description = "聚合值（用于单值聚合如 sum、avg 等）")
        private double value;

        @JsonProperty("value_as_string")
        @Schema(title = "value_as_string", description = "value_as_string")
        private String valueAsString;

        @JsonProperty("count")
        @Schema(title = "count", description = "聚合值（用于单值聚合如 sum、avg 等）")
        private long count;

        @JsonProperty("min")
        @Schema(title = "min", description = "聚合值（用于单值聚合如 sum、avg 等）")
        private double min;

        @JsonProperty("max")
        @Schema(title = "max", description = "聚合值（用于单值聚合如 sum、avg 等）")
        private double max;

        @JsonProperty("avg")
        @Schema(title = "avg", description = "聚合值（用于单值聚合如 sum、avg 等）")
        private double avg;

        @JsonProperty("sum")
        @Schema(title = "sum", description = "聚合值（用于单值聚合如 sum、avg 等）")
        private double sum;


        public int getDocCountErrorUpperBound() {
            return docCountErrorUpperBound;
        }

        public void setDocCountErrorUpperBound(int docCountErrorUpperBound) {
            this.docCountErrorUpperBound = docCountErrorUpperBound;
        }

        public int getSumOtherDocCount() {
            return sumOtherDocCount;
        }

        public void setSumOtherDocCount(int sumOtherDocCount) {
            this.sumOtherDocCount = sumOtherDocCount;
        }

        public List<Bucket> getBuckets() {
            return buckets;
        }

        public void setBuckets(List<Bucket> buckets) {
            this.buckets = buckets;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public String getValueAsString() {
            return valueAsString;
        }

        public void setValueAsString(String valueAsString) {
            this.valueAsString = valueAsString;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public double getMin() {
            return min;
        }

        public void setMin(double min) {
            this.min = min;
        }

        public double getMax() {
            return max;
        }

        public void setMax(double max) {
            this.max = max;
        }

        public double getAvg() {
            return avg;
        }

        public void setAvg(double avg) {
            this.avg = avg;
        }

        public double getSum() {
            return sum;
        }

        public void setSum(double sum) {
            this.sum = sum;
        }
    }
    /**
     * 聚合分组桶类（用于 terms 聚合）
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(title = "Bucket", description = "聚合分组桶")
    public static class Bucket {

        @JsonProperty("key_as_string")
        @Schema(title = "key_as_string", description = "key_as_string")
        private String keyAsString;

        @JsonProperty("key")
        @Schema(title = "key", description = "桶的键值")
        private String key;

        @JsonProperty("doc_count")
        @Schema(title = "docCount", description = "桶中的文档数量")
        private int docCount;

        @JsonAnySetter
        @Schema(title = "aggregation", description = "嵌套聚合结果")
        private Map<String, Aggregation> subAggregations = new HashMap<>();

        public String getKeyAsString() {
            return keyAsString;
        }

        public void setKeyAsString(String keyAsString) {
            this.keyAsString = keyAsString;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public int getDocCount() {
            return docCount;
        }

        public void setDocCount(int docCount) {
            this.docCount = docCount;
        }

        public Map<String, Aggregation> getSubAggregations() {
            return subAggregations;
        }

        public void setSubAggregations(Map<String, Aggregation> subAggregations) {
            this.subAggregations = subAggregations;
        }
    }

}