package uw.task.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于redis实现的分布式序列。
 *
 * @author axeon
 */
public class TaskSequenceManager {

    private static final Logger log = LoggerFactory.getLogger(TaskSequenceManager.class);

    private static final String REDIS_TAG = "uw-task-seq:";

    /**
     * 重试次数。
     */
    private static final int MAX_RETRY_TIMES = 3;

    /**
     * 序列递增步长值
     */
    private static final int SEQUENCE_INCREMENT_NUM = 10000;

    /**
     * redis定制连接工厂
     */
    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * sequenceMap
     */
    private final ConcurrentHashMap<String, RedisSequence> map = new ConcurrentHashMap<>();

    public TaskSequenceManager(final RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    /**
     * 获取序列值
     *
     * @param name 序列名称
     * @return 下一个序列值
     */
    public long nextId(String name) {
        Exception lastException = null;
        for (int tryCount = 0; tryCount < MAX_RETRY_TIMES; tryCount++) {
            try {
                RedisSequence redisSequence = map.computeIfAbsent(name, key -> new RedisSequence(key, SEQUENCE_INCREMENT_NUM, redisConnectionFactory));
                return redisSequence.nextId();
            } catch (Exception e) {
                lastException = e;
                log.error(e.getMessage(), e);
            }
        }
        throw new RuntimeException("TaskSequenceManager获取序列失败: " + name, lastException);
    }

    /**
     * redis序列发生器（号段模式：批量从redis取号，本地发号，减少redis交互）。
     * <p>
     * 线程安全：nextId 用 synchronized 保证本地发号原子性；
     * 多实例下各实例从 redis 取不同号段（getAndAdd 原子），互不冲突。
     *
     * @author axeon
     */
    static class RedisSequence {

        /**
         * 当前待发放的数值（尚未发出）。
         */
        private long currentId;

        /**
         * 当前号段可发放的上界（exclusive）：currentId < maxId 时无需访问redis。
         */
        private long maxId;

        /**
         * 每次从redis取号的步长。
         */
        private final int incrementNum;

        /**
         * redis计数器
         */
        private final RedisAtomicLong counter;

        /**
         * 初始化一个序列器：立即从redis取首个号段。
         *
         * @param name               序列名称
         * @param incrementNum       增长数（号段步长）
         * @param connectionFactory  redis连接工厂
         */
        private RedisSequence(String name, int incrementNum, RedisConnectionFactory connectionFactory) {
            this.incrementNum = incrementNum;
            this.counter = new RedisAtomicLong(REDIS_TAG + name, connectionFactory);
            // 取首个号段：[start, start+incrementNum)
            this.currentId = counter.getAndAdd(incrementNum);
            this.maxId = this.currentId + incrementNum;
        }

        /**
         * 获取下一个ID。
         * <p>
         * currentId 语义为"已发放的最大号"，nextId 返回 currentId+1；
         * 号段用尽（currentId+1 >= maxId）时从redis原子取新号段。
         * <p>
         * 取号失败时<b>不推进 currentId</b>（区别于旧版先++后判断的脏状态问题），
         * 保持对象状态一致，让上层 {@link TaskSequenceManager#nextId(String)} 的重试在干净状态上进行。
         * <p>
         * 注：号段起始值（如首个号段的 start）不会作为 ID 发出，首个返回值为 start+1——
         * 这是有意设计（currentId 初值即号段起点，作为"哨兵"），保持与历史行为一致，避免破坏既有ID序列。
         *
         * @return 下一个序列值
         */
        synchronized long nextId() {
            long next = currentId + 1;
            if (next >= maxId) {
                // 当前号段用尽，从redis取新号段；失败则抛异常但不改 currentId
                long newStart = counter.getAndAdd(incrementNum);
                currentId = newStart;
                maxId = newStart + incrementNum;
                return currentId;
            }
            currentId = next;
            return currentId;
        }
    }
}
