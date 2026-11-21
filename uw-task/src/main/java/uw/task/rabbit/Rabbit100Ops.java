package uw.task.rabbit;

import com.rabbitmq.client.amqp.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AMQP 1.0 实现：spring-rabbitmq-client 链路（SingleAmqpConnectionFactory + RabbitAmqpTemplate + RabbitAmqpAdmin）。
 *
 * <p>原生延迟消息：RabbitMQ 4.4+ 在 quorum queue 上支持消息级延迟——发布时设置
 * x-opt-delivery-time message annotation（绝对 Unix 时间戳毫秒），消息到期前对消费者不可见，
 * 到期后自动变为可投递（仍在原队列，无 TTL+DLX 搬运）。</p>
 *
 * @author axeon
 */
public class Rabbit100Ops implements TaskAmqpOps {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(Rabbit100Ops.class);

    /**
     * 原生延迟消息 annotation key（RabbitMQ 4.4 契约：绝对 Unix 时间戳毫秒）。
     */
    private static final String DELIVERY_TIME_ANNOTATION = "x-opt-delivery-time";

    /**
     * 解析集群地址列表：uw.task.rabbitmq.addresses（"h1:p1,h2:p2" 或 URI 形式）。
     * 返回 amqp:// URI 列表（含账号密码与 vhost）；单地址时返回单元素列表（调用方退回 host/port 模式）。
     */
    static List<String> buildClusterUris(RabbitProperties rabbitProperties) {
        List<String> uris = new ArrayList<>();
        // 账号密码/vhost URL 编码：密码含 @:/ 等字符时裸拼会破坏 URI 结构
        for (TaskAmqpOps.AmqpEndpoint endpoint : TaskAmqpOps.resolveEndpoints(rabbitProperties)) {
            String encodedUser = uriEncode(endpoint.username() == null ? "" : endpoint.username());
            String encodedPass = uriEncode(endpoint.password() == null ? "" : endpoint.password());
            String vhost = endpoint.virtualHost();
            String vPath = (vhost == null || vhost.isBlank() || "/".equals(vhost)) ? "" : "/" + uriEncode(vhost);
            uris.add("amqp://" + encodedUser + ":" + encodedPass + "@" + endpoint.host() + ":" + endpoint.port() + vPath);
        }
        return uris;
    }

    private static String uriEncode(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }

    /**
     * 暴露底层连接（评测/消费者等高级用途）。
     */
    Connection getConnection() {
        return connectionFactory.getConnection();
    }

    /**
     * RPC 回复队列前缀（每个应用实例独占：回复必须路由回发起请求的实例）。
     */
    private static final String RPC_REPLY_QUEUE_PREFIX = "uw-task.rpc.reply.";

    /**
     * amqp-client 环境（连接的根资源，destroy 时关闭）。
     */
    private final Environment environment;

    /**
     * 1.0 主连接工厂（RPC 回复队列/管理通道/listener 用）。
     */
    private final SingleAmqpConnectionFactory connectionFactory;
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
     * RPC 回复队列名（实例独占，非 durable，broker 重启即清防跨重启累积残留）。
     */
    private final String replyQueueName;

    /**
     * 发送侧连接池：proton-j2 每连接单 IO 线程，单连接发送上限 ~2万 msg/s（261120 实测，broker 调优后），
     * 多连接近线性扩展（4 连接=6.1万）。发送轮询取用；RPC/管理仍走主连接。
     */
    private final Connection[] sendConnections;
    /**
     * 发送 Publisher 池（每连接一个；构造失败的槽位为 null，nextSendPublisher 兜底）。
     */
    private final Publisher[] sendPublishers;
    /**
     * 发送轮询指针（正数化取模）。
     */
    private final AtomicInteger sendRotator = new AtomicInteger();

