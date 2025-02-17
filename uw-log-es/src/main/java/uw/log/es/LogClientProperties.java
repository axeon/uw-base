package uw.log.es;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 日志接口服务客户端属性配置类
 */
@Configuration
@ConfigurationProperties(prefix = "uw.log")
public class LogClientProperties {

    private EsConfig es = new EsConfig();

    /**
     * ES主机配置
     */
    public static class EsConfig {

        /**
         * 连接超时
         */
        private long connectTimeout = 30000;

        /**
         * 读超时
         */
        private long readTimeout = 30000;

        /**
         * 写超时
         */
        private long writeTimeout = 30000;

        /**
         * 用户名
         */
        private String username;

        /**
         * 密码
         */
        private String password;

        /**
         * ES集群HTTP REST地址
         */
        private String server = null;

        /**
         * Elasticsearch bulk api 地址
         */
        private String esBulk = "/_bulk?filter_path=took,errors";

        /**
         * 是否添加执行应用信息
         */
        private boolean appInfoOverwrite = true;

        /**
         * READ_ONLY: 只读模式; READ_WRITE: 读写模式[会有后台线程开销]
         */
        private LogMode mode = LogMode.READ_WRITE;

        /**
         * 刷新Bucket时间秒数.
         */
        private long maxFlushInSeconds = 10L;

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

        public long getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public long getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(long readTimeout) {
            this.readTimeout = readTimeout;
        }

        public long getWriteTimeout() {
            return writeTimeout;
        }

        public void setWriteTimeout(long writeTimeout) {
            this.writeTimeout = writeTimeout;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getServer() {
            return server;
        }

        public void setServer(String server) {
            this.server = server;
        }

        public String getEsBulk() {
            return esBulk;
        }

        public void setEsBulk(String esBulk) {
            this.esBulk = esBulk;
        }

        public boolean isAppInfoOverwrite() {
            return appInfoOverwrite;
        }

        public void setAppInfoOverwrite(boolean appInfoOverwrite) {
            this.appInfoOverwrite = appInfoOverwrite;
        }

        public LogMode getMode() {
            return mode;
        }

        public void setMode(LogMode mode) {
            this.mode = mode;
        }

        public long getMaxFlushInSeconds() {
            return maxFlushInSeconds;
        }

        public void setMaxFlushInSeconds(long maxFlushInSeconds) {
            this.maxFlushInSeconds = maxFlushInSeconds;
        }

        public long getMaxKiloBytesOfBatch() {
            return maxKiloBytesOfBatch;
        }

        public void setMaxKiloBytesOfBatch(long maxKiloBytesOfBatch) {
            this.maxKiloBytesOfBatch = maxKiloBytesOfBatch;
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

    }

    public enum LogMode {
        READ_ONLY, READ_WRITE
    }

    public EsConfig getEs() {
        return es;
    }

    public void setEs(EsConfig es) {
        this.es = es;
    }
}
