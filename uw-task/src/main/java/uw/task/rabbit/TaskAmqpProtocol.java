package uw.task.rabbit;

/**
 * AMQP 协议版本：uw-task 整库运行期唯一选定，启动期确定后不再切换。
 * <p>091 = AMQP 0-9-1（spring-rabbit，全云托管公共分母）；100 = AMQP 1.0（spring-rabbitmq-client，自建 RabbitMQ 4.x 推荐）。</p>
 *
 * @author axeon
 */
public enum TaskAmqpProtocol {

    /**
     * AMQP 0-9-1：1.0 握手不可用时的回退路径，兼容全部托管/自建环境。
     */
    AMQP_091,

    /**
     * AMQP 1.0：RabbitMQ 4.x 与 0-9-1 同端口协议头协商（无需独立监听端口）；原生延迟消息与协议无关（4.4+ 两种协议都可用）。
     */
    AMQP_100;
}
