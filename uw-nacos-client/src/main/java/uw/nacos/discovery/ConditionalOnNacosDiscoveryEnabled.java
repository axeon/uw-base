package uw.nacos.discovery;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Nacos 服务发现启用条件注解。
 *
 * <p>当配置项 {@code spring.cloud.nacos.discovery.enabled} 为 true（或缺省时），
 * 才会激活被标注的自动配置类或 Bean。结合 {@code @ConditionalOnDiscoveryEnabled} 共同控制
 * Nacos 服务发现相关组件的装配。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@ConditionalOnProperty(value = "spring.cloud.nacos.discovery.enabled",
		matchIfMissing = true)
public @interface ConditionalOnNacosDiscoveryEnabled {

}
