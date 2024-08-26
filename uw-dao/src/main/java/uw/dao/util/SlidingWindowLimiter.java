package uw.dao.util;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 一个简单滑动窗口计数器。
 */
public class SlidingWindowLimiter {

    /**
     * 错误限制计数。
     */
    private final int limit;

    /**
     * 时间窗口大小。
     */
    private final long windowMillis;

    /**
     * 时间队列。
     */
    private final ConcurrentLinkedQueue<Long> timesQueue;

    public SlidingWindowLimiter(int limit, long windowMillis) {
        this.limit = limit;
        this.windowMillis = windowMillis;
        this.timesQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     * 向滑动窗口添加一条记录。
     */
    public void add() {
        if (timesQueue.size() < limit) {
            timesQueue.offer( System.currentTimeMillis() );
        }
    }

    /**
     * 检查是否可以获得许可。
     *
     * @return
     */
    public boolean tryAcquire() {
        //不管3721，先判定条件。
        if (timesQueue.size() < limit) {
            return true;
        }
        long boundary = System.currentTimeMillis() - windowMillis;
        while (!timesQueue.isEmpty() && timesQueue.peek() < boundary) {
            timesQueue.poll();
        }
        return timesQueue.size() < limit;
    }
}
