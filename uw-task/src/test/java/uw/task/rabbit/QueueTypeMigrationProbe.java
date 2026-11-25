package uw.task.rabbit;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties;
import uw.task.TaskData;
import uw.task.TaskListenerManager;
import uw.task.api.TaskApiClient;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.conf.UwTaskProperties;
import uw.task.container.TaskDelayTransferContainer;
import uw.task.container.TaskRunnerContainer;
import uw.task.container.TaskSendCenter;
import uw.task.converter.TaskMessageConverter;
import uw.task.demo.EchoRunner;
import uw.task.entity.TaskCronerLog;
import uw.task.entity.TaskRunnerConfig;
import uw.task.entity.TaskRunnerLog;
import uw.task.util.TaskLocalRateLimiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 队列形态迁移的真实 MQ 验证（显式触发：-Damqp.bench=true）：同名 classic 队列与 quorum 配置冲突时，
 * 仅当队列确认无消息无消费者才删除重建，否则不动队列。
 */
@EnabledIfSystemProperty(named = "amqp.bench", matches = "true")
class QueueTypeMigrationProbe {

    static final String TASK_CLASS = "uw.task.probe.MigrateRunner";
    static final String QUEUE = TASK_CLASS + "#$default";

    /**
     * 旧的空 classic 队列：注册监听时自动删除并按 quorum 重建，监听正常启动。
     */
    @Test
    void emptyClassicQueue_migratedToQuorum() throws Exception {
        Rabbit091Ops ops = new Rabbit091Ops(mq(), 4, true);
        try {
            ops.deleteQueue(QUEUE);
            ops.declareDurableQueue(QUEUE, false);
            assertThat(ops.isEmptyWithoutConsumers(QUEUE)).isTrue();
            TaskRunnerContainer runner = newContainer(ops);
            runner.registerQueueListener(quorumConfig());
            assertThat(mismatchWhenDeclaredAs(ops, false)).as("队列应已是 quorum：按 classic 声明必须报形态不一致").isTrue();
            assertThat(ops.getQueueInfo(QUEUE)[1]).isEqualTo(1);
            runner.suspendAll();
            System.out.println("MIGRATE-PROBE OK: 空的 classic 队列已迁移为 quorum 并启动监听");
        } finally {
            cleanup(ops);
        }
    }

    /**
     * 有积压消息的 classic 队列：不迁移，队列保持 classic、消息仍在。
     */
    @Test
    void nonEmptyClassicQueue_notMigrated() throws Exception {
        Rabbit091Ops ops = new Rabbit091Ops(mq(), 4, true);
        try {
            ops.deleteQueue(QUEUE);
            ops.declareDurableQueue(QUEUE, false);
            ops.send(QUEUE, message(), 0, e -> {
            });
            long deadline = System.currentTimeMillis() + 10000;
            while (ops.getQueueInfo(QUEUE)[0] < 1 && System.currentTimeMillis() < deadline) {
                Thread.sleep(100);
            }
            TaskRunnerContainer runner = newContainer(ops);
            runner.registerQueueListener(quorumConfig());
            ops.declareDurableQueue(QUEUE, false);
            assertThat(ops.getQueueInfo(QUEUE)[0]).isEqualTo(1);
            assertThat(ops.getQueueInfo(QUEUE)[1]).isZero();
            System.out.println("MIGRATE-PROBE OK: 有消息的 classic 队列未被迁移");
        } finally {
            cleanup(ops);
        }
    }

    /**
     * 1.0 协议（协议操作面直接声明）：空的 classic 队列按 quorum 声明时被迁移；有消息的队列不动。
     */
    @Test
    void amqp100_migratesOnlyWhenEmpty() throws Exception {
        Rabbit100Ops ops = new Rabbit100Ops(mq(), true);
        // 消息数用 091 操作面查（passive declare 返回待投递数；管理接口的计数字段随版本变化，不稳）
        Rabbit091Ops inspector = new Rabbit091Ops(mq(), 1, true);
        try {
            // 迁移是双向的：迁移后再按 classic 声明会被迁回去，所以队列形态直接查管理接口确认，不用声明行为反推
            assumeTrue(managementReachable(), "管理接口不可达，无法确认队列形态");
            ops.deleteQueue(QUEUE);
            ops.declareDurableQueue(QUEUE, false);
            ops.declareDurableQueue(QUEUE, true);
            assertThat(queueType()).as("1.0：空队列应已迁移为 quorum").isEqualTo("quorum");
            ops.deleteQueue(QUEUE);
            ops.declareDurableQueue(QUEUE, false);
            ops.send(QUEUE, message(), 0, e -> {
            });
            long deadline = System.currentTimeMillis() + 10000;
            while (inspector.getQueueInfo(QUEUE)[0] < 1 && System.currentTimeMillis() < deadline) {
                Thread.sleep(100);
            }
            assertThatThrownBy(() -> ops.declareDurableQueue(QUEUE, true)).as("1.0：有消息的 classic 队列不应被迁移").isInstanceOf(Exception.class);
            assertThat(queueType()).as("1.0：有消息的 classic 队列不应被迁移").isEqualTo("classic");
            System.out.println("MIGRATE-PROBE OK: 1.0 协议下空队列已迁移、有消息的队列未迁移");
        } finally {
            cleanup(ops);
            inspector.shutdown();
        }
    }

