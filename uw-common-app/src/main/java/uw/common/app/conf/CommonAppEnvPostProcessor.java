package uw.common.app.conf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * uw-common-app 环境后置处理器：以"免配置"默认项的形式注入两组开关。
 * <p>
 * 在 Spring Boot 环境准备阶段执行，将默认值放入名为 {@value #PROPERTY_SOURCE_NAME} 的
 * {@link MapPropertySource}，并以 {@link Ordered#LOWEST_PRECEDENCE} 最低优先级追加
 * （{@code addLast}），因此 <b>application.yml / 命令行 / 系统属性均可覆盖</b>这些默认值。
 * </p>
 * <ul>
 *   <li><b>禁用 Spring LoadBalancer 缓存</b>：无条件注入
 *       {@code spring.cloud.loadbalancer.cache.enabled=false}，使每次服务实例选取直连 Nacos，
 *       避免默认 35s Caffeine 缓存导致的上下线延迟感知。等价于原先手写的 NoOpCache Bean。</li>
 *   <li><b>按环境禁用 Swagger</b>：当 active profile 不含 {@code debug}/{@code dev}，或显式配置
 *       {@code uw.common.app.disableSwagger=true} 时，注入 {@code springdoc.api-docs.enabled=false}
 *       与 {@code springdoc.swagger-ui.enabled=false}。语义与原先的 Swagger 拦截器一致
 *       （禁用条件 = 非 dev/debug 环境，或强制 disableSwagger）。</li>
 * </ul>
 * <p>
 * 注意：order 取 {@link Ordered#LOWEST_PRECEDENCE}，确保在 {@code ConfigDataEnvironmentPostProcessor}
 * （读取 application.yml、解析 active profiles）之后执行，此时
 * {@link ConfigurableEnvironment#getActiveProfiles()} 与 {@code uw.common.app.disableSwagger} 均已就绪。
 * </p>
 * <p>
 * Nacos 优雅停机不在本处理器范围，由 uw-nacos-client 的 {@code NacosGracefulShutdownDelegate} 负责。
 * </p>
 */
public class CommonAppEnvPostProcessor implements EnvironmentPostProcessor, Ordered {

    /**
     * 注入的 PropertySource 名称。
     */
    private static final String PROPERTY_SOURCE_NAME = "uw-common-app-defaults";

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaults = new HashMap<>();

        // 1) 禁用 Spring LoadBalancer 缓存层，直连 Nacos 服务发现，实现秒级上下线感知。
        defaults.put("spring.cloud.loadbalancer.cache.enabled", "false");

        // 2) 按环境禁用 Swagger：非 debug/dev 环境，或显式 disableSwagger=true。
        //    与原 Swagger 拦截器语义一致：禁用条件 = (!dev/debug) || disableSwagger。
        boolean disableSwagger = environment.getProperty("uw.common.app.disableSwagger", Boolean.class, false);
        Set<String> activeProfiles = new HashSet<>(Arrays.asList(environment.getActiveProfiles()));
        boolean devDebugProfile = activeProfiles.contains("dev") || activeProfiles.contains("debug");
        if (!devDebugProfile || disableSwagger) {
            defaults.put("springdoc.api-docs.enabled", "false");
            defaults.put("springdoc.swagger-ui.enabled", "false");
        }

        // 以最低优先级追加：application.yml / 命令行 / 系统属性均可覆盖以上默认值。
        if (!environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
