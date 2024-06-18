package uw.notify.client.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置类。
 *
 * @author axeon
 */
@ConfigurationProperties(prefix = "uw.notify")
public class UwNotifyProperties {

    /**
     * notify服务器
     */
    private String notifyCenterHost = "http://uw-notify-center";


    public String getNotifyCenterHost() {
        return notifyCenterHost;
    }

    public void setNotifyCenterHost(String notifyCenterHost) {
        this.notifyCenterHost = notifyCenterHost;
    }
}
