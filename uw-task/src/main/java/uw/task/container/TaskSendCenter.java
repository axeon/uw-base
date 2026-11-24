package uw.task.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import uw.common.util.SystemClock;
import uw.task.TaskData;
import uw.task.TaskListenerManager;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.constant.TaskConstants;
import uw.task.converter.TaskMessageConverter;
import uw.task.entity.TaskRunnerConfig;
import uw.task.exception.TaskRuntimeException;
import uw.task.listener.TaskSendListener;
import uw.task.rabbit.TaskAmqpOps;
import uw.task.util.TaskSequence;

/**
 * 任务投递中心：sendToQueue 全路由（delay&gt;0 自动分流 MQ 原生延迟 / Redis zset 搬运）+ 即时投递。
 *
 * <p>{@link uw.task.TaskFactory}（门面委托）与 {@code TaskRunnerContainer}（GLOBAL 重试重投）依赖本类；
 * {@link TaskDelayTransferContainer} 到期转投回调本类 sendToQueue——与本类 sendToQueue 的 zset 分支形成
 * Transfer↔本类互指，装配期先建 Transfer 再建本类、最后 {@code bindSendCenter} 回填破环。</p>
 *
 * @author axeon
 */
public class TaskSendCenter {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(TaskSendCenter.class);

    /**
     * 最大重试次数。
     */
    private static final int MAX_RETRY_TIMES = 20;

    /**
     * 协议无关的 AMQP 操作面。
     */
    private final TaskAmqpOps taskAmqpOps;

    /**
     * 任务元信息管理器（队列路由与配置查询）。
     */
    private final TaskMetaInfoManager taskMetaInfoManager;

    /**
     * 延时任务 zset 搬运器（MQ 原生延迟不可用时自动路由到此）。
     */
    private final TaskDelayTransferContainer taskDelayTransferContainer;

    /**
     * 监听管理器（投递侧回调）。
     */
    private final TaskListenerManager taskListenerManager;

    /**
     * 构建任务投递中心。
     *
     * @param taskAmqpOps                协议无关 AMQP 操作面（原生延速能力由其 supportsNativeDelay 自述）
     *
     * @param taskMetaInfoManager        任务元信息管理器
     * @param taskDelayTransferContainer 延时任务 zset 搬运器
     *
     * @param taskListenerManager        监听管理器（投递侧回调）
     */
    public TaskSendCenter(TaskAmqpOps taskAmqpOps, TaskMetaInfoManager taskMetaInfoManager,
                          TaskDelayTransferContainer taskDelayTransferContainer, TaskListenerManager taskListenerManager) {
        this.taskAmqpOps = taskAmqpOps;
        this.taskMetaInfoManager = taskMetaInfoManager;
        this.taskDelayTransferContainer = taskDelayTransferContainer;
        this.taskListenerManager = taskListenerManager;
    }

    /**
     * 将任务异步投递到 RabbitMQ 队列，由具备匹配 runner 的远端主机消费。
     * <p>延时任务统一路由：delay&gt;0 时二选一，能力自述单一事实源——
     * ① ops 支持 MQ 原生延迟（RabbitMQ ≥4.4，两种协议）且目标队列为 quorum → 消息级延迟（x-opt-delivery-time）；
     * ② 否则自动转 Redis zset 搬运器（到期自动转投本队列，delay 清零）。</p>
     *
     * @param taskData 执行任务对象（taskClass/taskParam 必须已设置）
     */
    public void sendToQueue(final TaskData<?, ?> taskData) {
        // 仅定时任务档（未配置 MQ）：入队与延时提交不可用，入口处快速失败（不等重试、不写 zset）
        TaskAmqpOps.requireConfigured(taskAmqpOps);
        if (taskData.getTaskDelay() > 0) {
            String delayQueue = taskMetaInfoManager.getFitQueue(taskData);
            if (taskAmqpOps.supportsNativeDelay() && isQuorumQueue(taskData)) {
                publish(taskData, delayQueue, buildQueueMessage(taskData), taskData.getTaskDelay());
                return;
            }
            // id/queueDate 先写入（score=queueDate+taskDelay）；到期转投时 sendImmediate 会重新取号与入队时间
            taskData.setId(TaskSequence.nextId(TaskConstants.KEY_RUNNER_LOG));
            taskData.setQueueDate(SystemClock.nowDate());
            taskDelayTransferContainer.submit(taskData);
            // 投递侧监听回调（异常吞掉仅记录）
            notifyDelaySubmit(taskData, delayQueue);
            return;
        }
        sendImmediate(taskData);
    }

