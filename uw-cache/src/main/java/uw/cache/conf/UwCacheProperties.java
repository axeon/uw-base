package uw.cache.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;

/**
 * uw-cache 配置属性。
 * <p>
 * 绑定前缀 {@code uw.cache}，通过 {@code uw.cache.redis.*} 配置专用 Redis 连接。
 * 通过 {@link DataRedisProperties} 复用 Spring Boot Redis 全部连接池/Lettuce/SSL 等配置项，
 * 同时与业务 Redis 隔离（独立 database 或实例）。
 */
@ConfigurationProperties(prefix = "uw.cache")
public class UwCacheProperties {

    /**
     * Redis 配置。
     */
    private DataRedisProperties redis = new DataRedisProperties();

    /**
     * 获取 Redis 配置。
     *
     * @return Redis 配置对象
     */
    public DataRedisProperties getRedis() {
        return redis;
    }

    /**
     * 设置 Redis 配置。
     *
     * @param redis Redis 配置对象
     */
    public void setRedis(DataRedisProperties redis) {
        this.redis = redis;
    }

}
