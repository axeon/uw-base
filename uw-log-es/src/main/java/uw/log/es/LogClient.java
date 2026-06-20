package uw.log.es;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.data.PageList;
import uw.log.es.service.LogService;
import uw.log.es.vo.*;

import java.util.*;

/**
 * 日志接口服务客户端。
 * <p>
 * 对 Elasticsearch 的日志写入与查询能力的统一入口，封装了：
 * <ul>
 *   <li>批量写入（基于 bulk api，后台守护线程定时/按字节阈值 flush）；</li>
 *   <li>DSL 查询、SQL 转 DSL、scroll 游标查询；</li>
 *   <li>聚合结果与分页结果的映射工具方法。</li>
 * </ul>
 * 通过 {@link LogClientAutoConfiguration} 由 Spring 自动装配，业务侧一般通过 {@link #getInstance()} 获取全局单例。
 */
public class LogClient {

    private static final Logger logger = LoggerFactory.getLogger(LogClient.class);

    /**
     * 提供一个静态实例化对象，便于各种调用。
     * <p>由 {@link LogClientAutoConfiguration} 在构造时注入，业务侧通过 {@link #getInstance()} 读取。
     */
    private static volatile LogClient INSTANCE = null;

    /**
     * 内部的logService对象。
     */
    private final LogService logService;

    /**
     * 构造客户端实例，并将其登记为全局静态单例 {@link #INSTANCE}。
     *
     * @param logService 日志服务实现
     */
    public LogClient(final LogService logService) {
        this.logService = logService;
        INSTANCE = this;
    }

    /**
     * 获取全局唯一实例。
     *
     * @return 全局单例；若尚未通过 Spring 装配初始化则为 {@code null}
     */
    public static LogClient getInstance() {
        return INSTANCE;
    }

    /**
     * 将 Elasticsearch _search 响应转换为 {@link PageList} 分页对象。
     * <p>当响应缺失或无命中时返回空分页；命中总数缺失时以当前页命中条数近似。
     *
     * @param response  ES 搜索响应，可为 {@code null}
     * @param startIndex 分页起始偏移量（对应 ES 的 from）
     * @param pageSize   每页大小（对应 ES 的 size）
     * @param <T>        日志对象类型
     * @return 分页结果，永不为 {@code null}
     */
    public static <T> PageList<T> mapQueryResponseToPageList(SearchResponse<T> response, int startIndex, int pageSize) {
        ArrayList<T> list = new ArrayList<>();
        if (response != null) {
            SearchResponse.HitResponse<T> hitsResponse = response.getHitResponse();
            List<SearchResponse.Hit<T>> hitsList = hitsResponse.getHits();
            if (!hitsList.isEmpty()) {
                for (SearchResponse.Hit<T> hit : hitsList) {
                    list.add(hit.getSource());
                }

                if (Objects.nonNull(hitsResponse.getTotal())) {
                    return new PageList<>(list, startIndex, pageSize, (int) hitsResponse.getTotal().getValue());
                } else {
                    return new PageList<>(list, startIndex, pageSize, hitsResponse.getHits().size());
                }
            }
        }
        return new PageList<>(list, startIndex, pageSize, 0);
    }

    /**
     * 获取指定聚合名的单值（用于 sum/avg/max/min/value 等单值聚合）。
     * <p>聚合不存在或入参为空时返回 {@code 0d}。
     *
     * @param aggMap  聚合结果映射表，可为 {@code null}
     * @param aggName 聚合名称
     * @return 聚合单值；缺失时为 {@code 0d}
     */
    public static double getAggValue(Map<String, SearchResponse.Aggregation> aggMap, String aggName) {
        if (aggMap == null) return 0d;
        SearchResponse.Aggregation aggregation = aggMap.get(aggName);
        if (aggregation == null) return 0d;
        return aggregation.getValue();
    }

