package uw.task.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.task.TaskData;
import uw.task.TaskFactory;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.exception.TaskRuntimeException;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

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
public class TaskQueueLocalExecutor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(TaskQueueLocalExecutor.class);

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

    public TaskQueueLocalExecutor(TaskFactory taskFactory, TaskMetaInfoManager taskMetaInfoManager, TaskData<?, ?> taskData) {
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
                log.warn("TaskQueueLocalExecutor 本地执行失败, 降级入队: taskClass=[{}]", taskData.getTaskClass(), e);
                taskFactory.sendToQueue(taskData);
            }
        } else {
            // 本机无 runner：降级投递到 MQ 队列，由具备该 runner 的远端主机消费。
            taskFactory.sendToQueue(taskData);
        }
    }

    /**
     * 转发到队列中。
     */
    private void sendToQueue() {
        taskFactory.sendToQueue(taskData);
    }

    /**
     * 当本地队列线程池满时，拒绝策略：将任务直接转发到 MQ 队列，由远端消费者兜底处理。
     */
    public static class SendToQueuePolicy implements RejectedExecutionHandler {

        /**
         * 线程池拒绝任务时回调：若是本地队列任务则转投 MQ。
         *
         * @param r        被拒绝的任务
         * @param executor 拒绝该任务的线程池
         */
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (r instanceof TaskQueueLocalExecutor e) {
                e.sendToQueue();
            }
        }
    }

}
