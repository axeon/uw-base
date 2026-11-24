package uw.task.rabbit;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.Publisher;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import org.apache.qpid.protonj2.client.Client;
import org.apache.qpid.protonj2.client.ConnectionOptions;
import org.apache.qpid.protonj2.client.DeliveryMode;
import org.apache.qpid.protonj2.client.Message;
import org.apache.qpid.protonj2.client.Sender;
import org.apache.qpid.protonj2.client.SenderOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.amqp.client.AmqpClient;
import org.springframework.amqp.client.SingleAmqpConnectionFactory;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AMQP 1.0 两套客户端库的发送吞吐对照（显式触发：-Damqp.bench=true -Dtest=AmqpClientLibraryBench）。
 *
 * <p>对照四档，全部 fire-and-forget（不等 broker 确认）、单线程发送、230B 消息、classic 队列：</p>
 * <ul>
 *   <li>rabbit-1：RabbitMQ 官方 amqp-client（Netty），1 连接——Rabbit100Ops 的单连接形态；</li>
 *   <li>rabbit-4：同上，4 连接轮询——多连接形态对照；</li>
 *   <li>proton-1：proton-j2 原生客户端，1 连接；</li>
 *   <li>spring-1：spring-amqp-client（通用库，封装 proton-j2），1 连接。</li>
 * </ul>
 * <p>计时口径：从首条发出到队列积压达到 N 条（经 091 passive declare 轮询），即端到端入队吞吐；
 * 另记"发布调用返回"耗时看调用侧是否被流控阻塞。每档跑 {@value ROUNDS} 轮取中位数，轮间交替顺序。</p>
 */
@EnabledIfSystemProperty(named = "amqp.bench", matches = "true")
class AmqpClientLibraryBench {

    static final int COUNT = Integer.parseInt(System.getProperty("amqp.bench.count", "50000"));
    static final int WARMUP = 2000;
    static final List<String> QUEUES = List.of("uw-bench-lib.rabbit1", "uw-bench-lib.rabbit2", "uw-bench-lib.rabbit4", "uw-bench-lib.rabbit8",
            "uw-bench-lib.rabbit16", "uw-bench-lib.proton", "uw-bench-lib.spring");
    static final int ROUNDS = 3;
    static final byte[] BODY = ("x".repeat(229) + "y").getBytes(StandardCharsets.US_ASCII);

    static RabbitProperties props;
    static Rabbit091Ops ops091;
    static Environment rabbitEnv;
    static Client protonClient;

    @BeforeAll
    static void setUp() {
        props = new RabbitProperties();
        props.setHost(System.getProperty("uw.task.rabbitmq.host", "10.0.0.10"));
        props.setPort(Integer.parseInt(System.getProperty("uw.task.rabbitmq.port", "5672")));
        props.setUsername(System.getProperty("uw.task.rabbitmq.username", "rabbit"));
        props.setPassword(System.getProperty("uw.task.rabbitmq.password"));
        ops091 = new Rabbit091Ops(props, 50, true);
        rabbitEnv = new AmqpEnvironmentBuilder().build();
        protonClient = Client.create();
        for (String queue : QUEUES) {
            ops091.declareDurableQueue(queue, false);
            ops091.purgeQueue(queue);
        }
    }

    @AfterAll
    static void tearDown() {
        if (ops091 != null) {
            ops091.shutdown();
        }
    }

    /**
     * 一档被测实现：队列名 + 发送器句柄列表（单连接档为 1 个元素）+ 单条发送动作。
     */
    record Tier(String name, String queue, List<Object> handles, Send send) {

        /**
         * 单条发送动作（允许抛受检异常）。
         */
        @FunctionalInterface
        interface Send {

            /**
             * 发送第 i 条。
             *
             * @param i 序号
             * @throws Exception 客户端异常
             */
            void send(int i) throws Exception;
        }
    }

    static List<Tier> tiers() {
        List<Tier> list = new ArrayList<>();
        // 连接数档位可扫（-Damqp.bench.pools=1,2,4,8,16）；-Damqp.bench.libs=true 时追加两套对照库
        for (String size : System.getProperty("amqp.bench.pools", "1,4").split(",")) {
            int n = Integer.parseInt(size.trim());
            list.add(tier("rabbit-" + n, "uw-bench-lib.rabbit" + n, n));
        }
        if (Boolean.getBoolean("amqp.bench.libs")) {
            list.add(protonTier());
            list.add(springTier());
        }
        return list;
    }

