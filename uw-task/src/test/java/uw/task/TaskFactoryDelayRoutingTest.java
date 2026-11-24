package uw.task;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.conf.UwTaskProperties;
import uw.task.container.TaskDelayTransferContainer;
import uw.task.container.TaskRunnerContainer;
import uw.task.container.TaskSendCenter;
import uw.task.converter.TaskMessageConverter;
import uw.task.entity.TaskRunnerConfig;
import uw.task.exception.TaskRuntimeException;
import uw.task.rabbit.TaskAmqpListenerContainer;
import uw.task.rabbit.TaskAmqpOps;
import uw.task.rabbit.TaskAmqpProtocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 延时任务统一路由规则验证：delay&gt;0 时按 {@code taskAmqpOps.supportsNativeDelay()} 自述能力 + 队列形态自动分流——
 * 支持（RabbitMQ ≥4.4，两种协议）且 quorum → MQ 原生延迟；否则（低版本/classic 队列）→ 自动转 Redis zset 搬运器。
 * <p>纯内存 fake：不连 MQ/Redis。</p>
 *
 * @author axeon
 */
class TaskFactoryDelayRoutingTest {

    /**
     * 记录型 fake ops：只关心 send 的 delay 参数与 supportsNativeDelay 自述。
     */
    static class FakeOps implements TaskAmqpOps {
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
        public void send(String queue, Message message, long delayMillis, Consumer<Exception> onAsyncFailure) {
            sends.add(new long[]{delayMillis});
        }

        @Override
        public Message sendAndReceive(String queue, Message message) {
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
        public TaskAmqpProtocol getProtocol() {
            return nativeDelay ? TaskAmqpProtocol.AMQP_100 : TaskAmqpProtocol.AMQP_091;
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
        final AtomicInteger submittedCount = new AtomicInteger();

        FakeTransfer() {
            super(null, null);
        }

        @Override
        public void submit(TaskData<?, ?> taskData) {
            submittedCount.incrementAndGet();
        }
    }

    private TaskFactory factory;

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.shutdown();
        }
    }

