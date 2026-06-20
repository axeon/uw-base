package uw.dao.sequence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.dao.DaoFactory;
import uw.common.data.PageRowSet;
import uw.dao.conf.DaoConfigManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * sequence工厂类，可以在集群环境中使用。
 * incrementNum是非常重要的参数，当其为1(默认值)时，tps≈(100)。
 * incrementNum和tps的关系公式为 tps = incrementNum*100。
 *
 * @author zhangjin
 */
public class DaoSequenceFactory {

    /**
     * 日志.
     */
    private static final Logger logger = LoggerFactory.getLogger(DaoSequenceFactory.class);
    /**
     * 初始化seq（INSERT IGNORE，并发建行时仅一条生效，其余忽略）。
     */
    private static final String INIT_SEQ = "insert ignore into sys_seq (seq_name,seq_id,seq_desc,increment_num,create_date,last_update) values(?,?,?,?,now(),now())";
    /**
     * 载入当前seq.
     */
    private static final String LOAD_SEQ = "select seq_id,increment_num from sys_seq where seq_name=? ";

    /**
     * 确认更新seq（乐观锁：仅当 seq_id=旧值时推进）。
     */
    private static final String UPDATE_SEQ = "update sys_seq set seq_id=?,last_update=now() where seq_name=? and seq_id=?";

    /**
     * 重置seq.
     */
    private static final String RESET_SEQ = "update sys_seq set seq_id=?,increment_num=?,last_update=now() where seq_name=?";

    /**
     * SequenceManager集合.
     */
    private static final Map<String, DaoSequenceFactory> seqFactory = new ConcurrentHashMap<String, DaoSequenceFactory>();

    /**
     * dao实例。
     */
    private static final DaoFactory dao = DaoFactory.getInstance();

    /**
     * 重试次数。
     */
    private static final int MAX_RETRY_TIMES = 100;

    /**
     * seqName.
     */
    private final String seqName;

    /**
     * 当前id.
     */
    private transient volatile long currentId = 0;

    /**
     * 当前可以获取的最大id.
     */
    private transient volatile long maxId = 0;

    /**
     * 默认增量数（仅在 DB 无行、首次建行时作为 incrementNum 占位值使用）。
     * 运行期实际步长始终从 DB 读取，避免跨线程共享实例字段导致的数据竞争。
     */
    private static final int DEFAULT_INCREMENT_NUM = 1;

    /**
     * 退避休眠：按重试次数指数增长，封顶 capMillis，避免固定 sleep 在条件已满足时仍空等。
     * 中断时恢复中断标志（调用方据返回值决定是否终止重试）。
     *
     * @param attempt   已重试次数（从 0 起）
     * @param floorMs   起始休眠毫秒
     * @param capMs     休眠上限毫秒
     * @return true 表示线程被中断（调用方可终止重试）；false 表示正常休眠完毕
     */
    private static boolean backoffSleep(int attempt, long floorMs, long capMs) {
        long shift = Math.min(attempt, 10);
        long millis = Math.min(capMs, floorMs << shift);
        try {
            Thread.sleep(millis);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }
    }

    /**
     * 建立一个Sequence实例.
     *
     * @param seqName seq名称。
     */
    private DaoSequenceFactory(String seqName) {
        this.seqName = seqName;
    }


    /**
     * 返回指定的表的currentId数值.
     *
     * @param seqName 表名
     * @return 下一个值
     */
    public static long getCurrentId(String seqName) {
        DaoSequenceFactory manager = seqFactory.computeIfAbsent(seqName, x -> new DaoSequenceFactory(seqName));
        return manager.currentId;
    }

    /**
     * 返回指定的表的sequenceId数值.
     *
     * @param seqName 表名
     * @return 下一个值
     */
    public static long getSequenceId(String seqName) {
        DaoSequenceFactory manager = seqFactory.computeIfAbsent(seqName, x -> new DaoSequenceFactory(seqName));
        synchronized (manager.seqName.intern()) {
            return manager.getSequenceId(1);
        }
    }