    /**
     * 管理接口是否可达（队列形态只能经管理接口确认，AMQP 端口的 passive declare 不返回形态）。
     *
     * @return 可达返回 true
     */
    static boolean managementReachable() {
        try {
            return httpGet("/api/overview") != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 队列形态（经管理接口查询）。
     *
     * @return classic 或 quorum
     * @throws Exception 查询失败
     */
    static String queueType() throws Exception {
        return jsonField(managementJson(), "type");
    }

    /**
     * 取管理接口 JSON 里的标量字段：找到字段名后的冒号，字符串取引号内内容，数字取到第一个非数字字符为止（够测试用，不引入 JSON 库）。
     *
     * @param json 管理接口返回
     * @param name 字段名
     * @return 字段值文本
     */
    static String jsonField(String json, String name) {
        String key = "\"" + name + "\"";
        int index = json.indexOf(key);
        assertThat(index >= 0).as("管理接口返回里应有字段 %s，实际返回 %s", name, json).isTrue();
        int colon = json.indexOf(':', index + key.length());
        assertThat(colon >= 0).as("字段 %s 后应有冒号", name).isTrue();
        String rest = json.substring(colon + 1).trim();
        return rest.startsWith("\"") ? rest.substring(1, rest.indexOf('"', 1)) : rest.replaceAll("[^0-9].*$", "");
    }

    static String managementJson() throws Exception {
        return httpGet("/api/queues/%2F/" + URLEncoder.encode(QUEUE, StandardCharsets.UTF_8).replace("+", "%20"));
    }

    /**
     * 带账号发一次 GET（与 AMQP 同账号）。
     *
     * @param path 管理接口路径
     * @return 响应体；非 200 返回 null
     * @throws Exception 请求失败
     */
    static String httpGet(String path) throws Exception {
        URI uri = URI.create("http://" + mq().getHost() + ":" + System.getProperty("uw.task.rabbitmq.management.port", "15672") + path);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((mq().getUsername() + ":" + mq().getPassword()).getBytes(StandardCharsets.UTF_8)))
                .GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200 ? response.body() : null;
    }

    static TaskRunnerContainer newContainer(TaskAmqpOps ops) {
        UwTaskProperties props = new UwTaskProperties();
        props.setTaskProject("uw.task.migrate.probe");
        TaskMetaInfoManager meta = new TaskMetaInfoManager(null, props);
        TaskListenerManager listeners = new TaskListenerManager();
        TaskDelayTransferContainer transfer = new TaskDelayTransferContainer(null, "uw.task.migrate.probe");
        TaskSendCenter sendCenter = new TaskSendCenter(ops, meta, transfer, listeners);
        return new TaskRunnerContainer(props, new NoopApiClient(), meta, new TaskLocalRateLimiter(), listeners, ops, sendCenter);
    }

    static TaskRunnerConfig quorumConfig() {
        TaskRunnerConfig config = new TaskRunnerConfig();
        config.setTaskClass(TASK_CLASS);
        config.setTaskTag("");
        config.setRunTarget("default");
        config.setQueueType(TaskRunnerConfig.QUEUE_TYPE_QUORUM);
        return config;
    }

    /**
     * 按指定形态声明是否报"队列形态不一致"（声明失败且原因是形态不一致）。
     *
     * @param ops    操作面
     * @param quorum 是否 quorum
     * @return 报形态不一致返回 true；声明成功或其他异常返回 false
     */
    static boolean mismatchWhenDeclaredAs(TaskAmqpOps ops, boolean quorum) {
        try {
            ops.declareDurableQueue(QUEUE, quorum);
            return false;
        } catch (Exception e) {
            return AmqpQueueDeclarer.isQueueTypeMismatch(e);
        }
    }

    static Message message() {
        return new TaskMessageConverter().toMessage(TaskData.<String, String>builder(EchoRunner.class, "migrate").build(), new MessageProperties());
    }

    static RabbitProperties mq() {
        RabbitProperties props = new RabbitProperties();
        props.setHost(System.getProperty("uw.task.rabbitmq.host", "10.0.0.10"));
        props.setPort(Integer.parseInt(System.getProperty("uw.task.rabbitmq.port", "5672")));
        props.setUsername(System.getProperty("uw.task.rabbitmq.username", "rabbit"));
        props.setPassword(System.getProperty("uw.task.rabbitmq.password"));
        return props;
    }

    static void cleanup(TaskAmqpOps ops) {
        try {
            ops.purgeQueue(QUEUE);
            ops.deleteQueue(QUEUE);
        } finally {
            ops.shutdown();
        }
    }

    /**
     * 不外发任务日志的 API 客户端。
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
