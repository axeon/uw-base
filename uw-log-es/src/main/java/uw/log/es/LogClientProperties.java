package uw.log.es;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 日志接口服务客户端属性配置类。
 * <p>配置前缀 {@code uw.log}，其下 {@code es} 子节点承载 Elasticsearch 连接与写入参数。
 * 通过 {@link LogClientAutoConfiguration} 的 {@code @EnableConfigurationProperties} 自动注册为 Bean。
 */
@ConfigurationProperties(prefix = "uw.log")
public class LogClientProperties {

    /**
     * ES主机配置
     */
    private EsConfig es = new EsConfig();

    /**
     * ES主机配置（连接、认证、写入参数）。
     */
    public static class EsConfig {

        /**
         * 连接超时（毫秒）
         */
        private long connectTimeout = 30000;

        /**
         * 读超时（毫秒）
         */
        private long readTimeout = 30000;

        /**
         * 写超时（毫秒）
         */
        private long writeTimeout = 30000;

        /**
         * 用户名（Http Basic 认证，与 password 同时配置时生效）
         */
        private String username;

        /**
         * 密码（Http Basic 认证，与 username 同时配置时生效）
         */
        private String password;

        /**
         * ES集群HTTP REST地址；为空时不写入日志，仅保留查询能力。
         */
        private String server = null;

        /**
         * Elasticsearch bulk api 地址（相对 server 的 path）。
         */
        private String esBulk = "/_bulk?filter_path=took,errors";

        /**
         * 是否用应用信息覆写日志体中的 appInfo/appHost。
         */
        private boolean appInfoOverwrite = true;

        /**
         * READ_ONLY: 只读模式; READ_WRITE: 读写模式[会有后台线程开销]
         */
        private LogMode mode = LogMode.READ_WRITE;

        /**
         * 后台批量提交的刷新间隔（秒）。
         */
        private long maxFlushInSeconds = 10L;

        /**
         * 触发立即 flush 的 buffer 阈值（KB）。
         */
        private long maxKiloBytesOfBatch = 8 * 1024;

        /**
         * 批量提交线程池的最大线程数。
         */
        private int maxBatchThreads = 5;

        /**
         * 批量提交线程池的队列容量。
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

    /**
     * 日志工作模式。
     */
    public enum LogMode {
        /**
         * 只读模式：不启动后台写入链路，仅支持查询。
         */
        READ_ONLY,
        /**
         * 读写模式：启动批量写入守护线程与线程池，支持写入与查询。
         */
        READ_WRITE
    }

    public EsConfig getEs() {
        return es;
    }

    public void setEs(EsConfig es) {
        this.es = es;
    }
}
