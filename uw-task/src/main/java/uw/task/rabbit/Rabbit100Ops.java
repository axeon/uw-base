package uw.task.rabbit;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Consumer;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.Publisher;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpAdmin;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpUtils;
import org.springframework.amqp.rabbitmq.client.SingleAmqpConnectionFactory;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties;
import uw.task.TaskData;
import uw.task.container.TaskRunnerContainer;
import uw.task.converter.TaskMessageConverter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AMQP 1.0 实现：spring-rabbitmq-client 链路（SingleAmqpConnectionFactory + RabbitAmqpTemplate + RabbitAmqpAdmin）。
 *
 * <p>原生延迟消息：RabbitMQ 4.4+ 在 quorum queue 上支持消息级延迟——发布时设置
 * x-opt-delivery-time message annotation（绝对 Unix 时间戳毫秒；0-9-1 侧见 Rabbit091Ops），消息到期前对消费者不可见，
 * 到期后自动变为可投递（仍在原队列，无 TTL+DLX 搬运）。</p>
 *
 * <p>成员排列与 {@link TaskAmqpOps} 一致（见其类注释的约定）。</p>
 *
 * @author axeon
 */
public class Rabbit100Ops implements TaskAmqpOps {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(Rabbit100Ops.class);

    /**
     * 消息转换器（无状态，全类共用）。
     */
    private static final TaskMessageConverter CONVERTER = new TaskMessageConverter();

    /**
     * 官方客户端判定"请求超时"的异常文案（{@code AmqpRequester} 抛的是不带子类型的 {@code AmqpException}，
     * 只能按文案识别；客户端升级若改文案，此处需同步）。
     */
    private static final String RPC_REQUEST_TIMED_OUT = "Request timed out";

    /**
     * amqp-client 环境（连接的根资源，destroy 时关闭）。
     */
    private final Environment environment;

    /**
     * 1.0 主连接工厂（发送/RPC/管理/listener 共用的单 Connection，官方形态：并发靠 link 复用）。
     */
    private final SingleAmqpConnectionFactory connectionFactory;
    /**
     * 发送/管理模板（TaskMessageConverter 转换；发送与 RPC 回复共用其长驻 publisher，RPC 走官方 sendAndReceive 路径）。
     */
    final RabbitAmqpTemplate template;

    /**
     * 队列声明/绑定/属性查询管理器（经 AMQP 连接内的 $management 通道）。
     */
    private final RabbitAmqpAdmin admin;

    /**
     * 原生延迟消息可用（探测期判定：broker ≥4.4）：延时任务路由的单一事实源。
     */
    private final boolean nativeDelayAvailable;

