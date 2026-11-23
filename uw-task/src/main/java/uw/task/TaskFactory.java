package uw.task;

import java.util.concurrent.CompletableFuture;
import uw.task.container.TaskDelayTransferContainer;
import uw.task.container.TaskSendCenter;
import uw.common.util.LimitedVirtualThreadExecutor;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;

import uw.task.rabbit.TaskAmqpOps;


import uw.common.util.SystemClock;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.util.TaskSequence;
import uw.task.conf.UwTaskProperties;
import uw.task.constant.TaskConstants;
import uw.task.converter.TaskMessageConverter;
import uw.task.container.TaskRunnerLocalExecutor;
import uw.task.container.TaskRunnerContainer;
import uw.task.exception.TaskRuntimeException;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 任务执行器。 通过调用此类，可以实现队列执行，RPC调用，本地调用等功能。
 *
 * <p>261120 分层整改后本类为业务门面 + 本地/RPC 执行编排：队列投递（含延时路由）委托
 * {@link TaskSendCenter}（容器不再静态回调本类，getInstance 仅供业务方使用）；RPC/本地执行仍在本类。</p>
 *
 * @author axeon
 */
public class TaskFactory {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(TaskFactory.class);
    /**
     * 最大重试次数。
     */
    private static final int MAX_RETRY_TIMES = 20;
    /**
     * 全局唯一实例（业务方门面；框架内部零静态依赖）。
     */
    private static volatile TaskFactory INSTANCE;
    /**
     * 协议无关的 AMQP 操作面（RPC 通道用）。
     */
    private final TaskAmqpOps taskAmqpOps;

    /**
     * 任务投递中心（全路由：延时分流 + 即时投递）。
     */
    private final TaskSendCenter taskSendCenter;
    /**
     * 任务meta信息管理器。
     */
    private final TaskMetaInfoManager taskMetaInfoManager;
    /**
     * 用于本地执行任务的taskConsumer。
     */
    private final TaskRunnerContainer taskRunnerContainer;
    /**
     * 延时任务 zset 搬运器（延时积压观测入口）。
     */
    private final TaskDelayTransferContainer taskDelayTransferContainer;
    /**
     * 监听管理器（客户端扩展点：经 {@link #getListenerManager()} 注册 runner/croner/投递监听器）。
     */
    private final TaskListenerManager taskListenerManager;

    /**
     * rpc异步执行器（VT+并发上限，快速模式无队列缓冲）：满时 CallerRuns 由提交线程执行（背压）。
     */
    private final LimitedVirtualThreadExecutor taskRpcService;
    /**
     * 任务本地队列执行器（VT+并发上限+有界缓冲）：缓冲也满时溢出到 MQ（SendToQueuePolicy 背压）。
     */
    private final LimitedVirtualThreadExecutor taskQueueService;

    /**
     * 构造 TaskFactory 并初始化两个内置执行器，同时设置全局单例。
     * <p>虚拟线程 + 并发上限（261120 VT 化）：taskRpcService 快速模式满时 CallerRuns；taskQueueService 有界缓冲
     * （容量=并发上限）满时经 {@code SendToQueuePolicy} 溢出到 MQ。上限值即背压闸门，与线程成本无关。</p>
     *
     * @param taskProperties      任务全局配置（提供线程数上下限等参数）
     * @param taskAmqpOps         协议无关 AMQP 操作面（RPC 通道）
     * @param taskSendCenter      任务投递中心（全路由 sendToQueue）
     * @param taskRunnerContainer 队列任务执行容器（本地/RPC 模式实际执行入口）
     * @param taskMetaInfoManager 任务元信息管理器（runner 实例与配置索引）
     * @param taskDelayTransferContainer 延时任务 zset 搬运器
     * @param taskListenerManager        监听管理器（客户端监听器注册入口）
     */
    public TaskFactory(UwTaskProperties taskProperties, TaskAmqpOps taskAmqpOps, TaskSendCenter taskSendCenter, TaskRunnerContainer taskRunnerContainer,
                       TaskMetaInfoManager taskMetaInfoManager, TaskDelayTransferContainer taskDelayTransferContainer, TaskListenerManager taskListenerManager) {
        this.taskAmqpOps = taskAmqpOps;
        this.taskSendCenter = taskSendCenter;
        this.taskRunnerContainer = taskRunnerContainer;
        this.taskMetaInfoManager = taskMetaInfoManager;
        this.taskDelayTransferContainer = taskDelayTransferContainer;
        this.taskListenerManager = taskListenerManager;
        taskRpcService = new LimitedVirtualThreadExecutor("uw.task.rpc-", taskProperties.getTaskRpcThreadMaxNum(),
                new LimitedVirtualThreadExecutor.CallerRunsPolicy());
        taskQueueService = new LimitedVirtualThreadExecutor("uw.task.queue-", taskProperties.getTaskLocalThreadMaxNum(),
                taskProperties.getTaskLocalThreadMaxNum(), new TaskRunnerLocalExecutor.SendToQueuePolicy());
        INSTANCE = this;
    }

