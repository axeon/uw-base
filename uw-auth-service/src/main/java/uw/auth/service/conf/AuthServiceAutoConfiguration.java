package uw.auth.service.conf;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import uw.auth.service.AuthServiceHelper;
import uw.auth.service.advice.GlobalExceptionAdvice;
import uw.auth.service.advice.GlobalResponseAdvice;
import uw.auth.service.filter.AuthServiceFilter;
import uw.auth.service.log.AuthCriticalLogStorage;
import uw.auth.service.log.impl.AuthCriticalLogNoneStorage;
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
                                                                       final LogClient logClient, final AuthCriticalLogStorage authCriticalLogStorage) {
        FilterRegistrationBean<AuthServiceFilter> registrationBean = new FilterRegistrationBean<AuthServiceFilter>();
        AuthServiceFilter authServiceFilter = new AuthServiceFilter( authServiceProperties, requestMappingHandlerMapping, authPermService, logClient,
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
     * @param tokenRestTemplate
     * @return
     */
    @Bean
    public AuthAppRpc authAppRpc(final AuthServiceProperties authServiceProperties, @Qualifier("tokenRestTemplate") final RestTemplate tokenRestTemplate) {
        return new AuthAppRpcImpl( authServiceProperties, tokenRestTemplate );
    }


    /**
     * AuthServiceRpc 接口
     *
     * @param authServiceProperties
     * @param tokenRestTemplate
     * @return
     */
    @Bean
    public AuthServiceRpc authServiceRpc(final AuthServiceProperties authServiceProperties, @Qualifier("tokenRestTemplate") final RestTemplate tokenRestTemplate) {
        return new AuthServiceRpcImpl( authServiceProperties, tokenRestTemplate );
    }

    /**
     * 用户权限服务接口
     *
     * @return
     */
    @Bean
    public AuthPermService authPermService() {
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
            logClient.regLogObjectWithIndexName( MscLoginLog.class, "uw.auth.login.log" );
            // 操作日志
            logClient.regLogObjectWithIndexName( MscActionLog.class, "uw.auth.action.log" );
        };
    }

}
