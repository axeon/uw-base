package uw.common.app;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

/**
 * 应用启动引导类。
 * <p>
 * 封装 {@link SpringApplicationBuilder}，并注入自定义 {@link org.springframework.beans.factory.support.BeanNameGenerator}，
 * 解决多模块场景下同后缀（Controller/Runner/Croner/SwaggerConfig 等）组件的 bean 名称冲突，
 * 以及 webflux/servlet 与 loadbalancer 同时引入时 AutoConfiguration 的 bean 名冲突。
 * </p>
 * 使用示例：
 * <pre>{@code
 * public static void main(String[] args) {
 *     AppBootStrap.run(MyApplication.class, args);
 * }
 * }</pre>
 */
public class AppBootStrap {

    /**
     * 私有构造器，禁止实例化。
     */
    private AppBootStrap() {
    }

    /**
     * 启动 Spring Boot 应用。
     * <p>
     * 以 {@code primarySource} 所在包为基准包，对基准包下的 {@code $PackageInfo$}、{@code SwaggerConfig}、
     * {@code Controller}、{@code Runner}、{@code Croner} 后缀类使用全限定类名作为 bean 名称；
     * 对全局的 {@code ClientHttpConnectorAutoConfiguration}、{@code LoadBalancerAutoConfiguration} 同样使用全限定类名（兼容性处理）；
     * 其余 bean 走 Spring 默认命名策略。
     * </p>
     *
     * @param primarySource 主配置类（通常为标注 @SpringBootApplication 的类）
     * @param args          启动参数
     */
    public static void run(Class<?> primarySource, String[] args) {
        final String basePackage = primarySource.getPackage().getName();

        new SpringApplicationBuilder(primarySource).beanNameGenerator((def, reg) -> {
            String className = def.getBeanClassName();
            if (className == null) {
                return new AnnotationBeanNameGenerator().generateBeanName(def, reg);
            }
            // 本项目包下判定：对多实例型组件（Controller/Runner/Croner/SwaggerConfig/PackageInfo）
            // 使用全限定类名作为 bean 名称，避免短名冲突导致 bean 被覆盖。
            // 注意：基于类名后缀匹配是隐式约定，新增同类后缀的第三方库类也会命中，
            // 引入新依赖时需评估是否存在意外命中。
            if (className.startsWith(basePackage)) {
                if (className.endsWith("$PackageInfo$") || className.endsWith("SwaggerConfig") || className.endsWith("Controller") || className.endsWith("Runner") || className.endsWith("Croner")) {
                    return className;
                }

            }

            // 全局判定：以下两个 AutoConfiguration 在 webflux/servlet 共存或 reactor-netty 与
            // loadbalancer 同时在场时，默认短名生成会引发 bean 名冲突。
            // TODO: 升级 Spring Boot/Cloud 后验证是否仍需此兼容，根因消除后应移除。
            if (className.contains("ClientHttpConnectorAutoConfiguration")
                    || className.endsWith("LoadBalancerAutoConfiguration")) {
                return className;
            }

            // 其余走默认
            return new AnnotationBeanNameGenerator().generateBeanName(def, reg);
        }).run(args);
    }

}
