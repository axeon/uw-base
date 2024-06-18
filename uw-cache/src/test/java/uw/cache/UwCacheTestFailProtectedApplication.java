package uw.cache;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import uw.cache.vo.FusionCacheNotifyMessage;

@SpringBootApplication
public class UwCacheTestFailProtectedApplication {

    public static void main(String[] args) throws InterruptedException {
        new SpringApplicationBuilder( UwCacheTestFailProtectedApplication.class ).beanNameGenerator( (beanDefinition, beanDefinitionRegistry) -> {
            String beanClassName = beanDefinition.getBeanClassName();
            if (beanClassName.contains( "uw.cache" )) {
                return beanClassName;
            }
            if (beanClassName.endsWith( "LoadBalancerAutoConfiguration" )) {
                return beanClassName;
            }
            return new AnnotationBeanNameGenerator().generateBeanName( beanDefinition, beanDefinitionRegistry );
        } ).run( args );
        FusionCache.Config fusionConfig =
                FusionCache.Config.builder().cacheName( "test" ).localCacheMaxNum( 1000 ).globalCacheExpireMillis( -1 ).failProtectMillis( 5000L ).build();
        FusionCache.config( fusionConfig, new CacheDataLoader<Integer, FusionCacheNotifyMessage>() {
            @Override
            public FusionCacheNotifyMessage load(Integer key) {
                System.out.println( "loading..." + key );
                return null;
            }
        } );
        FusionCacheNotifyMessage msg = FusionCache.get( "test", 123 );
        System.out.println( msg );
        while (true) {
            msg = FusionCache.get( "test", 123 );
            System.out.println( msg );
            Thread.sleep( 1000 );
        }
//        msg = FusionCache.get( "test", 123 );
//        msg = FusionCache.get( "test", 123 );

    }
}
