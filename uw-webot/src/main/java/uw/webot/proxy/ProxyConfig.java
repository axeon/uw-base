package uw.webot.proxy;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 代理配置类。
 * <p>
 * 用于配置代理服务器参数，支持 HTTP、HTTPS、SOCKS4、SOCKS5 等多种代理类型。
 * 同时支持代理池配置，用于管理多个代理服务器。
 * </p>
 *
 * @author axeon
 * @see ProxyType
 * @see ProxyServer
 * @since 1.0.0
 */
public class ProxyConfig implements Serializable {

    /**
     * 最大失败次数。
     */
    private int maxFailures = 3;

    /**
     * 健康检查间隔。
     */
    private Duration healthCheckInterval = Duration.ofMinutes(5);

    /**
     * 代理服务器列表。
     */
    private List<ProxyServer> servers = new ArrayList<>();

    public int getMaxFailures() {
        return maxFailures;
    }

    public void setMaxFailures(int maxFailures) {
        this.maxFailures = maxFailures;
    }

    public Duration getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public void setHealthCheckInterval(Duration healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
    }

    public List<ProxyServer> getServers() {
        return servers;
    }

    public void setServers(List<ProxyServer> servers) {
        this.servers = servers;
    }

    public ProxyConfig() {
    }

    private ProxyConfig(Builder builder) {
        setMaxFailures(builder.maxFailures);
        setHealthCheckInterval(builder.healthCheckInterval);
        setServers(builder.servers);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ProxyConfig copy) {
        Builder builder = new Builder();
        builder.maxFailures = copy.getMaxFailures();
        builder.healthCheckInterval = copy.getHealthCheckInterval();
        builder.servers = copy.getServers();
        return builder;
    }

    /**
     * 代理服务器配置。
     * <p>
     * 用于配置单个代理服务器的参数。
     * </p>
     */
    public static class ProxyServer implements Serializable {

        /**
         * 代理类型。
         */
        private ProxyType type = ProxyType.SOCKS5;

        /**
         * 代理服务器主机地址。
         */
        private String host;

        /**
         * 代理服务器端口。
         */
        private int port;

        /**
         * 代理认证用户名。
         */
        private String username;

        /**
         * 代理认证密码。
         */
        private String password;

        public ProxyServer() {
        }

        public ProxyServer(Builder builder) {
            setHost(builder.host);
            setPort(builder.port);
            setUsername(builder.username);
            setPassword(builder.password);
            setType(builder.type);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static Builder builder(ProxyServer copy) {
            Builder builder = new Builder();
            builder.host = copy.getHost();
            builder.port = copy.getPort();
            builder.username = copy.getUsername();
            builder.password = copy.getPassword();
            builder.type = copy.getType();
            return builder;
        }

        /**
         * 获取代理URL。
         *
         * @return 代理URL
         */
        public String getProxyUrl() {
            if (getUsername() != null && getPassword() != null) {
                return String.format("%s://%s:%s@%s:%d", getType(), getUsername(), getPassword(), getHost(), getPort());
            }
            return String.format("%s://%s:%d", getType(), getHost(), getPort());
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
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

        public ProxyType getType() {
            return type;
        }

        public void setType(ProxyType type) {
            this.type = type;
        }

        public static final class Builder {
            private String host;
            private int port;
            private String username;
            private String password;
            private ProxyType type;

            private Builder() {
            }

            public Builder host(String host) {
                this.host = host;
                return this;
            }

            public Builder port(int port) {
                this.port = port;
                return this;
            }

            public Builder username(String username) {
                this.username = username;
                return this;
            }

            public Builder password(String password) {
                this.password = password;
                return this;
            }

            public Builder type(ProxyType type) {
                this.type = type;
                return this;
            }

            public ProxyServer build() {
                return new ProxyServer(this);
            }
        }
    }

    public static final class Builder {
        private boolean enabled;
        private int maxFailures;
        private Duration healthCheckInterval;
        private List<ProxyServer> servers;

        private Builder() {
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder maxFailures(int maxFailures) {
            this.maxFailures = maxFailures;
            return this;
        }

        public Builder healthCheckInterval(Duration healthCheckInterval) {
            this.healthCheckInterval = healthCheckInterval;
            return this;
        }

        public Builder servers(List<ProxyServer> servers) {
            this.servers = servers;
            return this;
        }

        public ProxyConfig build() {
            return new ProxyConfig(this);
        }
    }
}
