package uw.task.rabbit;

import com.rabbitmq.client.amqp.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties;
import uw.task.TaskData;
import uw.task.container.TaskRunnerContainer;
import uw.task.converter.TaskMessageConverter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 1.0 线程伸缩与背压实测（显式触发：-Damqp.bench=true）。
 *
 * <p>用与 ElasticConsumerGroup 相同的组件直建（LimitedVirtualThreadExecutor 执行通道 + credit 消费者 +
 * settle-after-execute），实测三个命题：
 * ①执行并发严格封顶 consumerNum（慢任务下 activeCount 峰值==上限）；
 * ②resize 收缩/扩张即时生效（pause/unpause + 重建执行器）；
 * ③背压传导 broker（credit 耗尽后 unacked 停在 initialCredits，不会无限预取）。</p>
 */
@EnabledIfSystemProperty(named = "amqp.bench", matches = "true")
class ConcurrencyControlProbe {

    static Rabbit100Ops ops;
    static final String QUEUE = "uw-bench.concurrency";

    @BeforeAll
    static void setUp() {
        RabbitProperties props = new RabbitProperties();
        props.setHost(System.getProperty("uw.task.rabbitmq.host", "10.0.0.10"));
        props.setPort(Integer.parseInt(System.getProperty("uw.task.rabbitmq.port", "30772")));
        props.setUsername(System.getProperty("uw.task.rabbitmq.username", "rabbit"));
        props.setPassword(System.getProperty("uw.task.rabbitmq.password", "rabbit44eval"));
        ops = new Rabbit100Ops(props, "cc-probe", true);
        ops.declareDurableQueue(QUEUE, true);
    }

    @AfterAll
    static void tearDown() {
        ops.purgeQueue(QUEUE);
        ops.shutdown();
    }

    private static Message build() {
        return new TaskMessageConverter().toMessage(
                TaskData.<String, String>builder(uw.task.demo.EchoRunner.class, "cc").build(), new MessageProperties());
    }

