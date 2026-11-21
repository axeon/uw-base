package uw.oauth2.client.conf;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import uw.oauth2.client.OAuth2ClientHelper;
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
 * 启动自动配置。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({UwOAuth2ClientProperties.class})
public class UwOAuth2ClientAutoConfiguration {

    /**
     * 创建OAuth2客户端帮助类
     *
     * @param oauth2ClientProperties OAuth2客户端配置
     * @return OAuth2客户端帮助类
     */
    @Bean
    @ConditionalOnMissingBean
    public OAuth2ClientHelper oAuth2ClientHelper(UwOAuth2ClientProperties oauth2ClientProperties) {
        return new OAuth2ClientHelper(oauth2ClientProperties);
    }


    /**
     * uw.oauth2.client 属性 nacos 热更新监听器（classpath 有 spring-cloud-context 才装配；本地/纯测试行为不变）。
     *
     * @param properties 属性 bean
     * @return OAuth2ClientPropertiesRefresher
     */
    @ConditionalOnClass(EnvironmentChangeEvent.class)
    @Bean
    OAuth2ClientPropertiesRefresher oAuth2ClientPropertiesRefresher(UwOAuth2ClientProperties properties) {
        return new OAuth2ClientPropertiesRefresher(properties);
    }

    /**
     * uw.oauth2.client 属性 nacos 热更新监听（嵌套装配，见类方法 javadoc）。
     */
    static class OAuth2ClientPropertiesRefresher implements EnvironmentAware {

        /**
         * 日志器。
         */
        private static final Logger log = LoggerFactory.getLogger(OAuth2ClientPropertiesRefresher.class);

        /**
         * 配置前缀。
         */
        private static final String CONFIG_PREFIX = "uw.oauth2.client";

        /**
         * 属性 bean（原地重绑目标）。
         */
        private final UwOAuth2ClientProperties properties;

        /**
         * Spring 环境（重绑配置源）。
         */
        private Environment environment;

        /**
         * @param properties 属性 bean
         */
        public OAuth2ClientPropertiesRefresher(UwOAuth2ClientProperties properties) {
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
