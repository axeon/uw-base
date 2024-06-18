package uw.logback.es.appender;

/**
 * ElasticSearchAppender JMX Bean
 */
public interface ElasticSearchAppenderMBean {

    /**
     * force processLogEntries
     */
    void forceProcessLogBucket();

    /**
     * getMaxFlushInMilliseconds
     */
    long getMaxFlushInSeconds();

    /**
     * changeMaxFlushInMilliseconds
     *
     * @param maxFlushInMilliseconds
     */
    void changeMaxFlushInSeconds(long maxFlushInMilliseconds);

    /**
     * getMaxBytesOfBatch
     */
    long getMaxKiloBytesOfBatch();

    /**
     * changeMaxBytesOfBatch
     *
     * @param maxKiloBytesOfBatch
     */
    void changeMaxBytesOfBatch(long maxKiloBytesOfBatch);
}
