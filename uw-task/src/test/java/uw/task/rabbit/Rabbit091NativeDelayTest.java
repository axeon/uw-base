package uw.task.rabbit;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 0-9-1 原生延迟消息头（不连 MQ）：RabbitMQ 4.4 的 0-9-1 契约是 header {@code x-opt-delivery-time} 携带 Long 型绝对时间戳毫秒。
 */
class Rabbit091NativeDelayTest {

    /**
     * 写入的头必须是 Long（非整型字面量装箱的 Integer），取值=当前时刻+延迟。
     */
    @Test
    void deliveryTimeHeader_isLongEpochMillisPlusDelay() {
        Message message = new Message(new byte[0], new MessageProperties());
        long before = System.currentTimeMillis();
        Rabbit091Ops.setDeliveryTime(message, 60000);
        Object header = message.getMessageProperties().getHeaders().get(TaskAmqpOps.DELIVERY_TIME_KEY);
        assertThat(header).isInstanceOf(Long.class);
        assertThat((Long) header).isGreaterThanOrEqualTo(before + 60000).isLessThan(System.currentTimeMillis() + 60000);
    }
}
