package uw.cache;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

import java.util.Arrays;
import java.util.HashSet;

@SpringBootApplication
public class UwCacheTestListApplication {

    public static void main(String[] args) throws InterruptedException {
        new SpringApplicationBuilder( UwCacheTestListApplication.class ).beanNameGenerator( (beanDefinition, beanDefinitionRegistry) -> {
            String beanClassName = beanDefinition.getBeanClassName();
            if (beanClassName.contains( "uw.cache" )) {
                return beanClassName;
            }
            if (beanClassName.endsWith( "LoadBalancerAutoConfiguration" )) {
                return beanClassName;
            }
            return new AnnotationBeanNameGenerator().generateBeanName( beanDefinition, beanDefinitionRegistry );
        } ).run( args );

        String[] data = new String[]{"abc","efg","hij","klm","pqr","tuv","wxyz"};
        FusionCache.config( new FusionCache.Config("test",100,0) );
        FusionCache.put( "test","test",data );
        String[] getData = FusionCache.get( "test","test" );
        System.out.println( Arrays.toString(getData));
        HashSet getSet = FusionCache.get( "test","test" );
        System.out.println( getSet);
        Thread.sleep( System.currentTimeMillis() );
    }
}
