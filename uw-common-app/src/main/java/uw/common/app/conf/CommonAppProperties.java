package uw.common.app.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

@ConfigurationProperties(prefix = "uw.common.app")
public class CommonAppProperties {

    /**
     * 开启CritLog数据记录服务。
     */
    private boolean enableCritLog = true;

    /**
     * 默认语言。
     */
    private Locale defaultLocale = Locale.SIMPLIFIED_CHINESE;

    public boolean isEnableCritLog() {
        return enableCritLog;
    }

    public void setEnableCritLog(boolean enableCritLog) {
        this.enableCritLog = enableCritLog;
    }

    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }
}
