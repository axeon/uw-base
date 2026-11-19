package uw.common.util;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 本地固定窗口限速器三种同步策略的 JMH 对照基准（同算法同 API，唯一变量=同步策略）。
 *
 * <p>三版本对照：{@code reentrantLockTryAcquire}（ReentrantLock 策略，本地复刻——2026-11-19 实测后
 * main 现实现已选定该策略）、{@code synchronizedTryAcquire}（监视器锁策略，本地复刻，历史对照）、
 * {@code lockFreeTryAcquire}（双 packed CAS：窗口序号+计数各一个 volatile long 经 CAS 原子推进，
 * 32 位计数饱和）。均走放行热路径（返回 null 零分配），32 线程争用。</p>
 * <p>2026-11-19 本机（Darwin arm64）实测结论：ReentrantLock ≈62M ops/s &gt; synchronized ≈20-25M ops/s
 * &gt;&gt; packed CAS ≈2M ops/s（高争用下单热点 CAS 自旋退化 30 倍；限流判定的单点原子性使 striping 不可用）。
 * 据此 main 实现选定 ReentrantLock。</p>
 *
 * <p>运行方式：执行 main 方法（JDK23+ 需 {@code -Dmaven.compiler.proc=full} 重编生成 BenchmarkList，
 * 并经 {@code dependency:build-classpath} 组装 classpath 后 {@code java -cp} 直跑——exec:java
 * 不传递 fork 子进程 classpath）。</p>
 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(java.util.concurrent.TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 5, time = 3)
@Fork(1)
@Threads(32)
@State(Scope.Benchmark)
public class FixedWindowRateLimiterLockBenchmark {

    /**
     * 对照版A：ReentrantLock（迁移前的原同步策略，算法与 API 与 main 现实现一致）。
     */
    private ReentrantLockVersion lockVersion;

    /**
     * 对照版B：监视器锁策略（本地复刻，历史对照；main 现实现已用 ReentrantLock）。
     */
    private SynchronizedVersion syncVersion;

    /**
     * 对照版C：无锁（双 packed CAS）。
     */
    private LockFreeVersion lockFreeVersion;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(FixedWindowRateLimiterLockBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        lockVersion = new ReentrantLockVersion(600_000L, 1_000_000_000, 1_000_000_000);
        syncVersion = new SynchronizedVersion(600_000L, 1_000_000_000, 1_000_000_000);
        lockFreeVersion = new LockFreeVersion(600_000L, 1_000_000_000, 1_000_000_000);
    }

    @Benchmark
    public Object reentrantLockTryAcquire() {
        return lockVersion.tryAcquire(1, 0);
    }

    @Benchmark
    public Object synchronizedTryAcquire() {
        return syncVersion.tryAcquire(1, 0);
    }

    @Benchmark
    public Object lockFreeTryAcquire() {
        return lockFreeVersion.tryAcquire(1, 0);
    }

    /**
     * 监视器锁对照版：与 ReentrantLock 版同算法同 API，仅同步策略为 synchronized。
     */
    static final class SynchronizedVersion {

        private final long windowSizeInMillis;
        private final int limitPermits;
        private final long limitWeight;
        private long windowIndex = 0;
        private long windowPermits = 0;
        private long windowWeight = 0;

        SynchronizedVersion(long windowSizeInMillis, int limitPermits, long limitWeight) {
            this.windowSizeInMillis = windowSizeInMillis;
            this.limitPermits = limitPermits;
            this.limitWeight = limitWeight;
        }

        FixedWindowRateLimiter.Result tryAcquire(int permits, long weight) {
            if (this.windowSizeInMillis < 1) {
                return null;
            }
            if (permits < 0) {
                permits = 0;
            }
            if (weight < 0) {
                weight = 0;
            }
            synchronized (this) {
                long now = SystemClock.now();
                long idx = now / windowSizeInMillis;
                if (idx != windowIndex) {
                    windowIndex = idx;
                    windowPermits = 0;
                    windowWeight = 0;
                }
                windowPermits += permits;
                if (weight > 0) {
                    windowWeight += weight;
                }
                if (windowPermits > limitPermits || windowWeight > limitWeight) {
                    long remainMillis = windowSizeInMillis - (now - idx * windowSizeInMillis);
                    return new FixedWindowRateLimiter.Result(remainMillis, limitPermits - windowPermits, limitWeight - windowWeight);
                }
                return null;
            }
        }
    }

    /**
     * ReentrantLock 对照版（与 main 现实现同策略）。
     */
    static final class ReentrantLockVersion {

        private final long windowSizeInMillis;
        private final int limitPermits;
        private final long limitWeight;
        private final ReentrantLock lock = new ReentrantLock();
        private long windowIndex = 0;
        private long windowPermits = 0;
        private long windowWeight = 0;

        ReentrantLockVersion(long windowSizeInMillis, int limitPermits, long limitWeight) {
            this.windowSizeInMillis = windowSizeInMillis;
            this.limitPermits = limitPermits;
            this.limitWeight = limitWeight;
        }

