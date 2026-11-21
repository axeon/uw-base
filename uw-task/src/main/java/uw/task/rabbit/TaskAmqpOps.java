package uw.task.rabbit;

import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import uw.task.container.TaskRunnerContainer;

/**
 * 协议无关的 AMQP 操作面：uw-task 唯一的 MQ 收发/声明/监听通道。
 * <p>091 实现基于 spring-rabbit（CachingConnectionFactory+RabbitTemplate+RabbitAdmin），
 * 100 实现基于 spring-rabbitmq-client（RabbitAmqpTemplate+RabbitAmqpAdmin+原生延迟 annotation）。
 * 上层（TaskFactory/TaskRunnerContainer）只面向本接口编程，协议选择在启动期一次定死。</p>
 *
 * @author axeon
 */
public interface TaskAmqpOps {

    /**
     * 本操作面是否支持 MQ 原生延迟消息（1.0 协议 + RabbitMQ ≥4.4，x-opt-delivery-time）。
     * <p>延时任务路由的单一事实源：{@code TaskFactory.sendToQueue} 据此判定 delay&gt;0 走 MQ 原生延迟
     * 还是自动转 Redis zset 搬运（TaskDelayTransferContainer）。能力自述内聚在操作面上，
     * 路由判定与协议实现不可能错位（091 恒 false → 延时任务恒走 zset）。</p>
     *
     * @return 支持返回 true；不支持（091 协议/低版本 broker）返回 false
     */
    default boolean supportsNativeDelay() {
        return false;
    }

    /**
     * 投递消息到指定队列（exchange 与 queue 同名直连，routing key=队列名，与既有拓扑一致）。
     *
     * @param queue       目标队列名
     * @param message     消息
     * @param delayMillis 延迟毫秒：&gt;0 且 {@link #supportsNativeDelay()} 为 true 时设置 x-opt-delivery-time
     *                    （消息延迟可见，需 quorum 队列）；≤0 立即投递。不支持原生延迟的操作面不应收到 &gt;0 值
     *                    （路由层已自动转 zset；收到即为调用方绕过路由的 bug，延迟将被丢弃并记警告）
     */
    void send(String queue, Message message, long delayMillis);

    /**
     * 同步 RPC：发送请求并等待回复（复用 TaskMessageConverter 转换）。
     *
     * @param queue           目标队列名
     * @param message         请求消息（需已设置 messageId，1.0 用作 reply correlation）
     * @param timeoutMillis   超时毫秒
     * @return 回复消息；超时返回 null
     */
    Message sendAndReceive(String queue, Message message, long timeoutMillis);

    /**
     * 声明持久化队列 + 同名直连 exchange + 绑定（routing key=队列名，与既有拓扑一致）。
     *
     * @param queueName 队列名
     * @param quorum    true=quorum 队列；false=classic 队列
     */
    void declareDurableQueue(String queueName, boolean quorum);

    /**
     * 创建并启动该队列的监听容器（回调 TaskRunnerContainer.process）。
     *
     * @param queueName    队列名
     * @param consumerNum  消费并发
     * @param prefetchNum  预取/credit
     * @param runnerContainer 消息回调目标（process 方法）
     * @return 容器句柄
     */
    TaskAmqpListenerContainer createListener(String queueName, int consumerNum, int prefetchNum, TaskRunnerContainer runnerContainer);

    /**
     * 查询队列积压消息数与消费者数。
     *
     * @param queueName 队列名
     * @return [0]=消息数 [1]=消费者数；查询失败返回 null（调用方按无数据跳过）
     */
    int[] getQueueInfo(String queueName);

    /**
     * 清空队列全部消息（运维用）。
     *
     * @param queueName 队列名
     * @return 清除的消息数；失败返回 -1
     */
    int purgeQueue(String queueName);

    /**
     * 本实现运行的协议。
     */
    TaskAmqpProtocol getProtocol();

    /**
     * 释放底层资源（连接工厂等）。
     */
    void shutdown();

    /**
     * 解析后的连接端点（新 Boot 4.2 API，替代已 forRemoval 的 RabbitProperties.determine* 家族）。
     *
     * @param host       主机
     * @param port       端口
     * @param username   用户名（可空）
     * @param password   密码（可空）
     * @param virtualHost vhost（可空，null/空// 视为默认）
     */
    record AmqpEndpoint(String host, int port, String username, String password, String virtualHost) {
    }

    /**
     * 解析 rabbit 连接端点列表：addresses（"host:port" 列表，集群）非空时逐个解析，
     * 否则退回单 host/port。只使用非废弃 API（getAddresses/getHost/getPort/getUsername/getPassword/getVirtualHost）。
     *
     * @param rabbitProperties rabbit 连接配置
     * @return 端点列表（至少 1 个；addresses 未配置时为单元素）
     */
    static List<AmqpEndpoint> resolveEndpoints(RabbitProperties rabbitProperties) {
        String username = rabbitProperties.getUsername();
        String password = rabbitProperties.getPassword();
        String vhost = rabbitProperties.getVirtualHost();
        List<AmqpEndpoint> endpoints = new ArrayList<>();
        List<String> addresses = rabbitProperties.getAddresses();
        if (addresses != null && !addresses.isEmpty()) {
            for (String addr : addresses) {
                String a = addr.trim();
                if (a.isEmpty()) {
                    continue;
                }
                int idx = a.lastIndexOf(':');
                String host = idx > 0 ? a.substring(0, idx) : a;
                int port = idx > 0 ? Integer.parseInt(a.substring(idx + 1)) : defaultPort(rabbitProperties);
                endpoints.add(new AmqpEndpoint(host, port, username, password, vhost));
            }
        }
        if (endpoints.isEmpty()) {
            endpoints.add(new AmqpEndpoint(rabbitProperties.getHost(), defaultPort(rabbitProperties), username, password, vhost));
        }
        return endpoints;
    }

    private static int defaultPort(RabbitProperties rabbitProperties) {
        Integer port = rabbitProperties.getPort();
        return port != null ? port : 5672;
    }

    /**
     * 声明持久化队列 + 同名直连 exchange + 绑定的拓扑组装（两协议逐行相同，仅 admin 实现不同——
     * RabbitAdmin 与 RabbitAmqpAdmin 同实现 AmqpAdmin 接口，声明语义一致）。
     *
     * @param admin     AmqpAdmin（091=RabbitAdmin，100=RabbitAmqpAdmin）
     * @param queueName 队列名
     * @param quorum    true=quorum 队列（x-queue-type）；false=classic
     */
    static void declareTopology(AmqpAdmin admin, String queueName, boolean quorum) {
        QueueBuilder builder = QueueBuilder.durable(queueName);
        if (quorum) {
            builder.withArgument("x-queue-type", "quorum");
        }
        Queue queue = builder.build();
        Exchange exchange = ExchangeBuilder.directExchange(queueName).durable(true).build();
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(queueName).noargs();
        admin.declareQueue(queue);
        admin.declareExchange(exchange);
        admin.declareBinding(binding);
    }
}
