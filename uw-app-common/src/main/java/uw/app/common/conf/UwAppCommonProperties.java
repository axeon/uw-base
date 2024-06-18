package uw.app.common.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "uw.app.common")
public class UwAppCommonProperties {

    /**
     * 开启CritLog数据记录服务。
     */
    private boolean enableCritLog = true;

    public boolean isEnableCritLog() {
        return enableCritLog;
    }

    public void setEnableCritLog(boolean enableCritLog) {
        this.enableCritLog = enableCritLog;
    }
}
