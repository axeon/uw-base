package uw.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于 Redis 号段模式的全局序列发号器。
 *
 * <p>号段模式：批量从 Redis 原子取号（{@code getAndAdd(step)}），本地发号，段内零网络——
 * 与逐次 {@code INCR} 相比，Redis 交互次数降低为 1/step。多实例下各实例从 Redis 取到不同号段
 * （getAndAdd 原子），互不冲突。</p>
 *
 * <h3>取号失败语义</h3>
 * 取号失败时<b>不推进本地状态</b>（currentId/maxId 不变），保持对象状态一致，让
 * {@link #nextId(String)} 的重试在干净状态上进行。重试 {@link #MAX_RETRY_TIMES} 次仍失败则抛出异常。
 *
 * <h3>号段语义</h3>
 * 号段起始值不会作为 ID 发出，首个返回值为 {@code start+1}——currentId 初值即号段起点（哨兵），
 * 保持序列连续性语义与历史实现一致。序列 key 永久递增<b>不设 TTL</b>；
 * key 更名时需先做抬号迁移（旧 key 值复制到新 key），避免新序列从 0 重发与既有数据主键冲突。
 *
 * <h3>步长配置与缓存</h3>
 * 号段步长默认 {@link #DEFAULT_SEGMENT_STEP}（10000：每 10000 号一次 Redis 取段），
 * 经 {@link #config(String, int)} 按 name 注册覆盖（步长 1 走逐号 INCR 快捷路径——零跳号、无本地锁）。
 * 序列器按 {@code name} 缓存于 static 单表（实例持有本地号段状态，故须缓存），永不淘汰——
 * 调用方应使用有限的 name 集合（并编码自身域前缀，如 "uw-task-xxx"），动态 name 会造成内存增长与号段浪费。
 *
 * @author axeon
 */
public class GlobalSequence {

    private static final Logger log = LoggerFactory.getLogger(GlobalSequence.class);

    /**
     * redis key 前缀。
     */
    private static final String REDIS_PREFIX = "uw-seq:";

    /**
     * 默认号段步长。
     */
    private static final int DEFAULT_SEGMENT_STEP = 10_000;

    /**
     * 重试次数。
     */
    private static final int MAX_RETRY_TIMES = 3;

    /**
     * redis 连接工厂，由 Spring 注入 static 字段。
     */
    private static RedisConnectionFactory redisConnectionFactory;

    /**
     * 按 name 缓存的序列器实例（实例持有本地号段状态，故须缓存；computeIfAbsent 原子创建保证单实例）。
     * name 应为有限集合。
     */
    private static final ConcurrentMap<String, RedisSequence> map = new ConcurrentHashMap<>();

    /**
     * 构造方法，由 Spring 注入 Redis 连接工厂到 static 字段。
     *
     * @param redisConnectionFactory Redis 连接工厂
     */
    public GlobalSequence(RedisConnectionFactory redisConnectionFactory) {
        GlobalSequence.redisConnectionFactory = redisConnectionFactory;
    }

    /**
     * 注册/覆盖 name 的号段步长（默认 {@link #DEFAULT_SEGMENT_STEP}=10000），
     * 一般放在静态块或自动配置中调用一次。
     * <p>直接按新步长重建实例（旧实例未用号段作废，号段内损耗可控；正在使用旧实例的线程用完当前段即止，
     * 号码唯一性不受影响——号在取段时已全局预扣）。构造含一次 Redis 取段，Redis 不可用会立即抛出
     * （配置错误早暴露）。覆盖式：再次调用即再次重建。</p>
     *
     * @param name        序列名称
     * @param segmentStep 号段步长（&lt;=0 忽略）；1 走逐号 INCR 快捷路径（零跳号无本地锁）
     */
    public static void config(String name, int segmentStep) {
        if (segmentStep <= 0) {
            return;
        }
        if (redisConnectionFactory == null) {
            //早暴露:静默忽略会让调用方以为已注册(与"配置错误早暴露"语义矛盾)
            throw new IllegalStateException("GlobalSequence尚未初始化(连接工厂未注入),config须在Spring容器就绪后调用: " + name);
        }
        map.put(name, new RedisSequence(REDIS_PREFIX + name, segmentStep));
    }

    /**
     * 获取序列值（未 config 的 name 用默认步长惰性创建）。
     *
     * @param name 序列名称（应为有限集合，建议含调用方域前缀）
     * @return 下一个序列值
     */
    public static long nextId(String name) {
        Exception lastException = null;
        for (int tryCount = 0; tryCount < MAX_RETRY_TIMES; tryCount++) {
            try {
                RedisSequence redisSequence = map.computeIfAbsent(name,
                        key -> new RedisSequence(REDIS_PREFIX + key, DEFAULT_SEGMENT_STEP));
                return redisSequence.nextId();
            } catch (Exception e) {
                lastException = e;
                log.error(e.getMessage(), e);
            }
        }
        throw new RuntimeException("GlobalSequence获取序列失败: " + name, lastException);
    }

    /**
     * redis 序列发生器（号段模式：批量从redis取号，本地发号，减少redis交互）。
     * <p>
     * 线程安全：步长 1 走 INCR 原子快捷路径（无本地锁）；步长 &gt;1 的号段模式由
     * {@link #nextIdBySegment()} 的 synchronized 保证本地发号原子性；
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
         * redis计数器。
         */
        private final RedisAtomicLong counter;

        /**
         * 初始化一个序列器：立即从redis取首个号段（连接工厂用外层 static 注入，不传参）。
         *
         * @param fullKey      完整 redis key（前缀 + 序列名）
         * @param incrementNum 增长数（号段步长）
         */
        RedisSequence(String fullKey, int incrementNum) {
            this.incrementNum = incrementNum;
            this.counter = new RedisAtomicLong(fullKey, redisConnectionFactory);
            // 取首个号段：[start, start+incrementNum)
            this.currentId = counter.getAndAdd(incrementNum);
            this.maxId = this.currentId + incrementNum;
        }

        /**
         * 快捷路径（号段步长=1）：直接逐号 {@code INCR} 原子取号。
         * <p>步长 1 时本地号段缓存完全无用（每次必取段），且号段哨兵语义（段起点不发出）会
         * <b>跳过 50% 的号</b>——本路径既免本地锁（INCR 自身原子，多线程并发各得唯一号，
         * 无需 synchronized 串行化）又零跳号。</p>
         *
         * @return 下一个序列值
         */
        long nextId() {
            if (incrementNum == 1) {
                return counter.incrementAndGet();
            }
            return nextIdBySegment();
        }

        /**
         * 号段模式取号（步长&gt;1）。
         * <p>
         * currentId 语义为"已发放的最大号"，nextId 返回 currentId+1；
         * 号段用尽（currentId+1 >= maxId）时从redis原子取新号段。
         * <p>
         * 取号失败时<b>不推进 currentId</b>（区别于先++后判断的脏状态问题），
         * 保持对象状态一致，让上层 {@link GlobalSequence#nextId(String)} 的重试在干净状态上进行。
         * <p>
         * 注：号段起始值（如首个号段的 start）不会作为 ID 发出，首个返回值为 start+1——
         * 这是有意设计（currentId 初值即号段起点，作为"哨兵"），保持与历史行为一致，避免破坏既有ID序列。
         */
        private synchronized long nextIdBySegment() {
            if (currentId + 1 >= maxId) {
                // 号段耗尽,原子取新号段;失败时异常上抛且不推进本地状态
                currentId = counter.getAndAdd(incrementNum);
                maxId = currentId + incrementNum;
            }
            return ++currentId;
        }
    }

}
