package uw.notify.client.conf;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import uw.notify.client.NotifyClientHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

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

    /**
     * uw.notify 属性 nacos 热更新监听器（classpath 有 spring-cloud-context 才装配；本地/纯测试行为不变）。
     *
     * @param properties 属性 bean
     * @return UwNotifyPropertiesRefresher
     */
    @ConditionalOnClass(EnvironmentChangeEvent.class)
    @Bean
    UwNotifyPropertiesRefresher uwNotifyPropertiesRefresher(UwNotifyProperties properties) {
        return new UwNotifyPropertiesRefresher(properties);
    }

    /**
     * uw.notify 属性 nacos 热更新监听（嵌套装配，见类方法 javadoc）。
     */
    static class UwNotifyPropertiesRefresher implements EnvironmentAware {

        /**
         * 日志器。
         */
        private static final Logger log = LoggerFactory.getLogger(UwNotifyPropertiesRefresher.class);

        /**
         * 配置前缀。
         */
        private static final String CONFIG_PREFIX = "uw.notify";

        /**
         * 属性 bean（原地重绑目标）。
         */
        private final UwNotifyProperties properties;

        /**
         * Spring 环境（重绑配置源）。
         */
        private Environment environment;

        /**
         * @param properties 属性 bean
         */
        public UwNotifyPropertiesRefresher(UwNotifyProperties properties) {
            this.properties = properties;
        }

        /**
         * 注入 Spring 环境。
         *
         * @param environment Spring 环境
         */
        @Override
        public void setEnvironment(Environment environment) {
            this.environment = environment;
        }

        /**
         * 环境变更（nacos 推送等）：原地重绑属性。
         *
         * @param event 环境变更事件
         */
        @EventListener(EnvironmentChangeEvent.class)
        public void onEnvironmentChange(EnvironmentChangeEvent event) {
            if (event.getKeys() == null || event.getKeys().stream().noneMatch(k -> k.startsWith(CONFIG_PREFIX))) {
                return;
            }
            try {
                Binder.get(environment).bind(CONFIG_PREFIX, Bindable.ofInstance(properties));
                log.info("{} 属性已原地热更新（静态持有方自动生效；构造期捕获的组件不受影响）", CONFIG_PREFIX);
            } catch (Exception e) {
                log.warn("{} 属性热更新重绑失败, 保持当前生效配置: {}", CONFIG_PREFIX, e.getMessage());
            }
        }
    }
}
