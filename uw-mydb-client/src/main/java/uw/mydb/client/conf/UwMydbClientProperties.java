package uw.mydb.client.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置类。
 *
 * @author axeon
 */
@ConfigurationProperties(prefix = "uw.mydb")
public class UwMydbClientProperties {

    /**
     * notify服务器
     */
    private String mydbCenterHost = "http://uw-mydb-center";


    public String getMydbCenterHost() {
        return mydbCenterHost;
    }

    public void setMydbCenterHost(String mydbCenterHost) {
        this.mydbCenterHost = mydbCenterHost;
    }
}
