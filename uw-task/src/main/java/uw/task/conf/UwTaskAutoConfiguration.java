package uw.task.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestClient;
import uw.common.util.RedisConnectionFactoryBuilder;
import uw.common.util.ThreadFactoryBuilder;
import uw.log.es.LogClient;
import uw.task.TaskFactory;
import uw.task.TaskListenerManager;
import uw.task.api.TaskApiClient;
import uw.task.container.TaskCronerContainer;
import uw.task.container.TaskDelayTransferContainer;
import uw.task.container.TaskRunnerContainer;
import uw.task.container.TaskSendCenter;
import uw.task.constant.TaskConstants;
import uw.task.entity.TaskCronerLog;
import uw.task.entity.TaskRunnerLog;
import uw.task.rabbit.Rabbit091Ops;
import uw.task.rabbit.Rabbit100Ops;
import uw.task.rabbit.RabbitCapabilityProbe;
import uw.task.rabbit.TaskAmqpOps;
import uw.task.rabbit.TaskAmqpProtocol;
import uw.task.util.TaskGlobalRateLimiter;
import uw.task.util.TaskLeaderElector;
import uw.task.util.TaskLocalRateLimiter;
import uw.task.util.TaskSequence;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * uw-task 自动配置入口。
 *
 * <p><b>对外 bean 面（用户定案 261120 收敛）：仅 {@link #taskFactory} 一个</b>——业务方只感知 TaskFactory
 * （runTask 系列方法/sendToQueue/getInstance/getListenerManager），内部组件（AMQP 操作面/私有 Redis/投递中心/延时搬运/容器/注册器）
 * 全部在 taskFactory bean 方法内手工构建并持有，不进 Spring 容器，宿主应用零冲突面。</p>
 *
 * <p>本类实现 {@link SmartLifecycle}：容器刷新完成后启动周期调度（元信息扫描/配置刷新/主机上报/Leader 选举），
 * 关闭时反向有序回收。内部组件依赖链无环（SendCenter→{ops,meta,Transfer}、Transfer→SendCenter 经
 * 装配期回填破互指、RunnerContainer→SendCenter），框架内部零静态单例互调。</p>
 *
 * @author axeon
 */
@Configuration
@Conditional(UwTaskAutoConfiguration.OnTaskHostsCondition.class)
@EnableScheduling
@EnableConfigurationProperties({UwTaskProperties.class})
@AutoConfigureAfter({DataRedisAutoConfiguration.class, RabbitAutoConfiguration.class})
public class UwTaskAutoConfiguration implements SmartLifecycle {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(UwTaskAutoConfiguration.class);

    // ==================== 内部组件（taskFactory 方法构建，非 bean） ====================

    /**
     * 任务元信息管理器。
     */
    private TaskMetaInfoManager metaInfoManager;

    /**
     * 任务配置注册器。
     */
    private TaskServiceRegistrar serviceRegistrar;

    /**
     * 延时任务 zset 搬运器。
     */
    private TaskDelayTransferContainer delayTransferContainer;

    /**
     * uw-task 私有 Redis 连接工厂（手工构建，stop 时关闭防泄漏）。
     */
    private RedisConnectionFactory taskRedisConnectionFactory;

    /**
     * 协议无关 AMQP 操作面（启动期探测定死）。
     */
    private TaskAmqpOps taskAmqpOps;

    // ==================== SmartLifecycle 状态 ====================

    /**
     * 内部周期任务线程池。
     */
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * Leader 选举锁 key。
     */
    private String leaderLockKey;

    /**
     * 生命周期运行标记。
     */
    private volatile boolean lifecycleRunning = false;

    // ==================== 唯一对外 bean ====================

    /**
     * TaskFactory（业务门面）：方法内手工构建全部内部组件并串联——
     * ① AMQP 操作面（能力探测：amqp 端口双握手，1.0 协议头 + 0-9-1 版本解析，纯自动判定协议与原生延迟）；
     * ② 私有 Redis 工厂 + 静态工具 init（TaskSequence/TaskLeaderElector/TaskGlobalRateLimiter）；
     * ③ 工具与容器（meta/apiClient/sendCenter/transfer/runner/croner/registrar）；
     * ④ 注册 ES 日志对象（runner/croner log）；⑤ Leader 参与者注册。
     *
     * @param context        Spring 应用上下文（扫描任务 bean）
     * @param taskProperties 任务全局配置
     * @param authRestClient 鉴权 RestClient（调 task-center）
     * @param logClient      ES 日志客户端
     * @return TaskFactory bean（@PreDestroy shutdown 自动调用）
     */
    @Bean
    TaskFactory taskFactory(final ApplicationContext context, final UwTaskProperties taskProperties,
                            @Qualifier("authRestClient") final RestClient authRestClient, final LogClient logClient) {
        // ① AMQP 操作面：能力探测定协议（运行期唯一定死；原生延迟探测结果仅喂给 100 实现——supportsNativeDelay 单一事实源）
        RabbitCapabilityProbe capability = RabbitCapabilityProbe.probe(taskProperties.getRabbitmq());
        if (capability.getProtocol() == TaskAmqpProtocol.AMQP_100) {
            String instanceId = taskProperties.getAppName() + "@" + taskProperties.getAppHost() + ":" + taskProperties.getAppPort();
            taskAmqpOps = new Rabbit100Ops(taskProperties.getRabbitmq(), instanceId, capability.isNativeDelayAvailable());
        } else {
            // 091 路径 channelCacheSize 固定值：publisher/listener 共享 cache，50 覆盖常规规模
            taskAmqpOps = new Rabbit091Ops(taskProperties.getRabbitmq(), 50);
        }
        // ② 私有 Redis（uw.task.redis.* 平铺绑定，与业务 Redis 隔离）+ 静态工具 init（本方法是根构建点，保证使用前已 init）
        taskRedisConnectionFactory = RedisConnectionFactoryBuilder.build(taskProperties.getRedis(), null);
        TaskSequence.init(taskRedisConnectionFactory);
        TaskLeaderElector.init(taskRedisConnectionFactory);
        TaskGlobalRateLimiter.init(taskRedisConnectionFactory);
        // ③ 内部组件（依赖链手工排序，无环；Transfer↔SendCenter 互指经装配期回填破环）
        TaskListenerManager taskListenerManager = new TaskListenerManager();
        TaskApiClient taskApiClient = new TaskApiClient(taskProperties, authRestClient, logClient);
        metaInfoManager = new TaskMetaInfoManager(context, taskProperties);
        delayTransferContainer = new TaskDelayTransferContainer(buildTaskBytesRedisTemplate(taskRedisConnectionFactory), taskProperties.getTaskProject(), null);
        TaskSendCenter taskSendCenter = new TaskSendCenter(taskAmqpOps, metaInfoManager, delayTransferContainer, taskListenerManager);
        delayTransferContainer.bindSendCenter(taskSendCenter);
        TaskRunnerContainer taskRunnerContainer = new TaskRunnerContainer(taskProperties, taskApiClient, metaInfoManager,
                new TaskLocalRateLimiter(), taskListenerManager, taskAmqpOps, taskSendCenter);
        TaskCronerContainer taskCronerContainer = new TaskCronerContainer(taskProperties, taskApiClient, metaInfoManager, taskListenerManager);
        serviceRegistrar = new TaskServiceRegistrar(taskProperties, taskApiClient, taskRunnerContainer, taskCronerContainer, metaInfoManager, delayTransferContainer);
        // ④ 注册 ES 日志对象（原独立 CommandLineRunner 内联：bean 创建期即可完成，无需延迟到 runner 阶段）
        logClient.regLogObjectWithIndexName(TaskRunnerLog.class, TaskConstants.KEY_RUNNER_LOG);
        logClient.regLogObjectWithIndexName(TaskCronerLog.class, TaskConstants.KEY_CRONER_LOG);
        // ⑤ Leader 参与者注册（选举检查在 SmartLifecycle start 的周期任务里执行）
        leaderLockKey = taskProperties.getTaskProject();
        TaskLeaderElector.config(leaderLockKey,
                taskProperties.getAppName() + ":" + taskProperties.getAppVersion() + "@" + taskProperties.getAppHost() + ":" + taskProperties.getAppPort());
        return new TaskFactory(taskProperties, taskAmqpOps, taskSendCenter, taskRunnerContainer, metaInfoManager, delayTransferContainer, taskListenerManager);
    }

    /**
     * 构建延时搬运/私有组件共用的 bytes 模板（String key + byte[] value，Kryo 序列化 TaskData）。
     *
     * @param connectionFactory 私有 Redis 连接工厂
     * @return bytes 模板
     */
    private static RedisTemplate<String, byte[]> buildTaskBytesRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(RedisSerializer.byteArray());
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    // ==================== SmartLifecycle：周期调度启动/停止 ====================

    /**
     * 容器刷新完成后启动：元信息扫描 → 周期任务调度（注册主机/Leader 选举/配置刷新）。
     * <p>SmartLifecycle 回调时点保证 taskFactory bean（含全部内部组件）已就绪；start 幂等。</p>
     */
    @Override
    public void start() {
        if (lifecycleRunning) {
            return;
        }
        lifecycleRunning = true;
        // ① 扫描 TaskCroner/TaskRunner bean 建索引
        metaInfoManager.init();
        // ② 周期任务：注册主机（enable-registry=true 时）额外调度主机上报与 Leader 选举
        boolean enableRegistry = serviceRegistrar.isEnableRegistry();
        scheduledExecutorService = Executors.newScheduledThreadPool(enableRegistry ? 3 : 1,
                ThreadFactoryBuilder.of("uw-task-service-%d").daemon().build());
        if (enableRegistry) {
            // 主机报告任务（180s）
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    serviceRegistrar.reportHostInfo();
                } catch (Throwable e) {
                    log.error("reportHostInfo scheduled task error!", e);
                }
            }, 20, 180, TimeUnit.SECONDS);
            // 选举任务（60s）：检查 Leader 并同步启停延时搬运 mover（Leader 单例执行）
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    TaskLeaderElector.checkLeader(leaderLockKey);
                    delayTransferContainer.setLeader(TaskLeaderElector.isLeader(leaderLockKey));
                } catch (Throwable e) {
                    log.error("checkLeader scheduled task error!", e);
                }
            }, 0, 60, TimeUnit.SECONDS);
        }
        // 配置更新任务（60s，0s 首次驱动 Croner/Runner 启动：center 可用用 center 配置，失败且首次则本地降级）
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                serviceRegistrar.updateConfig();
            } catch (Throwable e) {
                log.error("updateConfig scheduled task error!", e);
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    /**
     * 停止（先于 bean 销毁回调）：三段式关调度池 → mover 停 → registrar 在 configLock 内停两容器
     * → AMQP 操作面/私有 Redis 工厂关闭。组件判空：taskFactory bean 部分失败场景 stop 仍可安全执行。
     */
    @Override
    public void stop() {
        lifecycleRunning = false;
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
        // mover 无条件先停（在途任务留 zset，重启后接续）
        if (delayTransferContainer != null) {
            delayTransferContainer.shutdown();
        }
        // registrar 持 configLock 停两容器（与在途 updateConfig 互斥，避免与容器操作交错）
        if (serviceRegistrar != null) {
            serviceRegistrar.shutdown();
        }
        if (taskAmqpOps != null) {
            taskAmqpOps.shutdown();
        }
        if (taskRedisConnectionFactory instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignore) {
                // 关闭失败不阻断其余回收
            }
        }
    }

    /**
     * @return 生命周期是否运行中
     */
    @Override
    public boolean isRunning() {
        return lifecycleRunning;
    }

    /**
     * uw.task 属性 nacos 热更新监听器（classpath 有 spring-cloud-context 才装配；本地/纯测试行为不变）。
     *
     * @param properties 属性 bean
     * @return TaskPropertiesRefresher
     */
    @ConditionalOnClass(EnvironmentChangeEvent.class)
    @Bean
    TaskPropertiesRefresher taskPropertiesRefresher(UwTaskProperties properties) {
        return new TaskPropertiesRefresher(properties);
    }

    /**
     * uw.task 属性 nacos 热更新监听（嵌套装配，见类方法 javadoc）。
     */
    static class TaskPropertiesRefresher implements EnvironmentAware {

        /**
         * 日志器。
         */
        private static final Logger log = LoggerFactory.getLogger(TaskPropertiesRefresher.class);

        /**
         * 配置前缀。
         */
        private static final String CONFIG_PREFIX = "uw.task";

        /**
         * 属性 bean（原地重绑目标）。
         */
        private final UwTaskProperties properties;

        /**
         * Spring 环境（重绑配置源）。
         */
        private Environment environment;

        /**
         * @param properties 属性 bean
         */
        public TaskPropertiesRefresher(UwTaskProperties properties) {
            this.properties = properties;
        }

        /**
         * 注入 Spring 环境。
         *
         * @param environment Spring 环境
         */
        @Override
        public void setEnvironment(Environment environment) {
            this.environment = environment;
        }

        /**
         * 环境变更（nacos 推送等）：原地重绑属性。
         *
         * @param event 环境变更事件
         */
        @EventListener(EnvironmentChangeEvent.class)
        public void onEnvironmentChange(EnvironmentChangeEvent event) {
            if (event.getKeys() == null || event.getKeys().stream().noneMatch(k -> k.startsWith(CONFIG_PREFIX))) {
                return;
            }
            try {
                Binder.get(environment).bind(CONFIG_PREFIX, Bindable.ofInstance(properties));
                log.info("{} 属性已原地热更新（静态持有方自动生效；构造期捕获的组件不受影响）", CONFIG_PREFIX);
            } catch (Exception e) {
                log.warn("{} 属性热更新重绑失败, 保持当前生效配置: {}", CONFIG_PREFIX, e.getMessage());
            }
        }
    }

    /**
     * 装配门控：uw.task.rabbitmq.host 与 uw.task.redis.host 缺失时打 WARN（一次性）并跳过装配——未使用方应用正常启动，
     * 不再因默认 localhost 连接阻塞（需要使用时按 WARN 提示配置即可）。
     */
    static class OnTaskHostsCondition implements org.springframework.context.annotation.Condition {

        /**
         * WARN 只打一次（Condition 可能被多次评估）。
         */
        private static final java.util.concurrent.atomic.AtomicBoolean WARNED = new java.util.concurrent.atomic.AtomicBoolean(false);

        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context, org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            boolean ok = context.getEnvironment().getProperty("uw.task.rabbitmq.host") != null && context.getEnvironment().getProperty("uw.task.redis.host") != null;
            if (!ok && WARNED.compareAndSet(false, true)) {
                LoggerFactory.getLogger(OnTaskHostsCondition.class).warn(
                        "uw-task 未配置 uw.task.rabbitmq.host / uw.task.redis.host，跳过初始化（不使用任务框架属正常；需要 TaskCroner/TaskRunner/sendToQueue 请配置 uw.task.*）");
            }
            return ok;
        }
    }
}
