package uw.common.app.conf;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.module.SimpleModule;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import uw.auth.service.conf.UwAuthServiceAutoConfiguration;
import uw.auth.service.log.AuthCriticalLogStorage;
import uw.common.constant.CommonConstants;
import uw.common.app.service.SysCritLogStorageService;
import uw.common.util.DateTools;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * uw-common-app 启动配置。
 * <p>
 * 在 {@link UwAuthServiceAutoConfiguration} 之后、{@link WebMvcAutoConfiguration} 之前装配，
 * 提供国际化、关键日志存储、LoadBalancer 缓存、Nacos 优雅停机、Swagger 禁用、ObjectMapper 定制等公共能力。
 * </p>
 */
@Configuration
@AutoConfigureBefore({WebMvcAutoConfiguration.class})
@AutoConfigureAfter(UwAuthServiceAutoConfiguration.class)
@EnableConfigurationProperties({UwCommonAppProperties.class})
public class UwCommonAppAutoConfiguration implements WebMvcConfigurer {

    /**
     * 日志记录器。
     */
    private static final Logger logger = LoggerFactory.getLogger(UwCommonAppAutoConfiguration.class);

    /**
     * Date 输出格式开关（作用于 @ResponseBody 全链路的 Spring Boot JsonMapper）。
     * <p>{@code true}（默认）= ISO 字符串 {@code yyyy-MM-dd'T'HH:mm:ss.SSSZZ}，与 uw-common 的 JsonUtils 一致；
     * {@code false} = Jackson3 默认的毫秒时间戳（long）。
     * <p><b>切换方式</b>：改这一个值，重新编译 uw-common-app 即可（下游 center build 后生效）。
     */
    private static final boolean DATE_AS_ISO = true;

    /**
     * 默认语言（Accept-Language 缺失或匹配失败时使用）。
     */
    private final Locale DEFAULT_LOCALE;

    /**
     * 可选语言列表（参与 Accept-Language 的 lookup 匹配）。
     */
    private final List<Locale> LOCALE_LIST;

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
     *
     * @param commonAppProperties 通用应用配置
     */
    public UwCommonAppAutoConfiguration(UwCommonAppProperties commonAppProperties) {
        this.DEFAULT_LOCALE = commonAppProperties.getLocaleDefault();
        this.LOCALE_LIST = commonAppProperties.getLocaleList();
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
    public AuthCriticalLogStorage sysCritLogStorageService(UwCommonAppProperties uwAppBaseProperties) {
        logger.info("Init SysCritLogStorageService.");
        return new SysCritLogStorageService(uwAppBaseProperties);
    }

    /**
     * 注册 String→Date 转换器（基于 {@link DateTools#stringToDate}）。
     *
     * @param registry 格式化注册器
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, Date.class, DateTools::stringToDate);
    }

    /**
     * 移除 XML 消息转换器，禁用 XML 响应能力。
     * <p>Spring 7 起 {@code WebMvcConfigurer.extendMessageConverters(List)} 已 {@code @Deprecated(forRemoval)}，
     * 改用 {@code configureMessageConverters(ServerBuilder)}，通过 {@code configureMessageConvertersList}
     * 操作 converter 列表（等价于原先的 removeIf）。
     *
     * @param builder 服务端消息转换器构建器
     */
    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        builder.configureMessageConvertersList(converters ->
                converters.removeIf(x -> x instanceof JacksonXmlHttpMessageConverter));
    }

    /**
     * 定制 Spring 容器中的 Jackson JsonMapper：注册日期反序列化模块、设置时区、关闭未知属性失败。
     * <p>
     * Jackson 3 中 ObjectMapper 构建后不可变，无法再像 Jackson 2 那样在 @PostConstruct 里 mutate
     * 已注入的 Bean。这里改为提供 {@link JsonMapperBuilderCustomizer}，由 Spring Boot 在 builder
     * 阶段应用同样的 module/timeZone/feature 配置（等价于原先的 registerModule/setTimeZone/configure）。
     * </p>
     *
     * @return JsonMapperBuilderCustomizer 定制器
     */
    @Bean
    public JsonMapperBuilderCustomizer commonAppJsonMapperCustomizer() {
        // 设置日期格式
        SimpleModule dateUtilModule = new SimpleModule();
        dateUtilModule.addDeserializer(Date.class, new ValueDeserializer<Date>() {
            @Override
            public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
                String dateString = jsonParser.getString();
                return DateTools.stringToDate(dateString);
            }
        });
        // DATE_AS_ISO=true：注册 ISO 序列化器（与 uw-common 的 JsonUtils 输出对齐）；
        // DATE_AS_ISO=false：不注册，由 Jackson3 默认（WRITE_DATES_AS_TIMESTAMPS=true）输出毫秒时间戳。
        if (DATE_AS_ISO) {
            dateUtilModule.addSerializer(Date.class, new ValueSerializer<Date>() {
                @Override
                public void serialize(Date date, JsonGenerator jsonGenerator, SerializationContext serializationContext) {
                    jsonGenerator.writeString(DateTools.dateToString(date, DateTools.DATE_MILLIS_ISO));
                }
            });
        }
        // 注意：时区跟随 JVM 运行机器（TimeZone.getDefault()）。多时区部署时需保证各节点 JVM 时区一致，
        // 否则 Date 序列化输出会产生漂移。如需固定时区，建议在应用层覆盖此 ObjectMapper 配置。
        return builder -> {
            builder.addModule(dateUtilModule)
                    .defaultTimeZone(TimeZone.getDefault());
            // ISO 模式显式关掉时间戳（自定义序列化器已 override，此处保证语义清晰，方便切换排查）
            if (DATE_AS_ISO) {
                builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
            }
            builder.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        };
    }

}
