package uw.kv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GlobalLocker} 分布式锁真实 Redis 集成测试。
 * <p>覆盖：互斥、CAS 解锁（错误 stamp 不误删）、重复解锁、锁过期再获取、keepLock 续期/重取、强制解锁。</p>
 */
@SpringBootTest(classes = GlobalLockerRedisTest.TestApp.class)
class GlobalLockerRedisTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    private static final String TYPE = "TestLocker" + System.currentTimeMillis();

    @AfterEach
    void cleanUp() {
        GlobalLocker.unlock(TYPE, "k1", 0, true);
        GlobalLocker.unlock(TYPE, "k2", 0, true);
        GlobalLocker.unlock(TYPE, "k3", 0, true);
    }

    @Test
    void tryLockMutualExclusionAndUnlock() {
        long stamp = GlobalLocker.tryLock(TYPE, "k1", 10_000L);
        assertThat(stamp).isPositive();
        //持锁期间再次加锁失败
        assertThat(GlobalLocker.tryLock(TYPE, "k1", 10_000L)).isZero();
        //错误stamp不解锁(CAS)
        assertThat(GlobalLocker.unlock(TYPE, "k1", stamp + 1)).isFalse();
        //锁仍在
        assertThat(GlobalLocker.tryLock(TYPE, "k1", 10_000L)).isZero();
        //正确stamp解锁成功,重复解锁失败
        assertThat(GlobalLocker.unlock(TYPE, "k1", stamp)).isTrue();
        assertThat(GlobalLocker.unlock(TYPE, "k1", stamp)).isFalse();
        //解锁后可重新获取
        assertThat(GlobalLocker.tryLock(TYPE, "k1", 10_000L)).isPositive();
    }

    @Test
    void lockExpiresAndReacquirable() throws InterruptedException {
        long stamp = GlobalLocker.tryLock(TYPE, "k2", 100L);
        assertThat(stamp).isPositive();
        Thread.sleep(300L);
        //锁已过期,可被他人抢占
        long stamp2 = GlobalLocker.tryLock(TYPE, "k2", 10_000L);
        assertThat(stamp2).isPositive();
        assertThat(stamp2).isNotEqualTo(stamp);
        //旧holder解锁不误删新holder的锁(CAS)
        assertThat(GlobalLocker.unlock(TYPE, "k2", stamp)).isFalse();
        assertThat(GlobalLocker.tryLock(TYPE, "k2", 10_000L)).isZero();
    }

    @Test
    void keepLockRenewAndWrongStamp() {
        long stamp = GlobalLocker.tryLock(TYPE, "k3", 10_000L);
        assertThat(stamp).isPositive();
        //正确stamp续期成功;错误stamp失败
        assertThat(GlobalLocker.keepLock(TYPE, "k3", stamp, 10_000L)).isTrue();
        assertThat(GlobalLocker.keepLock(TYPE, "k3", stamp + 1, 10_000L)).isFalse();
        //解锁后 keepLock 对不存在的锁重新获取(Lua elseif 分支)
        assertThat(GlobalLocker.unlock(TYPE, "k3", stamp)).isTrue();
        assertThat(GlobalLocker.keepLock(TYPE, "k3", stamp, 10_000L)).isTrue();
        //重新获取成功则持锁,新stamp解锁干净
        assertThat(GlobalLocker.unlock(TYPE, "k3", stamp)).isTrue();
    }

    @Test
    void forceUnlockIgnoresStamp() {
        assertThat(GlobalLocker.tryLock(TYPE, "k1", 10_000L)).isPositive();
        //强制解锁忽略stamp(管理面用法)
        assertThat(GlobalLocker.unlock(TYPE, "k1", 0, true)).isTrue();
        assertThat(GlobalLocker.tryLock(TYPE, "k1", 10_000L)).isPositive();
    }
}
