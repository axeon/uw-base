package uw.common.app.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
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
    private Locale localeDefault = Locale.SIMPLIFIED_CHINESE;

    /**
     * 可选的语言列表。
     */
    private List<Locale> localeList = List.of(Locale.getAvailableLocales());

    public boolean isEnableCritLog() {
        return enableCritLog;
    }

    public void setEnableCritLog(boolean enableCritLog) {
        this.enableCritLog = enableCritLog;
    }

    public Locale getLocaleDefault() {
        return localeDefault;
    }

    public void setLocaleDefault(Locale localeDefault) {
        this.localeDefault = localeDefault;
    }

    public List<Locale> getLocaleList() {
        return localeList;
    }

    public void setLocaleList(List<Locale> localeList) {
        this.localeList = localeList;
    }
}
