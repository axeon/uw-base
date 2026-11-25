package uw.task.rabbit;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties;
import uw.task.container.TaskRunnerContainer;
import uw.task.exception.TaskRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 协议无关的 AMQP 操作面：uw-task 唯一的 MQ 收发/声明/监听通道。
 * <p>091 实现基于 spring-rabbit（CachingConnectionFactory+RabbitTemplate+RabbitAdmin），
 * 100 实现基于 spring-rabbitmq-client（RabbitAmqpTemplate+RabbitAmqpAdmin+原生延迟 annotation）。
 * 上层（TaskFactory/TaskSendCenter/TaskRunnerContainer）只面向本接口编程，协议选择在启动期一次定死。</p>
 *
 * <p>成员排列约定（接口与两个实现类一致）：常量 → 字段 → 构造器 → 按本接口声明顺序排列的实例方法
 * （supportsNativeDelay、send、sendAndReceive、declareDurableQueue、createListener、getQueueInfo、purgeQueue、
 * isEmptyWithoutConsumers、deleteQueue、getProtocol、shutdown）→ 包内辅助方法 → 私有辅助方法 → 嵌套类型。</p>
 *
 * @author axeon
 */
public interface TaskAmqpOps {

    /**
     * RPC 同步等待上限与 RPC 请求消息过期时间（毫秒）。091 构造期设为模板 replyTimeout，1.0 作为 future 等待上限。
     */
    long RPC_TIMEOUT_MILLIS = 180_000L;

    /**
     * 未配置 MQ（仅定时任务档，操作面为 null）时，入队/消费/远程 RPC/延时提交的统一失败文案。
     */
    String MQ_NOT_CONFIGURED = "MQ is not configured (uw.task.rabbitmq.host or uw.task.rabbitmq.addresses): queue send, consume, RPC and delay are unavailable";

    /**
     * 取可用的 MQ 操作面；仅定时任务档（未配置 MQ，操作面为 null）直接抛异常快速失败。
     *
     * @param ops 协议操作面（可能为 null）
     * @return 传入的操作面
     * @throws TaskRuntimeException 操作面为 null
     */
    static TaskAmqpOps requireConfigured(TaskAmqpOps ops) {
        if (ops == null) {
            throw new TaskRuntimeException(MQ_NOT_CONFIGURED);
        }
        return ops;
    }

    /**
     * 原生延迟消息头/注解 key（RabbitMQ 4.4 契约：值为绝对 Unix 时间戳毫秒）。
     */
    String DELIVERY_TIME_KEY = "x-opt-delivery-time";

    /**
     * 本操作面是否支持 MQ 原生延迟消息（RabbitMQ ≥4.4，x-opt-delivery-time，0-9-1 与 1.0 都支持）。
     * <p>延时任务路由的单一事实源：{@code TaskSendCenter.sendToQueue} 据此判定 delay&gt;0 走 MQ 原生延迟
     * 还是自动转 Redis zset 搬运（TaskDelayTransferContainer）。能力自述内聚在操作面上，
     * 路由判定与协议实现不可能错位（低版本 broker → 延时任务走 zset）。</p>
     *
     * @return 支持返回 true；不支持（低版本 broker）返回 false
     */
    default boolean supportsNativeDelay() {
        return false;
    }

    /**
     * 投递消息到指定队列（exchange 与 queue 同名直连，routing key=队列名，与既有拓扑一致）。
     * <p>失败上报分两种，由实现决定：同步实现（091）失败时直接抛异常；异步实现（1.0）本方法正常返回只代表
     * 消息已交给客户端，broker 拒收时经 {@code onAsyncFailure} 上报（在客户端线程上回调，实现方不得在其中阻塞）。</p>
     *
     * @param queue       目标队列名
     * @param message     消息
     * @param delayMillis 延迟毫秒：&gt;0 且 {@link #supportsNativeDelay()} 为 true 时设置 x-opt-delivery-time
     *                    （消息延迟可见，需 quorum 队列）；≤0 立即投递。不支持原生延迟的操作面不应收到 &gt;0 值
     *                    （路由层已自动转 zset；收到即为调用方绕过路由的 bug，延迟将被丢弃并记错误日志）
     * @param onAsyncFailure 异步实现的失败回调（消息未被 broker 接收）；同步实现不调用
     */
    void send(String queue, Message message, long delayMillis, Consumer<Exception> onAsyncFailure);

    /**
     * 同步 RPC：发送请求并等待回复（最多 {@value #RPC_TIMEOUT_MILLIS} 毫秒，复用 TaskMessageConverter 转换）。
     *
     * @param queue   目标队列名
     * @param message 请求消息（需已设置 messageId，1.0 用作 reply correlation）
     * @return 回复消息；超时返回 null
     */
    Message sendAndReceive(String queue, Message message);

    /**
     * 声明持久化队列 + 同名直连 exchange + 绑定（routing key=队列名，与既有拓扑一致）。
     * 现有同名队列的形态与目标不一致时做有条件迁移（两个方向都迁）：仅当队列已确认无消息且无消费者才删除并重建，否则原样抛出。
     * <p><b>注意</b>：判空用的是待投递消息数，未到期的原生延迟消息不计入——quorum 队列改回 classic 时，队列里尚未到期的延迟消息会随旧队列一起删除。</p>
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
     * @return 清除的消息数；1.0 的管理通道不返回该值，成功恒为 0；失败返回 -1
     */
    int purgeQueue(String queueName);

    /**
     * 队列是否可安全重建：已确认无待投递消息且无消费者（消费者数为 0 即无在途未确认消息）。供队列形态迁移判断，无法确认时返回 false。
     * <p>未到期的原生延迟消息不计入待投递数，判断结果对它们不成立。</p>
     *
     * @param queueName 队列名
     * @return 确认无消息且无消费者返回 true；否则或查询失败返回 false
     */
    boolean isEmptyWithoutConsumers(String queueName);

    /**
     * 删除队列（队列上的绑定随之删除；同名 exchange 保留，重建队列时需重新声明绑定）。仅用于队列形态迁移等运维性场景，不校验队列是否为空。
     *
     * @param queueName 队列名
     * @return 队列存在并已删除返回 true；队列不存在或失败返回 false
     */
    boolean deleteQueue(String queueName);

    /**
     * 本实现运行的协议（启动期探测选定，运行期不变）。
     *
     * @return AMQP 0-9-1 或 AMQP 1.0
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

    /**
     * 取配置的 amqp 端口，未配置时为 5672。
     *
     * @param rabbitProperties rabbit 连接配置
     * @return amqp 端口
     */
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

