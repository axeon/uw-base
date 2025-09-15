package uw.auth.client.conf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 排除自动配置处理器。
 * 主要用于排除webClient的自动配置类。
 */
public class AuthClientExcludeAutoConfigProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String key = "spring.autoconfigure.exclude";
        String toExclude = "org.springframework.boot.autoconfigure.web.reactive.function.client.ClientHttpConnectorAutoConfiguration";
        String current = environment.getProperty(key, "");
        if (!current.isEmpty()) {
            toExclude = current + "," + toExclude;
        }
        environment.getSystemProperties().put(key, toExclude);
    }
}
