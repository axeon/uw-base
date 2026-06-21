package uw.mydb.client.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * uw-mydb-client 连接配置。
 * <p>
 * 配置前缀：{@code uw.mydb}。
 *
 * @author axeon
 */
@ConfigurationProperties(prefix = "uw.mydb")
public class UwMydbClientProperties {

    /**
     * mydb-center 服务地址，默认指向服务发现名称 {@code http://uw-mydb-center}。
     * <p>
     * 可通过配置项 {@code uw.mydb.mydb-center-host} 覆盖。
     */
    private String mydbCenterHost = "http://uw-mydb-center";


    public String getMydbCenterHost() {
        return mydbCenterHost;
    }

    public void setMydbCenterHost(String mydbCenterHost) {
        this.mydbCenterHost = mydbCenterHost;
    }
}
