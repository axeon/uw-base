package uw.dao.sequence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 一个基于Redis优化的Sequence工厂类。
 * 此类可以和DaoSequenceFactory同步。
 */
public class FusionSequenceFactory {

    private static final Logger log = LoggerFactory.getLogger(FusionSequenceFactory.class);

    /**
     * redis key。
     */
    private static final String REDIS_SEQ_POOL = "uw-dao-seq-pool:";

    /**
     * 发号段缓存前缀（cursor 与 end 两个 key）。
     */
    private static final String REDIS_SEQ = "uw-dao-seq:";

    /**
     * 段缓存游标 key 后缀：段内已发到哪（INCR 自增）。
     */
    private static final String CURSOR_SUFFIX = ":cursor";

    /**
     * 段缓存终点 key 后缀：当前段允许发到的最大值。
     */
    private static final String END_SUFFIX = ":end";

    /**
     * 重试次数。差不多超时50s左右。
     */
    private static final int MAX_RETRY_TIMES = 50;

    /**
     * 池子大小，每次从数据库中缓存1000个。
     */
    private static final int POOL_SIZE = 10000;

    /**
     * 池中最小容量。
     */
    private static final int POOL_MIN = 1000;

    /**
     * 安全解锁 Lua 脚本：仅当锁值等于本线程的 owner 时才删除，避免误删他人持有的锁。
     * 对应命令：{@code if get(KEYS[1])==ARGV[1] then del(KEYS[1]) end}
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end",
            Long.class);

    /**
     * 原子取号 Lua 脚本：检查池大小，充足则 pop 返回一个 ID；不足则返回补池标记。
     * <p>KEYS[1]=池key，ARGV[1]=POOL_MIN。返回 {@code {poppedId, needReserve}}：</p>
     * <ul>
     *   <li>{@code needReserve=0, poppedId>0}：池充足，已 pop，直接使用 poppedId。</li>
     *   <li>{@code needReserve=1, poppedId=0}：池不足，调用方需申请 DB 段补池后重试。</li>
     * </ul>
     * <p>将"检查池大小 + pop"封装为原子操作，消除多线程下"判大小后池被抽空"的竞态与空 pop。</p>
     */
    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> POP_OR_RESERVE_SCRIPT = new DefaultRedisScript<>(
            "local size=redis.call('scard',KEYS[1]) "
                    + "if size>tonumber(ARGV[1]) then "
                    + "  local id=redis.call('spop',KEYS[1]) "
                    + "  return {id,0} "
                    + "else "
                    + "  return {0,1} "
                    + "end",
            List.class);

    /**
     * 原子补池+取号 Lua 脚本：判池容量 + 追加新段 + pop 一气呵成。
     * <p>KEYS[1]=池key，ARGV[1]=base(段起点)，ARGV[2]=POOL_SIZE，ARGV[3]=POOL_MIN。</p>
     * <p>逻辑：若池容量 ≤ POOL_MIN，则 SADD 追加新段 [base, base+POOL_SIZE-1]（不 DEL，
     * 保留池内由 {@code restoreSequenceIdToPool} 回收的可重用 ID）；再 SPOP 一个返回。
     * 全程在 Redis 单线程内原子执行，消除判空-补池-pop 之间的并发竞态与重复发号。
     * 由于新段 [base, base+POOL_SIZE-1] 来自 DB 唯一真值源（与 Dao/Fusion 互不重叠），
     * 追加进 SET 后与已有元素天然去重，SPOP 返回值全局唯一。</p>
     *
     * <p>返回 poppedId：取出的 ID（Redis SPOP 在池空时返回 false，调用方据此重试）。</p>
     */
    private static final DefaultRedisScript<Long> REFILL_AND_POP_SCRIPT = new DefaultRedisScript<>(
            "local base=tonumber(ARGV[1]) "
                    + "local size=tonumber(ARGV[2]) "
                    + "local poolMin=tonumber(ARGV[3]) "
                    + "if redis.call('scard',KEYS[1])<=poolMin then "
                    + "  for i=0,size-1 do redis.call('sadd',KEYS[1],base+i) end "
                    + "end "
                    + "local id=redis.call('spop',KEYS[1]) "
                    + "if id==false or id==nil then return 0 else return id end",
            Long.class);

