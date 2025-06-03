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

    private static final Logger log = LoggerFactory.getLogger(TaskFactory.class);

    /**
     * 最大重试次数。
     */
    private static final int MAX_RETRY_TIMES = 50;
    /**
     * 全局唯一实例。
     */
    private static TaskFactory taskFactory;
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
        taskFactory = this;
    }

    /**
     * 返回全局唯一实例。
     *
     * @return
     */
    public static TaskFactory getInstance() {
        return taskFactory;
    }

    /**
     * 发送到延迟队列中
     *
     * @param taskData 执行任务对象
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
            taskQueueService.submit(new TaskQueueLocalExecutor(this, taskData));
        }
    }

    /**
     * 同步执行任务，可能会导致阻塞。
     * 在调用的时候，尤其要注意，taskData对象不可改变！
     *
     * @param taskData 任务数据
     * @return
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
                    return (TaskData<TP, RD>) rabbitTemplate.getMessageConverter().fromMessage(retMessage);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            throw new TaskRuntimeException("The channelMax limit is reached!");
        }
    }

    /**
     * 同步执行任务，没有线程池支持，会导致阻塞。
     * 在调用的时候，尤其要注意，taskData对象不可改变！
     *
     * @param taskData 任务数据
     * @return
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
     * 远程运行任务，并返回future。
     * 如果需要获取数据，可以使用future.get()来获取。
     * 此方法要谨慎使用，因为task存在限速，大并发下可能会导致线程数超。
     * 在调用的时候，尤其要注意，taskData对象不可改变！
     *
     * @param taskData 任务数据
     * @return
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
                        return (TaskData<TP, RD>) rabbitTemplate.getMessageConverter().fromMessage(retMessage);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
                throw new TaskRuntimeException("The channelMax limit is reached!");
            });
        }
    }

    /**
     * 获取队列信息。
     *
     * @param queueName
     * @return 0 是消息数量 1 是消费者数量
     */
    public int[] getQueueInfo(String queueName) {
        AMQP.Queue.DeclareOk declareOk = this.rabbitTemplate.execute(channel -> channel.queueDeclarePassive(queueName));
        return new int[]{declareOk.getMessageCount(), declareOk.getConsumerCount()};
    }

    /**
     * 清除队列。
     *
     * @param queueName
     * @return 被清除的队列数
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
        taskData.setRunType(TaskData.RUN_TYPE_GLOBAL);
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setConsumerQueue(taskMetaInfoManager.getFitQueue(taskData));
        // 没法拿到配置信息，除非返回。暂时先用taskDelay > 0处理
        if (taskData.getTaskDelay() > 0) {
            messageProperties.setExpiration(String.valueOf(taskData.getTaskDelay()));
        }
        return rabbitTemplate.getMessageConverter().toMessage(taskData, messageProperties);
    }

}
