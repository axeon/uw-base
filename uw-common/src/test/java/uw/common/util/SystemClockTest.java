package uw.common.util;


import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)//基准测试类型
@OutputTimeUnit(TimeUnit.MILLISECONDS)//基准测试结果的时间类型
@Warmup(iterations = 1, time = 5)//预热的迭代次数
@Threads(100)//测试线程数量
@State(Scope.Benchmark)//该状态为每个线程独享
//度量:iterations进行测试的轮次，time每轮进行的时长，timeUnit时长单位,batchSize批次数量
@Measurement(iterations = 3, time = 5)
public class SystemClockTest {

    public static void main(String[] args) throws RunnerException {
        //初始化路由管理器
        Options opt = new OptionsBuilder()
                .include(SystemClockTest.class.getSimpleName())
                .forks(0)
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public long currentTimeMillis() {
        return SystemClock.now();
    }

    @Benchmark
    public long systemClock() {
        return SystemClock.now();
    }

//    @Benchmark
//    public long nanoTime() {
//        return System.nanoTime();
//    }

}
