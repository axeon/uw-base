package uw.cache;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import uw.cache.constant.CacheNotifyType;
import uw.cache.vo.FusionCacheNotifyMessage;

@SpringBootApplication
public class UwCacheTestCacheApplication {

    public static void main(String[] args) throws InterruptedException {
        new SpringApplicationBuilder( UwCacheTestCacheApplication.class ).beanNameGenerator( (beanDefinition, beanDefinitionRegistry) -> {
            String beanClassName = beanDefinition.getBeanClassName();
            if (beanClassName.startsWith( "uw.cache" )) {
                return beanClassName;
            }
            if (beanClassName.endsWith( "LoadBalancerAutoConfiguration" )) {
                return beanClassName;
            }
            return new AnnotationBeanNameGenerator().generateBeanName( beanDefinition, beanDefinitionRegistry );
        } ).run( args );
        FusionCache.Config fusionConfig = FusionCache.Config.builder().cacheName( "global" ).localCacheMaxNum( 1000 ).globalCacheExpireMillis( 1000000 ).build();
        FusionCache.config( fusionConfig, new CacheDataLoader<Integer, FusionCacheNotifyMessage>() {
            @Override
            public FusionCacheNotifyMessage load(Integer key) {
                return new FusionCacheNotifyMessage( "global", CacheNotifyType.INVALIDATE.getValue(), key );
            }
        } );
        FusionCacheNotifyMessage msg = FusionCache.get( "global", 123 );
        System.out.println( msg );
        System.out.println( FusionCache.containsKey( "global", 123 ) );

    }
}
