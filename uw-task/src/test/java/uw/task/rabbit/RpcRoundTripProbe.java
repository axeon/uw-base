package uw.task.rabbit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties;
import uw.task.TaskData;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.conf.UwTaskProperties;
import uw.task.container.TaskRunnerContainer;
import uw.task.converter.TaskMessageConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 两协议 MQ RPC 往返实测（显式触发：-Damqp.bench=true）：经真实 broker 发请求、监听端回结果，测是否返回与往返耗时。
 *
 * <p>FullFlowE2ETest 的 rpcSyncRoundTrip 是 AUTO→本地执行，不经 MQ；本探针才覆盖
 * {@code sendAndReceive} 的传输链路。监听端用 TaskRunnerContainer 的最小替身（process 回填结果并返回）。</p>
 */
@EnabledIfSystemProperty(named = "amqp.bench", matches = "true")
class RpcRoundTripProbe {

    static final int WARMUP = 50;
    static final int COUNT = 500;

    @Test
    void rpcRoundTripBothProtocols() throws Exception {
        for (boolean amqp10 : new boolean[]{false, true}) {
            roundTrip(amqp10);
        }
    }

    /**
     * 并发在途：每协议 60 线程各发 10 次（服务端每条停 100ms），核对回复无丢失、无串号。
     */
    @Test
    void rpcConcurrentBothProtocols() throws Exception {
        for (boolean amqp10 : new boolean[]{false, true}) {
            RabbitProperties props = new RabbitProperties();
            props.setHost(System.getProperty("uw.task.rabbitmq.host", "10.0.0.10"));
            props.setPort(Integer.parseInt(System.getProperty("uw.task.rabbitmq.port", "5672")));
            props.setUsername(System.getProperty("uw.task.rabbitmq.username", "rabbit"));
            props.setPassword(System.getProperty("uw.task.rabbitmq.password"));
            TaskAmqpOps ops = amqp10 ? new Rabbit100Ops(props, true) : new Rabbit091Ops(props, 20, true);
            String queue = "uw-bench.rpc.c" + (amqp10 ? "10" : "091");
            TaskRunnerContainer server = new TaskRunnerContainer(new UwTaskProperties(), null, new TaskMetaInfoManager(null, new UwTaskProperties()),
                    null, null, ops, null) {
                @Override
                public TaskData process(TaskData taskData) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    taskData.setResultData("echo:" + taskData.getTaskParam());
                    taskData.setState(TaskData.STATE_SUCCESS);
                    return taskData;
                }
            };
            AtomicInteger wrong = new AtomicInteger();
            AtomicInteger index = new AtomicInteger();
            try {
                ops.declareDurableQueue(queue, false);
                ops.purgeQueue(queue);
                TaskAmqpListenerContainer listener = ops.createListener(queue, 60, 60, server);
                Thread.sleep(1500);
                TaskMessageConverter converter = new TaskMessageConverter();
                int ok = 0;
                try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
                    List<Future<Boolean>> results = new ArrayList<>();
                    for (int t = 0; t < 60; t++) {
                        results.add(pool.submit(() -> {
                            for (int i = 0; i < 10; i++) {
                                String param = "c" + index.getAndIncrement();
                                MessageProperties mp = new MessageProperties();
                                mp.setMessageId(param);
                                Message reply = ops.sendAndReceive(queue, converter.toMessage(new TaskData<String, String>("uw.bench.RpcEcho", param), mp));
                                if (reply == null) {
                                    System.out.println("RPC-CONCURRENT-DIAG " + (amqp10 ? "amqp1.0" : "amqp091") + " null reply param=" + param);
                                    return false;
                                }
                                Object got = ((TaskData<?, ?>) converter.fromMessage(reply)).getResultData();
                                if (!("echo:" + param).equals(got)) {
                                    wrong.incrementAndGet();
                                    System.out.println("RPC-CONCURRENT-DIAG " + (amqp10 ? "amqp1.0" : "amqp091") + " mismatch param=" + param + " got=" + got);
                                    return false;
                                }
                            }
                            return true;
                        }));
                    }
                    for (Future<Boolean> result : results) {
                        if (result.get()) {
                            ok++;
                        }
                    }
                }
                System.out.printf("RPC-CONCURRENT %s: 全部成功的线程=%d/60, 串号=%d%n", amqp10 ? "amqp1.0" : "amqp091", ok, wrong.get());
                assertEquals(60, ok, (amqp10 ? "amqp1.0" : "amqp091") + " 并发在途下存在丢失或串号的回复");
                assertEquals(0, wrong.get(), "回复串号");
                listener.destroy();
            } finally {
                ops.shutdown();
                SendPoolSweep.delete(props, queue);
            }
        }
    }

    void roundTrip(boolean amqp10) throws Exception {
        RabbitProperties props = new RabbitProperties();
        props.setHost(System.getProperty("uw.task.rabbitmq.host", "10.0.0.10"));
        props.setPort(Integer.parseInt(System.getProperty("uw.task.rabbitmq.port", "5672")));
        props.setUsername(System.getProperty("uw.task.rabbitmq.username", "rabbit"));
        props.setPassword(System.getProperty("uw.task.rabbitmq.password"));
        TaskAmqpOps ops = amqp10 ? new Rabbit100Ops(props, true) : new Rabbit091Ops(props, 20, true);
        String queue = "uw-bench.rpc." + (amqp10 ? "amqp10" : "amqp091");
        TaskRunnerContainer server = new TaskRunnerContainer(new UwTaskProperties(), null, new TaskMetaInfoManager(null, new UwTaskProperties()),
                null, null, ops, null) {
            @Override
            public TaskData process(TaskData taskData) {
                taskData.setResultData("echo:" + taskData.getTaskParam());
                taskData.setState(TaskData.STATE_SUCCESS);
                return taskData;
            }
        };
        try {
            ops.declareDurableQueue(queue, false);
            TaskAmqpListenerContainer listener = ops.createListener(queue, 1, 5, server);
            Thread.sleep(1500);
            TaskMessageConverter converter = new TaskMessageConverter();
            long[] samples = new long[COUNT];
            int nullReplies = 0;
            for (int i = 0; i < WARMUP + COUNT; i++) {
                MessageProperties mp = new MessageProperties();
                mp.setMessageId(String.valueOf(1_000_000L + i));
                Message request = converter.toMessage(new TaskData<String, String>("uw.bench.RpcEcho", "p" + i), mp);
                long t0 = System.nanoTime();
                Message reply = ops.sendAndReceive(queue, request);
                long us = (System.nanoTime() - t0) / 1_000;
                if (reply == null) {
                    nullReplies++;
                    continue;
                }
                TaskData<?, ?> data = (TaskData<?, ?>) converter.fromMessage(reply);
                assertEquals("echo:p" + i, data.getResultData(), "返回内容应与请求对应");
                if (i >= WARMUP) {
                    samples[i - WARMUP] = us;
                }
            }
            Arrays.sort(samples);
            long sum = 0;
            for (long s : samples) {
                sum += s;
            }
            System.out.printf("RPC-RTT %s: 成功=%d/%d, avg=%.2fms, p50=%.2fms, p99=%.2fms, max=%.2fms%n",
                    amqp10 ? "amqp1.0" : "amqp091", COUNT - nullReplies, COUNT, sum / 1000.0 / Math.max(1, COUNT - nullReplies),
                    samples[samples.length / 2] / 1000.0, samples[(int) (samples.length * 0.99)] / 1000.0, samples[samples.length - 1] / 1000.0);
            assertEquals(0, nullReplies, amqp10 ? "amqp1.0" : "amqp091" + " 存在未返回的 RPC");
            listener.destroy();
        } finally {
            ops.shutdown();
            if (amqp10) {
                SendPoolSweep.delete(props, queue);
            }
        }
    }
}
