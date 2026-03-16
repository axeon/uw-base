package uw.common.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 背压限制虚拟线程执行器。
 * <p>
 * 背压虚拟线程执行器是一个基于虚拟线程的并发执行器，具有以下特征：
 * <ul>
 *     <li>支持指定最大并发数限制</li>
 *     <li>支持指定调用策略，如阻塞等待、快速失败、由调用者线程执行、丢弃任务等</li>
 *     <li>支持获取当前活跃任务数、等待任务数、可用许可数和拒绝任务数</li>
 *     <li>支持关闭执行器</li>
 * </ul>
 * </p>
 * <p>
 */
public class LimitedVirtualThreadExecutor {

    /**
     * 背压信号量
     */
    private final Semaphore semaphore;
    /**
     * 虚拟线程执行器
     */
    private final ExecutorService executor;
    /**
     * 调用策略
     */
    private final CallPolicy callPolicy;
    /**
     * 活跃任务数
     */
    private final AtomicInteger activeCount = new AtomicInteger(0);
    /**
     * 拒绝任务数
     */
    private final AtomicInteger rejectedCount = new AtomicInteger(0);

    /**
     * 构造函数
     *
     * @param maxConcurrency 最大并发数
     * @param callPolicy     调用策略
     */
    public LimitedVirtualThreadExecutor(int maxConcurrency, CallPolicy callPolicy) {
        if (maxConcurrency <= 0) {
            throw new IllegalArgumentException("maxConcurrency must be positive");
        }
        this.semaphore = new Semaphore(maxConcurrency);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.callPolicy = callPolicy != null ? callPolicy : new BlockPolicy();
    }

    /**
     * 构造函数
     *
     * @param maxConcurrency
     */
    public LimitedVirtualThreadExecutor(int maxConcurrency) {
        this(maxConcurrency, new BlockPolicy());
    }

    /**
     * 安全提交任务（内部工具方法）
     */
    private static void safeSubmit(Runnable task, Semaphore semaphore, ExecutorService executor) {
        try {
            executor.submit(() -> {
                try {
                    task.run();
                } catch (Throwable e) {
                    // Log the error but ensure cleanup in finally
                    throw new RuntimeException("Task execution failed", e);
                } finally {
                    semaphore.release();
                }
            });
        } catch (Throwable e) {  // CHANGED: Catch Throwable instead of Exception
            // Release permit if submission fails (e.g., executor shut down)
            semaphore.release();
            throw new RejectedExecutionException("Task submission failed", e);
        }
    }

    /**
     * 提交任务（核心方法）
     */
    public void submit(Runnable task) {
        if (executor.isShutdown()) {
            throw new IllegalStateException("Executor has been shut down");
        }
        try {
            callPolicy.execute(executor, semaphore, wrap(task));
        } catch (RejectedExecutionException e) {
            rejectedCount.incrementAndGet();
            throw e;
        }
    }

    /**
     * 包装任务：记录活跃数
     */
    private Runnable wrap(Runnable task) {
        return () -> {
            activeCount.incrementAndGet();
            try {
                task.run();
            } finally {
                activeCount.decrementAndGet();
            }
        };
    }

    /**
     * 获得活跃任务数。
     */
    public int getActiveCount() {
        return activeCount.get();
    }

    /**
     * 获得等待任务数。
     */
    public int getQueuedTasks() {
        return semaphore.getQueueLength();
    }

    /**
     * 获得可用许可数。
     */
    public int getAvailablePermits() {
        return semaphore.availablePermits();
    }

    /**
     * 获得拒绝任务数。
     */
    public int getRejectedCount() {
        return rejectedCount.get();
    }

    /**
     * 关闭执行器。
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * 判断执行器是否已关闭。
     */
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    /**
     * 调用策略：当达到并发上限时的处理行为
     */
    @FunctionalInterface
    public interface CallPolicy {
        /**
         * @param executor  虚拟线程执行器
         * @param semaphore 背压信号量
         * @param task      待执行的任务
         * @throws RejectedExecutionException 当策略选择拒绝时抛出
         */
        void execute(ExecutorService executor, Semaphore semaphore, Runnable task) throws RejectedExecutionException;
    }

    /**
     * 策略实现1：阻塞等待（默认）
     */
    public static class BlockPolicy implements CallPolicy {
        @Override
        public void execute(ExecutorService executor, Semaphore semaphore, Runnable task) {
            try {
                semaphore.acquire(); // 阻塞直到获取许可
                safeSubmit(task, semaphore, executor);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Task " + task.toString() + " rejected from " + e.toString());
            }
        }
    }

    /**
     * 策略实现3：由调用者线程执行
     */
    public static class CallerRunsPolicy implements CallPolicy {
        @Override
        public void execute(ExecutorService executor, Semaphore semaphore, Runnable task) {
            if (!semaphore.tryAcquire()) {
                // 无法获取许可，由调用者直接执行（类似ThreadPoolExecutor的CallerRunsPolicy）
                task.run();
            } else {
                safeSubmit(task, semaphore, executor);
            }
        }
    }

    /**
     * 策略实现4：丢弃任务
     */
    public static class DiscardPolicy implements CallPolicy {
        @Override
        public void execute(ExecutorService executor, Semaphore semaphore, Runnable task) {
            if (!semaphore.tryAcquire()) {
                // 无法获取许可，直接丢弃任务
                return;
            }
            // 获取到许可，提交任务执行
            safeSubmit(task, semaphore, executor);
        }
    }

    /**
     * 策略实现2：快速失败
     */
    public static class FailFastPolicy implements CallPolicy {
        @Override
        public void execute(ExecutorService executor, Semaphore semaphore, Runnable task) {
            if (!semaphore.tryAcquire()) {
                throw new RejectedExecutionException("Concurrency limit reached: " + semaphore.availablePermits());
            }
            safeSubmit(task, semaphore, executor);
        }
    }
}

