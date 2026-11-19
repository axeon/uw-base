package uw.kv.conf;

import io.lettuce.core.resource.ClientResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import uw.common.util.RedisConnectionFactoryBuilder;
import uw.kv.*;
import uw.kv.listener.FusionCacheNotifyListener;

import java.util.concurrent.Executors;

/**
 * uw-kv 启动自动配置。
 * <p>
 * 仅在 classpath 存在 RedisTemplate 时生效（{@link ConditionalOnClass}），
 * 在 Spring Boot RedisAutoConfiguration 之后加载。负责：
 * <ol>
 *   <li>注册唯一的 {@code kvRedisConnectionFactory}：uw 框架协调层（缓存/计数/锁/限流/选举/发号）
 *       与业务 Redis（{@code spring.data.redis}）隔离的专用连接，配置经 Binder 平铺绑定
 *       {@code uw.kv.*} 全部连接项（standalone/sentinel/cluster、pool、lettuce、ssl 等，无属性 Bean）。</li>
 *   <li>注册两个专用 RedisTemplate：{@code dataCacheRedisTemplate}（byte[] 值，用于 Cache/HashSet/SortedSet）
 *       与 {@code longCacheRedisTemplate}（Long 值，用于 Counter/Locker），共用同一连接工厂。</li>
 *   <li>初始化各 Global* 组件（通过构造函数注入 static RedisTemplate）。</li>
 *   <li>注册 Redis Pub/Sub 监听容器，订阅 FusionCache 失效通知通道。</li>
 * </ol>
 * <p>
 * 限速器（{@code GlobalFixedWindowRateLimiter}）、{@code GlobalLeaderElector}、{@code GlobalSequence}
 * 与 Global* 家族同款 static API：经构造注入 static template/工厂后，
 * 调用方零注入直调静态方法；域隔离（key 前缀）由调用方编码进 name/lockKey。
 */