    /**
     * 返回全局唯一实例（业务方门面；框架内部零依赖）。
     *
     * @return 全局 TaskFactory 实例
     */
    public static TaskFactory getInstance() {
        return INSTANCE;
    }


    /**
     * AUTO_RPC 本地降级判定：本机有匹配 runner 且 target 匹配时置 LOCAL（三执行入口共用）。
     */
    private void resolveAutoRpcToLocal(TaskData<?, ?> taskData) {
        if (taskData.getRunType() == TaskData.RUN_TYPE_AUTO_RPC && taskMetaInfoManager.checkRunnerRunLocal(taskData)) {
            taskData.setRunType(TaskData.RUN_TYPE_LOCAL);
        }
    }

    /**
     * 将任务异步投递到 RabbitMQ 队列，由具备匹配 runner 的远端主机消费。
     * <p>
     * 完全异步、无返回值。若任务带 taskDelay，自动路由：支持原生延迟（1.0+4.4+quorum）以消息级延迟投递，
     * 否则经 Redis zset 搬运（到期自动转投）。实现见 {@link TaskSendCenter#sendToQueue}。
     * <p>
     * <b>阻塞风险</b>：即时投递遇资源类异常会在线程内退避重试（最多 {@value #MAX_RETRY_TIMES} 次，最坏约 2 分钟）；
     * 延时投递（taskDelay&gt;0）走 MQ 原生延迟或 zset 写入，无重试循环。避免在 Web 请求线程等敏感线程直接调用（见 README）。
     *
     * @param taskData 执行任务对象（taskClass/taskParam 必须已设置）
     * @throws TaskRuntimeException 投递重试全部失败时抛出
     */
    public void sendToQueue(final TaskData<?, ?> taskData) {
        taskSendCenter.sendToQueue(taskData);
    }

    /**
     * 本地运行队列。
     * 为了优化实时性高，且频繁的队列任务，可以优先在本地执行，减少mq压力，提升运行效率。
     * 当本地执行线程池满的时候，会直接转到队列执行。
     * 如果任务有延迟，则直接打入队列中。
     *
     * @param taskData 任务数据
     */
    public void runQueue(final TaskData<?, ?> taskData) {
        if (taskData.getTaskDelay() > 0) {
            this.sendToQueue(taskData);
        } else {
            taskQueueService.submit(new TaskRunnerLocalExecutor(this, taskMetaInfoManager, taskData));
        }
    }

    /**
     * 同步执行任务。
     * <p>
     * 根据 runType 决定执行方式：{@link TaskData#RUN_TYPE_AUTO_RPC} 时自动判定本机有无 runner——
     * 有则本地执行，无则走全局 RPC；{@link TaskData#RUN_TYPE_LOCAL} 强制本地执行；
     * 其他走全局 RPC（{@code sendAndReceive}，默认 180s 超时）。
     * <p>
     * <b>阻塞</b>：远程 RPC 在调用线程同步等待结果，本地执行也会阻塞至任务完成。
     * <b>不可变性</b>：调用方传入的 taskData 对象会被框架写入 id/queueDate/runType 等运行期字段，请勿复用同一对象。
     *
     * @param taskData 任务数据
     * @param <TP>     任务参数类型
     * @param <RD>     返回结果类型
     * @return 携带执行结果与状态的 taskData（同传入对象）
     * @throws TaskRuntimeException RPC 超时或重试耗尽时抛出
     * @see #doRpcCall 幂等性要求——远程 RPC 任务的服务端实现必须幂等（资源类异常重试可能二次执行）
     */
    @SuppressWarnings("unchecked")
    public <TP, RD> TaskData<TP, RD> runTask(final TaskData<TP, RD> taskData) {
        if (taskData.getTaskDelay() > 0) {
            // 延时任务：等同 sendToQueue（延时后经队列执行，同步 RPC 语义不成立）
            sendToQueue(taskData);
            return taskData;
        }
        taskData.setId(TaskSequence.nextId(TaskConstants.KEY_RUNNER_LOG));
        taskData.setQueueDate(SystemClock.nowDate());
        resolveAutoRpcToLocal(taskData);
        if (taskData.getRunType() == TaskData.RUN_TYPE_LOCAL) {
            taskRunnerContainer.process(taskData);
            return taskData;
        } else {
            taskData.setRunType(TaskData.RUN_TYPE_GLOBAL_RPC);
            // 全局运行模式
            Message message = buildRpcMessage(taskData);
            String queue = taskMetaInfoManager.getFitQueue(taskData);
            return doRpcCall(queue, message);
        }
    }

