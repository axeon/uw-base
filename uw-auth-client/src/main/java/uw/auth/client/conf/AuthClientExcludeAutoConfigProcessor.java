package uw.auth.client.conf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 排除自动配置处理器。
 * <p>
 * 本模块引入了 spring-boot-starter-webflux（用于提供 WebClient），当宿主应用是 Servlet 栈时，
 * Spring Boot 的 {@code ClientHttpConnectorAutoConfiguration} 会与本库的 WebClient 配置冲突。
 * 此处在 bootstrap 早期阶段将该自动配置类加入 {@code spring.autoconfigure.exclude}。
 * <p>
 * 注册方式：必须通过 {@code META-INF/spring.factories}（key =
 * {@code org.springframework.boot.env.EnvironmentPostProcessor}）注册，而非
 * {@code AutoConfiguration.imports}——后者只识别自动配置类，且生效时机晚于本处理器。
 */
public class AuthClientExcludeAutoConfigProcessor implements EnvironmentPostProcessor {

    /**
     * 需要排除的自动配置类全限定名。
     */
    private static final String TO_EXCLUDE =
            "org.springframework.boot.autoconfigure.web.reactive.function.client.ClientHttpConnectorAutoConfiguration";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String key = "spring.autoconfigure.exclude";
        String current = environment.getProperty(key, "");
        // 合并宿主已配置的 exclude，去重，避免重复追加。
        Set<String> excludes = new LinkedHashSet<>();
        if (StringUtils.hasText(current)) {
            excludes.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray(current)));
        }
        excludes.add(TO_EXCLUDE);
        environment.getSystemProperties().put(key, String.join(",", excludes));
    }
}
