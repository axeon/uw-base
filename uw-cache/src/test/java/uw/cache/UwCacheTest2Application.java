package uw.cache;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import uw.common.util.SystemClock;

@SpringBootApplication
public class UwCacheTest2Application {

    public static void main(String[] args) throws InterruptedException {
        new SpringApplicationBuilder( UwCacheTest2Application.class ).beanNameGenerator( (beanDefinition, beanDefinitionRegistry) -> {
            String beanClassName = beanDefinition.getBeanClassName();
            if (beanClassName.startsWith( "uw.cache" )) {
                return beanClassName;
            }
            if (beanClassName.endsWith( "LoadBalancerAutoConfiguration" )) {
                return beanClassName;
            }
            return new AnnotationBeanNameGenerator().generateBeanName( beanDefinition, beanDefinitionRegistry );
        } ).run( args );
        FusionCache.Config fusionConfig = FusionCache.Config.builder().cacheName( "fusion" ).localCacheMaxNum( 1000 ).globalCacheExpireMillis( 1000000 ).build();
        FusionCache.config( fusionConfig, new CacheDataLoader<Integer, String>() {
            @Override
            public String load(Integer key) {
                return "hello " + key;
            }
        } );
        System.out.println((String)FusionCache.get( "fusion", 123));
        Thread.sleep( SystemClock.now() );
    }
}