    /**
     * 命题①：执行并发严格封顶。3 路并发上限，投 30 条每条 sleep 300ms，
     * 采样 activeCount 峰值应 ==3（若超标说明限流失效）。
     */
    @Test
    void executionConcurrencyCapped() throws Exception {
        int cap = 3;
        uw.common.util.LimitedVirtualThreadExecutor worker = new uw.common.util.LimitedVirtualThreadExecutor(
                "cc-cap-", cap, 64, new uw.common.util.LimitedVirtualThreadExecutor.BlockPolicy());
        AtomicInteger active = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        AtomicInteger done = new AtomicInteger();
        for (int i = 0; i < 30; i++) {
            worker.submit(() -> {
                int now = active.incrementAndGet();
                peak.accumulateAndGet(now, Math::max);
                active.decrementAndGet();
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignore) {
                    Thread.currentThread().interrupt();
                }
                done.incrementAndGet();
            });
        }
        Thread.sleep(2500);
        System.out.printf("CC-CAP: done=%d/30 peakActive=%d (cap=%d)%n", done.get(), peak.get(), cap);
        org.junit.jupiter.api.Assertions.assertEquals(cap, peak.get(), "执行并发峰值必须==上限");
        org.junit.jupiter.api.Assertions.assertTrue(done.get() < 30, "3路×300ms 下 2.5s 不应跑完 30 条（背压排队中）");
        worker.shutdown();
    }

    /**
     * 命题②：消费者 pause/unpause 伸缩即时生效。建 4 消费者，pause 2 后投消息，
     * 消费速率应约减半；unpause 恢复后速率回升。
     */
    @Test
    void consumerPauseResumeScaling() throws Exception {
        // 走真实生产路径：createListener（ElasticConsumerGroup + LimitedVirtualThreadExecutor）
        // + tune 伸缩。process mock 为 20ms 业务耗时——并发=吞吐（4 路≈200/s，2 路≈100/s）。
        // 关键对照：裸消费者 pause 不减吞吐（credit 超发），生产路径靠执行通道封顶才有效。
        uw.task.container.TaskRunnerContainer mockRunner = org.mockito.Mockito.mock(uw.task.container.TaskRunnerContainer.class);
        java.util.concurrent.atomic.AtomicInteger consumed = new java.util.concurrent.atomic.AtomicInteger();
        org.mockito.Mockito.doAnswer(inv -> {
            Thread.sleep(20);
            consumed.incrementAndGet();
            return null;
        }).when(mockRunner).process(org.mockito.ArgumentMatchers.any(uw.task.TaskData.class));
        TaskAmqpListenerContainer container = ops.createListener(QUEUE, 4, 10, mockRunner);
        ops.purgeQueue(QUEUE);
        java.util.concurrent.atomic.AtomicBoolean sending = new java.util.concurrent.atomic.AtomicBoolean(true);
        Thread sender = new Thread(() -> {
            while (sending.get()) {
                ops.send(QUEUE, build(), 0);
            }
        }, "cc-sender");
        sender.setDaemon(true);
        sender.start();
        Thread.sleep(2000);
        consumed.getAndSet(0);
        Thread.sleep(1000);
        int full = consumed.getAndSet(0);
        container.tune(2, 10); // 收缩到 2 路
        Thread.sleep(1500);
        consumed.getAndSet(0);
        Thread.sleep(1000);
        int shrunk = consumed.getAndSet(0);
        container.tune(4, 10); // 扩张回 4 路
        Thread.sleep(1500);
        consumed.getAndSet(0);
        Thread.sleep(1000);
        int expanded = consumed.getAndSet(0);
        sending.set(false);
        sender.join(3000);
        container.destroy();
        ops.purgeQueue(QUEUE);
        System.out.printf("CC-SCALE(生产路径): 4路=%d/s, tune(2)=%d/s, tune(4)=%d/s%n", full, shrunk, expanded);
        org.junit.jupiter.api.Assertions.assertTrue(shrunk < full * 0.7, "tune 收缩后速率应显著下降 (shrunk=%d full=%d)".formatted(shrunk, full));
        org.junit.jupiter.api.Assertions.assertTrue(expanded > shrunk * 1.3, "tune 扩张后速率应回升 (expanded=%d shrunk=%d)".formatted(expanded, shrunk));
    }

    /**
     * 命题③：背压传导 broker。initialCredits=5、消费端 sleep 不 settle，
     * 投 50 条后 broker 侧 unacked 应停在 ≤5×消费者数（credit 窗口封顶，不无限预取）。
     */
    @Test
    void creditBackpressureToBroker() throws Exception {
        int credits = 5;
        CountDownLatch hold = new CountDownLatch(1);
        AtomicReference<Consumer> holder = new AtomicReference<>();
        Consumer consumer = ops.getConnection().consumerBuilder()
                .queue(QUEUE)
                .initialCredits(credits)
                .messageHandler((ctx, msg) -> {
                    try {
                        hold.await(); // 不 settle：模拟慢消费者
                    } catch (InterruptedException ignore) {
                        Thread.currentThread().interrupt();
                    }
                    ctx.accept();
                }).build();
        holder.set(consumer);
        ops.purgeQueue(QUEUE);
        for (int i = 0; i < 50; i++) {
            ops.send(QUEUE, build(), 0);
        }
        Thread.sleep(2000);
        int[] info = ops.getQueueInfo(QUEUE);
        int unacked = 50 - info[0];
        hold.countDown(); // 释放
        consumer.close();
        System.out.printf("CC-BACKPRESSURE: 投50条 credits=%d, broker侧ready=%d (已预取=%d, 窗口封顶应≈%d)%n",
                credits, info[0], 50 - info[0], credits);
        org.junit.jupiter.api.Assertions.assertTrue(50 - info[0] <= credits * 2,
                "broker 预取应被 credit 窗口封顶（ready 不应远小于 50-credits）");
        org.junit.jupiter.api.Assertions.assertTrue(info[0] >= 50 - credits * 2,
                "大部分消息应留在 broker（ready），仅 credit 窗口内被预取");
    }
}
