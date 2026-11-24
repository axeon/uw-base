package uw.task.rabbit;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 虚拟线程行为实测：carrier 数量、阻塞可见性（jcmd dump 含栈）、synchronized pinning。
 *
 * @author axeon
 */
class VirtualThreadBehaviorTest {

    /**
     * carrier（ForkJoinPool）默认并行度 = CPU 核数；carrier 线程名 ForkJoinPool-1-worker-N。
     * VT 阻塞（sleep/IO）不占 carrier（unmount）；synchronized 块内阻塞会 pin 住 carrier（JDK24 前部分解决）。
     */
    @Test
    void carrierPoolSizeAndBlockingBehavior() throws Exception {
        int carriers = Runtime.getRuntime().availableProcessors();
        System.out.printf("VT-CARRIER: CPU=%d, 默认carrier并行度=CPU核数（可用 -Djdk.virtualThreadScheduler.parallelism 调整）%n", carriers);
        // 1000 个 VT 同时 sleep：carrier 不被占用（unmount），验证承载不会被阻塞型任务耗尽
        int n = 1000;
        CountDownLatch allDone = new CountDownLatch(n);
        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignore) {
                    Thread.currentThread().interrupt();
                }
                allDone.countDown();
            });
        }
        boolean done = allDone.await(5, TimeUnit.SECONDS);
        System.out.printf("VT-BLOCK: %d个VT并发sleep(500ms)完成=%b 耗时=%dms (若占carrier则需%dms+)%n",
                n, done, System.currentTimeMillis() - start, n * 500 / carriers);
        assertThat(done).isTrue();
        assertThat(System.currentTimeMillis() - start).isLessThan(3000L);
    }
}
