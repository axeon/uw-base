package uw.auth.service.conf;

import uw.common.util.StringTools;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
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
import uw.auth.service.service.MscAppUpdateService;
import uw.auth.service.service.MscAuthPermService;
import uw.auth.service.util.IpWebUtils;
import uw.auth.service.vo.MscActionLog;
import uw.auth.service.vo.MscGuestLoginLog;
import uw.auth.service.vo.MscLoginLog;
import uw.log.es.LogClient;

import java.util.Arrays;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * 自动装配配置类。
 * <p>
 * 装配 {@link AuthServiceFilter}、{@link AuthServiceHelper}、{@code MscAuthPermService}、
 * {@code MscAppUpdateService}、RPC 实现、全局 Advice、CORS 过滤器等核心 Bean，
 * 并在 {@code ApplicationReadyEvent} 时触发可信代理初始化与应用注册。
 *
 * @author axeon
 */
@Configuration
@Import({GlobalResponseAdvice.class, GlobalExceptionAdvice.class})
@EnableConfigurationProperties({UwAuthServiceProperties.class})
public class UwAuthServiceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(UwAuthServiceAutoConfiguration.class);

    private static final int TOKEN_FILTER_ORDER = 999;

    /**
     * 应用更新服务
     */
    private MscAppUpdateService appUpdateService;

    /**
     * AuthService配置
     */
    private final UwAuthServiceProperties authServiceProperties;

    public UwAuthServiceAutoConfiguration(UwAuthServiceProperties authServiceProperties) {
        this.authServiceProperties = authServiceProperties;
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
    public FilterRegistrationBean<AuthServiceFilter> authServiceFilter(final UwAuthServiceProperties authServiceProperties, final RequestMappingHandlerMapping requestMappingHandlerMapping, @Qualifier("handlerExceptionResolver") final HandlerExceptionResolver exceptionResolver, final MscAuthPermService authPermService, final LogClient logClient, final AuthCriticalLogStorage authCriticalLogStorage) {
        FilterRegistrationBean<AuthServiceFilter> registrationBean = new FilterRegistrationBean<AuthServiceFilter>();
        AuthServiceFilter authServiceFilter = new AuthServiceFilter(authServiceProperties, requestMappingHandlerMapping, exceptionResolver, authPermService, logClient, authCriticalLogStorage);
        registrationBean.setFilter(authServiceFilter);
        registrationBean.setName("AuthServiceFilter");
        registrationBean.setOrder(TOKEN_FILTER_ORDER);
        if (StringUtils.isNotBlank(authServiceProperties.getAuthProtectedPaths())) {
            registrationBean.setUrlPatterns(StringTools.splitToStringList(authServiceProperties.getAuthProtectedPaths(), ","));
        }
        return registrationBean;
    }

    /**
     * 注册日志对象
     *
     * @param logClient
     * @return
     */
    @Bean
    public CommandLineRunner configAuthLogClient(final LogClient logClient) {
        return args -> {
            // 登录日志查询
            logClient.regLogObjectWithIndexName(MscLoginLog.class, "uw.auth.login.log");
            // 操作日志
            logClient.regLogObjectWithIndexName(MscActionLog.class, "uw.auth.action.log");
            // 客户登录日志查询
            logClient.regLogObjectWithIndexName(MscGuestLoginLog.class, "uw.auth.guest.login.log");
        };
    }

    /**
     * 改为自注入 不要在AuthCriticalLogNoneStorage上加注解 否则反向往center注入需要加包扫描
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthCriticalLogStorage authCriticalLogStorage() {
        return new AuthCriticalLogNoneStorage();
    }

    /**
     * 如果引入gateway网关、auth服务不需要配置cors。
     * corsFilter
     *
     * @param authServiceProperties
     * @return
     */
    @Bean
    @ConditionalOnProperty(prefix = "uw.auth.service", name = "enable-gateway", havingValue = "false")
    public FilterRegistrationBean<CorsFilter> corsFilter(final UwAuthServiceProperties authServiceProperties) {
        FilterRegistrationBean<CorsFilter> registrationBean = new FilterRegistrationBean<CorsFilter>();
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(authServiceProperties.getCors().getAllowedOrigins().split("\\s*,\\s*")));
        configuration.setAllowedMethods(Arrays.asList(authServiceProperties.getCors().getAllowedMethods().split("\\s*,\\s*")));
        configuration.setAllowedHeaders(Arrays.asList(authServiceProperties.getCors().getAllowedHeaders().split("\\s*,\\s*")));
        configuration.setAllowedOriginPatterns(Arrays.asList(authServiceProperties.getCors().getAllowedOriginPattern().split("\\s*,\\s*")));
        configuration.setAllowCredentials(authServiceProperties.getCors().getAllowCredentials());
        configuration.setMaxAge(authServiceProperties.getCors().getMaxAge());
        source.registerCorsConfiguration(authServiceProperties.getCors().getMapping(), configuration);
        CorsFilter corsFilter = new CorsFilter(source);
        registrationBean.setFilter(corsFilter);
        registrationBean.setName("CorsFilter");
        registrationBean.setOrder(TOKEN_FILTER_ORDER - 1);
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }

    /**
     * App更新服务
     *
     * @param requestMappingHandlerMapping
     * @param authServiceProperties
     * @param authAppRpc
     * @param authPermService
     * @return
     */
    @Bean
    public MscAppUpdateService appUpdateService(final ApplicationContext applicationContext, final RequestMappingHandlerMapping requestMappingHandlerMapping, final UwAuthServiceProperties authServiceProperties, final AuthAppRpc authAppRpc, final MscAuthPermService authPermService, final AuthServiceRpc authServiceRpc) {
        // AuthServiceHelper 为静态门面（下游 mockStatic 直调），bean 仅作构造锚点——随本方法内联构造，不再单独暴露
        new AuthServiceHelper(authServiceProperties, authPermService, authServiceRpc);
        appUpdateService = new MscAppUpdateService(applicationContext, requestMappingHandlerMapping, authServiceProperties, authAppRpc, authPermService);
        return appUpdateService;
    }

    /**
     * AuthAppRpc 接口
     *
     * @param authServiceProperties
     * @param authRestClient
     * @return
     */
    @Bean
    public AuthAppRpc authAppRpc(final UwAuthServiceProperties authServiceProperties, @Qualifier("authRestClient") final RestClient authRestClient) {
        return new AuthAppRpcImpl(authServiceProperties, authRestClient);
    }


    /**
     * AuthServiceRpc 接口
     *
     * @param authServiceProperties
     * @param authRestClient
     * @return
     */
    @Bean
    public AuthServiceRpc authServiceRpc(final UwAuthServiceProperties authServiceProperties, @Qualifier("authRestClient") final RestClient authRestClient) {
        return new AuthServiceRpcImpl(authServiceProperties, authRestClient);
    }

    /**
     * 用户权限服务接口
     *
     * @return
     */
    @Bean
    public MscAuthPermService authPermService() {
        return new MscAuthPermService();
    }

    /**
     * ApplicationContext初始化完成或刷新后执行init方法
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        IpWebUtils.initTrustedProxies(authServiceProperties.getTrustedProxies());
        if (appUpdateService != null) {
            appUpdateService.init();
        }
    }


    /**
     * uw.auth.service 属性 nacos 热更新监听器（classpath 有 spring-cloud-context 才装配；本地/纯测试行为不变）。
     *
     * @param properties 属性 bean
     * @return AuthServicePropertiesRefresher
     */
    @ConditionalOnClass(EnvironmentChangeEvent.class)
    @Bean
    AuthServicePropertiesRefresher authServicePropertiesRefresher(UwAuthServiceProperties properties) {
        return new AuthServicePropertiesRefresher(properties);
    }

    /**
     * uw.auth.service 属性 nacos 热更新监听（嵌套装配，见类方法 javadoc）。
     */
    static class AuthServicePropertiesRefresher implements EnvironmentAware {

        /**
         * 日志器。
         */
        private static final Logger log = LoggerFactory.getLogger(AuthServicePropertiesRefresher.class);

        /**
         * 配置前缀。
         */
        private static final String CONFIG_PREFIX = "uw.auth.service";

        /**
         * 属性 bean（原地重绑目标）。
         */
        private final UwAuthServiceProperties properties;

        /**
         * Spring 环境（重绑配置源）。
         */
        private Environment environment;

        /**
         * @param properties 属性 bean
         */
        public AuthServicePropertiesRefresher(UwAuthServiceProperties properties) {
            this.properties = properties;
        }

        /**
         * 注入 Spring 环境。
         *
         * @param environment Spring 环境
         */
        @Override
        public void setEnvironment(Environment environment) {
            this.environment = environment;
        }

        /**
         * 环境变更（nacos 推送等）：原地重绑属性。
         *
         * @param event 环境变更事件
         */
        @EventListener(EnvironmentChangeEvent.class)
        public void onEnvironmentChange(EnvironmentChangeEvent event) {
            if (event.getKeys() == null || event.getKeys().stream().noneMatch(k -> k.startsWith(CONFIG_PREFIX))) {
                return;
            }
            try {
                Binder.get(environment).bind(CONFIG_PREFIX, Bindable.ofInstance(properties));
                log.info("{} 属性已原地热更新（静态持有方自动生效；构造期捕获的组件不受影响）", CONFIG_PREFIX);
            } catch (Exception e) {
                log.warn("{} 属性热更新重绑失败, 保持当前生效配置: {}", CONFIG_PREFIX, e.getMessage());
            }
        }
    }
}
