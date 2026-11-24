package uw.task.rabbit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import uw.common.util.RedisConnectionFactoryBuilder;
import uw.task.TaskData;
import uw.task.TaskListenerManager;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.conf.UwTaskProperties;
import uw.task.container.TaskSendCenter;
import uw.task.entity.TaskRunnerConfig;
import uw.task.listener.TaskSendListener;
import uw.task.util.TaskSequence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.0 异步发布失败处理实测（显式触发：-Damqp.bench=true）：
 * ①往不存在的队列发布 → broker 拒收经回调重发，耗尽后触发 onSendFail（调用方不抛异常）；
 * ②往已声明队列发布 → 正常入队，不触发 onSendFail。
 */
@EnabledIfSystemProperty(named = "amqp.bench", matches = "true")
class AsyncSendFailProbe {

    static final String MISSING_TASK = "uw.bench.missing.MissingRunner";
    static final String OK_TASK = "uw.bench.missing.OkRunner";
    static final String OK_QUEUE = OK_TASK + "#$default";

    @Test
    void asyncFailureRetriesThenNotifiesListener() throws Exception {
        RabbitProperties props = new RabbitProperties();
        props.setHost(System.getProperty("uw.task.rabbitmq.host", "10.0.0.10"));
        props.setPort(Integer.parseInt(System.getProperty("uw.task.rabbitmq.port", "5672")));
        props.setUsername(System.getProperty("uw.task.rabbitmq.username", "rabbit"));
        props.setPassword(System.getProperty("uw.task.rabbitmq.password"));
        assertTrue(RabbitCapabilityProbe.probe(props).isAmqp100Available(), "须走 1.0 路径");
        Rabbit100Ops ops = new Rabbit100Ops(props, true);
        // 取号走 uw-task 私有 Redis（凭据经环境变量注入，独立库 15 不与常驻应用互扰）
        DataRedisProperties redis = new DataRedisProperties();
        redis.setHost(System.getProperty("uw.task.redis.host", "10.0.0.10"));
        redis.setPort(6380);
        redis.setDatabase(15);
        redis.setPassword(System.getenv("UW_TEST_REDIS_PASSWORD"));
        RedisConnectionFactory taskRedis = RedisConnectionFactoryBuilder.build(redis, null);
        TaskSequence.init(taskRedis);
        UwTaskProperties taskProps = new UwTaskProperties();
        TaskMetaInfoManager meta = new TaskMetaInfoManager(null, taskProps);
        meta.setRunnerConfig(MISSING_TASK + "#$default", config(MISSING_TASK));
        meta.setRunnerConfig(OK_QUEUE, config(OK_TASK));
        ops.declareDurableQueue(OK_QUEUE, false);
        ops.purgeQueue(OK_QUEUE);
        List<String> fails = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        TaskListenerManager listeners = new TaskListenerManager();
        listeners.addSendListener(new TaskSendListener() {
            @Override
            public void onDelaySubmit(TaskData<?, ?> taskData, String queueName) {
            }

            @Override
            public void onSendFail(TaskData<?, ?> taskData, String queueName, int attempts) {
                fails.add(taskData.getTaskClass() + "@" + attempts);
                latch.countDown();
            }
        });
        TaskSendCenter center = new TaskSendCenter(ops, meta, null, listeners);
        try {
            // ① 目标队列不存在：sendToQueue 立即返回（异步），失败经回调重发
            long t0 = System.currentTimeMillis();
            center.sendToQueue(task(OK_TASK));
            center.sendToQueue(task(MISSING_TASK));
            long returnedMs = System.currentTimeMillis() - t0;
            assertTrue(latch.await(90, TimeUnit.SECONDS), "90s 内应触发 onSendFail，实际=" + fails);
            System.out.printf("ASYNC-FAIL: 调用返回耗时ms=%d, onSendFail=%s, 总耗时ms=%d, 最终入队(正常队列)=%d%n",
                    returnedMs, fails, System.currentTimeMillis() - t0, ops.getQueueInfo(OK_QUEUE)[0]);
            assertEquals(1, fails.size(), "只有目标队列不存在的那条应失败");
            assertTrue(fails.get(0).startsWith(MISSING_TASK + "@"), fails.toString());
            assertEquals(1, ops.getQueueInfo(OK_QUEUE)[0], "正常队列的那条应恰好入队一次（未被重发重复）");
        } finally {
            ops.purgeQueue(OK_QUEUE);
            ops.shutdown();
            ((LettuceConnectionFactory) taskRedis).destroy();
        }
    }

    static TaskRunnerConfig config(String taskClass) {
        TaskRunnerConfig config = new TaskRunnerConfig();
        config.setTaskClass(taskClass);
        return config;
    }

    static TaskData<String, String> task(String taskClass) {
        return new TaskData<>(taskClass, "p");
    }
}
