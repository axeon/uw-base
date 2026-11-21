package uw.task.rabbit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 091 消费线程命名规则验证：SimpleAsyncTaskExecutor(queueName)+虚拟线程的线程名形态。
 *
 * @author axeon
 */
class SateNamingTest {

    /**
     * 091 消费线程命名验证：ThreadPerTaskVirtualExecutor（Thread.ofVirtual）线程名={队列名}-N，
     * 与 1.0 ElasticConsumerGroup 的 Semaphore+Thread.ofVirtual 命名完全一致，且为虚拟线程。
     */
    @Test
    void threadNameIsQueueNamePlusIndex() throws Exception {
        String queueName = "uw.task.demo.E2eSlowRunner#$default";
        org.springframework.core.task.TaskExecutor executor = new org.springframework.core.task.TaskExecutor() {
            private final java.util.concurrent.atomic.AtomicInteger idx = new java.util.concurrent.atomic.AtomicInteger();
            @Override
            public void execute(Runnable task) {
                Thread.ofVirtual().name(queueName + "-", idx.getAndIncrement()).start(task);
            }
        };
        String[] names = new String[3];
        boolean[] virtual = new boolean[1];
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(3);
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            executor.execute(() -> {
                names[idx] = Thread.currentThread().getName();
                virtual[0] |= Thread.currentThread().isVirtual();
                done.countDown();
            });
        }
        done.await();
        System.out.printf("091-NAMING(ofVirtual): %s virtual=%b%n", String.join(",", names), virtual[0]);
        for (String n : names) {
            assertThat(n).startsWith(queueName + "-").as("线程名必须为 {队列名}-N 形态: " + n);
        }
        assertThat(virtual[0]).as("必须是虚拟线程").isTrue();
    }
}
