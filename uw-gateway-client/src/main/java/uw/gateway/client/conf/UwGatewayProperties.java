package uw.gateway.client.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * uw-gateway-client 连接配置。
 * <p>
 * 配置前缀：{@code uw.gateway}。
 *
 * @author axeon
 */
@ConfigurationProperties(prefix = "uw.gateway")
public class UwGatewayProperties {

    /**
     * gateway-center 服务地址，默认指向服务发现名称 {@code http://uw-gateway-center}。
     * <p>
     * 可通过配置项 {@code uw.gateway.gateway-center-host} 覆盖。
     */
    private String gatewayCenterHost = "http://uw-gateway-center";


    public String getGatewayCenterHost() {
        return gatewayCenterHost;
    }

    public void setGatewayCenterHost(String gatewayCenterHost) {
        this.gatewayCenterHost = gatewayCenterHost;
    }
}
