package uw.task.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * 任务配置类。
 * <p><b>运行档位</b>（装配时经 {@link #resolveRuntimeMode(Environment)} 判定一次）：
 * Redis 是必需项——任务 id 发号、Leader 选举、延时搬运、全局限速都依赖它，队列任务的各入口第一步就是取号；
 * MQ 只有队列任务的收发与延时载体需要，定时任务和队列任务的本地执行不经过 MQ。</p>
 * <ul>
 *   <li>{@link RuntimeMode#FULL}：Redis 与 MQ 均已配置，全部功能可用；</li>
 *   <li>{@link RuntimeMode#CRONER_ONLY}：只配了 Redis——定时任务与队列任务的本地执行可用，
 *       入队/远程 RPC/延时提交直接抛异常（{@link uw.task.rabbit.TaskAmqpOps#MQ_NOT_CONFIGURED}）；</li>
 *   <li>{@link RuntimeMode#DISABLED}：未配 Redis，或执行主机（enable-registry=true）未配 task-project——不初始化 uw-task。</li>
 * </ul>
 * <p>可选参数不合法的取值（并发上限 ≤0、重试延时 &lt;0、run-target/task-center-host 空白）不阻断启动，
 * 由对应 getter 按默认值生效，装配时经 {@link #logInvalidSettings()} 各打一条 WARN。</p>
 *
 * @author axeon
 */
@ConfigurationProperties(prefix = "uw.task")
public class UwTaskProperties {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(UwTaskProperties.class);

    /**
     * task-center 地址默认值。
     */
    static final String DEFAULT_TASK_CENTER_HOST = "http://uw-task-center";

    /**
     * 运行目标默认值。
     */
    static final String DEFAULT_RUN_TARGET = "default";

    /**
     * runTaskAsync 在途并发上限默认值。
     */
    static final int DEFAULT_TASK_RPC_THREAD_MAX_NUM = 60;

    /**
     * runQueue 本地执行并发上限默认值。
     */
    static final int DEFAULT_TASK_LOCAL_THREAD_MAX_NUM = 60;

    /**
     * GLOBAL 队列任务重试延时默认值（毫秒）。
     */
    static final long DEFAULT_TASK_QUEUE_RETRY_DELAY = 2000L;

    /**
     * LOCAL 任务本地重试延时默认值（毫秒）。
     */
    static final long DEFAULT_TASK_RPC_RETRY_DELAY = 100L;

    /**
     * 默认构造器。
     */
    public UwTaskProperties() {
    }

    /**
     * 应用名称
     */
    @Value("${spring.application.name:unknown}")
    private String appName;

    /**
     * 应用版本
     */
    @Value("${project.version:unknown}")
    private String appVersion;

    /**
     * app主机
     */
    @Value("${spring.cloud.nacos.discovery.ip:unknown}")
    private String appHost;

    /**
     * app端口
     */
    @Value("${server.port:0}")
    private int appPort;

    /**
     * 是否启用任务注册（注册主机并执行任务），默认不启用；仅作为任务调用方的服务保持 false。
     */
    private boolean enableRegistry = false;

    /**
     * 任务中心（uw-task-center）地址。
     */
    private String taskCenterHost = DEFAULT_TASK_CENTER_HOST;

    /**
     * 任务项目，必须是包名前缀：只注册该包下的 TaskCroner/TaskRunner，也作为延时 zset 与 Leader 锁的隔离键。
     * <p>执行主机（enable-registry=true）必填：未配置时打 WARN 并跳过初始化（不注册 TaskFactory）；仅作任务调用方的服务可不配。</p>
     */
    private String taskProject;

    /**
     * 运行目标，重要参数！非运行目标不会执行。默认为“default”。
     */
    private String runTarget = DEFAULT_RUN_TARGET;

    /**
     * runTaskAsync 在途执行并发上限（虚拟线程执行器背压闸门）：远程模式每个在途任务等待 RPC 回复（最长 180s），
     * 超过此并发时由提交线程自行执行（CallerRuns）。此值为纯并发上限，与线程成本无关。
     */
    private int taskRpcThreadMaxNum = DEFAULT_TASK_RPC_THREAD_MAX_NUM;

    /**
     * runQueue 本地队列任务执行并发上限（虚拟线程池背压闸门）：超过此并发先入有界缓冲队列，
     * 缓冲也满则溢出到 MQ 队列由远端消费（SendToQueuePolicy）。虚拟线程化后此值为纯并发上限。
     */
    private int taskLocalThreadMaxNum = DEFAULT_TASK_LOCAL_THREAD_MAX_NUM;

    /**
     * GLOBAL 队列任务重试延时（毫秒）：重试延时=ranTimes×本值，默认 2 秒。
     */
    private long taskQueueRetryDelay = DEFAULT_TASK_QUEUE_RETRY_DELAY;

    /**
     * LOCAL 任务本地重试延时（毫秒）：重试延时=ranTimes×本值，默认 100 毫秒（远程 RPC 不在服务端重试，不使用本值）。
     */
    private long taskRpcRetryDelay = DEFAULT_TASK_RPC_RETRY_DELAY;

    /**
     * Rabbit MQ配置
     */
    private RabbitProperties rabbitmq = new RabbitProperties();

    /**
     * uw-task 私有 Redis（延时 zset/序列发号/Leader 选举/全局限速共用，与业务 Redis、uw-kv 隔离）。
     * 与 spring.data.redis 无关：各项取本处配置或 DataRedisProperties 默认值（uw.task.redis.host 未配置则不装配 uw-task）。
     */
    private final DataRedisProperties redis = new DataRedisProperties();

    /**
     * 获取应用名称。
     *
     * @return 应用名称
     */
    public String getAppName() {
        return appName;
    }

    /**
     * 获取应用版本。
     *
     * @return 应用版本
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * 获取app主机。
     *
     * @return app主机
     */
    public String getAppHost() {
        return appHost;
    }

    /**
     * 获取app端口。
     *
     * @return app端口
     */
    public int getAppPort() {
        return appPort;
    }

    /**
     * 获取是否启用任务注册（注册主机并执行任务），默认不启用；仅作为任务调用方的服务保持 false。
     *
     * @return 是否启用任务注册（注册主机并执行任务），默认不启用；仅作为任务调用方的服务保持 false
     */
    public boolean isEnableRegistry() {
        return enableRegistry;
    }

    /**
     * 设置是否启用任务注册（注册主机并执行任务），默认不启用；仅作为任务调用方的服务保持 false。
     *
     * @param enableRegistry 是否启用任务注册（注册主机并执行任务），默认不启用；仅作为任务调用方的服务保持 false
     */
    public void setEnableRegistry(boolean enableRegistry) {
        this.enableRegistry = enableRegistry;
    }

    /**
     * 获取任务中心（uw-task-center）地址。
     *
     * @return 任务中心（uw-task-center）地址
     */
    public String getTaskCenterHost() {
        return isBlank(taskCenterHost) ? DEFAULT_TASK_CENTER_HOST : taskCenterHost;
    }

    /**
     * 设置任务中心（uw-task-center）地址。
     *
     * @param taskCenterHost 任务中心（uw-task-center）地址
     */
    public void setTaskCenterHost(String taskCenterHost) {
        this.taskCenterHost = taskCenterHost;
    }

    /**
     * 获取任务项目，必须是包名前缀：只注册该包下的 TaskCroner/TaskRunner，也作为延时 zset 与 Leader 锁的隔离键。
     *
     * @return 任务项目，必须是包名前缀：只注册该包下的 TaskCroner/TaskRunner，也作为延时 zset 与 Leader 锁的隔离键
     */
    public String getTaskProject() {
        return taskProject;
    }

    /**
     * 设置任务项目，必须是包名前缀：只注册该包下的 TaskCroner/TaskRunner，也作为延时 zset 与 Leader 锁的隔离键。
     *
     * @param taskProject 任务项目，必须是包名前缀：只注册该包下的 TaskCroner/TaskRunner，也作为延时 zset 与 Leader 锁的隔离键
     */
    public void setTaskProject(String taskProject) {
        this.taskProject = taskProject;
    }

    /**
     * 获取运行目标，重要参数！非运行目标不会执行。默认为“default”。
     *
     * @return 运行目标，重要参数！非运行目标不会执行。默认为“default”
     */
    public String getRunTarget() {
        return isBlank(runTarget) ? DEFAULT_RUN_TARGET : runTarget;
    }

    /**
     * 设置运行目标，重要参数！非运行目标不会执行。默认为“default”。
     *
     * @param runTarget 运行目标，重要参数！非运行目标不会执行。默认为“default”
     */
    public void setRunTarget(String runTarget) {
        this.runTarget = runTarget;
    }

    /**
     * 获取runTaskAsync 在途执行并发上限（虚拟线程执行器背压闸门）：远程模式每个在途任务等待 RPC 回复（最长 180s），超过此并发时由提交线程自行执行（CallerRuns）。此值为纯并发上限，与线程成本无关。
     *
     * @return runTaskAsync 在途执行并发上限（虚拟线程执行器背压闸门）：远程模式每个在途任务等待 RPC 回复（最长 180s），超过此并发时由提交线程自行执行（CallerRuns）。此值为纯并发上限，与线程成本无关
     */
    public int getTaskRpcThreadMaxNum() {
        return taskRpcThreadMaxNum > 0 ? taskRpcThreadMaxNum : DEFAULT_TASK_RPC_THREAD_MAX_NUM;
    }

    /**
     * 设置runTaskAsync 在途执行并发上限（虚拟线程执行器背压闸门）：远程模式每个在途任务等待 RPC 回复（最长 180s），超过此并发时由提交线程自行执行（CallerRuns）。此值为纯并发上限，与线程成本无关。
     *
     * @param taskRpcThreadMaxNum runTaskAsync 在途执行并发上限（虚拟线程执行器背压闸门）：远程模式每个在途任务等待 RPC 回复（最长 180s），超过此并发时由提交线程自行执行（CallerRuns）。此值为纯并发上限，与线程成本无关
     */
    public void setTaskRpcThreadMaxNum(int taskRpcThreadMaxNum) {
        this.taskRpcThreadMaxNum = taskRpcThreadMaxNum;
    }

    /**
     * 获取runQueue 本地队列任务执行并发上限（虚拟线程池背压闸门）：超过此并发先入有界缓冲队列，缓冲也满则溢出到 MQ 队列由远端消费（SendToQueuePolicy）。虚拟线程化后此值为纯并发上限。
     *
     * @return runQueue 本地队列任务执行并发上限（虚拟线程池背压闸门）：超过此并发先入有界缓冲队列，缓冲也满则溢出到 MQ 队列由远端消费（SendToQueuePolicy）。虚拟线程化后此值为纯并发上限
     */
    public int getTaskLocalThreadMaxNum() {
        return taskLocalThreadMaxNum > 0 ? taskLocalThreadMaxNum : DEFAULT_TASK_LOCAL_THREAD_MAX_NUM;
    }

    /**
     * 设置runQueue 本地队列任务执行并发上限（虚拟线程池背压闸门）：超过此并发先入有界缓冲队列，缓冲也满则溢出到 MQ 队列由远端消费（SendToQueuePolicy）。虚拟线程化后此值为纯并发上限。
     *
     * @param taskLocalThreadMaxNum runQueue 本地队列任务执行并发上限（虚拟线程池背压闸门）：超过此并发先入有界缓冲队列，缓冲也满则溢出到 MQ 队列由远端消费（SendToQueuePolicy）。虚拟线程化后此值为纯并发上限
     */
    public void setTaskLocalThreadMaxNum(int taskLocalThreadMaxNum) {
        this.taskLocalThreadMaxNum = taskLocalThreadMaxNum;
    }

    /**
     * 获取GLOBAL 队列任务重试延时（毫秒）：重试延时=ranTimes×本值，默认 2 秒。
     *
     * @return GLOBAL 队列任务重试延时（毫秒）：重试延时=ranTimes×本值，默认 2 秒
     */
    public long getTaskQueueRetryDelay() {
        return taskQueueRetryDelay >= 0 ? taskQueueRetryDelay : DEFAULT_TASK_QUEUE_RETRY_DELAY;
    }

    /**
     * 设置GLOBAL 队列任务重试延时（毫秒）：重试延时=ranTimes×本值，默认 2 秒。
     *
     * @param taskQueueRetryDelay GLOBAL 队列任务重试延时（毫秒）：重试延时=ranTimes×本值，默认 2 秒
     */
    public void setTaskQueueRetryDelay(long taskQueueRetryDelay) {
        this.taskQueueRetryDelay = taskQueueRetryDelay;
    }

    /**
     * 获取LOCAL 任务本地重试延时（毫秒）：重试延时=ranTimes×本值，默认 100 毫秒（远程 RPC 不在服务端重试，不使用本值）。
     *
     * @return LOCAL 任务本地重试延时（毫秒）：重试延时=ranTimes×本值，默认 100 毫秒（远程 RPC 不在服务端重试，不使用本值）
     */
    public long getTaskRpcRetryDelay() {
        return taskRpcRetryDelay >= 0 ? taskRpcRetryDelay : DEFAULT_TASK_RPC_RETRY_DELAY;
    }

    /**
     * 设置LOCAL 任务本地重试延时（毫秒）：重试延时=ranTimes×本值，默认 100 毫秒（远程 RPC 不在服务端重试，不使用本值）。
     *
     * @param taskRpcRetryDelay LOCAL 任务本地重试延时（毫秒）：重试延时=ranTimes×本值，默认 100 毫秒（远程 RPC 不在服务端重试，不使用本值）
     */
    public void setTaskRpcRetryDelay(long taskRpcRetryDelay) {
        this.taskRpcRetryDelay = taskRpcRetryDelay;
    }

    /**
     * 获取Rabbit MQ配置。
     *
     * @return Rabbit MQ配置
     */
    public RabbitProperties getRabbitmq() {
        return rabbitmq;
    }

    /**
     * 设置Rabbit MQ配置。
     *
     * @param rabbitmq Rabbit MQ配置
     */
    public void setRabbitmq(RabbitProperties rabbitmq) {
        this.rabbitmq = rabbitmq;
    }

    /**
     * 装配时调用一次：对不合法的可选参数逐项打 WARN（取值已由对应 getter 回退为默认值，不阻断启动）。
     */
    public void logInvalidSettings() {
        if (isBlank(taskCenterHost)) {
            log.warn("uw.task.task-center-host 为空白，按默认值 {} 生效", DEFAULT_TASK_CENTER_HOST);
        }
        if (isBlank(runTarget)) {
            log.warn("uw.task.run-target 为空白，按默认值 {} 生效", DEFAULT_RUN_TARGET);
        }
        if (taskRpcThreadMaxNum <= 0) {
            log.warn("uw.task.task-rpc-max-thread-num={} 不合法（须 > 0），按默认值 {} 生效", taskRpcThreadMaxNum, DEFAULT_TASK_RPC_THREAD_MAX_NUM);
        }
        if (taskLocalThreadMaxNum <= 0) {
            log.warn("uw.task.task-local-max-thread-num={} 不合法（须 > 0），按默认值 {} 生效", taskLocalThreadMaxNum, DEFAULT_TASK_LOCAL_THREAD_MAX_NUM);
        }
        if (taskQueueRetryDelay < 0) {
            log.warn("uw.task.task-queue-retry-delay={} 不合法（须 >= 0），按默认值 {} 生效", taskQueueRetryDelay, DEFAULT_TASK_QUEUE_RETRY_DELAY);
        }
        if (taskRpcRetryDelay < 0) {
            log.warn("uw.task.task-rpc-retry-delay={} 不合法（须 >= 0），按默认值 {} 生效", taskRpcRetryDelay, DEFAULT_TASK_RPC_RETRY_DELAY);
        }
    }

    /**
     * 运行档位。
     */
    public enum RuntimeMode {
        /**
         * 不初始化 uw-task：未配 Redis，或执行主机未配 task-project。
         */
        DISABLED,
        /**
         * 仅定时任务档：只配了 Redis，定时任务与队列任务的本地执行可用，MQ 相关操作直接失败。
         */
        CRONER_ONLY,
        /**
         * 完整档：Redis 与 MQ 均已配置。
         */
        FULL
    }

    /**
     * 判定运行档位（装配时调用一次）。
     *
     * @param environment Spring 环境（原始配置源）
     * @return 运行档位
     */
    public RuntimeMode resolveRuntimeMode(Environment environment) {
        if (missingRequiredSetting(environment) != null) {
            return RuntimeMode.DISABLED;
        }
        return isMqConfigured(environment) ? RuntimeMode.FULL : RuntimeMode.CRONER_ONLY;
    }

    /**
     * 必填项检测（装配时调用一次）。
     * <p>Redis 的 host 在 Boot 属性类里默认是 localhost，绑定后的值分不出"未配置"与"显式配了 localhost"，
     * 所以按原始配置源判是否出现；其余项直接读本对象字段。</p>
     *
     * @param environment Spring 环境（原始配置源）
     * @return 缺失的必填项说明；必填项齐备返回 null
     */
    public String missingRequiredSetting(Environment environment) {
        Binder binder = Binder.get(environment);
        if (!isBound(binder.bind("uw.task.redis.host", String.class))) {
            return "uw.task.redis.host（Redis 是必需项：任务 id 发号、Leader 选举、延时搬运、全局限速都依赖它）";
        }
        // task-project 同时是 Leader 锁与延时 zset 的隔离键，执行主机缺它无法运行（配置过滤与选举都会空指针）
        if (enableRegistry && isBlank(taskProject)) {
            return "uw.task.task-project（enable-registry=true 时必填，须为任务类的包名前缀）";
        }
        return null;
    }

    /**
     * 是否配置了 MQ。
     *
     * @param environment Spring 环境（原始配置源）
     * @return 配置了 uw.task.rabbitmq.host 或 uw.task.rabbitmq.addresses 返回 true
     */
    public boolean isMqConfigured(Environment environment) {
        Binder binder = Binder.get(environment);
        // 集群形态可只配 addresses 不配 host（连接端点解析与协议探测均以 addresses 优先）
        return isBound(binder.bind("uw.task.rabbitmq.host", String.class))
                || !binder.bind("uw.task.rabbitmq.addresses", Bindable.listOf(String.class)).orElse(List.of()).isEmpty();
    }

    /**
     * 配置项是否有非空白值（null 与空白串均视为未配置）。
     *
     * @param bound 绑定结果
     * @return 有非空白值返回 true
     */
    private static boolean isBound(BindResult<String> bound) {
        return bound.isBound() && !bound.get().isBlank();
    }

    /**
     * 空白判定（null/空串/全空白）。
     *
     * @param value 待判字符串
     * @return 空白返回 true
     */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 返回uw-task 私有 Redis 配置。
     *
     * @return uw-task 私有 Redis 配置
     */
    public DataRedisProperties getRedis() {
        return redis;
    }
}
