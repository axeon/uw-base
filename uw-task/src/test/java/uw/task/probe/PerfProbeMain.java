package uw.task.probe;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties;
import uw.common.util.RedisConnectionFactoryBuilder;
import uw.task.TaskCroner;
import uw.task.TaskData;
import uw.task.TaskListenerManager;
import uw.task.TaskRunner;
import uw.task.api.TaskApiClient;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.conf.UwTaskProperties;
import uw.task.container.TaskCronerContainer;
import uw.task.container.TaskDelayTransferContainer;
import uw.task.container.TaskRunnerContainer;
import uw.task.container.TaskSendCenter;
import uw.task.entity.TaskContact;
import uw.task.entity.TaskCronerConfig;
import uw.task.entity.TaskCronerLog;
import uw.task.entity.TaskRunnerConfig;
import uw.task.entity.TaskRunnerLog;
import uw.task.rabbit.Rabbit091Ops;
import uw.task.rabbit.Rabbit100Ops;
import uw.task.rabbit.TaskAmqpOps;
import uw.task.util.TaskLocalRateLimiter;
import uw.task.util.TaskSequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * dev 上 091 / 1.0 的队列任务与定时任务性能对照（独立可执行程序，不依赖测试框架）。
 *
 * <p>用法：{@code java -cp "classes:test-classes:lib/*" uw.task.probe.PerfProbeMain}，参数经环境变量：</p>
 * <ul>
 *   <li>{@code UW_MQ_HOST/PORT/USERNAME/PASSWORD}：RabbitMQ（口令只经环境变量传入）；</li>
 *   <li>{@code UW_PROBE_REDIS_HOST/PORT/PASSWORD/DATABASE}：任务 id 发号用的 Redis；</li>
 *   <li>{@code UW_PERF_COUNT}：每轮消息数（默认 50000）；{@code UW_PERF_CONSUMERS}：消费并发（默认 4）；
 *       {@code UW_PERF_PREFETCH}：预取数（默认 5）；{@code UW_PERF_CRONERS}：定时任务配置数（默认 200）；</li>
 *   <li>{@code UW_PERF_QUEUE_TYPES}：只测指定队列形态，逗号分隔（默认 classic,quorum）；
 *       {@code UW_PERF_RUN_CRON=false}：跳过定时任务一段（只测队列）。</li>
 * </ul>
 *
 * <p>队列任务口径（走真实 TaskSendCenter / TaskRunnerContainer / Kryo 转换 / Redis 取号，任务体为计数）：
 * 入队=从首条 sendToQueue 到队列积压达到 N 条；消费=从启动监听到任务体被调用 N 次。协议实现直接构造
 * （不走启动期自动探测——探测在 4.x 上恒选 1.0），classic 与 quorum 队列各测，3 轮取中位数、轮间轮换顺序。</p>
 * <p>定时任务口径：N 个每秒触发的配置连续跑 10 秒，统计触发次数覆盖率与触发时刻相对计划时刻的延迟；
 * 定时任务调度不经过 MQ，三个档位（无 MQ / 091 / 1.0 连接常驻）各跑一遍以印证这一点。</p>
 * <p>测试队列与同名 exchange 在结束时删除。</p>
 */
public final class PerfProbeMain {

    static final int COUNT = Integer.parseInt(env("UW_PERF_COUNT", "50000"));
    static final int CONSUMERS = Integer.parseInt(env("UW_PERF_CONSUMERS", "4"));
    static final int PREFETCH = Integer.parseInt(env("UW_PERF_PREFETCH", "5"));
    static final int CRONERS = Integer.parseInt(env("UW_PERF_CRONERS", "200"));
    static final boolean RUN_CRON = Boolean.parseBoolean(env("UW_PERF_RUN_CRON", "true"));
    static final int ROUNDS = 3;
    static final long POLL_MS = 5;

    private PerfProbeMain() {
    }

