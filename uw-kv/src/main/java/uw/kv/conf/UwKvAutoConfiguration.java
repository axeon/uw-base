package uw.kv.conf;

import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import uw.common.util.RedisConnectionFactoryBuilder;
import uw.kv.FusionCache;
import uw.kv.GlobalCache;
import uw.kv.GlobalCounter;
import uw.kv.GlobalFixedWindowRateLimiter;
import uw.kv.GlobalHashSet;
import uw.kv.GlobalLeaderElector;
import uw.kv.GlobalLocker;
import uw.kv.GlobalSequence;
import uw.kv.GlobalSortedSet;
import io.lettuce.core.resource.ClientResources;
import uw.kv.listener.FusionCacheNotifyListener;

/**
 * uw-kv 自动配置入口。
 *
 * <p><b>对外 bean 面（用户定案 261120 收敛）：仅 {@link #kvRedisConnectionFactory} 一个</b>——
 * Global* 家族为静态门面（调用方零注入直调静态方法，构造仅向 static 字段注入 template/工厂），
 * 原 8 个 init 锚点 bean 与两个 RedisTemplate bean、Pub/Sub 监听容器全部内化为工厂 bean 方法内的手工构建，
 * 宿主应用零冲突面。监听容器生命周期由本类 {@link SmartLifecycle} 管理。</p>
 *
 * <p>结构（三层）：</p>
 * <ol>
 *   <li>注册唯一的 {@code kvRedisConnectionFactory}：uw 框架协调层（缓存/计数/锁/限流/选举/发号）
 *       私有连接，与业务 Redis 隔离。</li>
 *   <li>构建两个专用 RedisTemplate：{@code dataCacheRedisTemplate}（byte[] 值，用于 Cache/HashSet/SortedSet）
 *       与 {@code longCacheRedisTemplate}（Long 值，用于 Counter/Locker），共用同一连接工厂。</li>
 *   <li>以模板/工厂构造全部 Global* 静态门面 + FusionCache 失效通知订阅（虚拟线程回调）。</li>
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
public class UwKvAutoConfiguration implements SmartLifecycle {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(UwKvAutoConfiguration.class);

    /**
     * 私有连接工厂引用（bean 方法构建后保存，SmartLifecycle start 时接订阅容器）。
     */
    private RedisConnectionFactory kvFactory;

    /**
     * FusionCache 失效通知订阅容器（start/stop 由 SmartLifecycle 管理）。
     */
    private RedisMessageListenerContainer notifyListenerContainer;

    /**
     * uw 框架协调层唯一的 Redis 连接工厂（standalone/sentinel/cluster 按 uw.kv.* 配置自动判断）。
     * <p>
     * 配置经 {@link UwKvProperties}（{@code uw.kv.redis.*}，与其他独立域同构）由 Spring 绑定后直接注入。
     * <p>
     * <b>defaultCandidate=false</b>：通用类型（RedisConnectionFactory），不作宿主 by-type 注入的默认候选
     * ——宿主注入 Redis 工厂（含 Boot 自动配置的 reactiveRedisTemplate 等）只见自己的业务工厂，零歧义冲突面。
     * <b>无条件注册</b>（不加 {@code @ConditionalOnMissingBean}）：若按"缺失才注册"判断，宿主配置了
     * {@code spring.data.redis}（业务 Redis）时本工厂会退化为借用业务连接，破坏"框架协调层与业务
     * Redis 隔离"的设计。Bean 名 {@code kvRedisConnectionFactory} 与 Spring Boot 自动配置的
     * {@code lettuceConnectionFactory}（业务连接）天然区分、互不干扰；框架组件一律注入本工厂。
     * <p>
     * 本方法同时是全部 Global* 静态门面的唯一 init 点（模板构建 + 构造注入 + 通知订阅），
     * bean 创建完成即全家族可用。
     *
     * @param kvProperties     uw.kv 配置
     * @param clientResources  Lettuce 共享 ClientResources（可选）
     * @return Redis 连接工厂
     */
    @Bean(defaultCandidate = false)
    public RedisConnectionFactory kvRedisConnectionFactory(final UwKvProperties kvProperties, final ObjectProvider<ClientResources> clientResources) {
        RedisConnectionFactory factory = RedisConnectionFactoryBuilder.build(kvProperties.getRedis(), clientResources.getIfAvailable());
        kvFactory = factory;
        // 两个专用模板（byte[] 值 / Long 值），共用本工厂（行为对齐原独立 bean：默认序列化关闭，
        // byte[] 模板值序列化器不设——由调用方 Kryo 自序列化；Long 模板用 GenericToStringSerializer）
        RedisTemplate<String, byte[]> dataCacheRedisTemplate = buildTemplate(factory, null);
        RedisTemplate<String, Long> longCacheRedisTemplate = buildTemplate(factory, new GenericToStringSerializer<>(Long.class));
        // Global* 静态门面构造即完成 static 注入（调用方零注入直调静态方法）
        new GlobalCache(dataCacheRedisTemplate);
        new GlobalLocker(longCacheRedisTemplate);
        new GlobalCounter(longCacheRedisTemplate);
        new GlobalHashSet(dataCacheRedisTemplate);
        new GlobalSortedSet(dataCacheRedisTemplate);
        new GlobalFixedWindowRateLimiter(longCacheRedisTemplate);
        new GlobalLeaderElector(factory);
        new GlobalSequence(factory);
        log.info("uw-kv Global* 静态门面已初始化（dataCache/longCache 模板 + LeaderElector/Sequence）");
        return factory;
    }

    /**
     * 构建专用模板（String key + 指定值序列化器）。
     *
     * @param factory  连接工厂
     * @param valueSerializer 值序列化器
     * @param <V>      值类型
     * @return 已初始化模板
     */
    private static <V> RedisTemplate<String, V> buildTemplate(RedisConnectionFactory factory, RedisSerializer<V> valueSerializer) {
        RedisTemplate<String, V> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        if (valueSerializer != null) {
            template.setValueSerializer(valueSerializer);
        }
        template.setConnectionFactory(factory);
        template.setEnableDefaultSerializer(false);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 启动 FusionCache 失效通知订阅（虚拟线程回调，避免高频通知阻塞 Redis 订阅连接）。
     */
    @Override
    public void start() {
        if (notifyListenerContainer == null) {
            notifyListenerContainer = new RedisMessageListenerContainer();
            notifyListenerContainer.setConnectionFactory(kvFactory);
            AsyncTaskExecutor taskExecutor = new TaskExecutorAdapter(
                    Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("uw-kv-notify-", 0).factory()));
            notifyListenerContainer.setTaskExecutor(taskExecutor);
            notifyListenerContainer.addMessageListener(new FusionCacheNotifyListener(), new ChannelTopic(FusionCache.FUSION_CACHE_NOTIFY_CHANNEL));
            notifyListenerContainer.afterPropertiesSet();
        }
        notifyListenerContainer.start();
        log.info("uw-kv FusionCache 失效通知订阅已启动");
    }

    /**
     * 停止通知订阅容器。
     */
    @Override
    public void stop() {
        if (notifyListenerContainer != null && notifyListenerContainer.isRunning()) {
            notifyListenerContainer.stop();
        }
    }

    /**
     * @return 生命周期是否运行中
     */
    @Override
    public boolean isRunning() {
        return notifyListenerContainer != null && notifyListenerContainer.isRunning();
    }
}
