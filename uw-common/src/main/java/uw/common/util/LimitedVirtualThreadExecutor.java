package uw.common.util;

import org.jctools.queues.MpmcArrayQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 背压限制虚拟线程执行器。
 *
 * <p>基于虚拟线程（{@code Thread.ofVirtual}）的并发执行器，核心特征：
 * <ul>
 *   <li><b>并发限制</b>：通过 {@link Semaphore} 限制最大并发数（{@code maxConcurrency}），超过则排队或拒绝。</li>
 *   <li><b>有界队列缓冲</b>：{@code queueCapacity > 0} 时启用（无锁 {@link MpmcArrayQueue}），
 *       worker 完成后从队列接力取下一个任务（μs 级），消除"等外部 submit"的空窗。
 *       {@code queueCapacity = 0} 时不缓冲，满则直接走 {@link RejectedPolicy}。</li>
 *   <li><b>调用策略</b>：当并发满（且队列满，若有）时，委托 {@link RejectedPolicy} 决定行为
 *      （阻塞等待 / 调用者执行 / 丢弃 / 快速失败），与 JDK {@code RejectedExecutionHandler} 对齐。</li>
 *   <li><b>虚拟线程</b>：每个 worker 是独立虚拟线程，IO 阻塞时不占平台载体线程。</li>
 * </ul>
 * </p>
 *
 * <h3>两种模式</h3>
 * <ul>
 *   <li><b>队列模式（{@code queueCapacity > 0}）</b>：submit 先 {@code tryAcquire}，
 *       成功则起 worker 虚拟线程跑<b>接力循环</b>（跑完当前 task 后 {@code queue.poll} 取下一个，
 *       有则继续不 release，无则 release）；Semaphore 满则 {@code offer} 到无锁队列；
 *       队列也满则走 {@link RejectedPolicy}。</li>
 *   <li><b>快速模式（{@code queueCapacity = 0}）</b>：无队列缓冲，Semaphore 满则直接走 {@link RejectedPolicy}。</li>
 * </ul>
 *
 * <h3>关闭语义（对齐 JDK {@code ThreadPoolExecutor}）</h3>
 * <ul>
 *   <li>{@link #shutdown()}：不再接受新任务，已提交的任务（含队列缓冲）继续执行完毕。</li>
 *   <li>{@link #shutdownNow()}：尝试中断正在执行的任务，<b>返回</b>队列中未开始执行的任务列表（含接力队列）。</li>
 *   <li>{@link #awaitTermination(long, TimeUnit)}：等待所有任务完成或超时。</li>
 * </ul>
 *
 * @author axeon
 */
public class LimitedVirtualThreadExecutor {

    /**
     * 并发许可信号量：限制同时执行的 worker 数。
     */
    private final Semaphore semaphore;

    /**
     * 虚拟线程执行器（每任务一个虚拟线程）。
     */
    private final ExecutorService executor;

    /**
     * 调用策略：并发满（且队列满，若有）时的处理行为。与 JDK {@code RejectedExecutionHandler} 对齐。
     */
    private final RejectedPolicy callPolicy;

    /**
     * 正在执行的任务数（worker 活跃数）。
     */
    private final AtomicInteger activeCount = new AtomicInteger(0);

    /**
     * 被拒绝的任务数（走 RejectedPolicy 拒绝的，不含队列缓冲的）。
     */
    private final AtomicInteger rejectedCount = new AtomicInteger(0);

    /**
     * 无锁有界队列（JCTools {@link MpmcArrayQueue}，CAS 无锁）。
     * <p>{@code queueCapacity > 0} 时启用；{@code null} 表示快速模式（无队列缓冲）。</p>
     */
    private final MpmcArrayQueue<Runnable> queue;

    /**
     * 队列容量（0 = 快速模式无队列）。
     */
    private final int queueCapacity;

    /**
     * 最大并发数（= Semaphore 许可数 = 构造传入的 maxConcurrency）。
     */
    private final int maxConcurrency;

    // ==================== 构造函数 ====================

    /**
     * 构造执行器（快速模式，无队列缓冲，默认 {@link CallerRunsPolicy}）。
     *
     * @param maxConcurrency   最大并发数（必须 > 0）
     */
    public LimitedVirtualThreadExecutor(int maxConcurrency) {
        this(null, maxConcurrency, 0, new CallerRunsPolicy());
    }

    /**
     * 构造执行器（快速模式，无队列缓冲，默认 {@link CallerRunsPolicy}）。
     *
     * @param threadNamePrefix 线程名前缀
     * @param maxConcurrency   最大并发数（必须 > 0）
     */
    public LimitedVirtualThreadExecutor(String threadNamePrefix, int maxConcurrency) {
        this(threadNamePrefix, maxConcurrency, 0, new CallerRunsPolicy());
    }

    /**
     * 构造执行器（快速模式，无队列缓冲，指定调用策略）。
     *
     * @param threadNamePrefix 线程名前缀
     * @param maxConcurrency   最大并发数（必须 > 0）
     * @param callPolicy       调用策略（null 则默认 {@link CallerRunsPolicy}）
     */
    public LimitedVirtualThreadExecutor(String threadNamePrefix, int maxConcurrency, RejectedPolicy callPolicy) {
        this(threadNamePrefix, maxConcurrency, 0, callPolicy);
    }

    /**
     * 构造执行器（队列模式，有界队列缓冲，队列满则走 {@link BlockPolicy}）。
     *
     * @param threadNamePrefix 线程名前缀
     * @param maxConcurrency   最大并发数（必须 > 0）
     * @param queueCapacity    队列容量（> 0 启用队列模式；= 0 退化为快速模式）
     */
    public LimitedVirtualThreadExecutor(String threadNamePrefix, int maxConcurrency, int queueCapacity) {
        this(threadNamePrefix, maxConcurrency, queueCapacity, queueCapacity > 0 ? new BlockPolicy() : new CallerRunsPolicy());
    }

    /**
     * 全参构造函数。
     *
     * @param threadNamePrefix 线程名前缀
     * @param maxConcurrency   最大并发数（必须 > 0）
     * @param queueCapacity    队列容量（> 0 启用队列模式；= 0 快速模式）
     * @param callPolicy       调用策略（null 则默认 {@link CallerRunsPolicy}）
     */
    public LimitedVirtualThreadExecutor(String threadNamePrefix, int maxConcurrency, int queueCapacity, RejectedPolicy callPolicy) {
        if (threadNamePrefix == null || threadNamePrefix.isEmpty()) {
            threadNamePrefix = "LimitedVirtualThreadExecutor-";
        }
        if (maxConcurrency <= 0) {
            maxConcurrency = 1;
        }
        if (queueCapacity < 0) {
            queueCapacity = 1;
        }
        this.maxConcurrency = maxConcurrency;
        this.semaphore = new Semaphore(maxConcurrency);
        ThreadFactory factory = Thread.ofVirtual().name(threadNamePrefix, 0).factory();
        this.executor = Executors.newThreadPerTaskExecutor(factory);
        this.queueCapacity = queueCapacity;
        this.queue = queueCapacity > 0 ? new MpmcArrayQueue<>(queueCapacity) : null;
        this.callPolicy = callPolicy != null ? callPolicy : new CallerRunsPolicy();
    }

    // ==================== 核心方法 ====================

    /**
     * 提交任务执行。
     * <p>
     * <b>队列模式</b>：先 {@code tryAcquire}，成功则起 worker 虚拟线程跑接力循环；
     * Semaphore 满则 {@code offer} 到无锁队列；队列也满则走 {@link RejectedPolicy}。<br>
     * <b>快速模式</b>：直接走 {@link RejectedPolicy}（Semaphore 满时由策略决定行为）。
     * </p>
     *
     * @param task 待执行任务（不可为 null）
     * @throws RejectedExecutionException 如果 RejectedPolicy 选择拒绝
     * @throws IllegalStateException      执行器已关闭
     */
    public void submit(Runnable task) {
        if (executor.isShutdown()) {
            throw new IllegalStateException("Executor has been shut down");
        }
        if (queue != null) {
            // 队列模式：不 wrap（startWorker 的接力循环已处理 activeCount），队列存原始 task（shutdownNow 可回收）
            if (semaphore.tryAcquire()) {
                startWorker(task);
            } else if (queue.offer(task)) {
                // 入队成功，等 worker 接力
                // 补偿：应对"所有 worker 已退出"的竞态，确保有 worker 消费此任务
                drainQueueIfPresent();
            } else {
                // 队列也满 → 走 RejectedPolicy（与 JDK ThreadPoolExecutor 一致：pool+queue 满则走 RejectedExecutionHandler）
                try {
                    callPolicy.rejectedExecution(executor, semaphore, task);
                } catch (RejectedExecutionException e) {
                    rejectedCount.incrementAndGet();
                    throw e;
                }
            }
        } else {
            // 快速模式：wrap + 委托 RejectedPolicy
            Runnable wrapped = wrap(task);
            try {
                callPolicy.rejectedExecution(executor, semaphore, wrapped);
            } catch (RejectedExecutionException e) {
                rejectedCount.incrementAndGet();
                throw e;
            }
        }
    }

    /**
     * 队列模式：启动 worker 虚拟线程，跑<b>接力循环</b>。
     * <p>worker 跑完当前 task 后，非阻塞 {@code poll} 队列取下一个：有就接力跑（不 release 许可，消除空窗）；
     * 无就 release 退出，并在退出前调用 {@link #drainQueueIfPresent()} 扫尾补偿（消除 worker 退出与
     * submit offer 之间的竞态，避免孤儿任务）。单任务异常不中断接力循环（catch 吞掉，继续 poll 下一个）。</p>
     * <p><b>许可释放保证</b>：{@code executor.submit()} 本身失败（如执行器已关闭）时，外层 catch 归还
     * {@code tryAcquire} 拿到的许可，避免永久泄漏；同时把任务 best-effort 放回接力队列，便于 {@link #shutdownNow()} 回收。</p>
     *
     * @param firstTask worker 的第一个任务
     */
    private void startWorker(Runnable firstTask) {
        try {
            executor.submit(() -> {
                Runnable task = firstTask;
                try {
                    // while 只判 task 非空：保证已持有的 task（firstTask 或刚 queue.poll 的）一定执行完，
                    // 不因 shutdownNow interrupt 落在 while 条件处而丢失（interrupt 检查改放 run 之后）。
                    while (task != null) {
                        activeCount.incrementAndGet();
                        try {
                            task.run();
                        } catch (Throwable e) {
                            // 单任务异常不中断接力循环（除非是 shutdownNow 的 interrupt）
                        } finally {
                            activeCount.decrementAndGet();
                        }
                        // 被中断（shutdownNow）则不再 poll，退出循环让 shutdownNow 排空队列
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }
                        task = queue.poll();
                    }
                } finally {
                    semaphore.release();
                    // 扫尾补偿：worker 退出时队列可能刚被 offer（submit 与 worker 退出竞态），
                    // 若队列非空则重新启动 worker 消费，避免孤儿任务在 shutdown 后静默丢失
                    drainQueueIfPresent();
                }
            });
        } catch (RejectedExecutionException e) {
            // executor 已关闭：归还 tryAcquire 拿到的许可，避免永久泄漏
            semaphore.release();
            // best-effort 把任务放回接力队列，便于 shutdownNow drain 回收（队列满则丢弃，executor 已关闭无法执行）
            if (queue != null) {
                queue.offer(firstTask);
            }
            throw e;
        }
    }

    /**
     * 扫尾补偿：检查接力队列是否有滞留任务，若有且能获取许可则启动 worker 消费。
     * <p>消除两类竞态导致的孤儿任务：
     * <ul>
     *   <li>worker 退出（release）与 submit offer 之间的竞态——worker 退出时调用；</li>
     *   <li>所有 worker 已退出而 submit 刚 offer 成功——submit offer 后调用。</li>
     * </ul>
     * 两侧补偿保证"offer 成功 ⇒ 必有 worker 最终消费"，避免 shutdown 后队列任务静默丢失。</p>
     * <p>executor 已关闭时 {@link #startWorker} 会抛 {@link RejectedExecutionException}（许可已在其 catch 中归还，
     * 任务已 best-effort 放回队列），此处吞掉异常——扫尾失败不影响主流程，遗留任务由 {@link #shutdownNow()} drain 回收。</p>
     */
    private void drainQueueIfPresent() {
        // executor 已关闭时不再 startWorker：孤儿任务由 shutdownNow 的 queue drain 回收。
        // 此处若再 startWorker 会因 executor 已关闭而失败、把任务 offer 回 queue，与 shutdownNow drain 形成关闭竞态（极罕见但可能丢任务）。
        if (executor.isShutdown()) {
            return;
        }
        if (queue == null || queue.isEmpty()) {
            return;
        }
        if (!semaphore.tryAcquire()) {
            return; // 其他线程持有许可，会接力消费
        }
        Runnable next = queue.poll();
        if (next == null) {
            semaphore.release(); // 队列已被其他线程消费
            return;
        }
        try {
            startWorker(next);
        } catch (RejectedExecutionException e) {
            // executor 已关闭：许可已在 startWorker catch 中归还，任务已放回队列，
            // 由 shutdownNow drain 回收。扫尾失败不影响主流程，吞掉异常。
        }
    }

    // ==================== 关闭方法（对齐 JDK ThreadPoolExecutor）====================

    /**
     * 优雅关闭：不再接受新任务，已提交的任务（含队列缓冲）继续执行完毕。
     *
     * @see ExecutorService#shutdown()
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * 立即关闭：尝试中断正在执行的任务，返回<b>未开始执行</b>的任务列表。
     * <p>返回列表包含队列缓冲中的待执行任务（接力队列中的 Runnable）。
     * 调用方可遍历列表，将任务回收（如写回 zset），避免丢失。</p>
     *
     * @return 未开始执行的任务列表
     * @see ExecutorService#shutdownNow()
     */
    public List<Runnable> shutdownNow() {
        List<Runnable> result = new ArrayList<>(executor.shutdownNow());
        if (queue != null) {
            Runnable r;
            while ((r = queue.poll()) != null) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * 判断执行器是否已调用 {@link #shutdown()} 或 {@link #shutdownNow()}。
     *
     * @return 已关闭返回 true
     * @see ExecutorService#isShutdown()
     */
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    /**
     * 等待所有任务完成或超时。
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 所有任务已完成返回 true，超时返回 false
     * @throws InterruptedException 如果被中断
     * @see ExecutorService#awaitTermination(long, TimeUnit)
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    // ==================== 状态查询（对齐 JDK ThreadPoolExecutor）====================

    /**
     * 返回正在执行的任务数（近似值，worker 活跃数）。
     * <p>对齐 JDK {@code ThreadPoolExecutor.getActiveCount()}。</p>
     *
     * @return 正在执行的任务数
     */
    public int getActiveCount() {
        return activeCount.get();
    }

    /**
     * 返回当前空闲可立即调度的并发槽位数（= {@link Semaphore#availablePermits()}）。
     * <p>与 {@link #getActiveCount()} 互为反义：active=正在执行 {@code run()} 的 worker 数，
     * idle=当前还能再直接 {@code startWorker}（不进队列、不被拒）的新任务数。</p>
     * <p><b>勿用 {@code getMaximumPoolSize() - getActiveCount()} 估算空闲</b>：activeCount 只数已进入
     * {@code run()} 的 worker（含 sleep 中），不数"已 {@code tryAcquire} 持许可、但虚拟线程尚未 mount 进
     * {@code run()}（排队等载体）"的 worker，在脉冲 submit（虚拟线程排队等载体）场景下会<b>高估</b>空闲、
     * 导致批量偏大。本方法直接读 semaphore 真实许可，精确反映可立即占用的并发位。</p>
     *
     * @return 当前空闲并发槽位数（0 ~ maxConcurrency）
     */
    public int getIdleCount() {
        return semaphore.availablePermits();
    }

    /**
     * 返回最大并发数（= 构造传入的 maxConcurrency = Semaphore 许可数）。
     * <p>对齐 JDK {@code ThreadPoolExecutor.getMaximumPoolSize()}。</p>
     *
     * @return 最大并发数
     */
    public int getMaximumPoolSize() {
        return maxConcurrency;
    }

    /**
     * 返回队列容量（= 构造传入的 queueCapacity）。
     * <p>快速模式（queueCapacity=0）返回 0。</p>
     *
     * @return 队列容量
     */
    public int getQueueCapacity() {
        return queueCapacity;
    }

    /**
     * 返回队列中待执行的任务数。
     * <p>对齐 JDK {@code ThreadPoolExecutor.getQueue().size()} 的使用场景。
     * 队列模式下返回 {@link MpmcArrayQueue} 的大小；快速模式（无队列）返回 0。</p>
     *
     * @return 队列待执行任务数
     */
    public int getQueueSize() {
        return queue != null ? queue.size() : 0;
    }

    /**
     * 返回累计被拒绝的任务数（走 {@link RejectedPolicy} 拒绝的）。
     *
     * @return 被拒绝的任务总数
     */
    public int getRejectedCount() {
        return rejectedCount.get();
    }

    // ==================== 内部工具方法 ====================

    /**
     * 快速模式：安全提交任务（确保 Semaphore 正确释放）。
     * <p>{@code executor.submit()} 成功时由内部 lambda 的 finally 释放；
     * {@code executor.submit()} 本身失败时由外层 catch 释放。</p>
     */
    private static void safeSubmit(Runnable task, Semaphore semaphore, ExecutorService executor) {
        try {
            executor.submit(() -> {
                try {
                    task.run();
                } finally {
                    semaphore.release();
                }
            });
        } catch (Throwable e) {
            semaphore.release();
            throw new RejectedExecutionException("Task submission failed", e);
        }
    }

    /**
     * 快速模式：包装任务（记录 activeCount）。
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

    // ==================== 调用策略（对齐 JDK RejectedExecutionHandler）====================

    /**
     * 调用策略接口：当并发满（且队列满，若有）时的处理行为。
     * <p>对齐 JDK {@code RejectedExecutionHandler} 的设计，由调用方选择拒绝/阻塞/调用者执行等行为。</p>
     */
    @FunctionalInterface
    public interface RejectedPolicy {

        /**
         * 处理无法立即执行的任务（并发与队列均满时回调，对齐 JDK {@code RejectedExecutionHandler.rejectedExecution}）。
         *
         * @param executor  虚拟线程执行器
         * @param semaphore 背压信号量
         * @param task      待执行的任务
         * @throws RejectedExecutionException 如果策略选择拒绝
         */
        void rejectedExecution(ExecutorService executor, Semaphore semaphore, Runnable task) throws RejectedExecutionException;
    }

    /**
     * 策略：阻塞等待——{@code semaphore.acquire()} 阻塞直到有许可可用，然后提交执行。
     * <p>对齐 JDK {@code ThreadPoolExecutor} 的"不丢弃"语义，
     * 用阻塞代替丢弃——调用线程挂起直到有空位。适合入队线程可阻塞的场景（如 poll 线程）。</p>
     */
    public static class BlockPolicy implements RejectedPolicy {
        @Override
        public void rejectedExecution(ExecutorService executor, Semaphore semaphore, Runnable task) {
            try {
                semaphore.acquire();
                safeSubmit(task, semaphore, executor);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Blocked and interrupted: " + task, e);
            }
        }
    }

    /**
     * 策略：由调用者线程直接执行——{@code task.run()} 在当前线程同步执行。
     * <p>对齐 JDK {@code ThreadPoolExecutor.CallerRunsPolicy}。提供背压：
     * 调用线程被占用执行任务，自然减缓提交速率。</p>
     */
    public static class CallerRunsPolicy implements RejectedPolicy {
        @Override
        public void rejectedExecution(ExecutorService executor, Semaphore semaphore, Runnable task) {
            if (!semaphore.tryAcquire()) {
                task.run();
            } else {
                safeSubmit(task, semaphore, executor);
            }
        }
    }

    /**
     * 策略：丢弃任务——静默不执行。
     * <p>对齐 JDK {@code ThreadPoolExecutor.DiscardPolicy}。</p>
     */
    public static class DiscardPolicy implements RejectedPolicy {
        @Override
        public void rejectedExecution(ExecutorService executor, Semaphore semaphore, Runnable task) {
            if (semaphore.tryAcquire()) {
                safeSubmit(task, semaphore, executor);
            }
            // 获取不到许可则静默丢弃
        }
    }

    /**
     * 策略：快速失败——抛出 {@link RejectedExecutionException}。
     * <p>对齐 JDK {@code ThreadPoolExecutor.AbortPolicy}（默认策略）。
     * 调用方需 catch 处理（如写回 zset 重试）。</p>
     */
    public static class FailFastPolicy implements RejectedPolicy {
        @Override
        public void rejectedExecution(ExecutorService executor, Semaphore semaphore, Runnable task) {
            if (!semaphore.tryAcquire()) {
                throw new RejectedExecutionException("Concurrency limit reached");
            }
            safeSubmit(task, semaphore, executor);
        }
    }
}