@Configuration
@ConditionalOnClass({RedisTemplate.class, RedisConnectionFactory.class})
@AutoConfigureAfter({DataRedisAutoConfiguration.class})
@EnableConfigurationProperties(UwKvProperties.class)
public class UwKvAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(UwKvAutoConfiguration.class);

    /**
     * 初始化 GlobalCache，注入 dataCacheRedisTemplate。
     *
     * @param dataCacheRedisTemplate byte[] 值 RedisTemplate
     * @return GlobalCache 实例
     */
    @Bean
    public GlobalCache globalCache(RedisTemplate<String, byte[]> dataCacheRedisTemplate) {
        return new GlobalCache(dataCacheRedisTemplate);
    }

    /**
     * 初始化 GlobalLocker，注入 longCacheRedisTemplate。
     *
     * @param longCacheRedisTemplate Long 值 RedisTemplate
     * @return GlobalLocker 实例
     */
    @Bean
    public GlobalLocker globalLocker(RedisTemplate<String, Long> longCacheRedisTemplate) {
        return new GlobalLocker(longCacheRedisTemplate);
    }


    /**
     * 初始化 GlobalCounter，注入 longCacheRedisTemplate。
     *
     * @param longCacheRedisTemplate Long 值 RedisTemplate
     * @return GlobalCounter 实例
     */
    @Bean
    public GlobalCounter globalCounter(RedisTemplate<String, Long> longCacheRedisTemplate) {
        return new GlobalCounter(longCacheRedisTemplate);
    }

    /**
     * 初始化 GlobalHashSet，注入 dataCacheRedisTemplate。
     *
     * @param dataCacheRedisTemplate byte[] 值 RedisTemplate
     * @return GlobalHashSet 实例
     */
    @Bean
    public GlobalHashSet globalHashSet(RedisTemplate<String, byte[]> dataCacheRedisTemplate) {
        return new GlobalHashSet(dataCacheRedisTemplate);
    }

    /**
     * 初始化 GlobalSortedSet，注入 dataCacheRedisTemplate。
     *
     * @param dataCacheRedisTemplate byte[] 值 RedisTemplate
     * @return GlobalSortedSet 实例
     */
    @Bean
    public GlobalSortedSet globalSortedSet(RedisTemplate<String, byte[]> dataCacheRedisTemplate) {
        return new GlobalSortedSet(dataCacheRedisTemplate);
    }

    /**
     * 初始化 GlobalFixedWindowRateLimiter（全局限速器），注入 longCacheRedisTemplate。
     *
     * @param longCacheRedisTemplate Long 值 RedisTemplate
     * @return GlobalFixedWindowRateLimiter 实例
     */
    @Bean
    public GlobalFixedWindowRateLimiter globalFixedWindowRateLimiter(RedisTemplate<String, Long> longCacheRedisTemplate) {
        return new GlobalFixedWindowRateLimiter(longCacheRedisTemplate);
    }

    /**
     * 初始化 GlobalLeaderElector（Leader 选举器），注入 kvRedisConnectionFactory（内部自建 StringRedisTemplate）。
     *
     * @param kvRedisConnectionFactory uw.kv 连接工厂
     * @return GlobalLeaderElector 实例
     */
    @Bean
    public GlobalLeaderElector globalLeaderElector(RedisConnectionFactory kvRedisConnectionFactory) {
        return new GlobalLeaderElector(kvRedisConnectionFactory);
    }

    /**
     * 初始化 GlobalSequence（号段发号器），注入 kvRedisConnectionFactory。
     *
     * @param kvRedisConnectionFactory uw.kv 连接工厂
     * @return GlobalSequence 实例
     */
    @Bean
    public GlobalSequence globalSequence(RedisConnectionFactory kvRedisConnectionFactory) {
        return new GlobalSequence(kvRedisConnectionFactory);
    }

    /**
     * uw 框架协调层唯一的 Redis 连接工厂（standalone/sentinel/cluster 按 uw.kv.* 配置自动判断）。
     * <p>
     * 配置经 {@link UwKvProperties}（{@code uw.kv.redis.*}，与其他独立域同构）由 Spring 绑定后直接注入。
     * <p>
     * <b>无条件注册</b>（不加 {@code @ConditionalOnMissingBean}）：若按"缺失才注册"判断，宿主配置了
     * {@code spring.data.redis}（业务 Redis）时本工厂会退化为借用业务连接，破坏"框架协调层与业务
     * Redis 隔离"的设计。Bean 名 {@code kvRedisConnectionFactory} 与 Spring Boot 自动配置的
     * {@code lettuceConnectionFactory}（业务连接）天然区分、互不干扰；框架组件一律注入本工厂。
     *
     * @param environment     Spring 环境（uw.kv.* 配置源）
     * @param clientResources Lettuce 共享 ClientResources
     * @return Redis 连接工厂
     */
    @Bean
    public RedisConnectionFactory kvRedisConnectionFactory(final UwKvProperties kvProperties, final ClientResources clientResources) {
        return RedisConnectionFactoryBuilder.build(kvProperties.getRedis(), clientResources);
    }

    /**
     * 初始化 Redis Pub/Sub 监听容器。
     * <p>
     * 订阅通道 {@link FusionCache#FUSION_CACHE_NOTIFY_CHANNEL}，使用虚拟线程执行器处理消息回调，
     * 避免高频缓存通知时阻塞 Redis 订阅连接。
     *
     * @param kvRedisConnectionFactory uw.kv 连接工厂
     * @return RedisMessageListenerContainer 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory kvRedisConnectionFactory) {
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(kvRedisConnectionFactory);
        // 使用虚拟线程执行器处理消息回调，避免高频缓存通知时阻塞 Redis 订阅连接。
        // 虚拟线程由 JVM 在少量载体线程上调度，创建开销极低且数量可控，
        // 相比 SimpleAsyncTaskExecutor（每条消息 new 一个平台线程）不会在批量失效/刷新时引发线程数量失控和 GC 压力。
        AsyncTaskExecutor taskExecutor = new TaskExecutorAdapter(
                Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("uw-kv-notify-", 0).factory()));
        redisMessageListenerContainer.setTaskExecutor(taskExecutor);
        FusionCacheNotifyListener cacheMessageListener = new FusionCacheNotifyListener();
        redisMessageListenerContainer.addMessageListener(cacheMessageListener, new ChannelTopic(FusionCache.FUSION_CACHE_NOTIFY_CHANNEL));
        return redisMessageListenerContainer;
    }

    /**
     * dataCacheRedisTemplate：专为 Cache/HashSet/SortedSet 优化的 RedisTemplate。
     * <p>
     * key 用 StringRedisSerializer，value 不启用默认序列化（由调用方自行 Kryo 序列化为 byte[]）。
     *
     * @param kvRedisConnectionFactory uw.kv 连接工厂
     * @return byte[] 值 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, byte[]> dataCacheRedisTemplate(final RedisConnectionFactory kvRedisConnectionFactory) {
        RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(kvRedisConnectionFactory);
        redisTemplate.setEnableDefaultSerializer(false);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * longCacheRedisTemplate：专为 Counter/Locker 优化的 RedisTemplate。
     * <p>
     * key 用 StringRedisSerializer，value 用 GenericToStringSerializer（Long 与字符串互转），
     * 便于 Lua 脚本直接以字符串形式比较 stamp。
     *
     * @param kvRedisConnectionFactory uw.kv 连接工厂
     * @return Long 值 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Long> longCacheRedisTemplate(final RedisConnectionFactory kvRedisConnectionFactory) {
        RedisTemplate<String, Long> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericToStringSerializer<Long>(Long.class));
        redisTemplate.setConnectionFactory(kvRedisConnectionFactory);
        redisTemplate.setEnableDefaultSerializer(false);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
