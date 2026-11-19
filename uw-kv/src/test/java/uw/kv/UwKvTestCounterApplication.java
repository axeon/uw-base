package uw.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

@SpringBootApplication
public class UwKvTestCounterApplication {

    private static final Logger log = LoggerFactory.getLogger(UwKvTestCounterApplication.class);

    public static void main(String[] args) throws InterruptedException {
        new SpringApplicationBuilder(UwKvTestCounterApplication.class).beanNameGenerator((beanDefinition, beanDefinitionRegistry) -> {
            String beanClassName = beanDefinition.getBeanClassName();
            if (beanClassName.startsWith("uw.kv")) {
                return beanClassName;
            }
            if (beanClassName.endsWith("LoadBalancerAutoConfiguration")) {
                return beanClassName;
            }
            return new AnnotationBeanNameGenerator().generateBeanName(beanDefinition, beanDefinitionRegistry);
        }).run(args);
        FusionCounter.config("testCounter", 10_000L, 10_000L, (key, value) -> log.info(key + ":" + value + " write back!"));
        FusionCounter.init("testCounter", 100, 10_000L);
        for (int i = 0; i < 1000; i++) {
            FusionCounter.increment("testCounter", 100);
            Thread.sleep(1000);
        }
    }
}
