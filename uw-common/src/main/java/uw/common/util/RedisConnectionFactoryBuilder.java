package uw.common.util;

import io.lettuce.core.resource.ClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Redis（Lettuce）连接工厂构建工具，支持 Standalone / Cluster / Sentinel 三种模式。
 *
 * <p>各模块在自定义前缀下定义独立 Redis 配置（{@link DataRedisProperties}），
 * 调用此工具统一构建 {@link LettuceConnectionFactory}，消除重复的连接池/SSL/超时配置代码。
 * 连接模式由 {@link DataRedisProperties} 的 cluster/sentinel 配置自动判断。
 *
 * @author zhangjin
 */
public final class RedisConnectionFactoryBuilder {

    private RedisConnectionFactoryBuilder() {
    }

    /**
     * 基于 {@link DataRedisProperties} + {@link ClientResources} 构建 Lettuce 连接工厂。
     *
     * <p>根据 properties 自动判断连接模式：
     * <ul>
     *   <li>cluster 配置存在 → {@link RedisClusterConfiguration}</li>
     *   <li>sentinel 配置存在 → {@link RedisSentinelConfiguration}</li>
     *   <li>否则 → {@link RedisStandaloneConfiguration}</li>
     * </ul>
     *
     * @param redisProperties  Redis 配置
     * @param clientResources  Lettuce 客户端资源共享配置（通常全局共享）
     * @return 已完成 {@code afterPropertiesSet} 的 {@link LettuceConnectionFactory}
     */
    public static RedisConnectionFactory build(DataRedisProperties redisProperties, ClientResources clientResources) {
        RedisConfiguration configuration = createConfiguration(redisProperties);
        LettuceClientConfiguration clientConfig = createLettuceClientConfiguration(redisProperties, clientResources);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration, clientConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * 根据 DataRedisProperties 创建对应的 RedisConfiguration（参考 Spring Boot LettuceConnectionConfiguration 逻辑）。
     */
    private static RedisConfiguration createConfiguration(DataRedisProperties properties) {
        if (properties.getCluster() != null) {
            return createClusterConfiguration(properties);
        }
        if (properties.getSentinel() != null) {
            return createSentinelConfiguration(properties);
        }
        return createStandaloneConfiguration(properties);
    }

    /**
     * Standalone 模式配置。
     */
    private static RedisStandaloneConfiguration createStandaloneConfiguration(DataRedisProperties properties) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(properties.getHost());
        config.setPort(properties.getPort());
        config.setDatabase(properties.getDatabase());
        if (StringUtils.hasText(properties.getUsername())) {
            config.setUsername(properties.getUsername());
        }
        if (StringUtils.hasText(properties.getPassword())) {
            config.setPassword(RedisPassword.of(properties.getPassword()));
        }
        return config;
    }

    /**
     * Cluster 模式配置。
     */
    private static RedisClusterConfiguration createClusterConfiguration(DataRedisProperties properties) {
        RedisClusterConfiguration config = new RedisClusterConfiguration(properties.getCluster().getNodes());
        if (properties.getCluster().getMaxRedirects() != null) {
            config.setMaxRedirects(properties.getCluster().getMaxRedirects());
        }
        if (StringUtils.hasText(properties.getUsername())) {
            config.setUsername(properties.getUsername());
        }
        if (StringUtils.hasText(properties.getPassword())) {
            config.setPassword(RedisPassword.of(properties.getPassword()));
        }
        return config;
    }

    /**
     * Sentinel 模式配置。
     */
    private static RedisSentinelConfiguration createSentinelConfiguration(DataRedisProperties properties) {
        RedisSentinelConfiguration config = new RedisSentinelConfiguration();
        config.master(properties.getSentinel().getMaster());
        for (String node : properties.getSentinel().getNodes()) {
            String[] parts = node.split(":");
            config.sentinel(parts[0], parts.length > 1 ? Integer.parseInt(parts[1]) : 6379);
        }
        if (StringUtils.hasText(properties.getUsername())) {
            config.setUsername(properties.getUsername());
        }
        if (StringUtils.hasText(properties.getPassword())) {
            config.setPassword(RedisPassword.of(properties.getPassword()));
        }
        config.setDatabase(properties.getDatabase());
        return config;
    }

    /**
     * 构建 Lettuce 客户端配置（连接池/命令超时/关闭超时/ClientResources/SSL）。
     */
    private static LettuceClientConfiguration createLettuceClientConfiguration(DataRedisProperties properties, ClientResources clientResources) {
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
                LettucePoolingClientConfiguration.builder();

        // 连接池
        DataRedisProperties.Pool pool = properties.getLettuce().getPool();
        if (pool != null) {
            GenericObjectPoolConfig<io.lettuce.core.api.StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(pool.getMaxActive());
            poolConfig.setMaxIdle(pool.getMaxIdle());
            poolConfig.setMinIdle(pool.getMinIdle());
            poolConfig.setMaxWait(pool.getMaxWait());
            builder.poolConfig(poolConfig);
        }

        // 命令超时
        Duration timeout = properties.getTimeout();
        if (timeout != null && !timeout.isZero()) {
            builder.commandTimeout(timeout);
        }

        // 关闭超时
        Duration shutdownTimeout = properties.getLettuce().getShutdownTimeout();
        if (shutdownTimeout != null && !shutdownTimeout.isZero()) {
            builder.shutdownTimeout(shutdownTimeout);
        }

        // ClientResources（全局共享）
        if (clientResources != null) {
            builder.clientResources(clientResources);
        }

        // SSL
        if (properties.getSsl().isEnabled()) {
            builder.useSsl();
        }

        return builder.build();
    }
}