    /**
     * 立即投递任务到 MQ 队列（不经延时路由；zset 搬运到期转投复用本入口）。
     * <p>任务 ID 与入队时间在构造消息时写入；runType 在构造消息期间临时置 GLOBAL（消费端依赖此值走限速/重试链路），
     * 结束后恢复不污染调用方对象。</p>
     *
     * @param taskData 执行任务对象（taskClass/taskParam 必须已设置）
     */
    void sendImmediate(final TaskData<?, ?> taskData) {
        Message message = buildQueueMessage(taskData);
        publish(taskData, message.getMessageProperties().getConsumerQueue(), message, 0);
    }

    /**
     * 从头发布（第 0 次尝试），见 {@link #attempt}。
     *
     * @param taskData 任务数据
     * @param queue 目标队列
     *
     * @param message 消息
     * @param delayMillis 延时毫秒（0=即时）
     */
    private void publish(TaskData<?, ?> taskData, String queue, Message message, long delayMillis) {
        attempt(taskData, queue, message, delayMillis, 0);
    }

    /**
     * 带重试的发布：同步失败（091 send 抛异常 / 1.0 建连等失败）在当前线程内退避后重试；异步失败
     * （1.0 发布后 broker 拒收）由客户端线程回调，转交虚拟线程走同一套退避重试。
     * <p>重试上限 {@value #MAX_RETRY_TIMES} 次，退避累计约 19 秒；中间轮次仅记 WARN 计数（防全栈刷屏），
     * 最终失败记 ERROR、回调 {@link TaskSendListener#onSendFail}。同步路径最终失败向调用方抛
     * {@link TaskRuntimeException}；异步路径调用方早已返回，失败只体现在日志与监听器回调。</p>
     *
     * @param taskData    任务数据
     *
     * @param queue       目标队列
     * @param message     已构造的消息
     *
     * @param delayMillis 延时毫秒（0=即时）
     * @param from        起始尝试序号（从 0 起；异步重发时为上次失败序号+1）
     */
    private void attempt(TaskData<?, ?> taskData, String queue, Message message, long delayMillis, int from) {
        Exception lastError = null;
        boolean interrupted = false;
        for (int i = from; i < MAX_RETRY_TIMES; i++) {
            final int failed = i;
            try {
                if (i > from) {
                    Thread.sleep(i * 100L);
                }
                taskAmqpOps.send(queue, message, delayMillis, e -> {
                    // 回调在客户端 IO 线程上：只转交，退避与重发在虚拟线程里做
                    Thread.ofVirtual().name("uw-task-send-retry-", failed).start(() -> retryAfterAsyncFail(taskData, queue, message, delayMillis, e, failed));
                });
                return;
            } catch (Exception e) {
                lastError = e;
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    interrupted = true;
                    break;
                }
                // 中间轮次 WARN 计数防刷屏；全栈只在最终失败时记一次
                log.warn("sendToQueue 重试[{}/{}] taskClass={}, queue={}: {}", i + 1, MAX_RETRY_TIMES, taskData.getTaskClass(), queue, e.getMessage());
            }
        }
        // 中断=停机场景退出重试；耗尽=资源持续不可用。两路都回调监听并抛出，由调用方决策
        log.error("sendToQueue {} taskClass={}, queue={}", interrupted ? "被中断退出重试" : "重试耗尽(" + MAX_RETRY_TIMES + "次)", taskData.getTaskClass(), queue, lastError);
        notifySendFail(taskData, queue, interrupted ? 0 : MAX_RETRY_TIMES);
        throw new TaskRuntimeException("sendToQueue " + (interrupted ? "interrupted" : "failed after " + MAX_RETRY_TIMES + " retries")
                + ": taskClass=" + taskData.getTaskClass() + ", queue=" + queue);
    }

    /**
     * 异步发布失败（broker 拒收）后的退避重发；次数用尽则记 ERROR 并回调监听器。
     *
     * @param taskData    任务数据
     *
     * @param queue       目标队列
     * @param message     已构造的消息
     *
     * @param delayMillis 延时毫秒（0=即时）
     * @param cause       客户端上报的拒收原因
     *
     * @param failed      刚失败的是第几次尝试（从 0 起）
     */
    private void retryAfterAsyncFail(TaskData<?, ?> taskData, String queue, Message message, long delayMillis, Exception cause, int failed) {
        if (failed + 1 >= MAX_RETRY_TIMES) {
            log.error("sendToQueue 重试耗尽({}次) taskClass={}, queue={}", MAX_RETRY_TIMES, taskData.getTaskClass(), queue, cause);
            notifySendFail(taskData, queue, MAX_RETRY_TIMES);
            return;
        }
        log.warn("sendToQueue 异步失败重试[{}/{}] taskClass={}, queue={}: {}", failed + 1, MAX_RETRY_TIMES, taskData.getTaskClass(), queue, cause.getMessage());
        try {
            Thread.sleep((failed + 1) * 100L);
            attempt(taskData, queue, message, delayMillis, failed + 1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // attempt 内部已完成最终失败处理（日志+监听器回调），此处不重复
            log.debug("sendToQueue 异步重发终止 taskClass={}, queue={}", taskData.getTaskClass(), queue, e);
        }
    }

    /**
     * 延时任务 zset 提交监听回调（异常吞掉仅记录）。
     *
     * @param taskData 任务数据
     * @param delayQueue 到期转投的目标队列名
     */
    private void notifyDelaySubmit(TaskData<?, ?> taskData, String delayQueue) {
        for (TaskSendListener listener : taskListenerManager.getSendListenerList()) {
            try {
                listener.onDelaySubmit(taskData, delayQueue);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 投递最终失败监听回调（异常吞掉仅记录）。
     *
     * @param taskData 任务数据
     * @param queue 目标队列
     *
     * @param attempts 已尝试次数
     */
    private void notifySendFail(TaskData<?, ?> taskData, String queue, int attempts) {
        for (TaskSendListener listener : taskListenerManager.getSendListenerList()) {
            try {
                listener.onSendFail(taskData, queue, attempts);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 目标任务队列是否为 quorum 形态。延时任务走 MQ 原生延迟的判定条件
     * （x-opt-delivery-time 仅 quorum 队列支持），与声明端 TaskRunnerContainer.registerQueueListener 同源配置。
     *
     * @param taskData 任务数据
     *
     * @return 目标队列为 quorum 返回 true
     */
    private boolean isQuorumQueue(TaskData<?, ?> taskData) {
        TaskRunnerConfig config = taskMetaInfoManager.findRunnerConfig(taskData);
        return config != null && config.getQueueType() == TaskRunnerConfig.QUEUE_TYPE_QUORUM;
    }

    /**
     * 构造队列消息（即时投递与 MQ 原生延迟共用）：写入 id/queueDate 与目标队列（consumerQueue）。
     * <p>runType 临时置 GLOBAL 后恢复（消费端限速/重试链路依赖 GLOBAL，调用方对象不被污染）。
     * <p>持久化语义：classic 队列消息默认 NON_PERSISTENT 不写盘（驻内存，broker 崩溃丢失，换取内存级吞吐）；
     * quorum 队列恒持久化（Raft WAL 必写盘，deliveryMode 标志被 RabbitMQ 忽略），保持默认 PERSISTENT。</p>
     *
     * @param taskData 任务数据
     *
     * @return 待投递的消息
     */
    private Message buildQueueMessage(TaskData<?, ?> taskData) {
        taskData.setId(TaskSequence.nextId(TaskConstants.KEY_RUNNER_LOG));
        taskData.setQueueDate(SystemClock.nowDate());
        int originRunType = taskData.getRunType();
        taskData.setRunType(TaskData.RUN_TYPE_GLOBAL);
        try {
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setConsumerQueue(taskMetaInfoManager.getFitQueue(taskData));
            TaskRunnerConfig config = taskMetaInfoManager.findRunnerConfig(taskData);
            if (config != null && config.getQueueType() == TaskRunnerConfig.QUEUE_TYPE_CLASSIC) {
                messageProperties.setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
            }
            return new TaskMessageConverter().toMessage(taskData, messageProperties);
        } finally {
            taskData.setRunType(originRunType);
        }
    }
}
