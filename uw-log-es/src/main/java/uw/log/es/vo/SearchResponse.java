package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 查询返回的Response
 *
 * @since 2018-04-25
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResponse<T> {

    /**
     *
     */
    private Shards shards = new Shards();

    /**
     *
     */
    private HitsResponse<T> hitsResponse = new HitsResponse<T>();

    /**
     * 聚合结果
     */
    private LinkedHashMap<String, AggregationResult> aggregations;

    /**
     * false
     */
    private boolean timedOut;

    /**
     * 1
     */
    private int took;

    @JsonProperty("_shards")
    public Shards getShards() {
        return this.shards;
    }

    public void setShards(Shards shards) {
        this.shards = shards;
    }

    public void setHisResponse(HitsResponse<T> hitsResponse) {
        this.hitsResponse = hitsResponse;
    }

    @JsonProperty("hits")
    public HitsResponse<T> getHitsResponse() {
        return this.hitsResponse;
    }

    @JsonProperty("aggregations")
    public LinkedHashMap<String, AggregationResult> getAggregations() {
        return aggregations;
    }

    public void setAggregations(LinkedHashMap<String, AggregationResult> aggregations) {
        this.aggregations = aggregations;
    }

    @JsonProperty("timed_out")
    public boolean getTimedOut() {
        return this.timedOut;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    @JsonProperty("took")
    public int getTook() {
        return this.took;
    }

    public void setTook(int took) {
        this.took = took;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Shards {
        /**
         * 5
         */
        private String total;

        /**
         * 0
         */
        private String failed;

        /**
         * 5
         */
        private String successful;

        /**
         * 0
         */
        private String skipped;

        @JsonProperty("total")
        public String getTotal() {
            return this.total;
        }

        public void setTotal(String total) {
            this.total = total;
        }

        @JsonProperty("failed")
        public String getFailed() {
            return this.failed;
        }

        public void setFailed(String failed) {
            this.failed = failed;
        }

        @JsonProperty("successful")
        public String getSuccessful() {
            return this.successful;
        }

        public void setSuccessful(String successful) {
            this.successful = successful;
        }

        @JsonProperty("skipped")
        public String getSkipped() {
            return this.skipped;
        }

        public void setSkipped(String skipped) {
            this.skipped = skipped;
        }

    }


    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AggregationResult<T> {
        private int docCountErrorUpperBound;

        private int sumOtherDocCount;

        private BucketInfo[] buckets;

        @JsonProperty("doc_count_error_upper_bound")
        public int getDocCountErrorUpperBound() {
            return docCountErrorUpperBound;
        }

        public void setDocCountErrorUpperBound(int docCountErrorUpperBound) {
            this.docCountErrorUpperBound = docCountErrorUpperBound;
        }

        @JsonProperty("sum_other_doc_count")
        public int getSumOtherDocCount() {
            return sumOtherDocCount;
        }

        public void setSumOtherDocCount(int sumOtherDocCount) {
            this.sumOtherDocCount = sumOtherDocCount;
        }

        @JsonProperty("buckets")
        public BucketInfo[] getBuckets() {
            return buckets;
        }

        public void setBuckets(BucketInfo[] buckets) {
            this.buckets = buckets;
        }

        public static class BucketInfo {
            String key;
            int docCount;

            @JsonProperty("key")
            public String getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = key;
            }

            @JsonProperty("doc_count")
            public int getDocCount() {
                return docCount;
            }

            public void setDocCount(int docCount) {
                this.docCount = docCount;
            }
        }

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hits<T> {
        /**
         * 索引
         */
        private String index;

        /**
         * 类型
         */
        private String type;

        /**
         * 记录
         */
        private T source;

        /**
         * 主键
         */
        private String id;

        /**
         * 1.0
         */
        private String score;

        @JsonProperty("_index")
        public String getIndex() {
            return this.index;
        }

        public void setIndex(String index) {
            this.index = index;
        }

        @JsonProperty("_type")
        public String getType() {
            return this.type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @JsonProperty("_source")
        public T getSource() {
            return this.source;
        }

        public void setSource(T source) {
            this.source = source;
        }

        @JsonProperty("_id")
        public String getId() {
            return this.id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @JsonProperty("_score")
        public String getScore() {
            return this.score;
        }

        public void setScore(String score) {
            this.score = score;
        }

    }

    /**
     * 查询结果集
     *
     * @param <T>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HitsResponse<T> {
        /**
         *
         */
        private List<Hits<T>> hits = new ArrayList<Hits<T>>();

        /**
         * 1
         */
        private Total total;

        /**
         * 1.0
         */
        private String maxScore;

        @JsonProperty("hits")
        public List<Hits<T>> getHits() {
            return this.hits;
        }

        public void setHits(List<Hits<T>> hits) {
            this.hits = hits;
        }

        @JsonProperty("total")
        public Total getTotal() {
            return this.total;
        }

        public void setTotal(Total total) {
            this.total = total;
        }

        public void getMaxScore(String maxScore) {
            this.maxScore = maxScore;
        }

        @JsonProperty("max_score")
        public String getMaxScore() {
            return this.maxScore;
        }
    }
}
