package uw.common.app;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

/**
 * 应用启动类。
 */
public class AppBootStrap {

    private AppBootStrap() {
    }

    public static void run(Class<?> primarySource, String[] args) {
        final String basePackage = primarySource.getPackage().getName();

        new SpringApplicationBuilder(primarySource).beanNameGenerator((def, reg) -> {
            String className = def.getBeanClassName();
            if (className == null) {
                return new AnnotationBeanNameGenerator().generateBeanName(def, reg);
            }
            // 本项目包下判定
            if (className.startsWith(basePackage)) {
                if (className.endsWith("$PackageInfo$") || className.endsWith("SwaggerConfig") || className.endsWith("Controller") || className.endsWith("Runner") || className.endsWith("Croner")) {
                    return className;
                }

            }

            // 全局判定，临时兼容webflux和loadbalancer问题。
            if (className.contains("ClientHttpConnectorAutoConfiguration")
                    || className.endsWith("LoadBalancerAutoConfiguration")) {
                return className;
            }

            // 其余走默认
            return new AnnotationBeanNameGenerator().generateBeanName(def, reg);
        }).run(args);
    }

}
