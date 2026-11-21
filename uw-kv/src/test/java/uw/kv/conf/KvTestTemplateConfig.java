package uw.kv.conf;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 测试专用模板 bean（src/test）：主代码 261120 收敛后 dataCacheRedisTemplate/longCacheRedisTemplate
 * 不再是对外 bean（改为 kvRedisConnectionFactory bean 方法内手工构建），但模块内 Redis 集成测试
 * 需要注入模板做断言——由本配置从同一连接工厂重建同名测试 bean。
 *
 * @author axeon
 */
@Configuration
public class KvTestTemplateConfig {

    /**
     * byte[] 值模板（对齐主代码构建行为：默认序列化关闭，值由调用方 Kryo 自序列化）。
     *
     * @param kvRedisConnectionFactory uw.kv 连接工厂
     * @return byte[] 值模板
     */
    @Bean
    public RedisTemplate<String, byte[]> dataCacheRedisTemplate(@Qualifier("kvRedisConnectionFactory") RedisConnectionFactory kvRedisConnectionFactory) {
        RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(kvRedisConnectionFactory);
        redisTemplate.setEnableDefaultSerializer(false);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * Long 值模板（对齐主代码构建行为：GenericToStringSerializer + 默认序列化关闭）。
     *
     * @param kvRedisConnectionFactory uw.kv 连接工厂
     * @return Long 值模板
     */
    @Bean
    public RedisTemplate<String, Long> longCacheRedisTemplate(@Qualifier("kvRedisConnectionFactory") RedisConnectionFactory kvRedisConnectionFactory) {
        RedisTemplate<String, Long> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericToStringSerializer<>(Long.class));
        redisTemplate.setConnectionFactory(kvRedisConnectionFactory);
        redisTemplate.setEnableDefaultSerializer(false);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
