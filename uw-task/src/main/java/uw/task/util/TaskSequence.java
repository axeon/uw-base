package uw.task.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * uw-task 私有序列发号器（号段模式，key 空间私有 uw-task-seq:）。
 *
 * <p>号段模式：批量从 Redis 原子取号（getAndAdd(step)），本地发号，段内零网络；
 * 取号失败不推进本地状态，重试 {@value #MAX_RETRY_TIMES} 次仍失败抛异常。
 * 号段起始值不发出（哨兵语义，首个返回 start+1）。序列 key 永久递增不设 TTL；
 * key 更名需先抬号迁移。name 应为有限集合（task 侧仅 runner/croner 两个日志序列）。</p>
 *
 * @author axeon
 */
public final class TaskSequence {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(TaskSequence.class);

    /**
     * redis key 前缀（uw-task 私有空间）。
     */
    private static final String REDIS_PREFIX = "uw-task-seq:";

    /**
     * 号段步长（task 日志 id 量级：万级段足够摊薄 RTT，且实例异常时号段损耗可控）。
     */
    private static final int SEGMENT_STEP = 10_000;

    /**
     * 重试次数。
     */
    private static final int MAX_RETRY_TIMES = 3;

    /**
     * 按 name 缓存的序列器（实例持本地号段状态，须缓存；computeIfAbsent 原子创建）。
     */
    private static final ConcurrentMap<String, RedisSequence> map = new ConcurrentHashMap<>();

    /**
     * 私有连接工厂（init 注入，须在 nextId 前调用）。
     */
    private static RedisConnectionFactory redisConnectionFactory;

    /**
     * 工具类，禁止实例化。
     */
    private TaskSequence() {
    }

    /**
     * 注入私有连接工厂（自动配置调用一次，须在 nextId 前调用）。
     *
     * @param connectionFactory uw-task 私有 Redis 连接工厂
     */
    public static void init(RedisConnectionFactory connectionFactory) {
        TaskSequence.redisConnectionFactory = connectionFactory;
    }

    /**
     * 获取序列值（惰性创建，默认步长 {@value #SEGMENT_STEP}）。
     *
     * @param name 序列名称（有限集合，如 {@link uw.task.constant.TaskConstants#KEY_RUNNER_LOG}）
     * @return 下一个序列值
     */
    public static long nextId(String name) {
        Exception lastException = null;
        for (int tryCount = 0; tryCount < MAX_RETRY_TIMES; tryCount++) {
            try {
                RedisSequence sequence = map.computeIfAbsent(name,
                        key -> new RedisSequence(REDIS_PREFIX + key, SEGMENT_STEP));
                return sequence.nextId();
            } catch (Exception e) {
                lastException = e;
                log.error(e.getMessage(), e);
            }
        }
        throw new RuntimeException("TaskSequence failed to get id: " + name, lastException);
    }

    /**
     * redis 序列发生器（号段模式）。线程安全：nextId 的 synchronized 保证本地发号原子性；
     * 多实例从 redis 取不同号段（getAndAdd 原子）互不冲突。
     */
    static final class RedisSequence {

        /**
         * 当前待发放的数值（尚未发出；号段哨兵语义）。
         */
        private long currentId;

        /**
         * 当前号段可发放上界（exclusive）：currentId &lt; maxId 时无需访问 redis。
         */
        private long maxId;

        /**
         * 每次从 redis 取号的步长。
         */
        private final int incrementNum;

        /**
         * redis 计数器（getAndAdd 原子取段）。
         */
        private final RedisAtomicLong counter;

        /**
         * 构建序列发生器并取首个号段。
         *
         * @param fullKey      redis key（含前缀）
         * @param incrementNum 号段步长
         */
        RedisSequence(String fullKey, int incrementNum) {
            this.incrementNum = incrementNum;
            this.counter = new RedisAtomicLong(fullKey, redisConnectionFactory);
            this.currentId = counter.getAndAdd(incrementNum);
            this.maxId = this.currentId + incrementNum;
        }

        /**
         * 取下一个序列值；当前号段用尽时从 redis 取新号段。
         *
         * @return 下一个序列值
         */
        synchronized long nextId() {
            if (currentId + 1 >= maxId) {
                currentId = counter.getAndAdd(incrementNum);
                maxId = currentId + incrementNum;
            }
            return ++currentId;
        }
    }
}
