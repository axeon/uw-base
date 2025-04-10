package uw.common.app.conf;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
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
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import uw.auth.service.conf.AuthServiceAutoConfiguration;
import uw.auth.service.log.AuthCriticalLogStorage;
import uw.common.app.constant.CommonConstants;
import uw.common.app.service.SysCritLogStorageService;

import java.util.List;
import java.util.Locale;

/**
 * 启动配置。
 */
@Configuration
@AutoConfigureBefore({WebMvcAutoConfiguration.class})
@AutoConfigureAfter(AuthServiceAutoConfiguration.class)
@EnableConfigurationProperties({CommonAppProperties.class})
public class CommonAppAutoConfiguration {

    /**
     * 日志.
     */
    private static final Logger logger = LoggerFactory.getLogger(CommonAppAutoConfiguration.class);
    /**
     * 语言列表.
     */
    private static final List<Locale> LOCALE_LIST = List.of(Locale.getAvailableLocales());
    /**
     * 缓存语言对象.
     */
    private static final LoadingCache<String, Locale> LOCALE_CACHE = Caffeine.newBuilder().maximumSize(1000).build(new CacheLoader<>() {

        @Override
        public @Nullable Locale load(String language) {
            if (StringUtils.isNotBlank(language)) {
                try {
                    // 创建一个语言范围列表
                    List<Locale.LanguageRange> languageRanges = List.of(new Locale.LanguageRange(language));
                    // 创建 Locale 对象
                    Locale locale = Locale.lookup(languageRanges, LOCALE_LIST);
                    // 如果找不到匹配的语言范围，则返回默认语言
                    if (locale == null) {
                        locale = Locale.getDefault();
                    }
                    //对于香港地区单独处理成中文繁体。
                    if (locale.toLanguageTag().equals("zh-HK")) {
                        locale = Locale.TRADITIONAL_CHINESE;
                    }
                    return locale;
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            return Locale.getDefault();
        }
    });


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
                return LOCALE_CACHE.get(language);
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
        logger.info("init SysCritLogStorageService!");
        return new SysCritLogStorageService(uwAppBaseProperties);
    }

}
