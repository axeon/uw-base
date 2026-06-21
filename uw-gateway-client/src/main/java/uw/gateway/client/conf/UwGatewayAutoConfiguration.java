package uw.gateway.client.conf;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import uw.gateway.client.GatewayClientHelper;

/**
 * uw-gateway-client 自动装配配置类。
 * <p>
 * 注册 {@link GatewayClientHelper} Bean，通过其构造方法完成对静态 {@link RestClient}
 * 与 {@link UwGatewayProperties} 的注入。{@code uw-auth-client} 提供的共享
 * {@code authRestClient}（带鉴权拦截器）会自动注入。
 *
 * @author axeon
 */
@Configuration
@EnableConfigurationProperties({UwGatewayProperties.class})
public class UwGatewayAutoConfiguration {

    /**
     * 创建 GatewayClientHelper Bean 并完成静态依赖注入。
     * <p>
     * 当容器中不存在同类型 Bean 时才生效，允许业务方覆盖默认实现。
     *
     * @param uwGatewayProperties gateway-center 连接配置
     * @param authRestClient      共享的带鉴权 RestClient（由 uw-auth-client 提供）
     * @return 已完成初始化的 GatewayClientHelper 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public GatewayClientHelper gatewayClientHelper(UwGatewayProperties uwGatewayProperties, RestClient authRestClient) {
        return new GatewayClientHelper(uwGatewayProperties, authRestClient);
    }
}
