package uw.webot.proxy;

/**
 * 代理服务接口。
 * 定义代理池管理和代理应用的核心方法。
 *
 * @author axeon
 * @since 1.0.0
 */
public interface ProxyService {

    /**
     * 获取指定类型的代理。
     *
     * @param proxyType 代理类型
     * @return 代理信息
     */
    ProxyInfo getProxy(ProxyType proxyType);

    /**
     * 释放代理。
     *
     * @param proxyInfo 代理信息
     */
    void releaseProxy(ProxyInfo proxyInfo);

    /**
     * 标记代理为失败。
     *
     * @param proxyInfo 代理信息
     */
    void markProxyFailed(ProxyInfo proxyInfo);

    /**
     * 获取代理池统计信息。
     *
     * @return 统计信息
     */
    ProxyPoolStatistics getStatistics();

    /**
     * 检查代理是否可用。
     *
     * @param proxyInfo 代理信息
     * @return 是否可用
     */
    boolean checkProxyHealth(ProxyInfo proxyInfo);

    /**
     * 关闭代理服务。
     */
    void shutdown();

    /**
     * 代理信息。
     */
    class ProxyInfo {

        /**
         * 代理服务器信息。
         */
        private ProxyConfig.ProxyServer proxyServer;

        /**
         * 权重。
         */
        private final int weight;

        /**
         * 失败次数。
         */
        private volatile int failureCount;

        /**
         * 最后使用时间。
         */
        private volatile long lastUsedTime;

        /**
         * 上次健康检查时间戳（毫秒）。
         */
        private volatile long lastHealthCheckTime;

        /**
         * 上次健康检查结果。
         */
        private volatile boolean lastHealthCheckResult;

        public ProxyInfo(ProxyConfig.ProxyServer proxyServer, int weight, int failureCount, long lastUsedTime) {
            this.proxyServer = proxyServer;
            this.weight = weight;
            this.failureCount = failureCount;
            this.lastUsedTime = lastUsedTime;
            this.lastHealthCheckTime = 0;
            this.lastHealthCheckResult = false;
        }

        public ProxyInfo(ProxyConfig.ProxyServer proxyServer, int weight) {
            this.proxyServer = proxyServer;
            this.weight = weight;
            this.lastHealthCheckTime = 0;
            this.lastHealthCheckResult = false;
        }

        /**
         * 获取代理URL。
         *
         * @return 代理URL
         */
        public String getProxyUrl() {
            return proxyServer.getProxyUrl();
        }

        /**
         * 获取代理服务器主机名。
         *
         * @return 主机名
         */
        public String getHost() {
            return proxyServer.getHost();
        }

        /**
         * 获取代理服务器端口。
         *
         * @return 端口
         */
        public int getPort() {
            return proxyServer.getPort();
        }

        /**
         * 获取代理服务器用户名。
         *
         * @return 用户名
         */
        public String getUsername() {
            return proxyServer.getUsername();
        }

        /**
         * 获取代理服务器密码。
         *
         * @return 密码
         */
        public String getPassword() {
            return proxyServer.getPassword();
        }

        /**
         * 获取代理类型。
         *
         * @return 代理类型
         */
        public ProxyType getType() {
            return proxyServer.getType();
        }

        /**
         * 获取代理服务器信息。
         *
         * @return 代理服务器信息
         */
        public ProxyConfig.ProxyServer getProxyServer() {
            return proxyServer;
        }

        /**
         * 设置代理服务器信息。
         *
         * @param proxyServer 代理服务器信息
         */
        public void setProxyServer(ProxyConfig.ProxyServer proxyServer) {
            this.proxyServer = proxyServer;
        }

        /**
         * 获取代理权重。
         *
         * @return 权重
         */
        public int getWeight() {
            return weight;
        }

        /**
         * 获取代理失败次数。
         *
         * @return 失败次数
         */
        public int getFailureCount() {
            return failureCount;
        }

        /**
         * 获取代理最后使用时间戳。
         *
         * @return 时间戳（毫秒）
         */
        public long getLastUsedTime() {
            return lastUsedTime;
        }

        /**
         * 设置代理最后使用时间戳。
         *
         * @param lastUsedTime 时间戳（毫秒）
         */
        public void setLastUsedTime(long lastUsedTime) {
            this.lastUsedTime = lastUsedTime;
        }

        /**
         * 获取上次健康检查时间戳。
         *
         * @return 时间戳（毫秒）
         */
        public long getLastHealthCheckTime() {
            return lastHealthCheckTime;
        }

        /**
         * 获取上次健康检查结果。
         *
         * @return 是否健康
         */
        public boolean getLastHealthCheckResult() {
            return lastHealthCheckResult;
        }

        /**
         * 设置健康检查结果。
         *
         * @param healthy 是否健康
         */
        public void setHealthCheckResult(boolean healthy) {
            this.lastHealthCheckTime = System.currentTimeMillis();
            this.lastHealthCheckResult = healthy;
            if (!healthy) {
                this.failureCount++;
            } else {
                this.failureCount = 0;
            }

        }

    }

    /**
     * 代理池统计信息。
     */
    record ProxyPoolStatistics(
            /**
             * 代理总数。
             */
            int totalProxies,
            /**
             * 可用代理数。
             */
            int availableProxies,
            /**
             * 不可用代理数。
             */
            int unavailableProxies,
            /**
             * HTTP 代理数。
             */
            int httpProxies,
            /**
             * HTTPS 代理数。
             */
            int httpsProxies,
            /**
             * SOCKS5 代理数。
             */
            int socks5Proxies
    ) {
        @Override
        public String toString() {
            return "ProxyPoolStatistics{" +
                    "totalProxies=" + totalProxies +
                    ", availableProxies=" + availableProxies +
                    ", unavailableProxies=" + unavailableProxies +
                    ", httpProxies=" + httpProxies +
                    ", httpsProxies=" + httpsProxies +
                    ", socks5Proxies=" + socks5Proxies +
                    '}';
        }
    }
}
