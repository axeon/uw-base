package uw.log.es;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.log.es.service.LogService;
import uw.log.es.vo.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 日志接口服务客户端
 */
public class LogClient {

    private static final Logger logger = LoggerFactory.getLogger( LogClient.class );

    /**
     * 提供一个静态实例化对象，便于各种调用。
     */
    private static LogClient logClient = null;

    /**
     * 内部的logService对象。
     */
    private LogService logService;

    /**
     * 包内构造器。
     *
     * @param logService
     */
    protected LogClient(final LogService logService) {
        this.logService = logService;
        logClient = this;
    }

    /**
     * 获得全局唯一实例。
     *
     * @return
     */
    public static LogClient getInstance() {
        return logClient;
    }

    /**
     * 注册日志类型
     *
     * @param logClass 日志类
     */
    public void regLogObject(Class<?> logClass) {
        logService.regLogObject( logClass, null, null );
    }

    /**
     * 注册日志类型
     *
     * @param logClass 日志类
     * @param index    自定义索引名称
     */
    public void regLogObjectWithIndexName(Class<?> logClass, String index) {
        logService.regLogObject( logClass, index, null );
    }

    /**
     * 注册日志类型
     *
     * @param logClass     日志类
     * @param indexPattern 索引模式
     */
    public void regLogObjectWithIndexPattern(Class<?> logClass, String indexPattern) {
        logService.regLogObject( logClass, null, indexPattern );
    }

    /**
     * 注册日志类型
     *
     * @param logClass     日志类
     * @param index        自定义索引名称
     * @param indexPattern 索引模式
     */
    public void regLogObjectWithIndexNameAndPattern(Class<?> logClass, String index, String indexPattern) {
        logService.regLogObject( logClass, index, indexPattern );
    }

    /**
     * 获取日志配置的索引名。
     *
     * @param logClass
     * @return
     */
    public String getRawIndexName(Class<?> logClass) {
        return logService.getRawIndexName( logClass );
    }

    /**
     * 获得带引号的索引名。
     *
     * @param logClass
     * @return
     */

    public String getQuotedRawIndexName(Class<?> logClass) {
        return logService.getQuotedRawIndexName( logClass );
    }


    /**
     * 获取日志的查询索引。
     *
     * @param logClass
     * @return
     */
    public String getQueryIndexName(Class<?> logClass) {
        return logService.getQueryIndexName( logClass );
    }

    /**
     * 获得带引号的查询索引名。
     *
     * @param logClass
     * @return
     */
    public String getQuotedQueryIndexName(Class<?> logClass) {
        return logService.getQuotedQueryIndexName( logClass );
    }

    /**
     * 写日志
     *
     * @param source 日志对象
     */
    public <T extends LogBaseVo> void log(T source) {
        if (source.getLogLevel() > LogLevel.NONE.getValue()) {
            logService.writeLog( source );
        }
    }

    /**
     * 批量写日志
     *
     * @param sourceList 日志对象列表
     * @param <T>
     */
    public <T extends LogBaseVo> void bulkLog(List<T> sourceList) {
        logService.writeBulkLog( sourceList );
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
        return logService.dslQuery( tClass, logService.getQueryIndexName( tClass ), dslQuery );
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
        return logService.dslQuery( tClass, index, dslQuery );
    }

    /**
     * 转换Sql 成 DSL
     *
     * @param sql         注意index(tableName)要进行转义
     * @param resultNum   =0时 es会有默认值 10，>0 limit 拼接
     * @param isTrueCount 是否需要真实的总数
     * @return
     */
    public String translateSqlToDsl(String sql, int startIndex, int resultNum, boolean isTrueCount) throws Exception {
        return logService.translateSqlToDsl( sql, startIndex, resultNum, isTrueCount );
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
        return logService.scrollQueryOpen( tClass, index, scrollExpireSeconds, dslQuery );
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
        return logService.scrollQueryNext( tClass, scrollId, scrollExpireSeconds );
    }

    /**
     * 关闭scroll api 查询
     *
     * @param scrollId 需删除的scrollId
     * @param index    索引 非必要的 只是让使用者能清楚删除的scrollId index
     * @return
     */
    public DeleteScrollResponse scrollQueryClose(String scrollId, String index) {
        return logService.scrollQueryClose( scrollId );
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
    public <T> ESDataList<T> mapQueryResponseToEDataList(SearchResponse<T> response, int startIndex, int pageSize) {
        List<T> dataList = new ArrayList<>();
        if (response != null) {
            SearchResponse.HitsResponse<T> hitsResponse = response.getHitsResponse();
            List<SearchResponse.Hits<T>> hitsList = hitsResponse.getHits();
            if (!hitsList.isEmpty()) {
                for (SearchResponse.Hits<T> hits : hitsList) {
                    dataList.add( hits.getSource() );
                }

                if (Objects.nonNull( hitsResponse.getTotal() )) {
                    return new ESDataList<>( dataList, startIndex, pageSize, hitsResponse.getTotal().getValue() );
                } else {
                    return new ESDataList<>( dataList, startIndex, pageSize, hitsResponse.getHits().size() );
                }
            }
        }
        return new ESDataList<>( dataList, startIndex, pageSize, 0 );
    }

    /**
     * 关闭写日志系统
     */
    @PreDestroy
    void destroy() {
        logService.destroyLog();
    }


}
