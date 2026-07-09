package uw.task.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import uw.common.util.ExceptionUtils;
import uw.common.util.KryoUtils;
import uw.common.util.LimitedVirtualThreadExecutor;
import uw.common.util.SystemClock;
import uw.task.TaskDelayer;
import uw.task.TaskData;
import uw.task.TaskListenerManager;
import uw.task.api.TaskApiClient;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.conf.TaskProperties;
import uw.task.entity.TaskDelayerConfig;
import uw.task.entity.TaskDelayerLog;
import uw.task.entity.TaskDelayerStats;
import uw.task.entity.TaskRunnerConfig;
import uw.task.exception.TaskRuntimeException;
import uw.task.listener.TaskDelayerListener;
import uw.task.util.TaskGlobalRateLimiter;
import uw.task.util.TaskLocalRateLimiter;
import uw.task.util.TaskRateLimitHelper;
import uw.task.util.TaskSequenceManager;
import uw.task.util.TaskStatsService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 延迟任务运行容器，uw-task 第三种任务类型的核心引擎。
 *
 * <p>用 Redis zset 承载延迟任务（score=到期时间戳=queueDate+taskDelay），替代 classic TTL+DLX 延迟队列
 * （后者有 per-message TTL 队头阻塞，且换 quorum 也无解）。载体复用 {@link TaskData}（与 TaskRunner 一致）。
 * 执行模型为<b>多实例竞争 poll</b>：每个 uw-task 实例各自 poll 同一 zset，靠 Lua 原子
 * {@code ZRANGEBYSCORE+ZREM} 保证不重复取，执行能力随实例数水平扩展，无 Leader 单点。</p>
 *
 * <h3>运行期机制</h3>
 * <ul>
 *   <li><b>poll 节奏（三态）</b>：命中数据立即连续 poll（高峰贴近实时）；zset 无到期数据 sleep pollInterval秒（低谷省 Redis）；
 *       反压满（并发许可耗尽）sleep {@value #BACKPRESSURE_BACKOFF_MS}ms 后立即重试——反压检查只读本地 Semaphore 不打 Redis，故用短间隔尽快填满空闲 worker；</li>
 *   <li><b>虚拟线程执行</b>：worker 用 {@link LimitedVirtualThreadExecutor}（Semaphore 限并发=consumerNum），
 *       延迟任务 run 多为 IO 密集，虚拟线程不阻塞载体线程；</li>
 *   <li><b>动态批量</b>：单次 pop = min(prefetchNum, 可用许可数)，绝不取超过当前空闲并发数，取到即 submit 即执行，无内存队列缓冲；</li>
 *   <li><b>反压</b>：并发许可耗尽时跳过本次 poll（任务留 zset 不进内存），poll 线程睡 {@value #BACKPRESSURE_BACKOFF_MS}ms 后重试，不阻塞不丢数据；</li>
 *   <li><b>限速</b>：复用 TaskLocal/GlobalRateLimiter，在 worker 虚拟线程内等待配额（LOCAL 系 Guava tryAcquire、
 *       GLOBAL 系循环 sleep 等窗口滚动），等待期间占用一个并发许可；超过 rateLimitWait 仍未获配额则 resubmit 回 zset
 *       （runAt=now+rateLimitWait），连续超 retryTimesByOverrated 标记 STATE_FAIL_CONFIG 防死循环；</li>
 *   <li><b>重试</b>：业务失败（Partner/Program）按 retryTimes resubmit，指数退避（1s/2s/4s…封顶 60s）；</li>
 *   <li><b>统计/日志</b>：每次执行 updateDelayerStats（聚合上报），按 logLevel 写 ES。</li>
 * </ul>
 *
 * @author axeon
 */
public class TaskDelayerContainer {

    private static final Logger log = LoggerFactory.getLogger(TaskDelayerContainer.class);

    /**
     * zset key 前缀。
     */
    private static final String ZSET_PREFIX = "uw-task-delayer:";

    /**
     * Lua 原子 pop 脚本：ZRANGEBYSCORE 取出到期成员并 ZREM 删除，避免多实例重复取与查删竞态。
     * KEYS[1]=zset key，ARGV[1]=当前时间戳(now)，ARGV[2]=批量数。
     */
    private static final DefaultRedisScript<List> POP_SCRIPT = new DefaultRedisScript<>(
            "local members = redis.call('ZRANGEBYSCORE', KEYS[1], 0, ARGV[1], 'LIMIT', 0, ARGV[2]) "
                    + "if #members > 0 then for i=1,#members do redis.call('ZREM', KEYS[1], members[i]) end end "
                    + "return members",
            List.class);

    /** 任务全局配置（提供 runTarget 等）。 */
    private final TaskProperties taskProperties;
    /** 服务端任务 API 客户端（发送 delayer 日志）。 */
    private final TaskApiClient taskApiClient;
    /** 任务元信息管理器（提供 Delayer 实例索引与 configKey，实例注册由其 init() 统一扫描）。 */
    private final TaskMetaInfoManager taskMetaInfoManager;
    /** 进程内限速器（LOCAL 系列限速）。 */
    private final TaskLocalRateLimiter taskLocalRateLimiter;
    /** 全局限速器（GLOBAL 系列限速，基于 Redis）。 */
    private final TaskGlobalRateLimiter taskGlobalRateLimiter;
    /** 全局发号器（生成 TaskDelayerLog id，resubmit 换新 id 用）。 */
    private final TaskSequenceManager taskSequenceManager;
    /** Redis 操作模板，key 用 String 序列化、value 用 byte[]（Kryo 序列化 TaskData）。 */
    private final RedisTemplate<String, byte[]> redisTemplate;
    /** 任务监听管理器（execute 后回调 TaskDelayerListener，对齐 Runner/Croner）。 */
    private final TaskListenerManager taskListenerManager;

    /**
     * configKey -> 运行句柄（poll 线程 + 虚拟线程执行器），按 configKey 索引。
     * <p>用 configKey 而非 config.getId()：本地默认配置首次上传前 id=0，多个 Delayer 启动期按 id 入表会互相覆盖碰撞；
     * configKey（taskClass#taskTag$runTarget）天然区分多实例，且与 delayerConfigMap 一致，
     * 服务端下发真实 id 后按同一 configKey 热更新，handle 生命周期连续。</p>
     */
    private final Map<String, TaskDelayerRunHandle> runHandleMap = new ConcurrentHashMap<>();

    /**
     * 反压跳过累计（可观测）。
     */
    private final AtomicLong backpressureSkipCount = new AtomicLong();

    /**
     * 反压满时的短 backoff（毫秒）：worker 全在跑、zset 仍有到期数据待取时，poll 线程以此间隔重试。
     * <p>反压检查只读本地 Semaphore、不打 Redis，故可取较小值；worker 空闲后被重新填满的空窗 ≤ 此值。
     * 取 50ms：相对 pollInterval(默认 3s) 把反压空窗压到可忽略，唤醒频率 20/s 无压力。</p>
     */
    private static final long BACKPRESSURE_BACKOFF_MS = 50L;

    /**
     * 构造延迟任务容器，初始化专用的 RedisTemplate（String key + byte[] value，Kryo 序列化 TaskData）。
     *
     * @param taskProperties          任务全局配置
     * @param taskApiClient           服务端任务 API 客户端
     * @param taskMetaInfoManager     任务元信息管理器（提供 Delayer 实例索引与 configKey）
     * @param taskLocalRateLimiter    进程内限速器
     * @param taskGlobalRateLimiter   全局限速器
     * @param taskSequenceManager     全局发号器
     * @param redisConnectionFactory  Redis 连接工厂（构建 zset 操作模板）
     * @param taskListenerManager     任务监听管理器（execute 后回调 TaskDelayerListener）
     */
    public TaskDelayerContainer(TaskProperties taskProperties, TaskApiClient taskApiClient, TaskMetaInfoManager taskMetaInfoManager,
                                TaskLocalRateLimiter taskLocalRateLimiter, TaskGlobalRateLimiter taskGlobalRateLimiter,
                                TaskSequenceManager taskSequenceManager, RedisConnectionFactory redisConnectionFactory,
                                TaskListenerManager taskListenerManager) {
        this.taskProperties = taskProperties;
        this.taskApiClient = taskApiClient;
        this.taskMetaInfoManager = taskMetaInfoManager;
        this.taskLocalRateLimiter = taskLocalRateLimiter;
        this.taskGlobalRateLimiter = taskGlobalRateLimiter;
        this.taskSequenceManager = taskSequenceManager;
        this.taskListenerManager = taskListenerManager;
        this.redisTemplate = new RedisTemplate<>();
        this.redisTemplate.setKeySerializer(new StringRedisSerializer());
        this.redisTemplate.setValueSerializer(RedisSerializer.byteArray());
        this.redisTemplate.setConnectionFactory(redisConnectionFactory);
        this.redisTemplate.afterPropertiesSet();
    }

    /**
     * 启动：从 TaskMetaInfoManager 取已注册的 TaskDelayer 实例，用 initConfig() 默认配置注册并开始 poll。
     * 无 TaskDelayer bean 时实例 Map 为空，不启动任何 poll 线程、零资源占用。
     * <p>本方法本身不检查 enableRegistry（由调用方 TaskServiceRegistrar.updateTaskDelayerConfig 的降级分支决定）；
     * Delayer 启动现已与 TaskRunner/TaskCroner 对称——仅 enable-registry=true 的执行主机才启动任务。</p>
     */
    @SuppressWarnings("rawtypes")
    public void start() {
        // Delayer 实例由 TaskMetaInfoManager.init() 统一扫描注册，此处直接取索引遍历配置
        for (Map.Entry<String, TaskDelayer<?, ?>> entry : taskMetaInfoManager.getDelayerInstanceMap().entrySet()) {
            String taskClass = entry.getKey();
            TaskDelayer delayer = entry.getValue();
            log.info("TaskDelayerContainer 注册 delayer: {}", taskClass);
            try {
                TaskDelayerConfig config = delayer.initConfig();
                if (config != null) {
                    config.setTaskClass(taskClass);
                    config.setRunTarget(taskProperties.getRunTarget());
                    configureTaskDelayer(config);
                }
            } catch (Exception e) {
                log.error("TaskDelayerContainer 初始化 delayer[{}]配置失败: {}", taskClass, e.getMessage(), e);
            }
        }
    }

    /**
     * 注册/更新一个延迟任务配置：按 state 启动或暂停对应的 poll+worker。
     * <p>线程安全：本方法及 {@link #stop}、{@link #suspendAll}、{@link #destroyAll} 的所有生产调用路径都在
     * {@code TaskServiceRegistrar.configLock} 内串行执行（updateConfig / reportHostInfo 禁用分支 / shutdownAllTasks 持锁调用），
     * {@code runHandleMap} 为 ConcurrentHashMap（{@link #fillDelayerStats} 在锁外并发读亦安全），故无需 synchronized。
     * 注意：handle.stop(30000) 的 drain 仍在 configLock 持有期间执行，会阻塞竞争同一锁的 suspendAll/destroyAll
     *（仅 consumerNum 变更触发重建时 drain，罕见，通常 &lt;1s）。</p>
     */
    public void configureTaskDelayer(TaskDelayerConfig config) {
        // 配置兜底：防止 0/负值导致 busy poll(pollInterval=0)、永不取数(prefetchNum=0)、并发0
        if (config.getPollInterval() < 1) {
            config.setPollInterval(1);
        }
        if (config.getPrefetchNum() < 1) {
            config.setPrefetchNum(1);
        }
        if (config.getConsumerNum() < 1) {
            config.setConsumerNum(1);
        }
        String configKey = taskMetaInfoManager.getDelayerConfigKey(config);
        // state<1（暂停/删除）：remove 配置缓存 + stop handle，不缓存废弃配置（对齐 Runner fetchRunnerConfig 的 state<1 remove）
        if (config.getState() < 1) {
            taskMetaInfoManager.removeDelayerConfig(configKey);
            stop(configKey);
            log.info("TaskDelayer[{}]已暂停/删除", configKey);
            return;
        }
        TaskDelayerConfig oldConfig = taskMetaInfoManager.getDelayerConfigMap().get(configKey);
        // delayerConfigMap（MetaInfoManager 统一管理）总是更新：rateLimit/retry/log/pollInterval/prefetchNum 等靠 RunHandle.config(volatile) 即时生效
        taskMetaInfoManager.setDelayerConfig(configKey, config);
        TaskDelayerRunHandle existing = runHandleMap.get(configKey);
        // 仅 consumerNum(workerPool 并发数) 变化才需重建 handle；其余字段走 volatile 即时更新
        boolean needRebuild = existing == null
                || oldConfig == null
                || oldConfig.getConsumerNum() != config.getConsumerNum();
        if (!needRebuild) {
            existing.updateConfig(config);
            log.info("TaskDelayer[{}]配置已热更新: pollInterval={}s, prefetchNum={}, rateLimitType={}, retryByPartner={}, retryByProgram={}, logLevel={}",
                    configKey, config.getPollInterval(), config.getPrefetchNum(), config.getRateLimitType(),
                    config.getRetryTimesByPartner(), config.getRetryTimesByProgram(), config.getLogLevel());
            return;
        }
        if (existing != null) {
            existing.stop(30000);
            runHandleMap.remove(configKey);
        }
        if (taskMetaInfoManager.getDelayerInstance(config.getTaskClass()) == null) {
            log.warn("TaskDelayer[{}]未找到匹配 delayer 实例，跳过启动", config.getTaskClass());
            return;
        }
        TaskDelayerRunHandle handle = new TaskDelayerRunHandle(config, configKey);
        runHandleMap.put(configKey, handle);
        handle.start();
        log.info("TaskDelayer[{}]已启动: consumerNum={}, pollInterval={}s, prefetchNum={}", configKey, config.getConsumerNum(), config.getPollInterval(), config.getPrefetchNum());
    }

    /**
     * 提交一个延迟任务到 zset，到点后由本机 TaskDelayer 容器 poll 执行。
     * <p>调用方通过 {@link TaskData#builder} 设置 taskClass（TaskDelayer 实现类）、taskDelay（延迟毫秒）、
     * taskParam（业务参数）等；taskTag/rateLimitTag/refId/refTag 等可选。框架补 id/queueDate/runTarget/retryType。
     * 与 TaskFactory.sendToQueue 对称（都以 TaskData 为载体）。</p>
     *
     * @param task 延迟任务数据（taskClass/taskDelay/taskParam 必填，其余可选）
     */
    public void submit(TaskData<?, ?> task) {
        task.setId(taskSequenceManager.nextId("TaskDelayerLog"));
        task.setQueueDate(SystemClock.nowDate());
        if (task.getRunTarget() == null) {
            task.setRunTarget(taskProperties.getRunTarget());
        }
        task.setRetryType(TaskData.RETRY_TYPE_AUTO);
        // 错投防御：校验 (taskClass, taskTag, runTarget) 在服务端 delayerConfigMap 有对应配置，
        // 否则任务会落无 poll handle 的 zset 永久滞留。合法跨机投递（本机无 bean 但别机 poll）
        // 由服务端配置存在性保证——查配置表而非本机实例表。
        String configKey = taskMetaInfoManager.getConfigKey(task.getTaskClass(), task.getTaskTag(), task.getRunTarget());
        if (!taskMetaInfoManager.getDelayerConfigMap().containsKey(configKey)) {
            // 列出该 taskClass 已注册的 configKey 候选，便于排查：候选空=类型错投(taskClass 非 delayer)，候选非空但无精确 key=taskTag/runTarget 错配
            String taskClassPrefix = task.getTaskClass() + "#";
            List<String> registeredKeys = taskMetaInfoManager.getDelayerConfigMap().keySet().stream()
                    .filter(k -> k.startsWith(taskClassPrefix))
                    .toList();
            log.error("TaskDelayer投递被拒: configKey={} 未在服务端delayerConfigMap注册, taskClass={}, taskTag={}, runTarget={}, 该taskClass已注册配置={}",
                    configKey, task.getTaskClass(), task.getTaskTag(), task.getRunTarget(), registeredKeys);
            throw new TaskRuntimeException("TaskDelayer not registered or taskTag/runTarget mismatch.");
        }
        addTask(task);
    }

    /**
     * 将一个 TaskData 写入其对应 zset（submit 入口与重试 resubmit 共用）。score = queueDate + taskDelay（到期时间戳）。
     */
    private void addTask(TaskData task) {
        String zsetKey = ZSET_PREFIX + taskMetaInfoManager.getConfigKey(task.getTaskClass(), task.getTaskTag(), task.getRunTarget());
        double score = task.getQueueDate().getTime() + task.getTaskDelay();
        redisTemplate.opsForZSet().add(zsetKey, KryoUtils.serialize(task), score);
    }

    /**
     * 填充 delayer stats 的 queueSize（zset 积压数）与 consumerNum（活跃执行线程数）。
     * <p>reportHostInfo 上报前调用，按 stats.taskId 反查 configMap 得到 configKey，
     * 查 zset zcard 得积压数，查 runHandle 得本机当前正在执行的虚拟线程数（运行期实际值，
     * 对齐 runner 侧 AMQP getConsumerCount，不复读 config 上限）。</p>
     */
    public void fillDelayerStats(List<TaskDelayerStats> statsList) {
        if (statsList == null || statsList.isEmpty()) {
            return;
        }
        // 预建 taskId -> configKey 反查索引，避免对每个 stats 遍历全部 config（O(n*m)→O(n)，对齐 runner 侧 queueNameByTaskId）
        Map<String, TaskDelayerConfig> configMap = taskMetaInfoManager.getDelayerConfigMap();
        Map<Long, String> configKeyByTaskId = new HashMap<>(configMap.size());
        for (Map.Entry<String, TaskDelayerConfig> e : configMap.entrySet()) {
            configKeyByTaskId.put(e.getValue().getId(), e.getKey());
        }
        for (TaskDelayerStats stats : statsList) {
            String configKey = configKeyByTaskId.get(stats.getTaskId());
            if (configKey == null) {
                continue;
            }
            TaskDelayerRunHandle handle = runHandleMap.get(configKey);
            Long size = redisTemplate.opsForZSet().size(ZSET_PREFIX + configKey);
            stats.setQueueSize(size == null ? 0 : size.intValue());
            // 活跃执行数：本机当前正在执行的虚拟线程数；handle 不存在（本机无 bean / 已暂停 / 合法跨机投递由别机 poll）时填 0
            stats.setConsumerNum(handle == null ? 0 : handle.activeWorkerCount());
        }
    }

    /**
     * 停止指定 configKey 的 delayer handle（drain 30s 等在途完成），并从 runHandleMap 移除。
     * <p>对齐 TaskCronerContainer.stop(String)：单一职责只管运行句柄，配置缓存移除由调用方负责。
     * 线程安全由 TaskServiceRegistrar.configLock 保证（与 configureTaskDelayer 同在 configLock 内串行调用，从其内部调用为同线程重入）。</p>
     *
     * @param configKey 配置键 taskClass#taskTag$runTarget
     * @return 是否存在并停止了 handle
     */
    public boolean stop(String configKey) {
        TaskDelayerRunHandle handle = runHandleMap.get(configKey);
        if (handle != null) {
            handle.stop(30000);
            runHandleMap.remove(configKey);
            log.info("TaskDelayer[{}]已停止", configKey);
            return true;
        }
        return false;
    }

    /**
     * 暂停所有延迟任务（host 禁用）：drain 30s 等在途完成，不丢任务。
     */
    public void suspendAll() {
        log.info("All TaskDelayer Suspended....");
        for (TaskDelayerRunHandle handle : runHandleMap.values()) {
            handle.stop(30000);
        }
        runHandleMap.clear();
        log.info("TaskDelayerContainer 已暂停，反压跳过累计: {}", backpressureSkipCount.get());
    }

    /**
     * 销毁所有延迟任务（@PreDestroy）：drain 5s + shutdownNow 兜底，不阻塞应用关闭太久。
     */
    public void destroyAll() {
        log.info("All TaskDelayer Destroy....");
        for (TaskDelayerRunHandle handle : runHandleMap.values()) {
            handle.stop(5000);
        }
        runHandleMap.clear();
        log.info("TaskDelayerContainer 已销毁，反压跳过累计: {}", backpressureSkipCount.get());
    }

    /**
     * 单个 TaskDelayerConfig 的运行句柄：独立 poll 线程 + 虚拟线程执行器。
     */
    private final class TaskDelayerRunHandle {

        // volatile：configureTaskDelayer 热更新时 updateConfig 切换引用，pollLoop/pollOnce/execute 立即看到新值
        private volatile TaskDelayerConfig config;
        private final String zsetKey;
        private final LimitedVirtualThreadExecutor workerPool;
        private final int concurrency;
        private volatile boolean running = true;
        private Thread pollThread;

        TaskDelayerRunHandle(TaskDelayerConfig config, String configKey) {
            this.config = config;
            this.zsetKey = ZSET_PREFIX + configKey;
            this.concurrency = Math.max(1, config.getConsumerNum());
            // 虚拟线程 + Semaphore 限并发 + CallerRunsPolicy 反压（permit 满 poll 线程自跑 execute）
            this.workerPool = new LimitedVirtualThreadExecutor(
                    "uw-task-delayer-worker-" + config.getTaskClass() + "-",
                    concurrency,
                    new LimitedVirtualThreadExecutor.CallerRunsPolicy());
        }

        /**
         * 启动 poll 线程（daemon），独立轮询 zset 并将到期任务提交到 workerPool。
         */
        void start() {
            pollThread = new Thread(this::pollLoop, "uw-task-delayer-poll-" + config.getTaskClass());
            pollThread.setDaemon(true);
            pollThread.start();
        }

        /**
         * 热更新配置：切换 volatile config 引用，pollLoop/pollOnce/execute 下次读取即生效。
         * 仅用于非 consumerNum 字段（rateLimit/retry/log/pollInterval/prefetchNum 等）；consumerNum 变化需重建 handle 以重建 workerPool。
         */
        void updateConfig(TaskDelayerConfig config) {
            this.config = config;
        }

        /**
         * 当前正在执行的任务/虚拟线程数（运行期实际并发，对齐 runner 侧 AMQP getConsumerCount 语义）。
         */
        int activeWorkerCount() {
            return workerPool.getActiveCount();
        }

        /**
         * 停止 poll 线程与 workerPool：设置 running=false、中断 poll 线程、shutdown workerPool，
         * drain 模式等待最多 drainMillis 让在途虚拟线程任务完成（降级切换/关闭不丢任务），超时 shutdownNow 兜底。
         *
         * @param drainMillis drain 超时（suspendAll=30s 等在途完成；destroyAll=5s 不阻塞应用关闭太久）
         */
        void stop(long drainMillis) {
            running = false;
            if (pollThread != null) {
                pollThread.interrupt();
                try {
                    // 等 poll 线程退出：让它完成当前 batch 的 submit（或 submit 失败时写回 zset），
                    // 避免 workerPool.shutdown 后 poll 线程继续 submit 抛 IllegalStateException 导致已 pop 的任务丢失
                    pollThread.join(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            workerPool.shutdown();
            // drain：等待在途虚拟线程任务完成（IO 任务），降级切换/关闭不丢任务
            long deadline = SystemClock.now() + drainMillis;
            while (workerPool.getActiveCount() > 0 && SystemClock.now() < deadline) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (workerPool.getActiveCount() > 0) {
                log.warn("TaskDelayer[{}]workerPool 仍有 {} 个任务未完成，调用 shutdownNow 强制中断", config.getTaskClass(), workerPool.getActiveCount());
                workerPool.shutdownNow();
            }
        }

        /**
         * poll 循环：自适应间隔（命中连续 poll，空才 sleep）。
         */
        private void pollLoop() {
            while (running) {
                try {
                    int r = pollOnce();
                    if (r < 0) {
                        // BACKPRESSURE：worker 满、zset 可能有数据，短 backoff 后立即重试（反压检查不打 Redis）
                        Thread.sleep(BACKPRESSURE_BACKOFF_MS);
                    } else if (r == 0) {
                        // EMPTY：zset 无到期数据，正常间隔省 Redis
                        Thread.sleep(config.getPollInterval() * 1000L);
                    }
                    // r > 0：HIT，命中并已提交 worker，立即连续 poll
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Throwable e) {
                    log.error("TaskDelayer[{}]poll 异常: {}", config.getTaskClass(), e.getMessage(), e);
                    try {
                        Thread.sleep(config.getPollInterval() * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        /**
         * 执行一次 poll：反压水位检查 → Lua 原子 pop（动态批量=可用许可）→ 逐条反序列化 → 提交 worker。
         *
         * @return 1=HIT（命中并提交）；0=EMPTY（zset 无到期数据）；-1=BACKPRESSURE（并发许可耗尽，任务留 zset）
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private int pollOnce() {
            // 反压水位检查：并发许可耗尽 → 返回 BACKPRESSURE(-1)，任务留 zset 不进内存，pollLoop 用短 backoff 重试
            int available = workerPool.getAvailablePermits();
            if (available <= 0) {
                backpressureSkipCount.incrementAndGet();
                return -1;
            }
            int batch = Math.min(config.getPrefetchNum(), available);
            List<byte[]> members = redisTemplate.execute(POP_SCRIPT, java.util.Collections.singletonList(zsetKey),
                    String.valueOf(SystemClock.now()), String.valueOf(batch));
            if (members == null || members.isEmpty()) {
                return 0;
            }
            for (Object member : members) {
                byte[] bytes = (byte[]) member;
                TaskData task;
                try {
                    task = KryoUtils.deserialize(bytes, TaskData.class);
                } catch (Exception e) {
                    // 脏数据逐条跳过，遵循 KryoUtils 异常策略（不泄露 kryo 细节）
                    log.warn("TaskDelayer[{}]反序列化失败,跳过(len={}): {}", config.getTaskClass(), bytes.length, e.toString());
                    continue;
                }
                task.setConsumeDate(SystemClock.nowDate());
                try {
                    workerPool.submit(() -> execute(task, config));
                } catch (Exception e) {
                    // workerPool 已关闭（stop/降级切换）：把已 pop 的任务写回 zset，避免丢失（他机或本机恢复后重新 poll）
                    log.warn("TaskDelayer[{}]workerPool 提交失败,任务写回zset: {}", config.getTaskClass(), e.toString());
                    addTask(task);
                }
            }
            return 1;
        }
    }

    /**
     * 执行单个延迟任务（虚拟线程内）：限速(不阻塞) → run → 异常分类 → 重试 → 统计 → 日志。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void execute(TaskData task, TaskDelayerConfig config) {
        task.setRanTimes(task.getRanTimes() + 1);
        try {
            boolean rateLimitPassed = acquireRateLimit(config, task);
            long startRun = 0;
            if (rateLimitPassed) {
                // runDate 在限速通过后设置，确保 timeRun 只统计业务执行耗时
                task.setRunDate(SystemClock.nowDate());
                startRun = task.getRunDate().getTime();
                try {
                    TaskDelayer runner = taskMetaInfoManager.getDelayerInstance(task.getTaskClass());
                    if (runner != null) {
                        runner.run(task);
                        task.setState(TaskData.STATE_SUCCESS);
                    } else {
                        task.setState(TaskData.STATE_FAIL_PROGRAM);
                        task.setErrorInfo("TaskDelayer not found: " + task.getTaskClass());
                    }
                } catch (Throwable e) {
                    // 异常分类：TaskDataException→DATA / TaskPartnerException→PARTNER / 其余→PROGRAM
                    int failState = TaskStatsService.mapFailureState(e);
                    task.setState(failState);
                    task.setErrorInfo(ExceptionUtils.exceptionToString(e));
                    if (failState == TaskData.STATE_FAIL_PROGRAM) {
                        log.error(e.getMessage(), e);
                    }
                }
            } else {
                // 限速等待超时（rateLimitWait 内未获配额，极端高并发）：标记 STATE_FAIL_CONFIG，重试决策统一放到统计/日志之后
                task.setErrorInfo("RateLimit!!!" + config.getRateLimitValue() + "/" + config.getRateLimitTime() + "s");
                task.setState(TaskData.STATE_FAIL_CONFIG);
            }
            // 监听器回调（run 后，与 Runner/Croner 对齐）：无论成功/失败都回调，监听器异常不影响任务执行流程
            List<TaskDelayerListener> delayerListenerList = taskListenerManager.getDelayerListenerList();
            for (TaskDelayerListener listener : delayerListenerList) {
                try {
                    listener.onPostExecute(task);
                } catch (Throwable e) {
                    log.error("TaskDelayerListener.onPostExecute 异常: {}", e.getMessage(), e);
                }
            }
            // 清理 refObject，避免日志序列化时带入大对象/不可序列化对象（对齐 TaskRunner.process）
            task.setRefObject(null);
            task.setFinishDate(SystemClock.nowDate());
            // 统计：timeWaitDelay=consumeDate-(queueDate+taskDelay)（实际延迟等待），timeRun=runDate→finishDate
            long expectedRunAt = task.getQueueDate() != null ? task.getQueueDate().getTime() + task.getTaskDelay() : 0;
            int[] failCounters = TaskStatsService.toFailCounters(task.getState());
            TaskStatsService.updateDelayerStats(config.getId(), 1, failCounters[0], failCounters[1], failCounters[2], failCounters[3],
                    (task.getConsumeDate() != null && expectedRunAt > 0) ? TaskStatsService.toIntClamped(task.getConsumeDate().getTime() - expectedRunAt) : 0,
                    startRun > 0 ? TaskStatsService.toIntClamped(task.getFinishDate().getTime() - startRun) : 0);
            // 日志（先于重试落盘：resubmit 失败不丢统计/日志，对齐 TaskRunner.process 顺序）
            if (config.getLogLevel() > TaskDelayerConfig.TASK_LOG_TYPE_NONE) {
                TaskDelayerLog delayerLog = new TaskDelayerLog(task);
                delayerLog.setLogLevel(config.getLogLevel());
                delayerLog.setLogLimitSize(config.getLogLimitSize());
                delayerLog.setTaskId(config.getId());
                taskApiClient.sendTaskDelayerLog(delayerLog);
            }
            // 重试决策（统计/日志已落盘）：PARTNER/PROGRAM 指数退避，CONFIG(限速) 用 rateLimitWait 让配额恢复
            if (task.getRetryType() == TaskData.RETRY_TYPE_AUTO && task.getState() > TaskData.STATE_SUCCESS) {
                if (task.getState() == TaskData.STATE_FAIL_PARTNER && task.getRanTimes() <= config.getRetryTimesByPartner()) {
                    resubmit(task,retryBackoffMillis(task.getRanTimes()));
                } else if (task.getState() == TaskData.STATE_FAIL_PROGRAM && task.getRanTimes() <= config.getRetryTimesByProgram()) {
                    resubmit(task,retryBackoffMillis(task.getRanTimes()));
                } else if (task.getState() == TaskData.STATE_FAIL_CONFIG && task.getRanTimes() <= config.getRetryTimesByOverrated()) {
                    resubmit(task,config.getRateLimitWait() * 1000L);
                }
            }
        } catch (Throwable e) {
            // 兜底：任务已通过 Lua ZRANGEBYSCORE+ZREM 从 zset pop（in-flight 归本机），若 post-run（统计/日志/resubmit）抛异常，
            // 失败任务可能既不重试也无完整日志。对齐 TaskRunner.process 的 outer catch：映射失败状态 + 兜底写统计/日志保证可观测。
            // 最常见触发：resubmit→addTask 在 Redis 抖动时抛——恰是失败任务最需重试时。
            // 避免双计：finishDate 已设说明统计/日志已在正常路径落盘（异常发生在 resubmit 阶段），仅记录日志不再重写统计/日志。
            if (task.getFinishDate() == null) {
                if (task.getState() <= TaskData.STATE_SUCCESS) {
                    task.setState(TaskData.STATE_FAIL_PROGRAM);
                }
                task.setErrorInfo(ExceptionUtils.exceptionToString(e));
                task.setFinishDate(SystemClock.nowDate());
                int[] failCounters = TaskStatsService.toFailCounters(task.getState());
                TaskStatsService.updateDelayerStats(config.getId(), 1, failCounters[0], failCounters[1], failCounters[2], failCounters[3], 0, 0);
                if (config.getLogLevel() > TaskDelayerConfig.TASK_LOG_TYPE_NONE) {
                    TaskDelayerLog delayerLog = new TaskDelayerLog(task);
                    delayerLog.setLogLevel(config.getLogLevel());
                    delayerLog.setLogLimitSize(config.getLogLimitSize());
                    delayerLog.setTaskId(config.getId());
                    taskApiClient.sendTaskDelayerLog(delayerLog);
                }
                // 任务已通过 Lua pop（in-flight），异常发生在 run 之前（如限速器 acquireRateLimit 的 Redis 故障、
                // setRunDate 之前的任何异常），任务未执行也不进入正常重试决策——写回 zset 延迟重 poll，避免已 pop 任务静默丢失。
                // 必须有上限防无限死循环：ranTimes <= retryTimesByOverrated 才重投（cleanForRetry 不清 ranTimes，逐次累加达上限即停），
                // 超限则接受单任务丢失——避免持续基础设施故障下无限重投灌水统计、稀释平均延迟从而 masking 延迟超时告警。
                try {
                    if (task.getRanTimes() <= config.getRetryTimesByOverrated()) {
                        resubmit(task,config.getRateLimitWait() * 1000L);
                    } else {
                        log.warn("TaskDelayer[{}]兜底重投已达上限(ranTimes>{}),停止重投接受丢失: {}", config.getTaskClass(), config.getRetryTimesByOverrated(), e.getMessage());
                    }
                } catch (Throwable ex) {
                    log.error("TaskDelayer[{}]兜底写回zset失败,任务可能丢失: {}", config.getTaskClass(), ex.getMessage(), ex);
                }
            }
            log.error("TaskDelayer[{}]execute 兜底异常, taskClass={}, id={}: {}", config.getTaskClass(), task.getTaskClass(), task.getId(), e.getMessage(), e);
        }
    }

    /**
     * 业务失败重试退避（指数：1s/2s/4s/8s…，封顶 60s）。
     * 与 pollInterval 解耦——pollInterval 是 poll 节奏，不该影响重试间隔；指数递增防乒乓（频繁打故障方）。
     */
    private static long retryBackoffMillis(int ranTimes) {
        long backoff = 1000L << Math.clamp(ranTimes - 1, 0, 20);
        return Math.min(backoff, 60_000L);
    }

    /**
     * 重新提交任务回 zset（限速/业务失败重试）：清理运行期字段与 refObject，queueDate 重置，taskDelay=退避。
     */
    private void resubmit(TaskData task, long delayMillis) {
        TaskData copy = task.copy();
        // 重试使用新 id，避免与原任务同 id 导致 ES 日志互相覆盖（对齐 TaskRunner 重试 setId）
        copy.setId(taskSequenceManager.nextId("TaskDelayerLog"));
        copy.cleanForRetry();
        copy.setRefObject(null);
        copy.setQueueDate(SystemClock.nowDate());
        copy.setTaskDelay(delayMillis);
        addTask(copy);
    }

    /**
     * 限速检查：在 worker 虚拟线程内 sleep 等待配额（虚拟线程 park 不占平台载体），rateLimitWait 内通过则返回 true。
     * <ul>
     *   <li>LOCAL 系列：Guava 令牌桶 tryAcquire(rateLimitWait)（内部精确 park 等下个令牌）；</li>
     *   <li>GLOBAL 系列：tryAcquire 返回窗口剩余毫秒，sleep 到下个窗口后重试，循环直到通过或 rateLimitWait 超时。</li>
     * </ul>
     * 相比 resubmit 模式：不乒乓、不写放大 zset、poll 线程不做无用功；代价是限速等待期间占一个并发许可
     * （但虚拟线程 park 不占平台线程，且等待期间 Semaphore 许可被占，pollOnce 水位检查会跳过 poll，自然反压）。
     */
    private boolean acquireRateLimit(TaskDelayerConfig config, TaskData task) {
        int type = config.getRateLimitType();
        if (type == TaskDelayerConfig.RATE_LIMIT_NONE) {
            return true;
        }
        int value = config.getRateLimitValue();
        int time = config.getRateLimitTime();
        if (value == 0 || time == 0) {
            return true;
        }
        String taskClass = task.getTaskClass();
        String tag = task.getRateLimitTag();
        if (type == TaskDelayerConfig.RATE_LIMIT_LOCAL
                || type == TaskDelayerConfig.RATE_LIMIT_LOCAL_TASK
                || type == TaskDelayerConfig.RATE_LIMIT_LOCAL_TASK_TAG) {
            // 进程内限速：限流 key 拼接见 TaskRateLimitHelper.buildLocalLocker
            String locker = TaskRateLimitHelper.buildLocalLocker(type, taskClass, tag);
            return taskLocalRateLimiter.tryAcquire(locker, value, time, config.getRateLimitWait(), 1);
        } else {
            // GLOBAL 系列：限流 key 见 buildGlobalLocker，等待循环见 acquireGlobalWithWait（虚拟线程 park 不占载体）
            String locker = TaskRateLimitHelper.buildGlobalLocker(type, taskClass, tag, taskProperties.getAppHost());
            return TaskRateLimitHelper.acquireGlobalWithWait(taskGlobalRateLimiter, locker, value, time, config.getRateLimitWait());
        }
    }
}
