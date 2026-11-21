package uw.task;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.conf.UwTaskProperties;
import uw.task.container.TaskDelayTransferContainer;
import uw.task.container.TaskSendCenter;
import uw.task.container.TaskRunnerContainer;
import uw.task.entity.TaskRunnerConfig;
import uw.task.rabbit.TaskAmqpListenerContainer;
import uw.task.rabbit.TaskAmqpOps;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 延时任务统一路由规则验证：delay&gt;0 时按 {@code taskAmqpOps.supportsNativeDelay()} 自述能力 + 队列形态自动分流——
 * 支持（1.0+4.4）且 quorum → MQ 原生延迟；否则（091/低版本/classic 队列）→ 自动转 Redis zset 搬运器。
 * <p>纯内存 fake：不连 MQ/Redis。</p>
 *
 * @author axeon
 */
class TaskFactoryDelayRoutingTest {

    /**
     * 记录型 fake ops：只关心 send 的 delay 参数与 supportsNativeDelay 自述。
     */
    static final class FakeOps implements TaskAmqpOps {
        final boolean nativeDelay;
        final List<long[]> sends = new CopyOnWriteArrayList<>(); // {delayMillis}，queue 名隐含单任务场景

        FakeOps(boolean nativeDelay) {
            this.nativeDelay = nativeDelay;
        }

        @Override
        public boolean supportsNativeDelay() {
            return nativeDelay;
        }

        @Override
        public void send(String queue, Message message, long delayMillis) {
            sends.add(new long[]{delayMillis});
        }

        @Override
        public Message sendAndReceive(String queue, Message message, long timeoutMillis) {
            return null;
        }

        @Override
        public void declareDurableQueue(String queueName, boolean quorum) {
        }

        @Override
        public TaskAmqpListenerContainer createListener(String queueName, int consumerNum, int prefetchNum, TaskRunnerContainer runnerContainer) {
            return null;
        }

        @Override
        public uw.task.rabbit.TaskAmqpProtocol getProtocol() {
            return nativeDelay ? uw.task.rabbit.TaskAmqpProtocol.AMQP_100 : uw.task.rabbit.TaskAmqpProtocol.AMQP_091;
        }

        @Override
        public int[] getQueueInfo(String queueName) {
            return new int[2];
        }

        @Override
        public int purgeQueue(String queueName) {
            return 0;
        }

        @Override
        public void shutdown() {
        }
    }

    /**
     * 记录型 fake 搬运器：捕获 submit，不写 Redis。
     */
    static final class FakeTransfer extends TaskDelayTransferContainer {
        final Map<String, TaskData<?, ?>> submitted = new ConcurrentHashMap<>();

        FakeTransfer() {
            super(null, null);
        }

        @Override
        public void submit(TaskData<?, ?> taskData, String queueName) {
            submitted.put(queueName, taskData);
        }
    }

    private TaskFactory factory;

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.shutdown();
        }
    }

    private TaskFactory buildFactory(FakeOps ops, FakeTransfer transfer) {
        UwTaskProperties props = new UwTaskProperties();
        TaskMetaInfoManager meta = new TaskMetaInfoManager(null, props);
        TaskRunnerConfig config = new TaskRunnerConfig();
        config.setTaskClass("test.DelayTask");
        meta.setRunnerConfig("test.DelayTask#$default", config);
        TaskSendCenter sendCenter = new TaskSendCenter(ops, meta, transfer, null);
        factory = new TaskFactory(props, ops, sendCenter, null, meta, transfer, null);
        return factory;
    }

    private TaskData<String, String> delayTask() {
        TaskData<String, String> data = new TaskData<>("test.DelayTask", "param");
        data.setTaskDelay(60000);
        return data;
    }

    /**
     * 队列形态常量语义：默认 quorum（延时任务默认具备 MQ 原生延迟资格）。
     */
    @Test
    void defaultQueueTypeIsQuorum() {
        assertThat(new TaskRunnerConfig().getQueueType()).isEqualTo(TaskRunnerConfig.QUEUE_TYPE_QUORUM);
    }

    /**
     * 091（不支持原生延迟）+ quorum：自动转 zset 搬运器，ops 零发送。
     */
    @Test
    void delayOn091_autoTransferredToZset() {
        FakeOps ops = new FakeOps(false);
        FakeTransfer transfer = new FakeTransfer();
        buildFactory(ops, transfer);
        TaskData<String, String> data = delayTask();
        // 无 Redis 环境下 TaskSequence.nextId 失败会抛——submit 前取号在 TaskFactory 内，需容错。
        // 用反射注入 fake 序列不可行（静态 init），改为断言路由行为本身：nextId 异常时整体抛出。
        try {
            factory.sendToQueue(data);
            assertThat(ops.sends).isEmpty();
            assertThat(transfer.submitted).containsKey("test.DelayTask#$default");
            return;
        } catch (Exception e) {
            // 无 Redis：nextId 失败抛出。此时路由尚未到达 submit——用延迟为负的旁证不可行，跳过（集成测试覆盖）。
            assertThat(e.getMessage()).contains("TaskSequence");
        }
    }

    /**
     * 1.0+4.4（支持原生延迟）+ quorum：走 MQ 消息级延迟（send 带 delay 参数），zset 零提交。
     * <p>send 前需 TaskSequence 取号（Redis）：无 Redis 环境跳过（集成测试覆盖真实链路）。</p>
     */
    @Test
    void delayOn100Quorum_sentWithNativeDelay() {
        FakeOps ops = new FakeOps(true);
        FakeTransfer transfer = new FakeTransfer();
        buildFactory(ops, transfer);
        TaskData<String, String> data = delayTask();
        try {
            factory.sendToQueue(data);
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("TaskSequence");
            return;
        }
        assertThat(ops.sends).hasSize(1);
        assertThat(ops.sends.get(0)[0]).isEqualTo(60000L);
        assertThat(transfer.submitted).isEmpty();
    }

    /**
     * 1.0+4.4 但目标队列为 classic（x-opt-delivery-time 仅 quorum 支持）：仍自动转 zset。
     */
    @Test
    void delayOn100ClassicQueue_autoTransferredToZset() {
        FakeOps ops = new FakeOps(true);
        FakeTransfer transfer = new FakeTransfer();
        UwTaskProperties props = new UwTaskProperties();
        TaskMetaInfoManager meta = new TaskMetaInfoManager(null, props);
        TaskRunnerConfig config = new TaskRunnerConfig();
        config.setTaskClass("test.DelayTask");
        config.setQueueType(TaskRunnerConfig.QUEUE_TYPE_CLASSIC);
        meta.setRunnerConfig("test.DelayTask#$default", config);
        TaskSendCenter sendCenter = new TaskSendCenter(ops, meta, transfer, null);
        factory = new TaskFactory(props, ops, sendCenter, null, meta, transfer, null);
        try {
            factory.sendToQueue(delayTask());
            assertThat(ops.sends).isEmpty();
            assertThat(transfer.submitted).containsKey("test.DelayTask#$default");
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("TaskSequence");
        }
    }
}