    /**
     * 强制本地同步执行任务，不经 MQ。带 taskDelay 时在执行线程内延时（本地专属任务的延时/重试退避语义）。
     * <p>
     * {@link TaskData#RUN_TYPE_AUTO_RPC} 且本机有匹配 runner 时降级为本地执行；否则抛
     * {@link TaskRuntimeException}（不回退到队列，区别于本地队列优先的 {@link #runQueue}）。
     * 同样会阻塞调用线程至任务完成；调用方传入的 taskData 会被写入运行期字段，请勿复用。
     *
     * @param taskData 任务数据
     * @param <TP>     任务参数类型
     * @param <RD>     返回结果类型
     * @return 携带执行结果与状态的 taskData（同传入对象）
     * @throws TaskRuntimeException 任务无法在本地执行（本机无匹配 runner 或非 LOCAL/AUTO_RPC 类型）时抛出
     */
    @SuppressWarnings("unchecked")
    public <TP, RD> TaskData<TP, RD> runTaskLocal(final TaskData<TP, RD> taskData) {
        taskData.setId(TaskSequence.nextId(TaskConstants.KEY_RUNNER_LOG));
        taskData.setQueueDate(SystemClock.nowDate());
        resolveAutoRpcToLocal(taskData);
        if (taskData.getRunType() == TaskData.RUN_TYPE_LOCAL) {
            taskRunnerContainer.process(taskData);
            return taskData;
        } else {
            throw new TaskRuntimeException(taskData.getTaskClass() + " is not a local task! ");
        }
    }

