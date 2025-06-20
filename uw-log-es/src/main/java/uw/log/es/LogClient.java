package uw.log.es;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.log.es.service.LogService;
import uw.log.es.vo.*;

import java.util.*;

/**
 * 日志接口服务客户端
 */
public class LogClient {

    private static final Logger logger = LoggerFactory.getLogger(LogClient.class);

    /**
     * 提供一个静态实例化对象，便于各种调用。
     */
    private static LogClient logClient = null;

    /**
     * 内部的logService对象。
     */
    private final LogService logService;

    /**
     * 包内构造器。
     *
     * @param logService
     */
    public LogClient(final LogService logService) {
        this.logService = logService;
        logClient = this;
    }

    /**
     * 获取全局唯一实例。
     *
     * @return
     */
    public static LogClient getInstance() {
        return logClient;
    }

    /**
     * 转换成分页形式
     *
     * @param response
     * @param startIndex
     * @param pageSize
     * @param <T>
     * @return
     */
    public static <T> ESDataList<T> mapQueryResponseToEDataList(SearchResponse<T> response, int startIndex, int pageSize) {
        ArrayList<T> dataList = new ArrayList<>();
        if (response != null) {
            SearchResponse.HitResponse<T> hitsResponse = response.getHitResponse();
            List<SearchResponse.Hit<T>> hitsList = hitsResponse.getHits();
            if (!hitsList.isEmpty()) {
                for (SearchResponse.Hit<T> hit : hitsList) {
                    dataList.add(hit.getSource());
                }

                if (Objects.nonNull(hitsResponse.getTotal())) {
                    return new ESDataList<>(dataList, startIndex, pageSize, hitsResponse.getTotal().getValue());
                } else {
                    return new ESDataList<>(dataList, startIndex, pageSize, hitsResponse.getHits().size());
                }
            }
        }
        return new ESDataList<>(dataList, startIndex, pageSize, 0);
    }

    /**
     * 获取聚合值。
     */
    public static double getAggValue(Map<String, SearchResponse.Aggregation> aggMap, String aggName) {
        if (aggMap == null) return 0d;
        SearchResponse.Aggregation aggregation = aggMap.get(aggName);
        if (aggregation == null) return 0d;
        return aggregation.getValue();
    }

    /**
     * 获取聚合值。
     * 聚合值下方的Bucket，则放入到子map中。
     * 结构为agg->bucket。
     */
    public static Map<String, List<Map<String, Object>>> convertAggBucketListMap(Map<String, SearchResponse.Aggregation> aggMap) {
        Map<String, List<Map<String, Object>>> map = new LinkedHashMap<>();
        if (aggMap == null) return map;
        aggMap.forEach((k, v) -> {
            List<Map<String, Object>> list = new ArrayList<>();
            if (v.getBuckets() != null) {
                v.getBuckets().forEach(bucket -> {
                    Map<String, Object> subMap = new LinkedHashMap<>();
                    subMap.put("name", bucket.getKey());
                    subMap.put("count", bucket.getDocCount());
                    if (bucket.getSubAggregations() != null) {
                        bucket.getSubAggregations().forEach((k1, v1) -> {
                            subMap.put(k1, v1.getValue());
                        });
                    }
                    list.add(subMap);
                });
            }
            map.put(k, list);
        });
        return map;
    }

    /**
     * 获取聚合值。
     * 聚合值中如果有bucket，则将bucket中的值拉平加入到map中。
     * 结构为agg->bucket->agg+bucket。
     */
    public static Map<String, Map<String, Map<String, Double>>> convertAggBucketAggBucketFlatMap(Map<String, SearchResponse.Aggregation> aggMap) {
        Map<String, Map<String, Map<String, Double>>> map = new LinkedHashMap<>();
        if (aggMap == null) return map;
        aggMap.forEach((k, v) -> {
            if (v.getBuckets() != null) {
                Map<String, Map<String, Double>> subMap = new LinkedHashMap<>();
                v.getBuckets().forEach(bucket -> {
                    if (bucket.getSubAggregations() != null) {
                        Map<String, Double> subSubMap = new LinkedHashMap<>();
                        bucket.getSubAggregations().forEach((k1, v1) -> {
                            if (v1.getBuckets() != null) {
                                v1.getBuckets().forEach(bucket2 -> {
                                    subSubMap.put(k1 + bucket2.getKey(), (double) bucket2.getDocCount());
                                });
                            } else {
                                subSubMap.put(k1, v1.getValue());
                            }
                        });
                        subMap.put(bucket.getKey(), subSubMap);
                    }
                });
                map.put(k, subMap);
            }
        });
        return map;
    }

    /**
     * 获取聚合值。
     * 聚合值中如果有bucket，则将bucket中的值拉平加入到map中。
     * 结构为agg+bucket。
     */
    public static Map<String, Double> convertAggBucketFlatMap(Map<String, SearchResponse.Aggregation> aggMap) {
        Map<String, Double> map = new LinkedHashMap<>();
        if (aggMap == null) return map;
        aggMap.forEach((k, v) -> {
            if (v.getBuckets() != null) {
                v.getBuckets().forEach(bucket -> {
                    map.put(k + bucket.getKey(), (double) bucket.getDocCount());
                });
            } else {
                map.put(k, v.getValue());
            }
        });
        return map;
    }