    /**
     * 构建 1.0 协议操作面：单 Connection（官方形态），按 addresses 是否配置决定单节点或集群 URI。
     *
     * @param rabbitProperties     rabbit 连接配置（host/port/addresses/username/password/virtualHost）
     *
     * @param nativeDelayAvailable 原生延迟是否可用（探测结果：broker ≥4.4）
     */
    public Rabbit100Ops(RabbitProperties rabbitProperties, boolean nativeDelayAvailable) {
        this.nativeDelayAvailable = nativeDelayAvailable;
        // dispatching executor 用虚拟线程（默认是 newFixedThreadPool(CPU核) 平台池，全连接共享，
        // 是 1.0 消费并发天花板）：与 091 的虚拟线程消费者模型对齐，WorkPool 仍保证消费者间公平调度
        this.environment = new AmqpEnvironmentBuilder()
                .dispatchingExecutor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
        // RabbitMQ 4.x 的 AMQP 1.0 与 0-9-1 同端口协议头协商（无独立 5673），直连业务配置的 amqp 端口。
        // 集群：uw.task.rabbitmq.addresses 配多节点（h1:5672,h2:5672,...）时走 uris()，客户端内置
        // AddressSelector 轮询 + 连接 Recovery 自动故障转移；单节点退回 host/port。
        SingleAmqpConnectionFactory factory = new SingleAmqpConnectionFactory(environment)
                .setUsername(rabbitProperties.getUsername())
                .setPassword(rabbitProperties.getPassword());
        List<TaskAmqpOps.AmqpEndpoint> endpoints = TaskAmqpOps.resolveEndpoints(rabbitProperties);
        List<String> uris = buildClusterUris(endpoints);
        boolean clusterMode = endpoints.size() > 1;
        if (clusterMode) {
            factory.setUris(uris.toArray(new String[0]));
            log.info("Rabbit100Ops: 集群模式，{} 个节点 URI 已注入", uris.size());
        } else {
            TaskAmqpOps.AmqpEndpoint primary = endpoints.get(0);
            factory.setHost(primary.host());
            factory.setPort(primary.port());
            if (primary.virtualHost() != null && !primary.virtualHost().isBlank()) {
                factory.setVirtualHost(primary.virtualHost());
            }
        }
        this.connectionFactory = factory;
        this.template = new RabbitAmqpTemplate(factory);
        this.template.setMessageConverter(CONVERTER);
        this.admin = new RabbitAmqpAdmin(factory);
        // RPC 官方模板路径的等待上限（模板默认 60 秒）：publishTimeout 兼作 Requester 的 requestTimeout，
        // completionTimeout 是 future 完成上限，两者与框架的 RPC_TIMEOUT_MILLIS 对齐
        this.template.setPublishTimeout(Duration.ofMillis(RPC_TIMEOUT_MILLIS));
        this.template.setCompletionTimeout(Duration.ofMillis(RPC_TIMEOUT_MILLIS));
    }

    /**
     * {@inheritDoc}
     * <p>探测期一次判定（broker ≥4.4），运行期不变。</p>
     */
    @Override
    public boolean supportsNativeDelay() {
        return nativeDelayAvailable;
    }

    /**
     * {@inheritDoc}
     * <p>fire-and-forget 投递：经模板长驻 Publisher（单连接 link 复用，官方形态）发出，本方法返回只代表消息已交给客户端。
     * broker 拒收（RELEASED/REJECTED）时经 {@code onAsyncFailure} 上报——回调在客户端 IO 线程上执行，只做转交、不阻塞。
     * 连接建立等同步失败仍直接抛异常。
     * <p>类型说明：{@code java.util.function.Consumer} 与本类已 import 的 amqp {@code Consumer} 同名冲突，
     * 参数类型保留全限定；局部变量 {@code com.rabbitmq.client.amqp.Message} 与 spring-amqp 的 {@code Message} 同名冲突，同理。</p>
     * <p>{@code delayMillis &gt; 0} 时写入原生延迟消息注解（需 quorum 队列）。</p>
     */
    @Override
    public void send(String queue, Message message, long delayMillis, java.util.function.Consumer<Exception> onAsyncFailure) {
        Publisher publisher = template.getPublisher();
        long deliveryTime = delayMillis > 0 ? System.currentTimeMillis() + delayMillis : 0;
        com.rabbitmq.client.amqp.Message amqpMessage = publisher.message()
                .toAddress().queue(queue).message();
        RabbitAmqpUtils.toAmqpMessage(message, amqpMessage);
        if (deliveryTime > 0) {
            amqpMessage.annotation(DELIVERY_TIME_KEY, deliveryTime);
        }
        publisher.publish(amqpMessage, context -> {
            if (context.status() == Publisher.Status.RELEASED || context.status() == Publisher.Status.REJECTED) {
                onAsyncFailure.accept(new IllegalStateException("broker refused message: status=" + context.status()));
            }
        });
    }