    static Tier tier(String name, String queue, int connections) {
        List<Object> handles = new ArrayList<>();
        Publisher[] publishers = new Publisher[connections];
        try {
            for (int i = 0; i < connections; i++) {
                Connection connection = rabbitEnv.connectionBuilder()
                        .host(props.getHost()).port(props.getPort())
                        .username(props.getUsername()).password(props.getPassword()).build();
                handles.add(connection);
                Publisher publisher = connection.publisherBuilder().build();
                handles.add(publisher);
                publishers[i] = publisher;
            }
        } catch (Exception e) {
            throw new IllegalStateException("rabbit 客户端连接失败: " + name, e);
        }
        AtomicInteger rotator = new AtomicInteger();
        return new Tier(name, queue, handles, i -> publishers[(rotator.getAndIncrement() & 0x7fffffff) % connections]
                .publish(publishers[0].message(BODY).toAddress().queue(queue).message(), null));
    }

    static Tier protonTier() {
        try {
            org.apache.qpid.protonj2.client.Connection connection = protonClient.connect(props.getHost(), props.getPort(),
                    new ConnectionOptions().user(props.getUsername()).password(props.getPassword()));
            Sender sender = connection.openSession().openSender("/queues/" + "uw-bench-lib.proton",
                    new SenderOptions().deliveryMode(DeliveryMode.AT_MOST_ONCE));
            return new Tier("proton-1", "uw-bench-lib.proton", List.of(connection), i -> sender.send(Message.create(BODY)));
        } catch (Exception e) {
            throw new IllegalStateException("proton-j2 连接失败", e);
        }
    }

    static Tier springTier() {
        SingleAmqpConnectionFactory factory = new SingleAmqpConnectionFactory()
                .setHost(props.getHost()).setPort(props.getPort()).setUsername(props.getUsername()).setPassword(props.getPassword());
        AmqpClient client = AmqpClient.builder(factory)
                .senderOptions(options -> options.deliveryMode(DeliveryMode.AT_MOST_ONCE))
                .defaultToAddress("/queues/" + "uw-bench-lib.spring")
                .build();
        Message<byte[]> message = Message.create(BODY);
        return new Tier("spring-1", "uw-bench-lib.spring", List.of(client, factory), i -> client.send(message));
    }

    @Test
    void compareSendThroughput() throws Exception {
        List<Tier> tiers = tiers();
        assertTrue(RabbitCapabilityProbe.probe(props).isAmqp100Available(), "broker 须支持 AMQP 1.0");
        for (Tier tier : tiers) {
            run(tier, WARMUP);
        }
        long[][] samples = new long[tiers.size()][ROUNDS];
        long[][] callReturned = new long[tiers.size()][ROUNDS];
        for (int round = 0; round < ROUNDS; round++) {
            // 轮间交替起跑顺序，削掉"先跑的档吃掉 broker 缓存红利"
            for (int k = 0; k < tiers.size(); k++) {
                Tier tier = tiers.get((k + round) % tiers.size());
                long[] r = run(tier, COUNT);
                samples[tiers.indexOf(tier)][round] = r[0];
                callReturned[tiers.indexOf(tier)][round] = r[1];
            }
        }
        for (int i = 0; i < tiers.size(); i++) {
            Arrays.sort(samples[i]);
            Arrays.sort(callReturned[i]);
            System.out.printf("LIB-BENCH %-9s msg/s=%d (median of %d; rounds %s) | 发布调用返回耗时ms=%d%n",
                    tiers.get(i).name(), samples[i][ROUNDS / 2], ROUNDS, Arrays.toString(samples[i]), callReturned[i][ROUNDS / 2]);
        }
    }

    /**
     * @return [0]=端到端入队吞吐（msg/s）；[1]=N 条发布调用全部返回的耗时（毫秒）
     */
    long[] run(Tier tier, int count) throws Exception {
        ops091.purgeQueue(tier.queue);
        long t0 = System.nanoTime();
        for (int i = 0; i < count; i++) {
            tier.send().send(i);
        }
        long callMs = (System.nanoTime() - t0) / 1_000_000;
        long deadline = System.currentTimeMillis() + 120_000;
        long queuedAt;
        while (true) {
            int[] info = ops091.getQueueInfo(tier.queue);
            queuedAt = System.currentTimeMillis();
            if (info != null && info[0] >= count || queuedAt > deadline) {
                break;
            }
            Thread.sleep(10);
        }
        long totalMs = queuedAt * 1_000_000 < t0 ? 1 : Math.max(1, (System.nanoTime() - t0) / 1_000_000);
        Thread.sleep(500);
        ops091.purgeQueue(tier.queue);
        return new long[]{count * 1000L / totalMs, callMs};
    }
}
