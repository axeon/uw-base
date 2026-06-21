package uw.tinyurl.client.conf;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import uw.tinyurl.client.TinyurlClientHelper;

/**
 * uw-tinyurl-client 自动装配配置类。
 * <p>
 * 注册 {@link TinyurlClientHelper} Bean，通过其构造方法完成对静态 {@link RestClient}
 * 与 {@link UwTinyurlProperties} 的注入。{@code uw-auth-client} 提供的共享
 * {@code authRestClient}（带鉴权拦截器）会自动注入。
 *
 * @author axeon
 */
@Configuration
@EnableConfigurationProperties({UwTinyurlProperties.class})
public class UwTinyurlAutoConfiguration {

    /**
     * 创建 TinyurlClientHelper Bean 并完成静态依赖注入。
     * <p>
     * 当容器中不存在同类型 Bean 时才生效，允许业务方覆盖默认实现。
     *
     * @param uwTinyurlProperties tinyurl-center 连接配置
     * @param authRestClient      共享的带鉴权 RestClient（由 uw-auth-client 提供）
     * @return 已完成初始化的 TinyurlClientHelper 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public TinyurlClientHelper tinyurlClientHelper(UwTinyurlProperties uwTinyurlProperties, RestClient authRestClient) {
        return new TinyurlClientHelper(uwTinyurlProperties, authRestClient);
    }
}
