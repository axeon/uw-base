package uw.cache.conf;

import io.lettuce.core.resource.ClientResources;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import uw.cache.*;
import uw.cache.listener.FusionCacheNotifyListener;

import java.util.concurrent.Executors;

/**
 * uw-cache 启动自动配置。
 * <p>
 * 仅在 classpath 存在 RedisTemplate 时生效（{@link ConditionalOnClass}），
 * 在 Spring Boot RedisAutoConfiguration 之后加载。负责：
 * <ol>
 *   <li>注册两个专用 RedisTemplate：{@code dataCacheRedisTemplate}（byte[] 值，用于 Cache/HashSet/SortedSet）
 *       与 {@code longCacheRedisTemplate}（Long 值，用于 Counter/Locker）。</li>
 *   <li>初始化各 Global* 组件（通过构造函数注入 static RedisTemplate）。</li>
 *   <li>注册 Redis Pub/Sub 监听容器，订阅 FusionCache 失效通知通道。</li>
 * </ol>
 */
@Configuration
@ConditionalOnClass({RedisTemplate.class, RedisConnectionFactory.class})
@EnableConfigurationProperties({UwCacheProperties.class})
@AutoConfigureAfter({RedisAutoConfiguration.class})
public class UwCacheAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(UwCacheAutoConfiguration.class);

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
     * 初始化 Redis Pub/Sub 监听容器。
     * <p>
     * 订阅通道 {@link FusionCache#FUSION_CACHE_NOTIFY_CHANNEL}，使用虚拟线程执行器处理消息回调，
     * 避免高频缓存通知时阻塞 Redis 订阅连接。
     *
     * @param dataCacheRedisTemplate byte[] 值 RedisTemplate（提供连接工厂）
     * @return RedisMessageListenerContainer 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisTemplate<String, byte[]> dataCacheRedisTemplate) {
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(dataCacheRedisTemplate.getConnectionFactory());
        // 使用虚拟线程执行器处理消息回调，避免高频缓存通知时阻塞 Redis 订阅连接。
        // 虚拟线程由 JVM 在少量载体线程上调度，创建开销极低且数量可控，
        // 相比 SimpleAsyncTaskExecutor（每条消息 new 一个平台线程）不会在批量失效/刷新时引发线程数量失控和 GC 压力。
        AsyncTaskExecutor taskExecutor = new TaskExecutorAdapter(
                Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("uw-cache-notify-", 0).factory()));
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
     * @param uwCacheProperties uw-cache 配置属性
     * @param clientResources  Lettuce 共享 ClientResources
     * @return byte[] 值 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, byte[]> dataCacheRedisTemplate(final UwCacheProperties uwCacheProperties, final ClientResources clientResources) {
        RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory(uwCacheProperties.getRedis(), clientResources));
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
     * @param uwCacheProperties uw-cache 配置属性
     * @param clientResources  Lettuce 共享 ClientResources
     * @return Long 值 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Long> longCacheRedisTemplate(final UwCacheProperties uwCacheProperties, final ClientResources clientResources) {
        RedisTemplate<String, Long> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericToStringSerializer<Long>(Long.class));
        redisTemplate.setConnectionFactory(redisConnectionFactory(uwCacheProperties.getRedis(), clientResources));
        redisTemplate.setEnableDefaultSerializer(false);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 构建 Lettuce 连接工厂（Standalone + 连接池）。
     * <p>
     * 根据 uw.cache.redis 配置组装：连接池、命令超时、shutdown 超时、SSL、主机/端口/库/账号密码等。
     *
     * @param redisProperties uw.cache.redis 配置
     * @param clientResources Lettuce 共享 ClientResources
     * @return LettuceConnectionFactory
     */
    private RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties, ClientResources clientResources) {
        //设置连接池。
        RedisProperties.Pool poolProperties = redisProperties.getLettuce().getPool();
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(poolProperties.getMaxActive());
        poolConfig.setMaxIdle(poolProperties.getMaxIdle());
        poolConfig.setMinIdle(poolProperties.getMinIdle());
        if (poolProperties.getMaxWait() != null) {
            poolConfig.setMaxWait(poolProperties.getMaxWait());
        }
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder().poolConfig(poolConfig);
        if (redisProperties.getTimeout() != null) {
            builder.commandTimeout(redisProperties.getTimeout());
        }
        //设置shutdownTimeout。
        RedisProperties.Lettuce lettuce = redisProperties.getLettuce();
        if (lettuce.getShutdownTimeout() != null && !lettuce.getShutdownTimeout().isZero()) {
            builder.shutdownTimeout(redisProperties.getLettuce().getShutdownTimeout());
        }
        //设置clientResources。
        builder.clientResources(clientResources);
        //设置ssl。
        if (redisProperties.getSsl().isEnabled()) {
            builder.useSsl();
        }
        //构建standaloneConfig。
        LettuceClientConfiguration clientConfig = builder.build();
        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
        standaloneConfig.setHostName(redisProperties.getHost());
        standaloneConfig.setPort(redisProperties.getPort());
        standaloneConfig.setDatabase(redisProperties.getDatabase());
        if (StringUtils.isNotBlank(redisProperties.getUsername())) {
            standaloneConfig.setUsername(redisProperties.getUsername());
        }
        if (StringUtils.isNotBlank(redisProperties.getPassword())) {
            standaloneConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }
        LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfig, clientConfig);
        factory.afterPropertiesSet();
        return factory;
    }

}
