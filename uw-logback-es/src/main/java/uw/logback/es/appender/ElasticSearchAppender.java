package uw.logback.es.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import okhttp3.Credentials;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import uw.httpclient.http.HttpConfig;
import uw.httpclient.http.HttpData;
import uw.httpclient.http.HttpInterface;
import uw.httpclient.json.JsonInterfaceHelper;
import uw.httpclient.util.BufferRequestBody;
import uw.httpclient.util.MediaTypes;
import uw.logback.es.util.EncoderUtils;
import uw.logback.es.util.ThrowableProxyUtils;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Logback日志批量接收器
 */
public class ElasticSearchAppender<Event extends ILoggingEvent> extends UnsynchronizedAppenderBase<Event> implements ElasticSearchAppenderMBean {

    private static final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    /**
     * http操作接口。
     */
    private static final HttpInterface HTTP_INTERFACE = new JsonInterfaceHelper( HttpConfig.builder().retryOnConnectionFailure( true ).connectTimeout( 10_000L ).readTimeout( 10_000L ).writeTimeout( 10_000L ).build() );
    /**
     * 索引格式器
     */
    public static FastDateFormat INDEX_DATE_FORMAT;
    /**
     * 读写锁
     */
    private final Lock batchLock = new ReentrantLock();
    /**
     * Elasticsearch Web API endpoint
     */
    private String esServer;

    /**
     * Elasticsearch bulk api endpoint
     */
    private String esBulk = "/_bulk?filter_path=took,errors";

    /**
     * es用户名
     */
    private String esUsername;

    /**
     * es密码
     */
    private String esPassword;

    /**
     * 是否需要Basic Authentication
     */
    private boolean needBasicAuth;

    /**
     * ES索引
     */
    private String esIndex;

    /**
     * ES索引全名
     */
    private String esIndexFullName;

    /**
     * ES索引pattern
     */
    private String esIndexSuffix;

    /**
     * 应用主机
     */
    private String appHost;

    /**
     * 应用名称
     */
    private String appInfo;

    /**
     * 刷新Bucket时间秒数
     */
    private long maxFlushInSeconds = 10;

    /**
     * 允许最大Bucket字节数。
     */
    private long maxKiloBytesOfBatch = 8 * 1024;

    /**
     * 最大批量线程数。
     */
    private int maxBatchThreads = 5;

    /**
     * 最大批量线程队列数
     */
    private int maxBatchQueueSize = 20;

    /**
     * 异常输出最大深度。
     */
    private int maxDepthPerThrowable = 20;

    /**
     * 需要排除的异常关键字，多个关键字之间用','分割。
     */
    private String excludeThrowableKeys = "org.spring,org.apache,org.apache,java.base,jakarta,com.mysql";

    /**
     * 是否开启JMX
     */
    private boolean jmxMonitoring = false;

    /**
     * bucketList
     */
    private okio.Buffer buffer = new okio.Buffer();

    /**
     * 后台线程
     */
    private ElasticsearchDaemonExporter daemonExporter;

    /**
     * JMX注册名称
     */
    private ObjectName registeredObjectName;

    /**
     * 后台批量线程池。
     */
    private ThreadPoolExecutor batchExecutor;

    /**
     * 强制日志提交
     */
    @Override
    public void forceProcessLogBucket() {
        processLogBucket();
    }

    @Override
    public long getMaxFlushInSeconds() {
        return maxFlushInSeconds;
    }

    public void setMaxFlushInSeconds(long maxFlushInSeconds) {
        this.maxFlushInSeconds = maxFlushInSeconds;
    }

    @Override
    public void changeMaxFlushInSeconds(long maxFlushInSeconds) {
        setMaxFlushInSeconds( maxFlushInSeconds );
    }

    @Override
    public long getMaxKiloBytesOfBatch() {
        return maxKiloBytesOfBatch;
    }

    @Override
    public void changeMaxBytesOfBatch(long maxKiloBytesOfBatch) {
        setMaxKiloBytesOfBatch( maxKiloBytesOfBatch );
    }

    public void setMaxKiloBytesOfBatch(long maxKiloBytesOfBatch) {
        this.maxKiloBytesOfBatch = maxKiloBytesOfBatch;
    }

    public String getEsServer() {
        return esServer;
    }

    public void setEsServer(String esServer) {
        this.esServer = esServer;
    }