        FixedWindowRateLimiter.Result tryAcquire(int permits, long weight) {
            if (this.windowSizeInMillis < 1) {
                return null;
            }
            if (permits < 0) {
                permits = 0;
            }
            if (weight < 0) {
                weight = 0;
            }
            lock.lock();
            try {
                long now = SystemClock.now();
                long idx = now / windowSizeInMillis;
                if (idx != windowIndex) {
                    windowIndex = idx;
                    windowPermits = 0;
                    windowWeight = 0;
                }
                windowPermits += permits;
                if (weight > 0) {
                    windowWeight += weight;
                }
                if (windowPermits > limitPermits || windowWeight > limitWeight) {
                    long remainMillis = windowSizeInMillis - (now - idx * windowSizeInMillis);
                    return new FixedWindowRateLimiter.Result(remainMillis, limitPermits - windowPermits, limitWeight - windowWeight);
                }
                return null;
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 无锁对照版：双 packed CAS。
     * <p>许可状态 = (窗口序号&lt;&lt;32 | 许可计数)，权重状态 = (窗口序号&lt;&lt;32 | 权重累计)，
     * 各一个 volatile long 经 {@link VarHandle} CAS 原子推进：窗口变化时计数归零重打包。
     * 计数 32 位饱和（许可上限 42.9 亿 &gt; int 配额上限 21.5 亿，判定方向安全）；窗口序号取
     * (now-EPOCH)/windowSize 的低 32 位——1ms 窗口下连续两次调用间隔须小于 49.7 天
     * （回绕误判边界，固定窗口限速器不存在超长闲置场景）。</p>
     */
    static final class LockFreeVersion {

        /**
         * 窗口序号基准（2025-01-01 UTC），序号=(now-EPOCH)/windowSize。
         */
        private static final long EPOCH = 1_735_689_600_000L;

        /**
         * 计数字段位宽：32。
         */
        private static final int COUNT_BITS = 32;

        /**
         * 计数掩码。
         */
        private static final long COUNT_MASK = (1L << COUNT_BITS) - 1;

        /**
         * 许可状态 packed：(窗口序号&lt;&lt;32 | 许可计数)。
         */
        private volatile long permitsState;

        /**
         * 权重状态 packed：(窗口序号&lt;&lt;32 | 权重累计)。
         */
        private volatile long weightState;

        private final long windowSizeInMillis;
        private final int limitPermits;
        private final long limitWeight;

        /**
         * permitsState 的 VarHandle。
         */
        private static final VarHandle PERMITS;

        /**
         * weightState 的 VarHandle。
         */
        private static final VarHandle WEIGHT;

        static {
            try {
                PERMITS = MethodHandles.lookup().findVarHandle(LockFreeVersion.class, "permitsState", long.class);
                WEIGHT = MethodHandles.lookup().findVarHandle(LockFreeVersion.class, "weightState", long.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        LockFreeVersion(long windowSizeInMillis, int limitPermits, long limitWeight) {
            this.windowSizeInMillis = windowSizeInMillis;
            this.limitPermits = limitPermits;
            this.limitWeight = limitWeight;
        }

        FixedWindowRateLimiter.Result tryAcquire(int permits, long weight) {
            if (this.windowSizeInMillis < 1) {
                return null;
            }
            if (permits < 0) {
                permits = 0;
            }
            if (weight < 0) {
                weight = 0;
            }
            long now = SystemClock.now();
            long idx = ((now - EPOCH) / windowSizeInMillis) & COUNT_MASK;
            //许可维度:窗口切换与累加原子推进
            long newPermits;
            for (; ; ) {
                long s = permitsState;
                long count = (s >>> COUNT_BITS) == idx ? (s & COUNT_MASK) : 0L;
                newPermits = count + permits;
                if (newPermits > COUNT_MASK) {
                    newPermits = COUNT_MASK;
                }
                if (PERMITS.compareAndSet(this, s, (idx << COUNT_BITS) | newPermits)) {
                    break;
                }
            }
            //权重维度:仅预占时参与(两阶段场景传0,不进CAS路径)
            long newWeight = 0;
            if (weight > 0) {
                for (; ; ) {
                    long s = weightState;
                    long w = (s >>> COUNT_BITS) == idx ? (s & COUNT_MASK) : 0L;
                    newWeight = w + weight;
                    if (newWeight > COUNT_MASK) {
                        newWeight = COUNT_MASK;
                    }
                    if (WEIGHT.compareAndSet(this, s, (idx << COUNT_BITS) | newWeight)) {
                        break;
                    }
                }
            }
            if (newPermits > limitPermits || newWeight > limitWeight) {
                long remainMillis = windowSizeInMillis - ((now - EPOCH) % windowSizeInMillis);
                return new FixedWindowRateLimiter.Result(remainMillis, limitPermits - newPermits, limitWeight - newWeight);
            }
            return null;
        }
    }

}
