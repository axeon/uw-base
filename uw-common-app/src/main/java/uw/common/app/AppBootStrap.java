package uw.common.app;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
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
                // 项目自身SwaggerConfig处理
                if (className.endsWith("SwaggerConfig")) {
                    return className;
                }

                // Controller单独处理
                if (def instanceof AnnotatedBeanDefinition abd &&
                        abd.getMetadata().getAnnotationTypes().stream()
                                .anyMatch(t -> t.endsWith("Controller"))) {
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
