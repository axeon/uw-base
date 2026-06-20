package uw.task;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.rabbitmq.client.AMQP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uw.common.util.SystemClock;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.conf.TaskProperties;
import uw.task.container.TaskQueueLocalExecutor;
import uw.task.container.TaskRunnerContainer;
import uw.task.exception.TaskRuntimeException;
import uw.task.util.TaskSequenceManager;

import java.util.concurrent.*;

/**
 * 任务执行器。 通过调用此类，可以实现队列执行，RPC调用，本地调用等功能。
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
     * 全局唯一实例。
     */
    private static volatile TaskFactory INSTANCE;
    /**
     * rabbitTemplate模板.
     */
    private final RabbitTemplate rabbitTemplate;
    /**
     * 全局sequence序列，主要用于taskLog日志。
     */
    private final TaskSequenceManager taskSequenceManager;
    /**
     * 用于本地执行任务的taskConsumer。
     */
    private final TaskRunnerContainer taskRunnerContainer;
    /**
     * 任务meta信息管理器。
     */
    private final TaskMetaInfoManager taskMetaInfoManager;
    /**
     * rpc异步调用线程池
     */
    private final ExecutorService taskRpcService;
    /**
     * 任务本地运行线程池。
     */
    private final ExecutorService taskQueueService;

    public TaskFactory(TaskProperties taskProperties, RabbitTemplate rabbitTemplate, TaskRunnerContainer taskRunnerContainer, TaskSequenceManager taskSequenceManager,
                       TaskMetaInfoManager taskMetaInfoManager) {
        this.rabbitTemplate = rabbitTemplate;
        this.taskRunnerContainer = taskRunnerContainer;
        this.taskSequenceManager = taskSequenceManager;
        this.taskMetaInfoManager = taskMetaInfoManager;
        taskRpcService = new ThreadPoolExecutor(taskProperties.getTaskRpcThreadMinNum(), taskProperties.getTaskRpcThreadMaxNum(), 20L, TimeUnit.SECONDS,
                new SynchronousQueue<>(), new ThreadFactoryBuilder().setDaemon(true).setNameFormat("TaskRpc-%d").build(), new ThreadPoolExecutor.CallerRunsPolicy());
        taskQueueService = new ThreadPoolExecutor(taskProperties.getTaskLocalThreadMinNum(), taskProperties.getTaskLocalThreadMaxNum(), 20L, TimeUnit.SECONDS,
                new SynchronousQueue<>(), new ThreadFactoryBuilder().setDaemon(true).setNameFormat("TaskQueue-%d").build(), new TaskQueueLocalExecutor.SendToQueuePolicy());
        INSTANCE = this;
    }

    /**
     * 返回全局唯一实例。
     *
     * @return 全局 TaskFactory 实例
     */
    public static TaskFactory getInstance() {
        return INSTANCE;
    }

    /**
     * 将任务异步投递到 RabbitMQ 队列，由具备匹配 runner 的远端主机消费。
     * <p>
     * 完全异步、无返回值。任务 ID 与入队时间由框架自动设置；runType 会在方法内临时置为
     * {@link TaskData#RUN_TYPE_GLOBAL}（仅作用于本次发送，方法返回前恢复原值，不污染调用方对象）。
     * 若任务带 taskDelay 且配置了延迟队列，将投递到 TTL 队列延迟消费。
     * <p>
     * <b>阻塞风险</b>：RabbitMQ 出现 channelMax 资源耗尽时会在线程内重试（最多 {@value #MAX_RETRY_TIMES} 次，
     * 退避递增），最坏阻塞约 2 分钟，避免在 Web 请求线程等敏感线程直接调用（见 README）。
     *
     * @param taskData 执行任务对象（taskClass/taskParam 必须已设置）
     * @throws TaskRuntimeException 投递重试全部失败时抛出
     */
    public void sendToQueue(final TaskData<?, ?> taskData) {
        Message message = buildTaskQueueMessage(taskData);
        String queue = message.getMessageProperties().getConsumerQueue();
        //此处可能出现The channelMax limit is reached.报错，所以进行重试。
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            try {
                if (i > 0) {
                    Thread.sleep(i * 500);
                }
                rabbitTemplate.send(queue, queue, message);
                return;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        throw new TaskRuntimeException("The channelMax limit is reached!");
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
            taskQueueService.submit(new TaskQueueLocalExecutor(this, taskMetaInfoManager, taskData));
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
     * @throws TaskRuntimeException RPC 超时或 channelMax 资源耗尽时抛出
     */
    @SuppressWarnings("unchecked")
    public <TP, RD> TaskData<TP, RD> runTask(final TaskData<TP, RD> taskData) {
        taskData.setId(taskSequenceManager.nextId("TaskRunnerLog"));
        taskData.setQueueDate(SystemClock.nowDate());
        // 当自动RPC，并且本地有runner，而且target匹配的时候，运行在本地模式下。
        if (taskData.getRunType() == TaskData.RUN_TYPE_AUTO_RPC && taskMetaInfoManager.checkRunnerRunLocal(taskData)) {
            // 启动本地运行模式。
            taskData.setRunType(TaskData.RUN_TYPE_LOCAL);
        }
        if (taskData.getRunType() == TaskData.RUN_TYPE_LOCAL) {
            taskRunnerContainer.process(taskData);
            return taskData;
        } else {
            taskData.setRunType(TaskData.RUN_TYPE_GLOBAL_RPC);
            //加入优先级信息。
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setPriority(10);
            messageProperties.setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
            messageProperties.setExpiration("180000");
            Message message = rabbitTemplate.getMessageConverter().toMessage(taskData, messageProperties);
            // 全局运行模式
            String queue = taskMetaInfoManager.getFitQueue(taskData);
            //此处可能出现The channelMax limit is reached.报错，所以进行重试。
            for (int i = 0; i < MAX_RETRY_TIMES; i++) {
                try {
                    if (i > 0) {
                        Thread.sleep(i * 500);
                    }
                    Message retMessage = rabbitTemplate.sendAndReceive(queue, queue, message);
                    if (retMessage != null) {
                        return (TaskData<TP, RD>) rabbitTemplate.getMessageConverter().fromMessage(retMessage);
                    }
                    throw new TaskRuntimeException("RPC call timed out: " + queue);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            throw new TaskRuntimeException("The channelMax limit is reached!");
        }
    }

    /**
     * 强制本地同步执行任务，不经 MQ。
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
        taskData.setId(taskSequenceManager.nextId("TaskRunnerLog"));
        taskData.setQueueDate(SystemClock.nowDate());
        // 当自动RPC，并且本地有runner，而且target匹配的时候，运行在本地模式下。
        if (taskData.getRunType() == TaskData.RUN_TYPE_AUTO_RPC && taskMetaInfoManager.checkRunnerRunLocal(taskData)) {
            // 启动本地运行模式。
            taskData.setRunType(TaskData.RUN_TYPE_LOCAL);
        }
        if (taskData.getRunType() == TaskData.RUN_TYPE_LOCAL) {
            taskRunnerContainer.process(taskData);
            return taskData;
        } else {
            throw new TaskRuntimeException(taskData.getClass().getName() + " is not a local task! ");
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
        taskData.setId(taskSequenceManager.nextId("TaskRunnerLog"));
        taskData.setQueueDate(SystemClock.nowDate());

        // 当自动RPC，并且本地有runner，而且target匹配的时候，运行在本地模式下。
        if (taskData.getRunType() == TaskData.RUN_TYPE_AUTO_RPC && taskMetaInfoManager.checkRunnerRunLocal(taskData)) {
            // 启动本地运行模式。
            taskData.setRunType(TaskData.RUN_TYPE_LOCAL);
        }
        if (taskData.getRunType() == TaskData.RUN_TYPE_LOCAL) {
            // 启动本地运行模式。
            return taskRpcService.submit(() -> {
                taskRunnerContainer.process(taskData);
                return taskData;
            });
        } else {
            // 全局运行模式
            taskData.setRunType(TaskData.RUN_TYPE_GLOBAL_RPC);
            //加入优先级信息。
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setPriority(10);
            messageProperties.setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
            messageProperties.setExpiration("180000");
            Message message = rabbitTemplate.getMessageConverter().toMessage(taskData, messageProperties);
            String queue = taskMetaInfoManager.getFitQueue(taskData);
            return taskRpcService.submit(() -> {
                //此处可能出现The channelMax limit is reached.报错，所以进行重试。
                for (int i = 0; i < MAX_RETRY_TIMES; i++) {
                    try {
                        if (i > 0) {
                            Thread.sleep(i * 500);
                        }
                        Message retMessage = rabbitTemplate.sendAndReceive(queue, queue, message);
                        if (retMessage != null) {
                            return (TaskData<TP, RD>) rabbitTemplate.getMessageConverter().fromMessage(retMessage);
                        }
                        throw new TaskRuntimeException("RPC call timed out: " + queue);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
                throw new TaskRuntimeException("The channelMax limit is reached!");
            });
        }
    }

    /**
     * 查询指定队列的积压消息数与消费者数。
     *
     * @param queueName 队列名
     * @return 长度为 2 的数组：下标 0 为消息数量，下标 1 为消费者数量
     */
    public int[] getQueueInfo(String queueName) {
        AMQP.Queue.DeclareOk declareOk = this.rabbitTemplate.execute(channel -> channel.queueDeclarePassive(queueName));
        return new int[]{declareOk.getMessageCount(), declareOk.getConsumerCount()};
    }

    /**
     * 清空指定队列中的全部消息（危险操作，仅供运维使用）。
     *
     * @param queueName 队列名
     * @return 被清除的消息数量
     */
    public int purgeQueue(String queueName) {
        return this.rabbitTemplate.execute(channel -> {
            AMQP.Queue.PurgeOk queuePurged = channel.queuePurge(queueName);
            return queuePurged.getMessageCount();
        });
    }

    /**
     * 构造Task消息对象，此方法用于提前构造TaskData。
     *
     * @param taskData 用于任务执行传值对象
     * @return Message 发送的队列信息
     */
    private Message buildTaskQueueMessage(final TaskData taskData) {
        taskData.setId(taskSequenceManager.nextId("TaskRunnerLog"));
        taskData.setQueueDate(SystemClock.nowDate());
        // 发队列的消息体必须是 GLOBAL，消费端 process 依赖此值判定走限速/重试链路，
        // getFitQueue 也依赖 GLOBAL 判定 TTL 队列。但调用方传入的 taskData 可能被复用
        // （README 强调"taskData 对象不可改变"），故仅在此方法作用域内临时改为 GLOBAL，结束时恢复，
        // 避免污染调用方持有的对象。
        int originRunType = taskData.getRunType();
        taskData.setRunType(TaskData.RUN_TYPE_GLOBAL);
        try {
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setConsumerQueue(taskMetaInfoManager.getFitQueue(taskData));
            // 没法拿到配置信息，除非返回。暂时先用taskDelay > 0处理
            if (taskData.getTaskDelay() > 0) {
                messageProperties.setExpiration(String.valueOf(taskData.getTaskDelay()));
            }
            return rabbitTemplate.getMessageConverter().toMessage(taskData, messageProperties);
        } finally {
            taskData.setRunType(originRunType);
        }
    }

}
