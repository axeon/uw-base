package uw.task.container;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 延时搬运器构造与提交逻辑单测（不连真实 Redis：验证构造/幂等启动/stop 不抛）。
 *
 * @author axeon
 */
class TaskDelayTransferContainerTest {

    /**
     * 构造不抛异常 + setLeader 幂等（同向重复无副作用）+ 落选停止不抛。
     */
    @Test
    @SuppressWarnings("unchecked")
    void constructStartStopWithoutException() {
        TaskDelayTransferContainer container = new TaskDelayTransferContainer(mock(RedisTemplate.class), "test");
        assertThat(container).isNotNull();
        container.setLeader(true);
        container.setLeader(true);
        container.setLeader(false);
    }
}
