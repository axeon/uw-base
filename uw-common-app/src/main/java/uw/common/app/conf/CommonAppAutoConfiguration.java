package uw.common.app.conf;

import com.alibaba.cloud.nacos.registry.NacosAutoServiceRegistration;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ThreadUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.support.NoOpCache;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uw.auth.service.conf.AuthServiceAutoConfiguration;
import uw.auth.service.log.AuthCriticalLogStorage;
import uw.common.app.constant.CommonConstants;
import uw.common.app.service.SysCritLogStorageService;
import uw.common.util.DateUtils;

import java.io.IOException;
import java.util.Date;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * uw-common-app 启动配置。
 * <p>
 * 在 {@link AuthServiceAutoConfiguration} 之后、{@link WebMvcAutoConfiguration} 之前装配，
 * 提供国际化、关键日志存储、LoadBalancer 缓存、Nacos 优雅停机、Swagger 禁用、ObjectMapper 定制等公共能力。
 * </p>
 */
@Configuration
@AutoConfigureBefore({WebMvcAutoConfiguration.class})
@AutoConfigureAfter(AuthServiceAutoConfiguration.class)
@EnableConfigurationProperties({CommonAppProperties.class})
public class CommonAppAutoConfiguration implements WebMvcConfigurer {

    /**
     * 日志记录器。
     */
    private static final Logger logger = LoggerFactory.getLogger(CommonAppAutoConfiguration.class);

    /**
     * Jackson 对象映射器（由 Spring 容器注入，本类在其上追加日期反序列化模块与时区配置）。
     */
    private final ObjectMapper objectMapper;

    /**
     * Nacos 服务注册对象（通过 ObjectProvider 注入，未启用 Nacos 时为 null）。
     */
    private final NacosAutoServiceRegistration nacosAutoServiceRegistration;

    /**
     * 默认语言（Accept-Language 缺失或匹配失败时使用）。
     */
    private final Locale DEFAULT_LOCALE;

    /**
     * 可选语言列表（参与 Accept-Language 的 lookup 匹配）。
     */
    private final List<Locale> LOCALE_LIST;

    /**
     * 通用配置。
     */
    private final CommonAppProperties commonAppProperties;

    /**
     * Accept-Language → Locale 的解析缓存，避免重复 lookup。
     */
    private final LoadingCache<String, Locale> LOCALE_CACHE = Caffeine.newBuilder().maximumSize(1000).build(new CacheLoader<>() {

        @Override
        public Locale load(@NotNull String language) {
            try {
                // 创建一个语言范围列表
                List<Locale.LanguageRange> languageRanges = Locale.LanguageRange.parse(language);
                // 创建 Locale 对象
                Locale locale = Locale.lookup(languageRanges, LOCALE_LIST);
                // 对 Locale做特殊处理
                if (locale != null) {
                    //对于香港地区单独处理成中文繁体。
                    if (locale.toLanguageTag().equals("zh-HK")) {
                        locale = Locale.TRADITIONAL_CHINESE;
                    }
                    return locale;
                }
            } catch (Exception e) {
                logger.error("解析Accept-Language[{}]报错！{}", language, e.getMessage());
            }
            return DEFAULT_LOCALE;
        }
    });

    /**
     * 构造函数。
     * <p>
     * NacosAutoServiceRegistration 通过 ObjectProvider 注入，使本公共库在未启用 Nacos 服务发现
     * 的应用中也能正常装配（如非 cloud 应用、本地测试）。
     * </p>
     *
     * @param commonAppProperties       通用应用配置
     * @param objectMapper              Jackson 对象映射器
     * @param nacosRegistrationProvider Nacos 服务注册对象提供者（可能不存在）
     */
    public CommonAppAutoConfiguration(CommonAppProperties commonAppProperties, ObjectMapper objectMapper,
                                      ObjectProvider<NacosAutoServiceRegistration> nacosRegistrationProvider) {
        this.commonAppProperties = commonAppProperties;
        this.DEFAULT_LOCALE = commonAppProperties.getLocaleDefault();
        this.LOCALE_LIST = commonAppProperties.getLocaleList();
        this.objectMapper = objectMapper;
        this.nacosAutoServiceRegistration = nacosRegistrationProvider.getIfAvailable();
    }

    /**
     * 语言解析器 Bean。
     * <p>
     * 从请求头 Accept-Language 解析 Locale（命中 LOCALE_CACHE），缺失时返回默认语言。
     * </p>
     *
     * @return Locale 解析器
     */
    @Bean
    @Primary
    public LocaleResolver localeResolver() {
        logger.info("CommonLocaleResolver Bean initialized");
        return new LocaleResolver() {

            @NotNull
            @Override
            public Locale resolveLocale(@NotNull HttpServletRequest request) {
                // 获取请求来的语言方式
                String language = request.getHeader(CommonConstants.ACCEPT_LANG);
                if (StringUtils.isNotBlank(language)) {
                    return LOCALE_CACHE.get(language);
                }
                // 最后给兜底默认语言。
                return DEFAULT_LOCALE;
            }

            @Override
            public void setLocale(@NotNull HttpServletRequest request, HttpServletResponse response, Locale locale) {

            }
        };
    }


