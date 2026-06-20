package uw.dao.sequence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import uw.dao.DaoFactory;
import uw.dao.SeqTestApplication;
import uw.dao.conf.DaoConfigManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FusionSequenceFactory} 当前行为的固化测试。
 *
 * <p>采用"严格断言"策略：并发发号必须全部唯一。若当前实现存在并发竞态导致重复发号，
 * 测试将以红色失败——这正是需要修复的 P0 bug 的证据。</p>
 *
 * <p>清理：每个用例前后删除 Redis 中测试用的 seq key 与 pool key，以及 DB 的 sys_seq 行。</p>
 *
 * @author axeon
 */
@SpringBootTest(classes = SeqTestApplication.class)
@ActiveProfiles("seqtest")
class FusionSequenceFactoryTest {

    private static final String SEQ_PREFIX = "TEST_FUS_";
    private static final String REDIS_SEQ = "uw-dao-seq:";
    private static final String REDIS_SEQ_POOL = "uw-dao-seq-pool:";

    private final DaoFactory daoFactory = DaoFactory.getInstance();

    /**
     * 生成唯一 seqName（带 nanoTime 后缀），确保每个测试/每次运行的 Redis key 与 DB 行互不干扰，
     * 规避 Redis 跨库清理不彻底导致的残留污染。
     */
    private static String uniqueSeq(String tag) {
        return SEQ_PREFIX + tag + "_" + System.nanoTime();
    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    @AfterEach
    void cleanup() {
        // 清理 DB 测试 seq 行
        try {
            String rootConn = DaoConfigManager.getRouteMapping("sys_seq", "write");
            daoFactory.execute(rootConn, "delete from sys_seq where seq_name like ?", new Object[]{SEQ_PREFIX + "%"});
        } catch (Exception e) {
            // 忽略
        }
        // 清理 Dao 本地缓存（Fusion 内部会调 DaoSequenceFactory）
        clearDaoFactoryCache();
        // 清理 Redis 所有测试相关 key（段缓存 cursor/end/renew + 池 + 锁）
        try {
            deleteByPattern(REDIS_SEQ + SEQ_PREFIX + "*");
            deleteByPattern(REDIS_SEQ_POOL + SEQ_PREFIX + "*");
        } catch (Exception e) {
            // 忽略
        }
    }

    /**
     * 基本发号：连续两次调用，结果递增。
     */
    @Test
    @DisplayName("基本发号：连续调用单调递增")
    void basicIncrement() {
        String seq = uniqueSeq("basic");
        long a = FusionSequenceFactory.getSequenceId(seq);
        long b = FusionSequenceFactory.getSequenceId(seq);
        long c = FusionSequenceFactory.getSequenceId(seq);
        assertTrue(b == a + 1, "第二次=" + b + " 应为 " + (a + 1));
        assertTrue(c == b + 1, "第三次=" + c + " 应为 " + (b + 1));
    }

    /**
     * 并发不重复（严格）：32 线程各发若干号，所有返回值必须唯一。
     * 这是发号器最核心的硬约束。若失败，说明当前实现存在并发重复发号 bug。
     */
    @Test
    @DisplayName("并发发号：所有返回值必须唯一（严格断言）")
    void concurrentNoDuplicate() throws InterruptedException {
        String seq = uniqueSeq("conc");
        final int threads = 32;
        final int perThread = 500;
        final int total = threads * perThread;
        ConcurrentHashMap<Long, Long> seen = new ConcurrentHashMap<>();
        long[] duplicates = {0};
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    latch.await();
                    for (int j = 0; j < perThread; j++) {
                        long id = FusionSequenceFactory.getSequenceId(seq);
                        if (seen.put(id, id) != null) {
                            duplicates[0]++;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        latch.countDown();
        try {
            assertTrue(done.await(120, TimeUnit.SECONDS), "并发任务应按时完成");
        } finally {
            pool.shutdownNow();
        }
        assertEquals(total, seen.size(),
                "应发出 " + total + " 个唯一ID，实际唯一数 " + seen.size() + "，重复 " + duplicates[0] + " 个（存在并发重复发号 bug！）");
    }

    /**
     * 零丢号校验（严格）：纯 Fusion 单源并发发号，所有返回值必须构成<b>无空洞的连续区间</b>。
     * <p>发号器的硬约束不只是"不重复"，还包括"不丢失"——已从 DB 申请的号必须全部发出。
     * 旧实现"先 INCR 再判越界"会在段切换瞬间丢号；本测试验证修复后零丢号：
     * N 个号应恰好覆盖 [min, min+N-1]，无重复、无缺失。</p>
     */
    @Test
    @DisplayName("零丢号：纯 Fusion 并发发号构成无空洞连续区间")
    void noHoleConcurrent() throws InterruptedException {
        String seq = uniqueSeq("nohole");
        final int threads = 16;
        final int perThread = 600; // 总量 9600 ≈ POOL_SIZE(10000)，会触发多次段切换
        final int total = threads * perThread;
        ConcurrentHashMap<Long, Long> seen = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    latch.await();
                    for (int j = 0; j < perThread; j++) {
                        long id = FusionSequenceFactory.getSequenceId(seq);
                        seen.put(id, id);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        latch.countDown();
        try {
            assertTrue(done.await(120, TimeUnit.SECONDS), "并发任务应按时完成");
        } finally {
            pool.shutdownNow();
        }
        assertEquals(total, seen.size(),
                "应发出 " + total + " 个唯一ID，实际唯一数 " + seen.size() + "（存在重复发号！）");
        // 零丢号：集合应为连续区间，无空洞
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (long id : seen.keySet()) {
            if (id < min) min = id;
            if (id > max) max = id;
        }
        assertEquals(total, max - min + 1,
                "号段应无空洞：min=" + min + " max=" + max + " 区间长度=" + (max - min + 1)
                        + " 应等于发号数 " + total + "（存在丢号 bug！）");
    }

    /**
     * resetSequenceId 后发号 >= 重置值。
     */
    @Test
    @DisplayName("resetSequenceId 后发号 >= 重置值")
    void resetSequence() {
        String seq = uniqueSeq("reset");
        FusionSequenceFactory.getSequenceId(seq);
        FusionSequenceFactory.getSequenceId(seq);
        FusionSequenceFactory.resetSequenceId(seq, 500_000L);
        long next = FusionSequenceFactory.getSequenceId(seq);
        assertTrue(next >= 500_000L, "重置后发号=" + next + " 应 >= 500000");
    }

    /**
     * 交替混合发号冲突检测（严格）：同一线程内交替调用 Fusion 与 Dao 取号，
     * 每个发出的 ID 记入 idMap 并标注来源，若同一 ID 被两源重复发出则视为冲突。
     * <p>与 {@link #mixedCallNoDuplicate} 互补：前者按线程分派到单一发号源，
     * 本测试在<b>同一线程内</b>紧挨着交替取 Fusion 号与 Dao 号，覆盖更细粒度的混合竞态。
     * 段切换、DB 段分配交错下，两源段缓存必须互不重叠，所有 ID 全局唯一。</p>
     */
    @Test
    @DisplayName("交替混合发号(Fusion↔Dao)：同线程交替取号，全部唯一无冲突")
    void checkMixedSeqAlternating() throws InterruptedException {
        String seq = uniqueSeq("altmix");
        // 先建行并给 Dao 侧较大 incrementNum，避免 Dao 打 DB 过慢拖垮测试
        DaoSequenceFactory.getSequenceId(seq);
        DaoSequenceFactory.resetSequenceId(seq, 1L, 100);
        clearDaoFactoryCache();
        FusionSequenceFactory.getSequenceId(seq);

        final int threads = 32;
        final int perThread = 400; // 每线程交替取 (Fusion, Dao) 共 perThread*2 个号
        final int total = threads * perThread * 2;
        ConcurrentHashMap<Long, String> idMap = new ConcurrentHashMap<>();
        long[] duplicates = {0};
        java.util.concurrent.ConcurrentLinkedQueue<String> conflictSamples = new java.util.concurrent.ConcurrentLinkedQueue<>();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    latch.await();
                    for (int j = 0; j < perThread; j++) {
                        // 先 Fusion 后 Dao，交替取号
                        long fusionId = FusionSequenceFactory.getSequenceId(seq);
                        String fusionPrev = idMap.put(fusionId, "Fusion");
                        if (fusionPrev != null) {
                            duplicates[0]++;
                            if (conflictSamples.size() < 30) {
                                conflictSamples.add("Fusion id=" + fusionId + " 先由 " + fusionPrev + " 发出");
                            }
                        }
                        long daoId = DaoSequenceFactory.getSequenceId(seq);
                        String daoPrev = idMap.put(daoId, "Dao");
                        if (daoPrev != null) {
                            duplicates[0]++;
                            if (conflictSamples.size() < 30) {
                                conflictSamples.add("Dao id=" + daoId + " 先由 " + daoPrev + " 发出");
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        latch.countDown();
        try {
            assertTrue(done.await(180, TimeUnit.SECONDS), "交替混合并发任务应按时完成");
        } finally {
            pool.shutdownNow();
        }
        if (duplicates[0] > 0) {
            System.err.println("!! 交替混合冲突样本(前30): " + conflictSamples);
        }
        assertEquals(total, idMap.size(),
                "应发出 " + total + " 个唯一ID，实际唯一数 " + idMap.size() + "，冲突 " + duplicates[0]
                        + " 个（交替混合调用下存在跨源重复发号 bug！）");
    }

    /**
     * 随机池取号基本：连续取号返回不同 ID，且都 > 0。
     */
    @Test
    @DisplayName("随机池取号：基本取号返回正数且互异")
    void randomPoolBasic() {
        String seq = uniqueSeq("poolbasic");
        long a = FusionSequenceFactory.getRandomSequenceIdFromPool(seq);
        long b = FusionSequenceFactory.getRandomSequenceIdFromPool(seq);
        long c = FusionSequenceFactory.getRandomSequenceIdFromPool(seq);
        assertTrue(a > 0, "首个ID应>0");
        assertTrue(a != b && b != c && a != c, "三次取号应互异: " + a + "," + b + "," + c);
    }

    /**
     * 随机池并发取号：多线程各取若干号，所有返回值必须唯一。
     * <p>取号量超过初始 POOL_SIZE 会触发补池（DB 申请 + SADD），验证补池路径下也不重复。</p>
     */
    @Test
    @DisplayName("随机池并发取号：跨补池全部唯一")
    void randomPoolConcurrentNoDuplicate() throws InterruptedException {
        // 用唯一 seqName 避免跨测试/跨运行的 Redis pool 残留污染
        String seq = uniqueSeq("poolconc");
        final int threads = 16;
        final int perThread = 1000; // 总量 16000 > POOL_SIZE(10000)，必触发多次补池
        final int total = threads * perThread;
        ConcurrentHashMap<Long, Long> seen = new ConcurrentHashMap<>();
        java.util.concurrent.ConcurrentLinkedQueue<Long> dupIds = new java.util.concurrent.ConcurrentLinkedQueue<>();
        long[] duplicates = {0};
        ExecutorService threadPool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            threadPool.submit(() -> {
                try {
                    latch.await();
                    for (int j = 0; j < perThread; j++) {
                        long id = FusionSequenceFactory.getRandomSequenceIdFromPool(seq);
                        if (seen.put(id, id) != null) {
                            duplicates[0]++;
                            if (dupIds.size() < 30) {
                                dupIds.add(id);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        latch.countDown();
        try {
            assertTrue(done.await(180, TimeUnit.SECONDS), "随机池并发任务应按时完成");
        } finally {
            threadPool.shutdownNow();
        }
        if (duplicates[0] > 0) {
            System.err.println("!! 重复ID样本(前30): " + dupIds);
        }
        assertEquals(total, seen.size(),
                "应取出 " + total + " 个唯一ID，实际唯一数 " + seen.size() + "，重复 " + duplicates[0]
                        + " 个（随机池取号存在重复 bug！）");
    }

    /**
     * 清理 DaoSequenceFactory 本地缓存（Fusion 内部依赖它）。
     */
    @SuppressWarnings("unchecked")
    private void clearDaoFactoryCache() {
        try {
            java.lang.reflect.Field f = DaoSequenceFactory.class.getDeclaredField("seqFactory");
            f.setAccessible(true);
            ((java.util.Map<String, DaoSequenceFactory>) f.get(null)).clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 按通配模式删除 Redis key。
     */
    private void deleteByPattern(String pattern) {
        var keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }
}