    /**
     * 注册日志类型
     *
     * @param logClass 日志类
     */
    public void regLogObject(Class<?> logClass) {
        logService.regLogObject(logClass, null, null);
    }

    /**
     * 注册日志类型
     *
     * @param logClass 日志类
     * @param index    自定义索引名称
     */
    public void regLogObjectWithIndexName(Class<?> logClass, String index) {
        logService.regLogObject(logClass, index, null);
    }

    /**
     * 注册日志类型
     *
     * @param logClass     日志类
     * @param indexPattern 索引模式
     */
    public void regLogObjectWithIndexPattern(Class<?> logClass, String indexPattern) {
        logService.regLogObject(logClass, null, indexPattern);
    }

    /**
     * 注册日志类型
     *
     * @param logClass     日志类
     * @param index        自定义索引名称
     * @param indexPattern 索引模式
     */
    public void regLogObjectWithIndexNameAndPattern(Class<?> logClass, String index, String indexPattern) {
        logService.regLogObject(logClass, index, indexPattern);
    }

    /**
     * 获取日志配置的索引名。
     *
     * @param logClass
     * @return
     */
    public String getRawIndexName(Class<?> logClass) {
        return logService.getRawIndexName(logClass);
    }

    /**
     * 获取带引号的索引名。
     *
     * @param logClass
     * @return
     */

    public String getQuotedRawIndexName(Class<?> logClass) {
        return logService.getQuotedRawIndexName(logClass);
    }

    /**
     * 获取日志的查询索引。
     *
     * @param logClass
     * @return
     */
    public String getQueryIndexName(Class<?> logClass) {
        return logService.getQueryIndexName(logClass);
    }

    /**
     * 获取带引号的查询索引名。
     *
     * @param logClass
     * @return
     */
    public String getQuotedQueryIndexName(Class<?> logClass) {
        return logService.getQuotedQueryIndexName(logClass);
    }

    /**
     * 写日志
     *
     * @param source 日志对象
     */
    public <T extends LogBaseVo> void log(T source) {
        if (source.getLogLevel() > LogLevel.NONE.getValue()) {
            logService.writeLog(source);
        }
    }

    /**
     * 批量写日志
     *
     * @param sourceList 日志对象列表
     * @param <T>
     */
    public <T extends LogBaseVo> void bulkLog(List<T> sourceList) {
        logService.writeBulkLog(sourceList);
    }

    /**
     * dsl日志查询
     *
     * @param tClass   日志对象类型
     * @param dslQuery dsl查询内容
     * @param <T>
     * @return
     */
    public <T> SearchResponse<T> dslQuery(Class<T> tClass, String dslQuery) {
        return logService.dslQuery(tClass, logService.getQueryIndexName(tClass), dslQuery);
    }

    /**
     * dsl日志查询
     *
     * @param tClass   日志对象类型
     * @param index    索引
     * @param dslQuery dsl查询内容
     * @param <T>
     * @return
     */
    public <T> SearchResponse<T> dslQuery(Class<T> tClass, String index, String dslQuery) {
        return logService.dslQuery(tClass, index, dslQuery);
    }

    /**
     * 转换Sql 成 DSL
     *
     * @param sql         注意index(tableName)要进行转义
     * @param resultNum   =0时 es会有默认值 10，>0 limit 拼接
     * @param isTrueCount 是否需要真实的总数
     * @return
     */
    public String translateSqlToDsl(String sql, int startIndex, int resultNum, boolean isTrueCount) {
        return logService.translateSqlToDsl(sql, startIndex, resultNum, isTrueCount);
    }

    /**
     * scroll查询
     * 注：scroll dsl不能含有from节点
     *
     * @param tClass              日志对象类型
     * @param index               索引
     * @param scrollExpireSeconds scroll api 过期时间
     * @param <T>
     * @return 错误时为空
     */
    public <T> ScrollResponse<T> scrollQueryOpen(Class<T> tClass, String index, int scrollExpireSeconds, String dslQuery) {
        return logService.scrollQueryOpen(tClass, index, scrollExpireSeconds, dslQuery);
    }

    /**
     * scroll查询 当获取到scrollId后
     *
     * @param <T>
     * @param tClass              日志对象类型
     * @param index               索引 非必要的 只是让使用者能清楚的scrollId index
     * @param scrollExpireSeconds scroll api 过期时间
     * @return 错误时为空
     */
    public <T> ScrollResponse<T> scrollQueryNext(Class<T> tClass, String index, String scrollId, int scrollExpireSeconds) {
        return logService.scrollQueryNext(tClass, scrollId, scrollExpireSeconds);
    }

    /**
     * 关闭scroll api 查询
     *
     * @param scrollId 需删除的scrollId
     * @param index    索引 非必要的 只是让使用者能清楚删除的scrollId index
     * @return
     */
    public DeleteScrollResponse scrollQueryClose(String scrollId, String index) {
        return logService.scrollQueryClose(scrollId);
    }

    /**
     * 关闭写日志系统
     */
    void destroy() {
        logService.destroy();
    }


}
