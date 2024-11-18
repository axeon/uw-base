package uw.mfa.conf;

import io.lettuce.core.resource.ClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestTemplate;
import uw.mfa.helper.MfaCaptchaHelper;
import uw.mfa.helper.MfaDeviceCodeHelper;
import uw.mfa.helper.MfaIPLimitHelper;


/**
 * 启动配置。
 */
@Configuration
@EnableConfigurationProperties({UwMfaProperties.class})
@AutoConfigureAfter({RedisAutoConfiguration.class})
public class UwMfaAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger( UwMfaAutoConfiguration.class );

    @Bean("mfaRedisTemplate")
    protected RedisTemplate<String, String> mfaRedisTemplate(final UwMfaProperties uwMfaProperties, final ClientResources clientResources) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer( new StringRedisSerializer() );
        redisTemplate.setValueSerializer( new StringRedisSerializer() );
        redisTemplate.setHashKeySerializer( new StringRedisSerializer() );
        redisTemplate.setHashValueSerializer( new GenericToStringSerializer<Integer>( Integer.class ) );
        redisTemplate.setConnectionFactory( redisConnectionFactory( uwMfaProperties.getRedis(), clientResources ) );
        redisTemplate.setEnableDefaultSerializer( false );
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    @DependsOn("mfaRedisTemplate")
    @ConditionalOnMissingBean
    protected MfaCaptchaHelper mfaCaptchaHelper(final UwMfaProperties uwMfaProperties, @Qualifier("mfaRedisTemplate") final RedisTemplate<String, String> mfaRedisTemplate) {
        return new MfaCaptchaHelper( uwMfaProperties, mfaRedisTemplate );
    }

    @Bean
    @DependsOn("mfaRedisTemplate")
    @ConditionalOnMissingBean
    protected MfaDeviceCodeHelper mfaDeviceCodeHelper(final UwMfaProperties uwMfaProperties, @Qualifier("mfaRedisTemplate") final RedisTemplate<String, String> mfaRedisTemplate,
                                                      @Qualifier("tokenRestTemplate") final RestTemplate tokenRestTemplate) {
        return new MfaDeviceCodeHelper( uwMfaProperties, mfaRedisTemplate, tokenRestTemplate );
    }

    @Bean
    @DependsOn("mfaRedisTemplate")
    @ConditionalOnMissingBean
    protected MfaIPLimitHelper mfaIPLimitHelper(final UwMfaProperties uwMfaProperties, @Qualifier("mfaRedisTemplate") final RedisTemplate<String, String> mfaRedisTemplate) {
        return new MfaIPLimitHelper( uwMfaProperties, mfaRedisTemplate );
    }


    /**
     * Redis连接工厂
     *
     * @param redisProperties
     * @param clientResources
     * @return
     */
    private RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties, ClientResources clientResources) {
        //设置连接池。
        RedisProperties.Pool poolProperties = redisProperties.getLettuce().getPool();
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal( poolProperties.getMaxActive() );
        poolConfig.setMaxIdle( poolProperties.getMaxIdle() );
        poolConfig.setMinIdle( poolProperties.getMinIdle() );
        if (poolProperties.getMaxWait() != null) {
            poolConfig.setMaxWait( poolProperties.getMaxWait() );
        }
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder().poolConfig( poolConfig );
        if (redisProperties.getTimeout() != null) {
            builder.commandTimeout( redisProperties.getTimeout() );
        }
        //设置shutdownTimeout。
        RedisProperties.Lettuce lettuce = redisProperties.getLettuce();
        if (lettuce.getShutdownTimeout() != null && !lettuce.getShutdownTimeout().isZero()) {
            builder.shutdownTimeout( redisProperties.getLettuce().getShutdownTimeout() );
        }
        //设置clientResources。
        builder.clientResources( clientResources );
        //设置ssl。
        if (redisProperties.getSsl().isEnabled()) {
            builder.useSsl();
        }
        //构建standaloneConfig。
        LettuceClientConfiguration clientConfig = builder.build();
        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
        standaloneConfig.setHostName( redisProperties.getHost() );
        standaloneConfig.setPort( redisProperties.getPort() );
        standaloneConfig.setDatabase( redisProperties.getDatabase() );
        if (redisProperties.getUsername() != null) {
            standaloneConfig.setUsername( redisProperties.getUsername() );
        }
        if (redisProperties.getPassword() != null) {
            standaloneConfig.setPassword( RedisPassword.of( redisProperties.getPassword() ) );
        }
        LettuceConnectionFactory factory = new LettuceConnectionFactory( standaloneConfig, clientConfig );
        factory.afterPropertiesSet();
        return factory;
    }

}