    /**
     * 整段切换脚本（零丢号：段用完才换段，整段切换到新段）。
     * <p>KEYS[1]=cursor key，KEYS[2]=end key，ARGV[1]=newBase（新段起点），ARGV[2]=POOL_SIZE。</p>
     * <p>逻辑：原子设置 cursor = newBase-1（使首次取号从 newBase 开始），end = newBase+POOL_SIZE-1。</p>
     * <p><b>零丢号前提</b>：续段只在旧段已取完（cursor==end）时触发，因此旧段 [oldBase, oldEnd]
     * 的号已全部发出，整段切换不会丢任何号。混合调用下 Dao/Fusion 各自申请的 DB 段互不重叠
     * （DB 行级 UPDATE 串行），Fusion 新段 base 不一定紧接旧 end，但段内号连续且不丢。</p>
     * <p>Lua 内先 set end 后 set cursor，消除"end 未更新而 cursor 已重置"的中间态，
     * 避免其他线程误判为新段已耗尽而重复续段。</p>
     */
    private static final DefaultRedisScript<Long> SWITCH_SEGMENT_SCRIPT = new DefaultRedisScript<>(
            "local newBase=tonumber(ARGV[1]) "
                    + "local poolSize=tonumber(ARGV[2]) "
                    + "redis.call('set',KEYS[2],newBase+poolSize-1) "
                    + "redis.call('set',KEYS[1],newBase-1) "
                    + "return 1",
            Long.class);

    /**
     * 段缓存原子取号 Lua 脚本（零丢号：先判后增，越界不消费 cursor）。
     * <p>KEYS[1]=cursor key（段内游标，下一个待发号），KEYS[2]=end key（当前段终点）。</p>
     * <p>逻辑：先读 cursor 与 end，仅当 cursor &lt;= end（段内还有号）时才 INCR 并返回该号；
     * 否则<b>不推进 cursor</b>，返回 needRenew=1 让调用方续段。返回 {@code {seqId, needRenew}}：</p>
     * <ul>
     *   <li>{@code needRenew=0, seqId>0}：取号成功，seqId 即为发出的号。</li>
     *   <li>{@code needRenew=1, seqId=0}：段耗尽或未初始化，cursor 未变，调用方续段后重试。</li>
     * </ul>
     * <p>关键：旧实现"先 INCR 再判越界"，多个线程同时越过 end 时那些号被 INCR 消费却无人领取，
     * 造成永久丢号。本脚本"先判后增"保证 cursor 只在成功取号时 +1，绝不消费越界号。</p>
     */
    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> NEXT_SEQ_SCRIPT = new DefaultRedisScript<>(
            "local curStr=redis.call('get',KEYS[1]) "
                    + "local cur=0 "
                    + "if curStr~=false then cur=tonumber(curStr) end "
                    + "local segEnd=redis.call('get',KEYS[2]) "
                    + "if segEnd==false or cur>=tonumber(segEnd) then "
                    + "  return {0,1} "
                    + "else "
                    + "  local nxt=cur+1 "
                    + "  redis.call('set',KEYS[1],nxt) "
                    + "  return {nxt,0} "
                    + "end",
            List.class);

    /**
     * Redis 模板，用于执行 Lua 脚本（安全解锁等）。
     */
    private static RedisTemplate<String, Long> REDIS_TEMPLATE;

    /**
     * redis KV 操作。
     */
    private static ValueOperations<String, Long> KV_OP;

    /**
     * redis Set 操作。
     */
    private static SetOperations<String, Long> SET_OP;

    public FusionSequenceFactory(RedisTemplate<String, Long> daoRedisTemplate) {
        REDIS_TEMPLATE = daoRedisTemplate;
        KV_OP = daoRedisTemplate.opsForValue();
        SET_OP = daoRedisTemplate.opsForSet();
    }

