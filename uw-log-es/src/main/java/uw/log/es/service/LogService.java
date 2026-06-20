package uw.log.es.service;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
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
import uw.common.util.JsonUtils;
import uw.common.util.SystemClock;
import uw.httpclient.http.HttpConfig;
import uw.httpclient.http.HttpData;
import uw.httpclient.http.HttpInterface;
import uw.httpclient.json.JsonInterfaceHelper;
import uw.httpclient.util.BufferRequestBody;
import uw.httpclient.util.MediaTypes;
import uw.httpclient.util.SSLContextUtils;
import uw.log.es.LogClientProperties;
import uw.log.es.util.IndexConfigVo;
import uw.log.es.vo.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 日志服务。
 * <p>
 * {@link LogClient} 的核心实现，负责：
 * <ul>
 *   <li>日志类型注册与索引名管理（原始名 / 查询名 / 时间滚动模式）；</li>
 *   <li>日志写入：单条与批量均写入 {@link #buffer}，由 {@link ElasticsearchDaemonExporter}
 *       守护线程按时间阈值（{@code maxFlushInMilliseconds}）或字节阈值
 *       （{@code maxBytesOfBatch}）触发，提交至 {@link #batchExecutor} 执行 bulk；</li>
 *   <li>日志查询：DSL / SQL 转 DSL / scroll 游标查询。</li>
 * </ul>
 * 构造时根据 {@link LogClientProperties.LogMode} 决定是否启动后台写入链路：
 * {@code READ_ONLY} 仅支持查询，不创建线程池与守护线程。
 */
public class LogService {

    private static final Logger log = LoggerFactory.getLogger(LogService.class);

    /**
     * 日志编码
     */
    private static final Charset LOG_CHARSET = StandardCharsets.UTF_8;

    /**
     * 换行符字节
     */
    private static final byte[] LINE_SEPARATOR_BYTES = System.lineSeparator().getBytes(LOG_CHARSET);

    /**
     * 时间序列格式化（写入日志体 @timestamp 字段，ISO8601 毫秒精度）
     */
    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

    /**
     * scroll api URL 片段
     */
    private static final String SCROLL = "scroll";

    /**
     * buffer 读写互斥锁，保护 {@link #buffer} 的并发追加与换出。
     */
    private final Lock batchLock = new ReentrantLock();

    /**
     * 已注册的日志类型配置表：日志类 -> 索引配置。
     */
    private final Map<Class<?>, IndexConfigVo> regMap = new ConcurrentHashMap<>();
    /**
     * ES集群HTTP REST地址
     */
    private final String esServer;
    /**
     * 用户名
     */
    private final String esUsername;
    /**
     * 用户密码
     */
    private final String esPassword;
    /**
     * 应用名称（覆写日志体 appInfo 时使用）
     */
    private final String appName;
    /**
     * 应用主机信息（覆写日志体 appHost 时使用）
     */
    private final String appHost;
    /**
     * 是否用应用信息覆写日志体中的 appInfo/appHost。
     */
    private final boolean appInfoOverwrite;
    /**
     * HTTP 客户端，承载所有对 ES 的请求。
     */
    private HttpInterface httpInterface;
    /**
     * 是否启用写入链路（READ_WRITE 模式且 ES server 已配置时为 true）。
     */
    private boolean logState;
    /**
     * 是否需要Http Basic验证头
     */
    private boolean needBasicAuth;
    /**
     * Elasticsearch bulk api 地址（相对 server 的 path）。
     */
    private String esBulk;
    /**
     * 触发后台 flush 的时间阈值（毫秒）。
     */
    private long maxFlushInMilliseconds;
    /**
     * 触发立即 flush 的 buffer 字节阈值。
     */
    private long maxBytesOfBatch;
    /**
     * 批量提交线程池的最大线程数。
     */
    private int maxBatchThreads;
    /**
     * 批量提交线程池的队列容量。
     */
    private int maxBatchQueueSize;
    /**
     * 待提交的日志 bulk 内容缓冲区。
     */
    private okio.Buffer buffer = new okio.Buffer();
    /**
     * 后台监控守护线程，负责定时/按阈值触发 flush。
     */
    private ElasticsearchDaemonExporter daemonExporter;
    /**
     * 后台批量提交线程池。
     */
    private ThreadPoolExecutor batchExecutor;

    /**
     * 构造日志服务。
     * <p>当 {@code server} 未配置时标记 {@code logState=false}（不写入、不起后台线程，仅保留查询能力）；
     * 当 {@code mode=READ_WRITE} 时初始化批量线程池与守护线程。
     *
     * @param logClientProperties 配置器
     * @param appName             应用名称
     * @param appHost             应用主机信息
     */
    public LogService(final LogClientProperties logClientProperties, final String appName, final String appHost) {
        this.appName = appName;
        this.appHost = appHost;
        this.appInfoOverwrite = logClientProperties.getEs().isAppInfoOverwrite();
        this.esServer = logClientProperties.getEs().getServer();
        this.esUsername = logClientProperties.getEs().getUsername();
        this.esPassword = logClientProperties.getEs().getPassword();
        if (StringUtils.isBlank(this.esServer)) {
            log.error("ElasticSearch server config is null! LogClient can't log anything!!!");
            this.logState = false;
            return;
        }
        this.needBasicAuth = StringUtils.isNotBlank(esUsername) && StringUtils.isNotBlank(esPassword);
        this.httpInterface = new JsonInterfaceHelper(HttpConfig.builder().retryOnConnectionFailure(true).connectTimeout(logClientProperties.getEs().getConnectTimeout()).readTimeout(logClientProperties.getEs().getReadTimeout()).writeTimeout(logClientProperties.getEs().getWriteTimeout()).trustManager(SSLContextUtils.getTrustAllManager()).sslSocketFactory(SSLContextUtils.getTruestAllSocketFactory()).hostnameVerifier((hostName, sslSession) -> true).build());
        this.esBulk = logClientProperties.getEs().getEsBulk();
        this.maxFlushInMilliseconds = logClientProperties.getEs().getMaxFlushInSeconds() * 1000L;
        this.maxBytesOfBatch = logClientProperties.getEs().getMaxKiloBytesOfBatch() * 1024L;
        this.maxBatchThreads = logClientProperties.getEs().getMaxBatchThreads();
        this.maxBatchQueueSize = logClientProperties.getEs().getMaxBatchQueueSize();

        LogClientProperties.LogMode mode = logClientProperties.getEs().getMode();

        // 如果
        if (mode == LogClientProperties.LogMode.READ_WRITE) {
            this.logState = true;
            batchExecutor = new ThreadPoolExecutor(1, maxBatchThreads, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(maxBatchQueueSize),
                    new ThreadFactoryBuilder().setDaemon(true).setNameFormat("log-es-batch-%d").build(), new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    log.error("Log ES Batch Task {} rejected from {}", r.toString(), executor.toString());
                }
            });
            daemonExporter = new ElasticsearchDaemonExporter();
            daemonExporter.setName("log-es-monitor");
            daemonExporter.setDaemon(true);
            daemonExporter.init();
            daemonExporter.start();
        } else {
            this.logState = false;
        }
    }

    /**
     * 注册日志类型。
     * <p>若 {@code index} 为空则由类名按 lower_underscore 规则推导；若指定 {@code indexPattern}，
     * 则写入索引追加时间后缀、查询索引使用 {@code 原始名_*}。重复注册且索引名变化时会输出告警。
     *
     * @param logClass     日志类
     * @param index        自定义索引名，为 {@code null} 时按类名推导
     * @param indexPattern 索引时间滚动模式，为空表示不滚动
     */
    public void regLogObject(Class<?> logClass, String index, String indexPattern) {
        String rawIndex = index == null ? buildIndexName(logClass) : index;
        FastDateFormat dateFormat = null;
        String queryName = rawIndex;
        if (StringUtils.isNotBlank(indexPattern)) {
            dateFormat = FastDateFormat.getInstance(indexPattern, (TimeZone) null);
            queryName = queryName + "_*";
        }
        IndexConfigVo indexConfigVo = new IndexConfigVo(rawIndex, queryName, dateFormat);
        IndexConfigVo old = regMap.put(logClass, indexConfigVo);
        if (old != null && !old.getRawName().equals(rawIndex)) {
            log.warn("LogClass[{}] re-registered, index changed from [{}] to [{}]", logClass.getName(), old.getRawName(), rawIndex);
        }
    }

    /**
     * 获取日志类型配置的原始索引名（不含时间后缀与通配符）。
     *
     * @param logClass 日志类
     * @return 原始索引名；未注册时返回 {@code null}
     */
    public String getRawIndexName(Class<?> logClass) {
        IndexConfigVo configVo = regMap.get(logClass);
        if (configVo == null) {
            return null;
        }
        return configVo.getRawName();
    }

    /**
     * 获取带引号的原始索引名（裸引号 "xxx"），用于构造写入/bulk的_index等场景。
     *
     * @param logClass
     * @return
     */

    public String getQuotedRawIndexName(Class<?> logClass) {
        return '"' + getRawIndexName(logClass) + '"';
    }

    /**
     * 获取日志类型的查询索引名（设置模式时为 {@code 原始名_*} 通配形式）。
     *
     * @param logClass 日志类
     * @return 查询索引名；未注册时返回 {@code null}
     */
    public String getQueryIndexName(Class<?> logClass) {
        IndexConfigVo configVo = regMap.get(logClass);
        if (configVo == null) {
            return null;
        }
        return configVo.getQueryName();
    }

    /**
     * 获取带引号的查询索引名（转义引号 \"xxx*\"），用于拼入ES SQL的from子句，
     * 因为SQL字符串本身已被外层JSON字符串包裹，故需对引号做反斜杠转义。
     *
     * @param logClass
     * @return
     */

    public String getQuotedQueryIndexName(Class<?> logClass) {
        return "\\\"" + getQueryIndexName(logClass) + "\\\"";
    }

    /**
     * 写入单条日志到 buffer。
     * <p>写入前补齐 @timestamp；若启用 appInfoOverwrite 则覆写 appInfo/appHost。
     * 未注册的日志类型或未启用写入时直接返回。
     *
     * @param source 日志对象
     */
    public <T extends LogBaseVo> void writeLog(T source) {
        if (!logState) {
            return;
        }
        long now = SystemClock.now();
        String index = getIndex(source.getClass(), now);
        if (StringUtils.isBlank(index)) {
            log.warn("LogClass[{}] not registry!!!", source.getClass().getName());
            return;
        }
        // 写上时间戳
        source.setTimestamp(DATE_FORMAT.format(now));
        // 是否需要覆写
        if (appInfoOverwrite) {
            source.setAppInfo(appName);
            source.setAppHost(appHost);
        }
        okio.Buffer okb = new okio.Buffer();
        okb.writeUtf8("{\"create\":{\"_index\":\"").writeUtf8(index).writeUtf8("\"},\"_source\":false}");
        okb.write(LINE_SEPARATOR_BYTES);
        try {
            JsonUtils.write(source, okb.outputStream());
            okb.write(LINE_SEPARATOR_BYTES);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        batchLock.lock();
        try {
            buffer.writeAll(okb);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            batchLock.unlock();
        }
    }

    /**
     * 批量写入日志到 buffer。
     * <p>以列表首元素的运行时类作为整批索引判定依据；逐条补齐 @timestamp 与应用信息。
     * 未注册、未启用写入或空列表时直接返回。
     *
     * @param sourceList 日志对象列表
     * @param <T>        日志对象类型
     */
    public <T extends LogBaseVo> void writeBulkLog(List<T> sourceList) {
        if (!logState) {
            return;
        }

        if (sourceList == null || sourceList.isEmpty()) {
            return;
        }

        Class<? extends LogBaseVo> logClass = sourceList.getFirst().getClass();
        long now = SystemClock.now();
        String index = getIndex(logClass, now);

        if (StringUtils.isBlank(index)) {
            log.warn("LogClass[{}] not registry!!!", logClass.getName());
            return;
        }
        okio.Buffer okb = new okio.Buffer();
        for (T source : sourceList) {
            // 写上时间戳
            source.setTimestamp(DATE_FORMAT.format(now));
            // 是否需要覆写
            if (appInfoOverwrite) {
                source.setAppInfo(appName);
                source.setAppHost(appHost);
            }
            okb.writeUtf8("{\"index\":{\"_index\":\"").writeUtf8(index).writeUtf8("\"}}");
            okb.write(LINE_SEPARATOR_BYTES);
            try {
                JsonUtils.write(source, okb.outputStream());
                okb.write(LINE_SEPARATOR_BYTES);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                continue;
            }
        }
        batchLock.lock();
        try {
            buffer.writeAll(okb);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            batchLock.unlock();
        }
    }

    /**
     * 换出 buffer 并通过 bulk api 提交至 Elasticsearch。
     * <p>buffer 为空时直接返回；HTTP 非 200 或 bulk 返回 errors 时记录错误日志。
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
            Request.Builder requestBuilder = new Request.Builder().url(esServer + esBulk);
            if (needBasicAuth) {
                requestBuilder.header("Authorization", Credentials.basic(esUsername, esPassword));
            }
            HttpData httpData = httpInterface.requestForData(requestBuilder.post(BufferRequestBody.create(bufferData, MediaTypes.JSON_UTF8)).build());
            if (httpData.getStatusCode() != 200) {
                log.error("LogClient ES Batch process error! code:{}, response:{}", httpData.getStatusCode(), httpData.getResponseData());
                return;
            }
            BulkResponse bulkResponse = JsonUtils.parse(httpData.getResponseData(), BulkResponse.class);
            if (bulkResponse.isErrors()) {
                log.error("LogClient ES Bulk returned errors! took:{}, response:{}", bulkResponse.getTook(), httpData.getResponseData());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 关闭写日志系统。
     * <p>先置 {@code logState=false} 阻止新数据写入，再中断守护线程并等待其退出，
     * 随后关闭批量线程池并等待已提交任务完成（最多 5 秒，超时则 shutdownNow），
     * 最后同步 flush 一次以确保残留 buffer 落盘。
     */
    public void destroy() {
        if (logState) {
            //先关掉log，阻止新数据写入buffer
            logState = false;
            //通知守护线程退出，并打断其sleep以尽快响应
            daemonExporter.readyDestroy();
            daemonExporter.interrupt();
            try {
                daemonExporter.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            //关闭批量线程池，等待已提交任务完成，避免丢失buffer中的日志
            batchExecutor.shutdown();
            try {
                if (!batchExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    batchExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                batchExecutor.shutdownNow();
            }
            //最后再同步flush一次，确保剩余buffer落盘
            processLogBuffer();
        }
    }

    /**
     * 在指定索引上执行 DSL 查询。
     *
     * @param tClass   日志对象类型
     * @param index    索引
     * @param dslQuery dsl查询条件
     * @return 搜索响应；ES server 未配置或查询异常时返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> SearchResponse<T> dslQuery(Class<T> tClass, String index, String dslQuery) {
        if (StringUtils.isBlank(esServer)) {
            return null;
        }
        StringBuilder urlBuilder = new StringBuilder(esServer);
        urlBuilder.append("/").append(index).append("/").append("_search");
        SearchResponse<T> resp = null;
        JavaType javaType = JsonUtils.constructParametricType(SearchResponse.class, tClass);

        try {
            Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.toString());
            if (needBasicAuth) {
                requestBuilder.header("Authorization", Credentials.basic(esUsername, esPassword));
            }
            resp = (SearchResponse<T>) httpInterface.requestForEntity(requestBuilder.post(RequestBody.create(dslQuery, MediaTypes.JSON_UTF8)).build(), javaType).getValue();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return resp;
    }

    /**
     * 开启 scroll 游标查询，返回首批数据与 scrollId。
     *
     * @param tClass              日志对象类型
     * @param index               索引
     * @param scrollExpireSeconds scroll api 过期时间（秒），{@code <=0} 时默认 60
     * @param dslQuery            dsl查询条件（不可含 from 节点）
     * @param <T>                 日志对象类型
     * @return scroll 响应；ES server 未配置或查询异常时返回 {@code null}
     */
    public <T> ScrollResponse<T> scrollQueryOpen(Class<T> tClass, String index, int scrollExpireSeconds, String dslQuery) {
        if (StringUtils.isBlank(esServer)) {
            return null;
        }
        if (scrollExpireSeconds <= 0) {
            scrollExpireSeconds = 60;
        }
        StringBuilder urlBuilder = new StringBuilder(esServer);
        urlBuilder.append("/").append(index).append("/").append("_search?").append(SCROLL).append("=").append(scrollExpireSeconds).append("s");
        ScrollResponse<T> resp = null;
        try {
            Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.toString());
            if (needBasicAuth) {
                requestBuilder.header("Authorization", Credentials.basic(esUsername, esPassword));
            }
            JavaType javaType = JsonUtils.constructParametricType(ScrollResponse.class, tClass);
            resp = (ScrollResponse<T>) httpInterface.requestForEntity(requestBuilder.post(RequestBody.create(dslQuery, MediaTypes.JSON_UTF8)).build(), javaType).getValue();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return resp;
    }

    /**
     * 基于 scrollId 获取下一批数据。
     *
     * @param <T>                 日志对象类型
     * @param tClass              日志对象类型
     * @param scrollId            上一次返回的 scrollId，仅需传递 scrollId 即可获取下一页数据
     * @param scrollExpireSeconds scroll api 过期时间（秒），{@code <=0} 时默认 60
     * @return scroll 响应；ES server 未配置或查询异常时返回 {@code null}
     */
    public <T> ScrollResponse<T> scrollQueryNext(Class<T> tClass, String scrollId, int scrollExpireSeconds) {
        if (StringUtils.isBlank(esServer)) {
            return null;
        }
        if (scrollExpireSeconds <= 0) {
            scrollExpireSeconds = 60;
        }
        StringBuilder urlBuilder = new StringBuilder(esServer);
        urlBuilder.append("/_search/").append(SCROLL);
        String escapedScrollId = new String(JsonStringEncoder.getInstance().quoteAsString(scrollId));
        String requestBody = String.format("{\"scroll_id\":\"%s\",\"scroll\":\"%s\"}", escapedScrollId, scrollExpireSeconds + "s");
        ScrollResponse<T> resp = null;
        JavaType javaType = JsonUtils.constructParametricType(ScrollResponse.class, tClass);

        try {
            Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.toString());
            if (needBasicAuth) {
                requestBuilder.header("Authorization", Credentials.basic(esUsername, esPassword));
            }
            resp = (ScrollResponse<T>) httpInterface.requestForEntity(requestBuilder.post(RequestBody.create(requestBody, MediaTypes.JSON_UTF8)).build(), javaType).getValue();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return resp;
    }

    /**
     * 关闭 scroll 游标，释放 ES 端上下文资源。
     *
     * @param scrollId 需删除的scrollId
     * @return 删除响应；异常时返回一个 {@code succeeded=false} 的空响应
     */
    public DeleteScrollResponse scrollQueryClose(String scrollId) {
        if (StringUtils.isBlank(esServer)) {
            return null;
        }
        StringBuilder urlBuilder = new StringBuilder(esServer);
        urlBuilder.append("/_search/").append(SCROLL);
        String escapedScrollId = new String(JsonStringEncoder.getInstance().quoteAsString(scrollId));
        String requestBody = String.format("{\"scroll_id\":\"%s\"}", escapedScrollId);
        DeleteScrollResponse resp;
        try {
            Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.toString());
            if (needBasicAuth) {
                requestBuilder.header("Authorization", Credentials.basic(esUsername, esPassword));
            }
            resp = httpInterface.requestForEntity(requestBuilder.delete(RequestBody.create(requestBody, MediaTypes.JSON_UTF8)).build(), DeleteScrollResponse.class).getValue();
        } catch (Exception e) {
            resp = new DeleteScrollResponse();
            log.error(e.getMessage(), e);
        }

        return resp;
    }

    /**
     * 将 SQL 转换为 ES DSL。
     * <p>通过 ES {@code _sql/translate} 接口翻译后，基于 JsonNode 改写：
     * 附加 {@code from}（当 startIndex&gt;0）、设置 {@code track_total_hits}（当 isTrueCount）、
     * 将 {@code _source:false} 改为 true、移除 {@code fields}。SQL 不可包含 limit。
     *
     * @param sql         SQL 语句
     * @param startIndex  分页起始偏移量
     * @param resultNum   结果条数，{@code >0} 时拼接 limit
     * @param isTrueCount 是否需要真实总数
     * @return 转换后的 DSL 字符串
     */
    public String translateSqlToDsl(String sql, int startIndex, int resultNum, boolean isTrueCount) {
        if (StringUtils.isBlank(esServer)) {
            throw new IllegalArgumentException("es server is blank!");
        }
        if (StringUtils.isBlank(sql)) {
            throw new IllegalArgumentException("sql is blank!");
        }
        if (sql.contains("limit")) {
            throw new IllegalArgumentException("sql contains limit, please remove limit.");
        }
        sql = sql.trim();
        StringBuilder urlBuilder = new StringBuilder(esServer);
        urlBuilder.append("/").append("_sql/translate");
        // 这里支持count(*) 无需limit
        if (resultNum > 0) {
            sql = sql + " limit " + resultNum;
        }
        String escapedSql = new String(JsonStringEncoder.getInstance().quoteAsString(sql));
        String sqlBody = String.format("{\"query\": \"%s\"}", escapedSql);
        Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.toString());
        if (needBasicAuth) {
            requestBuilder.header("Authorization", Credentials.basic(esUsername, esPassword));
        }
        String dsl = httpInterface.requestForEntity(requestBuilder.post(RequestBody.create(sqlBody, MediaTypes.JSON_UTF8)).build(), String.class).getValue();

        // 以JsonNode方式解析后改写，避免字符串拼接破坏JSON结构（如fields数组内含']'字符、首字符非'{'等情况）。
        com.fasterxml.jackson.databind.node.ObjectNode root;
        try {
            root = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.getJsonMapper().readTree(dsl);
        } catch (Exception e) {
            log.error("translateSqlToDsl parse ES _sql/translate response failed! raw:{}", dsl, e);
            throw new IllegalArgumentException("invalid dsl from _sql/translate: " + dsl, e);
        }
        if (startIndex > 0) {
            root.put("from", startIndex);
        }
        if (isTrueCount) {
            root.put("track_total_hits", true);
        }
        // es8.x返回_source为false, 设置为true兼容
        com.fasterxml.jackson.databind.JsonNode sourceNode = root.get("_source");
        if (sourceNode != null && sourceNode.isBoolean() && !sourceNode.asBoolean()) {
            root.put("_source", true);
        }
        // 删除无用的fields字段。
        root.remove("fields");
        return root.toString();
    }

    /**
     * 根据类名建立索引名称：保留包路径前缀，简单类名转为 lower_underscore 风格。
     *
     * @param logClass 日志类
     * @return 索引名称
     */
    private String buildIndexName(Class<?> logClass) {
        String className = logClass.getName();
        int lastIndex = className.lastIndexOf(".");
        String indexName = "";
        if (lastIndex > 0) {
            // 偏移一下,把'.'带上
            lastIndex++;
            String canonicalPath = className.substring(0, lastIndex);
            String logVoName = className.substring(lastIndex);
            indexName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, logVoName);
            indexName = canonicalPath + indexName;
        } else {
            indexName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, className);
        }
        return indexName;
    }

    /**
     * 获取日志类在指定时间戳对应的实际写入索引名。
     * <p>未注册返回 {@code null}；无时间模式返回原始名；否则返回 {@code 原始名_时间后缀}。
     *
     * @param logClass  日志类
     * @param timestamp 时间戳（毫秒）
     * @return 实际写入索引名；未注册时为 {@code null}
     */
    private String getIndex(Class<?> logClass, long timestamp) {
        IndexConfigVo configVo = regMap.get(logClass);
        if (configVo == null) {
            return null;
        }
        FastDateFormat indexPattern = configVo.getIndexPattern();
        if (indexPattern == null) {
            return configVo.getRawName();
        }
        return configVo.getRawName() + '_' + indexPattern.format(timestamp);
    }

    /**
     * 后台监控守护线程，定时（按 {@code maxFlushInMilliseconds}）或按 buffer 字节阈值
     * （{@code maxBytesOfBatch}）向 {@link #batchExecutor} 提交 flush 任务。
     */
    public class ElasticsearchDaemonExporter extends Thread {

        /**
         * 运行标记，置 false 后线程在下一轮循环退出。
         */
        private volatile boolean isRunning = false;

        /**
         * 下一次定时 flush 的时间戳。
         */
        private volatile long nextScanTime = 0;

        /**
         * 初始化运行标记为 true。
         */
        public void init() {
            isRunning = true;
        }

        /**
         * 标记线程销毁（置 isRunning=false），由 {@link #destroy()} 调用。
         */
        public void readyDestroy() {
            isRunning = false;
        }

        @Override
        public void run() {
            while (isRunning) {
                try {
                    long now = SystemClock.now();
                    boolean shouldFlush;
                    batchLock.lock();
                    try {
                        shouldFlush = buffer.size() > maxBytesOfBatch;
                    } finally {
                        batchLock.unlock();
                    }
                    if (shouldFlush || now > nextScanTime) {
                        nextScanTime = now + maxFlushInMilliseconds;
                        batchExecutor.submit(() -> processLogBuffer());
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Exception processing log entries", e);
                }
            }
        }
    }
}
