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
     */
    void changeMaxFlushInSeconds(long maxFlushInSeconds);

    /**
     * getMaxBytesOfBatch
     */
    long getMaxKiloBytesOfBatch();

    /**
     * changeMaxBytesOfBatch
     *
     */
    void changeMaxBytesOfBatch(long maxKiloBytesOfBatch);
}
