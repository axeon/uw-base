package uw.task.rabbit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 协议决策（不连 MQ）：默认 091，仅对端不提供 0-9-1 而提供 1.0 时走 1.0；版本判定独立于协议。
 */
class RabbitCapabilityProbeDecisionTest {

    /**
     * 两种协议都可用（RabbitMQ 4.x 的常态）：走 091。
     */
    @Test
    void bothAvailable_defaultsTo091() {
        assertThat(RabbitCapabilityProbe.decideProtocol(true, true)).isEqualTo(TaskAmqpProtocol.AMQP_091);
    }

    /**
     * 只有 1.0（只开 1.0 的托管 MQ）：走 1.0。
     */
    @Test
    void only100Available_uses100() {
        assertThat(RabbitCapabilityProbe.decideProtocol(false, true)).isEqualTo(TaskAmqpProtocol.AMQP_100);
    }

    /**
     * 都不可用（启动时 MQ 不可达）或只有 091：按 091。
     */
    @Test
    void noneOrOnly091_uses091() {
        assertThat(RabbitCapabilityProbe.decideProtocol(false, false)).isEqualTo(TaskAmqpProtocol.AMQP_091);
        assertThat(RabbitCapabilityProbe.decideProtocol(true, false)).isEqualTo(TaskAmqpProtocol.AMQP_091);
    }

    /**
     * 原生延迟只看版本：091 路径同样判定可用。
     */
    @Test
    void nativeDelay_dependsOnVersionNotProtocol() {
        assertThat(RabbitCapabilityProbe.versionAtLeast("4.4.1", new int[]{4, 4, 0})).isTrue();
        assertThat(RabbitCapabilityProbe.versionAtLeast("3.13.7", new int[]{4, 4, 0})).isFalse();
        assertThat(RabbitCapabilityProbe.versionAtLeast(null, new int[]{4, 4, 0})).isFalse();
    }
}
