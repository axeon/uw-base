package uw.notify.client.conf;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import uw.notify.client.NotifyClientHelper;

/**
 * uw-notify-client 自动装配配置类。
 * <p>
 * 注册 {@link NotifyClientHelper} Bean，通过其构造方法完成对静态 {@link RestClient}
 * 与 {@link UwNotifyProperties} 的注入。{@code uw-auth-client} 提供的共享
 * {@code authRestClient}（带鉴权拦截器）会自动注入。
 *
 * @author axeon
 */
@Configuration
@EnableConfigurationProperties({UwNotifyProperties.class})
public class UwNotifyAutoConfiguration {

    /**
     * 创建 NotifyClientHelper Bean 并完成静态依赖注入。
     * <p>
     * 当容器中不存在同类型 Bean 时才生效，允许业务方覆盖默认实现。
     *
     * @param uwNotifyProperties notify-center 连接配置
     * @param authRestClient     共享的带鉴权 RestClient（由 uw-auth-client 提供）
     * @return 已完成初始化的 NotifyClientHelper 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public NotifyClientHelper notifyClientHelper(UwNotifyProperties uwNotifyProperties, RestClient authRestClient) {
        return new NotifyClientHelper(uwNotifyProperties, authRestClient);
    }
}