    /**
     * 入口。
     *
     * @param args 未使用
     * @throws Exception 任何运行异常
     */
    public static void main(String[] args) throws Exception {
        RabbitProperties mq = new RabbitProperties();
        mq.setHost(env("UW_MQ_HOST", "127.0.0.1"));
        mq.setPort(Integer.parseInt(env("UW_MQ_PORT", "5672")));
        mq.setUsername(env("UW_MQ_USERNAME", "rabbit"));
        mq.setPassword(System.getenv("UW_MQ_PASSWORD"));
        DataRedisProperties redis = new DataRedisProperties();
        redis.setHost(env("UW_PROBE_REDIS_HOST", "127.0.0.1"));
        redis.setPort(Integer.parseInt(env("UW_PROBE_REDIS_PORT", "6379")));
        redis.setDatabase(Integer.parseInt(env("UW_PROBE_REDIS_DATABASE", "15")));
        redis.setPassword(System.getenv("UW_PROBE_REDIS_PASSWORD"));
        TaskSequence.init(RedisConnectionFactoryBuilder.build(redis, null));
        System.out.printf("PERF-SETUP java=%s, mq=%s:%d, count=%d, consumers=%d, prefetch=%d, croners=%d%n",
                System.getProperty("java.version"), mq.getHost(), mq.getPort(), COUNT, CONSUMERS, PREFETCH, CRONERS);

        // ---------- 队列任务 ----------
        String[] protocols = {"amqp091", "amqp1.0"};
        String[] queueTypes = env("UW_PERF_QUEUE_TYPES", "classic,quorum").split(",");
        List<String> queues = new ArrayList<>();
        TaskAmqpOps ops091 = new Rabbit091Ops(mq, 50, true);
        TaskAmqpOps ops100 = new Rabbit100Ops(mq, true);
        Map<String, TaskAmqpOps> opsByProtocol = Map.of("amqp091", ops091, "amqp1.0", ops100);
        List<String[]> combos = new ArrayList<>();
        for (String protocol : protocols) {
            for (String queueType : queueTypes) {
                combos.add(new String[]{protocol, queueType});
            }
        }
        // results[combo][round] = {enqueue msg/s, consume msg/s}
        long[][][] results = new long[combos.size()][ROUNDS][2];
        for (int round = 0; round < ROUNDS; round++) {
            for (int k = 0; k < combos.size(); k++) {
                String[] combo = combos.get((k + round) % combos.size());
                long[] rates = queueRound(opsByProtocol.get(combo[0]), combo[0], combo[1], queues, round + 1);
                results[(k + round) % combos.size()][round] = rates;
            }
        }
        for (int i = 0; i < combos.size(); i++) {
            long[] enq = new long[ROUNDS];
            long[] con = new long[ROUNDS];
            for (int r = 0; r < ROUNDS; r++) {
                enq[r] = results[i][r][0];
                con[r] = results[i][r][1];
            }
            Arrays.sort(enq);
            Arrays.sort(con);
            System.out.printf("PERF-SUMMARY-QUEUE %-8s %-8s enqueue=%d msg/s (median of %d: %s), consume=%d msg/s (median of %d: %s)%n",
                    combos.get(i)[0], combos.get(i)[1], enq[ROUNDS / 2], ROUNDS, Arrays.toString(enq), con[ROUNDS / 2], ROUNDS, Arrays.toString(con));
        }

        // ---------- 定时任务 ----------
        if (RUN_CRON) {
            cronRound("no-mq      ", null);
            cronRound("amqp091    ", ops091);
            cronRound("amqp1.0    ", ops100);
        }

        ops091.shutdown();
        ops100.shutdown();
        if (!queues.isEmpty()) {
            delete(mq, queues.toArray(new String[0]));
        }
        System.exit(0);
    }

