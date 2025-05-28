package uw.logback.es.appender;

/**
 * ElasticSearchAppender JMX Bean
 */
public interface ElasticSearchAppenderMBean {
    /**
     * 强制日志提交
     */
    void forceProcessLogBucket();

    /**
     * 获取刷新Bucket时间秒数
     */
    long getMaxFlushInSeconds();

    /**
     * 设置刷新Bucket时间秒数
     *
     * @param maxFlushInSeconds
     */
    void setMaxFlushInSeconds(long maxFlushInSeconds);

    /**
     * 获取允许最大Bucket字节数。
     */
    long getMaxKiloBytesOfBatch();

    /**
     * 设置允许最大Bucket字节数。
     *
     * @param maxKiloBytesOfBatch
     */
    void setMaxKiloBytesOfBatch(long maxKiloBytesOfBatch);

    /**
     * 获取Elasticsearch Web API endpoint
     *
     * @return
     */
    String getEsServer();

    /**
     * 获取Elasticsearch bulk api endpoint
     *
     * @return
     */
    String getEsBulk();

    /**
     * 获取es用户名
     *
     * @return
     */
    String getEsUsername();

    /**
     * 获取ES索引
     *
     * @return
     */
    String getEsIndex();

    /**
     * 获取ES索引后缀
     *
     * @return
     */
    String getEsIndexSuffix();

    /**
     * 获取批量线程数
     *
     * @return
     */
    int getMaxBatchThreads();

    /**
     * 获取批量线程队列数
     *
     * @return
     */
    int getMaxBatchQueueSize();

    /**
     * 获取应用主机名
     *
     * @return
     */
    String getAppHost();

    /**
     * 获取应用信息
     *
     * @return
     */
    String getAppInfo();

    /**
     * 获取Throwable最大堆栈深度
     *
     * @return
     */
    int getMaxDepthPerThrowable();

    /**
     * 获取Throwable排除的key
     *
     * @return
     */
    String getExcludeThrowableKeys();

}
