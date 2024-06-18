package uw.tinyurl.client.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置类。
 *
 * @author axeon
 */
@ConfigurationProperties(prefix = "uw.tinyurl")
public class UwTinyurlProperties {

    /**
     * tinyurl服务器
     */
    private String tinyurlCenterHost = "http://uw-tinyurl-center";


    public String getTinyurlCenterHost() {
        return tinyurlCenterHost;
    }

    public void setTinyurlCenterHost(String tinyurlCenterHost) {
        this.tinyurlCenterHost = tinyurlCenterHost;
    }
}
