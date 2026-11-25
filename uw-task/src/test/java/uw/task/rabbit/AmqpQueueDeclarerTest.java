package uw.task.rabbit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 队列形态不一致的判定（不连 MQ）：091 与 1.0 两种报错文案都要认，异常链上任意一层匹配即可；其他声明失败不认。
 */
class AmqpQueueDeclarerTest {

    /**
     * 形态不一致（含被包装在异常链里）判为 true；其他声明失败判为 false。
     */
    @Test
    void detectsQueueTypeMismatchOnly() {
        Exception direct = new RuntimeException("reply-text=PRECONDITION_FAILED - inequivalent arg 'x-queue-type' for queue 'q'");
        Exception wrapped = new IllegalStateException("declare failed", new RuntimeException("Channel closed; PRECONDITION_FAILED - inequivalent arg 'x-queue-type'"));
        // 1.0 协议（RabbitAmqpAdmin）的报错文案：HTTP 409，没有 PRECONDITION_FAILED 字样
        Exception amqp100 = new RuntimeException("Unexpected response code: 409 instead of 200, 201 (message: 'inequivalent arg 'x-queue-type' for queue 'q' in vhost '/': received 'quorum' but current value is 'classic'')");
        assertThat(AmqpQueueDeclarer.isQueueTypeMismatch(direct)).isTrue();
        assertThat(AmqpQueueDeclarer.isQueueTypeMismatch(amqp100)).isTrue();
        assertThat(AmqpQueueDeclarer.isQueueTypeMismatch(wrapped)).isTrue();
        assertThat(AmqpQueueDeclarer.isQueueTypeMismatch(new RuntimeException("PRECONDITION_FAILED - inequivalent arg 'durable'"))).isFalse();
        assertThat(AmqpQueueDeclarer.isQueueTypeMismatch(new RuntimeException("Connection refused"))).isFalse();
        assertThat(AmqpQueueDeclarer.isQueueTypeMismatch(new RuntimeException((String) null))).isFalse();
    }
}