    /**
     * 申请一段连续ID号码范围 [start, start+range-1]，返回起始号码 start。
     * <p>直接通过 {@code reserveBlockImpl} 原子推进 DB，返回的段与本地号段缓存互不影响，
     * 可被 {@code FusionSequenceFactory} 等其他发号源安全调用，保证全局段不重叠。</p>
     *
     * @param seqName 表名
     * @param range   申请多少个号码
     * @return 起始号码（段的第一个ID）
     */
    public static long allocateSequenceRange(String seqName, long range) {
        DaoSequenceFactory manager = seqFactory.computeIfAbsent(seqName, x -> new DaoSequenceFactory(seqName));
        synchronized (manager.seqName.intern()) {
            long[] seg = manager.reserveBlockImpl(range);
            if (seg == null) {
                return -1;
            }
            // seg = [old, new]，号段 = (old, new]，起点 = old+1
            return seg[0] + 1;
        }
    }

    /**
     * 对齐申请一段ID号码，使起始号码为 range 的整数倍（向下对齐到下一个对齐点）。
     * <p>先申请到当前 DB 的 seq_id，计算到下一个对齐边界还需多少号，再申请该数量，
     * 返回对齐后的段起点。用于分库等需要 ID 对齐到固定步长的场景。</p>
     *
     * @param seqName 表名
     * @param range   对齐步长（号码数）
     * @return 对齐后的起始号码
     */
    public static long alignmentSequenceRange(String seqName, int range) {
        DaoSequenceFactory manager = seqFactory.computeIfAbsent(seqName, x -> new DaoSequenceFactory(seqName));
        synchronized (manager.seqName.intern()) {
            // 第一次申请1个号，拿到当前 DB 推进后的值 seg1[0]
            long[] seg1 = manager.reserveBlockImpl(1);
            if (seg1 == null) {
                return -1;
            }
            long currentEnd = seg1[1]; // 当前 DB seq_id
            // 计算到下一个对齐边界还需多少号（对齐到 range 的倍数）
            long rem = currentEnd % range;
            long need = rem == 0 ? 0 : (range - rem);
            if (need > 0) {
                long[] seg2 = manager.reserveBlockImpl(need);
                if (seg2 == null) {
                    return -1;
                }
                currentEnd = seg2[1];
            }
            // currentEnd 已对齐到 range 倍数，返回该对齐点作为起点
            return currentEnd;
        }
    }

    /**
     * 重置sequence信息。
     *
     * @param seqName      sequence名字
     * @param initSeq      初始值。
     * @param incrementNum 递增数。
     * @return
     */
    public static boolean resetSequenceId(String seqName, long initSeq, int incrementNum) {
        DaoSequenceFactory manager = seqFactory.computeIfAbsent(seqName, x -> new DaoSequenceFactory(seqName));
        synchronized (manager.seqName.intern()) {
            return manager.resetSequenceId(initSeq, incrementNum);
        }
    }

    /**
     * 返回下一个可以取到的id值,功能上类似于数据库的自动递增字段。
     * 由于集群环境下的资源竞争，系统最多会重试10s。
     * 如果返回-1，则说明获取失败。
     *
     * @param value seq值，如果=-1，则说明有问题。
     */
    private long getSequenceId(long value) {
        if (currentId + value > maxId) {
            // 本地号段不足，从 DB 申请一个 incrementNum 的号段
            if (!getNextBlock()) {
                return -1;
            }
        }
        return ++currentId;
    }

