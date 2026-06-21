package uw.notify.client.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * uw-notify-client 连接配置。
 * <p>
 * 配置前缀：{@code uw.notify}。默认指向服务发现名称 {@code http://uw-notify-center}，
 * 由 {@code NotifyClientHelper} 用于拼装 RPC 调用地址。
 *
 * @author axeon
 */
@ConfigurationProperties(prefix = "uw.notify")
public class UwNotifyProperties {

    /**
     * notify-center 服务地址，默认指向服务发现名称 {@code http://uw-notify-center}。
     * <p>
     * 可通过配置项 {@code uw.notify.notify-center-host} 覆盖。
     */
    private String notifyCenterHost = "http://uw-notify-center";


    public String getNotifyCenterHost() {
        return notifyCenterHost;
    }

    public void setNotifyCenterHost(String notifyCenterHost) {
        this.notifyCenterHost = notifyCenterHost;
    }
}
