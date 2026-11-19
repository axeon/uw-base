package uw.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FixedWindowRateLimiter} 的单元测试。
 *
 * <p>覆盖：基本限流、拒绝返回值、窗口切换、两阶段回填（addBytes 不清零请求计数）、
 * 不限流分支、负入参、以及高并发下"窗口切换瞬间"不超放/不丢更新的组合场景。</p>
 *
 * @author axeon
 */
class FixedWindowRateLimiterTest {

    /**
     * 基本：前 limit 个通过，第 limit+1 个拒绝。
     */
    @Test
    @DisplayName("基本限流：前N个通过，超出拒绝")
    void basicLimit() {
        FixedWindowRateLimiter l = new FixedWindowRateLimiter(60_000L, 5, 1_000_000);
        for (int i = 0; i < 5; i++) {
            assertNull(l.tryAcquire(1, 0), "第" + (i + 1) + "个应通过");
        }
        assertNotNull(l.tryAcquire(1, 0), "第6个应被拒绝");
    }

    /**
     * 拒绝返回值：剩余毫秒 > 0（Result 仅携带 remainMillis，超出量属装饰性信息不携带）。
     */
    @Test
    @DisplayName("拒绝返回值：剩余毫秒大于0")
    void rejectReturnValues() {
        FixedWindowRateLimiter l = new FixedWindowRateLimiter(60_000L, 1, 1_000_000);
        assertNull(l.tryAcquire(1, 0));
        FixedWindowRateLimiter.Result r = l.tryAcquire(1, 0);
        assertNotNull(r);
        assertTrue(r.remainMillis > 0, "剩余毫秒>0");
        assertTrue(r.remainPermits < 0, "拒绝时剩余许可额度为负(绝对值即超出量)");
    }

    /**
     * 窗口切换：用 1ms 窗口，跨窗口后应重新放行。
     */
    @Test
    @DisplayName("窗口切换：跨窗口后重新放行")
    void windowRollover() throws InterruptedException {
        FixedWindowRateLimiter l = new FixedWindowRateLimiter(1L, 1, 1_000_000);
        assertNull(l.tryAcquire(1, 0));
        assertNotNull(l.tryAcquire(1, 0), "同窗口第2个应拒绝");
        boolean reAllowed = false;
        for (int i = 0; i < 500; i++) {
            if (l.tryAcquire(1, 0) == null) {
                reAllowed = true;
                break;
            }
            Thread.sleep(0, 200_000); // 0.2ms
        }
        assertTrue(reAllowed, "跨窗口后应重新放行");
    }

    /**
     * 单参重载：单参构造器 + tryAcquire(int)，与双参 (permits, 0) 行为一致。
     */
    @Test
    @DisplayName("单参构造与单参tryAcquire重载")
    void singleDimensionOverloads() {
        FixedWindowRateLimiter l = new FixedWindowRateLimiter(60_000L, 2);
        assertNull(l.tryAcquire(1), "第1个通过");
        assertNull(l.tryAcquire(1), "第2个通过");
        FixedWindowRateLimiter.Result r = l.tryAcquire(1);
        assertNotNull(r, "第3个被拒");
        assertTrue(r.remainPermits < 0, "剩余许可额度为负");
        assertTrue(r.remainWeight > 0, "单维度场景权重额度为正冗余值");
    }

    /**
     * addPermits：回填累加计入窗口（不清零、参与判定），负回填被防御。
     */
    @Test
    @DisplayName("addPermits回填累加与负值防御")
    void addPermitsAccumulatesAndDefendsNegative() {
        FixedWindowRateLimiter l = new FixedWindowRateLimiter(60_000L, 10);
        l.tryAcquire(2);
        l.addPermits(3);
        // 已用 2+3=5，再用5个到10个，第11个被拒
        for (int i = 0; i < 5; i++) {
            assertNull(l.tryAcquire(1), "第" + (i + 6) + "个通过");
        }
        assertNotNull(l.tryAcquire(1), "第11个被拒，证明 addPermits 计入窗口");
        l.addPermits(-100);
        assertEquals(11, readWindowPermits(l), "负回填被丢弃，计数不变");
    }

    /**
     * addWeight 负回填防御：负值不返还权重配额。
     */
    @Test
    @DisplayName("addWeight负回填防御")
    void addWeightDefendsNegative() {
        FixedWindowRateLimiter l = new FixedWindowRateLimiter(60_000L, 1_000_000, 100);
        l.addWeight(80);
        l.addWeight(-200);
        assertEquals(80, readWindowBytes(l), "负回填被丢弃，权重计数不变");
    }

