package uw.cache.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * uw-cache 配置属性。
 * <p>
 * 绑定前缀 {@code uw.cache}，通过 {@code uw.cache.redis.*} 配置专用 Redis 连接。
 * 内部 {@link RedisProperties} 继承自 Spring Boot 的 RedisProperties，
 * 以复用其全部连接池/Lettuce/SSL 等配置项，同时与业务 Redis 隔离（独立 database 或实例）。
 */
@ConfigurationProperties(prefix = "uw.cache")
public class UwCacheProperties {

    /**
     * Redis 配置。
     */
    private RedisProperties redis = new RedisProperties();

    /**
     * 获取 Redis 配置。
     *
     * @return Redis 配置对象
     */
    public RedisProperties getRedis() {
        return redis;
    }

    /**
     * 设置 Redis 配置。
     *
     * @param redis Redis 配置对象
     */
    public void setRedis(RedisProperties redis) {
        this.redis = redis;
    }

    /**
     * Redis 配置，继承 Spring Boot RedisProperties 以复用全部配置项。
     */
    public static class RedisProperties extends org.springframework.boot.autoconfigure.data.redis.RedisProperties {
    }
}
