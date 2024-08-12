package uw.auth.service.conf;

import com.google.common.base.Splitter;
import io.lettuce.core.resource.ClientResources;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import uw.auth.service.AuthServiceHelper;
import uw.auth.service.advice.GlobalExceptionAdvice;
import uw.auth.service.advice.GlobalResponseAdvice;
import uw.auth.service.filter.AuthServiceFilter;
import uw.auth.service.ipblock.IpMatchHelper;
import uw.auth.service.log.AuthCriticalLogStorage;
import uw.auth.service.log.impl.AuthCriticalLogNoneStorage;
import uw.auth.service.ratelimit.MscRateLimiter;
import uw.auth.service.ratelimit.RateLimitConfig;
import uw.auth.service.ratelimit.RateLimitUtils;
import uw.auth.service.ratelimit.impl.GlobalRateLimiter;
import uw.auth.service.ratelimit.impl.LocalRateLimiter;
import uw.auth.service.ratelimit.impl.NoneRateLimiter;
import uw.auth.service.rpc.AuthAppRpc;
import uw.auth.service.rpc.AuthServiceRpc;
import uw.auth.service.rpc.impl.AuthAppRpcImpl;
import uw.auth.service.rpc.impl.AuthServiceRpcImpl;
import uw.auth.service.service.AppUpdateService;
import uw.auth.service.service.AuthPermService;
import uw.auth.service.vo.MscActionLog;
import uw.auth.service.vo.MscLoginLog;
import uw.log.es.LogClient;

import java.util.Arrays;

/**
 * 配置自动装配类
 */
@Configuration
@Import({GlobalResponseAdvice.class, GlobalExceptionAdvice.class})
@EnableConfigurationProperties({AuthServiceProperties.class})
@AutoConfigureAfter({RedisAutoConfiguration.class})
public class AuthServiceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger( AuthServiceAutoConfiguration.class );

    private static final int TOKEN_FILTER_ORDER = 999;

    /**
     * 应用更新服务
     */
    private AppUpdateService appUpdateService;


    /**
     * AuthServer的入口类。
     *
     * @param authServiceProperties
     * @return
     */
    @Bean
    public AuthServiceHelper authServerHelper(final AuthServiceProperties authServiceProperties, final AuthPermService authPermService, final AuthServiceRpc authServiceRpc) {
        return new AuthServiceHelper( authServiceProperties, authPermService, authServiceRpc );
    }

    /**
     * AuthServer的入口类。
     *
     * @param authServiceProperties
     * @return
     */
    @Bean
    public IpMatchHelper ipBlockHelper(final AuthServiceProperties authServiceProperties) {
        return new IpMatchHelper( 500 );
    }

    /**
     * 限速器。
     *
     * @param rateLimitRedisTemplate
     * @param authServiceProperties
     * @return
     */
    @Bean
    public MscRateLimiter mscRateLimiter(@Autowired(required = false) @Qualifier("rateLimitRedisTemplate") final RedisTemplate<String, String> rateLimitRedisTemplate,
                                         final AuthServiceProperties authServiceProperties) {
        //解析出来默认限速信息。
        RateLimitConfig defaultRateLimit = authServiceProperties.getRateLimit().getDefaultConfig();
        RateLimitUtils.setDefaultConfig( defaultRateLimit );
        //限速类型。
        AuthServiceProperties.RateLimitType type = authServiceProperties.getRateLimit().getType();
        if (type.equals( AuthServiceProperties.RateLimitType.GLOBAL )) {
            return new GlobalRateLimiter( rateLimitRedisTemplate );
        } else if (type.equals( AuthServiceProperties.RateLimitType.LOCAL )) {
            return new LocalRateLimiter( authServiceProperties.getRateLimit().getCacheNum() );
        } else {
            return new NoneRateLimiter();
        }

    }

    /**
     * AuthServiceFilter
     *
     * @param authServiceProperties
     * @param requestMappingHandlerMapping
     * @param authPermService
     * @param logClient
     * @return
     */
    @Bean
    public FilterRegistrationBean<AuthServiceFilter> authServiceFilter(final AuthServiceProperties authServiceProperties,
                                                                       final RequestMappingHandlerMapping requestMappingHandlerMapping, final AuthPermService authPermService,
                                                                       final MscRateLimiter mscRateLimiter, final LogClient logClient,
                                                                       final AuthCriticalLogStorage authCriticalLogStorage) {
        FilterRegistrationBean<AuthServiceFilter> registrationBean = new FilterRegistrationBean<AuthServiceFilter>();
        AuthServiceFilter authServiceFilter = new AuthServiceFilter( authServiceProperties, requestMappingHandlerMapping, authPermService, mscRateLimiter, logClient,
                authCriticalLogStorage );
        registrationBean.setFilter( authServiceFilter );
        registrationBean.setName( "AuthServiceFilter" );
        registrationBean.setOrder( TOKEN_FILTER_ORDER );
        if (StringUtils.isNotBlank( authServiceProperties.getAuthEntryPoint() )) {
            registrationBean.setUrlPatterns( Splitter.on( "," ).trimResults().omitEmptyStrings().splitToList( authServiceProperties.getAuthEntryPoint() ) );
        }
        return registrationBean;
    }

    /**
     * 改为自注入 不要在AuthCriticalLogNoneStorage上加注解 否则反向往center注入需要加包扫描
     *
     * @return
     */
    @Bean
    public AuthCriticalLogStorage authCriticalLogStorage() {
        return new AuthCriticalLogNoneStorage();
    }

    /**
     * 引入gateway网关、auth服务不需要配置cors、保留代码
     * corsFilter
     *
     * @param authServiceProperties
     * @return
     */
