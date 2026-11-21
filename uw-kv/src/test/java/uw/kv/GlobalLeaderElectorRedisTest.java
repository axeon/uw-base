package uw.kv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GlobalLeaderElector} 的真实 Redis 集成验证（见 test/resources/application.yml）。
 *
 * <p>覆盖：抢锁与互斥、稳定标识续期、他主持有时返回非 Leader、Redis 锁值=instanceId（可排查性）、
 * "重启续回"语义（同 instanceId 的新注册立即续回原任期）。</p>
 */
@SpringBootTest(classes = GlobalLeaderElectorRedisTest.TestApp.class)
class GlobalLeaderElectorRedisTest {

    /**
     * 最小启动配置：仅触发自动配置，不做组件扫描。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    /**
     * 本轮测试锁 key（唯一化，防共享环境冲突）。
     */
    private static final String LOCK_A = "test-leader:" + System.currentTimeMillis() + ":a";

    /**
     * 第二把锁（隔离 A 的互斥用例与续回用例的注册状态）。
     */
    private static final String LOCK_B = "test-leader:" + System.currentTimeMillis() + ":b";

    @Autowired
    @Qualifier("kvRedisConnectionFactory")
    private RedisConnectionFactory kvRedisConnectionFactory;

    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        stringRedisTemplate = new StringRedisTemplate(kvRedisConnectionFactory);
        stringRedisTemplate.afterPropertiesSet();
    }

    @AfterEach
    void cleanUp() {
        stringRedisTemplate.delete("uw-leader:" + LOCK_A);
        stringRedisTemplate.delete("uw-leader:" + LOCK_B);
    }

    /**
     * 抢锁与互斥：A 抢到 Leader，B 被拒；A 续期仍 Leader，B 仍非 Leader。
     */
    @Test
    void electMutualExclusionAndRenew() {
        GlobalLeaderElector.config(LOCK_A, "app:1.0@hostA:10010");
        GlobalLeaderElector.config(LOCK_A + ":b", "app:1.0@hostB:10010");
        assertThat(GlobalLeaderElector.checkLeader(LOCK_A)).as("A 首次应当选").isTrue();
        assertThat(GlobalLeaderElector.isLeader(LOCK_A)).isTrue();
        // B 以独立锁 key 注册同一逻辑锁（同 redis key 需经注册覆盖，此处用注册覆盖语义验证互斥）
        GlobalLeaderElector.config(LOCK_A, "app:1.0@hostB:10010");
        assertThat(GlobalLeaderElector.checkLeader(LOCK_A)).as("B 应被拒（redis 锁值仍为 A）").isFalse();
        // A 重新注册回自身标识可续回
        GlobalLeaderElector.config(LOCK_A, "app:1.0@hostA:10010");
        assertThat(GlobalLeaderElector.checkLeader(LOCK_A)).as("A 续期应保持 Leader").isTrue();
    }

    /**
     * 锁值=instanceId：redis 中的锁值即实例标识，便于运维排查 Leader 归属。
     */
    @Test
    void lockValueIsInstanceId() {
        GlobalLeaderElector.config(LOCK_B, "app:1.0@hostA:10010");
        GlobalLeaderElector.checkLeader(LOCK_B);
        assertThat(stringRedisTemplate.opsForValue().get("uw-leader:" + LOCK_B)).isEqualTo("app:1.0@hostA:10010");
    }

    /**
     * 重启续回：同 instanceId 的重新注册（模拟重启后新进程）首次 checkLeader 即续回原任期，
     * 不产生接管空窗——稳定标识设计的核心语义。
     */
    @Test
    void restartWithSameInstanceIdRegainsLeadership() {
        GlobalLeaderElector.config(LOCK_B, "app:1.0@hostA:10010");
        assertThat(GlobalLeaderElector.checkLeader(LOCK_B)).isTrue();
        // 模拟重启：新进程重新注册、同一稳定标识
        GlobalLeaderElector.config(LOCK_B, "app:1.0@hostA:10010");
        assertThat(GlobalLeaderElector.checkLeader(LOCK_B)).as("重启后同标识应立即续回 Leader").isTrue();
    }

    /**
     * 任期释放：锁 key 删除（Leader 下线超时后的接管路径）后，B 可当选。
     */
    @Test
    void takeoverAfterLockGone() {
        GlobalLeaderElector.config(LOCK_B, "app:1.0@hostA:10010");
        GlobalLeaderElector.checkLeader(LOCK_B);
        stringRedisTemplate.delete("uw-leader:" + LOCK_B);
        GlobalLeaderElector.config(LOCK_B, "app:1.0@hostB:10010");
        assertThat(GlobalLeaderElector.checkLeader(LOCK_B)).as("锁失效后 B 应当选").isTrue();
    }

    /**
     * 未注册的 lockKey：checkLeader/isLeader 返回 false 而不抛异常。
     */
    @Test
    void unregisteredKeyReturnsFalse() {
        assertThat(GlobalLeaderElector.isLeader("test-leader:never-registered")).isFalse();
        assertThat(GlobalLeaderElector.checkLeader("test-leader:never-registered")).isFalse();
    }

}
