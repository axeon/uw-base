package uw.tinyurl.client.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * uw-tinyurl-client 连接配置。
 * <p>
 * 配置前缀：{@code uw.tinyurl}。
 *
 * @author axeon
 */
@ConfigurationProperties(prefix = "uw.tinyurl")
public class UwTinyurlProperties {

    /**
     * tinyurl-center 服务地址，默认指向服务发现名称 {@code http://uw-tinyurl-center}。
     * <p>
     * 可通过配置项 {@code uw.tinyurl.tinyurl-center-host} 覆盖。
     */
    private String tinyurlCenterHost = "http://uw-tinyurl-center";


    public String getTinyurlCenterHost() {
        return tinyurlCenterHost;
    }

    public void setTinyurlCenterHost(String tinyurlCenterHost) {
        this.tinyurlCenterHost = tinyurlCenterHost;
    }
}
