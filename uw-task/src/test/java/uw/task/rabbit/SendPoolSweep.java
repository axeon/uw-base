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
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties;

/**
 * AMQP 1.0 发送连接数扫描的独立可执行程序（不依赖测试框架，可部署到目标网络就近运行）。
 *
 * <p>用法：{@code java -cp "classes:lib/*" uw.task.rabbit.SendPoolSweep}，参数经环境变量：</p>
 * <ul>
 *   <li>{@code UW_MQ_HOST}/{@code UW_MQ_PORT}/{@code UW_MQ_USERNAME}/{@code UW_MQ_PASSWORD}：RabbitMQ 连接（口令只经环境变量传入）；</li>
 *   <li>{@code UW_SWEEP_POOLS}：连接数档位，默认 {@value #DEFAULT_POOLS}；</li>
 *   <li>{@code UW_SWEEP_COUNT}：每轮条数，默认 {@value #DEFAULT_COUNT}；{@code UW_SWEEP_ROUNDS}：轮数，默认 {@value #ROUNDS}。</li>
 * </ul>
 * <p>口径同 {@code AmqpClientLibraryBench}：单线程 fire-and-forget、230B 消息、classic 队列、
 * 从首条发出到队列积压达到 N 条计为端到端入队吞吐。测试队列与同名 exchange 在结束时删除。</p>
 */
public final class SendPoolSweep {

    static final String DEFAULT_POOLS = "1,2,4,8,16";
    static final int DEFAULT_COUNT = 100000;
    static final int ROUNDS = 3;
    static final int WARMUP = 2000;
    static final byte[] BODY = ("x".repeat(229) + "y").getBytes(StandardCharsets.US_ASCII);
    static final String QUEUE_PREFIX = "uw-bench-sweep.";

    private SendPoolSweep() {
    }

    /**
     * 入口。
     *
     * @param args 未使用
     * @throws Exception 任何运行异常
     */
    public static void main(String[] args) throws Exception {
        String host = env("UW_MQ_HOST", "127.0.0.1");
        int port = Integer.parseInt(env("UW_MQ_PORT", "5672"));
        String username = env("UW_MQ_USERNAME", "rabbit");
        String password = System.getenv("UW_MQ_PASSWORD");
        // UW_SWEEP_DELETE=a,b：只删除这些队列及同名 exchange 后退出（清理残留测试队列）
        String delete = System.getenv("UW_SWEEP_DELETE");
        if (delete != null && !delete.isBlank()) {
            String[] names = Arrays.stream(delete.split(",")).map(String::trim).toArray(String[]::new);
            delete(host, port, username, password, names);
            for (String queue : names) {
                System.out.println("SWEEP-DELETED " + queue);
            }
            return;
        }
        int[] pools = Arrays.stream(env("UW_SWEEP_POOLS", DEFAULT_POOLS).split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
        int count = Integer.parseInt(env("UW_SWEEP_COUNT", String.valueOf(DEFAULT_COUNT)));

        RabbitProperties props = new RabbitProperties();
        props.setHost(host);
        props.setPort(port);
        props.setUsername(username);
        props.setPassword(password);
        Rabbit091Ops ops091 = new Rabbit091Ops(props, 20, true);
        Environment env = new AmqpEnvironmentBuilder().build();
        List<Publisher[]> pool = new ArrayList<>();
        for (int i = 0; i < pools.length; i++) {
            Publisher[] publishers = new Publisher[pools[i]];
            for (int j = 0; j < pools[i]; j++) {
                Connection connection = env.connectionBuilder().host(host).port(port).username(username).password(password).build();
                publishers[j] = connection.publisherBuilder().build();
            }
            pool.add(publishers);
            ops091.declareDurableQueue(queue(pools[i]), false);
        }
        System.out.printf("SWEEP host=%s:%s count=%d rounds=%d pools=%s%n", host, port, count, ROUNDS, Arrays.toString(pools));
        for (int i = 0; i < pools.length; i++) {
            run(ops091, queue(pools[i]), pool.get(i), WARMUP);
        }
        long[][] samples = new long[pools.length][ROUNDS];
        for (int round = 0; round < ROUNDS; round++) {
            for (int k = 0; k < pools.length; k++) {
                int i = (k + round) % pools.length;
                samples[i][round] = run(ops091, queue(pools[i]), pool.get(i), count);
            }
        }
        for (int i = 0; i < pools.length; i++) {
            Arrays.sort(samples[i]);
            System.out.printf("SWEEP-RESULT connections=%d msg/s=%d rounds=%s%n", pools[i], samples[i][ROUNDS / 2], Arrays.toString(samples[i]));
        }
        env.close();
        ops091.shutdown();
        cleanup(host, port, username, password, pools);
    }

    static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    static String queue(int connections) {
        return QUEUE_PREFIX + "c" + connections;
    }

    /**
     * 跑一轮并返回端到端入队吞吐（msg/s）。
     */
    static long run(Rabbit091Ops ops091, String queue, Publisher[] publishers, int count) throws Exception {
        ops091.purgeQueue(queue);
        AtomicInteger rotator = new AtomicInteger();
        long t0 = System.nanoTime();
        for (int i = 0; i < count; i++) {
            Publisher publisher = publishers[(rotator.getAndIncrement() & 0x7fffffff) % publishers.length];
            publisher.publish(publisher.message(BODY).toAddress().queue(queue).message(), null);
        }
        long deadline = System.currentTimeMillis() + 120_000;
        while (true) {
            int[] info = ops091.getQueueInfo(queue);
            if (info != null && info[0] >= count || System.currentTimeMillis() > deadline) {
                break;
            }
            Thread.sleep(5);
        }
        long totalMs = Math.max(1, (System.nanoTime() - t0) / 1_000_000);
        Thread.sleep(300);
        ops091.purgeQueue(queue);
        return count * 1000L / totalMs;
    }

    /**
     * 删除测试队列与同名 exchange。
     */
    static void cleanup(String host, int port, String username, String password, int[] pools) {
        delete(host, port, username, password, Arrays.stream(pools).mapToObj(SendPoolSweep::queue).toArray(String[]::new));
    }

    /**
     * 删除指定的队列及同名 exchange（其他测试清理测试资源时复用）。
     *
     * @param rabbitProperties rabbit 连接配置
     * @param names            队列名列表
     */
    static void delete(RabbitProperties rabbitProperties, String... names) {
        delete(rabbitProperties.getHost(), rabbitProperties.getPort(), rabbitProperties.getUsername(), rabbitProperties.getPassword(), names);
    }

    static void delete(String host, int port, String username, String password, String... names) {
        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);
        RabbitAdmin admin = new RabbitAdmin(factory);
        for (String name : names) {
            try {
                admin.deleteQueue(name);
                admin.deleteExchange(name);
            } catch (Exception e) {
                System.out.println("SWEEP cleanup failed: " + name + " " + e.getMessage());
            }
        }
        factory.destroy();
    }
}