    /**
     * 异步执行任务，返回 {@link Future}。
     * <p>
     * 本地或远程执行的判定逻辑同 {@link #runTask}，区别在于实际执行提交到 RPC 线程池异步进行，
     * 调用方可通过 {@code future.get()} 获取结果。远程模式下每个 future 占用一个线程等待 RPC 返回，
     * <b>大并发下需注意线程数与限速叠加可能导致线程池耗尽</b>，谨慎使用。
     * 调用方传入的 taskData 会被写入运行期字段，请勿复用。
     *
     * @param taskData 任务数据
     * @param <TP>     任务参数类型
     * @param <RD>     返回结果类型
     * @return 异步执行结果的 Future
     */
    @SuppressWarnings("unchecked")
    public <TP, RD> Future<TaskData<TP, RD>> runTaskAsync(final TaskData<TP, RD> taskData) {
        if (taskData.getTaskDelay() > 0) {
            // 延时任务：等同 sendToQueue（延时后经队列执行）
            sendToQueue(taskData);
            return CompletableFuture.completedFuture(taskData);
        }
        taskData.setId(TaskSequence.nextId(TaskConstants.KEY_RUNNER_LOG));
        taskData.setQueueDate(SystemClock.nowDate());

        resolveAutoRpcToLocal(taskData);
        if (taskData.getRunType() == TaskData.RUN_TYPE_LOCAL) {
            // 启动本地运行模式。
            CompletableFuture<TaskData<TP, RD>> future = new CompletableFuture<>();
            taskRpcService.submit(() -> {
                try {
                    taskRunnerContainer.process(taskData);
                    future.complete(taskData);
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            });
            return future;
        } else {
            // 全局运行模式
            taskData.setRunType(TaskData.RUN_TYPE_GLOBAL_RPC);
            Message message = buildRpcMessage(taskData);
            String queue = taskMetaInfoManager.getFitQueue(taskData);
            CompletableFuture<TaskData<TP, RD>> future = new CompletableFuture<>();
            taskRpcService.submit(() -> {
                try {
                    future.complete(doRpcCall(queue, message));
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            });
            return future;
        }
    }


    /**
     * 查询指定队列的积压消息数与消费者数。
     *
     * @param queueName 队列名
     * @return 长度为 2 的数组：下标 0 为消息数量，下标 1 为消费者数量
     */
    public int[] getQueueInfo(String queueName) {
        int[] info = taskAmqpOps.getQueueInfo(queueName);
        return info == null ? new int[]{0, 0} : info;
    }

    /**
     * 清空指定队列中的全部消息（危险操作，仅供运维使用）。
     *
     * @param queueName 队列名
     * @return 被清除的消息数量
     */
    public int purgeQueue(String queueName) {
        return taskAmqpOps.purgeQueue(queueName);
    }

    /**
     * 构造 RPC 调用消息：高优先级(10)、非持久化、180s 过期。
     *
     * @param taskData 任务数据
     * @return AMQP 消息
     */
    private Message buildRpcMessage(TaskData<?, ?> taskData) {
        //加入优先级信息。
        MessageProperties messageProperties = new MessageProperties();
        // 1.0 协议 RPC reply correlation 依赖 messageId（模板强校验）
        messageProperties.setMessageId(String.valueOf(taskData.getId()));
        messageProperties.setPriority(10);
        messageProperties.setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
        messageProperties.setExpiration("180000");
        return new TaskMessageConverter().toMessage(taskData, messageProperties);
    }

    /**
     * 执行全局 RPC 调用与退避重试、超时判定。
     * <p><b>幂等性要求</b>：RPC 任务（GLOBAL_RPC）的服务端实现必须幂等——重试窗口内消息可能已被服务端
     * 消费执行（发送成功但响应丢失/连接异常），重试会导致二次执行，框架不防重。</p>
     * <p><b>重试策略</b>：异常一律尽量重试（最多 {@value #MAX_RETRY_TIMES} 次）——同步 RPC 下服务端业务失败
     * 经响应消息回传（不走异常路径），异常路径基本均为传输类；超时(对方无响应 retMessage==null)立即失败
     * 不重试（重试只会阻塞调用方且可能重复执行）；InterruptedException 中断后立即返回不重试。</p>
     *
     * @param queue   目标队列
     * @param message 已构造的 RPC 消息
     * @param <TP>    任务参数类型
     * @param <RD>    返回结果类型
     * @return 携带执行结果的 taskData
     * @throws TaskRuntimeException RPC 超时或重试耗尽时抛出
     */
    @SuppressWarnings("unchecked")
    private <TP, RD> TaskData<TP, RD> doRpcCall(String queue, Message message) {
        // RPC 调用异常一律尽量重试；RPC 超时(retMessage==null)不重试（重试只阻塞调用方+可能重复执行）。
        String rpcTimeoutQueue = null;
        Exception lastError = null;
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            try {
                if (i > 0) {
                    Thread.sleep(i * 50);
                }
                Message retMessage = taskAmqpOps.sendAndReceive(queue, message, 180000);
                if (retMessage != null) {
                    return (TaskData<TP, RD>) new TaskMessageConverter().fromMessage(retMessage);
                }
                // RPC 超时（对方没回）：立即失败，不重试
                rpcTimeoutQueue = queue;
                break;
            } catch (Exception e) {
                lastError = e;
                log.error(e.getMessage(), e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (rpcTimeoutQueue != null) {
            throw new TaskRuntimeException("RPC call timed out: " + rpcTimeoutQueue);
        }
        throw new TaskRuntimeException("RPC call failed after retries: " + queue + (lastError != null ? ", last=" + lastError.getMessage() : ""));
    }

    /**
     * 容器销毁时优雅关闭 RPC 执行器与本地队列执行器。
     * <p>三段式关闭：先 shutdown 阻止新任务，最多等待 5 秒让在途任务完成，超时后 shutdownNow 强制中断。</p>
     */
    @PreDestroy
    public void shutdown() {
        shutdownQuietly(taskRpcService);
        shutdownQuietly(taskQueueService);
    }

    /**
     * 优雅关闭执行器：shutdown → awaitTermination(5s) → shutdownNow。
     * <p>三段式关闭保证在途任务尽量自然结束，超时才强制中断；中断后恢复线程中断状态。</p>
     *
     * @param pool 待关闭的执行器，为 null 时直接返回
     */
    private static void shutdownQuietly(LimitedVirtualThreadExecutor pool) {
        if (pool == null) {
            return;
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
