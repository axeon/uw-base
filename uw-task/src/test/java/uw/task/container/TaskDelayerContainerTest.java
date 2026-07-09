package uw.task.container;

import org.junit.jupiter.api.Test;
import uw.task.api.TaskApiClient;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.conf.TaskProperties;
import uw.task.util.TaskGlobalRateLimiter;
import uw.task.util.TaskLocalRateLimiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link TaskDelayerContainer} 单元测试。
 *
 * <p>Container 的核心流程（Lua 原子 poll、多实例竞争、worker 虚拟线程池、限速等待、异常分类重试、统计日志）
 * 依赖真实 Redis 且内部自建 RedisTemplate（生产路径，不可注入 Mock），难以纯单元覆盖。
 * 本类聚焦可单测的纯逻辑（configKey 三元组、构造）；核心流程的端到端验证见
 * plan 的集成验证步骤（真实 Redis + demo TaskDelayer）。</p>
 */
class TaskDelayerContainerTest {


    /**
     * 构造不抛异常（自建 RedisTemplate + mock 连接工厂）。
     */
    @Test
    void constructWithoutException() {
        TaskProperties props = new TaskProperties();
        TaskDelayerContainer container = new TaskDelayerContainer(
                props, mock(TaskApiClient.class), mock(TaskMetaInfoManager.class),
                mock(TaskLocalRateLimiter.class), mock(TaskGlobalRateLimiter.class),
                mock(uw.task.util.TaskSequenceManager.class), mock(org.springframework.data.redis.connection.RedisConnectionFactory.class),
                mock(uw.task.TaskListenerManager.class));
        assertThat(container).isNotNull();
    }
}
