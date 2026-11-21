package uw.task.demo;

import org.springframework.stereotype.Component;
import uw.task.TaskData;
import uw.task.TaskRunner;
import uw.task.entity.TaskContact;
import uw.task.entity.TaskRunnerConfig;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 测试用业务 Runner：模拟 uw-task 使用方定义的任务。
 * <p>
 * runTask 回显 taskParam（返回 "echo:" + param），并在消费时 countdown 静态 latch、记录收到的 param，
 * 供 {@link uw.task.UwTaskIntegrationTest} 异步断言（MQ 消费是异步的，靠 latch 等待）。
 * </p>
 * <p>
 * 静态字段是测试钩子（uw-task 消费在框架线程，测试需跨线程拿到消费结果）。
 * </p>
 *
 * @author axeon
 */
@Component
public class EchoRunner extends TaskRunner<String, String> {

    /**
     * 测试前设置；消费时 countdown。volatile：测试线程写、框架消费线程读。
     */
    public static volatile CountDownLatch latch;

    /**
     * 记录消费端收到的 taskParam。
     */
    public static final AtomicReference<String> receivedParam = new AtomicReference<>();

    @Override
    public String runTask(TaskData<String, String> taskData) {
        String param = taskData.getTaskParam();
        receivedParam.set(param);
        CountDownLatch l = latch;
        if (l != null) {
            l.countDown();
        }
        return "echo:" + param;
    }

    @Override
    public TaskRunnerConfig initConfig() {
        TaskRunnerConfig config = new TaskRunnerConfig();
        config.setTaskClass(getClass().getName());
        // 任务级队列：每个 EchoRunner 一个独立队列
        config.setQueueType(TaskRunnerConfig.QUEUE_TYPE_CLASSIC);
        config.setConsumerNum(2);
        config.setPrefetchNum(1);
        // rateLimitType 用默认（NONE）
        return config;
    }

    @Override
    public TaskContact initContact() {
        return null;
    }
}
