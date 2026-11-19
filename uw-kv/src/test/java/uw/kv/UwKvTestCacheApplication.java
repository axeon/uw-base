package uw.kv;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import uw.kv.constant.CacheNotifyType;
import uw.kv.vo.FusionCacheNotifyMessage;

@SpringBootApplication
public class UwKvTestCacheApplication {

    public static void main(String[] args) throws InterruptedException {
        new SpringApplicationBuilder(UwKvTestCacheApplication.class).beanNameGenerator((beanDefinition, beanDefinitionRegistry) -> {
            String beanClassName = beanDefinition.getBeanClassName();
            if (beanClassName.startsWith("uw.kv")) {
                return beanClassName;
            }
            if (beanClassName.endsWith("LoadBalancerAutoConfiguration")) {
                return beanClassName;
            }
            return new AnnotationBeanNameGenerator().generateBeanName(beanDefinition, beanDefinitionRegistry);
        }).run(args);
        FusionCache.Config fusionConfig = FusionCache.Config.builder().cacheName("global").localCacheMaxNum(1000).cacheExpireMillis(1000000).build();
        FusionCache.config(fusionConfig, new CacheDataLoader<Integer, FusionCacheNotifyMessage>() {
            @Override
            public FusionCacheNotifyMessage load(Integer key) {
                return new FusionCacheNotifyMessage(FusionCache.INSTANCE_ID, "global", CacheNotifyType.INVALIDATE.getValue(), key);
            }
        });
        FusionCacheNotifyMessage msg = FusionCache.get("global", 123);
        System.out.println(msg);
        System.out.println(FusionCache.containsKey("global", 123));

    }
}