    /**
     * {@inheritDoc}
     * <p>1.0 走官方模板路径：模板每次调用新建并关闭一个 Requester（Direct Reply-To），各多一次网络往返。</p>
     */
    @Override
    public Message sendAndReceive(String queue, Message message) {
        try {
            return template.sendAndReceive(queue, queue, message).get(RPC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            if (isRpcTimeout(e)) {
                return null;
            }
            throw new IllegalStateException("Rabbit100Ops RPC failed: queue=" + queue + ", " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>经 RabbitAmqpAdmin（AMQP 连接内的 $management 通道）声明。</p>
     */
    @Override
    public void declareDurableQueue(String queueName, boolean quorum) {
        TaskAmqpOps.declareTopology(admin, queueName, quorum);
    }

    /**
     * {@inheritDoc}
     * <p>自管消费者组（见 {@link ElasticConsumerGroup}）：不经 spring 容器，直接经 connection.consumerBuilder() 构建；
     * 消费并发=consumerNum，每消费者 credit=prefetchNum，任务在虚拟线程上执行（线程名={队列名}-N）；
     * 带 reply-to 的请求（RPC）由消费者组回复结果。</p>
     */
    @Override
    public TaskAmqpListenerContainer createListener(String queueName, int consumerNum, int prefetchNum, TaskRunnerContainer runnerContainer) {
        // 自管消费者（不经 spring 容器）：直接经 connection.consumerBuilder() 构建，
        // 支持 pause/unpause 弹性伸缩；消息经 TaskMessageConverter 直连 process（不经 spring 适配器层——
        // MessagingMessageListenerAdapter 是注解驱动监听专用，直连构造会静默丢任务），
        // RPC 请求（带 reply-to）由 ElasticConsumerGroup.reply 回结果。
        ElasticConsumerGroup group = new ElasticConsumerGroup(queueName, prefetchNum, runnerContainer);
        group.resize(consumerNum);
        return TaskAmqpListenerContainer.of(group);
    }

    /**
     * {@inheritDoc}
     * <p>1.0 没有 passive declare：经 $management 通道查询队列属性。</p>
     */
    @Override
    public int[] getQueueInfo(String queueName) {
        // 1.0 无 passive declare；经 AMQP 连接内的 $management 通道查询队列属性
        try {
            Properties props = admin.getQueueProperties(queueName);
            if (props == null) {
                return null;
            }
            Object count = props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
            Object consumers = props.get(RabbitAdmin.QUEUE_CONSUMER_COUNT);
            return new int[]{count == null ? 0 : Integer.parseInt(count.toString()),
                    consumers == null ? 0 : Integer.parseInt(consumers.toString())};
        } catch (Exception e) {
            log.warn("Rabbit100Ops 查询队列信息失败 {}: {}", queueName, e.getMessage());
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * <p>管理通道不返回清除条数，成功恒为 0。</p>
     */
    @Override
    public int purgeQueue(String queueName) {
        try {
            admin.purgeQueue(queueName, false);
            return 0;
        } catch (Exception e) {
            log.warn("Rabbit100Ops 清空队列失败 {}: {}", queueName, e.getMessage());
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TaskAmqpProtocol getProtocol() {
        return TaskAmqpProtocol.AMQP_100;
    }

    /**
     * {@inheritDoc}
     * <p>依次关闭模板、主连接工厂与 amqp-client 环境；任一步失败不阻断后续回收。</p>
     */
    @Override
    public void shutdown() {
        try {
            template.destroy();
        } catch (Exception ignore) {
            // 关闭失败不阻断后续回收
        }
        try {
            connectionFactory.destroy();
        } catch (Exception ignore) {
            // 关闭失败不阻断后续回收
        }
        try {
            environment.close();
        } catch (Exception ignore) {
            // 关闭失败不阻断后续回收
        }
    }

    /**
     * 暴露底层连接（测试探针直建消费者用，框架内部不调用）。
     *
     * @return 1.0 主连接
     */
    Connection getConnection() {
        return connectionFactory.getConnection();
    }

    /**
     * 把全部解析端点拼成 amqp:// URI 列表（含 URL 编码后的账号密码与 vhost）。
     *
     * @param endpoints 已解析的连接端点
     *
     * @return 与端点一一对应的 amqp:// URI 列表
     */
    static List<String> buildClusterUris(List<TaskAmqpOps.AmqpEndpoint> endpoints) {
        List<String> uris = new ArrayList<>();
        // 账号密码/vhost URL 编码：密码含 @:/ 等字符时裸拼会破坏 URI 结构
        for (TaskAmqpOps.AmqpEndpoint endpoint : endpoints) {
            String encodedUser = uriEncode(endpoint.username() == null ? "" : endpoint.username());
            String encodedPass = uriEncode(endpoint.password() == null ? "" : endpoint.password());
            String vhost = endpoint.virtualHost();
            String vPath = (vhost == null || vhost.isBlank() || "/".equals(vhost)) ? "" : "/" + uriEncode(vhost);
            uris.add("amqp://" + encodedUser + ":" + encodedPass + "@" + endpoint.host() + ":" + endpoint.port() + vPath);
        }
        return uris;
    }

    /**
     * 是否 RPC 等待超时（等待上限到期，或客户端按 requestTimeout 判定请求超时）。
     *
     * @param e sendAndReceive 抛出的异常（ExecutionException 取其 cause）
     *
     * @return 超时返回 true
     */
    private static boolean isRpcTimeout(Exception e) {
        Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
        return cause instanceof TimeoutException || RPC_REQUEST_TIMED_OUT.equals(cause.getMessage());
    }

    /**
     * URL 编码（UTF-8）。
     *
     * @param raw 原始字符串
     *
     * @return URL 编码后的字符串
     */
    private static String uriEncode(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }

    /**
     * 弹性消费者组：自管 N 个 AMQP Consumer，消费并发=consumerNum（091 语义对齐）。
     * <p>执行模型=Semaphore + 每任务一个虚拟线程（线程名={队列名}-N，与 091 消费者线程命名一致，
     * 运维按队列定位线程）：1.0 的消息分发经 credit 窗口可超前投递多条，若不限执行并发会放大到
     * consumerNum×credits，Semaphore=consumerNum 把执行并发收敛回 consumerNum（与 091 等价）；
     * 满时提交线程（dispatching 虚拟线程）在 acquire 上 park，零平台成本。</p>
     * <p>settle 必须在执行完成后（finally 中 context.accept()）——credit 持有至 handler 结束，
     * 背压传导 broker。</p>
     */
    final class ElasticConsumerGroup {

        /**
         * 目标队列名。
         */
        private final String queueName;

        /**
         * 每 consumer 的初始 credit（=prefetchNum，在途未确认窗口/缓冲）。
         */
        private final int initialCredits;

        /**
         * 消息回调目标（TaskRunnerContainer.process）。
         */
        private final TaskRunnerContainer runnerContainer;

        /**
         * 执行并发许可（=consumerNum）：满时提交线程 park（dispatching 虚拟线程上，零平台成本）。
         */
        private volatile Semaphore permits;

        /**
         * 执行线程编号（{队列名}-N 命名）。
         */
        private final AtomicInteger threadIndex = new AtomicInteger();

        /**
         * 已建消费者（收缩=pause，扩张=unpause/新建）。
         */
        private final List<Consumer> consumers = new CopyOnWriteArrayList<>();

        /**
         * 构建弹性消费者组。
         *
         * @param queueName       目标队列名
         *
         * @param initialCredits   每 consumer 的初始 credit（=prefetchNum）
         * @param runnerContainer 消息回调目标（process 方法）
         */
        ElasticConsumerGroup(String queueName, int initialCredits, TaskRunnerContainer runnerContainer) {
            this.queueName = queueName;
            this.initialCredits = initialCredits;
            this.runnerContainer = runnerContainer;
        }

        /**
         * 调整并发：重建 Semaphore + 消费者 pause/unpause/新建。
         * <p>旧 Semaphore 不关闭——在途任务 release 到旧实例无副作用（新许可立即生效于后续提交）。</p>
         *
         * @param targetCount 目标并发数（consumerNum）
         */
        synchronized void resize(int targetCount) {
            if (targetCount < 1) {
                targetCount = 1;
            }
            permits = new Semaphore(targetCount);
            while (consumers.size() < targetCount) {
                Consumer consumer = connectionFactory.getConnection().consumerBuilder()
                        .queue(queueName)
                        .initialCredits(initialCredits)
                        // 此处以 lambda 桥接：amqp Message 与 spring Message 同名，lambda 参数类型推断可避免内联全限定名
                        .messageHandler((context, message) -> onMessage(context, RabbitAmqpUtils.fromAmqpMessage(message, null)))
                        .build();
                consumers.add(consumer);
            }
            for (int i = 0; i < consumers.size(); i++) {
                Consumer consumer = consumers.get(i);
                if (i < targetCount) {
                    consumer.unpause();
                } else {
                    consumer.pause();
                }
            }
            log.info("Rabbit100Ops: 队列[{}]消费并发调整为 {} (总建 {})", queueName, targetCount, consumers.size());
        }

        /**
         * 消息入口：信号量封顶执行并发（满时在 dispatching 虚拟线程上 park，零平台成本），
         * 每任务一个虚拟线程（线程名={队列名}-N），执行完成后 settle（credit 释放=背压传导）。
         *
         * @param context 消费者上下文（settle 用）
         * @param message 消息
         */
        private void onMessage(Consumer.Context context, Message message) {
            Semaphore semaphore = permits;
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // 未执行：requeue 把消息退回队列重新投递（与 091 未确认消息重新入队同语义），
                // 同时归还 credit——不做 settle 的话该消息会悬挂并永久占用一个 credit 位
                context.requeue();
                return;
            }
            Thread.ofVirtual().name(queueName + "-", threadIndex.getAndIncrement()).start(() -> {
                try {
                    TaskData<?, ?> taskData = (TaskData<?, ?>) CONVERTER.fromMessage(message);
                    TaskData<?, ?> result = runnerContainer.process(taskData);
                    // 带回复地址的请求即 RPC（调用方在 sendAndReceive 上等待）：把执行结果按 messageId 关联回给调用方
                    if (result != null && message.getMessageProperties().getReplyTo() != null) {
                        reply(message, result);
                    }
                } catch (Exception ex) {
                    log.error("Rabbit100Ops: 消息处理异常, queue={}: {}", queueName, ex.getMessage(), ex);
                } finally {
                    semaphore.release();
                    // settle 在执行完成后：credit 持有至 handler 结束（背压），异常也确认防消息悬挂
                    try {
                        context.accept();
                    } catch (Exception ignore) {
                    }
                }
            });
        }

        /**
         * RPC 回复：结果消息按请求的 reply-to 地址发出、correlation-id 取请求的 messageId（调用方模板据此匹配等待中的请求）。
         *
         * @param request 请求消息（需带 reply-to 与 messageId）
         * @param result  执行结果（process 的返回值）
         */
        private void reply(Message request, TaskData<?, ?> result) {
            try {
                Publisher publisher = template.getPublisher();
                com.rabbitmq.client.amqp.Message reply = publisher.message();
                RabbitAmqpUtils.toAmqpMessage(CONVERTER.toMessage(result, new MessageProperties()), reply);
                reply.correlationId(request.getMessageProperties().getMessageId());
                reply.to(request.getMessageProperties().getReplyTo());
                publisher.publish(reply, context -> {
                    if (context.status() == Publisher.Status.RELEASED || context.status() == Publisher.Status.REJECTED) {
                        log.error("Rabbit100Ops RPC 回复发布失败: queue={}, status={}", queueName, context.status());
                    }
                });
            } catch (Exception e) {
                // 回复失败调用方会等到超时；此处留痕便于排查
                log.error("Rabbit100Ops RPC 回复异常: queue={}", queueName, e);
            }
        }

        /**
         * 关闭全部消费者（在途任务的 VT 自行跑完，无需关闭信号量）。
         */
        void shutdown() {
            for (Consumer consumer : consumers) {
                try {
                    consumer.close();
                } catch (Exception ignore) {
                }
            }
            consumers.clear();
        }
    }
}
