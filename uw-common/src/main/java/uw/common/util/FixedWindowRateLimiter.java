package uw.common.util;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 单机固定窗口限速器，限速器家族的本地（Local）形态。
 *
 * <p>以 {@code windowSizeInMillis} 为一个时间窗口，窗口内累计许可数不得超过 {@link #limitPermits}、
 * 累计权重不得超过 {@link #limitWeight}（网关场景下权重即流量字节数）。窗口边界对齐到系统时钟网格
 * （{@code floor(now/windowSizeInMillis)}），超过窗口后计数归零，进入下一窗口。</p>
 *
 * <h3>双维度通用命名</h3>
 * 两维度为通用抽象：<b>permits</b>（许可次数）与 <b>weight</b>（权重）——调用方按自身域赋予含义
 * （如网关：许可=请求数、权重=请求/响应字节数；其他域：许可=操作次数、权重=自定义计费权重）。
 * 仅需单维度时用单参构造器与 {@link #tryAcquire(int)} 重载即可（权重维度不参与）。
 *
 * <h3>两阶段限流</h3>
 * 实际流量在请求进入时尚不可知时（如网关的请求/响应字节数），采用两阶段：
 * <ul>
 *   <li>请求入口：{@link #tryAcquire(int, long)} 通常传 {@code weight=0}，只检查并占用"许可"配额；</li>
 *   <li>请求结束：{@link #addPermits(int)} / {@link #addWeight(long)} 按实际数量回填对应配额。</li>
 * </ul>
 * <p>因此权重维度是"事后统计"——本次调用不会被权重维度拦截，但会影响后续调用的判定。</p>
 *
 * <h3>同步策略</h3>
 * <p>窗口重置与计数累加在 {@link ReentrantLock} 临界区内原子完成，避免"重置与累加交叉"导致的计数失真
 * 或限流失效。策略经 32 线程 JMH 三方案对照实测选定（ReentrantLock/synchronized/无锁 packed CAS）：
 * 高争用下 ReentrantLock 最优（锁排队串行化极短临界区），单热点 CAS 因自旋失败退化 30 倍——
 * 限流计数的判定语义需要单点原子性，互斥锁即最优解（详见 test 下对照基准与锁字段 javadoc）。</p>
 *
 * <h3>算法限制</h3>
 * <p>固定窗口在窗口边界处存在最多 2 倍突发。</p>
 *
 * <h3>限速器家族形态</h3>
 * <ul>
 *   <li>本地形态：本类（uw-common，零依赖纯内存，双维度+明细 VO）；</li>
 *   <li>全局形态：{@code uw.kv.GlobalFixedWindowRateLimiter}（uw-kv，Redis 固定窗口，单维度，
 *       返回 long 等待毫秒取最轻——两形态各为其场景最优，勿相互"统一"）；</li>
 * </ul>
 * 全局/融合形态窗口边界同为墙钟网格对齐，同配置下行为可互推。
 *
 * <h3>语义说明</h3>
 * <ul>
 *   <li>窗口边界对齐到墙钟网格（如 windowSize=1s 时窗口为整秒边界），所有实例的同类窗口边界一致。</li>
 *   <li>限流对象创建后首个窗口长度取决于创建时刻在网格中的位置（0 ~ windowSize 之间），属正常行为。</li>
 *   <li>窗口切换以 {@link #tryAcquire(int, long)} 为权威：发现窗口过期时归零所有计数。
 *   {@link #addPermits(int)} / {@link #addWeight(long)} 不触发窗口重置，跨窗口回填计入回填时刻所属窗口，
 *   存在轻微统计偏差，不影响限流正确性。</li>
 * </ul>
 *
 * @author axeon
 */
public final class FixedWindowRateLimiter {

    /**
     * 时间窗口，毫秒级。
     */
    private final long windowSizeInMillis;

    /**
     * 窗口内许可数上限。
     */
    private final int limitPermits;

    /**
     * 窗口内权重上限。
     */
    private final long limitWeight;

    /**
     * 保护窗口状态的锁。临界区为纯内存算术（纳秒级），持锁极短。
     * <p>同步策略经 32 线程 JMH 对照实测选定（见 test 下 FixedWindowRateLimiterLockBenchmark）：
     * ReentrantLock ≈62M ops/s &gt; synchronized ≈20-25M ops/s &gt;&gt; 单热点 packed CAS ≈2M ops/s
     * （高争用下 CAS 自旋退化）。限流计数需要单点原子性（striping 的非原子 sum 无法支撑判定语义），
     * 高争用下互斥锁即最优解。</p>
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 当前窗口编号 = floor(now / windowSizeInMillis)，初值 0 保证首次调用必然触发窗口初始化。
     */
    private long windowIndex = 0;

    /**
     * 当前窗口累计许可数（锁内读写，无需 volatile）。
     */
    private long windowPermits = 0;

    /**
     * 当前窗口累计权重（锁内读写，无需 volatile）。
     */
    private long windowWeight = 0;

    /**
     * 构造函数。
     *
     * @param windowSizeInMillis 时间窗口，毫秒级。小于 1 表示不限流
     * @param limitPermits       时间窗口内允许的最大许可数
     * @param limitWeight        时间窗口内允许的最大权重；仅需单维度时传 {@code Long.MAX_VALUE}
     */
    public FixedWindowRateLimiter(long windowSizeInMillis, int limitPermits, long limitWeight) {
        this.windowSizeInMillis = windowSizeInMillis;
        this.limitPermits = limitPermits;
        this.limitWeight = limitWeight;
    }

    /**
     * 构造函数（单维度：仅许可数，权重维度不限）。
     *
     * @param windowSizeInMillis 时间窗口，毫秒级。小于 1 表示不限流
     * @param limitPermits       时间窗口内允许的最大许可数
     */
    public FixedWindowRateLimiter(long windowSizeInMillis, int limitPermits) {
        this(windowSizeInMillis, limitPermits, Long.MAX_VALUE);
    }

    /**
     * 尝试占用配额（单维度便捷重载：仅许可维度，权重不预占）。
     *
     * @param permits 本次申请的许可数
     * @return {@code null} 表示通过；否则返回 {@link Result}（窗口剩余毫秒与各维度剩余额度）
     */
    public Result tryAcquire(int permits) {
        return tryAcquire(permits, 0L);
    }

    /**
     * 尝试占用配额（双维度），是窗口切换的权威判定点。
     *
     * <p>许可维度立即检查并占用；权重维度仅在 {@code weight > 0} 时预占（两阶段场景传 0，
     * 事后用 {@link #addWeight(long)} 回填）。每次调用先检测当前是否进入新窗口，是则归零所有计数。</p>
     * <p>返回形态说明：本形态返回明细 VO（含各维度剩余额度）是两阶段/双维度场景所需；
     * 全局/融合形态（uw-kv）无权重维度，返回 {@code long}（0=通过，&gt;0=等待毫秒）取最轻。</p>
     *
     * @param permits 本次申请的许可数（负值按 0 处理——负值会变相"返还"配额，可被构造请求绕过限流）
     * @param weight  本次预占的权重；传 &lt;= 0 表示不预占（事后用 {@link #addWeight(long)} 回填）
     * @return {@code null} 表示通过（未超限）；否则返回 {@link Result}，任一维度超出即整体拒绝
     */
    public Result tryAcquire(int permits, long weight) {
        // 时间窗口小于 1，不限制
        if (this.windowSizeInMillis < 1) {
            return null;
        }
        // 防御负入参：负的 permits/weight 会变相"返还"配额，可被构造请求绕过限流。
        if (permits < 0) {
            permits = 0;
        }
        if (weight < 0) {
            weight = 0;
        }
        lock.lock();
        try {
            long now = SystemClock.now();
            long idx = now / windowSizeInMillis;
            // 窗口切换则归零全部计数（tryAcquire 是窗口切换的权威判定点）
            if (idx != windowIndex) {
                windowIndex = idx;
                windowPermits = 0;
                windowWeight = 0;
            }
            // 占用许可配额
            windowPermits += permits;
            // 占用权重配额（weight<=0 时不预占，交由 addWeight 事后回填）
            if (weight > 0) {
                windowWeight += weight;
            }
            if (windowPermits > limitPermits || windowWeight > limitWeight) {
                long remainMillis = windowSizeInMillis - (now - idx * windowSizeInMillis);
                return new Result(remainMillis, limitPermits - windowPermits, limitWeight - windowWeight);
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 事后回填实际许可数。
     * <p>两阶段场景（与 {@link #addWeight(long)} 对称）：入口按预估值或 0 占用，结束后按实际数量回填，
     * 影响后续调用的许可维度判定。不触发窗口重置（窗口切换权威在 {@link #tryAcquire}），
     * 跨窗口回填计入回填时刻所属窗口，存在轻微统计偏差，不影响限流正确性。</p>
     *
     * @param permits 本次回填的许可数
     */
    public void addPermits(int permits) {
        // 防御负入参：负回填会变相"返还"配额，与 tryAcquire 的负参防御同口径
        if (permits < 0) {
            permits = 0;
        }
        lock.lock();
        try {
            windowPermits += permits;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 事后回填实际权重。
     *
     * <p>两阶段场景下，实际权重只有在请求结束后才知道（如网关的请求/响应字节数），因此请求入口的
     * {@link #tryAcquire} 通常不预占权重，而在请求结束后调用本方法按真实值回填，用于影响后续调用的
     * 权重维度判定。</p>
     *
     * <p>本方法<b>不触发窗口重置</b>（窗口切换由 {@link #tryAcquire} 统一负责），因此不会干扰许可维度计数。
     * 若回填发生时窗口已切换（由后续某次 {@code tryAcquire} 检测并重置），本次回填的权重将计入
     * {@code windowIndex} 所标记的窗口，存在轻微统计偏差，不影响限流正确性。</p>
     *
     * @param weight 本次回填的权重
     */
    public void addWeight(long weight) {
        // 防御负入参：负回填会变相"返还"配额（如网关回填值来自请求 Content-Length 解析，可被构造），与 tryAcquire 同口径
        if (weight < 0) {
            weight = 0;
        }
        lock.lock();
        try {
            windowWeight += weight;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 拒绝结果（限速器家族通用语义，三字段统一"剩余"视角）。
     *
     * <p>{@link #tryAcquire(int, long)} 返回 {@code null} 表示通过（未超限），
     * 返回本对象表示拒绝，三字段一致回答"本窗口还剩多少"：剩多久（时间）、剩多少许可、剩多少权重。
     * 拒绝时额度字段为负值（绝对值即超出量，如网关流控响应的 overRequests = -remainPermits）。
     * 字段为 primitive、手写只读（禁 Lombok），仅在拒绝路径分配（放行路径返回 null 零分配）。</p>
     */
    public static final class Result {

        /**
         * 当前窗口剩余毫秒（大于 0；到点窗口翻转重新计数）。
         */
        public final long remainMillis;

        /**
         * 当前窗口剩余许可额度（limitPermits - windowPermits；拒绝时为负值，绝对值即超出量）。
         */
        public final long remainPermits;

        /**
         * 当前窗口剩余权重额度（limitWeight - windowWeight；拒绝时为负值，绝对值即超出量；单维度场景为正冗余值）。
         */
        public final long remainWeight;

        /**
         * 构造拒绝结果。
         *
         * @param remainMillis  当前窗口剩余毫秒（大于 0）
         * @param remainPermits 当前窗口剩余许可额度
         * @param remainWeight  当前窗口剩余权重额度
         */
        public Result(long remainMillis, long remainPermits, long remainWeight) {
            this.remainMillis = remainMillis;
            this.remainPermits = remainPermits;
            this.remainWeight = remainWeight;
        }

    }

}
