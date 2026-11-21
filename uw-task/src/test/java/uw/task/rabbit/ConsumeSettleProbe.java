package uw.task.rabbit;
import com.rabbitmq.client.amqp.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import uw.task.TaskData;
import uw.task.converter.TaskMessageConverter;
/**
 * 1.0 消费+settle 连续性验证（显式触发）：发 initialCredits×3 条消息，断言全部被消费
 * （settle 失效会在 credits 耗尽后停摆，收不满）。需要本地已启动的完整上下文，经 AmqpProtocolBench 场景容器简化：
 * 直接用 Rabbit100Ops 建监听 + 发送。
 */
@EnabledIfSystemProperty(named = "amqp.bench", matches = "true")
class ConsumeSettleProbe {
    @org.junit.jupiter.api.Test
    void consumeBeyondCredits() throws Exception {
        org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties props = new org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties();
        props.setHost(System.getProperty("uw.task.rabbitmq.host", "10.0.0.10"));
        props.setPort(Integer.parseInt(System.getProperty("uw.task.rabbitmq.port", "30772")));
        props.setUsername(System.getProperty("uw.task.rabbitmq.username", "rabbit"));
        props.setPassword(System.getProperty("uw.task.rabbitmq.password", "rabbit44eval"));
        Rabbit100Ops ops = new Rabbit100Ops(props, "settle-probe", true);
        ops.declareDurableQueue("uw-bench.settle", true);
        java.util.concurrent.atomic.AtomicInteger consumed = new java.util.concurrent.atomic.AtomicInteger();
        com.rabbitmq.client.amqp.Consumer consumer = ops.getConnection().consumerBuilder()
                .queue("uw-bench.settle")
                .initialCredits(5)
                .messageHandler((ctx, msg) -> {
                    consumed.incrementAndGet();
                    ctx.accept(); // 与 ElasticConsumerGroup.onMessage 同款 settle 语义
                }).build();
        Message m = new TaskMessageConverter().toMessage(TaskData.<String, String>builder(uw.task.demo.EchoRunner.class, "s").build(), new MessageProperties());
        for (int i = 0; i < 15; i++) {
            ops.send("uw-bench.settle", m, 0);
        }
        long deadline = System.currentTimeMillis() + 15000;
        while (consumed.get() < 15 && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }
        consumer.close();
        ops.purgeQueue("uw-bench.settle");
        ops.shutdown();
        org.junit.jupiter.api.Assertions.assertEquals(15, consumed.get(), "credits=5 发 15 条应全部消费（settle 生效）");
        System.out.println("SETTLE-PROBE OK: consumed=" + consumed.get());
    }
}