    /**
     * 反射读取当前窗口许可数。
     */
    private static long readWindowPermits(FixedWindowRateLimiter l) {
        try {
            java.lang.reflect.Field f = FixedWindowRateLimiter.class.getDeclaredField("windowPermits");
            f.setAccessible(true);
            return (long) f.get(l);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 回归测试：addBytes 不能清零请求计数（旧实现的 bug）。
     */
    @Test
    @DisplayName("addWeight不清零许可计数")
    void addBytesDoesNotResetRequests() {
        FixedWindowRateLimiter l = new FixedWindowRateLimiter(60_000L, 10, 1_000_000);
        for (int i = 0; i < 8; i++) {
            assertNull(l.tryAcquire(1, 0));
        }
        l.addWeight(500);
        assertNull(l.tryAcquire(1, 0), "第9个通过");
        assertNull(l.tryAcquire(1, 0), "第10个通过");
        assertNotNull(l.tryAcquire(1, 0), "第11个被拒，证明计数未被清零");
    }

    /**
     * windowSizeInMillis < 1 时不限流。
     */
    @Test
    @DisplayName("windowSize<1时不限流")
    void noLimitWhenWindowTooSmall() {
        FixedWindowRateLimiter l = new FixedWindowRateLimiter(0L, 1, 1);
        for (int i = 0; i < 1000; i++) {
            assertNull(l.tryAcquire(1, 1_000_000), "不限流时永远返回null");
        }
    }

    /**
     * 负入参：requests 传 0 应放行且不改变实际占用。
     */
    @Test
    @DisplayName("permits=0放行且不占用配额")
    void zeroRequestsAllowed() {
        FixedWindowRateLimiter l = new FixedWindowRateLimiter(60_000L, 1, 1_000_000);
        assertNull(l.tryAcquire(0, 0), "0个请求应放行");
        assertNull(l.tryAcquire(0, 0), "再次0个请求仍放行");
        // 实际占用仍为0，第1个真实请求应通过
        assertNull(l.tryAcquire(1, 0), "第1个真实请求通过");
        assertNotNull(l.tryAcquire(1, 0), "第2个真实请求被拒");
    }

    /**
     * 并发不超放：32 线程各抢 limit 次，总放行数应恰好 == limit。
     */
    @Test
    @DisplayName("并发不超放：放行数恰好等于limit")
    void concurrentNoOverAllow() throws InterruptedException {
        final int limit = 1000;
        final FixedWindowRateLimiter l = new FixedWindowRateLimiter(60_000L, limit, Integer.MAX_VALUE);
        final AtomicInteger allowed = new AtomicInteger(0);
        final int threads = 32;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    latch.await();
                    for (int j = 0; j < limit; j++) {
                        if (l.tryAcquire(1, 0) == null) {
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
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdownNow();
        int a = allowed.get();
        assertTrue(a <= limit, "放行数" + a + " 不能超过 limit " + limit);
        assertEquals(limit, a, "放行数应恰好等于 limit");
    }

    /**
     * 并发字节累加精确：32 线程各发 1000 次 tryAcquire(1, 12)，字节总数应精确等于 384000。
     */
    @Test
    @DisplayName("并发字节累加精确无丢失")
    void concurrentBytesExact() throws InterruptedException {
        final FixedWindowRateLimiter l = new FixedWindowRateLimiter(60_000L, Integer.MAX_VALUE, Integer.MAX_VALUE);
        final long bytesPerCall = 12L;
        final int callsPerThread = 1000;
        final int threads = 32;
        final long expectedBytes = (long) threads * callsPerThread * bytesPerCall;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    latch.await();
                    for (int j = 0; j < callsPerThread; j++) {
                        l.tryAcquire(1, bytesPerCall);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        latch.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdownNow();
        long actualBytes = readWindowBytes(l);
        assertEquals(expectedBytes, actualBytes, "并发字节累加应精确");
    }

    /**
     * addWeight 并发精确：32 线程各 addBytes(1000) 共 100 次，字节总数应精确等于 3_200_000。
     */
    @Test
    @DisplayName("addWeight并发累加精确无丢失")
    void addBytesConcurrentExact() throws InterruptedException {
        final FixedWindowRateLimiter l = new FixedWindowRateLimiter(60_000L, Integer.MAX_VALUE, Integer.MAX_VALUE);
        final long bytesPerCall = 1000L;
        final int callsPerThread = 100;
        final int threads = 32;
        final long expectedBytes = (long) threads * callsPerThread * bytesPerCall;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    latch.await();
                    for (int j = 0; j < callsPerThread; j++) {
                        l.addWeight(bytesPerCall);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        latch.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdownNow();
        long actualBytes = readWindowBytes(l);
        assertEquals(expectedBytes, actualBytes, "addBytes并发累加应精确");
    }

    /**
     * 反射读取当前窗口权重。
     */
    private static long readWindowBytes(FixedWindowRateLimiter l) {
        try {
            java.lang.reflect.Field f = FixedWindowRateLimiter.class.getDeclaredField("windowWeight");
            f.setAccessible(true);
            return (long) f.get(l);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
