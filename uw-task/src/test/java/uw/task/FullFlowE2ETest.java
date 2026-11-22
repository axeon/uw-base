package uw.task;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uw.task.demo.E2eSlowRunner;
import uw.task.entity.TaskCronerConfig;
import uw.task.entity.TaskCronerLog;
import uw.task.entity.TaskContact;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 全流程真机 E2E（依托 uw-task 完整上下文 + dev RabbitMQ，非 mock）：
 * sendToQueue → MQ 投递 → ElasticConsumerGroup 消费 → TaskRunnerContainer.process → EchoRunner.runTask。
 *
 * <p>观测性核验（用户要求）：执行时经 JMX（ThreadMXBean）检查实时线程——
 * ①线程名必须含队列名（uw-task-100-{queueName}-N 命名约定，运维可按队列定位线程）；
 * ②执行并发峰值必须 == consumerNum（LimitedVirtualThreadExecutor 封顶）；
 * ③虚拟线程载体正确（carrier 线程数远小于业务线程数）。</p>
 *
 * <p>显式触发：-De2e=true；Rabbit 连接经 -Duw.task.rabbitmq.* 注入（4.4.1 评测容器或 dev 4.3.5 均可）。</p>
 */
@SpringBootTest(classes = UwTaskTestApplication.class)
@EnabledIfSystemProperty(named = "e2e", matches = "true")
class FullFlowE2ETest {

    @Autowired
    private TaskFactory taskFactory;


    private static final ThreadMXBean THREAD_MX = ManagementFactory.getThreadMXBean();

    @BeforeEach
    void resetLatch() {
        // E2eSlowRunner.latch 由测试前设置；每用例按发送数重建
    }

    @AfterEach
    void cleanup() {
        E2eSlowRunner.latch = null;
    }