    /**
     * 将聚合结果转换为「聚合名 -> 桶列表」的结构。
     * <p>每个桶以子 Map 表示，固定包含 {@code name}(桶 key)与 {@code count}(文档数)，
     * 桶下的子聚合名值也会平铺进该子 Map。
     * 结构为 {@code agg -> [ {name, count, 子聚合名:值, ...} ]}。
     *
     * @param aggMap 聚合结果映射表，可为 {@code null}
     * @return 聚合名到桶列表的映射，永不为 {@code null}
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
     * 将「聚合 -> 桶 -> 子聚合」的三层结构拉平为嵌套 Map。
     * <p>对桶下的子聚合：若子聚合仍是桶型，则键为 {@code 子聚合名+桶key}，值为文档数；
     * 否则键为子聚合名，值为子聚合单值。
     * 结构为 {@code agg -> { 桶key -> { 子聚合名(+桶key): 值, ... } } }。
     *
     * @param aggMap 聚合结果映射表，可为 {@code null}
     * @return 三层嵌套映射，永不为 {@code null}
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
     * 将聚合结果拉平为「聚合名+桶key -> 值」的单层 Map。
     * <p>对桶型聚合，键为 {@code 聚合名+桶key}、值为文档数；对单值聚合，键为聚合名、值为单值。
     *
     * @param aggMap 聚合结果映射表，可为 {@code null}
     * @return 拉平后的映射，永不为 {@code null}
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
     * 注册日志类型，索引名由类名按 lower_underscore 规则自动推导。
     *
     * @param logClass 日志类
     */
    public void regLogObject(Class<?> logClass) {
        logService.regLogObject(logClass, null, null);
    }

    /**
     * 注册日志类型，使用自定义索引名。
     *
     * @param logClass 日志类
     * @param index    自定义索引名称
     */
    public void regLogObjectWithIndexName(Class<?> logClass, String index) {
        logService.regLogObject(logClass, index, null);
    }

    /**
     * 注册日志类型，并指定按时间滚动的索引模式（如 {@code yyyyMM}）。
     * <p>设置模式后，写入索引会追加时间后缀，查询索引会使用 {@code 原始名_*} 通配。
     *
     * @param logClass     日志类
     * @param indexPattern 索引模式（FastDateFormat 兼容格式）
     */
    public void regLogObjectWithIndexPattern(Class<?> logClass, String indexPattern) {
        logService.regLogObject(logClass, null, indexPattern);
    }

    /**
     * 注册日志类型，同时指定自定义索引名与时间滚动模式。
     *
     * @param logClass     日志类
     * @param index        自定义索引名称
     * @param indexPattern 索引模式（FastDateFormat 兼容格式）
     */
    public void regLogObjectWithIndexNameAndPattern(Class<?> logClass, String index, String indexPattern) {
        logService.regLogObject(logClass, index, indexPattern);
    }

    /**
     * 获取日志类型配置的原始索引名（不含时间后缀与通配符）。
     *
     * @param logClass 日志类
     * @return 原始索引名；未注册时返回 {@code null}
     */
    public String getRawIndexName(Class<?> logClass) {
        return logService.getRawIndexName(logClass);
    }

    /**
     * 获取带双引号的原始索引名（裸引号 {@code "xxx"}），用于构造写入 bulk 的 _index 等场景。
     *
     * @param logClass 日志类
     * @return 带引号的原始索引名；未注册时返回 {@code "null"}
     */

    public String getQuotedRawIndexName(Class<?> logClass) {
        return logService.getQuotedRawIndexName(logClass);
    }

    /**
     * 获取日志类型的查询索引名（设置模式时为 {@code 原始名_*} 通配形式）。
     *
     * @param logClass 日志类
     * @return 查询索引名；未注册时返回 {@code null}
     */
    public String getQueryIndexName(Class<?> logClass) {
        return logService.getQueryIndexName(logClass);
    }

    /**
     * 获取带转义引号的查询索引名（{@code \"xxx*\"}）。
     * <p>因 ES SQL 的 from 子句需被外层 JSON 字符串包裹，引号须以反斜杠转义。
     *
     * @param logClass 日志类
     * @return 带转义引号的查询索引名；未注册时返回 {@code \"null\"}
     */
    public String getQuotedQueryIndexName(Class<?> logClass) {
        return logService.getQuotedQueryIndexName(logClass);
    }

    /**
     * 写入单条日志。
     * <p>仅当 {@code logLevel > LogLevel.NONE} 时才真正写入；写入经 buffer 聚合后由后台线程批量提交。
     *
     * @param source 日志对象
     */
    public <T extends LogBaseVo> void log(T source) {
        if (source.getLogLevel() > LogLevel.NONE.getValue()) {
            logService.writeLog(source);
        }
    }

