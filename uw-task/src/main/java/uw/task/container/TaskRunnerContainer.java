package uw.task.container;

import com.rabbitmq.client.AMQP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import uw.common.util.ExceptionUtils;
import uw.common.util.SystemClock;
import uw.task.TaskData;
import uw.task.TaskFactory;
import uw.task.TaskListenerManager;
import uw.task.TaskRunner;
import uw.task.api.TaskApiClient;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.conf.TaskProperties;
import uw.task.converter.TaskMessageConverter;
import uw.task.entity.TaskRunnerConfig;
import uw.task.entity.TaskRunnerLog;
import uw.task.entity.TaskRunnerStats;
import uw.task.listener.TaskRunnerListener;
import uw.task.util.TaskGlobalRateLimiter;
import uw.task.util.TaskLocalRateLimiter;
import uw.task.util.TaskRateLimitHelper;
import uw.task.util.TaskStatsService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 队列任务执行容器：MQ 消费端 / 本地执行 / RPC 服务端的统一执行入口。
 *
 * <p>由 {@link TaskFactory} 的各个 run* 方法或 RabbitMQ MessageListenerAdapter 调用 {@link #process}，
 * 统一处理限速、延时、异常分类、监听器回调、统计与日志、失败重试。支持三种 runType：
 * {@code RUN_TYPE_GLOBAL}（MQ 消费）、{@code RUN_TYPE_LOCAL}（本地同步）、{@code RUN_TYPE_GLOBAL_RPC}（RPC 同步）。</p>
 *
 * @author axeon
 */
public class TaskRunnerContainer {

    private static final Logger log = LoggerFactory.getLogger(TaskRunnerContainer.class);


    /**
     * 任务配置
     */
    private final TaskProperties taskProperties;

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
     * 全局流量限制服务
     */
    private final TaskGlobalRateLimiter taskGlobalRateLimiter;

    /**
     * 监听管理器。
     */
    private final TaskListenerManager taskListenerManager;

    /**
     * RabbitMQ 管理器（声明队列/交换机/绑定 + 查询队列积压）。
     */
    private final RabbitAdmin rabbitAdmin;

    /**
     * 任务专用的 RabbitMQ 连接工厂（用于队列监听容器）。
     */
    private final ConnectionFactory taskConnectionFactory;

    /**
     * container缓存。key=队列名,value=container。
     */
    private final ConcurrentHashMap<String, SimpleMessageListenerContainer> queueListenerMap = new ConcurrentHashMap<>();

    /**
     * 默认构造器。
     *
     * @param taskProperties        任务全局配置
     * @param taskApiClient         服务端任务 API 客户端（发送日志等）
     * @param taskMetaInfoManager   任务元信息管理器（runner 实例与配置索引）
     * @param taskLocalRateLimiter  进程内限速器（LOCAL 系列限速）
     * @param taskGlobalRateLimiter 全局限速器（GLOBAL 系列限速，基于 Redis）
     * @param taskListenerManager   任务监听管理器（pre/postExecute 回调）
     * @param rabbitAdmin           RabbitMQ 管理器（声明队列/交换机/绑定 + 查询积压）
     * @param taskConnectionFactory 任务专用 RabbitMQ 连接工厂（队列监听容器用）
     */
    public TaskRunnerContainer(TaskProperties taskProperties, TaskApiClient taskApiClient, TaskMetaInfoManager taskMetaInfoManager,
                               TaskLocalRateLimiter taskLocalRateLimiter, TaskGlobalRateLimiter taskGlobalRateLimiter, TaskListenerManager taskListenerManager,
                               RabbitAdmin rabbitAdmin, ConnectionFactory taskConnectionFactory) {
        this.taskProperties = taskProperties;
        this.taskApiClient = taskApiClient;
        this.taskMetaInfoManager = taskMetaInfoManager;
        this.taskLocalRateLimiter = taskLocalRateLimiter;
        this.taskGlobalRateLimiter = taskGlobalRateLimiter;
        this.taskListenerManager = taskListenerManager;
        this.rabbitAdmin = rabbitAdmin;
        this.taskConnectionFactory = taskConnectionFactory;
    }


    /**
     * 执行单个队列任务（MQ 消费端 / 本地执行 / RPC 服务端的统一入口）。
     * <p>
     * 完整流程：
     * <ol>
     *   <li>记录消费时间，定位 runner 实例与配置；runner 缺失则标记 STATE_FAIL_PROGRAM；</li>
     *   <li>按配置的限速类型执行流控（LOCAL 系列用进程令牌桶，GLOBAL 系列用 Redis 固定窗口，
     *       超限则在 rateLimitWait 内轮询等待，仍超限标记 STATE_FAIL_CONFIG）；</li>
     *   <li>无延迟队列时按 taskDelay 做线程内延时；</li>
     *   <li>调用 {@code runTask}，按异常类型映射状态（PARTNER/DATA/PROGRAM）；</li>
     *   <li>触发 RunnerTaskListener、更新统计、按 logLevel 写日志；</li>
     *   <li>失败时按 retryType/retryTimes 自动重试：<b>仅 LOCAL 任务</b>走 {@link TaskFactory#runTaskLocal} 本地重跑，
     *       GLOBAL（MQ 队列任务）走 {@link TaskFactory#sendToQueue} 重新投递；
     *       <b>GLOBAL_RPC 不在服务端重试</b>（RPC 调用方同步等待，服务端重试易致调用方超时——失败结果直接返回调用方自行决策）。</li>
     * </ol>
     * RPC/LOCAL 模式返回携带结果的 taskData；GLOBAL 模式（MQ 消费）返回 null（结果已随重试/日志处理）。</p>
     *
     * @param taskData 任务数据
     * @return RPC/LOCAL 模式返回 taskData（含结果）；GLOBAL 模式返回 null；入参为 null 时返回 null
     */
    @SuppressWarnings("ALL")
    public TaskData process(TaskData taskData) {
        if (taskData == null) {
            log.warn("正在处理的TaskData为NULL，请检查日志.");
            return null;
        }
        try {
            return processInternal(taskData);
        } catch (Throwable e) {
            // 兜底守护：未守护路径（getRunnerConfigByData 找不到配置 / 限速 tryAcquire Redis 故障 / 重试 runTaskLocal·sendToQueue 抛等）
            // 逃逸到 MessageListenerAdapter 配合 defaultRequeueRejected(false) 会直接丢消息——此处 catch 保证消息 ack（不丢）。
            // 防双计：finishDate==null 说明异常发生在正常统计/日志落盘之前（配置缺失/限速故障），补写 stats/log；
            // finishDate!=null 说明异常发生在重试投递阶段（runTaskLocal/sendToQueue 抛），正常路径已用真实失败态写过 stats/log，
            // 此处仅记录日志，不重复写统计、不覆盖真实 state（对齐 TaskDelayerContainer.execute 的 finishDate 守卫）。
            if (taskData.getFinishDate() == null) {
                taskData.setState(TaskData.STATE_FAIL_PROGRAM);
                taskData.setErrorInfo(ExceptionUtils.exceptionToString(e));
                taskData.setFinishDate(SystemClock.nowDate());
                long taskId = 0;
                try {
                    TaskRunnerConfig cfg = taskMetaInfoManager.getRunnerConfigByData(taskData);
                    if (cfg != null) {
                        taskId = cfg.getId();
                    }
                } catch (Exception ignore) {
                    // 配置缺失（正是本 catch 常见触发原因），taskId 保持 0
                }
                int[] failCounters = TaskStatsService.toFailCounters(taskData.getState());
                TaskStatsService.updateRunnerStats(taskId, 1, failCounters[0], failCounters[1], failCounters[2], failCounters[3], 0, 0, 0);
                TaskRunnerLog runnerLog = new TaskRunnerLog(taskData);
                runnerLog.setTaskId(taskId);
                runnerLog.setLogLevel(TaskRunnerConfig.TASK_LOG_TYPE_RECORD);
                taskApiClient.sendTaskRunnerLog(runnerLog);
            }
            log.error("TaskRunner process 未预期异常(已捕获防丢消息),taskClass={}: {}", taskData.getTaskClass(), e.getMessage(), e);
            return taskData;
        }
    }

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
            long taskId = 0;
            try {
                TaskRunnerConfig cfg = taskMetaInfoManager.getRunnerConfigByData(taskData);
                if (cfg != null) {
                    taskId = cfg.getId();
                }
            } catch (Exception ignore) {
                // 未注册配置（实例/配置同进退），taskId 保持 0
            }
            TaskRunnerLog runnerLog = new TaskRunnerLog(taskData);
            runnerLog.setTaskId(taskId);
            runnerLog.setLogLevel(TaskRunnerConfig.TASK_LOG_TYPE_RECORD);
            taskApiClient.sendTaskRunnerLog(runnerLog);
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
                    rateLimitPassed = TaskRateLimitHelper.acquireGlobalWithWait(taskGlobalRateLimiter, locker, rateLimitValue, rateLimitTime, rateLimitWait);
                }
            }
        }
        // 执行任务延时设定，必须是无延时队列功能，才会执行线程延时。
        if (taskConfig.getDelayType() == TaskRunnerConfig.TYPE_DELAY_OFF && taskData.getTaskDelay() > 0 && taskData.getQueueDate() != null) {
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
            if (runnerListenerList != null && runnerListenerList.size() > 0) {
                try {
                    for (TaskRunnerListener listener : runnerListenerList) {
                        listener.onPostExecute(taskData);
                    }
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
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

        if (taskData.getRunType() == TaskData.RUN_TYPE_GLOBAL_RPC || taskData.getRunType() == TaskData.RUN_TYPE_LOCAL) {
            // GLOBAL_RPC（RPC 调用，调用方同步等待）不在服务端重试——重新执行会让调用方因等待而超时，
            // 失败结果直接返回调用方由其决策是否重试；仅 LOCAL 任务在本地重跑。
            if (taskData.getRunType() == TaskData.RUN_TYPE_LOCAL && taskData.getRetryType() == TaskData.RETRY_TYPE_AUTO) {
                if (taskData.getState() > TaskData.STATE_SUCCESS) {
                    //设置任务延时
                    taskData.setTaskDelay(taskData.getRanTimes() * taskProperties.getTaskRpcRetryDelay());
                    if (taskData.getState() == TaskData.STATE_FAIL_CONFIG) {
                        // 重试判定用 <=：ranTimes 含本次执行，N 次额外重试意味着 ranTimes 可达 N+1。
                        // 即 retryTimesByOverrated=N 时，总计执行 N+1 次（1 次初始 + N 次重试）。
                        if (taskData.getRanTimes() <= taskConfig.getRetryTimesByOverrated()) {
                            taskData = TaskFactory.getInstance().runTaskLocal(cleanTaskInfo(taskData));
                        }
                    } else if (taskData.getState() == TaskData.STATE_FAIL_PARTNER) {
                        if (taskData.getRanTimes() <= taskConfig.getRetryTimesByPartner()) {
                            taskData = TaskFactory.getInstance().runTaskLocal(cleanTaskInfo(taskData));
                        }
                    } else if (taskData.getState() == TaskData.STATE_FAIL_PROGRAM) {
                        if (taskData.getRanTimes() <= taskConfig.getRetryTimesByProgram()) {
                            taskData = TaskFactory.getInstance().runTaskLocal(cleanTaskInfo(taskData));
                        }
                    }
                }
            }
            return taskData;
        } else {
            // 如果异常，根据任务设置，重新跑
            if (taskData.getRetryType() == TaskData.RETRY_TYPE_AUTO) {
                if (taskData.getState() > TaskData.STATE_SUCCESS) {
                    //设置任务延时
                    taskData.setTaskDelay(taskData.getRanTimes() * taskProperties.getTaskQueueRetryDelay());
                    if (taskData.getState() == TaskData.STATE_FAIL_CONFIG) {
                        // 重试判定用 <=：ranTimes 含本次执行，N 次额外重试意味着 ranTimes 可达 N+1。
                        if (taskData.getRanTimes() <= taskConfig.getRetryTimesByOverrated()) {
                            TaskFactory.getInstance().sendToQueue(cleanTaskInfo(taskData));
                        }
                    } else if (taskData.getState() == TaskData.STATE_FAIL_PARTNER) {
                        if (taskData.getRanTimes() <= taskConfig.getRetryTimesByPartner()) {
                            TaskFactory.getInstance().sendToQueue(cleanTaskInfo(taskData));
                        }
                    } else if (taskData.getState() == TaskData.STATE_FAIL_PROGRAM) {
                        if (taskData.getRanTimes() <= taskConfig.getRetryTimesByProgram()) {
                            TaskFactory.getInstance().sendToQueue(cleanTaskInfo(taskData));
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 清理任务信息。
     *
     * @param srcData
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
        String queueName = taskMetaInfoManager.getQueueNameByConfig(runnerConfig);
        SimpleMessageListenerContainer sysContainer = queueListenerMap.computeIfAbsent(queueName, key -> {
            if (runnerConfig.getState() != 1) {
                log.warn("!TaskRunner: [{}]状态为暂停，不进行注册。。。", queueName);
                return null;
            }
            if (log.isDebugEnabled()) {
                log.debug("TaskRunner: [{}]正在注册并启动监听...", queueName);
            }
            try {
                Queue queue = QueueBuilder.durable(queueName).build();
                // 声明处理
                declareManage(queue);
                // 启动任务监听器
                SimpleMessageListenerContainer container = bindMessageListenerContainer(runnerConfig, queueName);
                if (runnerConfig.getDelayType() == TaskRunnerConfig.TYPE_DELAY_ON) {
                    // 建立TTL队列，需要设置名称
                    Queue ttlQueue =
                            QueueBuilder.durable(taskMetaInfoManager.getTTLQueueName(queueName))
                                    .withArgument("x-dead-letter-exchange", queueName)
                                    .withArgument("x-dead-letter-routing-key", queueName)
                                    .build();
                    // 声明处理
                    declareManage(ttlQueue);
                }
                return container;
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
                    //只有runner的queueType才可以在线设置。对于配置了死信队列，此处消费者配置为配置死信队列消费
                    if (runnerConfig.getQueueType() == TaskRunnerConfig.TYPE_QUEUE_TASK) {
                        sysContainer.setMaxConcurrentConsumers(runnerConfig.getConsumerNum());
                        sysContainer.setConcurrentConsumers((int) Math.ceil(runnerConfig.getConsumerNum() * 0.1f));
                        sysContainer.setPrefetchCount(runnerConfig.getPrefetchNum());
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                // state<1：GROUP/PROJECT 队列共享，销毁前检查是否仍有其他活跃配置映射到同一 queueName，避免误销毁共享 listener（致同包其他 runner 消费中断）
                boolean stillInUse = taskMetaInfoManager.getRunnerConfigMap().values().stream()
                        .anyMatch(c -> queueName.equals(taskMetaInfoManager.getQueueNameByConfig(c)));
                if (!stillInUse) {
                    queueListenerMap.remove(queueName);
                    sysContainer.destroy();
                } else if (log.isDebugEnabled()) {
                    log.debug("TaskRunner[{}]仍有活跃配置共享此队列，跳过 destroy", queueName);
                }
            }
        }
    }

    /**
     * 通过 RabbitAdmin 声明队列、直连交换机及其绑定关系（三者同名，routing key = 队列名）。
     *
     * @param queue 队列定义
     */
    public void declareManage(Queue queue) {
        // 交换机
        Exchange exchange = ExchangeBuilder.directExchange(queue.getName()).durable(true).build();
        // 队列和交换机绑定
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(queue.getName()).noargs();
        // 定义队列
        rabbitAdmin.declareQueue(queue);
        // 定义交换机
        rabbitAdmin.declareExchange(exchange);
        // 队列和交换机绑定
        rabbitAdmin.declareBinding(binding);
    }

    /**
     * 绑定队列监听器容器
     *
     * @param runnerConfig runner配置信息
     * @param queueName    队列名称
     * @return SimpleMessageListenerContainer 监听器容器
     */
    public SimpleMessageListenerContainer bindMessageListenerContainer(TaskRunnerConfig runnerConfig, String queueName) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setAutoStartup(false);
        // 消费者线程虚拟化：basicConsume 长连接等待 + process 内的 IO/限速 sleep 都 park 在虚拟线程上，不占平台载体
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor(queueName);
        taskExecutor.setVirtualThreads(true);
        container.setTaskExecutor(taskExecutor);
        // 提高启动consumer速度。
        container.setStartConsumerMinInterval(100);
        container.setConsecutiveActiveTrigger(1);
        container.setStopConsumerMinInterval(20000);
        container.setConsecutiveIdleTrigger(3);
        switch (runnerConfig.getQueueType()) {
            case TaskRunnerConfig.TYPE_QUEUE_TASK:
                container.setMaxConcurrentConsumers(runnerConfig.getConsumerNum());
                container.setConcurrentConsumers((int) Math.ceil(runnerConfig.getConsumerNum() * 0.1f));
                container.setPrefetchCount(runnerConfig.getPrefetchNum());
                break;
            case TaskRunnerConfig.TYPE_QUEUE_GROUP_PRIORITY:
                container.setMaxConcurrentConsumers(taskProperties.getQueueGroupPriorityThreadMaxNum());
                container.setConcurrentConsumers(taskProperties.getQueueGroupPriorityThreadMinNum());
                container.setPrefetchCount(taskProperties.getQueueGroupPriorityPrefetchNum());
                break;
            case TaskRunnerConfig.TYPE_QUEUE_GROUP:
                container.setMaxConcurrentConsumers(taskProperties.getQueueGroupDefaultThreadMaxNum());
                container.setConcurrentConsumers(taskProperties.getQueueGroupDefaultThreadMinNum());
                container.setPrefetchCount(taskProperties.getQueueGroupDefaultPrefetchNum());
                break;
            case TaskRunnerConfig.TYPE_QUEUE_PROJECT_PRIORITY:
                container.setMaxConcurrentConsumers(taskProperties.getQueueProjectPriorityThreadMaxNum());
                container.setConcurrentConsumers(taskProperties.getQueueProjectPriorityThreadMinNum());
                container.setPrefetchCount(taskProperties.getQueueProjectPriorityPrefetchNum());
                break;
            default:
                container.setMaxConcurrentConsumers(taskProperties.getQueueProjectDefaultThreadMaxNum());
                container.setConcurrentConsumers(taskProperties.getQueueProjectDefaultThreadMinNum());
                container.setPrefetchCount(taskProperties.getQueueProjectDefaultPrefetchNum());
        }
        container.setConnectionFactory(taskConnectionFactory);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        container.setQueueNames(queueName);
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(this, "process");
        listenerAdapter.setMessageConverter(new TaskMessageConverter());
        container.setMessageListener(listenerAdapter);
        // 如果发生错误不再重新排队，我们自己catch异常处理
        container.setDefaultRequeueRejected(false);
        container.setAutoStartup(true);
        container.afterPropertiesSet();
        container.start();
        return container;
    }

    /**
     * 暂停所有队列监听容器（destroy listener + 清 map），保留底层 connectionFactory 以便恢复。
     * <p>用于主机被禁用(state=0)或降级恢复切换时停 listener 却保留连接，解除禁用后可重新注册。</p>
     */
    public void suspendAll() {
        log.info("All TaskRunner Suspended....");
        for (SimpleMessageListenerContainer container : queueListenerMap.values()) {
            try {
                // shutdown() 是彻底关闭、stop() 是优雅停止；先 shutdown 后 stop 状态会异常。
                // SimpleMessageListenerContainer 的 destroy() 按 Spring 生命周期顺序处理关闭，最为稳妥。
                container.destroy();
            } catch (Throwable e) {
                // 单个 listener destroy 失败不阻断其余 listener 停机 + clear（host-disable 是运维关键操作，须保证后续 croner/delayer 也被停）
                log.error("TaskRunner listener destroy 失败: {}", e.getMessage(), e);
            }
        }
        // 清空 map：禁用主机解除后，updateConfig 主循环 registerQueueListener 才会重新创建 listener（否则 map 残留已 destroy 的容器导致跳过）
        queueListenerMap.clear();
    }

    /**
     * 销毁所有队列监听容器（@PreDestroy 场景）：Runner 的 suspend=destroy（listener destroy 停消费，connectionFactory 由 TaskAutoConfiguration 关）。
     */
    public void destroyAll() {
        log.info("All TaskRunner Destroy....");
        suspendAll();
    }

    /**
     * 填充 runner stats 的 queueSize（队列积压数）与 consumerNum（消费者数）。
     * <p>reportHostInfo 上报前调用，按 stats.taskId 反查 configMap 得到 queueName，用 queueDeclarePassive 查询积压。</p>
     *
     * @param taskRunnerStatsList runner 统计列表（原地填充 queueSize/consumerNum）
     */
    public void fillQueueBacklog(List<TaskRunnerStats> taskRunnerStatsList) {
        if (taskRunnerStatsList == null || taskRunnerStatsList.isEmpty()) {
            return;
        }
        // 预先构建 taskId -> queueName 索引，避免对每个 stats 遍历全部 config（O(n*m)）。
        Map<Long, String> queueNameByTaskId = new HashMap<>(taskMetaInfoManager.getRunnerConfigMap().size());
        for (TaskRunnerConfig taskRunnerConfig : taskMetaInfoManager.getRunnerConfigMap().values()) {
            queueNameByTaskId.put(taskRunnerConfig.getId(), taskMetaInfoManager.getQueueNameByConfig(taskRunnerConfig));
        }
        //查询缓存，应对公用队列的情况，防止重复查询。key:队列名。
        Map<String, AMQP.Queue.DeclareOk> queueInfoMap = new HashMap<>();
        for (TaskRunnerStats stats : taskRunnerStatsList) {
            String queueName = queueNameByTaskId.get(stats.getTaskId());
            if (queueName == null) {
                continue;
            }
            AMQP.Queue.DeclareOk declareOk;
            try {
                declareOk = queueInfoMap.computeIfAbsent(queueName,
                        (Key) -> rabbitAdmin.getRabbitTemplate().execute(channel -> channel.queueDeclarePassive(queueName)));
            } catch (Exception e) {
                // 单个队列查询失败（队列不存在/MQ 瞬时故障）不阻断其他队列 + 后续 hostInfo 上报/hostDisabled 检查
                log.warn("查询队列积压失败,queueName={}: {}", queueName, e.getMessage());
                continue;
            }
            stats.setQueueSize(declareOk.getMessageCount());
            stats.setConsumerNum(declareOk.getConsumerCount());
        }
    }

}
