package uw.log.es.service;

import com.fasterxml.jackson.databind.JavaType;
import com.google.common.base.CaseFormat;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.httpclient.http.HttpConfig;
import uw.httpclient.http.HttpInterface;
import uw.httpclient.json.JsonInterfaceHelper;
import uw.httpclient.util.BufferRequestBody;
import uw.httpclient.util.MediaTypes;
import uw.log.es.LogClientProperties;
import uw.log.es.util.IndexConfigVo;
import uw.log.es.vo.DeleteScrollResponse;
import uw.log.es.vo.LogBaseVo;
import uw.log.es.vo.ScrollResponse;
import uw.log.es.vo.SearchResponse;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 日志服务
 */
public class LogService {

    private static final Logger log = LoggerFactory.getLogger( LogService.class );


    /**
     * 日志编码
     */
    private static final Charset LOG_CHARSET = StandardCharsets.UTF_8;

    /**
     * 换行符字节
     */
    private static final byte[] LINE_SEPARATOR_BYTES = System.lineSeparator().getBytes( LOG_CHARSET );

    /**
     * 时间序列格式化
     */
    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance( "yyyy-MM-dd'T'HH:mm:ss.SSSZZ", (TimeZone) null );

    /**
     * scroll api
     */
    private static final String SCROLL = "scroll";

    /**
     * 读写锁
     */
    private final Lock batchLock = new ReentrantLock();

    /**
     * 注册Mapping,<Class<?>,String>
     */
    private final Map<Class<?>, IndexConfigVo> regMap = new HashMap<>();

    /**
     * httpInterface
     */
    private HttpInterface httpInterface;

    /**
     * es集群地址
     */
    private final String server;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 是否需要记录日志
     */
    private boolean logState;

    /**
     * 是否需要Http Basic验证头
     */
    private boolean needBasicAuth;

    /**
     * Elasticsearch bulk api 地址
     */
    private String esBulk;

    /**
     * 刷新Bucket时间毫秒数
     */
    private long maxFlushInMilliseconds;

    /**
     * 允许最大Bucket字节数
     */
    private long maxBytesOfBatch;

    /**
     * 最大批量线程数
     */
    private int maxBatchThreads;

    /**
     * 最大批量线程队列数
     */
    private int maxBatchQueueSize;

    /**
     * buffer
     */
    private okio.Buffer buffer = new okio.Buffer();

    /**
     * 后台线程
     */
    private ElasticsearchDaemonExporter daemonExporter;

    /**
     * 后台批量线程池。
     */
    private ThreadPoolExecutor batchExecutor;

    /**
     * 应用名称
     */
    private final String appName;

    /**
     * 应用主机信息
     */
    private final String appHost;

    /**
     * 是否添加执行应用信息
     */
    private final boolean appInfoOverwrite;

