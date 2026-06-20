package uw.logback.es.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import okhttp3.Credentials;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import uw.common.util.SystemClock;
import uw.httpclient.http.HttpConfig;
import uw.httpclient.http.HttpData;
import uw.httpclient.http.HttpInterface;
import uw.httpclient.json.JsonInterfaceHelper;
import uw.httpclient.util.BufferRequestBody;
import uw.httpclient.util.MediaTypes;
import uw.httpclient.util.SSLContextUtils;
import uw.logback.es.util.JsonEncoderUtils;
import uw.logback.es.util.ThrowableProxyUtils;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于 Logback 的 Elasticsearch 批量日志 Appender。
 * <p>
 * 直接通过 ES 的 {@code /_bulk} 接口批量写入日志，省去 Logstash 中转。日志先写入内存中的
 * {@link okio.Buffer}，由后台监控线程 {@link ElasticsearchDaemonExporter} 周期性或在 buffer
 * 达到阈值时，把数据提交到线程池 {@link #batchExecutor} 执行 flush。
 * <p>
 * <b>线程模型</b>：
 * <ul>
 *   <li>业务线程在 {@link #append} 中先把单条日志编码到一个临时 buffer，再加锁 {@link #batchLock}
 *       追加到全局 {@link #buffer}，锁持有时间极短；</li>
 *   <li>后台监控线程负责判断触发条件并提交 flush 任务；</li>
 *   <li>flush 在批量线程池中执行，HTTP 请求阻塞调用。</li>
 * </ul>
 * <p>
 * <b>属性注入</b>：logback 按 setter 名注入，XML 标签名需与 {@code setXxx} 的 {@code xxx} 一致
 * （例如 {@code <esServer>} 对应 {@link #setEsServer}）。
 *
 * @param <Event> 日志事件类型，约束为 {@link ILoggingEvent}
 * @see ElasticSearchAppenderMBean
 */
public class ElasticSearchAppender<Event extends ILoggingEvent> extends UnsynchronizedAppenderBase<Event> implements ElasticSearchAppenderMBean {

    /**
     * 平台 MBean Server，用于 JMX 注册/注销。
     */
    private static final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    /**
     * 全局共享的 HTTP 接口实例。
     * <p>
     * 配置为：信任全部证书 + 连接/读/写各 10s 超时 + 连接失败重试。因 appender 实例通常全局唯一，
     * 用 static 共享以复用连接池与 Dispatcher。
     */
    private static final HttpInterface HTTP_INTERFACE = new JsonInterfaceHelper(HttpConfig.builder().retryOnConnectionFailure(true).connectTimeout(10_000L).readTimeout(10_000L).writeTimeout(10_000L).trustManager(SSLContextUtils.getTrustAllManager()).sslSocketFactory(SSLContextUtils.getTruestAllSocketFactory()).hostnameVerifier((hostName, sslSession) -> true).build());

    /**
     * 索引名时间格式化器，仅当配置了 {@link #esIndexSuffix} 时初始化。
     */
    private FastDateFormat indexDateFormat;

    /**
     * 保护全局 {@link #buffer} 读写的互斥锁。
     */
    private final Lock batchLock = new ReentrantLock();

    /**
     * ES Web API 基础地址，如 {@code http://localhost:9200}。必填。
     */
    private String esServer;

    /**
     * ES bulk API 路径，拼接到 {@link #esServer} 之后。默认带 {@code filter_path} 仅返回关键字段。
     */
    private String esBulk = "/_bulk?filter_path=took,errors";

    /**
     * ES Basic 认证用户名，与 {@link #esPassword} 同时配置才生效。
     */
    private String esUsername;

    /**
     * ES Basic 认证密码，与 {@link #esUsername} 同时配置才生效。
     */
    private String esPassword;

    /**
     * 是否启用 Basic 认证，由 {@link #start()} 根据 username/password 是否非空推导。
     */
    private boolean needBasicAuth;

    /**
     * 索引名（不含时间后缀）。为空时在 {@link #start()} 中默认取 {@link #appInfo}。
     */
    private String esIndex;

    /**
     * 索引全名（含时间后缀），由 {@link #calcIndexName()} 周期性重算。
     */
    private String esIndexFullName;

    /**
     * 索引时间后缀格式（如 {@code _yyyy-MM-dd}），支持 FastDateFormat 模式。
     */
    private String esIndexSuffix;

    /**
     * 应用主机标识，写入日志 {@code appHost} 字段。
     */
    private String appHost;

    /**
     * 应用名称，写入日志 {@code appInfo} 字段；{@link #esIndex} 为空时同时作为索引名。必填。
     */
    private String appInfo;

    /**
     * 定时 flush 间隔，单位：秒。buffer 未达字节阈值时，每隔此时长触发一次 flush。
     * 小于 1 会在 {@link #start()} 中被提升到 1，避免配置为 0/负数导致频繁 flush 或关机不等待。
     */
    private long maxFlushInSeconds = 10;

    /**
     * 批量提交触发阈值，单位：KB（千字节）。buffer 累积字节数 {@code >> 10}（即除以 1024）
     * 后超过该值即触发 flush。默认 {@code 8 * 1024}（即 8MB）；小于 1 会在 {@link #start()} 中被提升到 1。
     */
    private long maxKiloBytesOfBatch = 8 * 1024;

    /**
     * 批量 flush 线程池最大线程数。
     */
    private int maxBatchThreads = 5;

    /**
     * 批量 flush 线程池任务队列容量，满后新任务被拒绝（仅记录错误）。
     */
    private int maxBatchQueueSize = 20;

    /**
     * 异常堆栈输出最大深度（行数），小于 10 会在 {@link #start()} 中被提升到 10。
     */
    private int maxDepthPerThrowable = 20;

    /**
     * 需要折叠（合并计数后省略）的堆栈类名前缀，多个关键字之间用 {@code ','} 分割。
     */
    private String excludeThrowableKeys = "java.base,org.spring,jakarta,org.apache,com.mysql,okhttp,com.fasterxml,uw.auth.service.filter";

    /**
     * 是否注册 JMX MBean 以便运行期观察/调整配置。
     */
    private boolean jmxMonitoring = false;

    /**
     * 全局待 flush 的日志缓冲区，由 {@link #batchLock} 保护并发访问。
     */
    private okio.Buffer buffer = new okio.Buffer();

    /**
     * 后台监控线程，负责周期性判断 flush 条件并提交任务。
     */
    private ElasticsearchDaemonExporter daemonExporter;

    /**
     * JMX 注册成功后返回的 ObjectName，用于 {@link #stop()} 时注销；未注册或失败时为 null。
     */
    private ObjectName registeredObjectName;

    /**
     * 批量 flush 线程池，在 {@link #start()} 中创建、{@link #stop()} 中关闭。
     */
    private ThreadPoolExecutor batchExecutor;

    /**
     * 强制把当前 buffer 中的日志同步提交一次（JMX 可调用）。
     * <p>
     * 注意：调用方线程会阻塞直到本次 HTTP bulk 请求完成。
     */
    @Override
    public void forceProcessLogBucket() {
        processLogBucket();
    }

    /**
     * 获取定时 flush 间隔。
     *
     * @return 间隔秒数
     */
    @Override
    public long getMaxFlushInSeconds() {
        return maxFlushInSeconds;
    }

    /**
     * 设置定时 flush 间隔。
     *
     * @param maxFlushInSeconds 间隔秒数
     */
    @Override
    public void setMaxFlushInSeconds(long maxFlushInSeconds) {
        this.maxFlushInSeconds = maxFlushInSeconds;
    }

    /**
     * 获取批量提交触发阈值。
     *
     * @return 阈值（单位：KB）
     */
    @Override
    public long getMaxKiloBytesOfBatch() {
        return maxKiloBytesOfBatch;
    }

    /**
     * 设置批量提交触发阈值。
     *
     * @param maxKiloBytesOfBatch 阈值（单位：KB）
     */
    @Override
    public void setMaxKiloBytesOfBatch(long maxKiloBytesOfBatch) {
        this.maxKiloBytesOfBatch = maxKiloBytesOfBatch;
    }

    /**
     * 获取 ES 服务地址。
     *
     * @return ES 基础地址
     */
    @Override
    public String getEsServer() {
        return esServer;
    }

    /**
     * 设置 ES 服务地址。
     *
     * @param esServer ES 基础地址，如 {@code http://localhost:9200}
     */
    public void setEsServer(String esServer) {
        this.esServer = esServer;
    }

    /**
     * 获取 ES bulk 接口路径。
     *
     * @return bulk 路径
     */
    @Override
    public String getEsBulk() {
        return esBulk;
    }

    /**
     * 设置 ES bulk 接口路径。
     *
     * @param esBulk bulk 路径
     */
    public void setEsBulk(String esBulk) {
        this.esBulk = esBulk;
    }

    /**
     * 获取 ES 认证用户名。
     *
     * @return 用户名
     */
    @Override
    public String getEsUsername() {
        return esUsername;
    }

    /**
     * 设置 ES 认证用户名。
     *
     * @param esUsername 用户名
     */
    public void setEsUsername(String esUsername) {
        this.esUsername = esUsername;
    }

    /**
     * 获取 ES 认证密码。
     *
     * @return 密码
     */
    public String getEsPassword() {
        return esPassword;
    }

    /**
     * 设置 ES 认证密码。
     *
     * @param esPassword 密码
     */
    public void setEsPassword(String esPassword) {
        this.esPassword = esPassword;
    }

    /**
     * 获取索引名。
     *
     * @return 索引名
     */
    @Override
    public String getEsIndex() {
        return esIndex;
    }

    /**
     * 设置索引名。
     *
     * @param esIndex 索引名
     */
    public void setEsIndex(String esIndex) {
        this.esIndex = esIndex;
    }

    /**
     * 获取索引时间后缀格式。
     *
     * @return 后缀格式串
     */
    @Override
    public String getEsIndexSuffix() {
        return esIndexSuffix;
    }

    /**
     * 设置索引时间后缀格式。
     *
     * @param esIndexSuffix 后缀格式串，如 {@code _yyyy-MM-dd}
     */
    public void setEsIndexSuffix(String esIndexSuffix) {
        this.esIndexSuffix = esIndexSuffix;
    }

    /**
     * 获取批量 flush 线程池最大线程数。
     *
     * @return 线程数
     */
    @Override
    public int getMaxBatchThreads() {
        return maxBatchThreads;
    }

    /**
     * 设置批量 flush 线程池最大线程数。
     *
     * @param maxBatchThreads 线程数
     */
    public void setMaxBatchThreads(int maxBatchThreads) {
        this.maxBatchThreads = maxBatchThreads;
    }

    /**
     * 获取批量 flush 线程池队列容量。
     *
     * @return 队列容量
     */
    @Override
    public int getMaxBatchQueueSize() {
        return maxBatchQueueSize;
    }

    /**
     * 设置批量 flush 线程池队列容量。
     *
     * @param maxBatchQueueSize 队列容量
     */
    public void setMaxBatchQueueSize(int maxBatchQueueSize) {
        this.maxBatchQueueSize = maxBatchQueueSize;
    }

    /**
     * 是否开启 JMX 监控。
     *
     * @return true 表示开启
     */
    public boolean isJmxMonitoring() {
        return jmxMonitoring;
    }

    /**
     * 设置是否开启 JMX 监控。
     *
     * @param jmxMonitoring 是否开启
     */
    public void setJmxMonitoring(boolean jmxMonitoring) {
        this.jmxMonitoring = jmxMonitoring;
    }

    /**
     * 获取应用主机标识。
     *
     * @return 主机标识
     */
    @Override
    public String getAppHost() {
        return appHost;
    }

    /**
     * 设置应用主机标识。
     *
     * @param appHost 主机标识
     */
    public void setAppHost(String appHost) {
        this.appHost = appHost;
    }

    /**
     * 获取应用名称。
     *
     * @return 应用名称
     */
    @Override
    public String getAppInfo() {
        return appInfo;
    }

    /**
     * 设置应用名称。
     *
     * @param appInfo 应用名称
     */
    public void setAppInfo(String appInfo) {
        this.appInfo = appInfo;
    }

    /**
     * 获取异常堆栈输出最大深度。
     *
     * @return 深度行数
     */
    @Override
    public int getMaxDepthPerThrowable() {
        return maxDepthPerThrowable;
    }

    /**
     * 设置异常堆栈输出最大深度。
     *
     * @param maxDepthPerThrowable 深度行数，小于 10 会在 {@link #start()} 中被提升到 10
     */
    public void setMaxDepthPerThrowable(int maxDepthPerThrowable) {
        this.maxDepthPerThrowable = maxDepthPerThrowable;
    }

    /**
     * 获取被折叠的堆栈类名前缀列表（逗号分隔字符串）。
     *
     * @return 前缀串
     */
    @Override
    public String getExcludeThrowableKeys() {
        return excludeThrowableKeys;
    }

    /**
     * 设置被折叠的堆栈类名前缀列表。
     *
     * @param excludeThrowableKeys 逗号分隔的前缀串
     */
    public void setExcludeThrowableKeys(String excludeThrowableKeys) {
        this.excludeThrowableKeys = excludeThrowableKeys;
    }

    /**
     * 追加单条日志：先编码到临时 buffer，再加锁追加到全局 {@link #buffer}。
     * <p>
     * 临时 buffer 编码在锁外完成，最大化降低锁持有时间。
     *
     * @param event 日志事件
     */
    @Override
    protected void append(Event event) {
        if (!isStarted()) {
            return;
        }
        //先写入一个okioBuffer，减少锁时间。
        okio.Buffer okb = null;
        try {
            okb = fillBuffer(event);
        } catch (Exception e) {
            addError("Failed to fill buffer", e);
        }

        if (okb == null) {
            return;
        }
        batchLock.lock();
        try {
            buffer.writeAll(okb);
        } catch (Exception e) {
            addError("Failed to write buffer", e);
        } finally {
            batchLock.unlock();
        }
    }

    /**
     * 启动 appender：校验必填项、初始化索引格式器与全局堆栈参数、注册 JMX、创建线程池并启动监控线程。
     * <p>
     * 必填项（{@link #esServer}、{@link #appInfo}）缺失时仅记录错误并直接返回，不调用 {@code super.start()}。
     */
    @Override
    public void start() {
        if (StringUtils.isBlank(esServer)) {
            addError("!!!No config for <esServer>!!!");
            return;
        }
        if (StringUtils.isBlank(appInfo)) {
            addError("!!!No appInfo was configured. !!!");
            return;
        }
        if (StringUtils.isBlank(esIndex)) {
            esIndex = appInfo;
        }
        if (StringUtils.isNotBlank(esIndexSuffix)) {
            indexDateFormat = FastDateFormat.getInstance(esIndexSuffix, (TimeZone) null);
        }
        if (maxDepthPerThrowable < 10) {
            maxDepthPerThrowable = 10;
        }
        if (maxFlushInSeconds < 1) {
            maxFlushInSeconds = 1;
        }
        if (maxKiloBytesOfBatch < 1) {
            maxKiloBytesOfBatch = 1;
        }
        ThrowableProxyUtils.MaxDepthPerThrowable = maxDepthPerThrowable;
        // 预热索引全名,避免 daemon 线程首次 calcIndexName 之前到达的日志写入 index="null"。
        calcIndexName();
        if (StringUtils.isNotBlank(excludeThrowableKeys)) {
            // 以 "," 切分并剔除空白项，避免末尾逗号产生的空前缀（startsWith("") 会匹配所有堆栈帧导致全部被折叠）。
            ThrowableProxyUtils.ExcludeThrowableKeys = Arrays.stream(excludeThrowableKeys.split(","))
                    .map(String::trim).filter(StringUtils::isNotBlank).toArray(String[]::new);
        }
        if (jmxMonitoring) {
            String objectName = "uw.logback.es:type=ElasticsearchAppender,name=ElasticsearchAppender@" + System.identityHashCode(this);
            try {
                registeredObjectName = mbeanServer.registerMBean(this, new ObjectName(objectName)).getObjectName();
            } catch (Exception e) {
                addError("Failed to register JMX MBean", e);
            }
        }
        this.needBasicAuth = StringUtils.isNotBlank(esUsername) && StringUtils.isNotBlank(esPassword);
        batchExecutor = new ThreadPoolExecutor(1, maxBatchThreads, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(maxBatchQueueSize),
                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("logback-es-batch-%d").build(), (r, executor) -> addError("Logback ES Batch Task " + r.toString() + " rejected from " + executor.toString()));

        daemonExporter = new ElasticsearchDaemonExporter();
        daemonExporter.setName("logback-es-monitor");
        daemonExporter.setDaemon(true);
        daemonExporter.init();
        daemonExporter.start();
        super.start();
    }

    /**
     * 停止 appender：先停监控线程、关闭并等待批量线程池、同步 flush 残余日志，最后注销 JMX。
     */
    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }
        // 先停止监控线程,避免停止过程中继续向已关闭的 executor 提交任务。
        if (daemonExporter != null) {
            daemonExporter.readyDestroy();
            daemonExporter.interrupt();
        }
        // 关闭批量线程池,并等待已提交的 flush 任务完成,尽量减少关机时丢日志。
        if (batchExecutor != null) {
            batchExecutor.shutdown();
            try {
                if (!batchExecutor.awaitTermination(maxFlushInSeconds, TimeUnit.SECONDS)) {
                    batchExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                batchExecutor.shutdownNow();
            }
        }
        // 同步把残余 buffer 中的日志提交一次。
        forceProcessLogBucket();
        if (registeredObjectName != null) {
            try {
                mbeanServer.unregisterMBean(registeredObjectName);
            } catch (Exception e) {
                addError("Failed to unregister JMX MBean", e);
            }
        }
        super.stop();
    }

    /**
     * 将单条日志事件编码为 ES bulk NDJSON 的两行（action 行 + source 行），写入新 buffer 返回。
     * <p>
     * 所有 JSON 字段值均经过 {@link JsonEncoderUtils#escapeJSON(String)} 转义，避免破坏 NDJSON 结构。
     *
     * @param event 日志事件
     * @return 装载单条日志 NDJSON 的 buffer
     * @throws IOException 写入 buffer 失败
     */
    private okio.Buffer fillBuffer(Event event) throws IOException {
        okio.Buffer okb = new okio.Buffer();
        okb.writeUtf8("{\"create\":{\"_index\":\"").writeUtf8(JsonEncoderUtils.escapeJSON(esIndexFullName)).writeUtf8("\"},\"_source\":false}").write(JsonEncoderUtils.LINE_SEPARATOR_BYTES);
        okb.writeUtf8("{\"@timestamp\":\"").writeUtf8(JsonEncoderUtils.DATE_FORMAT.format(event.getTimeStamp())).writeUtf8("\",");
        okb.writeUtf8("\"appInfo\":\"").writeUtf8(JsonEncoderUtils.escapeJSON(appInfo)).writeUtf8("\",");
        okb.writeUtf8("\"appHost\":\"").writeUtf8(JsonEncoderUtils.escapeJSON(appHost)).writeUtf8("\",");
        okb.writeUtf8("\"level\":\"").writeUtf8(JsonEncoderUtils.escapeJSON(event.getLevel().toString())).writeUtf8("\",");
        okb.writeUtf8("\"logger\":\"").writeUtf8(JsonEncoderUtils.escapeJSON(event.getLoggerName())).writeUtf8("\",");
        okb.writeUtf8("\"message\":\"").writeUtf8(JsonEncoderUtils.escapeJSON(event.getFormattedMessage())).writeUtf8("\",");
        okb.writeUtf8("\"thread\":\"").writeUtf8(JsonEncoderUtils.escapeJSON(event.getThreadName())).writeUtf8("\"");
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            okb.writeUtf8(",\"stack_trace\":\"");
            ThrowableProxyUtils.writeThrowable(okb, throwableProxy);
            okb.writeUtf8("\"");
        }
        okb.writeUtf8("}");
        okb.write(JsonEncoderUtils.LINE_SEPARATOR_BYTES);
        return okb;
    }

    /**
     * 根据当前时间重算 {@link #esIndexFullName}。无后缀格式器时直接取 {@link #esIndex}。
     */
    private void calcIndexName() {
        if (indexDateFormat == null) {
            esIndexFullName = esIndex;
        } else {
            esIndexFullName = esIndex + '_' + indexDateFormat.format(SystemClock.now());
        }
    }

    /**
     * 把当前 {@link #buffer} 中的数据一次性 flush 到 ES bulk 接口。
     * <p>
     * 先加锁切走 buffer 引用（立即解锁），再做 HTTP 请求；请求失败仅记录错误，本批数据会丢失。
     */
    private void processLogBucket() {
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
            Request.Builder requestBuilder = new Request.Builder().url(esServer + getEsBulk());
            if (needBasicAuth) {
                requestBuilder.header("Authorization", Credentials.basic(esUsername, esPassword));
            }
            HttpData httpData = HTTP_INTERFACE.requestForData(requestBuilder.post(BufferRequestBody.create(bufferData, MediaTypes.JSON_UTF8)).build());
            if (httpData.getStatusCode() != 200) {
                addError("Logback ES Batch process error! code:" + httpData.getStatusCode() + ", response: " + httpData.getResponseData());
            }
        } catch (Exception e) {
            addError("Logback ES Batch process exception", e);
        }
    }

    /**
     * 后台监控线程：周期性（默认 500ms）重算索引名、判断 flush 条件，
     * 并把 flush 任务提交到 {@link #batchExecutor}。
     * <p>
     * flush 触发条件二选一：buffer 达到 {@link #maxKiloBytesOfBatch}（KB），或距上次 flush 超过 {@link #maxFlushInSeconds} 秒。
     */
    public class ElasticsearchDaemonExporter extends Thread {

        /**
         * 运行标记，由 {@link #init()} 置 true、{@link #readyDestroy()} 置 false 退出主循环。
         */
        private volatile boolean isRunning = false;

        /**
         * 下一次定时 flush 的触发时间戳（毫秒）。
         */
        private volatile long nextScanTime = 0;

        /**
         * 设置运行标记为 true，应在 {@link #start()} 线程启动前调用。
         */
        public void init() {
            isRunning = true;
        }

        /**
         * 设置运行标记为 false，主循环在下次检查时退出（配合 {@link #interrupt()} 立即打断 sleep）。
         */
        public void readyDestroy() {
            isRunning = false;
        }

        /**
         * 主循环：重算索引名 → 判断是否 flush → 提交任务 → sleep 500ms，直到 {@link #isRunning} 为 false 或被中断。
         */
        @Override
        public void run() {
            while (isRunning) {
                try {
                    //重算索引名。
                    calcIndexName();
                    boolean shouldFlush;
                    batchLock.lock();
                    try {
                        shouldFlush = buffer.size() >> 10 > maxKiloBytesOfBatch;
                    } finally {
                        batchLock.unlock();
                    }
                    if (shouldFlush || SystemClock.now() > nextScanTime) {
                        nextScanTime = SystemClock.now() + maxFlushInSeconds * 1000;
                        batchExecutor.submit(() -> processLogBucket());
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } catch (Exception e) {
                    addError("Exception in logback-es monitor", e);
                }
            }
        }
    }
}
