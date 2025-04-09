package uw.common.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 1ms精度的计时器，在高并发下可以提高100倍性能。
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
     * 默认构造器。
     */
    static {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, runnable -> {
            Thread thread = new Thread(runnable, "system.clock");
            thread.setDaemon(true);
            return thread;
        });

        scheduler.scheduleAtFixedRate(() -> NOW.set(System.currentTimeMillis()), PRECISION, PRECISION, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取当前时间戳。
     *
     * @return 当前时间戳
     */
    public static long now() {
        return NOW.get();
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