    /**
     * 单轮队列任务：预发送 N 条（无消费者）测入队，再启动监听测消费。
     *
     * @return [0]=端到端入队 msg/s；[1]=消费 msg/s
     */
    static long[] queueRound(TaskAmqpOps ops, String protocol, String queueType, List<String> queues, int round) throws Exception {
        boolean quorum = "quorum".equals(queueType);
        UwTaskProperties props = new UwTaskProperties();
        TaskMetaInfoManager meta = new TaskMetaInfoManager(null, props);
        meta.getRunnerInstanceMap().put(PerfRunner.class.getName(), new PerfRunner());
        TaskRunnerConfig config = new TaskRunnerConfig();
        config.setTaskClass(PerfRunner.class.getName());
        config.setTaskTag(queueType);
        config.setQueueType(quorum ? TaskRunnerConfig.QUEUE_TYPE_QUORUM : TaskRunnerConfig.QUEUE_TYPE_CLASSIC);
        config.setConsumerNum(CONSUMERS);
        config.setPrefetchNum(PREFETCH);
        config.setLogLevel(TaskRunnerConfig.TASK_LOG_TYPE_NONE);
        meta.setRunnerConfig(meta.getRunnerConfigKey(config), config);
        TaskListenerManager listeners = new TaskListenerManager();
        // 延时搬运器不参与（delay=0 的即时投递不触碰它）
        TaskDelayTransferContainer transfer = new TaskDelayTransferContainer(null, "uw.task.probe");
        TaskSendCenter sendCenter = new TaskSendCenter(ops, meta, transfer, listeners);
        TaskRunnerContainer runner = new TaskRunnerContainer(props, new NoopApiClient(), meta, new TaskLocalRateLimiter(), listeners, ops, sendCenter);
        String queue = meta.getQueueNameByConfig(config);
        if (!queues.contains(queue)) {
            queues.add(queue);
            ops.declareDurableQueue(queue, quorum);
        }
        PerfRunner.EXECUTED.set(0);
        ops.purgeQueue(queue);
        Thread.sleep(300);

        long t0 = System.nanoTime();
        for (int i = 0; i < COUNT; i++) {
            TaskData<String, String> data = new TaskData<>(PerfRunner.class.getName(), "p" + i);
            data.setTaskTag(queueType);
            sendCenter.sendToQueue(data);
        }
        long callMs = (System.nanoTime() - t0) / 1_000_000;
        waitQueueDepth(ops, queue, COUNT);
        long enqueueMs = (System.nanoTime() - t0) / 1_000_000;
        long enqueueRate = COUNT * 1000L / Math.max(1, enqueueMs);

        long t2 = System.nanoTime();
        runner.registerQueueListener(config);
        waitExecuted(COUNT);
        long consumeMs = (System.nanoTime() - t2) / 1_000_000;
        long consumeRate = COUNT * 1000L / Math.max(1, consumeMs);
        runner.suspendAll();
        System.out.printf("PERF-ROUND %-8s %-8s r%d sendCall=%dms enqueue=%dms (%d msg/s), consume=%dms (%d msg/s)%n",
                protocol, queueType, round, callMs, enqueueMs, enqueueRate, consumeMs, consumeRate);
        Thread.sleep(500);
        return new long[]{enqueueRate, consumeRate};
    }

    /**
     * 单轮定时任务：CRONERS 个每秒触发的配置连续跑 10 秒，统计触发覆盖率与触发延迟。
     */
    static void cronRound(String tier, TaskAmqpOps ops) throws Exception {
        UwTaskProperties props = new UwTaskProperties();
        TaskMetaInfoManager meta = new TaskMetaInfoManager(null, props);
        TaskCronerContainer container = new TaskCronerContainer(props, new NoopApiClient(), meta, new TaskListenerManager());
        PerfCroner croner = new PerfCroner();
        for (int i = 0; i < CRONERS; i++) {
            TaskCronerConfig config = new TaskCronerConfig();
            config.setTaskClass(PerfCroner.class.getName());
            config.setTaskParam("p" + i);
            config.setRunTarget("default");
            config.setTaskCron("* * * * * *");
            config.setRunType(TaskCronerConfig.RUN_TYPE_ANYWAY);
            config.setLogLevel(TaskCronerConfig.TASK_LOG_TYPE_NONE);
            if (!container.configureTask(croner, config)) {
                throw new IllegalStateException("croner 配置注册失败: " + i);
            }
        }
        // 等所有配置完成首轮触发（对齐到秒边界），再开始计数
        Thread.sleep(2500);
        PerfCroner.reset();
        long seconds = 10;
        Thread.sleep(seconds * 1000L);
        long fires = PerfCroner.FIRES.get();
        long expected = (long) CRONERS * seconds;
        int sampled = (int) Math.min(PerfCroner.INDEX.get(), PerfCroner.LATENESS.length);
        long[] lateness = Arrays.copyOf(PerfCroner.LATENESS, sampled);
        Arrays.sort(lateness);
        System.out.printf("PERF-SUMMARY-CRON tier=%s croners=%d fires=%d expected=%d coverage=%.1f%% latenessMs p50=%d p99=%d max=%d%n",
                tier.trim(), CRONERS, fires, expected, 100.0 * fires / expected,
                sampled == 0 ? -1 : lateness[sampled / 2], sampled == 0 ? -1 : lateness[(int) (sampled * 0.99)], sampled == 0 ? -1 : lateness[sampled - 1]);
        container.shutdown();
    }