    public String getEsBulk() {
        return esBulk;
    }

    public void setEsBulk(String esBulk) {
        this.esBulk = esBulk;
    }

    public String getEsUsername() {
        return esUsername;
    }

    public void setEsUsername(String esUsername) {
        this.esUsername = esUsername;
    }

    public String getEsPassword() {
        return esPassword;
    }

    public void setEsPassword(String esPassword) {
        this.esPassword = esPassword;
    }

    public String getEsIndex() {
        return esIndex;
    }

    public void setEsIndex(String esIndex) {
        this.esIndex = esIndex;
    }

    public String getEsIndexSuffix() {
        return esIndexSuffix;
    }

    public void setEsIndexSuffix(String esIndexSuffix) {
        this.esIndexSuffix = esIndexSuffix;
    }

    public int getMaxBatchThreads() {
        return maxBatchThreads;
    }

    public void setMaxBatchThreads(int maxBatchThreads) {
        this.maxBatchThreads = maxBatchThreads;
    }

    public int getMaxBatchQueueSize() {
        return maxBatchQueueSize;
    }

    public void setMaxBatchQueueSize(int maxBatchQueueSize) {
        this.maxBatchQueueSize = maxBatchQueueSize;
    }

    public boolean isJmxMonitoring() {
        return jmxMonitoring;
    }

    public void setJmxMonitoring(boolean jmxMonitoring) {
        this.jmxMonitoring = jmxMonitoring;
    }

    public String getAppHost() {
        return appHost;
    }

    public void setAppHost(String appHost) {
        this.appHost = appHost;
    }

    public String getAppInfo() {
        return appInfo;
    }

    public void setAppInfo(String appInfo) {
        this.appInfo = appInfo;
    }

    public int getMaxDepthPerThrowable() {
        return maxDepthPerThrowable;
    }

    public void setMaxDepthPerThrowable(int maxDepthPerThrowable) {
        this.maxDepthPerThrowable = maxDepthPerThrowable;
    }

    public String getExcludeThrowableKeys() {
        return excludeThrowableKeys;
    }

    public void setExcludeThrowableKeys(String excludeThrowableKeys) {
        this.excludeThrowableKeys = excludeThrowableKeys;
    }

