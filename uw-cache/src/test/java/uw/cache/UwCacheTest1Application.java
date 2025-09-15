package uw.cache;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import uw.common.util.SystemClock;

@SpringBootApplication
public class UwCacheTest1Application {

    public static void main(String[] args) throws InterruptedException {
        new SpringApplicationBuilder( UwCacheTest1Application.class ).beanNameGenerator( (beanDefinition, beanDefinitionRegistry) -> {
            String beanClassName = beanDefinition.getBeanClassName();
            if (beanClassName.startsWith( "uw.cache" )) {
                return beanClassName;
            }
            if (beanClassName.endsWith( "LoadBalancerAutoConfiguration" )) {
                return beanClassName;
            }
            return new AnnotationBeanNameGenerator().generateBeanName( beanDefinition, beanDefinitionRegistry );
        } ).run( args );
        FusionCache.Config fusionConfig =
                FusionCache.Config.builder().cacheName( "fusion" ).localCacheMaxNum( 10000 ).globalCacheExpireMillis( 86400_000L ).nullProtectMillis( 86400_000L ).build();
        FusionCache.config( fusionConfig, new CacheDataLoader<Long, String>() {
            @Override
            public String load(Long key) {
//                throw new RuntimeException();
                return "hello " + key;
            }
        } );
//        for (int i = 0; i < 1000; i++) {
        System.out.println( (String) FusionCache.get( "fusion", SystemClock.now() ) );
        System.out.println( (String) FusionCache.get( "fusion", SystemClock.now() ) );

//        }
        Thread.sleep( SystemClock.now() );
    }
}
