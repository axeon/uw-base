package uw.task.demo;

import org.springframework.stereotype.Component;
import uw.task.TaskData;
import uw.task.TaskRunner;
import uw.task.entity.TaskContact;
import uw.task.entity.TaskRunnerConfig;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 全流程 E2E 专用 Runner：独立队列名（首次声明为 quorum，延时消息生效）+ 可调业务耗时（线程观测采样）。
 *
 * @author axeon
 */
@Component
public class E2eSlowRunner extends TaskRunner<String, String> {

    /**
     * 测试前设置；消费时 countdown。
     */
    public static volatile CountDownLatch latch;

    /**
     * 记录消费端收到的 taskParam。
     */
    public static final AtomicReference<String> receivedParam = new AtomicReference<>();

    /**
     * 业务耗时钩子（毫秒，0=立即）：线程采样需要慢任务拉长执行窗口。
     */
    public static volatile long sleepMillis = 0;

    /**
     * 执行线程名记录（线程观测断言：名字必须含队列名）。
     */
    public static final java.util.Set<String> executingThreadNames = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * 执行线程虚拟性记录（VT 断言）。
     */
    public static final java.util.Set<Boolean> executingThreadVirtual = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * 真实并发峰值（进入/离开 runTask 原子增减）：并发封顶断言依据。
     */
    public static final java.util.concurrent.atomic.AtomicInteger currentConcurrency = new java.util.concurrent.atomic.AtomicInteger();
    public static final java.util.concurrent.atomic.AtomicInteger peakConcurrency = new java.util.concurrent.atomic.AtomicInteger();

    @Override
    public String runTask(TaskData<String, String> taskData) {
        executingThreadNames.add(Thread.currentThread().getName());
        executingThreadVirtual.add(Thread.currentThread().isVirtual());
        peakConcurrency.accumulateAndGet(currentConcurrency.incrementAndGet(), Math::max);
        try {
        if (sleepMillis > 0) {
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        } finally {
            currentConcurrency.decrementAndGet();
        }
        receivedParam.set(taskData.getTaskParam());
        if (latch != null) {
            latch.countDown();
        }
        return "ok:" + taskData.getTaskParam();
    }

    @Override
    public TaskContact initContact() {
        return null;
    }

    @Override
    public TaskRunnerConfig initConfig() {
        TaskRunnerConfig config = new TaskRunnerConfig();
        config.setTaskName("E2E全流程测试任务");
        config.setTaskDesc("全流程E2E：sendToQueue→MQ→消费→执行（quorum+慢任务+线程观测）");
        config.setQueueType(TaskRunnerConfig.QUEUE_TYPE_QUORUM);
        config.setConsumerNum(4);
        config.setPrefetchNum(1);
        return config;
    }
}
