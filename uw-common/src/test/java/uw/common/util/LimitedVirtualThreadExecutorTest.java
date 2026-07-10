package uw.common.util;

import org.jctools.queues.MpmcArrayQueue;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LimitedVirtualThreadExecutor} 并发与关闭路径测试。
 *
 * <p>重点回归两个历史 bug：
 * <ul>
 *   <li><b>P0</b>：队列模式下 submit 通过 isShutdown 检查并 tryAcquire 成功后，若 executor 在 startWorker 内被关闭，
 *       executor.submit 抛 RejectedExecutionException 导致 Semaphore 许可永久泄漏（worker 饿死）。</li>
 *   <li><b>P1</b>：worker poll 到 null 退出与 submit offer 之间的竞态，导致队列中孤儿任务在 shutdown 后静默丢失。</li>
 * </ul>
 *
 * <p>部分用例通过反射访问 private 字段（semaphore/queue）与方法（startWorker/drainQueueIfPresent），
 * 以确定性触发竞态路径并断言内部契约——这是验证并发 bug 修复的必要手段。
 */
class LimitedVirtualThreadExecutorTest {

    // ==================== 反射辅助 ====================

    private static Semaphore getSemaphore(LimitedVirtualThreadExecutor e) throws Exception {
        Field f = LimitedVirtualThreadExecutor.class.getDeclaredField("semaphore");
        f.setAccessible(true);
        return (Semaphore) f.get(e);
    }

    @SuppressWarnings("unchecked")
    private static MpmcArrayQueue<Runnable> getQueue(LimitedVirtualThreadExecutor e) throws Exception {
        Field f = LimitedVirtualThreadExecutor.class.getDeclaredField("queue");
        f.setAccessible(true);
        return (MpmcArrayQueue<Runnable>) f.get(e);
    }

