package uw.dao.sequence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uw.dao.DaoFactory;
import uw.dao.SeqTestApplication;
import uw.dao.conf.DaoConfigManager;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DaoSequenceFactory} 当前行为的固化测试。
 *
 * <p>目的：在重构前锁定现有行为（包括已知的怪异/可能有问题之处），以便重构后精确对比。
 * 使用独立的 seq_name 前缀 {@link #SEQ_PREFIX}，每个用例前后清理 DB 与本地缓存。</p>
 *
 * @author axeon
 */
@SpringBootTest(classes = SeqTestApplication.class)
@ActiveProfiles("seqtest")
@ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
class DaoSequenceFactoryTest {

    private static final String SEQ_PREFIX = "TEST_DAO_";

    private final DaoFactory daoFactory = DaoFactory.getInstance();

    @BeforeEach
    @AfterEach
    void cleanup() {
        // 清理 DB 中测试用的 seq 记录（连接名在 Spring 上下文就绪后获取）
        try {
            String rootConn = DaoConfigManager.getRouteMapping("sys_seq", "write");
            daoFactory.execute(rootConn, "delete from sys_seq where seq_name like ?", new Object[]{SEQ_PREFIX + "%"});
        } catch (Exception e) {
            // 忽略清理异常
        }
        // 清理本地 manager 缓存，避免上个用例的 currentId/maxId 残留
        clearFactoryCache();
    }

    /**
     * 基本发号：连续两次调用，结果递增。
     */
    @Test
    @DisplayName("基本发号：连续调用单调递增")
    void basicIncrement() {
        String seq = SEQ_PREFIX + "basic";
        long a = DaoSequenceFactory.getSequenceId(seq);
        long b = DaoSequenceFactory.getSequenceId(seq);
        long c = DaoSequenceFactory.getSequenceId(seq);
        assertTrue(b == a + 1, "第二次=" + b + " 应为 " + (a + 1));
        assertTrue(c == b + 1, "第三次=" + c + " 应为 " + (b + 1));
    }

    /**
     * allocateSequenceRange：返回的起始号，后续普通发号应紧随其后（段内连续）。
     */
    @Test
    @DisplayName("allocateSequenceRange 返回连续段起点")
    void allocateRange() {
        String seq = SEQ_PREFIX + "alloc";
        long start = DaoSequenceFactory.allocateSequenceRange(seq, 100);
        assertTrue(start > 0, "起始号应大于0");
        long next = DaoSequenceFactory.getSequenceId(seq);
        assertTrue(next >= start, "后续发号 next=" + next + " 应 >= 段起点 " + start);
    }

    /**
     * resetSequenceId：重置后下次发号应 >= 重置值。
     */
    @Test
    @DisplayName("resetSequenceId 后发号 >= 重置值")
    void resetSequence() {
        String seq = SEQ_PREFIX + "reset";
        // 先发几个号
        DaoSequenceFactory.getSequenceId(seq);
        DaoSequenceFactory.getSequenceId(seq);
        // 重置到一个大值
        boolean ok = DaoSequenceFactory.resetSequenceId(seq, 1_000_000L, 10);
        assertTrue(ok, "reset 应成功");
        long next = DaoSequenceFactory.getSequenceId(seq);
        assertTrue(next >= 1_000_000L, "重置后发号=" + next + " 应 >= 1000000");
    }

    /**
     * 并发不重复：多线程各发若干号，所有返回值应唯一。
     * 这是发号器最核心的硬约束。
     * <p>注意：DaoSequenceFactory 在 incrementNum 较小时并发吞吐有限（每号段都要打 DB + 乐观锁竞争），
     * 故此处用适中的并发量与较大的段大小，避免 DB 拥塞导致的重试放大。</p>
     */
    @Test
    @DisplayName("并发发号：所有返回值唯一（无重复）")
    void concurrentNoDuplicate() throws InterruptedException {
        String seq = SEQ_PREFIX + "conc";
        // 先发一个号让 sys_seq 建行，再设较大的 incrementNum 让段缓存生效
        DaoSequenceFactory.getSequenceId(seq);
        assertTrue(DaoSequenceFactory.resetSequenceId(seq, 1L, 100), "预设 incrementNum=100 失败");
        clearFactoryCache(); // 清缓存让新的 incrementNum 生效
        final int threads = 16;
        final int perThread = 100;
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
                        long id = DaoSequenceFactory.getSequenceId(seq);
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
            assertTrue(done.await(90, TimeUnit.SECONDS), "并发任务应按时完成");
        } finally {
            pool.shutdownNow();
        }
        assertEquals(total, seen.size(), "应发出 " + total + " 个唯一ID，实际 " + seen.size() + "（有重复！）");
    }

    /**
     * 单调性校验（线程内）：单线程内发号严格递增。
     */
    @Test
    @DisplayName("单线程内发号严格递增")
    void monotonicInSingleThread() {
        String seq = SEQ_PREFIX + "mono";
        long prev = DaoSequenceFactory.getSequenceId(seq);
        for (int i = 0; i < 1000; i++) {
            long cur = DaoSequenceFactory.getSequenceId(seq);
            assertTrue(cur > prev, "应严格递增: prev=" + prev + " cur=" + cur);
            prev = cur;
        }
    }

    /**
     * 清理本地 seqFactory map（通过反射），让每个用例拿到全新的 manager。
     */
    @SuppressWarnings("unchecked")
    private void clearFactoryCache() {
        try {
            Field f = DaoSequenceFactory.class.getDeclaredField("seqFactory");
            f.setAccessible(true);
            ((Map<String, DaoSequenceFactory>) f.get(null)).clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