    @Override
    protected void append(Event event) {
        if (!isStarted()) {
            return;
        }
        //先写入一个okioBuffer，减少锁时间。
        okio.Buffer okb = null;
        try {
            okb = fillBuffer( event );
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (okb == null) {
            return;
        }
        // SegmentPool pooling
        batchLock.lock();
        try {
            buffer.writeAll( okb );
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            batchLock.unlock();
        }
    }

    @Override
    public void start() {
        if (StringUtils.isBlank( esServer )) {
            addError( "!!!No config for <esHost>!!!" );
            return;
        }
        if (StringUtils.isBlank( appInfo )) {
            addError( "!!!No elasticsearch index was configured. !!!" );
            return;
        }
        if (StringUtils.isBlank( esIndex )) {
            esIndex = appInfo;
        }
        if (StringUtils.isNotBlank( esIndexSuffix )) {
            INDEX_DATE_FORMAT = FastDateFormat.getInstance( esIndexSuffix, (TimeZone) null );
        }
        if (maxDepthPerThrowable < 10) {
            maxDepthPerThrowable = 10;
        }
        ThrowableProxyUtils.MaxDepthPerThrowable = maxDepthPerThrowable;
        if (StringUtils.isNotBlank( excludeThrowableKeys )) {
            ThrowableProxyUtils.ExcludeThrowableKeys = excludeThrowableKeys.split( "," );
        }
        if (jmxMonitoring) {
            String objectName = "uw.logback.es:type=ElasticsearchBatchAppender,name=ElasticsearchBatchAppender@" + System.identityHashCode( this );
            try {
                registeredObjectName = mbeanServer.registerMBean( this, new ObjectName( objectName ) ).getObjectName();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.needBasicAuth = StringUtils.isNotBlank( esUsername ) && StringUtils.isNotBlank( esPassword );
        batchExecutor = new ThreadPoolExecutor( 1, maxBatchThreads, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>( maxBatchQueueSize ),
                new ThreadFactoryBuilder().setDaemon( true ).setNameFormat( "logback-es-batch-%d" ).build(), new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                addError( "Logback ES Batch Task " + r.toString() + " rejected from " + executor.toString() );
            }
        } );

        daemonExporter = new ElasticsearchDaemonExporter();
        daemonExporter.setName( "logback-es-monitor" );
        daemonExporter.setDaemon( true );
        daemonExporter.init();
        daemonExporter.start();
        super.start();
    }

    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }
        if (daemonExporter != null) {
            daemonExporter.readyDestroy();
        }
        if (batchExecutor != null) {
            batchExecutor.shutdown();
        }
        forceProcessLogBucket();
        if (registeredObjectName != null) {
            try {
                mbeanServer.unregisterMBean( registeredObjectName );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.stop();
    }

    /**
     * 填充okio.buffer。
     *
     * @param event
     * @return
     * @throws IOException
     */
    private okio.Buffer fillBuffer(Event event) throws IOException {
        okio.Buffer okb = new okio.Buffer();
        okb.writeUtf8( "{\"create\":{\"_index\":\"" ).writeUtf8( esIndexFullName ).writeUtf8( "\"},\"_source\":false}" ).write( EncoderUtils.LINE_SEPARATOR_BYTES );
        okb.writeUtf8( "{\"@timestamp\":\"" ).writeUtf8( EncoderUtils.DATE_FORMAT.format( event.getTimeStamp() ) ).writeUtf8( "\"," );
        okb.writeUtf8( "\"appName\":\"" ).writeUtf8( appInfo ).writeUtf8( "\"," );
        okb.writeUtf8( "\"appHost\":\"" ).writeUtf8( appHost ).writeUtf8( "\"," );
        okb.writeUtf8( "\"level\":\"" ).writeUtf8( event.getLevel().toString() ).writeUtf8( "\"," );
        okb.writeUtf8( "\"logger\":\"" ).writeUtf8( event.getLoggerName() ).writeUtf8( "\"," );
        okb.writeUtf8( "\"message\":\"" ).writeUtf8( EncoderUtils.escapeJSON( event.getFormattedMessage() ) ).writeUtf8( "\"," );
        okb.writeUtf8( "\"thread\":\"" ).writeUtf8( event.getThreadName() ).writeUtf8( "\"" );
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            okb.writeUtf8( ",\"stack_trace\":\"" );
            ThrowableProxyUtils.writeThrowable( okb, throwableProxy );
            okb.writeUtf8( "\"" );
        }
        okb.writeUtf8( "}" );
        okb.write( EncoderUtils.LINE_SEPARATOR_BYTES );
        return okb;
    }

    /**
     * 处理索引
     *
     * @return
     */
    private void calcIndexName() {
        if (INDEX_DATE_FORMAT == null) {
            esIndexFullName = esIndex;
        } else {
            esIndexFullName = esIndex + '_' + INDEX_DATE_FORMAT.format( System.currentTimeMillis() );
        }
    }

    /**
     * Send buffer to Elasticsearch
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
            Request.Builder requestBuilder = new Request.Builder().url( esServer + getEsBulk() );
            if (needBasicAuth) {
                requestBuilder.header( "Authorization", Credentials.basic( esUsername, esPassword ) );
            }
            HttpData httpData = HTTP_INTERFACE.requestForData( requestBuilder.post( BufferRequestBody.create( bufferData, MediaTypes.JSON_UTF8 ) ).build() );
            if (httpData.getStatusCode() != 200) {
                System.err.println( "Logback ES Batch process error! code:" + httpData.getStatusCode() + ", response: " + httpData.getResponseData() );
            }
            System.out.println( "Logback ES Batch process success! " + httpData.getResponseData());
        } catch (Exception e) {
            //直接打印到控制台输出吧
            e.printStackTrace();
        }
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
                    //重算索引名。
                    calcIndexName();
                    if (buffer.size() >> 10 > maxKiloBytesOfBatch || System.currentTimeMillis() > nextScanTime) {
                        nextScanTime = System.currentTimeMillis() + maxFlushInSeconds * 1000;
                        batchExecutor.submit( new Runnable() {
                            @Override
                            public void run() {
                                processLogBucket();
                            }
                        } );
                    }
                    Thread.sleep( 500 );
                } catch (Exception e) {
                    //直接打印到控制台输出吧
                    e.printStackTrace();
                }
            }
        }
    }
}
