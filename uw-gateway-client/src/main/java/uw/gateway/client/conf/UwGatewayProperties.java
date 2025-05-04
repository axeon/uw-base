package uw.gateway.client.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置类。
 *
 * @author axeon
 */
@ConfigurationProperties(prefix = "uw.gateway")
public class UwGatewayProperties {

    /**
     * Gateway服务器
     */
    private String GatewayCenterHost = "http://uw-gateway-center";


    public String getGatewayCenterHost() {
        return GatewayCenterHost;
    }

    public void setGatewayCenterHost(String GatewayCenterHost) {
        this.GatewayCenterHost = GatewayCenterHost;
    }
}
