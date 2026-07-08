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
import uw.task.api.TaskApiClient;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.conf.TaskProperties;
import uw.task.entity.TaskDelayerConfig;
import uw.task.entity.TaskDelayerLog;
import uw.task.entity.TaskDelayerStats;
import uw.task.entity.TaskRunnerConfig;
import uw.task.exception.TaskDataException;
import uw.task.exception.TaskPartnerException;
import uw.task.util.TaskGlobalRateLimiter;
import uw.task.util.TaskLocalRateLimiter;
import uw.task.util.TaskRateLimitHelper;
import uw.task.util.TaskSequenceManager;
import uw.task.util.TaskStatsService;

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
 *   <li><b>限速不阻塞</b>：复用 TaskLocal/GlobalRateLimiter，但<b>不在 worker 内 sleep 等待</b>——超限直接
 *       resubmit 回 zset（runAt=now+需等待毫秒），worker 立即释放并发许可；限速等待通过 zset 延迟实现，
 *       不占线程。连续超 retryTimesByOverrated 标记 STATE_FAIL_CONFIG 防死循环；</li>
 *   <li><b>重试</b>：业务失败（Partner/Program）按 retryTimes resubmit，线性退避；</li>
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
    private static final String ZSET_PREFIX = "uw-task-delay:";

    /**
     * Lua 原子 pop 脚本：ZRANGEBYSCORE 取出到期成员并 ZREM 删除，避免多实例重复取与查删竞态。
     * KEYS[1]=zset key，ARGV[1]=当前时间戳(now)，ARGV[2]=批量数。
     */
    private static final DefaultRedisScript<List> POP_SCRIPT = new DefaultRedisScript<>(
            "local members = redis.call('ZRANGEBYSCORE', KEYS[1], 0, ARGV[1], 'LIMIT', 0, ARGV[2]) "
                    + "if #members > 0 then for i=1,#members do redis.call('ZREM', KEYS[1], members[i]) end end "
                    + "return members",
            List.class);

    /** 任务元信息管理器（提供 Delayer 实例索引与 configKey，实例注册由其 init() 统一扫描）。 */
    private final TaskMetaInfoManager taskMetaInfoManager;
    /** 服务端任务 API 客户端（发送 delayer 日志）。 */
    private final TaskApiClient taskApiClient;
    /** 进程内限速器（LOCAL 系列限速）。 */
    private final TaskLocalRateLimiter taskLocalRateLimiter;
    /** 全局限速器（GLOBAL 系列限速，基于 Redis）。 */
    private final TaskGlobalRateLimiter taskGlobalRateLimiter;
    /** 任务全局配置（提供 runTarget 等）。 */
    private final TaskProperties taskProperties;
    /** 全局发号器（生成 TaskDelayerLog id，resubmit 换新 id 用）。 */
    private final TaskSequenceManager taskSequenceManager;
    /** Redis 操作模板，key 用 String 序列化、value 用 byte[]（Kryo 序列化 TaskData）。 */
    private final RedisTemplate<String, byte[]> redisTemplate;

    /**
     * taskId -> 运行句柄（poll 线程 + 虚拟线程执行器），按服务端配置 id 索引（对齐 Croner cronerTasks 按 id）。
     */
    private final Map<Long, TaskDelayerRunHandle> runHandleMap = new ConcurrentHashMap<>();

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
     * @param taskMetaInfoManager     任务元信息管理器（提供 Delayer 实例索引与 configKey）
     * @param taskApiClient           服务端任务 API 客户端
     * @param taskLocalRateLimiter    进程内限速器
     * @param taskGlobalRateLimiter   全局限速器
     * @param taskProperties          任务全局配置
     * @param taskSequenceManager     全局发号器
     * @param redisConnectionFactory  Redis 连接工厂（构建 zset 操作模板）
     */
    public TaskDelayerContainer(TaskMetaInfoManager taskMetaInfoManager, TaskApiClient taskApiClient, TaskLocalRateLimiter taskLocalRateLimiter,
                                TaskGlobalRateLimiter taskGlobalRateLimiter, TaskProperties taskProperties,
                                TaskSequenceManager taskSequenceManager, RedisConnectionFactory redisConnectionFactory) {
        this.taskMetaInfoManager = taskMetaInfoManager;
        this.taskApiClient = taskApiClient;
        this.taskLocalRateLimiter = taskLocalRateLimiter;
        this.taskGlobalRateLimiter = taskGlobalRateLimiter;
        this.taskProperties = taskProperties;
        this.taskSequenceManager = taskSequenceManager;
        this.redisTemplate = new RedisTemplate<>();
        this.redisTemplate.setKeySerializer(new StringRedisSerializer());
        this.redisTemplate.setValueSerializer(RedisSerializer.byteArray());
        this.redisTemplate.setConnectionFactory(redisConnectionFactory);
        this.redisTemplate.afterPropertiesSet();
    }

    /**
     * 启动：从 TaskMetaInfoManager 取已注册的 TaskDelayer 实例，用 initConfig() 默认配置注册并开始 poll。
     * 无 TaskDelayer bean 时实例 Map 为空，不启动任何 poll 线程、零资源占用（与 TaskRunner/TaskCroner 一致，故无需启用开关）。
     */
    @SuppressWarnings("rawtypes")
    public void start() {
        // Delayer 实例由 TaskMetaInfoManager.init() 统一扫描注册，此处直接取索引遍历配置
        for (Map.Entry<String, TaskDelayer<?, ?>> entry : taskMetaInfoManager.getDelayerInstanceMap().entrySet()) {
            String taskClass = entry.getKey();
            TaskDelayer runner = entry.getValue();
            log.info("TaskDelayerContainer 注册 runner: {}", taskClass);
            try {
                TaskDelayerConfig config = runner.initConfig();
                if (config != null) {
                    config.setTaskClass(taskClass);
                    config.setRunTarget(taskProperties.getRunTarget());
                    configureTaskDelayer(config);
                }
            } catch (Exception e) {
                log.error("TaskDelayerContainer 初始化 runner[{}]配置失败: {}", taskClass, e.getMessage(), e);
            }
        }
    }

    /**
     * 注册/更新一个延迟任务配置：按 state 启动或暂停对应的 poll+worker。
     */
    public synchronized void configureTaskDelayer(TaskDelayerConfig config) {
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
        String configKey = TaskMetaInfoManager.getConfigKey(config.getTaskClass(), config.getTaskTag(), config.getRunTarget());
        // state<1（暂停/删除）：remove 配置缓存 + stop handle，不缓存废弃配置（对齐 Runner updateRunnerConfig 的 state<1 remove）
        if (config.getState() < 1) {
            taskMetaInfoManager.removeDelayerConfig(configKey);
            TaskDelayerRunHandle existingHandle = runHandleMap.get(config.getId());
            if (existingHandle != null) {
                existingHandle.stop();
                runHandleMap.remove(config.getId());
            }
            log.info("TaskDelayer[{}]已暂停/删除", configKey);
            return;
        }
        TaskDelayerConfig oldConfig = taskMetaInfoManager.getDelayerConfigMap().get(configKey);
        // delayerConfigMap（MetaInfoManager 统一管理）总是更新：rateLimit/retry/log/pollInterval/prefetchNum 等靠 RunHandle.config(volatile) 即时生效
        taskMetaInfoManager.setDelayerConfig(configKey, config);
        TaskDelayerRunHandle existing = runHandleMap.get(config.getId());
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
            existing.stop();
            runHandleMap.remove(config.getId());
        }
        if (taskMetaInfoManager.getDelayerInstance(config.getTaskClass()) == null) {
            log.warn("TaskDelayer[{}]未找到匹配 runner，跳过启动", config.getTaskClass());
            return;
        }
        TaskDelayerRunHandle handle = new TaskDelayerRunHandle(config, configKey);
        runHandleMap.put(config.getId(), handle);
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
        addTask(task);
    }

    /**
     * 将一个 TaskData 写入其对应 zset（submit 入口与重试 resubmit 共用）。score = queueDate + taskDelay（到期时间戳）。
     */
    private void addTask(TaskData task) {
        String zsetKey = ZSET_PREFIX + TaskMetaInfoManager.getConfigKey(task.getTaskClass(), task.getTaskTag(), task.getRunTarget());
        double score = task.getQueueDate().getTime() + task.getTaskDelay();
        redisTemplate.opsForZSet().add(zsetKey, KryoUtils.serialize(task), score);
    }

    /**
     * 填充 delayer stats 的 zsetSize（zset 积压数）与 consumerNum（执行线程数）。
     * <p>reportHostInfo 上报前调用，按 stats.taskId 反查 configMap 得到 configKey，查 zset zcard。</p>
     */
    public void fillDelayerStats(List<TaskDelayerStats> statsList) {
        if (statsList == null || statsList.isEmpty()) {
            return;
        }
        for (TaskDelayerStats s : statsList) {
            for (Map.Entry<String, TaskDelayerConfig> e : taskMetaInfoManager.getDelayerConfigMap().entrySet()) {
                if (e.getValue().getId() == s.getTaskId()) {
                    Long size = redisTemplate.opsForZSet().size(ZSET_PREFIX + e.getKey());
                    s.setQueueSize(size == null ? 0 : size.intValue());
                    s.setConsumerNum(e.getValue().getConsumerNum());
                    break;
                }
            }
        }
    }

    /**
     * 关闭所有 poll 与 worker。
     */
    public synchronized void stopAllTaskDelayer() {
        for (TaskDelayerRunHandle handle : runHandleMap.values()) {
            handle.stop();
        }
        runHandleMap.clear();
        log.info("TaskDelayerContainer 已停止，反压跳过累计: {}", backpressureSkipCount.get());
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
         * 停止 poll 线程与 workerPool：设置 running=false、中断 poll 线程、shutdown workerPool，
         * 最多等待 5 秒让在途虚拟线程任务完成，超时后强制关闭并告警。
         */
        void stop() {
            running = false;
            if (pollThread != null) {
                pollThread.interrupt();
            }
            workerPool.shutdown();
            // 有限等待在途虚拟线程（IO 任务），避免进程关闭丢任务
            long deadline = SystemClock.now() + 5000;
            while (workerPool.getActiveCount() > 0 && SystemClock.now() < deadline) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (workerPool.getActiveCount() > 0) {
                log.warn("TaskDelayer[{}]workerPool 仍有 {} 个任务未完成，强制关闭", config.getTaskClass(), workerPool.getActiveCount());
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
                workerPool.submit(() -> execute(task, config));
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
            } catch (TaskDataException e) {
                task.setState(TaskData.STATE_FAIL_DATA);
                task.setErrorInfo(ExceptionUtils.exceptionToString(e));
            } catch (TaskPartnerException e) {
                task.setState(TaskData.STATE_FAIL_PARTNER);
                task.setErrorInfo(ExceptionUtils.exceptionToString(e));
            } catch (Throwable e) {
                task.setState(TaskData.STATE_FAIL_PROGRAM);
                task.setErrorInfo(ExceptionUtils.exceptionToString(e));
                log.error(e.getMessage(), e);
            }
        } else {
            // 限速等待超时（rateLimitWait 内未获配额，极端高并发）：标记 STATE_FAIL_CONFIG，重试决策统一放到统计/日志之后
            task.setErrorInfo("RateLimit!!!" + config.getRateLimitValue() + "/" + config.getRateLimitTime() + "s");
            task.setState(TaskData.STATE_FAIL_CONFIG);
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
        if (config.getLogLevel() > TaskRunnerConfig.TASK_LOG_TYPE_NONE) {
            TaskDelayerLog delayLog = new TaskDelayerLog(task);
            delayLog.setLogLevel(config.getLogLevel());
            delayLog.setLogLimitSize(config.getLogLimitSize());
            delayLog.setTaskId(config.getId());
            taskApiClient.sendTaskDelayerLog(delayLog);
        }
        // 重试决策（统计/日志已落盘）：PARTNER/PROGRAM 指数退避，CONFIG(限速) 用 rateLimitWait 让配额恢复
        if (task.getRetryType() == TaskData.RETRY_TYPE_AUTO && task.getState() > TaskData.STATE_SUCCESS) {
            if (task.getState() == TaskData.STATE_FAIL_PARTNER && task.getRanTimes() <= config.getRetryTimesByPartner()) {
                resubmit(task, config, retryBackoffMillis(task.getRanTimes()));
            } else if (task.getState() == TaskData.STATE_FAIL_PROGRAM && task.getRanTimes() <= config.getRetryTimesByProgram()) {
                resubmit(task, config, retryBackoffMillis(task.getRanTimes()));
            } else if (task.getState() == TaskData.STATE_FAIL_CONFIG && task.getRanTimes() <= config.getRetryTimesByOverrated()) {
                resubmit(task, config, config.getRateLimitWait() * 1000L);
            }
        }
    }

    /**
     * 业务失败重试退避（指数：1s/2s/4s/8s…，封顶 60s）。
     * 与 pollInterval 解耦——pollInterval 是 poll 节奏，不该影响重试间隔；指数递增防乒乓（频繁打故障方）。
     */
    private static long retryBackoffMillis(int ranTimes) {
        long backoff = 1000L << Math.min(Math.max(ranTimes - 1, 0), 20);
        return Math.min(backoff, 60_000L);
    }

    /**
     * 重新提交任务回 zset（限速/业务失败重试）：清理运行期字段与 refObject，queueDate 重置，taskDelay=退避。
     */
    private void resubmit(TaskData task, TaskDelayerConfig config, long delayMillis) {
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
        if (type == TaskRunnerConfig.RATE_LIMIT_NONE) {
            return true;
        }
        int value = config.getRateLimitValue();
        int time = config.getRateLimitTime();
        if (value == 0 || time == 0) {
            return true;
        }
        String taskClass = task.getTaskClass();
        String tag = task.getRateLimitTag();
        String host = taskProperties.getAppHost();
        if (type == TaskRunnerConfig.RATE_LIMIT_LOCAL) {
            return taskLocalRateLimiter.tryAcquire("", value, time, config.getRateLimitWait(), 1);
        } else if (type == TaskRunnerConfig.RATE_LIMIT_LOCAL_TASK) {
            return taskLocalRateLimiter.tryAcquire(taskClass, value, time, config.getRateLimitWait(), 1);
        } else if (type == TaskRunnerConfig.RATE_LIMIT_LOCAL_TASK_TAG) {
            return taskLocalRateLimiter.tryAcquire(taskClass + "$" + tag, value, time, config.getRateLimitWait(), 1);
        } else {
            // GLOBAL 系列：sleep 等待窗口滚动（虚拟线程 park），rateLimitWait 内循环重试
            // 限流 key 拼接规则见 TaskRateLimitHelper.buildGlobalLocker。
            String locker = TaskRateLimitHelper.buildGlobalLocker(type, taskClass, tag, host);
            long end = SystemClock.now() + config.getRateLimitWait() * 1000L;
            while (SystemClock.now() <= end) {
                long waitMillis = taskGlobalRateLimiter.tryAcquire(locker, value, time, 1);
                if (waitMillis == 0) {
                    return true;
                }
                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        }
    }
}
