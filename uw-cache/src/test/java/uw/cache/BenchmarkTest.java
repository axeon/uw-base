package uw.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput})//基准测试类型
@OutputTimeUnit(TimeUnit.MILLISECONDS)//基准测试结果的时间类型
@Warmup(iterations = 1, time = 3, timeUnit = TimeUnit.SECONDS)//预热的迭代次数
@Threads(20)//测试线程数量
@State(Scope.Benchmark)//该状态为每个线程独享
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS, batchSize = -1)
public class BenchmarkTest {
    private ConfigurableApplicationContext context;

    private ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

    private LoadingCache<String, String> caffeine = Caffeine.newBuilder().maximumSize( 100 ).build( key -> "hello " + key );

    private LoadingCache<String, String> caffeineWithExpire = Caffeine.newBuilder().maximumSize( 100 ).expireAfterWrite( 1, TimeUnit.HOURS ).build( key -> "hello " + key );

    private LoadingCache<String, String> caffeineWithStats = Caffeine.newBuilder().maximumSize( 100 ).recordStats().build( key -> "hello " + key );


    public BenchmarkTest() {
    }

    public static void main(String[] args) throws RunnerException {
        Options opts = new OptionsBuilder().include( BenchmarkTest.class.getSimpleName() ).forks( 1 ).build();
        new Runner( opts ).run();
    }

    @Setup
    public void init() {
        context = SpringApplication.run( UwCacheTest1Application.class );
        FusionCache.Config fusionConfig = FusionCache.Config.builder().cacheName( "fusion" ).localCacheMaxNum( 1000 ).globalCacheExpireMillis( 1000000 ).build();
        FusionCache.config( fusionConfig, new CacheDataLoader<String, String>() {
            @Override
            public String load(String key) {
                return "hello " + key;
            }
        } );
        FusionCache.Config caffineConfig = FusionCache.Config.builder().cacheName( "fusionLocal" ).localCacheMaxNum( 1000 ).globalCacheExpireMillis( -1 ).build();
        FusionCache.config( caffineConfig, new CacheDataLoader<String, String>() {
            @Override
            public String load(String key) {
                return "hello " + key;
            }
        } );
    }

//    /**
//     * 测试jdk map。
//     */
//    @Benchmark
//    public void testMapCache() {
//        map.computeIfAbsent( "123", key -> "hello " + key );
//    }

//    /**
//     * 测试caffeine缓存。
//     */
//    @Benchmark
//    public void testCaffeineCache() {
//        caffeine.get( "123" );
//    }

//    /**
//     * 测试caffeine缓存。
//     */
//    @Benchmark
//    public void testCaffeineWithExpireCache() {
//        caffeineWithExpire.get( "123" );
//    }
//
//    /**
//     * 测试caffeine缓存。
//     */
//    @Benchmark
//    public void testCaffeineWithStats() {
//        caffeineWithStats.get( "123" );
//    }

    /**
     * 测试fusion纯本地缓存。
     */
    @Benchmark
    public void testFusionLocalCache() {
        FusionCache.get( "fusionLocal", "123" );
    }

    /**
     * 测试fusion复合缓存。
     */
    @Benchmark
    public void testFusionCache() {
        FusionCache.get( "fusion", "123" );
    }

//    /**
//     * 测试纯redis缓存。
//     */
//    @Benchmark
//    public void testGlobalCache() {
//        GlobalCache.get( "test", 123, new CacheDataLoader<Integer, String>() {
//            @Override
//            public String load(Integer key) {
//                return "hello " + key;
//            }
//        }, 600_000L, 1000, 100, 10 );
//    }

    @TearDown
    public void down() {
        context.close();
    }


}
