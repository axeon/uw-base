package uw.task.conf;

import uw.common.util.ThreadFactoryBuilder;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;
import uw.kv.conf.UwKvAutoConfiguration;
import uw.kv.GlobalLeaderElector;
import uw.log.es.LogClient;
import uw.task.TaskFactory;
import uw.task.TaskListenerManager;
import uw.task.api.TaskApiClient;
import uw.task.container.TaskCronerContainer;
import uw.task.container.TaskDelayerContainer;
import uw.task.container.TaskRunnerContainer;
import uw.task.converter.TaskMessageConverter;
import uw.task.entity.TaskCronerLog;
import uw.task.entity.TaskDelayerLog;
import uw.task.entity.TaskRunnerLog;
import uw.task.util.TaskLocalRateLimiter;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * uw-task 自动配置入口。
 *
 * <p>通过 spring-boot 自动装配机制（见 META-INF/spring/...AutoConfiguration.imports）启用任务框架：
 * 构建 task 专用的 RabbitMQ/Redis 连接工厂与各类工具（限速器、Leader 锁、发号器），
 * 装配 {@link TaskFactory} 及配套容器，并在容器就绪后启动周期性任务
 * （配置刷新、主机上报、Leader 选举）。所有组件在 {@link #taskFactory} Bean 内手动创建与串联。</p>
 *
 * @author axeon
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({TaskProperties.class})
@AutoConfigureAfter({DataRedisAutoConfiguration.class, RabbitAutoConfiguration.class, UwKvAutoConfiguration.class})
public class TaskAutoConfiguration {
    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(TaskAutoConfiguration.class);

    /**
     * 任务meta信息管理器。
     */
    TaskMetaInfoManager taskMetaInfoManager;

    /**
     * 是否已初始化配置，保证只初始化一次；
     */
    private final AtomicBoolean initFlag = new AtomicBoolean(false);

    /**
     * 服务端任务配置。
     */
    private TaskServiceRegistrar taskServiceRegistrar;

    /**
     * 定时任务容器。
     */
    private TaskCronerContainer taskCronerContainer;

    /**
     * 队列任务容器。
     */
    private TaskRunnerContainer taskRunnerContainer;

    /**
     * 延迟任务容器。
     */
    private TaskDelayerContainer taskDelayerContainer;

    /**
     * Leader选举锁 key（GlobalLeaderElector 注册制，static 调用）。
     */
    private String leaderLockKey;
    /**
     * 内部自持任务。
     */
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * 任务专用 RabbitMQ 连接工厂（应用就绪后按 runner 数动态调 channelCacheSize）。
     */
    private CachingConnectionFactory taskRabbitConnectionFactory;

    /**
     * channelCacheSize 系数：每个 runner（队列）对应的 channel cache 基数（覆盖 publisher 并发 + listener channel + 余量）。
     * channelCacheSize = max(runnerCount × 系数, {@link #DEFAULT_MIN_CHANNEL_CACHE})。
     */
    private static final int CHANNEL_CACHE_COEFFICIENT = 1;

    /**
     * channelCacheSize 下限：即使 runner 少也保证一定 cache，避免 CachingConnectionFactory 默认 1 的性能陷阱
     * （channel 频繁创建/close → spring-rabbit-deferred-pool 线程暴涨 + 低吞吐）。
     */
    private static final int DEFAULT_MIN_CHANNEL_CACHE = 10;

    /**
     * 注册日志对象
     *
     * @param logClient
     * @return
     */
    @Bean
    public CommandLineRunner configTaskLogClient(final LogClient logClient) {
        return args -> {
            // runner日志
            logClient.regLogObjectWithIndexName(TaskRunnerLog.class, "uw.task.runner.log");
            // croner日志
            logClient.regLogObjectWithIndexName(TaskCronerLog.class, "uw.task.croner.log");
            // delayer日志
            logClient.regLogObjectWithIndexName(TaskDelayerLog.class, "uw.task.delayer.log");
        };
    }

    /**
     * 应用就绪后初始化任务框架（仅执行一次）。
     * <p>扫描任务实例、启动内部调度线程池：注册主机时额外调度主机上报（180s）与 Leader 选举（60s），
     * 无论是否注册都调度配置刷新（60s）。</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void handleContextRefresh() {
        if (initFlag.compareAndSet(false, true)) {
            taskMetaInfoManager.init();
            // 动态调 channelCacheSize：按 runner（队列）数 × 系数（每队列 publisher+listener channel+余量），下限 10。
            // 避免 CachingConnectionFactory 默认 1 的性能陷阱：sendToQueue+listener 抢 channel 频繁创建/close，
            // 每次关 channel 丢进 spring-rabbit-deferred-pool（newCachedThreadPool 无上限）致线程暴涨 + 吞吐腰斩。
            // 必须在 listener 启动（下方 updateConfig→taskRunnerContainer.registerQueueListener）前设，否则 connection 已按默认 1 创建。
            if (taskRabbitConnectionFactory != null) {
                int runnerCount = taskMetaInfoManager.getRunnerInstanceMap().size();
                int channelCacheSize = Math.max(runnerCount * CHANNEL_CACHE_COEFFICIENT, DEFAULT_MIN_CHANNEL_CACHE);
                taskRabbitConnectionFactory.setChannelCacheSize(channelCacheSize);
                log.info("TaskAutoConfiguration: channelCacheSize 动态设为 {} (runnerCount={}, coefficient={}, min={})",
                        channelCacheSize, runnerCount, CHANNEL_CACHE_COEFFICIENT, DEFAULT_MIN_CHANNEL_CACHE);
            }
            // 注：TaskDelayer/Croner/Runner 启动统一由下方 updateConfig（0s 首次）驱动：
            // center 可用则用 center 配置启动；拉取失败+首次则本地降级启动（initConfig 默认）。
            // 三类对称：仅 enable-registry=true 的执行主机才启动任务（Delayer 自此与 Runner/Croner 一致，不再无视 enableRegistry）。
            //task自持任务
            if (taskServiceRegistrar.isEnableRegistry()) {
                scheduledExecutorService = Executors.newScheduledThreadPool(3, ThreadFactoryBuilder.of("uw-task-service-%d").daemon().build());
                //主机报告任务
                scheduledExecutorService.scheduleAtFixedRate(() -> {
                    try {
                        taskServiceRegistrar.reportHostInfo();
                    } catch (Throwable e) {
                        log.error("reportHostInfo scheduled task error!", e);
                    }
                }, 20, 180, TimeUnit.SECONDS);
                //选举任务
                scheduledExecutorService.scheduleAtFixedRate(() -> {
                    try {
                        GlobalLeaderElector.checkLeader(leaderLockKey);
                    } catch (Throwable e) {
                        log.error("checkLeader scheduled task error!", e);
                    }
                }, 0, 60, TimeUnit.SECONDS);
            } else {
                scheduledExecutorService = Executors.newScheduledThreadPool(1, ThreadFactoryBuilder.of("uw-task-service-%d").daemon().build());
            }
            //配置更新任务
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    taskServiceRegistrar.updateConfig();
                } catch (Throwable e) {
                    log.error("updateConfig scheduled task error!", e);
                }
            }, 0, 60, TimeUnit.SECONDS);
        }
    }

    /**
     * 容器销毁时优雅关闭内部调度线程池与各任务容器。
     * <p>scheduledExecutorService 按 shutdown → awaitTermination(5s) → shutdownNow 三段式关闭；
     * taskServiceRegistrar / taskCronerContainer / taskDelayerContainer 在 taskFactory Bean 内部创建，
     * 若该 Bean 因依赖缺失未实例化则对应字段为 null，故逐个判空后再关闭，避免 NPE 掩盖真正的关闭异常。</p>
     */
    @PreDestroy
    public void destroy() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
            try {
                if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduledExecutorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        // 以下组件仅在 taskFactory Bean 内部初始化；若该 Bean 因依赖缺失等原因未创建，
        // 这些字段为 null，直接调用会 NPE 并掩盖真正的关闭异常，故先判空。
        if (taskServiceRegistrar != null) {
            // 持 configLock 停所有任务（与在途 updateConfig/reportHostInfo 互斥，避免 destroyAll 与 registerQueueListener/configureTask 交错）
            taskServiceRegistrar.shutdownAllTasks();
        } else {
            // taskFactory Bean 部分失败（container 已创建但 registrar 未创建）时，独立关已创建的 container
            if (taskRunnerContainer != null) {
                taskRunnerContainer.destroyAll();
            }
            if (taskCronerContainer != null) {
                taskCronerContainer.destroyAll();
            }
            if (taskDelayerContainer != null) {
                taskDelayerContainer.destroyAll();
            }
        }
        // 关闭任务专用 RabbitMQ 连接工厂：taskFactory Bean 内手工 new 的 DisposableBean，
        // 非 Spring 管理，Spring 不会代为销毁，优雅关闭场景下不关会残留连接。
        // Redis 连接由 uw-kv 的 kvRedisConnectionFactory Bean 提供，生命周期归 Spring 管理，无需在此关闭。
        if (taskRabbitConnectionFactory != null) {
            taskRabbitConnectionFactory.destroy();
        }
    }

    /**
     * 获取任务自定义的rabbitConnectionFactory
     *
     * @param rabbitProperties RabbitMQ配置
     * @return ConnectionFactory
     */
    private ConnectionFactory getTaskRabbitConnectionFactory(RabbitProperties rabbitProperties) {
        PropertyMapper map = PropertyMapper.get();
        RabbitConnectionFactoryBean factoryBean = new RabbitConnectionFactoryBean();
        map.from(rabbitProperties::determineHost).to(factoryBean::setHost);
        map.from(rabbitProperties::determinePort).to(factoryBean::setPort);
        map.from(rabbitProperties::determineUsername).to(factoryBean::setUsername);
        map.from(rabbitProperties::determinePassword).to(factoryBean::setPassword);
        map.from(rabbitProperties::determineVirtualHost).to(factoryBean::setVirtualHost);
        map.from(rabbitProperties::getRequestedHeartbeat).asInt(Duration::getSeconds).to(factoryBean::setRequestedHeartbeat);
        RabbitProperties.Ssl ssl = rabbitProperties.getSsl();
        if (ssl.getEnabled() != null && ssl.getEnabled()) {
            factoryBean.setUseSSL(true);
            map.from(ssl::getAlgorithm).to(factoryBean::setSslAlgorithm);
            map.from(ssl::getKeyStoreType).to(factoryBean::setKeyStoreType);
            map.from(ssl::getKeyStore).to(factoryBean::setKeyStore);
            map.from(ssl::getKeyStorePassword).to(factoryBean::setKeyStorePassphrase);
            map.from(ssl::getTrustStoreType).to(factoryBean::setTrustStoreType);
            map.from(ssl::getTrustStore).to(factoryBean::setTrustStore);
            map.from(ssl::getTrustStorePassword).to(factoryBean::setTrustStorePassphrase);
        }
        map.from(rabbitProperties::getConnectionTimeout).asInt(Duration::toMillis).to(factoryBean::setConnectionTimeout);
        try {
            factoryBean.afterPropertiesSet();
        } catch (Exception e) {
            log.error("配置RabbitConnectionFactoryBean出错", e);
        }

        CachingConnectionFactory connFactory = null;
        try {
            com.rabbitmq.client.ConnectionFactory connectionFactory = factoryBean.getObject();
            if (connectionFactory == null) {
                throw new IllegalStateException("RabbitConnectionFactoryBean.getObject() returned null");
            }
            connFactory = new CachingConnectionFactory(connectionFactory);
            map.from(rabbitProperties::determineAddresses).to(connFactory::setAddresses);
        } catch (Exception e) {
            log.error("获取ConnectionFactory出错", e);
            throw new RuntimeException("获取ConnectionFactory出错", e);
        }
        map.from(rabbitProperties::getPublisherConfirmType).to(connFactory::setPublisherConfirmType);
        map.from(rabbitProperties::isPublisherReturns).to(connFactory::setPublisherReturns);
        RabbitProperties.Cache.Channel channel = rabbitProperties.getCache().getChannel();
        map.from(channel::getSize).to(connFactory::setChannelCacheSize);
        map.from(channel::getCheckoutTimeout).as(Duration::toMillis).to(connFactory::setChannelCheckoutTimeout);
        RabbitProperties.Cache.Connection connection = rabbitProperties.getCache().getConnection();
        map.from(connection::getMode).to(connFactory::setCacheMode);
        map.from(connection::getSize).to(connFactory::setConnectionCacheSize);

        try {
            connFactory.afterPropertiesSet();
        } catch (Exception e) {
            log.error("配置CachingConnectionFactory出错", e);
        }
        return connFactory;
    }

    /**
     * 转换器用TaskMessageConverter转换实体类对象。
     *
     * @param connectionFactory
     * @return RabbitTemplate
     */
    private RabbitTemplate getTaskRabbitTemplate(final ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new TaskMessageConverter());
        template.setReplyTimeout(180000);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 任务监听管理器。
     *
     * @return
     */
    @Bean
    TaskListenerManager taskListenerManager() {
        return new TaskListenerManager();
    }

    /**
     * 声明 TaskFactory bean
     *
     * @param context              Spring 应用上下文（扫描任务 bean）
     * @param taskProperties       任务全局配置
     * @param taskListenerManager  任务监听管理器
     * @param authRestClient       鉴权 RestClient（调 task-center）
     * @param kvRedisConnectionFactory uw-kv 框架协调层 Redis 连接工厂（限速/发号/Leader/延迟队列共用）
     * @param logClient            ES 日志客户端
     * @return TaskFactory bean
     */
    @Bean
    TaskFactory taskFactory(final ApplicationContext context, final TaskProperties taskProperties, final TaskListenerManager taskListenerManager,
                            @Qualifier("authRestClient") final RestClient authRestClient, @Qualifier("kvRedisConnectionFactory") final RedisConnectionFactory kvRedisConnectionFactory, final LogClient logClient) {
        // task自定义的rabbit连接工厂
        taskRabbitConnectionFactory = (CachingConnectionFactory) getTaskRabbitConnectionFactory(taskProperties.getRabbitmq());
        // 本地限速器。
        TaskLocalRateLimiter taskLocalRateLimiter = new TaskLocalRateLimiter();
        // 全局限速/发号走 uw-kv static 家族（GlobalFixedWindowRateLimiter/GlobalSequence），域隔离由 name 前缀编码。
        // 任务交互API
        TaskApiClient taskApiClient = new TaskApiClient(taskProperties, authRestClient, logClient);
        // rabbit模板
        RabbitTemplate rabbitTemplate = getTaskRabbitTemplate(taskRabbitConnectionFactory);
        // rabbit管理器
        RabbitAdmin rabbitAdmin = new RabbitAdmin(taskRabbitConnectionFactory);
        // Leader选举注册（uw-kv GlobalLeaderElector 注册制：稳定实例标识跨重启续期；原 uw-task-locker: 锁 key 90s TTL 自动过期，无迁移）
        leaderLockKey = "uw-task-locker:" + taskProperties.getTaskProject();
        GlobalLeaderElector.config(leaderLockKey,
                taskProperties.getAppName() + ":" + taskProperties.getAppVersion() + "@" + taskProperties.getAppHost() + ":" + taskProperties.getAppPort());
        // 任务信息管理器。
        taskMetaInfoManager = new TaskMetaInfoManager(context, taskProperties);
        // 队列任务容器。
        taskRunnerContainer = new TaskRunnerContainer(taskProperties, taskApiClient, taskMetaInfoManager, taskLocalRateLimiter, taskListenerManager, rabbitAdmin, taskRabbitConnectionFactory);
        // 延迟任务容器：多实例竞争 zset poll，本地模式自启动（扫描 TaskDelayer + initConfig）
        taskDelayerContainer = new TaskDelayerContainer(taskProperties, taskApiClient, taskMetaInfoManager, taskLocalRateLimiter, kvRedisConnectionFactory, taskListenerManager);
        // 定时任务容器。
        taskCronerContainer = new TaskCronerContainer(taskProperties, taskApiClient, taskMetaInfoManager, taskListenerManager);
        // 初始化TaskServerConfig
        taskServiceRegistrar = new TaskServiceRegistrar(taskProperties, taskApiClient, taskRunnerContainer, taskCronerContainer, taskDelayerContainer, taskMetaInfoManager);
        // 返回TaskScheduler
        TaskFactory taskFactory = new TaskFactory(taskProperties, rabbitTemplate, taskRunnerContainer, taskMetaInfoManager, taskDelayerContainer);
        return taskFactory;
    }
}