    /**
     * 批量写入日志。
     * <p>空列表或 {@code null} 直接返回；批量数据一次性写入 buffer，由后台线程统一 flush。
     *
     * @param sourceList 日志对象列表
     * @param <T>        日志对象类型
     */
    public <T extends LogBaseVo> void bulkLog(List<T> sourceList) {
        if (sourceList == null || sourceList.isEmpty()) {
            return;
        }
        logService.writeBulkLog(sourceList);
    }

    /**
     * 使用日志类型注册的查询索引执行 DSL 查询。
     *
     * @param tClass   日志对象类型
     * @param dslQuery dsl查询内容
     * @param <T>      日志对象类型
     * @return 搜索响应；ES 不可用或查询异常时返回 {@code null}
     */
    public <T> SearchResponse<T> dslQuery(Class<T> tClass, String dslQuery) {
        return logService.dslQuery(tClass, logService.getQueryIndexName(tClass), dslQuery);
    }

    /**
     * 在指定索引上执行 DSL 查询。
     *
     * @param tClass   日志对象类型
     * @param index    索引
     * @param dslQuery dsl查询内容
     * @param <T>      日志对象类型
     * @return 搜索响应；ES 不可用或查询异常时返回 {@code null}
     */
    public <T> SearchResponse<T> dslQuery(Class<T> tClass, String index, String dslQuery) {
        return logService.dslQuery(tClass, index, dslQuery);
    }

    /**
     * 将 SQL 转换为 ES DSL。
     * <p>SQL 中表名（索引名）需自行转义；SQL 不可包含 limit（由 {@code resultNum} 控制）。
     *
     * @param sql         SQL 语句，注意 index(tableName) 要进行转义
     * @param startIndex  分页起始偏移量（from）；{@code <=0} 时不附加 from 节点
     * @param resultNum   结果条数：{@code =0} 时由 ES 默认取 10 条，{@code >0} 时拼接 limit
     * @param isTrueCount 是否需要真实总数（附加 {@code track_total_hits:true}）
     * @return 转换后的 DSL 字符串
     */
    public String translateSqlToDsl(String sql, int startIndex, int resultNum, boolean isTrueCount) {
        return logService.translateSqlToDsl(sql, startIndex, resultNum, isTrueCount);
    }

    /**
     * 开启 scroll 游标查询。
     * <p>注：scroll 用的 DSL 中不能包含 from 节点。
     *
     * @param tClass              日志对象类型
     * @param index               索引
     * @param scrollExpireSeconds scroll api 过期时间（秒），{@code <=0} 时默认 60
     * @param dslQuery            dsl查询内容
     * @param <T>                 日志对象类型
     * @return scroll 响应，包含首批数据与 scrollId；错误时为 {@code null}
     */
    public <T> ScrollResponse<T> scrollQueryOpen(Class<T> tClass, String index, int scrollExpireSeconds, String dslQuery) {
        return logService.scrollQueryOpen(tClass, index, scrollExpireSeconds, dslQuery);
    }

    /**
     * 基于 scrollId 获取下一批数据。
     *
     * @param tClass              日志对象类型
     * @param index               索引（仅用于调用方辨识，实际不参与请求）
     * @param scrollId            上一次返回的 scrollId
     * @param scrollExpireSeconds scroll api 过期时间（秒），{@code <=0} 时默认 60
     * @param <T>                 日志对象类型
     * @return scroll 响应；错误时为 {@code null}
     */
    public <T> ScrollResponse<T> scrollQueryNext(Class<T> tClass, String index, String scrollId, int scrollExpireSeconds) {
        return logService.scrollQueryNext(tClass, scrollId, scrollExpireSeconds);
    }

    /**
     * 关闭 scroll 游标，释放 ES 端资源。
     *
     * @param scrollId 需删除的scrollId
     * @param index    索引（仅用于调用方辨识，实际不参与请求）
     * @return 删除响应；异常时返回一个 {@code succeeded=false} 的空响应
     */
    public DeleteScrollResponse scrollQueryClose(String scrollId, String index) {
        return logService.scrollQueryClose(scrollId);
    }

    /**
     * 关闭写日志系统，flush 剩余 buffer 并终止后台线程。
     */
    void destroy() {
        logService.destroy();
    }


}
