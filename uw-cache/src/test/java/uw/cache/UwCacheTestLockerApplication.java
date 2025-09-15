package uw.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

@SpringBootApplication
public class UwCacheTestLockerApplication {

    private static final Logger log = LoggerFactory.getLogger( UwCacheTestLockerApplication.class );

    public static void main(String[] args) throws InterruptedException {
        new SpringApplicationBuilder( UwCacheTestLockerApplication.class ).beanNameGenerator( (beanDefinition, beanDefinitionRegistry) -> {
            String beanClassName = beanDefinition.getBeanClassName();
            if (beanClassName.startsWith( "uw.cache" )) {
                return beanClassName;
            }
            if (beanClassName.endsWith( "LoadBalancerAutoConfiguration" )) {
                return beanClassName;
            }
            return new AnnotationBeanNameGenerator().generateBeanName( beanDefinition, beanDefinitionRegistry );
        } ).run( args );
        for (int i = 0; i < 100; i++) {
            System.out.println( GlobalLocker.tryLock( "testLocker", 1, 10_000L ) );
            Thread.sleep(1000);
        }
    }
}
