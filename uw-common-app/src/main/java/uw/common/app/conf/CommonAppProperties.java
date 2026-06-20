package uw.common.app.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * uw-common-app 配置属性。
 * <p>
 * 配置前缀为 {@code uw.common.app}，控制关键日志、i18n、Swagger 与优雅关闭等行为。
 * </p>
 */
@ConfigurationProperties(prefix = "uw.common.app")
public class CommonAppProperties {

    /**
     * 开启CritLog数据记录服务。
     */
    private boolean enableCritLog = true;

    /**
     * 默认语言。
     */
    private Locale localeDefault = Locale.SIMPLIFIED_CHINESE;

    /**
     * 可选的语言列表。
     * 默认仅包含 i18n 资源实际覆盖的语言，避免全量 Locale（约160个）导致
     * LOCALE_CACHE 命中率低与无意义的 lookup 开销。可通过配置覆盖。
     */
    private List<Locale> localeList = List.of(
            Locale.SIMPLIFIED_CHINESE,
            Locale.TRADITIONAL_CHINESE,
            Locale.ENGLISH,
            Locale.JAPANESE,
            Locale.KOREAN,
            Locale.GERMAN,
            Locale.FRENCH,
            Locale.ITALIAN,
            Locale.of("es"),
            Locale.of("ru"),
            Locale.of("pt"),
            Locale.of("ar")
    );

    /**
     * 优雅关闭超时时间。
     */
    private Duration shutdownTimeout = Duration.ofSeconds(3);

    /**
     * 禁用swagger
     */
    private boolean disableSwagger = false;

    /**
     * 是否开启 CritLog 数据记录服务。
     *
     * @return true 表示开启，false 表示关闭
     */
    public boolean isEnableCritLog() {
        return enableCritLog;
    }

    /**
     * 设置是否开启 CritLog 数据记录服务。
     *
     * @param enableCritLog 是否开启
     */
    public void setEnableCritLog(boolean enableCritLog) {
        this.enableCritLog = enableCritLog;
    }

    /**
     * 获取默认语言（Accept-Language 缺失或匹配失败时使用）。
     *
     * @return 默认语言
     */
    public Locale getLocaleDefault() {
        return localeDefault;
    }

    /**
     * 设置默认语言。
     *
     * @param localeDefault 默认语言
     */
    public void setLocaleDefault(Locale localeDefault) {
        this.localeDefault = localeDefault;
    }

    /**
     * 获取可选语言列表（不会返回 null，未配置时返回空列表）。
     *
     * @return 可选语言列表
     */
    public List<Locale> getLocaleList() {
        return localeList != null ? localeList : List.of();
    }

    /**
     * 设置可选语言列表（null 入参会被归一化为空列表）。
     *
     * @param localeList 可选语言列表
     */
    public void setLocaleList(List<Locale> localeList) {
        this.localeList = localeList != null ? localeList : List.of();
    }

    /**
     * 获取优雅关闭超时时间（Nacos 反注册后预留的停机等待时长）。
     *
     * @return 优雅关闭超时时间
     */
    public Duration getShutdownTimeout() {
        return shutdownTimeout;
    }

    /**
     * 设置优雅关闭超时时间。
     *
     * @param shutdownTimeout 优雅关闭超时时间
     */
    public void setShutdownTimeout(Duration shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }

    /**
     * 是否禁用 Swagger 接口（含 /v3/api-docs 与 /swagger-ui）。
     *
     * @return true 表示禁用
     */
    public boolean isDisableSwagger() {
        return disableSwagger;
    }

    /**
     * 设置是否禁用 Swagger 接口。
     *
     * @param disableSwagger 是否禁用
     */
    public void setDisableSwagger(boolean disableSwagger) {
        this.disableSwagger = disableSwagger;
    }
}
