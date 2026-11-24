package uw.task.rabbit;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties;
import org.springframework.core.task.TaskExecutor;
import uw.task.container.TaskRunnerContainer;
import uw.task.converter.TaskMessageConverter;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * AMQP 0-9-1 实现：spring-rabbit 链路（CachingConnectionFactory + RabbitTemplate + RabbitAdmin），
 * 与 1.0 实现（Rabbit100Ops）行为对齐（含原生延迟：RabbitMQ 4.4+ 的 quorum 队列同样接受 0-9-1 消息头 x-opt-delivery-time）。
 *
 * <p>成员排列与 {@link TaskAmqpOps} 一致（见其类注释的约定）。</p>
 *
 * @author axeon
 */
public class Rabbit091Ops implements TaskAmqpOps {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(Rabbit091Ops.class);

    /**
     * 消息转换器（无状态，全类共用）。
     */
    private static final TaskMessageConverter CONVERTER = new TaskMessageConverter();

    /**
     * 091 专用缓存连接工厂（channelCacheSize 由构造参数设定）。
     */
    private final CachingConnectionFactory connectionFactory;

    /**
     * 发送/RPC 模板（TaskMessageConverter 转换，replyTimeout 构造期设死）。
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 队列/交换机/绑定声明与查询管理器。
     */
    private final RabbitAdmin rabbitAdmin;

    /**
     * 原生延迟消息是否可用（探测结果：broker ≥4.4）。
     */
    private final boolean nativeDelayAvailable;

    /**
     * 构建 091 协议操作面：rabbitmq 配置映射为连接工厂（addresses 集群形态透传），
     * 初始化模板与管理器。
     *
     * @param rabbitProperties     rabbit 连接配置（uw.task.rabbitmq.*，与 spring.rabbitmq 同构）
     * @param channelCacheSize     channel 缓存数（publisher/listener 共同一连接的 channel 缓存上限）
     * @param nativeDelayAvailable 原生延迟是否可用（探测结果：broker ≥4.4）
     */
    public Rabbit091Ops(RabbitProperties rabbitProperties, int channelCacheSize, boolean nativeDelayAvailable) {
        this.nativeDelayAvailable = nativeDelayAvailable;
        this.connectionFactory = buildConnectionFactory(rabbitProperties, channelCacheSize);
        this.rabbitTemplate = new RabbitTemplate(connectionFactory);
        this.rabbitTemplate.setMessageConverter(CONVERTER);
        // replyTimeout 构造期设死：避免每次 sendAndReceive 改共享 template 状态的并发互覆
        this.rabbitTemplate.setReplyTimeout(RPC_TIMEOUT_MILLIS);
        this.rabbitTemplate.afterPropertiesSet();
        this.rabbitAdmin = new RabbitAdmin(connectionFactory);
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
     * <p>同步投递：模板 send 返回即已写入连接，失败直接抛异常，不调用 {@code onAsyncFailure}。
     * {@code delayMillis &gt; 0} 时写入原生延迟消息头（需 quorum 队列）。</p>
     */
    @Override
    public void send(String queue, Message message, long delayMillis, Consumer<Exception> onAsyncFailure) {
        // 091 模板 send 是同步的：失败直接抛异常，不走异步失败回调
        if (delayMillis > 0) {
            if (!nativeDelayAvailable) {
                // 低版本 broker：TaskSendCenter.sendToQueue 的延时路由已据 supportsNativeDelay() 自动转 Redis zset 搬运，
                // 本分支正常不可达——收到即调用方绕过路由直发 ops 的 bug，延迟只能丢弃立即投递，记 error 暴露（防静默提前执行）。
                log.error("Rabbit091Ops: 收到延时投递请求但 broker 版本不支持原生延迟(delay={}ms)，延迟已丢弃立即投递, queue={}——请检查是否绕过 TaskFactory 路由", delayMillis, queue);
            } else {
                setDeliveryTime(message, delayMillis);
            }
        }
        rabbitTemplate.send(queue, queue, message);
    }

    /**
     * {@inheritDoc}
     * <p>走 RabbitTemplate 的 Direct Reply-To：不建回复队列；等待上限=构造期设死的 {@value TaskAmqpOps#RPC_TIMEOUT_MILLIS} 毫秒，超时返回 null。</p>
     */
    @Override
    public Message sendAndReceive(String queue, Message message) {
        // 等待上限=构造期设死的 replyTimeout（RPC_TIMEOUT_MILLIS）
        return rabbitTemplate.sendAndReceive(queue, queue, message);
    }

    /**
     * {@inheritDoc}
     * <p>经 RabbitAdmin 声明。</p>
     */
    @Override
    public void declareDurableQueue(String queueName, boolean quorum) {
        TaskAmqpOps.declareTopology(rabbitAdmin, queueName, quorum);
    }

    /**
     * {@inheritDoc}
     * <p>SimpleMessageListenerContainer（自管，不注册为 bean）：最大并发=consumerNum、初始并发见 {@link #minConsumers}，
     * 预取=prefetchNum，自动确认；消费者线程与任务执行都在虚拟线程上（线程名={队列名}-N）。</p>
     */
    @Override
    public TaskAmqpListenerContainer createListener(String queueName, int consumerNum, int prefetchNum, TaskRunnerContainer runnerContainer) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        // 消费者线程虚拟化：basicConsume 长连接等待 + process 内的 IO/限速 sleep 都 park 在虚拟线程上。
        // 每消费者一个 Thread.ofVirtual（线程名={队列名}-N，与 1.0 ElasticConsumerGroup 命名一致）
        container.setTaskExecutor(new ThreadPerTaskVirtualExecutor(queueName + "-"));
        container.setStartConsumerMinInterval(100);
        container.setConsecutiveActiveTrigger(1);
        container.setStopConsumerMinInterval(20000);
        container.setConsecutiveIdleTrigger(3);
        container.setMaxConcurrentConsumers(consumerNum);
        container.setConcurrentConsumers(minConsumers(consumerNum));
        container.setPrefetchCount(prefetchNum);
        container.setConnectionFactory(connectionFactory);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        container.setQueueNames(queueName);
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(runnerContainer, TaskRunnerContainer.PROCESS_METHOD);
        listenerAdapter.setMessageConverter(CONVERTER);
        container.setMessageListener(listenerAdapter);
        // 发生错误不再重新排队，由 TaskRunnerContainer.process 内部 catch 处理
        container.setDefaultRequeueRejected(false);
        container.setAutoStartup(true);
        container.afterPropertiesSet();
        container.start();
        return TaskAmqpListenerContainer.of(container);
    }