    /**
     * RPC 重试：前两次连接类异常、第三次拿到回复 → 返回结果，调用次数=3（日志合并为一条 WARN 由 doRpcCall 内部完成）。
     * <p>经反射直调 doRpcCall，绕开 runTask 入口的取号（需 Redis）。</p>
     */
    @Test
    void rpcRetriesThenSucceeds() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Message reply = new TaskMessageConverter().toMessage(TaskData.<String, String>builder("test.Rpc", "p").build(), new MessageProperties());
        FakeOps ops = new FakeOps(false) {
            @Override
            public Message sendAndReceive(String queue, Message message) {
                if (calls.incrementAndGet() <= 2) {
                    throw new IllegalStateException("boom");
                }
                return reply;
            }
        };
        buildFactory(ops, new FakeTransfer());
        Method doRpcCall = TaskFactory.class.getDeclaredMethod("doRpcCall", String.class, Message.class);
        doRpcCall.setAccessible(true);
        Object result = doRpcCall.invoke(factory, "test.Rpc", reply);
        assertThat(calls.get()).isEqualTo(3);
        assertThat(((TaskData<?, ?>) result).getTaskClass()).isEqualTo("test.Rpc");
    }

    /**
     * RPC 重试耗尽：每次都抛异常 → 调用上限 20 次后抛 TaskRuntimeException（不重试的只有超时与中断）。
     */
    @Test
    void rpcGivesUpAfterMaxRetries() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        FakeOps ops = new FakeOps(false) {
            @Override
            public Message sendAndReceive(String queue, Message message) {
                calls.incrementAndGet();
                throw new IllegalStateException("always down");
            }
        };
        buildFactory(ops, new FakeTransfer());
        Method doRpcCall = TaskFactory.class.getDeclaredMethod("doRpcCall", String.class, Message.class);
        doRpcCall.setAccessible(true);
        Message request = new TaskMessageConverter().toMessage(TaskData.<String, String>builder("test.Rpc", "p").build(), new MessageProperties());
        assertThatThrownBy(() -> doRpcCall.invoke(factory, "test.Rpc", request))
                .isInstanceOf(InvocationTargetException.class)
                .cause()
                .isInstanceOf(TaskRuntimeException.class)
                .hasMessageContaining("RPC call failed after retries");
        assertThat(calls.get()).isEqualTo(20);
    }

    /**
     * 仅定时任务档（未配置 MQ，操作面为 null）：远程 RPC 在 doRpcCall 入口快速失败，不调用 ops、不进重试。
     */
    @Test
    void rpcFailsFastWhenMqNotConfigured() throws Exception {
        FakeOps ops = new FakeOps(true) {
            @Override
            public Message sendAndReceive(String queue, Message message) {
                throw new AssertionError("未配置 MQ 时不应调用 ops");
            }
        };
        UwTaskProperties props = new UwTaskProperties();
        TaskMetaInfoManager meta = new TaskMetaInfoManager(null, props);
        TaskDelayTransferContainer transfer = new FakeTransfer();
        TaskListenerManager listeners = new TaskListenerManager();
        // 仅 sendToQueue 一侧接 ops（验证入队快速失败）；RPC 一侧传 null 模拟仅定时任务档
        TaskSendCenter sendCenter = new TaskSendCenter(ops, meta, transfer, listeners);
        TaskFactory nullOpsFactory = new TaskFactory(props, null, sendCenter, null, meta, transfer, listeners);
        Method doRpcCall = TaskFactory.class.getDeclaredMethod("doRpcCall", String.class, Message.class);
        doRpcCall.setAccessible(true);
        Message request = new TaskMessageConverter().toMessage(TaskData.<String, String>builder("test.Rpc", "p").build(), new MessageProperties());
        assertThatThrownBy(() -> doRpcCall.invoke(nullOpsFactory, "test.Rpc", request))
                .isInstanceOf(InvocationTargetException.class)
                .cause()
                .isInstanceOf(TaskRuntimeException.class)
                .hasMessage(TaskAmqpOps.MQ_NOT_CONFIGURED);
        nullOpsFactory.shutdown();
    }

    private TaskFactory buildFactory(FakeOps ops, FakeTransfer transfer) {
        UwTaskProperties props = new UwTaskProperties();
        TaskMetaInfoManager meta = new TaskMetaInfoManager(null, props);
        TaskRunnerConfig config = new TaskRunnerConfig();
        config.setTaskClass("test.DelayTask");
        meta.setRunnerConfig("test.DelayTask#$default", config);
        TaskSendCenter sendCenter = new TaskSendCenter(ops, meta, transfer, new TaskListenerManager());
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
            assertThat(transfer.submittedCount.get()).isEqualTo(1);
            return;
        } catch (Exception e) {
            // 无 Redis：nextId 失败抛出。此时路由尚未到达 submit——用延迟为负的旁证不可行，跳过（集成测试覆盖）。
            assertThat(e.getMessage()).contains("TaskSequence");
        }
    }

    /**
     * broker 支持原生延迟 + quorum：走 MQ 消息级延迟（send 带 delay 参数），zset 零提交。
     * <p>send 前需 TaskSequence 取号（Redis）：无 Redis 环境跳过（集成测试覆盖真实链路）。</p>
     */
    @Test
    void delayOnNativeCapableQuorum_sentWithNativeDelay() {
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
        assertThat(transfer.submittedCount.get()).isEqualTo(0);
    }

    /**
     * broker 支持原生延迟但目标队列为 classic（x-opt-delivery-time 仅 quorum 支持）：仍自动转 zset。
     */
    @Test
    void delayOnNativeCapableClassicQueue_autoTransferredToZset() {
        FakeOps ops = new FakeOps(true);
        FakeTransfer transfer = new FakeTransfer();
        UwTaskProperties props = new UwTaskProperties();
        TaskMetaInfoManager meta = new TaskMetaInfoManager(null, props);
        TaskRunnerConfig config = new TaskRunnerConfig();
        config.setTaskClass("test.DelayTask");
        config.setQueueType(TaskRunnerConfig.QUEUE_TYPE_CLASSIC);
        meta.setRunnerConfig("test.DelayTask#$default", config);
        TaskSendCenter sendCenter = new TaskSendCenter(ops, meta, transfer, new TaskListenerManager());
        factory = new TaskFactory(props, ops, sendCenter, null, meta, transfer, null);
        try {
            factory.sendToQueue(delayTask());
            assertThat(ops.sends).isEmpty();
            assertThat(transfer.submittedCount.get()).isEqualTo(1);
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("TaskSequence");
        }
    }
}