    /**
     * 判断 Fusion 发号器是否可用（Redis 操作句柄已通过构造器注入就绪）。
     * <p>三个句柄在构造器中一次性同时赋值，要么全 null 要么全就绪，不会出现部分就绪的中间态。
     * 本方法是发号器就绪状态的唯一真相源：</p>
     * <ul>
     *   <li>供 {@link uw.dao.SequenceFactory} 在配置了 Redis 但初始化失败时降级到
     *       {@link DaoSequenceFactory}，避免全局发号因 Redis 不可用而瘫痪；</li>
     *   <li>供 {@link #ensureInitialized()} 复用，保证公共方法入口校验与降级判定条件始终一致。</li>
     * </ul>
     *
     * @return true 表示 Redis 句柄已就绪
     */
    public static boolean isAvailable() {
        return REDIS_TEMPLATE != null && KV_OP != null && SET_OP != null;
    }

    /**
     * 确保已通过构造器注入 Redis 操作句柄，防止 Spring 上下文未就绪时静态方法被调用导致 NPE。
     * <p>判定逻辑复用 {@link #isAvailable()}，保持"就绪状态"单一真相源，
     * 避免两处判定条件手工同步导致的不一致。</p>
     */
    private static void ensureInitialized() {
        if (!isAvailable()) {
            throw new IllegalStateException("FusionSequenceFactory 尚未初始化，请先通过构造器注入 RedisTemplate");
        }
    }

