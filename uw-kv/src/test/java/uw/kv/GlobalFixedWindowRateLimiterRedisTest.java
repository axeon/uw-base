package uw.kv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GlobalFixedWindowRateLimiter} 的真实 Redis 集成验证（见 test/resources/application.yml）。
 *
 * <p>覆盖：基本限流与超限剩余毫秒、跨窗口放行、负参/零参不限流、并发不超放（放行数严格 == requests）、
 * INCREX PXAT 主路径下的窗口 TTL 生效（dev Redis 8.8+）。</p>
 */
@SpringBootTest(classes = GlobalFixedWindowRateLimiterRedisTest.TestApp.class)
class GlobalFixedWindowRateLimiterRedisTest {

    /**
     * 最小启动配置：仅触发自动配置，不做组件扫描。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    /**
     * 本轮测试 name（唯一化，防共享环境冲突）。
     */
    private static final String NAME = "TestGFWRL" + System.currentTimeMillis();

    @Autowired
    @Qualifier("longCacheRedisTemplate")
    private RedisTemplate<String, Long> longCacheRedisTemplate;

    @AfterEach
    void cleanUp() {
        for (String suffix : new String[]{"", "c", "2d", "2dw"}) {
            longCacheRedisTemplate.delete("uw-rate:" + NAME + suffix);
            longCacheRedisTemplate.delete("uw-rate:" + NAME + suffix + ":p");
            longCacheRedisTemplate.delete("uw-rate:" + NAME + suffix + ":w");
        }
    }

    /**
     * 基本：前 N 个通过（返回 0），第 N+1 个超限（返回剩余毫秒>0）。
     */
    @Test
    void basicLimitAndRejectReturn() {
        // 大窗口（120s）保证测试期内不切窗
        long windowMillis = 120_000L;
        for (int i = 0; i < 5; i++) {
            long r = GlobalFixedWindowRateLimiter.tryAcquire(NAME, 5, windowMillis, 1);
            assertThat(r).as("第" + (i + 1) + "个应通过").isZero();
        }
        long rejected = GlobalFixedWindowRateLimiter.tryAcquire(NAME, 5, windowMillis, 1);
        assertThat(rejected).as("第6个应超限").isPositive();
        assertThat(rejected).as("剩余毫秒不超过窗口长度").isLessThanOrEqualTo(windowMillis);
    }

    /**
     * 跨窗口：1 秒窗口配额 1，连续申请必然观测到同窗超限；此后持续申请应观测到跨窗重新放行。
     * <p>不做"连续两次调用必同窗"的硬断言——调用跨越窗口边界时放行是正确行为（新窗口新配额），
     * 断言改为对两种现象的循环观测，消除时序脆弱性。</p>
     */
    @Test
    void windowRolloverReAllows() throws InterruptedException {
        assertThat(GlobalFixedWindowRateLimiter.tryAcquire(NAME + "c", 1, 1000L, 1)).isZero();
        boolean sawReject = false;
        long deadline = System.currentTimeMillis() + 3_000L;
        while (System.currentTimeMillis() < deadline) {
            if (GlobalFixedWindowRateLimiter.tryAcquire(NAME + "c", 1, 1000L, 1) > 0) {
                sawReject = true;
                break;
            }
        }
        assertThat(sawReject).as("1秒窗口内应观测到超限").isTrue();
        boolean reAllowed = false;
        deadline = System.currentTimeMillis() + 3_000L;
        while (System.currentTimeMillis() < deadline) {
            if (GlobalFixedWindowRateLimiter.tryAcquire(NAME + "c", 1, 1000L, 1) == 0) {
                reAllowed = true;
                break;
            }
            Thread.sleep(1);
        }
        assertThat(reAllowed).as("跨窗口后应重新放行").isTrue();
    }

    /**
     * 零参/负参：requests 或 seconds 为 0/负数时不限流（恒返回 0，且不写 redis key）。
     */
    @Test
    void noLimitOnZeroOrNegativeParams() {
        assertThat(GlobalFixedWindowRateLimiter.tryAcquire(NAME, 0, 10_000L, 1)).isZero();
        assertThat(GlobalFixedWindowRateLimiter.tryAcquire(NAME, 10, 0L, 1)).isZero();
        assertThat(GlobalFixedWindowRateLimiter.tryAcquire(NAME, -1, 10_000L, 1)).isZero();
        assertThat(GlobalFixedWindowRateLimiter.tryAcquire(NAME, 10, -1L, 1)).isZero();
        assertThat(longCacheRedisTemplate.hasKey("uw-rate:" + NAME)).isFalse();
    }