    /**
     * @param rabbitProperties rabbit 连接配置（host/port/username/password/virtualHost）
     * @param instanceId       实例标识（appName@appHost:appPort），用于 RPC 回复队列命名
     */
    public Rabbit100Ops(RabbitProperties rabbitProperties, String instanceId, boolean nativeDelayAvailable) {
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
        List<String> uris = buildClusterUris(rabbitProperties);
        boolean clusterMode = rabbitProperties.getAddresses() != null && !rabbitProperties.getAddresses().isEmpty();
        if (clusterMode) {
            factory.setUris(uris.toArray(new String[0]));
            log.info("Rabbit100Ops: 集群模式，{} 个节点 URI 已注入", uris.size());
        } else {
            TaskAmqpOps.AmqpEndpoint primary = TaskAmqpOps.resolveEndpoints(rabbitProperties).get(0);
            factory.setHost(primary.host());
            factory.setPort(primary.port());
            if (primary.virtualHost() != null && !primary.virtualHost().isBlank()) {
                factory.setVirtualHost(primary.virtualHost());
            }
        }
        this.connectionFactory = factory;
        this.template = new RabbitAmqpTemplate(factory);
        this.template.setMessageConverter(new TaskMessageConverter());
        this.admin = new RabbitAmqpAdmin(factory);
        // RPC 回复队列：实例独占（classic，自动删除语义不跨实例共享），声明失败不阻断启动（首次 RPC 时再暴露）
        this.replyQueueName = RPC_REPLY_QUEUE_PREFIX + instanceId.replaceAll("[^A-Za-z0-9._-]", "_");
        this.template.setReplyToQueue(replyQueueName);
        try {
            admin.declareQueue(new Queue(replyQueueName, false, false, true));
        } catch (Exception e) {
            log.warn("Rabbit100Ops: RPC 回复队列[{}]声明失败，将在首次 RPC 时重试: {}", replyQueueName, e.getMessage());
        }
        // 发送连接池（默认 4，-Duw.task.rabbit100.send-pool-size 可调）：单连接 IO 线程是发送吞吐瓶颈，池化近线性扩展
        int poolSize;
        try {
            poolSize = Integer.parseInt(System.getProperty("uw.task.rabbit100.send-pool-size", "4"));
        } catch (NumberFormatException e) {
            log.warn("Rabbit100Ops: send-pool-size 配置非数字，回退默认 4");
            poolSize = 4;
        }
        this.sendConnections = new Connection[poolSize];
        this.sendPublishers = new Publisher[poolSize];
        for (int i = 0; i < poolSize; i++) {
            try {
                ConnectionBuilder builder = environment.connectionBuilder()
                        .username(rabbitProperties.getUsername())
                        .password(rabbitProperties.getPassword());
                if (clusterMode) {
                    builder.uris(uris.toArray(new String[0]));
                } else {
                    TaskAmqpOps.AmqpEndpoint primary = TaskAmqpOps.resolveEndpoints(rabbitProperties).get(0);
                    builder.host(primary.host()).port(primary.port());
                }
                sendConnections[i] = builder.build();
                sendPublishers[i] = sendConnections[i].publisherBuilder().build();
            } catch (Exception e) {
                log.error("Rabbit100Ops: 发送连接池第{}个连接创建失败: {}", i + 1, e.getMessage());
            }
        }
        log.info("Rabbit100Ops: 发送连接池就绪，poolSize={}", poolSize);
    }

    @Override
    public boolean supportsNativeDelay() {
        return nativeDelayAvailable;
    }

    @Override
    public void send(String queue, Message message, long delayMillis) {
        // 走发送池轮询（单连接 IO 线程是吞吐瓶颈）；fire-and-forget（091 对齐，无 confirm 语义）
        Publisher publisher = nextSendPublisher();
        long deliveryTime = delayMillis > 0 ? System.currentTimeMillis() + delayMillis : 0;
        // com.rabbitmq.client.amqp.Message 与 spring-amqp 的 Message 同名冲突，此处保留全限定
        com.rabbitmq.client.amqp.Message amqpMessage = publisher.message()
                .toAddress().queue(queue).message();
        RabbitAmqpUtils.toAmqpMessage(message, amqpMessage);
        if (deliveryTime > 0) {
            amqpMessage.annotation(DELIVERY_TIME_ANNOTATION, deliveryTime);
        }
        publisher.publish(amqpMessage, context -> {
            if (context.status() == Publisher.Status.RELEASED || context.status() == Publisher.Status.REJECTED) {
                log.error("Rabbit100Ops send 失败: queue={}, delay={}, status={}", queue, delayMillis, context.status());
            }
        });
    }

