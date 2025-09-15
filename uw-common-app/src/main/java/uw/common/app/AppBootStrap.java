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
        /* 1. 取主类所在包作为保留前缀 */
        final String basePackage = primarySource.getPackage().getName();

        new SpringApplicationBuilder(primarySource).beanNameGenerator((def, reg) -> {
            String className = def.getBeanClassName();
            if (className == null) {
                return new AnnotationBeanNameGenerator().generateBeanName(def, reg);
            }

            // 本项目包下的 bean 直接用全限定名
            if (className.startsWith(basePackage)) {
                return className;
            }
            // 临时兼容
            if (className.endsWith("ClientHttpConnectorAutoConfiguration")
                    || className.endsWith("LoadBalancerAutoConfiguration")) {
                return className;
            }
            // 其余走默认
            return new AnnotationBeanNameGenerator().generateBeanName(def, reg);
        }).run(args);
    }

}
