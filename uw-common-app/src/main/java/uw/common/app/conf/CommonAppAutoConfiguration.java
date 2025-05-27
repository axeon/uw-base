package uw.common.app.conf;

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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import uw.auth.service.conf.AuthServiceAutoConfiguration;
import uw.auth.service.log.AuthCriticalLogStorage;
import uw.common.app.constant.CommonConstants;
import uw.common.app.service.SysCritLogStorageService;
import uw.common.util.DateUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 启动配置。
 */
@Configuration
@AutoConfigureBefore({WebMvcAutoConfiguration.class})
@AutoConfigureAfter(AuthServiceAutoConfiguration.class)
@EnableConfigurationProperties({CommonAppProperties.class})
public class CommonAppAutoConfiguration implements WebMvcConfigurer {

    /**
     * 日志.
     */
    private static final Logger logger = LoggerFactory.getLogger(CommonAppAutoConfiguration.class);

    /**
     * 对象映射器.
     */
    private final ObjectMapper objectMapper;

    /**
     * 默认语言.
     */
    private final Locale DEFAULT_LOCALE;

    /**
     * 语言列表.
     */
    private final List<Locale> LOCALE_LIST;

    /**
     * 缓存语言对象.
     */
    private final LoadingCache<String, Locale> LOCALE_CACHE = Caffeine.newBuilder().maximumSize(1000).build(new CacheLoader<>() {

        @Override
        public @Nullable Locale load(String language) {
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
     * 构造函数.
     *
     * @param commonAppProperties
     */
    public CommonAppAutoConfiguration(CommonAppProperties commonAppProperties, ObjectMapper objectMapper) {
        this.DEFAULT_LOCALE = commonAppProperties.getLocaleDefault();
        this.LOCALE_LIST = commonAppProperties.getLocaleList();
        this.objectMapper = objectMapper;
    }

    /**
     * 语言解析器.
     *
     * @return
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
     * 默认拦截器 其中lang表示切换语言的参数名
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
     * critical日志存储服务.
     *
     * @return
     */
    @Bean
    @Primary
    public AuthCriticalLogStorage SysCritLogStorageService(CommonAppProperties uwAppBaseProperties) {
        logger.info("Init SysCritLogStorageService.");
        return new SysCritLogStorageService(uwAppBaseProperties);
    }


    /**
     * 添加mvc的Date格式转换器.
     *
     * @param registry
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, Date.class, DateUtils::stringToDate);
    }

    /**
     * 移除XML消息转换器
     *
     * @param converters
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(x -> x instanceof MappingJackson2XmlHttpMessageConverter);
    }

    /**
     * 配置ObjectMapper.
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
        objectMapper.setTimeZone(TimeZone.getDefault());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }

}
