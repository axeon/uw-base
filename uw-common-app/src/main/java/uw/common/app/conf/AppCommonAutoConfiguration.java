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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import uw.common.app.constant.CommonConstants;
import uw.common.app.service.SysCritLogStorageService;
import uw.auth.service.conf.AuthServiceAutoConfiguration;
import uw.auth.service.log.AuthCriticalLogStorage;

import java.util.List;
import java.util.Locale;

/**
 * 启动配置。
 */
@Configuration
@AutoConfigureAfter(AuthServiceAutoConfiguration.class)
@EnableConfigurationProperties({CommonAppProperties.class})
public class AppCommonAutoConfiguration implements WebMvcConfigurer {

    /**
     * 日志.
     */
    private static final Logger logger = LoggerFactory.getLogger( AppCommonAutoConfiguration.class );
    /**
     * 语言列表.
     */
    private static final List<Locale> LOCALE_LIST = List.of( Locale.getAvailableLocales() );
    /**
     * 缓存语言对象.
     */
    private static final LoadingCache<String, Locale> LOCALE_CACHE = Caffeine.newBuilder().maximumSize( 1000 ).build( new CacheLoader<>() {

        @Override
        public @Nullable Locale load(String language) {
            if (StringUtils.isNotBlank( language )) {
                try {
                    // 创建一个语言范围列表
                    List<Locale.LanguageRange> languageRanges = List.of( new Locale.LanguageRange( language ) );
                    // 创建 Locale 对象
                    return Locale.lookup( languageRanges, LOCALE_LIST );
                } catch (Exception e) {
                    logger.error( e.getMessage(), e );
                }
            }
            return Locale.getDefault();
        }
    } );

    /**
     * 语言切换拦截器.
     *
     * @return
     */
    @Bean
    public LocaleChangeInterceptor commonLocaleChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName( "lang" ); // 指定语言参数名
        return interceptor;
    }

    /**
     * 语言解析器.
     *
     * @return
     */
    @Bean
    public LocaleResolver commonLocaleResolver() {
        return new LocaleResolver() {

            @NotNull
            @Override
            public Locale resolveLocale(@NotNull HttpServletRequest request) {
                // 获取请求来的语言方式
                String language = request.getHeader( CommonConstants.ACCEPT_LANG );
                return LOCALE_CACHE.get( language );
            }

            @Override
            public void setLocale(@NotNull HttpServletRequest request, HttpServletResponse response, Locale locale) {

            }
        };
    }

    /**
     * 拦截器注册.
     *
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor( commonLocaleChangeInterceptor() );
    }

    /**
     * critical日志存储服务.
     *
     * @return
     */
    @Bean
    @Primary
    public AuthCriticalLogStorage SysCritLogStorageService(CommonAppProperties uwAppBaseProperties) {
        logger.info( "init SysCritLogStorageService!" );
        return new SysCritLogStorageService( uwAppBaseProperties );
    }

}
