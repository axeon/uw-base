package uw.task.rabbit;

import com.rabbitmq.client.AMQP;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.core.task.TaskExecutor;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import uw.task.container.TaskRunnerContainer;
import uw.task.converter.TaskMessageConverter;

import java.time.Duration;

/**
 * AMQP 0-9-1 实现：spring-rabbit 链路（CachingConnectionFactory + RabbitTemplate + RabbitAdmin），
 * 逻辑承自 uw-task 的 UwTaskAutoConfiguration/TaskRunnerContainer，行为对齐。
 *
 * @author axeon
 */
public class Rabbit091Ops implements TaskAmqpOps {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(Rabbit091Ops.class);

    /**
     * RPC 默认超时（毫秒）：与 TaskFactory doRpcCall 的 180s 口径一致。
     */
    static final long RPC_TIMEOUT_MILLIS = 180000L;

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
     * 构建 091 协议操作面：rabbitmq 配置映射为连接工厂（addresses 集群形态透传），
     * 初始化模板与管理器。
     *
     * @param rabbitProperties rabbit 连接配置（uw.task.rabbitmq.*，与 spring.rabbitmq 同构）
     * @param channelCacheSize channel 缓存数（publisher/listener 共享；3k 队列场景由 listener 独立连接消费不受此限）
     */
    public Rabbit091Ops(RabbitProperties rabbitProperties, int channelCacheSize) {
        this.connectionFactory = buildConnectionFactory(rabbitProperties, channelCacheSize);
        this.rabbitTemplate = new RabbitTemplate(connectionFactory);
        this.rabbitTemplate.setMessageConverter(new TaskMessageConverter());
        // replyTimeout 构造期设死：避免每次 sendAndReceive 改共享 template 状态的并发互覆
        this.rabbitTemplate.setReplyTimeout(RPC_TIMEOUT_MILLIS);
        this.rabbitTemplate.afterPropertiesSet();
        this.rabbitAdmin = new RabbitAdmin(connectionFactory);
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
                connFactory.setAddresses(endpoints.stream().map(e -> e.host() + ":" + e.port()).collect(Collectors.joining(",")));
            }
            connFactory.setChannelCacheSize(channelCacheSize);
            connFactory.afterPropertiesSet();
            return connFactory;
        } catch (Exception e) {
            throw new IllegalStateException("Rabbit091Ops 初始化连接工厂失败", e);
        }
    }

    @Override
    public void send(String queue, Message message, long delayMillis) {
        if (delayMillis > 0) {
            // 091 恒不支持原生延迟（supportsNativeDelay()=false）：TaskFactory.sendToQueue 的延时路由
            // 已据此自动转 Redis zset 搬运，本分支正常不可达——收到即调用方绕过路由直发 ops 的 bug，
            // 此处延迟只能丢弃立即投递，记 error 暴露（防静默提前执行）。
            log.error("Rabbit091Ops: 收到延时投递请求但 091 协议不支持原生延迟(delay={}ms)，延迟已丢弃立即投递, queue={}——请检查是否绕过 TaskFactory 路由", delayMillis, queue);
        }
        rabbitTemplate.send(queue, queue, message);
    }

    @Override
    public Message sendAndReceive(String queue, Message message, long timeoutMillis) {
        // timeoutMillis 由 TaskFactory 恒传 RPC_TIMEOUT_MILLIS（180s），构造期已设死，此处直用
        return rabbitTemplate.sendAndReceive(queue, queue, message);
    }

    @Override
    public void declareDurableQueue(String queueName, boolean quorum) {
        TaskAmqpOps.declareTopology(rabbitAdmin, queueName, quorum);
    }

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
        container.setConcurrentConsumers((int) Math.ceil(consumerNum * 0.1f));
        container.setPrefetchCount(prefetchNum);
        container.setConnectionFactory(connectionFactory);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        container.setQueueNames(queueName);
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(runnerContainer, "process");
        listenerAdapter.setMessageConverter(new TaskMessageConverter());
        container.setMessageListener(listenerAdapter);
        // 发生错误不再重新排队，由 TaskRunnerContainer.process 内部 catch 处理
        container.setDefaultRequeueRejected(false);
        container.setAutoStartup(true);
        container.afterPropertiesSet();
        container.start();
        return TaskAmqpListenerContainer.of(container);
    }

    /**
     * 每任务一个虚拟线程的 TaskExecutor（直接 Thread.ofVirtual）。线程名={前缀}{N}。
     * 直接 Thread.ofVirtual。线程名={前缀}{N}。
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

        ThreadPerTaskVirtualExecutor(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void execute(Runnable task) {
            Thread.ofVirtual().name(prefix, index.getAndIncrement()).start(task);
        }
    }

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

    @Override
    public TaskAmqpProtocol getProtocol() {
        return TaskAmqpProtocol.AMQP_091;
    }

    @Override
    public void shutdown() {
        connectionFactory.destroy();
    }
}
