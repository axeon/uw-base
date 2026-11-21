package uw.task.constant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 队列任务队列形态（classic/quorum），对应 TaskRunnerConfig.queueType。
 * 序列化为 OBJECT 形态（含 value/label）。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TaskQueueType {

    /**
     * classic 队列：单副本、吞吐最优（默认）。
     */
    CLASSIC(0, "classic"),

    /**
     * quorum 队列：Raft 复制；原生延迟消息（RabbitMQ 4.4+）仅 quorum 支持。
     */
    QUORUM(1, "quorum");

    /**
     * 对应 TaskRunnerConfig.QUEUE_TYPE_* 常量值。
     */
    private final int value;

    /**
     * 展示名。
     */
    private final String label;

    TaskQueueType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
