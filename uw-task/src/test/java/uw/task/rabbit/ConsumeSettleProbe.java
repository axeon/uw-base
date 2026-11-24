package uw.task.rabbit;

import java.util.concurrent.atomic.AtomicInteger;

import com.rabbitmq.client.amqp.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties;
import uw.task.TaskData;
import uw.task.converter.TaskMessageConverter;
import uw.task.demo.EchoRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 1.0 消费+settle 连续性验证（显式触发：-Damqp.bench=true）：发 initialCredits×3 条消息，断言全部被消费
 * （settle 失效会在 credits 耗尽后停摆，收不满）。直接用 Rabbit100Ops 建监听 + 发送。
 */
@EnabledIfSystemProperty(named = "amqp.bench", matches = "true")
class ConsumeSettleProbe {

    static final String QUEUE = "uw-bench.settle";
    static final int CREDITS = 5;
    static final int COUNT = 15;

    @Test
    void consumeBeyondCredits() throws Exception {
        RabbitProperties props = new RabbitProperties();
        props.setHost(System.getProperty("uw.task.rabbitmq.host", "10.0.0.10"));
        props.setPort(Integer.parseInt(System.getProperty("uw.task.rabbitmq.port", "5672")));
        props.setUsername(System.getProperty("uw.task.rabbitmq.username", "rabbit"));
        props.setPassword(System.getProperty("uw.task.rabbitmq.password"));
        AtomicInteger consumed = new AtomicInteger();
        Rabbit100Ops ops = new Rabbit100Ops(props, true);
        try {
            ops.declareDurableQueue(QUEUE, true);
            ops.purgeQueue(QUEUE);
            // 与 ElasticConsumerGroup.onMessage 同款 settle 语义：处理完成后 accept
            Consumer consumer = ops.getConnection().consumerBuilder()
                    .queue(QUEUE)
                    .initialCredits(CREDITS)
                    .messageHandler((ctx, msg) -> {
                        consumed.incrementAndGet();
                        ctx.accept();
                    }).build();
            Message message = new TaskMessageConverter().toMessage(TaskData.<String, String>builder(EchoRunner.class, "s").build(), new MessageProperties());
            for (int i = 0; i < COUNT; i++) {
                ops.send(QUEUE, message, 0, e -> {
                });
            }
            long deadline = System.currentTimeMillis() + 15000;
            while (consumed.get() < COUNT && System.currentTimeMillis() < deadline) {
                Thread.sleep(200);
            }
            consumer.close();
            assertEquals(COUNT, consumed.get(), "credits=" + CREDITS + " 发 " + COUNT + " 条应全部消费（settle 生效）");
            System.out.println("SETTLE-PROBE OK: consumed=" + consumed.get());
        } finally {
            ops.purgeQueue(QUEUE);
            ops.shutdown();
        }
    }

    /**
     * 未执行即退回的语义核实（classic 与 quorum 各一遍）：首次投递调 {@code requeue()}（AMQP 1.0 released），
     * 消息应重新入队并被再次投递，不丢、不进死信；第二次投递 accept 后队列清空。对应 ElasticConsumerGroup.onMessage
     * 等待并发许可被中断时的处理。
     */
    @Test
    void requeueRedeliversWithoutLoss() throws Exception {
        for (boolean quorum : new boolean[]{false, true}) {
            String queue = QUEUE + ".requeue." + (quorum ? "quorum" : "classic");
            RabbitProperties props = new RabbitProperties();
            props.setHost(System.getProperty("uw.task.rabbitmq.host", "10.0.0.10"));
            props.setPort(Integer.parseInt(System.getProperty("uw.task.rabbitmq.port", "5672")));
            props.setUsername(System.getProperty("uw.task.rabbitmq.username", "rabbit"));
            props.setPassword(System.getProperty("uw.task.rabbitmq.password"));
            AtomicInteger deliveries = new AtomicInteger();
            Rabbit100Ops ops = new Rabbit100Ops(props, true);
            try {
                ops.declareDurableQueue(queue, quorum);
                ops.purgeQueue(queue);
                Consumer consumer = ops.getConnection().consumerBuilder()
                        .queue(queue)
                        .initialCredits(1)
                        .messageHandler((ctx, msg) -> {
                            if (deliveries.incrementAndGet() == 1) {
                                ctx.requeue();
                            } else {
                                ctx.accept();
                            }
                        }).build();
                Thread.sleep(1000);
                Message message = new TaskMessageConverter().toMessage(TaskData.<String, String>builder(EchoRunner.class, "r").build(), new MessageProperties());
                ops.send(queue, message, 0, e -> {
                });
                long deadline = System.currentTimeMillis() + 15000;
                // 等到第二次投递被 accept、队列清空
                while (System.currentTimeMillis() < deadline && !(deliveries.get() >= 2 && ops.getQueueInfo(queue)[0] == 0)) {
                    Thread.sleep(200);
                }
                consumer.close();
                int[] info = ops.getQueueInfo(queue);
                assertEquals(2, deliveries.get(), quorum ? "quorum" : "classic" + "：requeue 后应恰好再投递一次");
                assertEquals(0, info[0], quorum ? "quorum" : "classic" + "：第二次投递 accept 后队列应清空");
                System.out.println("REQUEUE-PROBE " + (quorum ? "quorum" : "classic") + ": deliveries=" + deliveries.get() + ", ready=" + info[0]);
            } finally {
                ops.purgeQueue(queue);
                ops.shutdown();
            }
        }
    }
}