    /**
     * 退避休眠指定毫秒，中断时恢复中断标志（不抛出，让上层循环继续重试）。
     * 抽出公共方法，统一处理 InterruptedException，避免散落各处的 try/catch 样板。
     *
     * @param millis 休眠毫秒数（&lt;=0 则不休眠）
     */
    private static void sleepBackoff(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 指数退避休眠：按重试次数从 floorMs 翻倍增长，封顶 capMs。
     * 用于"重试条件依赖外部状态恢复"的场景（DB/Redis 恢复、锁释放），
     * 相比固定 sleep 能更快感知条件满足，又不致在持续故障时打爆下游。
     *
     * @param attempt 已重试次数（从 0 起）
     * @param floorMs 起始休眠毫秒
     * @param capMs   休眠上限毫秒
     */
    private static void sleepBackoff(int attempt, long floorMs, long capMs) {
        long shift = Math.min(attempt, 10);
        sleepBackoff(Math.min(capMs, floorMs << shift));
    }

    /**
     * 通过实体类获取当前主键ID（最近一次已发的号；未初始化返回 0）。
     *
     * @param entityCls 实体类（序列名取类简单名）
     * @return 当前已发号；未初始化返回 0
     */
    public static long getCurrentId(Class<?> entityCls) {
        return getCurrentId(entityCls.getSimpleName());
    }

    /**
     * 通过实体类获取主键ID。
     *
     * @param entityCls 实体类（序列名取类简单名）
     * @return 下一个ID
     */
    public static long getSequenceId(Class<?> entityCls) {
        return getSequenceId(entityCls.getSimpleName());
    }


    /**
     * 通过SeqName获取当前主键ID（段缓存模型下为当前段游标值，即最近一次已发的号；未初始化返回0）。
     *
     * @return 当前已发号；未初始化返回 0
     */
    public static long getCurrentId(String seqName) {
        ensureInitialized();
        for (int tryCount = 0; tryCount < MAX_RETRY_TIMES; tryCount++) {
            try {
                Long seqId = KV_OP.get(REDIS_SEQ + seqName + CURSOR_SUFFIX);
                return seqId == null ? 0L : seqId;
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
            log.warn("WARNING: FusionSequence[{}] failed to obtain getCurrentId. Trying {} times ...", seqName, tryCount);
            // 仅单次 Redis GET，异常多为瞬时网络抖动，指数退避 5→50ms 快速重试
            sleepBackoff(tryCount, 5L, 50L);
        }
        throw new RuntimeException("FusionSequence[" + seqName + "] failed to obtain getCurrentId!!!");
    }

    /**
     * 通过SeqName获取主键ID（段缓存模型）。
     *
     * <p>核心模型：DB {@code sys_seq.seq_id} 是唯一真值源，Dao 与 Fusion 都通过它申请互不重叠的号段。
     * Fusion 在 Redis 缓存一个段 {@code [base, base+POOL_SIZE-1]}，段内取号只需一次 Redis INCR（Lua 原子），
     * 段耗尽时通过 {@link DaoSequenceFactory#allocateSequenceRange} 从 DB 申请下一段。</p>
     *
     * <p>由于 DB 行级 UPDATE 是串行的，Dao 与 Fusion 拿到的段天然不重叠，
     * 因此<b>同一个 seqName 被 Dao 与 Fusion 混合并发调用时也不会重复发号</b>（跳号/浪费可接受）。</p>
     *
     * @param seqName 序列名
     * @return 下一个ID
     */
    public static long getSequenceId(String seqName) {
        ensureInitialized();
        String cursorKey = REDIS_SEQ + seqName + CURSOR_SUFFIX;
        String endKey = REDIS_SEQ + seqName + END_SUFFIX;
        String renewLocker = REDIS_SEQ + seqName + ":renew";
        for (int tryCount = 0; tryCount < MAX_RETRY_TIMES; tryCount++) {
            try {
                // 原子取号：先判后增，仅 cursor<end 时取号并推进，否则返回 needRenew
                List<?> res = REDIS_TEMPLATE.execute(NEXT_SEQ_SCRIPT,
                        java.util.Arrays.asList(cursorKey, endKey));
                long seqId = ((Number) res.get(0)).longValue();
                long needRenew = ((Number) res.get(1)).longValue();
                if (needRenew == 0) {
                    return seqId;
                }
                // 段耗尽（或未初始化），抢续段锁后从 DB 申请新段
                // owner 使用随机 long 而非时间戳：毫秒级时间戳在同毫秒内可能重复，
                // 会导致 UNLOCK 误删他人持有的锁；全空间随机 long 碰撞概率可忽略
                Long renewOwner = ThreadLocalRandom.current().nextLong();
                if (!KV_OP.setIfAbsent(renewLocker, renewOwner, 10_000L, TimeUnit.MILLISECONDS)) {
                    // 锁被占，说明其他线程正在续段：指数退避 2→50ms 后重试取号，
                    // 持锁者续段通常几 ms~几十 ms，固定 sleep 会让等锁线程空转浪费吞吐
                    sleepBackoff(tryCount, 2L, 50L);
                    continue;
                }
                boolean renewed = false;
                try {
                    // 抢到锁后再次确认段确实耗尽：重新读取最新的 cursor 与 end 比对。
                    // 必须以 Redis 当前真值为准，避免误续段。
                    Long cur = KV_OP.get(cursorKey);
                    Long end = KV_OP.get(endKey);
                    if (end == null || cur == null || cur >= end) {
                        // 从 DB 申请下一段（与 Dao 共享 DB 行乐观锁，段互不重叠）。
                        long base = DaoSequenceFactory.allocateSequenceRange(seqName, POOL_SIZE);
                        // 整段切换：旧段已取完（cursor>=end），段内号全发出，零丢号；
                        // 原子设置新段 cursor=base-1、end=base+POOL_SIZE-1。
                        REDIS_TEMPLATE.execute(SWITCH_SEGMENT_SCRIPT,
                                java.util.Arrays.asList(cursorKey, endKey),
                                String.valueOf(base), String.valueOf(POOL_SIZE));
                        renewed = true;
                    } else {
                        // 二次确认发现段已被其他线程续过（新段就绪），标记续段完成
                        renewed = true;
                    }
                } finally {
                    // 安全解锁
                    try {
                        REDIS_TEMPLATE.execute(UNLOCK_SCRIPT,
                                Collections.singletonList(renewLocker), renewOwner.toString());
                    } catch (Throwable e) {
                        log.warn("FusionSequence[{}] 续段锁释放失败，等待TTL自动过期", seqName, e);
                    }
                }
                if (renewed) {
                    // 续段完成（本线程申请或确认到他线程申请的新段），新段已就绪，
                    // 立即重试取号，跳过底部的异常退避，避免无谓的 100ms 等待
                    continue;
                }
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
            log.warn("WARNING: FusionSequence[{}] failed to obtain getSequenceId. Trying {} times ...", seqName, tryCount);
            //异常退避：仅在真异常（Redis/DB 抛错）时执行，指数 10→100ms，避免对正常段切换路径的吞吐拖累
            sleepBackoff(tryCount, 10L, 100L);
        }
        throw new RuntimeException("FusionSequence[" + seqName + "] failed to obtain getSequenceId!!!");
    }

    /**
     * 通过传入entityClass名，重置sequenceId.
     *
     * @param entityCls
     */
    public static void resetSequenceId(Class<?> entityCls, long seqId) {
        resetSequenceId(entityCls.getSimpleName(), seqId);
    }

    /**
     * 重置sequenceId。
     * <p>段缓存模型下，重置 = 清除 Redis 段缓存（cursor/end/续段锁）并重置 DB 真值源。
     * 重置后下次 getSequenceId 会从 DB 重新申请段（从 seqId 开始）。</p>
     *
     * <p>执行顺序：先清 Redis 段缓存，再重置 DB。若 DB 失败抛异常重试，期间 Redis 段已被清除，
     * 下次取号会因段未初始化而重新从 DB 申请，不会发出旧段残留的号。</p>
     *
     * @param seqName
     * @param seqId
     */
    public static synchronized void resetSequenceId(String seqName, long seqId) {
        ensureInitialized();
        int tryCount = 0;
        do {
            tryCount++;
            try {
                // 清除 Redis 段缓存（cursor/end/续段锁），使下次取号重新从 DB 申请段
                REDIS_TEMPLATE.delete(java.util.Arrays.asList(
                        REDIS_SEQ + seqName + CURSOR_SUFFIX,
                        REDIS_SEQ + seqName + END_SUFFIX,
                        REDIS_SEQ + seqName + ":renew"));
                // 重置 dao seq（DB 真值源），失败抛异常触发重试
                DaoSequenceFactory.resetSequenceId(seqName, seqId, 100);
                return;
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
            log.warn("WARNING: FusionSequence[{}] failed to resetSequenceId. Trying {} times...", seqName, tryCount);
            // 重置依赖 Redis+DB 双写成功，异常多为瞬时故障，指数退避 10→100ms
            sleepBackoff(tryCount - 1, 10L, 100L);
        } while (tryCount < MAX_RETRY_TIMES);
        throw new RuntimeException("FusionSequence[" + seqName + "] failed to resetSequenceId!!!");
    }

    /**
     * 从池中随机分配一个id（按实体类名取池）。语义见 {@link #getRandomSequenceIdFromPool(String)}。
     *
     * @param entityCls 实体类
     * @return 随机分配的ID
     */
    public static long getRandomSequenceIdFromPool(Class<?> entityCls) {
        return getRandomSequenceIdFromPool(entityCls.getSimpleName());
    }

    /**
     * 从池中随机分配一个id。
     * <p>对于系统中非常重要的ID，采用随机分配以避免规律性ID带来的问题：</p>
     * <ol>
     *   <li>规律性增长容易被竞争对手判断业务量；</li>
     *   <li>连续ID在分库场景下数据分布不均。</li>
     * </ol>
     * <p>流程：从 Redis SET 池中 pop 一个 ID；池容量低于 {@link #POOL_MIN} 时，抢锁补池
     * （一次从 DB 申请 {@link #POOL_SIZE} 个 ID 放入池）。补池锁通过 owner 比对释放，避免误删。</p>
     *
     * @param seqName 序列名
     * @return 随机分配的ID
     */
    public static long getRandomSequenceIdFromPool(String seqName) {
        ensureInitialized();
        String redisKey = REDIS_SEQ_POOL + seqName;
        String redisLocker = redisKey + ":lock";
        int tryCount = 0;
        do {
            tryCount++;
            try {
                // 原子取号：池充足则 pop 返回，不足则返回补池标记
                List<?> res = REDIS_TEMPLATE.execute(POP_OR_RESERVE_SCRIPT,
                        Collections.singletonList(redisKey), String.valueOf(POOL_MIN));
                long poppedId = ((Number) res.get(0)).longValue();
                long needReserve = ((Number) res.get(1)).longValue();
                if (needReserve == 0 && poppedId > 0) {
                    return poppedId;
                }
                // 池不足，抢补池锁；抢到则从 DB 申请段，并用 Lua 原子补池+pop
                // owner 使用随机 long 而非时间戳，避免同毫秒重复导致 UNLOCK 误删他人锁
                Long lockOwner = ThreadLocalRandom.current().nextLong();
                if (!KV_OP.setIfAbsent(redisLocker, lockOwner, 10_000L, TimeUnit.MILLISECONDS)) {
                    // 锁被占，持锁者正在补池（几 ms~几十 ms）：指数退避 2→50ms 后重试，重试时池通常已补足
                    sleepBackoff(tryCount - 1, 2L, 50L);
                    continue;
                }
                try {
                    // 从 DB 申请一段（与 Dao/Fusion 共享 DB 行乐观锁，段互不重叠）
                    long base = DaoSequenceFactory.allocateSequenceRange(seqName, POOL_SIZE);
                    // 原子补池+pop：判空→SADD追加新段→SPOP，Redis 单线程执行，绝不重复
                    Long id = REDIS_TEMPLATE.execute(REFILL_AND_POP_SCRIPT,
                            Collections.singletonList(redisKey),
                            String.valueOf(base), String.valueOf(POOL_SIZE), String.valueOf(POOL_MIN));
                    if (id != null && id > 0) {
                        return id;
                    }
                    // 极端情况（并发把刚补的池瞬间抽空），立即重试，不走底部异常退避
                    continue;
                } finally {
                    // 安全解锁：仅当锁值仍为本线程 owner 时才删除，避免误删他人持有的锁
                    try {
                        REDIS_TEMPLATE.execute(UNLOCK_SCRIPT,
                                Collections.singletonList(redisLocker), lockOwner.toString());
                    } catch (Throwable e) {
                        log.warn("FusionSequence[{}] 补池锁释放失败，等待TTL自动过期", seqName, e);
                    }
                }
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
            log.warn("WARNING: FusionSequence[{}] failed to getRandomSequenceIdFromPool. Trying {} times ...", seqName, tryCount);
            //异常退避：仅真异常（Redis/DB 抛错）时执行，指数 10→100ms
            sleepBackoff(tryCount - 1, 10L, 100L);
        } while (tryCount < MAX_RETRY_TIMES);
        log.error("ERROR: FusionSequence[{}] failed to getRandomSequenceIdFromPool. Trying {} times ...", seqName, tryCount);
        //如果执行到这里，直接报错吧。
        throw new RuntimeException("FusionSequence[" + seqName + "] failed to getRandomSequenceIdFromPool!!!");
    }

    /**
     * 回收一个seqId到池中，使其可以重新用于分配。
     * 常见于前台恶意注册，浪费了seqId。
     *
     * @param entityCls 实体类（池名取类简单名）
     * @param seqId     要回收的ID
     */
    public void restoreSequenceIdToPool(Class<?> entityCls, long seqId) {
        restoreSequenceIdToPool(entityCls.getSimpleName(), seqId);
    }

    /**
     * 回收一个seqId到池中，使其可以重新用于分配。
     * 常见于前台恶意注册，浪费了seqId。
     *
     * @param seqName 序列名（池名）
     * @param seqId   要回收的ID
     */
    public void restoreSequenceIdToPool(String seqName, long seqId) {
        ensureInitialized();
        SET_OP.add(REDIS_SEQ_POOL + seqName, seqId);
    }

}