    /** 反射调用 private startWorker，解包 InvocationTargetException。 */
    private static void invokeStartWorker(LimitedVirtualThreadExecutor e, Runnable task) throws Throwable {
        Method m = LimitedVirtualThreadExecutor.class.getDeclaredMethod("startWorker", Runnable.class);
        m.setAccessible(true);
        try {
            m.invoke(e, task);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    /** 反射调用 private drainQueueIfPresent，解包 InvocationTargetException。 */
    private static void invokeDrain(LimitedVirtualThreadExecutor e) throws Throwable {
        Method m = LimitedVirtualThreadExecutor.class.getDeclaredMethod("drainQueueIfPresent");
        m.setAccessible(true);
        try {
            m.invoke(e);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    // ==================== P0：startWorker 提交失败必须释放许可 ====================

    /**
     * P0 确定性验证：executor 已关闭时调用 startWorker（模拟 submit 通过 isShutdown 检查、tryAcquire 成功后，
     * startWorker 内 executor.submit 失败的路径），必须抛 RejectedExecutionException 并归还许可。
     * <p>修复前此用例泄漏 1 个许可（availablePermits 停在 maxConcurrency-1）。
     */
    @Test
    void testStartWorkerReleasesPermitWhenExecutorShutdown() throws Throwable {
        int maxConcurrency = 3;
        LimitedVirtualThreadExecutor e =
                new LimitedVirtualThreadExecutor("p0-", maxConcurrency, 4);
        Semaphore sem = getSemaphore(e);

        e.shutdown(); // 先关闭 executor，使后续 executor.submit 必然失败
        assertTrue(e.isShutdown());
        assertEquals(maxConcurrency, sem.availablePermits());

        // 模拟 submit 的 tryAcquire 成功（占用一个许可）
        sem.acquire();
        assertEquals(maxConcurrency - 1, sem.availablePermits());

        AtomicBoolean ran = new AtomicBoolean(false);
        // startWorker 应抛 RejectedExecutionException 并释放许可
        assertThrows(RejectedExecutionException.class,
                () -> invokeStartWorker(e, () -> ran.set(true)));

        // 关键断言：许可已归还，无泄漏
        assertEquals(maxConcurrency, sem.availablePermits(),
                "startWorker 失败后许可必须归还，不得泄漏");
        assertFalse(ran.get(), "任务不应被执行（executor 已关闭）");
    }

    // ==================== P1：drainQueueIfPresent 补偿孤儿任务 ====================

    /**
     * P1 补偿机制验证：队列中残留任务且无活跃 worker 时（孤儿），调用 drainQueueIfPresent 应启动 worker 消费。
     * 对应"所有 worker 已退出、submit 刚 offer 成功"与"worker 退出时队列被 offer"两类竞态的补偿路径。
     */
    @Test
    void testDrainQueueIfPresentConsumesOrphanTask() throws Throwable {
        LimitedVirtualThreadExecutor e = new LimitedVirtualThreadExecutor("p1-", 1, 4);
        MpmcArrayQueue<Runnable> queue = getQueue(e);

        CountDownLatch done = new CountDownLatch(1);
        AtomicBoolean ran = new AtomicBoolean(false);
        Runnable task = () -> {
            ran.set(true);
            done.countDown();
        };

        // 直接塞入队列但不启动 worker —— 模拟孤儿任务
        assertTrue(queue.offer(task));
        assertEquals(1, e.getQueueSize());

        // 触发扫尾补偿
        invokeDrain(e);

        assertTrue(done.await(2, TimeUnit.SECONDS), "孤儿任务应被补偿 worker 消费");
        assertTrue(ran.get());
        assertEquals(0, e.getQueueSize());

        e.shutdown();
        assertTrue(e.awaitTermination(2, TimeUnit.SECONDS));
    }

    /**
     * P1 补偿幂等性：队列为空时调用 drainQueueIfPresent 应无副作用（不启动 worker、不获取/泄漏许可）。
     */
    @Test
    void testDrainQueueIfPresentNoopOnEmptyQueue() throws Throwable {
        LimitedVirtualThreadExecutor e = new LimitedVirtualThreadExecutor("p1b-", 2, 4);
        Semaphore sem = getSemaphore(e);

        invokeDrain(e); // 空队列，应直接返回
        assertEquals(2, sem.availablePermits(), "空队列时不得获取/泄漏许可");

        e.shutdown();
        assertTrue(e.awaitTermination(2, TimeUnit.SECONDS));
    }

    // ==================== P0 并发回归：高并发 submit + shutdownNow 不泄漏许可 ====================

    /**
     * P0 并发回归：慢任务占满 worker + 多线程持续 submit + shutdownNow 中断 worker 的竞态下，
     * 关闭完成后许可必须全部归还。修复前若竞态触发 P0 路径会泄漏许可。
     */
    @Test
    void testNoPermitLeakUnderConcurrentShutdown() throws Exception {
        int maxConcurrency = 4;
        int queueCapacity = 8;
        int rounds = 50;

        for (int round = 0; round < rounds; round++) {
            LimitedVirtualThreadExecutor e =
                    new LimitedVirtualThreadExecutor("p0c-" + round + "-", maxConcurrency, queueCapacity);
            Semaphore sem = getSemaphore(e);

            // 慢任务占满所有 worker（持有全部许可）
            CountDownLatch hold = new CountDownLatch(1);
            Runnable slow = () -> {
                try {
                    hold.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            };
            for (int i = 0; i < maxConcurrency; i++) {
                e.submit(slow);
            }

            // 多线程持续 submit，制造与 shutdownNow 的竞态
            AtomicInteger errs = new AtomicInteger();
            int threads = 4;
            Thread[] submitters = new Thread[threads];
            for (int i = 0; i < threads; i++) {
                submitters[i] = new Thread(() -> {
                    Runnable noop = () -> {
                    };
                    for (int j = 0; j < 300; j++) {
                        try {
                            e.submit(noop);
                        } catch (IllegalStateException | RejectedExecutionException ex) {
                            // 关闭后提交被拒（isShutdown 拦截 / FailFast 拒绝），正常
                            errs.incrementAndGet();
                        }
                    }
                });
                submitters[i].start();
            }

            Thread.sleep(3);
            e.shutdownNow();  // 中断 worker（release 许可）+ drain 队列
            hold.countDown(); // 释放慢任务

            for (Thread t : submitters) {
                t.join(5000);
                assertFalse(t.isAlive(), "submitter 线程应已退出");
            }
            assertTrue(e.awaitTermination(5, TimeUnit.SECONDS), "executor 应终止");

            // 关键断言：许可无泄漏
            assertEquals(maxConcurrency, sem.availablePermits(),
                    "第 " + round + " 轮：并发关闭后许可必须全部归还");
        }
    }

    // ==================== P1 端到端回归：优雅关闭不丢任务 ====================

    /**
     * P1 端到端：提交大量任务后 shutdown + awaitTermination，全部任务应被执行（无孤儿丢失）。
     * queueCapacity 足够大以避免触发 FailFast，聚焦验证接力 + 补偿的"不丢"语义。
     */
    @Test
    void testAllTasksExecutedOnGracefulShutdown() throws Exception {
        int total = 2000;
        LimitedVirtualThreadExecutor e =
                new LimitedVirtualThreadExecutor("p1e-", 4, 2048);

        AtomicInteger executed = new AtomicInteger();
        for (int i = 0; i < total; i++) {
            e.submit(executed::incrementAndGet);
        }

        e.shutdown();
        assertTrue(e.awaitTermination(10, TimeUnit.SECONDS), "应在超时前完成所有任务");
        assertEquals(total, executed.get(), "所有提交的任务都应被执行，不得有孤儿丢失");
        assertEquals(0, e.getQueueSize());
    }

    // ==================== 基础功能对照（确保修复未破坏正常路径）====================

    /**
     * 队列模式接力 + 状态查询对照。
     */
    @Test
    void testQueueModeRelayAndStats() throws Exception {
        int maxConcurrency = 3;
        int queueCapacity = 64;
        LimitedVirtualThreadExecutor e =
                new LimitedVirtualThreadExecutor("base-", maxConcurrency, queueCapacity);

        assertEquals(maxConcurrency, e.getMaximumPoolSize());
        assertEquals(queueCapacity, e.getQueueCapacity());

        CountDownLatch done = new CountDownLatch(20);
        AtomicInteger executed = new AtomicInteger();
        for (int i = 0; i < 20; i++) {
            e.submit(() -> {
                executed.incrementAndGet();
                done.countDown();
            });
        }
        assertTrue(done.await(3, TimeUnit.SECONDS), "20 个任务应在超时前完成");
        assertEquals(20, executed.get());

        e.shutdown();
        assertTrue(e.awaitTermination(2, TimeUnit.SECONDS));
        assertEquals(0, e.getActiveCount());
    }

    /**
     * getIdleCount（= semaphore.availablePermits）与 getActiveCount 的 active/idle 对照。
     */
    @Test
    void testGetIdleCount() throws Exception {
        int maxConcurrency = 3;
        LimitedVirtualThreadExecutor e = new LimitedVirtualThreadExecutor("idle-", maxConcurrency, 8);

        // 初始：全部空闲，无活跃
        assertEquals(maxConcurrency, e.getIdleCount());
        assertEquals(0, e.getActiveCount());

        // 占满 worker（持有许可、正在执行 task）
        CountDownLatch hold = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(maxConcurrency);
        for (int i = 0; i < maxConcurrency; i++) {
            e.submit(() -> {
                started.countDown();
                try {
                    hold.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        assertTrue(started.await(2, TimeUnit.SECONDS));
        assertTrue(awaitCondition(() -> e.getIdleCount() == 0, 1000), "占满后空闲槽位应为 0");
        assertEquals(maxConcurrency, e.getActiveCount(), "全部 worker 应在执行 task");

        hold.countDown();
        e.shutdown();
        assertTrue(e.awaitTermination(2, TimeUnit.SECONDS));
        // 关闭后：许可全部归还，无活跃
        assertEquals(maxConcurrency, e.getIdleCount());
        assertEquals(0, e.getActiveCount());
    }

    /**
     * 快速模式（默认 BlockPolicy）基础行为对照。
     */
    @Test
    void testFastModeBasic() throws Exception {
        LimitedVirtualThreadExecutor e = new LimitedVirtualThreadExecutor("fast-", 2);
        assertEquals(0, e.getQueueCapacity());

        CountDownLatch done = new CountDownLatch(5);
        AtomicInteger executed = new AtomicInteger();
        for (int i = 0; i < 5; i++) {
            e.submit(() -> {
                executed.incrementAndGet();
                done.countDown();
            });
        }
        assertTrue(done.await(3, TimeUnit.SECONDS));
        assertEquals(5, executed.get());

        e.shutdown();
        assertTrue(e.awaitTermination(2, TimeUnit.SECONDS));
    }

    /**
     * shutdownNow 返回未执行任务（队列 drain），对齐 TaskDelayerContainer 回收语义。
     */
    @Test
    void testShutdownNowDrainsQueuedTasks() throws Exception {
        LimitedVirtualThreadExecutor e = new LimitedVirtualThreadExecutor("sn-", 1, 16);

        // 慢任务占住唯一 worker
        CountDownLatch hold = new CountDownLatch(1);
        e.submit(() -> {
            try {
                hold.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        });

        // 再提交若干进队列（worker 被占，进队列）
        int queued = 10;
        AtomicInteger ran = new AtomicInteger();
        for (int i = 0; i < queued; i++) {
            e.submit(ran::incrementAndGet);
        }
        // 等队列堆起来
        assertTrue(awaitCondition(() -> e.getQueueSize() >= queued, 1000));

        List<Runnable> remaining = e.shutdownNow();
        hold.countDown();

        // 队列中的任务应被 drain 返回（未执行）
        assertEquals(queued, remaining.size(), "队列任务应被 shutdownNow drain 返回");
        assertEquals(0, ran.get(), "队列中的任务不应被执行");
        assertTrue(e.awaitTermination(2, TimeUnit.SECONDS));
    }

    // ==================== 工具 ====================

    private static boolean awaitCondition(java.util.function.BooleanSupplier cond, long timeoutMs)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (System.nanoTime() < deadline) {
            if (cond.getAsBoolean()) {
                return true;
            }
            Thread.sleep(5);
        }
        return cond.getAsBoolean();
    }
}