    /**
     * 轮询取发送 Publisher；池未就绪时退回模板的 publisher（保证可用性优先）。
     */
    private Publisher nextSendPublisher() {
        for (int i = 0; i < sendPublishers.length; i++) {
            Publisher p = sendPublishers[(sendRotator.getAndIncrement() & 0x7fffffff) % sendPublishers.length];
            if (p != null) {
                return p;
            }
        }
        return template.getPublisher();
    }


    @Override
    public Message sendAndReceive(String queue, Message message, long timeoutMillis) {
        try {
            return template.sendAndReceive(queue, queue, message).get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            if (e.getCause() instanceof TimeoutException || e instanceof TimeoutException) {
                return null;
            }
            throw new IllegalStateException("Rabbit100Ops RPC 失败: queue=" + queue + ", " + e.getMessage(), e);
        }
    }

    @Override
    public void declareDurableQueue(String queueName, boolean quorum) {
        TaskAmqpOps.declareTopology(admin, queueName, quorum);
    }

    @Override
    public TaskAmqpListenerContainer createListener(String queueName, int consumerNum, int prefetchNum, TaskRunnerContainer runnerContainer) {
        // 自管消费者（不经 spring 容器）：直接经 connection.consumerBuilder() 构建，
        // 支持 pause/unpause 弹性伸缩；消息转换直连 TaskMessageConverter → process，
        // 直连转换+process（不经 spring 适配器层）。
        ElasticConsumerGroup group = new ElasticConsumerGroup(queueName, prefetchNum, runnerContainer);
        group.resize(consumerNum);
        return TaskAmqpListenerContainer.of(group);
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
         * 当前激活并发数。
         */
        private volatile int active;

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
                        .messageHandler(this::onMessage)
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
            if (active != targetCount) {
                log.info("Rabbit100Ops: 队列[{}]消费并发调整为 {} (总建 {})", queueName, targetCount, consumers.size());
            }
            active = targetCount;
        }

        /**
         * 消息入口：信号量封顶执行并发（满时在 dispatching 虚拟线程上 park，零平台成本），
         * 每任务一个虚拟线程（线程名={队列名}-N），执行完成后 settle（credit 释放=背压传导）。
         */
        private void onMessage(Consumer.Context context, com.rabbitmq.client.amqp.Message message) {
            Semaphore semaphore = permits;
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            Thread.ofVirtual().name(queueName + "-", threadIndex.getAndIncrement()).start(() -> {
                try {
                    Message springMessage = RabbitAmqpUtils.fromAmqpMessage(message, null);
                    TaskData<?, ?> taskData = (TaskData<?, ?>) new TaskMessageConverter().fromMessage(springMessage);
                    runnerContainer.process(taskData);
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
         * 当前激活并发数。
         */
        int activeCount() {
            return active;
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

    @Override
    public TaskAmqpProtocol getProtocol() {
        return TaskAmqpProtocol.AMQP_100;
    }

    @Override
    public void shutdown() {
        for (int i = 0; i < sendPublishers.length; i++) {
            try {
                if (sendPublishers[i] != null) sendPublishers[i].close();
            } catch (Exception ignore) {
            }
            try {
                if (sendConnections[i] != null) sendConnections[i].close();
            } catch (Exception ignore) {
            }
        }
        try {
            template.destroy();
        } catch (Exception ignore) {
        }
        try {
            connectionFactory.destroy();
        } catch (Exception ignore) {
        }
        try {
            environment.close();
        } catch (Exception ignore) {
        }
    }
}
