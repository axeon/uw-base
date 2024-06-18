package uw.cache.conf;

import io.lettuce.core.resource.ClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import uw.cache.*;
import uw.cache.listener.FusionCacheNotifyListener;

/**
 * 启动配置。
 */
@Configuration
@EnableConfigurationProperties({UwCacheProperties.class})
@AutoConfigureAfter({RedisAutoConfiguration.class})
public class UwCacheAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger( UwCacheAutoConfiguration.class );

    /**
     * 初始化GlobalCache。
     *
     * @param dataCacheRedisTemplate
     * @return
     */
    @Bean
    public GlobalCache globalCache(RedisTemplate<String, byte[]> dataCacheRedisTemplate) {
        return new GlobalCache( dataCacheRedisTemplate );
    }

    /**
     * 初始化GlobalLocker。
     *
     * @param longCacheRedisTemplate
     * @return
     */
    @Bean
    public GlobalLocker globalLocker(RedisTemplate<String, Long> longCacheRedisTemplate) {
        return new GlobalLocker( longCacheRedisTemplate );
    }


    /**
     * 初始化GlobalCounter。
     *
     * @param longCacheRedisTemplate
     * @return
     */
    @Bean
    public GlobalCounter globalCounter(RedisTemplate<String, Long> longCacheRedisTemplate) {
        return new GlobalCounter( longCacheRedisTemplate );
    }

    /**
     * 初始化 GlobalHashSet。
     *
     * @param dataCacheRedisTemplate
     * @return
     */
    @Bean
    public GlobalHashSet globalHashSet(RedisTemplate<String, byte[]> dataCacheRedisTemplate) {
        return new GlobalHashSet( dataCacheRedisTemplate );
    }

    /**
     * 初始化 GlobalSortedSet。
     *
     * @param dataCacheRedisTemplate
     * @return
     */
    @Bean
    public GlobalSortedSet globalSortedSet(RedisTemplate<String, byte[]> dataCacheRedisTemplate) {
        return new GlobalSortedSet( dataCacheRedisTemplate );
    }


    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisTemplate<String, byte[]> dataCacheRedisTemplate) {
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory( dataCacheRedisTemplate.getConnectionFactory() );
        FusionCacheNotifyListener cacheMessageListener = new FusionCacheNotifyListener();
        redisMessageListenerContainer.addMessageListener( cacheMessageListener, new ChannelTopic( FusionCache.FUSION_CACHE_NOTIFY_CHANNEL ) );
        return redisMessageListenerContainer;
    }


    /**
     * dataCacheRedisTemplate。
     * 专门针对fusion优化的Redis模版。
     *
     * @param uwCacheProperties
     * @param clientResources
     * @return
     */
    @Bean
    public RedisTemplate<String, byte[]> dataCacheRedisTemplate(final UwCacheProperties uwCacheProperties,
                                                            final ClientResources clientResources) {
        RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory(uwCacheProperties.getRedis(), clientResources));
        redisTemplate.setEnableDefaultSerializer(false);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * commonCacheRedisTemplate。
     * 通用的缓存Redis模版。
     *
     * @param uwCacheProperties
     * @param clientResources
     * @return
     */
    @Bean
    public RedisTemplate<String, Long> longCacheRedisTemplate(final UwCacheProperties uwCacheProperties,
                                                            final ClientResources clientResources) {
        RedisTemplate<String, Long> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer( new StringRedisSerializer() );
        redisTemplate.setValueSerializer( new GenericToStringSerializer<Long>(Long.class));
        redisTemplate.setConnectionFactory( redisConnectionFactory( uwCacheProperties.getRedis(), clientResources ) );
        redisTemplate.setEnableDefaultSerializer( false );
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * Redis连接工厂
     *
     * @param redisProperties
     * @param clientResources
     * @return
     */
    private RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties,
                                                          ClientResources clientResources) {
        RedisProperties.Pool pool = redisProperties.getLettuce().getPool();
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder;
        if (pool == null) {
            builder = LettuceClientConfiguration.builder();
        } else {
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setMaxTotal( pool.getMaxActive() );
            config.setMaxIdle( pool.getMaxIdle() );
            config.setMinIdle( pool.getMinIdle() );
            if (pool.getMaxWait() != null) {
                config.setMaxWait( pool.getMaxWait() );
            }
            builder = LettucePoolingClientConfiguration.builder().poolConfig( config );
        }

        if (redisProperties.getTimeout() != null) {
            builder.commandTimeout( redisProperties.getTimeout() );
        }
        if (redisProperties.getLettuce() != null) {
            RedisProperties.Lettuce lettuce = redisProperties.getLettuce();
            if (lettuce.getShutdownTimeout() != null && !lettuce.getShutdownTimeout().isZero()) {
                builder.shutdownTimeout( redisProperties.getLettuce().getShutdownTimeout() );
            }
        }
        builder.clientResources( clientResources );
        LettuceClientConfiguration config = builder.build();

        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
        standaloneConfig.setHostName( redisProperties.getHost() );
        standaloneConfig.setPort( redisProperties.getPort() );
        standaloneConfig.setPassword( RedisPassword.of( redisProperties.getPassword() ) );
        standaloneConfig.setDatabase( redisProperties.getDatabase() );

        LettuceConnectionFactory factory = new LettuceConnectionFactory( standaloneConfig, config );
        factory.afterPropertiesSet();
        return factory;
    }

}
