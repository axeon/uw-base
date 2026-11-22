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
 * <p>261120 分层整改（合并原 TaskMessageSender，TaskSender 接口删除——单实现接口无意义）：
 * {@code TaskFactory}（门面委托）与 {@code TaskRunnerContainer}（GLOBAL 重试重投）依赖本类；
 * {@code TaskDelayTransferContainer} 到期转投依赖 {@link #sendImmediate}——与 sendToQueue 的 zset 分支
 * 形成 Transfer↔本类 互指，装配侧以 @Lazy 破构造环（Spring 惯用法），运行期无环。</p>
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
     * @param taskAmqpOps                协议无关 AMQP 操作面（原生延迟能力由其 supportsNativeDelay 自述）
     * @param taskMetaInfoManager        任务元信息管理器
     * @param taskDelayTransferContainer 延时任务 zset 搬运器
     * @param taskListenerManager        监听管理器（投递侧回调，可为 null）
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
     * <p>延时任务统一路由（261120 定案"一切全自动化"）：delay&gt;0 时二选一，能力自述单一事实源——
     * ① ops 支持 MQ 原生延迟（1.0+4.4）且目标队列为 quorum → 消息级延迟（x-opt-delivery-time）；
     * ② 否则自动转 Redis zset 搬运器（到期自动转投本队列，delay 清零）。</p>
     *
     * @param taskData 执行任务对象（taskClass/taskParam 必须已设置）
     */
    public void sendToQueue(final TaskData<?, ?> taskData) {
        if (taskData.getTaskDelay() > 0) {
            String delayQueue = taskMetaInfoManager.getFitQueue(taskData);
            if (taskAmqpOps.supportsNativeDelay() && isQuorumQueue(taskData)) {
                Message delayMessage = buildDelayMessage(taskData);
                taskAmqpOps.send(delayQueue, delayMessage, taskData.getTaskDelay());
                return;
            }
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
     * <p>任务 ID 与入队时间由本方法写入；runType 在构造消息期间临时置 GLOBAL（消费端依赖此值走限速/重试链路），
     * 结束后恢复不污染调用方对象。投递可能因连接/channel 资源类异常瞬时失败，退避重试（最多 {@value #MAX_RETRY_TIMES} 次，
     * 最坏约 2 分钟）；中间轮次仅记 WARN 计数（防全栈刷屏），最终失败记 ERROR 全栈、回调
     * {@link TaskSendListener#onSendFail} 后抛 {@link TaskRuntimeException}。</p>
     *
     * @param taskData 执行任务对象（taskClass/taskParam 必须已设置）
     */
    void sendImmediate(final TaskData<?, ?> taskData) {
        Message message = buildTaskQueueMessage(taskData);
        String queue = message.getMessageProperties().getConsumerQueue();
        Exception lastError = null;
        boolean interrupted = false;
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            try {
                if (i > 0) {
                    Thread.sleep(i * 100);
                }
                taskAmqpOps.send(queue, message, 0);
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
     * 延时任务 zset 提交监听回调（异常吞掉仅记录）。
     */
    private void notifyDelaySubmit(TaskData<?, ?> taskData, String delayQueue) {
        if (taskListenerManager == null) {
            return;
        }
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
     */
    private void notifySendFail(TaskData<?, ?> taskData, String queue, int attempts) {
        if (taskListenerManager == null) {
            return;
        }
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
     */
    private boolean isQuorumQueue(TaskData<?, ?> taskData) {
        try {
            TaskRunnerConfig config = taskMetaInfoManager.getRunnerConfigByData(taskData);
            return config != null && config.getQueueType() == TaskRunnerConfig.QUEUE_TYPE_QUORUM;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 构造 MQ 原生延迟消息：写入 id/queueDate，runType 临时置 GLOBAL 后恢复（消费端限速/重试链路依赖 GLOBAL）。
     *
     * @param taskData 用于任务执行传值对象
     * @return 延迟投递的消息
     */
    private Message buildDelayMessage(final TaskData<?, ?> taskData) {
        taskData.setId(TaskSequence.nextId(TaskConstants.KEY_RUNNER_LOG));
        taskData.setQueueDate(SystemClock.nowDate());
        int originRunType = taskData.getRunType();
        taskData.setRunType(TaskData.RUN_TYPE_GLOBAL);
        try {
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setConsumerQueue(taskMetaInfoManager.getFitQueue(taskData));
            return new TaskMessageConverter().toMessage(taskData, messageProperties);
        } finally {
            taskData.setRunType(originRunType);
        }
    }

    /**
     * 构造队列消息：写入 id/queueDate，runType 临时置 GLOBAL 后恢复（消费端限速/重试链路依赖 GLOBAL）。
     *
     * @param taskData 用于任务执行传值对象
     * @return Message 发送的队列信息
     */
    private Message buildTaskQueueMessage(final TaskData<?, ?> taskData) {
        taskData.setId(TaskSequence.nextId(TaskConstants.KEY_RUNNER_LOG));
        taskData.setQueueDate(SystemClock.nowDate());
        // 发队列的消息体必须是 GLOBAL，消费端 process 依赖此值判定走限速/重试链路。
        // 调用方传入的 taskData 可能被复用（README 强调"taskData 对象不可改变"），故仅在此方法作用域内
        // 临时改为 GLOBAL，结束时恢复，避免污染调用方持有的对象。
        int originRunType = taskData.getRunType();
        taskData.setRunType(TaskData.RUN_TYPE_GLOBAL);
        try {
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setConsumerQueue(taskMetaInfoManager.getFitQueue(taskData));
            // 持久化语义（用户定案 261120）：classic 队列消息默认 NON_PERSISTENT 不写盘（驻内存，broker 崩溃丢失，
            // 换取内存级吞吐）；quorum 队列恒持久化（Raft WAL 必写盘，deliveryMode 标志被 RabbitMQ 忽略）。
            TaskRunnerConfig sendConfig = taskMetaInfoManager.getRunnerConfigByData(taskData);
            if (sendConfig != null && sendConfig.getQueueType() == TaskRunnerConfig.QUEUE_TYPE_CLASSIC) {
                messageProperties.setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
            }
            return new TaskMessageConverter().toMessage(taskData, messageProperties);
        } finally {
            taskData.setRunType(originRunType);
        }
    }
}