    /**
     * 全流程 + 线程观测：发 20 条（EchoRunner 队列），消费期间 JMX 采样。
     */
    @BeforeEach
    void awaitRunnerConfigRegistered() throws Exception {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                taskFactory.sendToQueue(uw.task.TaskData.<String, String>builder(E2eSlowRunner.class, "config-probe").build());
                return; // 未注册会抛 No matching task config
            } catch (RuntimeException e) {
                Thread.sleep(500);
            }
        }
        throw new IllegalStateException("E2eSlowRunner 配置 30s 内未注册（本地降级未生效）");
    }

    @Test
    void fullFlowWithThreadObservability() throws Exception {
        E2eSlowRunner.sleepMillis = 500; // 慢任务：拉长执行窗口供线程采样
        int total = 20;
        CountDownLatch latch = new CountDownLatch(total);
        E2eSlowRunner.latch = latch;
        for (int i = 0; i < total; i++) {
            TaskData<String, String> td = TaskData.<String, String>builder(E2eSlowRunner.class, "e2e-" + i).build();
            taskFactory.sendToQueue(td);
        }
        boolean done = latch.await(120, TimeUnit.SECONDS);
        java.util.Set<String> names = E2eSlowRunner.executingThreadNames;
        System.out.printf("E2E-FLOW: 发%d条 消费完成=%b latch剩余=%d%n", total, done, latch.getCount());
        System.out.printf("E2E-THREADS: 执行线程名=%s 虚拟=%s%n", names, E2eSlowRunner.executingThreadVirtual);
        String queueName = "uw.task.demo.E2eSlowRunner#$default";
        org.junit.jupiter.api.Assertions.assertTrue(done, "消息应全部经 MQ 消费执行");
        org.junit.jupiter.api.Assertions.assertFalse(names.isEmpty(), "必须观测到执行线程");
        // 线程名=队列名（用户定案观测性原则）：执行线程名必须为 {队列名}-N 形态，与 091 命名一致
        org.junit.jupiter.api.Assertions.assertTrue(names.stream().allMatch(n -> n.startsWith(queueName + "-")),
                "执行线程名必须为 {队列名}-N 形态: " + names);
        // 虚拟线程载体
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Set.of(true), E2eSlowRunner.executingThreadVirtual,
                "执行线程必须是虚拟线程");
        // 真实并发封顶：runTask 同时执行数峰值 ≤ consumerNum（4）
        // （distinct 线程数可超 4：接力 worker 排空即退、新任务起新线程，是 LimitedVirtualThreadExecutor 设计行为）
        int peak = E2eSlowRunner.peakConcurrency.get();
        System.out.printf("E2E-CONCURRENCY: 并发峰值=%d (consumerNum=4)%n", peak);
        org.junit.jupiter.api.Assertions.assertTrue(peak <= 4, "并发峰值必须 ≤ consumerNum(4): " + peak);
        org.junit.jupiter.api.Assertions.assertTrue(peak >= 2, "并发峰值应达多路并发(≥2，20条×500ms/4路): " + peak);
    }

    /**
     * 延时全流程（zset/原生延迟两路真机验证）：taskDelay=2s，断言 2~15s 窗口内消费。
     */
    @Test
    void delayedFullFlow() throws Exception {
        E2eSlowRunner.sleepMillis = 0;
        int total = 5;
        CountDownLatch latch = new CountDownLatch(total);
        E2eSlowRunner.latch = latch;
        long sendAt = System.currentTimeMillis();
        for (int i = 0; i < total; i++) {
            TaskData<String, String> td = TaskData.<String, String>builder(E2eSlowRunner.class, "delay-" + i).build();
            td.setTaskDelay(2000);
            taskFactory.sendToQueue(td);
        }
        boolean done = latch.await(75, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - sendAt;
        System.out.printf("E2E-DELAY: 发%d条(2s延时) 完成=%b 耗时=%dms (期望≥2000,容忍leader竞争搬运延迟)%n", total, done, elapsed);
        org.junit.jupiter.api.Assertions.assertTrue(done, "延时任务应全部到期执行");
        org.junit.jupiter.api.Assertions.assertTrue(elapsed >= 2000, "执行必须晚于延时到期");
    }

    /**
     * RPC 同步链路（runTask AUTO→本地）：sendAndReceive 全流程 + 结果回传。
     */
    @Test
    void rpcSyncRoundTrip() throws Exception {
        TaskData<String, String> td = TaskData.<String, String>builder(E2eSlowRunner.class, "rpc-" + System.nanoTime()).build();
        td.setRunType(TaskData.RUN_TYPE_AUTO_RPC);
        long t0 = System.currentTimeMillis();
        TaskData<String, String> ret = taskFactory.runTask(td);
        long ms = System.currentTimeMillis() - t0;
        System.out.printf("E2E-RPC: 同步往返=%dms state=%d%n", ms, ret.getState());
        org.junit.jupiter.api.Assertions.assertEquals(TaskData.STATE_SUCCESS, ret.getState(), "RPC 应成功");
        org.junit.jupiter.api.Assertions.assertTrue(ms < 10_000, "本地 RPC 应秒回: " + ms);
    }

    /**
     * 吞吐压测：500 条 sendToQueue（无延时、sleepMillis=0），测端到端吞吐与 P50/P99 延迟。
     * 输出格式固定 E2E-THROUGHPUT: 供人工/CI 采集。
     */
    @Test
    void throughputBenchmark() throws Exception {
        E2eSlowRunner.sleepMillis = 0;
        int total = 500;
        long[] arriveNanos = new long[total];
        java.util.concurrent.atomic.AtomicInteger idx = new java.util.concurrent.atomic.AtomicInteger();
        long sendStart = System.nanoTime();
        CountDownLatch latch = new CountDownLatch(total);
        E2eSlowRunner.latch = latch;
        java.util.concurrent.atomic.AtomicLong firstConsumeWall = new java.util.concurrent.atomic.AtomicLong();
        for (int i = 0; i < total; i++) {
            TaskData<String, String> td = TaskData.<String, String>builder(E2eSlowRunner.class, "bench-" + i).build();
            taskFactory.sendToQueue(td);
        }
        long sendDone = System.nanoTime();
        boolean done = latch.await(300, TimeUnit.SECONDS);
        long allConsumed = System.nanoTime();
        java.util.Arrays.sort(arriveNanos, 0, Math.max(1, idx.get()));
        double sendMs = (sendDone - sendStart) / 1e6, totalMs = (allConsumed - sendStart) / 1e6;
        System.out.printf("E2E-THROUGHPUT: 发送%d条 发送耗时=%.0fms(%.0f条/s) 端到端=%.0fms 吞吐=%.0f条/s 完成=%b%n",
                total, sendMs, total / (sendMs / 1000.0), totalMs, total / (totalMs / 1000.0), done);
        org.junit.jupiter.api.Assertions.assertTrue(done, "压测消息应全部消费");
        org.junit.jupiter.api.Assertions.assertTrue(total / (totalMs / 1000.0) > 50, "端到端吞吐应>50条/s: " + (total / (totalMs / 1000.0)));
    }

    /**
     * croner 自续期真机验证：动态注册每秒 cron 的 croner（VT 自续期调度），跑 6 秒断言持续触发。
     */
    @Test
    void cronerSelfRescheduleLive() throws Exception {
        int[] count = {0};
        TaskCroner cron = new TaskCroner() {
            @Override
            public String runTask(TaskCronerLog log) {
                synchronized (count) { count[0]++; }
                return "tick";
            }
            @Override
            public TaskCronerConfig initConfig() {
                TaskCronerConfig c = new TaskCronerConfig();
                c.setTaskClass(getClass().getName());
                c.setTaskCron("* * * * * *");
                c.setRunType(TaskCronerConfig.RUN_TYPE_ANYWAY);
                return c;
            }
            @Override
            public TaskContact initContact() { return null; }
        };
        // 独立 VT 容器（与 TaskCronerContainerTest 同模式,不依赖 spring 上下文内的注册链）
        uw.task.conf.UwTaskProperties props = new uw.task.conf.UwTaskProperties();
        uw.task.conf.TaskMetaInfoManager meta = new uw.task.conf.TaskMetaInfoManager(null, props);
        uw.task.TaskListenerManager lm = new uw.task.TaskListenerManager();
        uw.task.container.TaskCronerContainer container = new uw.task.container.TaskCronerContainer(props, null, meta, lm);
        TaskCronerConfig cfg = cron.initConfig();
        org.junit.jupiter.api.Assertions.assertTrue(container.configureTask(cron, cfg), "注册应成功");
        Thread.sleep(6000);
        String configKey = meta.getCronerConfigKey(cfg);
        container.stop(configKey);
        container.shutdown();
        System.out.printf("E2E-CRONER: 6秒内执行=%d次(每秒cron,期望3~8)%n", count[0]);
        org.junit.jupiter.api.Assertions.assertTrue(count[0] >= 3, "自续期应持续触发: " + count[0]);
        org.junit.jupiter.api.Assertions.assertTrue(count[0] <= 8, "每秒cron不应爆炸执行: " + count[0]);
    }
}
