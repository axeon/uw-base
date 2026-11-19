package uw.kv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GlobalSequence} 的真实 Redis 集成验证（见 test/resources/application.yml）。
 *
 * <p>覆盖：连续发号、跨号段衔接（config 覆盖小步长强制多次取段）、并发发号全局唯一无重复。</p>
 */
@SpringBootTest(classes = GlobalSequenceRedisTest.TestApp.class)
class GlobalSequenceRedisTest {

    /**
     * 最小启动配置：仅触发自动配置，不做组件扫描。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    /**
     * 本轮测试序列名（唯一化，防共享环境冲突）。
     */
    private static final String NAME = "TestSeq" + System.currentTimeMillis();

    @Autowired
    @Qualifier("longCacheRedisTemplate")
    private RedisTemplate<String, Long> longCacheRedisTemplate;


    @AfterEach
    void cleanUp() {
        longCacheRedisTemplate.delete("uw-seq:" + NAME);
        longCacheRedisTemplate.delete("uw-seq:" + NAME + "c");
        longCacheRedisTemplate.delete("test-seq:" + NAME + "x");
        longCacheRedisTemplate.delete("test-seq:" + NAME + "one");
        longCacheRedisTemplate.delete("uw-seq:" + NAME + "cfg");
    }

    /**
     * 连续发号：同 name 的序列应严格递增。
     */
    @Test
    void sequentialIncrement() {
        long first = GlobalSequence.nextId(NAME);
        long second = GlobalSequence.nextId(NAME);
        long third = GlobalSequence.nextId(NAME);
        assertThat(second).isEqualTo(first + 1);
        assertThat(third).isEqualTo(second + 1);
    }

    /**
     * 跨号段衔接：config 覆盖 step=2 强制每 2 个号取一次段。号段起点为哨兵不发出（documented 行为，
     * 段预扣的固有跳号），断言严格单调递增且跳号不超过段长（不重不回退）。
     */
    @Test
    void crossSegmentMonotonicWithBoundedGaps() {
        // 直测包内 RedisSequence（小步长强制跨段；静态入口固定步长 10000 不适合频繁跨段验证）
        GlobalSequence.RedisSequence seq = new GlobalSequence.RedisSequence("test-seq:" + NAME + "x", 2);
        long prev = seq.nextId();
        for (int i = 0; i < 20; i++) {
            long curr = seq.nextId();
            assertThat(curr).as("应严格单调递增").isGreaterThan(prev);
            assertThat(curr - prev).as("跳号不应超过段长(起点哨兵)").isLessThanOrEqualTo(2);
            prev = curr;
        }
    }

    /**
     * config 注册即生效：覆盖步长后新序列按新步长取段（跳号受新步长约束）。
     */
    @Test
    void configTakesEffectImmediately() {
        GlobalSequence.config(NAME + "cfg", 2);
        long prev = GlobalSequence.nextId(NAME + "cfg");
        for (int i = 0; i < 10; i++) {
            long curr = GlobalSequence.nextId(NAME + "cfg");
            assertThat(curr).isGreaterThan(prev);
            assertThat(curr - prev).as("跳号不超过注册步长2").isLessThanOrEqualTo(2);
            prev = curr;
        }
        // 覆盖为1:走快捷路径,零跳号
        GlobalSequence.config(NAME + "cfg", 1);
        long a = GlobalSequence.nextId(NAME + "cfg");
        assertThat(GlobalSequence.nextId(NAME + "cfg")).as("覆盖为1后应零跳号连续").isEqualTo(a + 1);
    }

    /**
     * 步长=1 快捷路径：逐号 INCR 直取——连续无跳号（对比号段哨兵语义的段起点跳号）、并发唯一。
     */
    @Test
    void stepOneFastPathNoGapsAndUnique() throws InterruptedException {
        GlobalSequence.RedisSequence seq = new GlobalSequence.RedisSequence("test-seq:" + NAME + "one", 1);
        long first = seq.nextId();
        long second = seq.nextId();
        assertThat(second).as("step=1 应零跳号(连续+1)").isEqualTo(first + 1);
        // 并发唯一（INCR 原子,无本地锁）
        int threads = 8, perThread = 20;
        java.util.Set<Long> ids = java.util.concurrent.ConcurrentHashMap.newKeySet();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(threads);
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    latch.await();
                    for (int j = 0; j < perThread; j++) {
                        ids.add(seq.nextId());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        latch.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
        assertThat(ids).as("并发发号应全局唯一").hasSize(threads * perThread);
    }

    /**
     * 并发唯一：多线程并发发号（小步长放大取段竞争），所有 ID 全局无重复。
     */
    @Test
    void concurrentUniqueIds() throws InterruptedException {
        int threads = 8;
        int perThread = 50;
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    latch.await();
                    for (int j = 0; j < perThread; j++) {
                        ids.add(GlobalSequence.nextId(NAME + "c"));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        latch.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
        assertThat(ids).as("并发发号应全局唯一").hasSize(threads * perThread);
    }

}
