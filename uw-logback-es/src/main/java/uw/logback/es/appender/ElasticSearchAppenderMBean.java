package uw.logback.es.appender;

/**
 * {@link ElasticSearchAppender} 的 JMX 暴露接口。
 * <p>
 * 当 {@link ElasticSearchAppender#isJmxMonitoring()} 为 true 时，appender 会把自身注册到平台
 * MBean Server，运维可通过 JMX 客户端实时读取/调整关键参数，并强制触发 flush。
 * <p>
 * 暴露内容包含：ES 地址与索引配置、批量与 flush 参数、应用标识、堆栈压缩配置。
 *
 * @see ElasticSearchAppender
 */
public interface ElasticSearchAppenderMBean {
    /**
     * 强制把当前 buffer 中的日志同步提交一次（阻塞当前调用线程）。
     */
    void forceProcessLogBucket();

    /**
     * 获取定时 flush 间隔。
     *
     * @return 间隔秒数
     */
    long getMaxFlushInSeconds();

    /**
     * 设置定时 flush 间隔。
     *
     * @param maxFlushInSeconds 间隔秒数
     */
    void setMaxFlushInSeconds(long maxFlushInSeconds);

    /**
     * 获取批量提交触发阈值。
     *
     * @return 阈值（单位：KB）
     */
    long getMaxKiloBytesOfBatch();

    /**
     * 设置批量提交触发阈值。
     *
     * @param maxKiloBytesOfBatch 阈值（单位：KB）
     */
    void setMaxKiloBytesOfBatch(long maxKiloBytesOfBatch);

    /**
     * 获取 ES 服务地址。
     *
     * @return ES 基础地址
     */
    String getEsServer();

    /**
     * 获取 ES bulk 接口路径。
     *
     * @return bulk 路径
     */
    String getEsBulk();

    /**
     * 获取 ES 认证用户名。
     *
     * @return 用户名
     */
    String getEsUsername();

    /**
     * 获取索引名。
     *
     * @return 索引名
     */
    String getEsIndex();

    /**
     * 获取索引时间后缀格式。
     *
     * @return 后缀格式串
     */
    String getEsIndexSuffix();

    /**
     * 获取批量 flush 线程池最大线程数。
     *
     * @return 线程数
     */
    int getMaxBatchThreads();

    /**
     * 获取批量 flush 线程池队列容量。
     *
     * @return 队列容量
     */
    int getMaxBatchQueueSize();

    /**
     * 获取应用主机标识。
     *
     * @return 主机标识
     */
    String getAppHost();

    /**
     * 获取应用名称。
     *
     * @return 应用名称
     */
    String getAppInfo();

    /**
     * 获取异常堆栈输出最大深度。
     *
     * @return 深度行数
     */
    int getMaxDepthPerThrowable();

    /**
     * 获取被折叠的堆栈类名前缀列表（逗号分隔字符串）。
     *
     * @return 前缀串
     */
    String getExcludeThrowableKeys();

}
