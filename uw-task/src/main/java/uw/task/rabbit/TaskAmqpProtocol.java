package uw.task.rabbit;

/**
 * AMQP 协议版本：uw-task 整库运行期唯一选定，启动期确定后不再切换。
 * <p>091 = AMQP 0-9-1（spring-rabbit，全云托管公共分母）；100 = AMQP 1.0（spring-rabbitmq-client，自建 RabbitMQ 4.x 推荐）。</p>
 *
 * @author axeon
 */
public enum TaskAmqpProtocol {

    /**
     * AMQP 0-9-1：默认保守路径，兼容全部托管/自建环境。
     */
    AMQP_091,

    /**
     * AMQP 1.0：RabbitMQ 4.x 与 0-9-1 同端口协议头协商（无需独立监听端口）；4.4+ 可用原生延迟消息。
     */
    AMQP_100;

    public static TaskAmqpProtocol fromConfig(String value) {
        if (value == null || value.isBlank() || "auto".equalsIgnoreCase(value.trim())) {
            return null;
        }
        if ("100".equals(value.trim()) || "1.0".equalsIgnoreCase(value.trim()) || "amqp10".equalsIgnoreCase(value.trim())) {
            return AMQP_100;
        }
        return AMQP_091;
    }
}