    /**
     * {@inheritDoc}
     * <p>经 passive declare 读取队列属性。</p>
     */
    @Override
    public int[] getQueueInfo(String queueName) {
        try {
            AMQP.Queue.DeclareOk declareOk = rabbitTemplate.execute(channel -> channel.queueDeclarePassive(queueName));
            return new int[]{declareOk.getMessageCount(), declareOk.getConsumerCount()};
        } catch (Exception e) {
            log.warn("Rabbit091Ops 查询队列信息失败 {}: {}", queueName, e.getMessage());
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * <p>返回 RabbitMQ 实际清掉的消息数。</p>
     */
    @Override
    public int purgeQueue(String queueName) {
        try {
            AMQP.Queue.PurgeOk purged = rabbitTemplate.execute(channel -> channel.queuePurge(queueName));
            return purged == null ? -1 : purged.getMessageCount();
        } catch (Exception e) {
            log.warn("Rabbit091Ops 清空队列失败 {}: {}", queueName, e.getMessage());
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TaskAmqpProtocol getProtocol() {
        return TaskAmqpProtocol.AMQP_091;
    }

    /**
     * {@inheritDoc}
     * <p>关闭缓存连接工厂（其上的监听容器与模板随之失效）。</p>
     */
    @Override
    public void shutdown() {
        connectionFactory.destroy();
    }

    /**
     * 拉模式接收一条消息（basicGet，自动确认），最多等待给定毫秒；无消息返回 null。仅测试探针使用。
     *
     * @param queueName 队列名
     * @param timeoutMs 等待毫秒
     * @return 消息；超时无消息返回 null
     */
    Message receiveForTest(String queueName, int timeoutMs) {
        return rabbitTemplate.receive(queueName, timeoutMs);
    }

    /**
     * 写入原生延迟消息头：值为绝对 Unix 时间戳毫秒（必须传 Long，RabbitMQ 4.4 的 0-9-1 契约）。
     *
     * @param message     消息
     * @param delayMillis 延迟毫秒（&gt;0）
     */
    static void setDeliveryTime(Message message, long delayMillis) {
        message.getMessageProperties().setHeader(DELIVERY_TIME_KEY, System.currentTimeMillis() + delayMillis);
    }

    /**
     * 初始（最小）消费者数：max 的 10% 上取整，至少 1——低负载时少占连接，按需弹性增至 consumerNum。
     *
     * @param consumerNum 最大消费并发
     * @return 初始消费者数
     */
    static int minConsumers(int consumerNum) {
        return Math.max(1, (int) Math.ceil(consumerNum * 0.1f));
    }

    /**
     * 构建 091 连接工厂：单节点用 host/port；集群（addresses 多节点）时 setAddresses 注入地址列表
     * （RabbitMQ 客户端原生地址洗牌 + 连接故障转移）。
     *
     * @param rabbitProperties rabbit 连接配置（uw.task.rabbitmq.*）
     * @param channelCacheSize channel 缓存数
     * @return 已初始化的缓存连接工厂
     */
    private static CachingConnectionFactory buildConnectionFactory(RabbitProperties rabbitProperties, int channelCacheSize) {
        PropertyMapper map = PropertyMapper.get();
        RabbitConnectionFactoryBean factoryBean = new RabbitConnectionFactoryBean();
        List<TaskAmqpOps.AmqpEndpoint> endpoints = TaskAmqpOps.resolveEndpoints(rabbitProperties);
        TaskAmqpOps.AmqpEndpoint primary = endpoints.get(0);
        factoryBean.setHost(primary.host());
        factoryBean.setPort(primary.port());
        map.from(primary::username).to(factoryBean::setUsername);
        map.from(primary::password).to(factoryBean::setPassword);
        if (primary.virtualHost() != null && !primary.virtualHost().isBlank()) {
            factoryBean.setVirtualHost(primary.virtualHost());
        }
        map.from(rabbitProperties::getRequestedHeartbeat).asInt(Duration::getSeconds).to(factoryBean::setRequestedHeartbeat);
        RabbitProperties.Ssl ssl = rabbitProperties.getSsl();
        if (ssl.getEnabled() != null && ssl.getEnabled()) {
            factoryBean.setUseSSL(true);
            map.from(ssl::getAlgorithm).to(factoryBean::setSslAlgorithm);
        }
        map.from(rabbitProperties::getConnectionTimeout).asInt(Duration::toMillis).to(factoryBean::setConnectionTimeout);
        try {
            factoryBean.afterPropertiesSet();
            ConnectionFactory rawFactory = factoryBean.getObject();
            if (rawFactory == null) {
                throw new IllegalStateException("RabbitConnectionFactoryBean.getObject() returned null");
            }
            CachingConnectionFactory connFactory = new CachingConnectionFactory(rawFactory);
            if (endpoints.size() > 1) {
                connFactory.setAddresses(String.join(",", endpoints.stream().map(e -> e.host() + ":" + e.port()).toList()));
            }
            connFactory.setChannelCacheSize(channelCacheSize);
            connFactory.afterPropertiesSet();
            return connFactory;
        } catch (Exception e) {
            throw new IllegalStateException("Rabbit091Ops failed to init connection factory", e);
        }
    }

    /**
     * 每任务一个虚拟线程的 TaskExecutor（直接 Thread.ofVirtual）。线程名={前缀}{N}。
     */
    private static final class ThreadPerTaskVirtualExecutor implements TaskExecutor {

        /**
         * 线程名前缀（{队列名}-）。
         */
        private final String prefix;

        /**
         * 线程编号。
         */
        private final AtomicInteger index = new AtomicInteger();

        /**
         * 构建执行器。
         *
         * @param prefix 线程名前缀
         */
        ThreadPerTaskVirtualExecutor(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void execute(Runnable task) {
            Thread.ofVirtual().name(prefix, index.getAndIncrement()).start(task);
        }
    }
}
