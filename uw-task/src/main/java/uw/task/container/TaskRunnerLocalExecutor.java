package uw.task.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.LimitedVirtualThreadExecutor;
import uw.task.TaskData;
import uw.task.TaskFactory;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.exception.TaskRuntimeException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

/**
 * 为了优化执行一些队列任务，使它支持本地线程化运行，以提高效率。
 *
 * <p>由 {@link TaskFactory#runQueue} 提交到本地队列线程池后执行：本机存在匹配 runner 时本地同步执行，
 * 否则降级投递到 MQ 队列。<b>绝不走 {@code TaskFactory.runTask()} 的同步 RPC</b>——
 * 本地队列线程承担高频短任务，若退化为最长 180s 的 sendAndReceive 会拖垮线程池，
 * 违背"减少 MQ 压力、提升运行效率"的设计初衷。</p>
 *
 * @author axeon
 */
public class TaskRunnerLocalExecutor implements Runnable {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(TaskRunnerLocalExecutor.class);

    /**
     * TaskFactory，用于本地执行与降级入队。
     */
    private final TaskFactory taskFactory;

    /**
     * 任务元信息管理器，用于判定本机是否存在匹配 runner。
     */
    private final TaskMetaInfoManager taskMetaInfoManager;

    /**
     * 要执行的任务。
     */
    private final TaskData<?, ?> taskData;

    /**
     * 构建本地队列执行器。
     *
     * @param taskFactory           TaskFactory（本地执行与降级入队入口）
     * @param taskMetaInfoManager   任务元信息管理器
     * @param taskData              要执行的任务
     */
    public TaskRunnerLocalExecutor(TaskFactory taskFactory, TaskMetaInfoManager taskMetaInfoManager, TaskData<?, ?> taskData) {
        this.taskFactory = taskFactory;
        this.taskMetaInfoManager = taskMetaInfoManager;
        this.taskData = taskData;
    }

    /**
     * 本地队列执行入口：本机存在匹配 runner 时本地同步执行，否则降级投递到 MQ 队列。
     */
    @Override
    public void run() {
        if (taskMetaInfoManager.checkRunnerRunLocal(taskData)) {
            try {
                // AUTO_RPC 在 runTaskLocal 内部会判定为 LOCAL；本地同步执行。
                taskFactory.runTaskLocal(taskData);
            } catch (TaskRuntimeException e) {
                // 极端情况下本地执行入口判定失败（不应发生），降级到队列以保证任务不丢。
                log.warn("TaskRunnerLocalExecutor 本地执行失败, 降级入队: taskClass=[{}]", taskData.getTaskClass(), e);
                taskFactory.sendToQueue(taskData);
            }
        } else {
            // 本机无 runner：降级投递到 MQ 队列，由具备该 runner 的远端主机消费。
            taskFactory.sendToQueue(taskData);
        }
    }

    /**
     * 本地队列执行器（VT+并发上限+有界缓冲）满载时的溢出策略：将任务直接转发到 MQ 队列，由远端消费者兜底处理。
     * <p>队列模式（queueCapacity&gt;0）下 RejectedPolicy 收到的是原始 Runnable，instanceof 判定可靠。</p>
     */
    public static class SendToQueuePolicy implements LimitedVirtualThreadExecutor.RejectedPolicy {

    /**
     * 默认构造器。
     */
    public SendToQueuePolicy() {
    }

        /**
         * 并发与缓冲均满时回调：若是本地队列任务则转投 MQ（不抛异常、不阻塞提交线程）。
         *
         * @param executor  虚拟线程执行器（未使用）
         * @param semaphore 背压信号量（未使用）
         * @param task      被溢出的任务
         */
        @Override
        public void rejectedExecution(ExecutorService executor, Semaphore semaphore, Runnable task) {
            if (task instanceof TaskRunnerLocalExecutor e) {
                e.taskFactory.sendToQueue(e.taskData);
            }
        }
    }
}
