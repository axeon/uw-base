package uw.task.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.ExceptionUtils;
import uw.common.util.SystemClock;
import uw.task.TaskData;
import uw.task.TaskListenerManager;
import uw.task.TaskRunner;
import uw.task.api.TaskApiClient;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.conf.UwTaskProperties;
import uw.task.constant.TaskConstants;
import uw.task.entity.TaskRunnerConfig;
import uw.task.entity.TaskRunnerLog;
import uw.task.entity.TaskRunnerStats;
import uw.task.listener.TaskRunnerListener;
import uw.task.rabbit.TaskAmqpListenerContainer;
import uw.task.rabbit.TaskAmqpOps;
import uw.task.util.TaskLocalRateLimiter;
import uw.task.util.TaskRateLimitHelper;
import uw.task.util.TaskSequence;
import uw.task.util.TaskStatsService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 队列任务执行容器：MQ 消费端 / 本地执行 / RPC 服务端的统一执行入口。
 *
 * <p>由 TaskFactory 的各个 run* 方法或协议适配层的队列监听器调用 {@link #process}，
 * 统一处理限速、延时、异常分类、监听器回调、统计与日志、失败重试。支持三种 runType：
 * {@code RUN_TYPE_GLOBAL}（MQ 消费）、{@code RUN_TYPE_LOCAL}（本地同步）、{@code RUN_TYPE_GLOBAL_RPC}（RPC 同步）。</p>
 *
 * @author axeon
 */
public class TaskRunnerContainer {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(TaskRunnerContainer.class);

    /**
     * 任务配置
     */
    private final UwTaskProperties taskProperties;

    /**
     * 服务端任务API
     */
    private final TaskApiClient taskApiClient;

    /**
     * 任务信息管理器。
     */
    private final TaskMetaInfoManager taskMetaInfoManager;

    /**
     * 本地流量限制服务
     */
    private final TaskLocalRateLimiter taskLocalRateLimiter;

    /**
     * 监听管理器。
     */
    private final TaskListenerManager taskListenerManager;

    /**
     * 协议无关的 AMQP 操作面（091/100 由启动期探测决定，运行期不变）。
     */
    private final TaskAmqpOps taskAmqpOps;

    /**
     * 任务投递门面（GLOBAL 重试重投；原静态 TaskFactory.getInstance() 回环改为注入，261120 分层整改）。
     */
    private final TaskSendCenter taskSendCenter;

    /**
     * 消息回调方法名（091 协议 MessageListenerAdapter 反射调用）。
     */
    public static final String PROCESS_METHOD = "process";

    /**
     * container缓存。key=队列名,value=container。
     */
    private final ConcurrentHashMap<String, TaskAmqpListenerContainer> queueListenerMap = new ConcurrentHashMap<>();

    /**
     * 构造队列任务执行容器。
     *
     * @param taskProperties        任务全局配置
     * @param taskApiClient         服务端任务 API 客户端（发送日志等）
     * @param taskMetaInfoManager   任务元信息管理器（runner 实例与配置索引）
     * @param taskLocalRateLimiter  进程内限速器（LOCAL 系列限速）
     * @param taskListenerManager   任务监听管理器（pre/postExecute 回调）
     * @param taskAmqpOps           协议无关 AMQP 操作面（声明/收发/监听/查询）
     * @param taskSendCenter         任务投递中心（GLOBAL 重试重投，全路由）
     */
    public TaskRunnerContainer(UwTaskProperties taskProperties, TaskApiClient taskApiClient, TaskMetaInfoManager taskMetaInfoManager,
                               TaskLocalRateLimiter taskLocalRateLimiter, TaskListenerManager taskListenerManager,
                               TaskAmqpOps taskAmqpOps, TaskSendCenter taskSendCenter) {
        this.taskProperties = taskProperties;
        this.taskApiClient = taskApiClient;
        this.taskMetaInfoManager = taskMetaInfoManager;
        this.taskLocalRateLimiter = taskLocalRateLimiter;
        this.taskListenerManager = taskListenerManager;
        this.taskAmqpOps = taskAmqpOps;
        this.taskSendCenter = taskSendCenter;
    }

    /**
     * 执行单个队列任务（MQ 消费端 / 本地执行 / RPC 服务端的统一入口）。
     * <p>
     * 完整流程：
     * <ol>
     *   <li>记录消费时间，定位 runner 实例与配置；runner 缺失则标记 STATE_FAIL_PROGRAM；</li>
     *   <li>按配置的限速类型执行流控（LOCAL 系列用进程令牌桶，GLOBAL 系列用 Redis 固定窗口，
     *       超限则在 rateLimitWait 内轮询等待，仍超限标记 STATE_FAIL_CONFIG）；</li>
     *   <li>LOCAL 任务按 taskDelay 做执行线程内延时（GLOBAL 消息的延迟由投递侧承载）；</li>
     *   <li>调用 {@code runTask}，按异常类型映射状态（PARTNER/DATA/PROGRAM）；</li>
     *   <li>触发 RunnerTaskListener、更新统计、按 logLevel 写日志；</li>
     *   <li>失败时按 retryType/retryTimes 自动重试：<b>仅 LOCAL 任务</b>走 {@link #retryLocal} 本地重跑，
     *       GLOBAL（MQ 队列任务）走 {@link TaskSendCenter#sendToQueue} 重新投递；
     *       <b>GLOBAL_RPC 不在服务端重试</b>（RPC 调用方同步等待，服务端重试易致调用方超时——失败结果直接返回调用方自行决策）。</li>
     * </ol>
     * RPC/LOCAL 模式返回携带结果的 taskData；GLOBAL 模式（MQ 消费）返回 null（结果已随重试/日志处理）。
     *
     * @param taskData 任务数据
     * @return RPC/LOCAL 模式返回 taskData（含结果）；GLOBAL 模式返回 null；入参为 null 时返回 null
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public TaskData process(TaskData taskData) {
        if (taskData == null) {
            log.warn("正在处理的TaskData为NULL，请检查日志.");
            return null;
        }
        try {
            return processInternal(taskData);
        } catch (Throwable e) {
            // 兜底守护：未守护路径（getRunnerConfigByData 找不到配置 / 限速 tryAcquire Redis 故障 / 重试 runTaskLocal·sendToQueue 抛等）
            // 逃逸到 091 监听器（defaultRequeueRejected=false）会直接丢消息——此处 catch 保证消息 ack（不丢）。
            // 防双计：finishDate==null 说明异常发生在正常统计/日志落盘之前（配置缺失/限速故障），补写 stats/log；
            // finishDate!=null 说明异常发生在重试投递阶段（runTaskLocal/sendToQueue 抛），正常路径已用真实失败态写过 stats/log，
            // 此处仅记录日志，不重复写统计、不覆盖真实 state。
            if (taskData.getFinishDate() == null) {
                taskData.setState(TaskData.STATE_FAIL_PROGRAM);
                taskData.setErrorInfo(ExceptionUtils.exceptionToString(e));
                taskData.setFinishDate(SystemClock.nowDate());
                recordProgramFail(taskData);
            }
            log.error("TaskRunner process 未预期异常(已捕获防丢消息),taskClass={}: {}", taskData.getTaskClass(), e.getMessage(), e);
            return taskData;
        }
    }

    /**
     * 单轮执行主体：定位 runner 与配置 → 限速 → 执行 → 统计与日志 → 按失败类型重试（流程说明见 {@link #process}）。
     *
     * @param taskData 任务数据
     * @return RPC/LOCAL 模式返回 taskData；GLOBAL 模式返回 null
     */
    private TaskData processInternal(TaskData taskData) {
        // 设置开始消费时间
        taskData.setConsumeDate(SystemClock.nowDate());
        // 获取任务实例
        TaskRunner<?, ?> taskRunner = taskMetaInfoManager.getRunnerInstance(taskData.getTaskClass());
        if (taskRunner == null) {
            // 列出该 taskClass 已注册的 runner configKey 候选，便于排查：候选空=taskClass 未注册(类型错投/拼写错)，候选非空=配置有但本机未部署 runner bean(部署不一致)
            String taskClassPrefix = taskData.getTaskClass() + "#";
            List<String> registeredKeys = taskMetaInfoManager.getRunnerConfigMap().keySet().stream()
                    .filter(k -> k.startsWith(taskClassPrefix))
                    .toList();
            log.error("未找到TaskRunner实例: taskClass={}, runType={}, runTarget={}, refId={}, refTag={}, 该taskClass已注册配置={}",
                    taskData.getTaskClass(), taskData.getRunType(), taskData.getRunTarget(), taskData.getRefId(), taskData.getRefTag(), registeredKeys);
            taskData.setState(TaskData.STATE_FAIL_PROGRAM);
            taskData.setErrorInfo("TaskRunner not found: " + taskData.getTaskClass());
            taskData.setFinishDate(SystemClock.nowDate());
            // 错投/未注册：强制写一条 ES 日志让可观测；明确不重试（不调用 sendToQueue/runTaskLocal）。
            recordProgramFail(taskData);
            return taskData;
        }
        // 获取任务设置数据
        TaskRunnerConfig taskConfig = taskMetaInfoManager.getRunnerConfigByData(taskData);
        // 增加执行信息
        taskData.setRanTimes(taskData.getRanTimes() + 1);

        // 限速是否通过：true=通过可执行；RPC/LOCAL 模式与无限制时默认通过。
        boolean rateLimitPassed = true;
        // 对于RPC调用和本地调用来说，不受任何流控限制。
        if (taskData.getRunType() != TaskData.RUN_TYPE_GLOBAL_RPC && taskData.getRunType() != TaskData.RUN_TYPE_LOCAL) {
            int rateLimitType = taskConfig.getRateLimitType();
            if (rateLimitType != TaskRunnerConfig.RATE_LIMIT_NONE) {
                int rateLimitValue = taskConfig.getRateLimitValue();
                int rateLimitTime = taskConfig.getRateLimitTime();
                int rateLimitWait = taskConfig.getRateLimitWait();
                String taskClass = taskData.getTaskClass();
                String rateLimitTag = taskData.getRateLimitTag();
                if (rateLimitType == TaskRunnerConfig.RATE_LIMIT_LOCAL
                        || rateLimitType == TaskRunnerConfig.RATE_LIMIT_LOCAL_TASK
                        || rateLimitType == TaskRunnerConfig.RATE_LIMIT_LOCAL_TASK_TAG) {
                    // 进程内限速：限流 key 拼接见 TaskRateLimitHelper.buildLocalLocker
                    String locker = TaskRateLimitHelper.buildLocalLocker(rateLimitType, taskClass, rateLimitTag);
                    rateLimitPassed = taskLocalRateLimiter.tryAcquire(locker, rateLimitValue, rateLimitTime, rateLimitWait, 1);
                } else {
                    // GLOBAL 系列：限流 key 见 buildGlobalLocker，等待循环见 acquireGlobalWithWait
                    String locker = TaskRateLimitHelper.buildGlobalLocker(rateLimitType, taskClass, rateLimitTag, taskProperties.getAppHost());
                    rateLimitPassed = TaskRateLimitHelper.acquireGlobalWithWait(locker, rateLimitValue, rateLimitTime, rateLimitWait);
                }
            }
        }
        // LOCAL 任务（含本地重试退避）：执行线程内延时。GLOBAL 消息不走此分支——
        // MQ 原生延迟到达的消息 taskDelay 仍为原值，若 sleep 会双重延时；zset 转投路径 delay 已清零。
        if (taskData.getRunType() == TaskData.RUN_TYPE_LOCAL && taskData.getTaskDelay() > 0 && taskData.getQueueDate() != null) {
            long delaySleep = taskData.getTaskDelay() - (SystemClock.now() - taskData.getQueueDate().getTime());
            if (delaySleep > 0) {
                try {
                    Thread.sleep(delaySleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        // 设置开始执行时间
        taskData.setRunDate(SystemClock.nowDate());
        // 如果允许，则开始执行。
        if (rateLimitPassed) {
            List<TaskRunnerListener> runnerListenerList = taskListenerManager.getRunnerListenerList();
            // taskData 在下方重试分支可能被重新赋值，监听器回调固定引用本轮执行的这个对象
            final TaskData<?, ?> executing = taskData;
            notifyRunnerListeners(runnerListenerList, listener -> listener.onPreExecute(executing));
            try {
                // 执行任务
                taskData.setResultData(taskRunner.runTask(taskData));
                taskData.setState(TaskData.STATE_SUCCESS);
            } catch (Throwable e) {
                // 异常分类：TaskDataException→DATA / TaskPartnerException→PARTNER / 其余→PROGRAM
                int failState = TaskStatsService.mapFailureState(e);
                taskData.setState(failState);
                // 设置异常信息，自动屏蔽spring自己的输出。
                taskData.setErrorInfo(ExceptionUtils.exceptionToString(e));
                if (failState == TaskData.STATE_FAIL_PROGRAM) {
                    log.error(e.getMessage(), e);
                }
            }
            // 执行监听器操作
            notifyRunnerListeners(runnerListenerList, listener -> listener.onPostExecute(executing));
            // 清除refObject。
            taskData.setRefObject(null);
        } else {
            taskData.setErrorInfo("RateLimit!!!" + taskConfig.getRateLimitValue() + "/" + taskConfig.getRateLimitTime() + "s, Wait " + taskConfig.getRateLimitWait() + "s!");
            taskData.setState(TaskData.STATE_FAIL_CONFIG);
        }

        // 不管如何，都给设定结束日期。
        taskData.setFinishDate(SystemClock.nowDate());
        //开始输出统计数据。
        int[] failCounters = TaskStatsService.toFailCounters(taskData.getState());
        TaskStatsService.updateRunnerStats(taskConfig.getId(), 1, failCounters[0], failCounters[1], failCounters[2], failCounters[3],
                TaskStatsService.toIntClamped(taskData.getConsumeDate().getTime() - (taskData.getQueueDate() != null ? taskData.getQueueDate().getTime() : taskData.getConsumeDate().getTime())),
                TaskStatsService.toIntClamped(taskData.getRunDate().getTime() - taskData.getConsumeDate().getTime()),
                TaskStatsService.toIntClamped(taskData.getFinishDate().getTime() - taskData.getRunDate().getTime()));
        // 保存日志与统计信息
        int logLevel = taskConfig.getLogLevel();
        if (logLevel > TaskRunnerConfig.TASK_LOG_TYPE_NONE) {
            TaskRunnerLog log = new TaskRunnerLog(taskData);
            log.setLogLevel(logLevel);
            log.setLogLimitSize(taskConfig.getLogLimitSize());
            log.setTaskId(taskConfig.getId());
            taskApiClient.sendTaskRunnerLog(log);
        }

        if (taskData.getRunType() == TaskData.RUN_TYPE_GLOBAL_RPC) {
            // RPC 调用（调用方同步等待）不在服务端重试——重新执行会让调用方因等待而超时，
            // 失败结果直接返回调用方由其决策是否重试。
            return taskData;
        }
        if (taskData.getRunType() == TaskData.RUN_TYPE_LOCAL) {
            // 仅 LOCAL 任务在本地重跑；GLOBAL 任务重新入队。
            if (taskData.getRetryType() == TaskData.RETRY_TYPE_AUTO && taskData.getState() > TaskData.STATE_SUCCESS
                    && taskData.getRanTimes() <= retryLimit(taskConfig, taskData.getState())) {
                taskData.setTaskDelay(taskData.getRanTimes() * taskProperties.getTaskRpcRetryDelay());
                taskData = retryLocal(cleanTaskInfo(taskData));
            }
            return taskData;
        }
        if (taskData.getRetryType() == TaskData.RETRY_TYPE_AUTO && taskData.getState() > TaskData.STATE_SUCCESS
                && taskData.getRanTimes() <= retryLimit(taskConfig, taskData.getState())) {
            taskData.setTaskDelay(taskData.getRanTimes() * taskProperties.getTaskQueueRetryDelay());
            taskSendCenter.sendToQueue(cleanTaskInfo(taskData));
        }
        return null;
    }

    /**
     * 逐个回调 runner 监听器（COW 列表快照遍历，单个监听器异常只记日志不中断其余回调）。
     *
     * @param listeners 监听器列表
     * @param callback  本次要触发的回调
     */
    private static void notifyRunnerListeners(List<TaskRunnerListener> listeners, Consumer<TaskRunnerListener> callback) {
        for (TaskRunnerListener listener : listeners) {
            try {
                callback.accept(listener);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 按失败状态取重试上限（OVERRATED 即限速超限 FAIL_CONFIG）。STATE_FAIL_DATA 无重试通道，返回 0。
     *
     * @param taskConfig 任务配置
     * @param state      失败状态
     * @return 额外重试次数上限
     */
    private static int retryLimit(TaskRunnerConfig taskConfig, int state) {
        // 重试判定用 <=：ranTimes 含本次执行，上限 N 时总计执行 N+1 次（1 次初始 + N 次重试）。
        if (state == TaskData.STATE_FAIL_CONFIG) {
            return taskConfig.getRetryTimesByOverrated();
        }
        if (state == TaskData.STATE_FAIL_PARTNER) {
            return taskConfig.getRetryTimesByPartner();
        }
        if (state == TaskData.STATE_FAIL_PROGRAM) {
            return taskConfig.getRetryTimesByProgram();
        }
        return 0;
    }

    /**
     * 框架级程序失败落盘（runner 缺失 / process 兜底异常）：写统计并强制记一条 ES 日志。
     * <p>taskId 与日志截断长度按任务数据反查配置（配置缺失记 0，即 taskId=0、日志不截断）。</p>
     *
     * @param taskData 已置 STATE_FAIL_PROGRAM 与 finishDate 的任务数据
     */
    private void recordProgramFail(TaskData<?, ?> taskData) {
        TaskRunnerConfig config = taskMetaInfoManager.findRunnerConfig(taskData);
        long taskId = config == null ? 0 : config.getId();
        int[] failCounters = TaskStatsService.toFailCounters(taskData.getState());
        TaskStatsService.updateRunnerStats(taskId, 1, failCounters[0], failCounters[1], failCounters[2], failCounters[3], 0, 0, 0);
        TaskRunnerLog runnerLog = new TaskRunnerLog(taskData);
        runnerLog.setTaskId(taskId);
        runnerLog.setLogLevel(TaskRunnerConfig.TASK_LOG_TYPE_RECORD);
        runnerLog.setLogLimitSize(config == null ? 0 : config.getLogLimitSize());
        taskApiClient.sendTaskRunnerLog(runnerLog);
    }

    /**
     * LOCAL 任务本地重跑（重试路径）：重取任务号与入队时间后走 {@link #process}。
     * <p>语义对齐原 TaskFactory.runTaskLocal 的 LOCAL 分支（此处 runType 已是 LOCAL，无需再判 AUTO_RPC 降级）。</p>
     *
     * @param taskData 已清理的重试任务
     * @return 执行结果（同传入对象）
     */
    private TaskData<?, ?> retryLocal(TaskData<?, ?> taskData) {
        taskData.setId(TaskSequence.nextId(TaskConstants.KEY_RUNNER_LOG));
        taskData.setQueueDate(SystemClock.nowDate());
        process(taskData);
        return taskData;
    }

    /**
     * 复制任务数据并清场运行期字段，得到可重投/重跑的副本（原对象保留执行结果供日志与返回）。
     *
     * @param srcData 已执行失败的任务数据
     * @return 清场后的副本
     */
    private TaskData cleanTaskInfo(TaskData srcData) {
        TaskData taskData = srcData.copy();
        //先清除一些任务信息。
        taskData.setRefObject(null);
        taskData.cleanForRetry();
        return taskData;
    }

    /**
     * 注册/更新 RabbitMQ 队列监听容器：首次按配置声明队列/交换机/绑定并启动监听，
     * 后续周期按 state/consumerNum/prefetchNum 动态调整或销毁。
     *
     * @param runnerConfig runner 配置信息
     */
    public void registerQueueListener(TaskRunnerConfig runnerConfig) {
        if (taskAmqpOps == null) {
            // 仅定时任务档（未配置 MQ）：不声明队列、不启动监听（否则每分钟连接失败刷警告）
            log.debug("未配置 MQ，跳过队列监听注册: [{}]", runnerConfig.getTaskClass());
            return;
        }
        String queueName = taskMetaInfoManager.getQueueNameByConfig(runnerConfig);
        TaskAmqpListenerContainer sysContainer = queueListenerMap.computeIfAbsent(queueName, key -> {
            if (runnerConfig.getState() != 1) {
                log.warn("!TaskRunner: [{}]状态为暂停，不进行注册。。。", queueName);
                return null;
            }
            if (log.isDebugEnabled()) {
                log.debug("TaskRunner: [{}]正在注册并启动监听...", queueName);
            }
            try {
                // 队列形态：queueType 单一来源（延时任务的 MQ 原生延迟承载要求 quorum，
                // 由 TaskSendCenter.sendToQueue 的 isQuorumQueue 同源判定保证一致）
                boolean quorum = runnerConfig.getQueueType() == TaskRunnerConfig.QUEUE_TYPE_QUORUM;
                taskAmqpOps.declareDurableQueue(queueName, quorum);
                return taskAmqpOps.createListener(queueName, runnerConfig.getConsumerNum(), runnerConfig.getPrefetchNum(), this);
            } catch (Exception e) {
                // 此处每分钟（updateConfig 周期）会重试，为避免大堆栈刷屏仅打 message；
                // 配置持续失败（如 MQ 不可用）会在后续周期自动重试恢复。
                log.warn("任务队列创建失败, 将持续重试: queue=[{}], msg={}", queueName, e.getMessage());
            }
            return null;
        });
        if (sysContainer != null) {
            if (runnerConfig.getState() == 1) {
                try {
                    // 任务级队列：并发与预取在线可调（协议适配层映射到各自容器参数）
                    sysContainer.tune(runnerConfig.getConsumerNum(), runnerConfig.getPrefetchNum());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                // state<1：队列名即配置键（任务级队列，一对一），销毁监听并摘除缓存
                queueListenerMap.remove(queueName);
                sysContainer.destroy();
            }
        }
    }

    /**
     * 暂停所有队列监听容器（destroy listener + 清 map），保留底层 connectionFactory 以便恢复。
     * <p>用于主机被禁用(state=0)或降级恢复切换时停 listener 却保留连接，解除禁用后可重新注册。</p>
     */
    public void suspendAll() {
        log.info("TaskRunner 全部队列监听已暂停");
        for (TaskAmqpListenerContainer container : queueListenerMap.values()) {
            try {
                // 经协议适配层分发：091 走 destroy()（Spring 生命周期顺序关闭），1.0 关闭全部消费者。
                container.destroy();
            } catch (Throwable e) {
                // 单个 listener destroy 失败不阻断其余 listener 停机 + clear（host-disable 是运维关键操作，须保证后续 croner 也被停）
                log.error("TaskRunner listener destroy 失败: {}", e.getMessage(), e);
            }
        }
        // 清空 map：禁用主机解除后，updateConfig 主循环 registerQueueListener 才会重新创建 listener（否则 map 残留已 destroy 的容器导致跳过）
        queueListenerMap.clear();
    }

    /**
     * 销毁所有队列监听容器（应用关闭场景）：listener 销毁即停消费，底层连接由 AMQP 操作面统一关闭。
     */
    public void shutdown() {
        log.info("TaskRunner 开始销毁，暂停全部队列监听");
        suspendAll();
    }

    /**
     * 填充 runner stats 的 queueSize（队列积压数）与 consumerNum（消费者数）。
     * <p>reportHostInfo 上报前调用，按 stats.taskId 反查 configMap 得到 queueName，经协议操作面查询积压
     * （091 用 queueDeclarePassive，1.0 用管理通道）；同一队列只查一次。</p>
     *
     * @param taskRunnerStatsList runner 统计列表（原地填充 queueSize/consumerNum）
     */
    public void fillQueueBacklog(List<TaskRunnerStats> taskRunnerStatsList) {
        if (taskAmqpOps == null || taskRunnerStatsList == null || taskRunnerStatsList.isEmpty()) {
            // 未配置 MQ（仅定时任务档）：无队列积压可查
            return;
        }
        // 预先构建 taskId -> queueName 索引，避免对每个 stats 遍历全部 config（O(n*m)）。
        Map<Long, String> queueNameByTaskId = new HashMap<>(taskMetaInfoManager.getRunnerConfigMap().size());
        for (TaskRunnerConfig taskRunnerConfig : taskMetaInfoManager.getRunnerConfigMap().values()) {
            queueNameByTaskId.put(taskRunnerConfig.getId(), taskMetaInfoManager.getQueueNameByConfig(taskRunnerConfig));
        }
        //查询缓存，应对公用队列的情况，防止重复查询。key:队列名。
        Map<String, int[]> queueInfoMap = new HashMap<>();
        for (TaskRunnerStats stats : taskRunnerStatsList) {
            String queueName = queueNameByTaskId.get(stats.getTaskId());
            if (queueName == null) {
                continue;
            }
            int[] info;
            try {
                info = queueInfoMap.computeIfAbsent(queueName, taskAmqpOps::getQueueInfo);
            } catch (Exception e) {
                log.warn("查询队列积压失败,queueName={}: {}", queueName, e.getMessage());
                continue;
            }
            if (info == null) {
                continue;
            }
            stats.setQueueSize(info[0]);
            stats.setConsumerNum(info[1]);
        }
    }
}

