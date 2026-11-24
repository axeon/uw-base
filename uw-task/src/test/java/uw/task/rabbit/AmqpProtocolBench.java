package uw.task.rabbit;

import java.util.concurrent.TimeUnit;

import com.rabbitmq.client.amqp.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties;
import uw.task.TaskData;
import uw.task.converter.TaskMessageConverter;
import uw.task.demo.EchoRunner;

/**
 * RabbitMQ 4.4.1 综合评测（显式触发：-Dtest=AmqpProtocolBench -Damqp.bench=true）。
 * <p>前提：独立 4.4.1 实例可达（-Duw.task.rabbitmq.port/username/password 注入）。
 * 覆盖：①探测判定 ②091/100 声明 classic+quorum ③两协议吞吐对照 ④原生延迟消息到点投递 ⑤RPC。</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfSystemProperty(named = "amqp.bench", matches = "true")
class AmqpProtocolBench {

    static RabbitProperties props;
    static RabbitCapabilityProbe capability;
    static Rabbit091Ops ops091;
    static Rabbit100Ops ops100;
    static final String QUEUE_CLASSIC = "uw-bench.classic";
    static final String QUEUE_QUORUM = "uw-bench.quorum";
    static final int WARMUP = 2000;
    static final int COUNT = Integer.parseInt(System.getProperty("amqp.bench.count", "50000"));

    @BeforeAll
    static void setUp() {
        props = new RabbitProperties();
        props.setHost(System.getProperty("uw.task.rabbitmq.host", "10.0.0.10"));
        props.setPort(Integer.parseInt(System.getProperty("uw.task.rabbitmq.port", "5672")));
        props.setUsername(System.getProperty("uw.task.rabbitmq.username", "rabbit"));
        props.setPassword(System.getProperty("uw.task.rabbitmq.password"));
        capability = RabbitCapabilityProbe.probe(props);
        ops091 = new Rabbit091Ops(props, 50, true);
        if (capability.getProtocol() == TaskAmqpProtocol.AMQP_100 || capability.isAmqp100Available()) {
            ops100 = new Rabbit100Ops(props, true);
        }
    }

    @AfterAll
    static void tearDown() {
        if (ops091 != null) ops091.shutdown();
        if (ops100 != null) ops100.shutdown();
    }

    @Test
    @Order(1)
    void probe_shouldDetect091DefaultAndNativeDelay() {
        Assertions.assertTrue(capability.isAmqp100Available(), "4.4.1 应握手出 1.0");
        Assertions.assertTrue(capability.isNativeDelayAvailable(), "4.4.1 应支持原生延迟");
        Assertions.assertTrue(capability.isAmqp091Available(), "4.4.1 应握手出 0-9-1");
        Assertions.assertEquals(TaskAmqpProtocol.AMQP_091, capability.getProtocol(), "默认 091");
    }

    @Test
    @Order(2)
    void declare_classicAndQuorum_bothProtocols() {
        ops091.declareDurableQueue(QUEUE_CLASSIC, false);
        ops091.declareDurableQueue(QUEUE_QUORUM, true);
        if (ops100 != null) {
            ops100.declareDurableQueue(QUEUE_CLASSIC, false);
            ops100.declareDurableQueue(QUEUE_QUORUM, true);
        }
    }

    /**
     * 原生延迟端到端：发 delay=3s 的消息到 quorum 队列，091 轮询接收（basicGet 自动确认）——2.5s 内必须收不到，
     * 之后必须收到（允许到点投递约 0.5s 的调度误差）。两种协议同一队列、同一口径。
     */
    private void nativeDelayEndToEnd(TaskAmqpOps ops, String protocol) throws Exception {
        long delay = 3000;
        ops.send(QUEUE_QUORUM, buildMessage("delay-e2e-" + protocol), delay, e -> { });
        long sentAt = System.currentTimeMillis();
        org.springframework.amqp.core.Message received = null;
        long receivedAt = 0;
        while (System.currentTimeMillis() - sentAt < delay + 6000) {
            received = ops091.receiveForTest(QUEUE_QUORUM, 500);
            if (received != null) {
                receivedAt = System.currentTimeMillis();
                break;
            }
        }
        Assertions.assertNotNull(received, protocol + " 延迟消息到期后应可接收");
        Assertions.assertTrue(receivedAt - sentAt >= delay - 500, protocol + " 延迟消息到期前不应可见: " + (receivedAt - sentAt) + "ms");
        System.out.printf("BENCH nativeDelay-e2e %s: 延迟 %dms，实际 %dms 后收到 OK%n", protocol, delay, receivedAt - sentAt);
    }

    private Message buildMessage(String tag) {
        TaskData<String, String> td = TaskData.<String, String>builder(EchoRunner.class, "bench-" + tag).build();
        return new TaskMessageConverter().toMessage(td, new MessageProperties());
    }

    private long benchSend(TaskAmqpOps ops, String queue, String tag) {
        for (int i = 0; i < WARMUP; i++) {
            ops.send(queue, buildMessage(tag), 0, e -> { });
        }
        long start = System.nanoTime();
        for (int i = 0; i < COUNT; i++) {
            ops.send(queue, buildMessage(tag), 0, e -> { });
        }
        return (long) (COUNT / ((System.nanoTime() - start) / 1e9));
    }

    @Test
    @Order(3)
    void throughput_091_vs_100() {
        long t091 = benchSend(ops091, QUEUE_CLASSIC, "091");
        System.out.printf("BENCH 091 send: %d msg/s (%d msgs)%n", t091, COUNT);
        if (ops100 != null) {
            long t100 = benchSend(ops100, QUEUE_CLASSIC, "100");
            System.out.printf("BENCH 100 send: %d msg/s (%d msgs)%n", t100, COUNT);
        }
    }

    @Test
    @Order(4)
    void nativeDelay_messageInvisibleBeforeDue() throws Exception {
        Assumptions.assumeTrue(ops100 != null && capability.isNativeDelayAvailable());
        // 发送 delay=5s 的消息到 quorum 队列：到期前必须不可见（消费者收不到 = 延迟生效）。
        // 注：到点投递已用真实 Consumer 独立实证（261119：5s 延迟 +5486ms 收到，精度~0.5s）；
        // 本处不做 receive 拉模式验证——receiver 超时后链路仍持 credit 会静默吞消息，不适合做轮询验证。
        ops100.send(QUEUE_QUORUM, buildMessage("delay"), 5000, e -> { });
        long t0 = System.currentTimeMillis();
        // 3s 内不应有任何消息可收（到期前不可见性）
        try {
            Message early = ops100.template.receive(QUEUE_QUORUM).get(3, TimeUnit.SECONDS);
            Assertions.assertNull(early, "延迟消息到期前不应可见");
        } catch (Exception ignore) {
            // 超时=无消息，正是期望
        }
        System.out.printf("BENCH nativeDelay: 到期前不可见 OK (%dms)%n", System.currentTimeMillis() - t0);
        nativeDelayEndToEnd(ops091, "091");
        if (ops100 != null) {
            nativeDelayEndToEnd(ops100, "1.0");
        }
        // 收尾：挂真实消费者收走到期消息，避免污染后续用例
        try {
            Consumer consumer = ops100.getConnection().consumerBuilder().queue(QUEUE_QUORUM)
                    .messageHandler((ctx, msg) -> ctx.accept()).build();
            Thread.sleep(8000);
            consumer.close();
        } catch (Exception ignore) {
        }
    }

    @Test
    @Order(5)
    void queueInfo_purge() {
        int[] info091 = ops091.getQueueInfo(QUEUE_CLASSIC);
        System.out.printf("BENCH queueInfo(091): messages=%d consumers=%d%n", info091[0], info091[1]);
        if (ops100 != null) {
            int[] info100 = ops100.getQueueInfo(QUEUE_CLASSIC);
            System.out.printf("BENCH queueInfo(100): %s%n", info100 == null ? "null" : "messages=" + info100[0] + " consumers=" + info100[1]);
        }
        ops091.purgeQueue(QUEUE_CLASSIC);
        ops091.purgeQueue(QUEUE_QUORUM);
    }
}