    /**
     * 从 DB 申请新号段，更新本地缓存 {@code [currentId, maxId]}。
     * <p>号段大小固定为 DB 的 incrementNum，支持运行期调整。</p>
     *
     * @return 是否成功
     */
    private boolean getNextBlock() {
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            long[] seg = reserveBlockImpl(-1);
            if (seg != null) {
                // seg = [old, new]，本地缓存覆盖 (old, new]
                this.currentId = seg[0];
                this.maxId = seg[1];
                return true;
            }
            logger.warn("WARNING: DaoSequenceFactory[{}] failed to obtain Sequence next ID block . Trying {}...", this.seqName, i + 1);
            // DB 抖动恢复即满足重试条件，指数退避 10→100ms，比固定 100ms 更快感知恢复
            if (backoffSleep(i, 10L, 100L)) {
                logger.error("DaoSequenceFactory[{}] getNextBlock interrupted", this.seqName);
                break;
            }
        }
        return false;
    }

    /**
     * 原子号段申请核心（不依赖实例 currentId/maxId 状态）。
     *
     * <p>流程（乐观锁 CAS 循环）：</p>
     * <ol>
     *   <li>从 DB load 当前 seq_id（记为 old）；若行不存在，INSERT IGNORE 建行后重新 load。</li>
     *   <li>newId = old + step；step&gt;0 用 step，否则用 DB 的 incrementNum。</li>
     *   <li>{@code UPDATE seq_id=newId WHERE seq_name=? AND seq_id=old}（乐观锁）。</li>
     *   <li>成功返回 {@code [old, newId]}（号段 = (old, newId]）；失败则重试。</li>
     * </ol>
     *
     * <p>DB 行级 UPDATE 串行，多调用者拿到的号段天然不重叠，保证全局唯一。
     * 本方法不读写实例的 currentId/maxId，{@code allocateSequenceRange} 可安全调用而不污染本地号段缓存。</p>
     *
     * @param step 号段步长（号数）；step&lt;=0 表示使用 DB 的 incrementNum
     * @return {@code [old, newId]}；重试耗尽返回 null
     */
    private long[] reserveBlockImpl(long step) {
        String connName = DaoConfigManager.getRouteMapping("sys_seq", "write");
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            try {
                PageRowSet ds = dao.queryForRowSet(connName, LOAD_SEQ, new Object[]{seqName});
                long old;
                // 本次申请使用的步长，始终从 DB 读取当前 incrementNum，避免实例字段跨线程竞争
                int dbIncrementNum = DEFAULT_INCREMENT_NUM;
                if (ds.next()) {
                    old = ds.getLong(0);
                    dbIncrementNum = ds.getInt(1);
                } else {
                    // 行不存在，INSERT IGNORE 建行（并发仅一条生效），建后重新 load
                    dao.execute(connName, INIT_SEQ, new Object[]{seqName, 0L, seqName, DEFAULT_INCREMENT_NUM});
                    ds = dao.queryForRowSet(connName, LOAD_SEQ, new Object[]{seqName});
                    if (!ds.next()) {
                        continue;
                    }
                    old = ds.getLong(0);
                    dbIncrementNum = ds.getInt(1);
                }
                long useStep = step > 0 ? step : dbIncrementNum;
                long newId = old + useStep;
                int effectedNum = dao.execute(connName, UPDATE_SEQ, new Object[]{newId, seqName, old});
                if (effectedNum == 1) {
                    return new long[]{old, newId};
                }
                // CAS 失败（被其他实例/线程抢先），重试
            } catch (Throwable e) {
                logger.error("DaoSequenceFactory reserveBlockImpl exception! {}", e.getMessage(), e);
            }
            // CAS 失败时其他线程刚释放 DB 行锁，指数退避 1→10ms 快速重试抢占
            if (backoffSleep(i, 1L, 10L)) {
                return null;
            }
        }
        return null;
    }

    /**
     * 重置sequence信息。
     *
     * @param initSeq      初始值。
     * @param incrementNum 递增数。
     * @return 是否重置成功
     */
    private boolean resetSequenceId(long initSeq, int incrementNum) {
        boolean success = false;
        try {
            String connName = DaoConfigManager.getRouteMapping("sys_seq", "write");
            int effectedNum = dao.execute(connName, RESET_SEQ, new Object[]{initSeq, incrementNum, seqName});
            if (effectedNum == 0) {
                // 行不存在（该 seq 从未发过号），直接 INSERT 建行为目标值（upsert 语义）
                dao.execute(connName, INIT_SEQ, new Object[]{seqName, initSeq, seqName, incrementNum});
            }
            success = true;
            // 本地缓存失效到 initSeq，下次发号会触发新号段申请，从 initSeq 开始
            this.currentId = initSeq;
            this.maxId = initSeq;
        } catch (Throwable e) {
            logger.error("DaoSequenceFactory resetSeq exception! {}", e.getMessage(), e);
        }
        return success;
    }

}