    static void waitQueueDepth(TaskAmqpOps ops, String queue, int target) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 180_000;
        while (System.currentTimeMillis() < deadline) {
            int[] info = ops.getQueueInfo(queue);
            if (info != null && info[0] >= target) {
                return;
            }
            Thread.sleep(POLL_MS);
        }
        System.out.println("PERF-WARN 等待入队超时: queue=" + queue + ", depth=" + Arrays.toString(ops.getQueueInfo(queue)));
    }

    static void waitExecuted(int target) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 300_000;
        while (System.currentTimeMillis() < deadline && PerfRunner.EXECUTED.get() < target) {
            Thread.sleep(POLL_MS);
        }
        if (PerfRunner.EXECUTED.get() < target) {
            System.out.println("PERF-WARN 等待消费超时: executed=" + PerfRunner.EXECUTED.get());
        }
    }

    /**
     * 删除测试队列及同名 exchange。
     */
    static void delete(RabbitProperties mq, String... names) {
        ConnectionFactory raw = new ConnectionFactory();
        raw.setHost(mq.getHost());
        raw.setPort(mq.getPort());
        raw.setUsername(mq.getUsername());
        raw.setPassword(mq.getPassword());
        CachingConnectionFactory factory = new CachingConnectionFactory(raw);
        AmqpAdmin admin = new RabbitAdmin(factory);
        for (String name : names) {
            try {
                admin.deleteQueue(name);
                admin.deleteExchange(name);
                System.out.println("PERF-DELETED " + name);
            } catch (Exception e) {
                System.out.println("PERF-DELETE-FAILED " + name + " " + e.getMessage());
            }
        }
        factory.destroy();
    }

    static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * 计数型队列任务（任务体只做一次原子自增，测的是框架链路本身）。
     */
    static class PerfRunner extends TaskRunner<String, String> {

        static final AtomicInteger EXECUTED = new AtomicInteger();

        @Override
        public String runTask(TaskData<String, String> taskData) {
            EXECUTED.incrementAndGet();
            return "ok";
        }

        @Override
        public TaskRunnerConfig initConfig() {
            TaskRunnerConfig config = new TaskRunnerConfig();
            config.setTaskClass(PerfRunner.class.getName());
            config.setLogLevel(TaskRunnerConfig.TASK_LOG_TYPE_NONE);
            return config;
        }

        @Override
        public TaskContact initContact() {
            return new TaskContact();
        }
    }

    /**
     * 计数型定时任务：记录触发次数与"实际执行时刻 − 计划触发时刻"。
     */
    static class PerfCroner extends TaskCroner {

        static final AtomicInteger FIRES = new AtomicInteger();
        static final long[] LATENESS = new long[2_000_000];
        static final AtomicLong INDEX = new AtomicLong();

        static void reset() {
            FIRES.set(0);
            INDEX.set(0);
        }

        @Override
        public String runTask(TaskCronerLog taskCronerLog) {
            FIRES.incrementAndGet();
            long index = INDEX.getAndIncrement();
            if (index < LATENESS.length) {
                LATENESS[(int) index] = System.currentTimeMillis() - taskCronerLog.getScheduleDate().getTime();
            }
            return "ok";
        }

        @Override
        public TaskCronerConfig initConfig() {
            TaskCronerConfig config = new TaskCronerConfig();
            config.setTaskClass(PerfCroner.class.getName());
            config.setLogLevel(TaskCronerConfig.TASK_LOG_TYPE_NONE);
            return config;
        }

        @Override
        public TaskContact initContact() {
            return new TaskContact();
        }
    }

    /**
     * 不外发的任务 API 客户端（探针不写 ES、不调 task-center）。
     */
    static class NoopApiClient extends TaskApiClient {

        NoopApiClient() {
            super(new UwTaskProperties(), null, null);
        }

        @Override
        public void sendTaskRunnerLog(TaskRunnerLog log) {
        }

        @Override
        public void sendTaskCronerLog(long taskId, TaskCronerLog taskCronerLog) {
        }
    }
}
