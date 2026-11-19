package uw.kv;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import uw.common.util.FixedWindowRateLimiter;

import java.util.concurrent.TimeUnit;

/**
 * 限速器两形态的 JMH 吞吐对比（选型依据，结论同步 uw-kv README"限速器选型"章节）。
 *
 * <p>运行方式：执行 main 方法（需 dev Redis 可达，见 test/resources/application.yml；
 * 密码经环境变量 UW_TEST_REDIS_PASSWORD 注入）。配额设大避免触发拒绝分支，纯压放行热路径：</p>
 * <ul>
 *   <li>fixedWindow：纯内存 ReentrantLock 临界区（uw-common FixedWindowRateLimiter，本地形态）；</li>
 *   <li>globalFixedWindow：每次判定一次 Redis 往返（INCREX，全局形态的固有成本）；</li>
 *       Fusion 的性能下限，展示"batch 配置过小时 Fusion 退化到低于 Global"的边界。</li>
 * </ul>
 * <p>globalFixedWindow 反映跨实例判定的往返成本；fixedWindow 为本地形态参照（无网络参与，量级不可比，仅作上下文）。</p>
 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 5, time = 3)
@Fork(1)
@Threads(16)
@State(Scope.Benchmark)
public class RateLimiterBenchmark {

    private FixedWindowRateLimiter local;
    private GlobalFixedWindowRateLimiter global;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(RateLimiterBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        local = new FixedWindowRateLimiter(600_000L, 1_000_000_000, 1_000_000_000);
        String redisHost = System.getProperty("uw.redis.host", "10.0.0.10");
        int redisPort = Integer.getInteger("uw.redis.port", 6380);
        String redisPassword = System.getenv("UW_TEST_REDIS_PASSWORD");
        org.springframework.data.redis.connection.RedisStandaloneConfiguration config =
                new org.springframework.data.redis.connection.RedisStandaloneConfiguration(redisHost, redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        config.setDatabase(9);
        // 与生产等价的池化配置（池耗尽/建连失败会使基准测到降级路径而非真实性能）
        org.apache.commons.pool2.impl.GenericObjectPoolConfig<io.lettuce.core.api.StatefulConnection<?, ?>> poolConfig =
                new org.apache.commons.pool2.impl.GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(32);
        poolConfig.setMaxIdle(32);
        poolConfig.setMinIdle(8);
        org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory factory =
                new org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory(config,
                        org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration.builder()
                                .commandTimeout(java.time.Duration.ofMillis(2_000L))
                                .poolConfig(poolConfig)
                                .build());
        factory.afterPropertiesSet();
        factory.start();
        // static 家族：经构造完成 static template 注入（与自动配置同款路径），之后走静态方法
        org.springframework.data.redis.core.RedisTemplate<String, Long> template =
                new org.springframework.data.redis.core.RedisTemplate<>();
        template.setKeySerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
        template.setValueSerializer(new org.springframework.data.redis.serializer.GenericToStringSerializer<>(Long.class));
        template.setConnectionFactory(factory);
        template.afterPropertiesSet();
        global = new GlobalFixedWindowRateLimiter(template);
    }

    @Benchmark
    public Object fixedWindowTryAcquire() {
        return local.tryAcquire(1, 0);
    }

    @Benchmark
    public long globalFixedWindowTryAcquire() {
        // 配额 10亿：每次一次 Redis 往返
        return GlobalFixedWindowRateLimiter.tryAcquire("bench", 1_000_000_000, 600_000L, 1);
    }



}