//    @Bean 网关已做统一跨域处理 不用在各个服务做跨域处理 此处放开会导致双跨域问题
    public FilterRegistrationBean<CorsFilter> corsFilter(final AuthServiceProperties authServiceProperties) {
        FilterRegistrationBean<CorsFilter> registrationBean = new FilterRegistrationBean<CorsFilter>();
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins( Arrays.asList( authServiceProperties.getCors().getAllowedOrigins().split( "\\s*,\\s*" ) ) );
        configuration.setAllowedMethods( Arrays.asList( authServiceProperties.getCors().getAllowedMethods().split( "\\s*,\\s*" ) ) );
        configuration.setAllowedHeaders( Arrays.asList( authServiceProperties.getCors().getAllowedHeaders().split( "\\s*,\\s*" ) ) );
        configuration.setAllowedOriginPatterns( Arrays.asList( authServiceProperties.getCors().getAllowedOriginPattern().split( "\\s*,\\s*" ) ) );
        configuration.setAllowCredentials( authServiceProperties.getCors().getAllowCredentials() );
        configuration.setMaxAge( authServiceProperties.getCors().getMaxAge() );
        source.registerCorsConfiguration( authServiceProperties.getCors().getMapping(), configuration );
        CorsFilter corsFilter = new CorsFilter( source );
        registrationBean.setFilter( corsFilter );
        registrationBean.setName( "CorsFilter" );
        registrationBean.setOrder( TOKEN_FILTER_ORDER - 1 );
        registrationBean.addUrlPatterns( "/*" );
        return registrationBean;
    }


    /**
     * App更新服务
     *
     * @param authServiceProperties
     * @param authAppRpc
     * @param authPermService
     * @param requestMappingHandlerMapping
     * @return
     */
    @Bean
    public AppUpdateService appUpdateService(final AuthServiceProperties authServiceProperties, final AuthAppRpc authAppRpc, final AuthPermService authPermService,
                                             final RequestMappingHandlerMapping requestMappingHandlerMapping) {
        appUpdateService = new AppUpdateService( authServiceProperties, authAppRpc, authPermService, requestMappingHandlerMapping );
        return appUpdateService;
    }

    /**
     * AuthAppRpc 接口
     *
     * @param authServiceProperties
     * @param restTemplate
     * @return
     */
    @Bean
    public AuthAppRpc authAppRpc(final AuthServiceProperties authServiceProperties, @Qualifier("tokenRestTemplate") final RestTemplate restTemplate) {
        return new AuthAppRpcImpl( authServiceProperties, restTemplate );
    }


    /**
     * AuthServiceRpc 接口
     *
     * @param authServiceProperties
     * @param restTemplate
     * @return
     */
    @Bean
    public AuthServiceRpc authServiceRpc(final AuthServiceProperties authServiceProperties, @Qualifier("tokenRestTemplate") final RestTemplate restTemplate) {
        return new AuthServiceRpcImpl( authServiceProperties, restTemplate );
    }

    /**
     * 用于存储验证信息的RedisTemplate。
     *
     * @param authServiceProperties
     * @param clientResources
     * @return
     */
    @Bean
    @ConditionalOnProperty(prefix = "uw.auth.service", name = "rate-limit.type", havingValue = "GLOBAL")
    public RedisTemplate<String, String> rateLimitRedisTemplate(final AuthServiceProperties authServiceProperties, final ClientResources clientResources) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<String, String>();
        redisTemplate.setKeySerializer( new StringRedisSerializer() );
        redisTemplate.setValueSerializer( new GenericToStringSerializer<String>( String.class ) );
        redisTemplate.setConnectionFactory( redisConnectionFactory( authServiceProperties.getRateLimit().getRedis(), clientResources ) );
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 用户权限服务接口
     *
     * @return
     */
    @Bean
    public AuthPermService authPermService(final AuthServiceProperties authServiceProperties) {
        return new AuthPermService();
    }

    /**
     * ApplicationContext初始化完成或刷新后执行init方法
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        appUpdateService.init();
    }

    /**
     * 注册日志对象
     *
     * @param logClient
     * @return
     */
    @Bean
    public CommandLineRunner configLogClient(final LogClient logClient) {
        return args -> {
            // 登录日志查询
            logClient.regLogObjectWithIndexPattern( MscLoginLog.class, "yyyyMM" );
            // 操作日志
            logClient.regLogObjectWithIndexPattern( MscActionLog.class, "yyyyMM" );
        };
    }

    /**
     * Redis连接工厂
     *
     * @param redisProperties
     * @param clientResources
     * @return
     */
    private RedisConnectionFactory redisConnectionFactory(AuthServiceProperties.RedisProperties redisProperties, ClientResources clientResources) {
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
