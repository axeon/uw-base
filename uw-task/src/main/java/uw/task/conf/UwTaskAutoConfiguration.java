package uw.task.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitAutoConfiguration;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;
import uw.common.util.RedisConnectionFactoryBuilder;
import uw.common.util.ThreadFactoryBuilder;
import uw.log.es.LogClient;
import uw.task.TaskFactory;
import uw.task.TaskListenerManager;
import uw.task.api.TaskApiClient;
import uw.task.constant.TaskConstants;
import uw.task.container.TaskCronerContainer;
import uw.task.container.TaskDelayTransferContainer;
import uw.task.container.TaskRunnerContainer;
import uw.task.container.TaskSendCenter;
import uw.task.entity.TaskCronerLog;
import uw.task.entity.TaskRunnerLog;
import uw.task.rabbit.*;
import uw.task.util.TaskGlobalRateLimiter;
import uw.task.util.TaskLeaderElector;
import uw.task.util.TaskLocalRateLimiter;
import uw.task.util.TaskSequence;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * uw-task 自动配置入口。
 *
 * <p><b>对外 bean 面：业务方只需注入 {@link #taskFactory}</b>——业务方只感知 TaskFactory
 * （runTask 系列方法/sendToQueue/getInstance/getListenerManager），内部组件（AMQP 操作面/私有 Redis/投递中心/延时搬运/容器/注册器）
 * 全部在 taskFactory bean 方法内手工构建并持有，不进 Spring 容器，宿主应用零冲突面。
 * 运行档位（见 {@link UwTaskProperties.RuntimeMode}）在装配时判定：
 * {@code DISABLED} 档整个配置类不注册（与 uw-kv/uw-mfa 的装配门控同机制），未使用任务框架的应用照常启动；
 * {@code CRONER_ONLY} 档（只配 Redis）不探测协议、不创建 MQ 操作面，队列收发类操作入口处快速失败。</p>
 *
 * <p>本类实现 {@link SmartLifecycle}：容器刷新完成后启动周期调度（元信息扫描/配置刷新/主机上报/Leader 选举），
 * 关闭时反向有序回收。内部组件依赖链无环（SendCenter→{ops,meta,Transfer}、Transfer→SendCenter 经
 * 装配期回填破互指、RunnerContainer→SendCenter），框架内部不调用 TaskFactory 静态单例。
 * 另含可选的 {@link TaskPropertiesRefresher}（classpath 有 spring-cloud-context 时装配）。</p>
 *
 * @author axeon
 */
@Configuration
@Conditional(UwTaskAutoConfiguration.OnTaskSettingsCondition.class)
@EnableScheduling
@EnableConfigurationProperties({UwTaskProperties.class})
@AutoConfigureAfter({DataRedisAutoConfiguration.class, RabbitAutoConfiguration.class})
public class UwTaskAutoConfiguration implements SmartLifecycle {

    /**
     * 默认构造器。
     */
    public UwTaskAutoConfiguration() {
    }

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
     * uw-task 私有 Redis 连接工厂（手工构建，stop 时销毁防泄漏）。
     */
    private RedisConnectionFactory taskRedisConnectionFactory;

    /**
     * 协议无关 AMQP 操作面（启动期探测定死）；仅定时任务档（未配置 MQ）为 null。
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
     * ① AMQP 操作面（仅完整档创建：能力探测 amqp 端口双握手，0-9-1 与 1.0 两种协议头，默认 091、仅 091 不可用时走 1.0，并按 0-9-1 版本解析判定原生延迟；
     * 仅定时任务档跳过探测，操作面为 null）；
     * ② 私有 Redis 工厂 + 静态工具 init（TaskSequence/TaskLeaderElector/TaskGlobalRateLimiter）；
     * ③ 工具与容器（meta/apiClient/sendCenter/transfer/runner/croner/registrar）；
     * ④ 注册 ES 日志对象（runner/croner log）；⑤ Leader 参与者注册。
     * <p>档位为 {@link UwTaskProperties.RuntimeMode#DISABLED} 时已被 {@link OnTaskSettingsCondition} 挡在装配之前，
     * 本方法不会再被调用；此处仍做一次判定作为兜底（缺失则告警并返回 null，即不注册 TaskFactory）。</p>
     *
     * @param environment     Spring 环境（档位按原始配置源判定）
     * @param context         Spring 应用上下文（扫描任务 bean）
     * @param taskProperties  任务全局配置
     * @param authRestClient  鉴权 RestClient（调 task-center）
     * @param logClient       ES 日志客户端
     * @return TaskFactory bean（@PreDestroy shutdown 自动调用）
     */
    @Bean
    TaskFactory taskFactory(final Environment environment, final ApplicationContext context, final UwTaskProperties taskProperties,
                            @Qualifier("authRestClient") final RestClient authRestClient, final LogClient logClient) {
        // ⓪ 运行档位：DISABLED 兜底（正常情况下已被 Condition 挡住）
        UwTaskProperties.RuntimeMode mode = taskProperties.resolveRuntimeMode(environment);
        if (mode == UwTaskProperties.RuntimeMode.DISABLED) {
            log.warn("uw-task 未配置 {}，跳过初始化（不使用任务框架属正常；需要 TaskCroner/TaskRunner/sendToQueue 请配置 uw.task.*）",
                    taskProperties.missingRequiredSetting(environment));
            return null;
        }
        // 可选参数合法性：不合法项告警（取值由 getter 回退默认值，不阻断启动）
        taskProperties.logInvalidSettings();
        // ① AMQP 操作面：仅完整档创建
        if (mode == UwTaskProperties.RuntimeMode.FULL) {
            // 能力探测定协议（运行期唯一定死）；原生延迟探测结果喂给两个实现——supportsNativeDelay 单一事实源
            RabbitCapabilityProbe capability = RabbitCapabilityProbe.probe(taskProperties.getRabbitmq());
            if (capability.getProtocol() == TaskAmqpProtocol.AMQP_100) {
                taskAmqpOps = new Rabbit100Ops(taskProperties.getRabbitmq(), capability.isNativeDelayAvailable());
            } else {
                // 091 路径 channelCacheSize 固定值：publisher/listener 共享 cache，50 覆盖常规规模
                taskAmqpOps = new Rabbit091Ops(taskProperties.getRabbitmq(), 50, capability.isNativeDelayAvailable());
            }
        } else {
            log.info("uw-task 未配置 MQ（uw.task.rabbitmq.host 或 addresses），以仅定时任务档运行：定时任务与队列任务的本地执行可用，入队/远程 RPC/延时提交不可用");
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
        delayTransferContainer = new TaskDelayTransferContainer(buildTaskBytesRedisTemplate(taskRedisConnectionFactory), taskProperties.getTaskProject());
        TaskSendCenter taskSendCenter = new TaskSendCenter(taskAmqpOps, metaInfoManager, delayTransferContainer, taskListenerManager);
        delayTransferContainer.bindSendCenter(taskSendCenter);
        TaskRunnerContainer taskRunnerContainer = new TaskRunnerContainer(taskProperties, taskApiClient, metaInfoManager,
                new TaskLocalRateLimiter(), taskListenerManager, taskAmqpOps, taskSendCenter);
        TaskCronerContainer taskCronerContainer = new TaskCronerContainer(taskProperties, taskApiClient, metaInfoManager, taskListenerManager);
        serviceRegistrar = new TaskServiceRegistrar(taskProperties, taskApiClient, taskRunnerContainer, taskCronerContainer, metaInfoManager, delayTransferContainer);
        // ④ 注册 ES 日志对象（原独立 CommandLineRunner 内联：bean 创建期即可完成，无需延迟到 runner 阶段）
        logClient.regLogObjectWithIndexName(TaskRunnerLog.class, TaskConstants.KEY_RUNNER_LOG);
        logClient.regLogObjectWithIndexName(TaskCronerLog.class, TaskConstants.KEY_CRONER_LOG);
        // ⑤ Leader 参与者注册（选举检查在 SmartLifecycle start 的周期任务里执行）。
        // 选举名=task-project：仅作调用方的主机可以不配它，此时不注册（ConcurrentHashMap 不接受 null key）
        leaderLockKey = taskProperties.getTaskProject();
        if (leaderLockKey == null || leaderLockKey.isBlank()) {
            leaderLockKey = null;
            return new TaskFactory(taskProperties, taskAmqpOps, taskSendCenter, taskRunnerContainer, metaInfoManager, delayTransferContainer, taskListenerManager);
        }
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
     * <p>SmartLifecycle 回调时点保证 taskFactory bean（含全部内部组件）已就绪；start 幂等。
     * 必填项缺失而未初始化时（内部组件为 null）直接返回。</p>
     */
    @Override
    public void start() {
        if (lifecycleRunning || serviceRegistrar == null) {
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
                    log.error("主机信息上报定时任务异常", e);
                }
            }, 20, 180, TimeUnit.SECONDS);
            // 选举任务（60s）：检查 Leader 并同步启停延时搬运 mover（Leader 单例执行）
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    TaskLeaderElector.checkLeader(leaderLockKey);
                    delayTransferContainer.setLeader(TaskLeaderElector.isLeader(leaderLockKey));
                } catch (Throwable e) {
                    log.error("Leader 检查定时任务异常", e);
                }
            }, 0, 60, TimeUnit.SECONDS);
        }
        // 配置更新任务（60s，0s 首次驱动 Croner/Runner 启动：center 可用用 center 配置，失败且首次则本地降级）
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                serviceRegistrar.updateConfig();
            } catch (Throwable e) {
                log.error("配置刷新定时任务异常", e);
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    /**
     * 停止（先于 bean 销毁回调）：关调度池 → mover 停 → registrar 在 configLock 内停两容器
     * → AMQP 操作面/私有 Redis 工厂关闭。组件判空：taskFactory bean 构建中途失败时 stop 仍可安全执行。
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
        // RedisConnectionFactory 接口本身不可关闭，Lettuce 实现须经 destroy() 释放连接与线程
        if (taskRedisConnectionFactory instanceof LettuceConnectionFactory lettuce) {
            try {
                lettuce.destroy();
            } catch (Exception ignore) {
                // 关闭失败不阻断其余回收
            }
        }
    }

    /**
     * 返回生命周期是否运行中。
     *
     * @return 生命周期是否运行中
     */
    @Override
    public boolean isRunning() {
        return lifecycleRunning;
    }

    /**
     * uw.task 属性 nacos 热更新监听器（classpath 有 spring-cloud-context 才装配）。
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
     * uw.task 属性原地热更新监听器：环境变更事件含 uw.task.* 键时，把新值重绑到同一个属性对象。
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
         * 构建属性热更新监听器。
         *
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
                String taskProjectBefore = properties.getTaskProject();
                Binder.get(environment).bind(CONFIG_PREFIX, Bindable.ofInstance(properties));
                // task-project 是 Leader 锁与延时 zset 的隔离键，运行期被热更新成空白会让配置过滤与选举空指针：恢复原值并告警
                if (properties.isEnableRegistry() && (properties.getTaskProject() == null || properties.getTaskProject().isBlank())) {
                    properties.setTaskProject(taskProjectBefore);
                    log.warn("uw.task.task-project 被热更新为空白，已忽略（执行主机必填），保持原值: {}", taskProjectBefore);
                }
                log.info("{} 属性已原地热更新（静态持有方自动生效；构造期捕获的组件不受影响）", CONFIG_PREFIX);
            } catch (Exception e) {
                log.warn("{} 属性热更新重绑失败, 保持当前生效配置: {}", CONFIG_PREFIX, e.getMessage());
            }
        }
    }

    /**
     * 装配门控（与 uw-kv/uw-mfa 的装配门控同机制）：运行档位为 {@link UwTaskProperties.RuntimeMode#DISABLED} 时打 WARN（一次性）
     * 并跳过整个配置类——未使用方应用正常启动，不会去连默认的 localhost：
     * <ul>
     *   <li>未配置 uw.task.redis.host（Redis 是必需项）；</li>
     *   <li>执行主机（uw.task.enable-registry=true）未配置 uw.task.task-project——它是 Leader 锁与延时 zset 的隔离键，缺失无法运行。</li>
     * </ul>
     * 只配 Redis 不配 MQ 属于 {@link UwTaskProperties.RuntimeMode#CRONER_ONLY}，照常装配。属性读取走 Binder，环境变量等宽松绑定形式同样生效。
     */
    static class OnTaskSettingsCondition implements Condition {

        /**
         * WARN 只打一次（Condition 可能被多次评估）。
         */
        private static final AtomicBoolean WARNED = new AtomicBoolean(false);

        /**
         * 默认构造器。
         */
        OnTaskSettingsCondition() {
        }

        /**
         * 按运行档位判定是否装配。
         *
         * @param context  条件上下文（取环境属性）
         * @param metadata 注解元数据（未使用）
         * @return 档位不是 DISABLED 返回 true
         */
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            // 配置类此时尚未绑定属性 bean，这里按 uw.task 前缀绑一个临时对象，判定逻辑与装配处共用同一个方法
            UwTaskProperties properties;
            try {
                properties = Binder.get(context.getEnvironment()).bind("uw.task", Bindable.of(UwTaskProperties.class)).orElseGet(UwTaskProperties::new);
            } catch (Exception e) {
                // 裸 Binder 遇到未知配置键会抛异常（@ConfigurationProperties 则默认忽略）：此处不拦截，
                // 交由装配处按已绑定的属性 bean 判定档位
                return true;
            }
            String missing = properties.missingRequiredSetting(context.getEnvironment());
            if (missing == null) {
                return true;
            }
            if (WARNED.compareAndSet(false, true)) {
                log.warn("uw-task 未配置 {}，跳过初始化（不使用任务框架属正常；需要 TaskCroner/TaskRunner/sendToQueue 请配置 uw.task.*）", missing);
            }
            return false;
        }
    }
}