    /**
     * 语言切换拦截器，通过请求参数 {@code lang} 切换 Locale。
     *
     * @return WebMvc 配置器
     */
    @Bean
    public WebMvcConfigurer localeInterceptor() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                LocaleChangeInterceptor localeInterceptor = new LocaleChangeInterceptor();
                localeInterceptor.setParamName("lang");
                registry.addInterceptor(localeInterceptor);
            }
        };
    }

    /**
     * 关键日志存储服务 Bean。
     * <p>
     * 唯一注册点（实现类 {@link SysCritLogStorageService} 不再标注 @Service），避免重复 bean 注册。
     * </p>
     *
     * @param uwAppBaseProperties 通用应用配置
     * @return 关键日志存储实现
     */
    @Bean
    @Primary
    public AuthCriticalLogStorage sysCritLogStorageService(CommonAppProperties uwAppBaseProperties) {
        logger.info("Init SysCritLogStorageService.");
        return new SysCritLogStorageService(uwAppBaseProperties);
    }

    /**
     * 注册 String→Date 转换器（基于 {@link DateUtils#stringToDate}）。
     *
     * @param registry 格式化注册器
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, Date.class, DateUtils::stringToDate);
    }

    /**
     * 移除 XML 消息转换器，禁用 XML 响应能力。
     *
     * @param converters HTTP 消息转换器列表
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(x -> x instanceof MappingJackson2XmlHttpMessageConverter);
    }

    /**
     * 定制 ObjectMapper：注册日期反序列化模块、设置时区、关闭未知属性失败。
     */
    @PostConstruct
    public void configureObjectMapper() {
        // 设置日期格式
        SimpleModule dateUtilModule = new SimpleModule();
        dateUtilModule.addDeserializer(Date.class, new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
                String dateString = jsonParser.getText();
                return DateUtils.stringToDate(dateString);
            }
        });
        // 不设置日期序列化的原因，是为了使用系统设置。
        objectMapper.registerModule(dateUtilModule);
        // 注意：时区跟随 JVM 运行机器（TimeZone.getDefault()）。多时区部署时需保证各节点 JVM 时区一致，
        // 否则 Date 序列化输出会产生漂移。如需固定时区，建议在应用层覆盖此 ObjectMapper 配置。
        objectMapper.setTimeZone(TimeZone.getDefault());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }

    /**
     * 禁用 Spring LoadBalancer 缓存层，直连 Nacos 服务发现，实现秒级响应服务上下线。
     * <p>
     * Spring LoadBalancer 默认使用 Caffeine 缓存（TTL 35 秒），导致服务下线后延迟感知。
     * 返回 NoOpCache，每次请求都 cache miss，直接走 delegate 从 Nacos 获取实例列表。
     * </p>
     *
     * @return NoOp 实现的 LoadBalancer 缓存管理器
     */
    @Bean
    @Primary
    LoadBalancerCacheManager loadBalancerCacheManager() {
        Cache noop = new NoOpCache("noop");
        return new LoadBalancerCacheManager() {
            @Override
            public Cache getCache(String name) {
                return noop;
            }

            @Override
            public Collection<String> getCacheNames() {
                return Collections.emptyList();
            }
        };
    }

    /**
     * 容器关闭事件处理：解决 Nacos 无法正确优雅停机的问题。
     * <p>
     * 先停止 Nacos 服务注册（触发反注册），再预留 {@link CommonAppProperties#getShutdownTimeout()}
     * 时长等待流量摘除。未启用 Nacos 时跳过。
     * </p>
     *
     * @param contextClosedEvent 容器关闭事件
     */
    @EventListener(ContextClosedEvent.class)
    void onContextClosedEvent(ContextClosedEvent contextClosedEvent) {
        // 未启用 Nacos 服务发现时跳过优雅停机逻辑
        if (nacosAutoServiceRegistration == null) {
            return;
        }
        logger.info("onContextClosedEvent stop nacos discovery service.");
        // 停止nacos服务注册
        nacosAutoServiceRegistration.stop();
        // 预留3s的停止时间
        ThreadUtils.sleepQuietly(commonAppProperties.getShutdownTimeout());
    }

    /**
     * 按 Profile 禁用 Swagger：仅在非 debug/dev 环境、且未显式关闭禁用时生效。
     *
     * @return 拦截 Swagger 路径的 WebMvc 配置器
     */
    @Bean
    @Profile("!debug & !dev")
    @ConditionalOnProperty(name = "uw.common.app.disableSwagger", havingValue = "false", matchIfMissing = true)
    public WebMvcConfigurer swaggerProfileDisableInterceptor() {
        return swaggerDisableInterceptor();
    }

    /**
     * 强制禁用 Swagger：仅由配置项控制（无视 Profile），disableSwagger=true 时所有环境均禁用。
     *
     * @return 拦截 Swagger 路径的 WebMvc 配置器
     */
    @Bean
    @ConditionalOnProperty(name = "uw.common.app.disableSwagger", havingValue = "true")
    public WebMvcConfigurer swaggerForceDisableInterceptor() {
        return swaggerDisableInterceptor();
    }

    /**
     * 构建 Swagger 禁用拦截器：对 /v3/api-docs/**、/swagger-ui 路径抛出 NoResourceFoundException。
     *
     * @return WebMvc 配置器
     */
    private WebMvcConfigurer swaggerDisableInterceptor() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                        HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());
                        throw new NoResourceFoundException(httpMethod, request.getRequestURI());
                    }
                }).addPathPatterns("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**");
            }
        };
    }

}