    /**
     * 并发不超放：32 线程共抢 requests 配额，放行总数应恰好 == requests（INCREX 原子累加保证）。
     */
    @Test
    void concurrentNoOverAllow() throws InterruptedException {
        int requests = 50;
        int threads = 32;
        java.util.concurrent.atomic.AtomicInteger allowed = new java.util.concurrent.atomic.AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    latch.await();
                    for (int j = 0; j < 5; j++) {
                        if (GlobalFixedWindowRateLimiter.tryAcquire(NAME + "c", requests, 120_000L, 1) == 0) {
                            allowed.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        latch.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
        assertThat(allowed.get()).as("放行数应恰好等于 requests").isEqualTo(requests);
    }

    /**
     * Lua fallback 路径：强制冷却走降级脚本，验证判定内嵌与 PTTL 返回（脚本语法在此路径暴露）。
     */
    @Test
    void luaFallbackLimitsAndReturnsPttl() {
        GlobalFixedWindowRateLimiter.markIncrexUnavailableForTest();
        try {
            for (int i = 0; i < 3; i++) {
                long r = GlobalFixedWindowRateLimiter.tryAcquire(NAME, 3, 120_000L, 1);
                assertThat(r).as("Lua路径 前3个应通过").isZero();
            }
            long rejected = GlobalFixedWindowRateLimiter.tryAcquire(NAME, 3, 120_000L, 1);
            assertThat(rejected).as("Lua路径 第4个应超限").isPositive();
            assertThat(rejected).as("PTTL返回值不超过窗口长度").isLessThanOrEqualTo(120_000L);
            Long ttl = longCacheRedisTemplate.getExpire("uw-rate:" + NAME, java.util.concurrent.TimeUnit.MILLISECONDS);
            assertThat(ttl).as("Lua路径窗口 key 必须有 TTL").isPositive();
        } finally {
            GlobalFixedWindowRateLimiter.resetIncrexUnavailableForTest();
        }
    }

    /**
     * 双维度：许可超限拒绝、权重超限拒绝、双过通过、weight=0 不预占（两阶段入口）。
     */
    @Test
    void twoDimensionLimits() {
        String n = NAME + "2d";
        // 许可维度超限（权重远未到）
        assertThat(GlobalFixedWindowRateLimiter.tryAcquire(n, 2, 1_000_000L, 120_000L, 1, 0)).isZero();
        assertThat(GlobalFixedWindowRateLimiter.tryAcquire(n, 2, 1_000_000L, 120_000L, 1, 0)).isZero();
        assertThat(GlobalFixedWindowRateLimiter.tryAcquire(n, 2, 1_000_000L, 120_000L, 1, 0)).as("许可超限应拒绝").isPositive();
        // 权重维度超限（许可远未到）
        String nw = NAME + "2dw";
        assertThat(GlobalFixedWindowRateLimiter.tryAcquire(nw, 1_000_000, 100L, 120_000L, 1, 60)).isZero();
        assertThat(GlobalFixedWindowRateLimiter.tryAcquire(nw, 1_000_000, 100L, 120_000L, 1, 60)).as("权重超限应拒绝").isPositive();
        // 两阶段：入口 weight=0 不预占,事后回填计入
        String n2 = NAME + "2d";
        GlobalFixedWindowRateLimiter.addWeight(n2, 120_000L, 500);
        Long w = longCacheRedisTemplate.opsForValue().get("uw-rate:" + n2 + ":w");
        assertThat(w).as("回填权重应计入 w key").isEqualTo(500L);
        // 双 key 均有 TTL（防永生）
        assertThat(longCacheRedisTemplate.getExpire("uw-rate:" + n2 + ":p", java.util.concurrent.TimeUnit.MILLISECONDS)).isPositive();
        assertThat(longCacheRedisTemplate.getExpire("uw-rate:" + n2 + ":w", java.util.concurrent.TimeUnit.MILLISECONDS)).isPositive();
    }

    /**
     * 窗口 TTL 生效：INCREX PXAT 设置的 key 应有正 TTL（窗口终点回收，防 key 永生）。
     */
    @Test
    void windowKeyHasTtl() {
        GlobalFixedWindowRateLimiter.tryAcquire(NAME, 100, 120_000L, 1);
        Long ttl = longCacheRedisTemplate.getExpire("uw-rate:" + NAME, TimeUnit.MILLISECONDS);
        assertThat(ttl).as("窗口 key 必须有 TTL（PXAT）").isPositive();
    }

}
