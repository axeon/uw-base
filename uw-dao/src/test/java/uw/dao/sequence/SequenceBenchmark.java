package uw.dao.sequence;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import uw.dao.SeqTestApplication;

import java.util.concurrent.TimeUnit;

/**
 * DaoSequenceFactory 与 FusionSequenceFactory 的发号吞吐基准。
 *
 * <p>对比三组：</p>
 * <ul>
 *   <li>{@code daoIncr1}：DaoSequenceFactory，incrementNum=1（每号打 DB，最慢基线）。</li>
 *   <li>{@code daoIncr100}：DaoSequenceFactory，incrementNum=100（段缓存生效）。</li>
 *   <li>{@code fusion}：FusionSequenceFactory（Redis 自增 + 段对账）。</li>
 * </ul>
 *
 * <p>运行：执行 main 方法。使用 seqtest profile（application-seqtest.yml）。</p>
 *
 * @author axeon
 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 3)
@Measurement(iterations = 5, time = 3)
@Fork(1)
@Threads(16)
@State(Scope.Benchmark)
public class SequenceBenchmark {

    private static final String SEQ_DAO1 = "TEST_BM_DAO1";
    private static final String SEQ_DAO100 = "TEST_BM_DAO100";
    private static final String SEQ_FUSION = "TEST_BM_FUS";

    private ConfigurableApplicationContext context;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SequenceBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        // 启动 seqtest profile 上下文
        context = SpringApplication.run(SeqTestApplication.class,
                "--spring.profiles.active=seqtest");
        // 预热 Dao 段：先建行并设置 incrementNum
        // daoIncr1：默认 incrementNum=1，先发一个号建行
        DaoSequenceFactory.getSequenceId(SEQ_DAO1);
        // daoIncr100：建行后设 incrementNum=100
        DaoSequenceFactory.getSequenceId(SEQ_DAO100);
        DaoSequenceFactory.resetSequenceId(SEQ_DAO100, 1L, 100);
        clearDaoCache();
        // Fusion：发一个号触发初始化
        FusionSequenceFactory.getSequenceId(SEQ_FUSION);
    }

    @TearDown
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Benchmark
    public long daoIncr1() {
        return DaoSequenceFactory.getSequenceId(SEQ_DAO1);
    }

    @Benchmark
    public long daoIncr100() {
        return DaoSequenceFactory.getSequenceId(SEQ_DAO100);
    }

    @Benchmark
    public long fusion() {
        return FusionSequenceFactory.getSequenceId(SEQ_FUSION);
    }

    @SuppressWarnings("unchecked")
    private void clearDaoCache() {
        try {
            java.lang.reflect.Field f = DaoSequenceFactory.class.getDeclaredField("seqFactory");
            f.setAccessible(true);
            ((java.util.Map<String, DaoSequenceFactory>) f.get(null)).clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
