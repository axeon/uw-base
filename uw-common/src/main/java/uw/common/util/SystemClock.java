package uw.common.util;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 高性能动态系统时钟服务。
 * 通过监控调度每隔10s对调用频率进行监控。
 * 低于10/ms的调用频率，直接调用系统时钟。
 * 高于10/ms的调用频率，则通过定时器进行调用。
 *
 * 测试环境：mbp m2 max, jdk21，100线程。
 * 测试结果：
 * System.currentTimeMillis() 5.9w/ms
 * System.nanoTime()          1.8w/ms
 * SystemClock.now()低速       5.0w/ms
 * SystemClock.now()高速       229w/ms
 *
 * @author axeon
 */
public class SystemClock {

    /**
     * 精度数据。
     */
    private static final long PRECISION = 1L;

    /**
     * 当前时间戳。
     */
    private static final AtomicLong NOW = new AtomicLong(System.currentTimeMillis());

    /**
     * 定义调用频率阈值。
     */
    private static final int CALL_THRESHOLD = 100_000; // 每毫秒10次

    /**
     * 监控间隔。
     */
    private static final long MONITOR_INTERVAL = 10_000L; // 10秒检查一次调用频率

    /**
     * 监控任务的调度器。
     */
    private static final ScheduledExecutorService monitorScheduler = Executors.newScheduledThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable, "clock.monitor");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * 调用计数器。
     * 在这里并不需要精确计数，只是需要一个模糊计数，所以并未做多线程处理。
     * 设置为AtomicInteger和volatile，都会大幅度降低性能。
     */
    private static int CALL_COUNT = 0;

    /**
     * 定时更新时间戳的调度器。
     */
    private static ScheduledExecutorService updateScheduler;

    /**
     * 更新时间戳任务。
     */
    private static ScheduledFuture<?> updateTask;


    static {
        // 启动监控线程。
        monitorScheduler.scheduleAtFixedRate(() -> {
            int count = CALL_COUNT;
            CALL_COUNT = 0;
            if (count >= CALL_THRESHOLD) {
                if (updateTask == null || updateTask.isCancelled()) {
                    updateScheduler = Executors.newScheduledThreadPool(1, runnable -> {
                        Thread thread = new Thread(runnable, "clock.system");
                        thread.setDaemon(true);
                        return thread;
                    });
                    updateTask = updateScheduler.scheduleAtFixedRate(() -> NOW.set(System.currentTimeMillis()), 0, PRECISION, TimeUnit.MILLISECONDS);
                }
            } else {
                if (updateTask != null && !updateTask.isCancelled()) {
                    updateTask.cancel(true);
                    updateScheduler.shutdown();
                    updateScheduler = null;
                    updateTask = null;
                }
            }
        }, MONITOR_INTERVAL, MONITOR_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取当前时间戳。
     *
     * @return 当前时间戳
     */
    public static long now() {
        CALL_COUNT++;
        if (updateTask == null) {
            return System.currentTimeMillis();
        } else {
            return NOW.get();
        }
    }

    /**
     * 获取当前Date。
     *
     * @return 当前时间戳
     */
    public static Date nowDate() {
        return new Date(now());
    }

    /**
     * 计算从startTime到当前时间的时间差。
     *
     * @param startTime 开始时间戳
     * @return 时间差
     */
    public static long elapsedMillis(final long startTime) {
        return now() - startTime;
    }

    /**
     * 计算两个时间戳的时间差。
     *
     * @param startTime 开始时间戳
     * @param endTime   结束时间戳
     * @return 时间差
     */
    public static long elapsedMillis(final long startTime, final long endTime) {
        return endTime - startTime;
    }

}