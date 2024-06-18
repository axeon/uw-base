package uw.task.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.task.TaskData;
import uw.task.TaskFactory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 为了优化执行一些队列任务，使它支持本地线程化运行，以提高效率。
 *
 * @author axeon
 */
public class TaskQueueLocalExecutor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(TaskQueueLocalExecutor.class);

    /**
     * TaskFactory
     */
    private TaskFactory taskFactory;

    /**
     * 要执行的任务。
     */
    private TaskData taskData;

    public TaskQueueLocalExecutor(TaskFactory taskFactory, TaskData taskData) {
        this.taskFactory = taskFactory;
        this.taskData = taskData;
    }

    @Override
    public void run() {
        taskFactory.runTask(taskData);
    }

    /**
     * 转发到队列中。
     */
    private void sendToQueue() {
        taskFactory.sendToQueue(taskData);
    }

    /**
     * 当线程池满的时候，直接发送到MQ队列中。
     */
    public static class SendToQueuePolicy implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (r instanceof TaskQueueLocalExecutor e) {
                e.sendToQueue();
            }
        }
    }

}
