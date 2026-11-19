package uw.kv.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;

/**
 * uw-kv 配置（{@code uw.kv.redis.*}，与 uw.mfa.redis / uw.dao.redis 等独立域同构的嵌套键形态）。
 * <p>自有类型（组合持有 Redis 连接配置），无 Boot 属性 Bean 的类型装配歧义，工厂直接按本类型注入。
 * 曾评估过平铺键（uw.kv.*）：无论属性 Bean 化（类型歧义需 defaultCandidate 补丁）、Binder 编程式绑定
 * 还是子类化（LSP 歧义），实现成本均高于嵌套一层键——收敛为全框架同构的最简形态。</p>
 *
 * @author axeon
 */
@ConfigurationProperties(prefix = "uw.kv")
public class UwKvProperties {

    /**
     * Redis 连接配置（uw.kv.redis.*，复用 Spring Boot 原生全部字段：host/port/password/database/sentinel/cluster/lettuce.pool/ssl/timeout）。
     */
    private DataRedisProperties redis = new DataRedisProperties();

    public DataRedisProperties getRedis() {
        return redis;
    }

    public void setRedis(DataRedisProperties redis) {
        this.redis = redis;
    }

}
