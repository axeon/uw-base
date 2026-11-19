package uw.common.util;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * {@link FixedWindowRateLimiter} 的 JMH 吞吐基准。
 *
 * <p>正确性验证见 {@link FixedWindowRateLimiterTest}（JUnit）。本类仅对生产实现做吞吐压测，
 * 确认 ReentrantLock 版本的性能水平。</p>
 *
 * <p>运行方式：执行 main 方法。窗口设大、配额设大，避免测量期间窗口切换或拒绝分支干扰吞吐数据，
 * 纯测 tryAcquire 热路径（临界区纯内存算术）的并发吞吐。</p>
 *
 * @author axeon
 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 5, time = 3)
@Fork(1)
@Threads(32)
@State(Scope.Benchmark)
public class FixedWindowRateLimiterBenchmark {

    private FixedWindowRateLimiter limiter;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(FixedWindowRateLimiterBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        // 窗口 60s 远大于测量时长，配额极大避免触发拒绝分支，纯压热路径
        limiter = new FixedWindowRateLimiter(60_000L, 1_000_000_000, 1_000_000_000);
    }

    @Benchmark
    public FixedWindowRateLimiter.Result tryAcquire() {
        return limiter.tryAcquire(1, 0);
    }
}
