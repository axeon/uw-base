package uw.task.conf;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import uw.log.es.LogClient;
import uw.task.TaskData;
import uw.task.TaskFactory;
import uw.task.exception.TaskRuntimeException;
import uw.task.rabbit.TaskAmqpOps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 装配门控与运行档位（真实 Spring 上下文，不连 MQ/Redis/ES）：
 * <ul>
 *   <li>{@link UwTaskProperties.RuntimeMode#DISABLED}：整个配置类不注册，上下文照常启动；</li>
 *   <li>{@link UwTaskProperties.RuntimeMode#CRONER_ONLY}（只配 Redis）：装配 TaskFactory，MQ 相关操作入口处快速失败。</li>
 * </ul>
 * 完整档的正常装配由集成测试覆盖（需真实 MQ/Redis）。
 */
class UwTaskAutoConfigurationGatingTest {

    static final String REDIS = "uw.task.redis.host=127.0.0.1";
    static final String MQ = "uw.task.rabbitmq.host=127.0.0.1";

    /**
     * 只提供 taskFactory 方法的两个外部依赖；uw.task 属性由各用例按需给出。
     */
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean("authRestClient", RestClient.class, RestClient::create)
            .withBean(LogClient.class, () -> Mockito.mock(LogClient.class))
            .withUserConfiguration(UwTaskAutoConfiguration.class);

    /**
     * 完全未配置 uw.task：上下文启动成功，没有 TaskFactory。
     */
    @Test
    void disabledWhenNothingConfigured() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(TaskFactory.class);
        });
    }

    /**
     * 只配 MQ 不配 Redis：Redis 是必需项（任务 id 发号等），同样不装配。
     */
    @Test
    void disabledWhenOnlyMqConfigured() {
        runner.withPropertyValues(MQ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(TaskFactory.class);
        });
    }

    /**
     * 连接地址齐备但执行主机缺 task-project：同样不装配。
     */
    @Test
    void disabledWhenRegistryHostMissesTaskProject() {
        runner.withPropertyValues(REDIS, MQ, "uw.task.enable-registry=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(TaskFactory.class);
        });
    }

    /**
     * 只配 Redis：正常装配（仅定时任务档），入队与延时提交直接抛出"未配置 MQ"、不进入重试；队列查询按无数据处理。
     */
    @Test
    void cronerOnlyWhenOnlyRedisConfigured() {
        runner.withPropertyValues(REDIS).run(context -> {
            assertThat(context).hasNotFailed();
            TaskFactory factory = context.getBean(TaskFactory.class);
            assertThatThrownBy(() -> factory.sendToQueue(new TaskData<>("com.demo.NoMqTask", "p")))
                    .isInstanceOf(TaskRuntimeException.class)
                    .hasMessage(TaskAmqpOps.MQ_NOT_CONFIGURED);
            assertThat(factory.getQueueInfo("com.demo.NoMqTask#$default")).containsExactly(0, 0);
            assertThat(factory.purgeQueue("com.demo.NoMqTask#$default")).isEqualTo(-1);
        });
    }
}
