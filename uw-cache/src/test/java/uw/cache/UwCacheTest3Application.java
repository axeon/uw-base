package uw.cache;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

@SpringBootApplication
public class UwCacheTest3Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder( UwCacheTest3Application.class ).beanNameGenerator( (beanDefinition, beanDefinitionRegistry) -> {
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
        System.out.println(FusionCache.containsKey("fusion",123));
        FusionCache.invalidate( "fusion",123 );
        System.out.println(FusionCache.containsKey("fusion",123));
    }
}
