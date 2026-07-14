package uw.mfa.conf;

import io.lettuce.core.resource.ClientResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestClient;
import uw.common.util.RedisConnectionFactoryBuilder;
import uw.mfa.helper.MfaCaptchaHelper;
import uw.mfa.helper.MfaDeviceCodeHelper;
import uw.mfa.helper.MfaIPLimitHelper;
import uw.mfa.helper.MfaTotpHelper;


/**
 * uw-mfa自动配置类。
 * <p>创建MFA专用RedisTemplate（独立连接池，不复用主Redis）、4个Helper Bean（Captcha/DeviceCode/IPLimit/Totp），</p>
 * <p>并配置通知服务RPC客户端。Bean可通过 @ConditionalOnMissingBean 覆盖。</p>
 */
@Configuration
@EnableConfigurationProperties({UwMfaProperties.class})
@AutoConfigureAfter({DataRedisAutoConfiguration.class})
public class UwMfaAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(UwMfaAutoConfiguration.class);

    /**
     * 创建MFA专用RedisTemplate（String序列化，独立Lettuce连接池）。
     *
     * @param uwMfaProperties MFA配置（含独立Redis配置）
     * @param clientResources Lettuce客户端资源共享配置
     * @return RedisTemplate实例
     */
    @Bean("mfaRedisTemplate")
    protected RedisTemplate<String, String> mfaRedisTemplate(final UwMfaProperties uwMfaProperties, final ClientResources clientResources) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericToStringSerializer<Integer>(Integer.class));
        redisTemplate.setConnectionFactory(RedisConnectionFactoryBuilder.build(uwMfaProperties.getRedis(), clientResources));
        redisTemplate.setEnableDefaultSerializer(false);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    @DependsOn("mfaRedisTemplate")
    @ConditionalOnMissingBean
    protected MfaCaptchaHelper mfaCaptchaHelper(final UwMfaProperties uwMfaProperties, @Qualifier("mfaRedisTemplate") final RedisTemplate<String, String> mfaRedisTemplate) {
        return new MfaCaptchaHelper(uwMfaProperties, mfaRedisTemplate);
    }

    @Bean
    @DependsOn("mfaRedisTemplate")
    @ConditionalOnMissingBean
    protected MfaDeviceCodeHelper mfaDeviceCodeHelper(final UwMfaProperties uwMfaProperties, @Qualifier("mfaRedisTemplate") final RedisTemplate<String, String> mfaRedisTemplate,
                                                      @Qualifier("authRestClient") final RestClient authRestClient) {
        return new MfaDeviceCodeHelper(uwMfaProperties, mfaRedisTemplate, authRestClient);
    }

    @Bean
    @DependsOn("mfaRedisTemplate")
    @ConditionalOnMissingBean
    protected MfaIPLimitHelper mfaIPLimitHelper(final UwMfaProperties uwMfaProperties, @Qualifier("mfaRedisTemplate") final RedisTemplate<String, String> mfaRedisTemplate) {
        return new MfaIPLimitHelper(uwMfaProperties, mfaRedisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    protected MfaTotpHelper mfaTotpHelper(final UwMfaProperties uwMfaProperties, @Qualifier("mfaRedisTemplate") final RedisTemplate<String, String> mfaRedisTemplate) {
        return new MfaTotpHelper(uwMfaProperties, mfaRedisTemplate);
    }
}