    /**
     * LogService.
     *
     * @param logClientProperties 配置器
     * @param appName             应用名称
     * @param appHost             应用主机信息
     */
    public LogService(final LogClientProperties logClientProperties, final String appName, final String appHost) {
        this.appName = appName;
        this.appHost = appHost;
        this.appInfoOverwrite = logClientProperties.getEs().isAppInfoOverwrite();
        this.server = logClientProperties.getEs().getServer();
        if (StringUtils.isBlank( this.server )) {
            log.error( "ElasticSearch server config is null! LogClient can't log anything!!!" );
            this.logState = false;
            return;
        }

        this.username = logClientProperties.getEs().getUsername();
        this.password = logClientProperties.getEs().getPassword();
        this.needBasicAuth = StringUtils.isNotBlank( username ) && StringUtils.isNotBlank( password );
        this.httpInterface =
                new JsonInterfaceHelper( HttpConfig.builder().retryOnConnectionFailure( true ).connectTimeout( logClientProperties.getEs().getConnectTimeout() ).readTimeout( logClientProperties.getEs().getReadTimeout() ).writeTimeout( logClientProperties.getEs().getWriteTimeout() ).build() );
        this.esBulk = logClientProperties.getEs().getEsBulk();
        this.maxFlushInMilliseconds = logClientProperties.getEs().getMaxFlushInSeconds()*1000L;
        this.maxBytesOfBatch = logClientProperties.getEs().getMaxKiloBytesOfBatch()*1024L;
        this.maxBatchThreads = logClientProperties.getEs().getMaxBatchThreads();
        this.maxBatchQueueSize = logClientProperties.getEs().getMaxBatchQueueSize();
        /**
         * 模式
         */
        LogClientProperties.LogMode mode = logClientProperties.getEs().getMode();

        // 如果
        if (mode == LogClientProperties.LogMode.READ_WRITE) {
            this.logState = true;
            batchExecutor = new ThreadPoolExecutor( 1, maxBatchThreads, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>( maxBatchQueueSize ),
                    new ThreadFactoryBuilder().setDaemon( true ).setNameFormat( "log-es-batch-%d" ).build(), new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    log.error( "Log ES Batch Task {} rejected from {}", r.toString(), executor.toString() );
                }
            } );
            daemonExporter = new ElasticsearchDaemonExporter();
            daemonExporter.setName( "log-es-monitor" );
            daemonExporter.setDaemon( true );
            daemonExporter.init();
            daemonExporter.start();
        } else {
            this.logState = false;
        }
    }

    /**
     * 注册日志类型
     *
     * @param logClass
     */
    public void regLogObject(Class<?> logClass, String index, String indexPattern) {
        String rawIndex = index == null ? buildIndexName( logClass ) : index;
        FastDateFormat dateFormat = indexPattern == null ? null : FastDateFormat.getInstance( indexPattern, (TimeZone) null );
        IndexConfigVo indexConfigVo = new IndexConfigVo( rawIndex, rawIndex + "_*", dateFormat );
        regMap.put( logClass, indexConfigVo );
    }

    /**
     * 获取日志配置的索引名。
     *
     * @param logClass
     */
    public String getRawIndexName(Class<?> logClass) {
        IndexConfigVo configVo = regMap.get( logClass );
        if (configVo == null) {
            return null;
        }
        return configVo.getRawName();
    }

    /**
     * 获得带引号的索引名。
     *
     * @param logClass
     * @return
     */

    public String getQuotedRawIndexName(Class<?> logClass) {
        return '"' + getRawIndexName( logClass ) + '"';
    }

    /**
     * 获取日志的查询索引
     *
     * @param logClass
     */
    public String getQueryIndexName(Class<?> logClass) {
        IndexConfigVo configVo = regMap.get( logClass );
        if (configVo == null) {
            return null;
        }
        return configVo.getQueryName();
    }

    /**
     * 获得带引号的查询索引名。
     *
     * @param logClass
     * @return
     */

    public String getQuotedQueryIndexName(Class<?> logClass) {
        return "\\\"" + getQueryIndexName( logClass ) + "\\\"";
    }

    /**
     * 写日志
     *
     * @param source 日志对象
     */
    public <T extends LogBaseVo> void writeLog(T source) {
        if (!logState) {
            return;
        }
        String index = getIndex( source.getClass() );
        if (StringUtils.isBlank( index )) {
            log.warn( "LogClass[{}] not registry!!!", source.getClass().getName() );
            return;
        }
        // 写上时间戳
        source.setTimestamp( DATE_FORMAT.format( System.currentTimeMillis() ) );
        // 是否需要覆写
        if (appInfoOverwrite) {
            source.setAppInfo( appName );
            source.setAppHost( appHost );
        }
        okio.Buffer okb = new okio.Buffer();
        okb.writeUtf8( "{\"index\":{\"_index\":\"" ).writeUtf8( index )
//                .writeUtf8("\",\"_type\":\"")
//                .writeUtf8(INDEX_TYPE)
                .writeUtf8( "\"}}" );
        okb.write( LINE_SEPARATOR_BYTES );
        try {
            JsonInterfaceHelper.JSON_CONVERTER.write( okb.outputStream(), source );
        } catch (Exception e) {
            log.error( e.getMessage(), e );
        }
        okb.write( LINE_SEPARATOR_BYTES );
        batchLock.lock();
        try {
            buffer.writeAll( okb );
        } catch (Exception e) {
            log.error( e.getMessage(), e );
        } finally {
            batchLock.unlock();
        }
    }

    /**
     * 批量写日志
     *
     * @param sourceList 日志对象列表
     * @param <T>
     */
    public <T extends LogBaseVo> void writeBulkLog(List<T> sourceList) {
        if (!logState) {
            return;
        }

        if (sourceList == null || sourceList.isEmpty()) {
            return;
        }
        Class logClass = sourceList.get( 0 ).getClass();
        String index = getIndex( logClass );
        if (StringUtils.isBlank( index )) {
            log.warn( "LogClass[{}] not registry!!!", logClass.getName() );
            return;
        }
        okio.Buffer okb = new okio.Buffer();
        for (T source : sourceList) {
            // 写上时间戳
            source.setTimestamp( DATE_FORMAT.format( System.currentTimeMillis() ) );
            // 是否需要覆写
            if (appInfoOverwrite) {
                source.setAppInfo( appName );
                source.setAppHost( appHost );
            }
            okb.writeUtf8( "{\"index\":{\"_index\":\"" ).writeUtf8( index )
//                    .writeUtf8("\",\"_type\":\"")
//                    .writeUtf8(INDEX_TYPE)
                    .writeUtf8( "\"}}" );
            okb.write( LINE_SEPARATOR_BYTES );
            try {
                JsonInterfaceHelper.JSON_CONVERTER.write( okb.outputStream(), source );
            } catch (Exception e) {
                log.error( e.getMessage(), e );
            }
            okb.write( LINE_SEPARATOR_BYTES );
        }
        batchLock.lock();
        try {
            buffer.writeAll( okb );
        } catch (Exception e) {
            log.error( e.getMessage(), e );
        } finally {
            batchLock.unlock();
        }
    }

    /**
     * Send buffer to Elasticsearch
     */
    public void processLogBuffer() {
        okio.Buffer bufferData = null;
        batchLock.lock();
        try {
            if (buffer.size() > 0) {
                bufferData = buffer;
                buffer = new okio.Buffer();
            }
        } finally {
            batchLock.unlock();
        }
        if (bufferData == null) {
            return;
        }
        try {
            Request.Builder requestBuilder = new Request.Builder().url( server + esBulk );
            if (needBasicAuth) {
                requestBuilder.header( "Authorization", Credentials.basic( username, password ) );
            }
            httpInterface.requestForData( requestBuilder.post( BufferRequestBody.create( bufferData, MediaTypes.JSON_UTF8 ) ).build() );
        } catch (Exception e) {
            log.error( e.getMessage(), e );
        }
    }

    /**
     * 关闭写日志系统
     */
    public void destroyLog() {
        if (logState) {
            //先关掉log
            logState = false;
            daemonExporter.readyDestroy();
            batchExecutor.shutdown();
            processLogBuffer();
        }
    }

    /**
     * dsl查询日志
     *
     * @param tClass   日志对象类型
     * @param index    索引
     * @param dslQuery dsl查询条件
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> SearchResponse<T> dslQuery(Class<T> tClass, String index, String dslQuery) {
        if (StringUtils.isBlank( server )) {
            return null;
        }
        StringBuilder urlBuilder = new StringBuilder( server );
        urlBuilder.append( "/" ).append( index ).append( "/" ).append( "_search" );
        SearchResponse<T> resp = null;
        JavaType javaType = JsonInterfaceHelper.JSON_CONVERTER.constructParametricType( SearchResponse.class, tClass );

        try {
            Request.Builder requestBuilder = new Request.Builder().url( urlBuilder.toString() );
            if (needBasicAuth) {
                requestBuilder.header( "Authorization", Credentials.basic( username, password ) );
            }
            resp = (SearchResponse<T>) httpInterface.requestForEntity( requestBuilder.post( RequestBody.create( dslQuery, MediaTypes.JSON_UTF8 ) ).build(), javaType ).getValue();
        } catch (Exception e) {
            log.error( e.getMessage(), e );
        }
        return resp;
    }

    /**
     * scroll查询
     *
     * @param tClass              日志对象类型
     * @param index               索引
     * @param scrollExpireSeconds scroll api 过期时间
     * @param <T>
     * @return
     */
    public <T> ScrollResponse<T> scrollQueryOpen(Class<T> tClass, String index, int scrollExpireSeconds, String dslQuery) {
        if (StringUtils.isBlank( server )) {
            return null;
        }
        if (scrollExpireSeconds <= 0) {
            scrollExpireSeconds = 60;
        }
        StringBuilder urlBuilder = new StringBuilder( server );
        urlBuilder.append( "/" ).append( index ).append( "/" ).append( "_search?" ).append( SCROLL ).append( "=" ).append( scrollExpireSeconds ).append( "s" );
        ScrollResponse<T> resp = null;
        try {
            Request.Builder requestBuilder = new Request.Builder().url( urlBuilder.toString() );
            if (needBasicAuth) {
                requestBuilder.header( "Authorization", Credentials.basic( username, password ) );
            }
            JavaType javaType = JsonInterfaceHelper.JSON_CONVERTER.constructParametricType( ScrollResponse.class, tClass );
            resp = (ScrollResponse<T>) httpInterface.requestForEntity( requestBuilder.post( RequestBody.create( dslQuery, MediaTypes.JSON_UTF8 ) ).build(), javaType ).getValue();
        } catch (Exception e) {
            log.error( e.getMessage(), e );
        }
        return resp;
    }

    /**
     * scroll查询
     *
     * @param <T>
     * @param tClass              日志对象类型
     * @param scrollId            这里只需传递scrollId即可返回下一页的数据
     * @param scrollExpireSeconds scroll api 过期时间
     * @return
     */
    public <T> ScrollResponse<T> scrollQueryNext(Class<T> tClass, String scrollId, int scrollExpireSeconds) {
        if (StringUtils.isBlank( server )) {
            return null;
        }
        if (scrollExpireSeconds <= 0) {
            scrollExpireSeconds = 60;
        }
        StringBuilder urlBuilder = new StringBuilder( server );
        urlBuilder.append( "/_search/" ).append( SCROLL );
        String requestBody = String.format( "{\"scroll_id\" : \"%s\",\"scroll\": \"%s\"}", scrollId, scrollExpireSeconds + "s" );
        ScrollResponse<T> resp = null;
        JavaType javaType = JsonInterfaceHelper.JSON_CONVERTER.constructParametricType( ScrollResponse.class, tClass );

        try {
            Request.Builder requestBuilder = new Request.Builder().url( urlBuilder.toString() );
            if (needBasicAuth) {
                requestBuilder.header( "Authorization", Credentials.basic( username, password ) );
            }
            resp = (ScrollResponse<T>) httpInterface.requestForEntity( requestBuilder.post( RequestBody.create( requestBody, MediaTypes.JSON_UTF8 ) ).build(), javaType ).getValue();
        } catch (Exception e) {
            log.error( e.getMessage(), e );
        }
        return resp;
    }

    /**
     * 关闭scroll api 查询
     *
     * @param scrollId 需删除的scrollId
     * @return
     */
    public DeleteScrollResponse scrollQueryClose(String scrollId) {
        if (StringUtils.isBlank( server )) {
            return null;
        }
        StringBuilder urlBuilder = new StringBuilder( server );
        urlBuilder.append( "/_search/" ).append( SCROLL );
        String requestBody = String.format( "{\"scroll_id\":\"%s\"}", scrollId );
        DeleteScrollResponse resp;
        try {
            Request.Builder requestBuilder = new Request.Builder().url( urlBuilder.toString() );
            if (needBasicAuth) {
                requestBuilder.header( "Authorization", Credentials.basic( username, password ) );
            }
            resp = httpInterface.requestForEntity( requestBuilder.delete( RequestBody.create( requestBody, MediaTypes.JSON_UTF8 ) ).build(), DeleteScrollResponse.class ).getValue();
        } catch (Exception e) {
            resp = new DeleteScrollResponse();
            log.error( e.getMessage(), e );
        }

        return resp;
    }

    /**
     * 转换Sql为DSL。
     *
     * @param sql
     * @param isTrueCount
     * @return
     */
    public String translateSqlToDsl(String sql, int startIndex, int resultNum, boolean isTrueCount) throws Exception {
        if (StringUtils.isBlank( server )) {
            throw new IllegalArgumentException( "es server is blank!" );
        }
        if (StringUtils.isBlank( sql )) {
            throw new IllegalArgumentException( "sql is blank!" );
        }
        if (sql.contains( "limit" )) {
            throw new IllegalArgumentException( "sql contains limit, please remove limit." );
        }
        sql = sql.trim();
        String dsl = null;
        StringBuilder urlBuilder = new StringBuilder( server );
        urlBuilder.append( "/" ).append( "_sql/translate" );
        // 这里支持count(*) 无需limit
        if (resultNum > 0) {
            sql = sql + " limit " + resultNum;
        }
        sql = String.format( "{\"query\": \"%s\"}", sql );
        Request.Builder requestBuilder = new Request.Builder().url( urlBuilder.toString() );
        if (needBasicAuth) {
            requestBuilder.header( "Authorization", Credentials.basic( username, password ) );
        }
        dsl = httpInterface.requestForEntity( requestBuilder.post( RequestBody.create( sql, MediaTypes.JSON_UTF8 ) ).build(), String.class ).getValue();

        if (startIndex > 0) {
            dsl = "{" + " \"from\" : " + startIndex + "," + dsl.substring( 1, dsl.length() );
        }
        if (isTrueCount && !dsl.contains( "\"track_total_hits\"" )) {
            dsl = "{" + "\"track_total_hits\": true," + dsl.substring( 1, dsl.length() );
        } else if (isTrueCount && dsl.contains( "\"track_total_hits\"" )) {
            // es8 track_total_hits为-1、换为true
            dsl = dsl.replace( "\"track_total_hits\":-1", "\"track_total_hits\":true" );
        }

        // es8.x返回_source为false, 设置为true兼容
        dsl = dsl.replace( "\"_source\":false", "\"_source\":true" );

        // 删除无用的fields相关字段。
        int fStart = dsl.indexOf( "\"fields\":[" );
        int fEnd = dsl.indexOf( "],", fStart ) + 2;
        if (fStart > 0 && fEnd > 0 && fEnd > fStart) {
            dsl = dsl.substring( 0, fStart ) + dsl.substring( fEnd );
        }
        return dsl;
    }

    /**
     * 将查询结果映射成List
     *
     * @param resp
     * @param tClass
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> mapQueryResponseToList(String resp, Class<T> tClass) {
        if (StringUtils.isNotBlank( resp )) {
            SearchResponse<T> response = null;
            try {
                response = JsonInterfaceHelper.JSON_CONVERTER.parse( resp, JsonInterfaceHelper.JSON_CONVERTER.constructParametricType( SearchResponse.class, tClass ) );
            } catch (Exception e) {
                log.error( e.getMessage(), e );
            }
            if (response != null) {
                List<SearchResponse.Hits<T>> hitsList = response.getHitsResponse().getHits();
                if (!hitsList.isEmpty()) {
                    List<T> dataList = new ArrayList<>();
                    for (SearchResponse.Hits<T> hits : hitsList) {
                        dataList.add( hits.getSource() );
                    }
                    return dataList;
                }
            }
        }
        return null;
    }

    /**
     * 根据类名建立索引名称
     *
     * @param logClass
     * @return
     */
    private String buildIndexName(Class<?> logClass) {
        String className = logClass.getName();
        int lastIndex = className.lastIndexOf( "." );
        String indexName = "";
        if (lastIndex > 0) {
            // 偏移一下,把'.'带上
            lastIndex++;
            String canonicalPath = className.substring( 0, lastIndex );
            String logVoName = className.substring( lastIndex, className.length() );
            indexName = CaseFormat.UPPER_CAMEL.to( CaseFormat.LOWER_UNDERSCORE, logVoName );
            indexName = canonicalPath + indexName;
        } else {
            indexName = CaseFormat.UPPER_CAMEL.to( CaseFormat.LOWER_UNDERSCORE, className );
        }
        return indexName;
    }

    /**
     * @param logClass
     * @return
     */
    private String getIndex(Class<?> logClass) {
        IndexConfigVo configVo = regMap.get( logClass );
        if (configVo == null) {
            return null;
        }
        FastDateFormat indexPattern = configVo.getIndexPattern();
        if (indexPattern == null) {
            return configVo.getRawName();
        }
        return configVo.getRawName() + '_' + indexPattern.format( System.currentTimeMillis() );
    }

    /**
     * 后台写日志线程
     */
    public class ElasticsearchDaemonExporter extends Thread {

        /**
         * 运行标记.
         */
        private volatile boolean isRunning = false;

        /**
         * 下一次运行时间
         */
        private volatile long nextScanTime = 0;

        /**
         * 初始化
         */
        public void init() {
            isRunning = true;
        }

        /**
         * 销毁标记.
         */
        public void readyDestroy() {
            isRunning = false;
        }

        @Override
        public void run() {
            while (isRunning) {
                try {
                    if (buffer.size() > maxBytesOfBatch || nextScanTime < System.currentTimeMillis()) {
                        nextScanTime = System.currentTimeMillis() + maxFlushInMilliseconds;
                        batchExecutor.submit( new Runnable() {
                            @Override
                            public void run() {
                                processLogBuffer();
                            }
                        } );
                    }
                    Thread.sleep( 500 );
                } catch (Exception e) {
                    log.error( "Exception processing log entries", e );
                }
            }
        }
    }
}
