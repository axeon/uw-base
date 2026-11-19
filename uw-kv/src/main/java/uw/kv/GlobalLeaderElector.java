package uw.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于 Redis 的 Leader 选举器（注册制，对齐 {@link FusionCounter#config} 的 static 家族风格）。
 *
 * <p>在多个实例之间选举出唯一一个 Leader（如全局任务执行者）。使用前先以
 * {@link #config(String, String)} 注册锁（name + 稳定实例标识），之后周期性调用
 * {@link #checkLeader(String)} 竞选/续期。锁的值为注册的<b>稳定实例标识</b>
 * （如 {@code app:version@host:port}）——实例重启后只要原锁 key 尚未过期且值仍为自身标识，
 * 重启后首次 {@link #checkLeader(String)} 会直接续期成功，不丢失原本属于自己的 Leader 身份。
 * 所有"抢锁 / 续期 / 判断归属"逻辑整合在一段 Lua 脚本中由 Redis 单线程原子执行，
 * 保证任意时刻最多一个实例认为自己是 Leader，且整个流程只产生一次 Redis 往返。</p>
 *
 * <h3>Leader 选举脚本语义</h3>
 * <pre>
 * local v = redis.call('get', KEYS[1])
 * if v == ARGV[1] then              -- 锁由本实例持有，续期
 *     redis.call('pexpire', KEYS[1], ARGV[2]); return 1
 * end
 * if v == false then                -- 锁不存在，抢锁
 *     redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[2]); return 1
 * end
 * return 0                          -- 锁由他人持有，什么都不做
 * </pre>
 *
 * <h3>使用约束</h3>
 * <ul>
 *   <li>非 Leader 实例不执行全局逻辑，调用方应在执行前调用 {@link #checkLeader(String)}。</li>
 *   <li>{@link #checkLeader(String)} 的调用频率必须小于 {@code lockMillis}（默认 90 秒），
 *       否则 Leader 持有的锁会因 TTL 过期而丢失，可能引发 Leader 频繁切换。</li>
 *   <li>实例正常下线时锁依赖 TTL 自然过期，接管延迟最长为 {@code lockMillis}。</li>
 * </ul>
 * <h3>实现取舍：为什么是 Lua 而非新命令组合</h3>
 * Redis 8.x 无单命令三态原语（持有续期/无主抢锁/他主持有）。最接近的替代是
 * {@code GETEX key PX}(GET+续期原子) + {@code SET NX PX}(抢锁) 两命令组合：语义可行但两次往返、
 * 三态逻辑分散在客户端；Lua 单往返原子、三态集中由 Redis 单线程裁定。心跳为低频（分钟级），
 * 两条路线的往返差异无感，取语义集中者。勿因新版本命令特性"简化"回两命令组合。
 *
 * <h3>key 约定</h3>
 * 家族统一风格：类内前缀 {@code uw-leader:} + 调用方 name（建议含域标识，如项目名）。
 *
 * <h3>异常语义</h3>
 * Redis 不可用时<b>保留上一次的 Leader 状态</b>——原本的 Leader 按本地快照继续执行
 * （优先保证可用性，接受抖动窗口内极小概率的双跑）；Redis 恢复后下一次调用自动修正状态。
 *
 * <h3>线程安全</h3>
 * 线程安全：注册表为 {@link ConcurrentMap}，各锁状态内部 volatile，{@link StringRedisTemplate} 与
 * {@link DefaultRedisScript} 均线程安全，{@link #checkLeader(String)} 可被多线程并发调用。
 *
 * @author axeon
 */
public class GlobalLeaderElector {

    private static final Logger log = LoggerFactory.getLogger(GlobalLeaderElector.class);

    /**
     * redis key 前缀（家族统一风格：类内前缀 + 调用方 name）。
     */
    private static final String REDIS_PREFIX = "uw-leader:";

    /**
     * 默认锁定（Leader 任期）时间，单位毫秒。
     */
    private static final long DEFAULT_LOCK_MILLIS = 90_000L;

    /**
     * Leader 选举 / 续期 Lua 脚本。
     * <p>KEYS[1]=锁 key，ARGV[1]=自身标识，ARGV[2]=TTL（毫秒）。
     * 返回 1 表示本实例为 Leader（抢锁成功或续期成功），返回 0 表示非 Leader（锁由他人持有）。</p>
     */
    private static final DefaultRedisScript<Long> LEADER_SCRIPT = new DefaultRedisScript<>(
            "local v=redis.call('get',KEYS[1]) "
                    + "if v==ARGV[1] then redis.call('pexpire',KEYS[1],ARGV[2]) return 1 end "
                    + "if v==false then redis.call('set',KEYS[1],ARGV[1],'PX',ARGV[2]) return 1 end "
                    + "return 0",
            Long.class);

    /**
     * redis 操作模板，由 Spring 注入 static 字段。
     */
    private static StringRedisTemplate stringRedisTemplate;

    /**
     * 注册表：lockKey → 选举状态。
     */
    private static final ConcurrentMap<String, ElectorState> states = new ConcurrentHashMap<>();

    /**
     * 构造方法，由 Spring 注入 uw.kv 连接工厂，内部自建 StringRedisTemplate 到 static 字段。
     * <p>不注入 Boot 自动配置的 {@code stringRedisTemplate}——那是业务 Redis（spring.data.redis）的连接，
     * 协调层组件必须走 uw.kv 隔离连接。</p>
     *
     * @param redisConnectionFactory uw.kv 连接工厂
     */
    public GlobalLeaderElector(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate(redisConnectionFactory);
        template.afterPropertiesSet();
        GlobalLeaderElector.stringRedisTemplate = template;
    }

    /**
     * 注册 Leader 选举锁（默认任期 90 秒），一般放在静态块或自动配置中调用一次。
     *
     * @param name        选举名称（建议含调用方域标识，如项目名——完整 key 为 {@code uw-leader:{name}}）
     * @param instanceId  稳定实例标识（如 app:version@host:port），跨重启稳定以保留续期权
     */
    public static void config(String name, String instanceId) {
        config(name, instanceId, DEFAULT_LOCK_MILLIS);
    }

    /**
     * 注册 Leader 选举锁。
     *
     * @param name        选举名称
     * @param instanceId  稳定实例标识（如 app:version@host:port），跨重启稳定以保留续期权
     * @param lockMillis  锁定（Leader 任期）时间，毫秒；checkLeader 调用间隔必须小于该值
     */
    public static void config(String name, String instanceId, long lockMillis) {
        states.compute(name, (key, existing) ->
                existing != null && existing.instanceId.equals(instanceId) && existing.lockMillis == lockMillis
                        ? existing
                        : new ElectorState(instanceId, lockMillis));
    }

    /**
     * 返回最近一次 {@link #checkLeader(String)} 的选举结果。
     * <p>注意：该值为本地缓存的快照，不会主动刷新，调用方应通过 {@link #checkLeader(String)} 获取实时状态。</p>
     *
     * @param name 注册的选举名称
     * @return 当前实例是否为 Leader；未注册的 name 返回 false
     */
    public static boolean isLeader(String name) {
        ElectorState state = states.get(name);
        return state != null && state.leader;
    }

    /**
     * 检查/竞选 Leader，并刷新本地 Leader 状态。
     *
     * <p>通过 {@link #LEADER_SCRIPT} 在 Redis 端原子地完成"抢锁 / 续期 / 判断归属"，
     * 整个流程只产生一次 Redis 往返。返回值含义：</p>
     * <ul>
     *   <li>锁不存在 → 抢锁，成为 Leader。</li>
     *   <li>锁由本实例持有 → 续期，保持 Leader。</li>
     *   <li>锁由他人持有 → 什么都不做，非 Leader。</li>
     * </ul>
     *
     * <p>Leader 状态翻转时会记录日志，便于排查切换时机。</p>
     *
     * <p><b>异常处理</b>：若 Redis 不可用导致脚本执行抛异常，保留上一次的 Leader 状态不变。
     * 这意味着 Redis 短暂抖动期间，原本的 Leader 仍会按本地快照继续执行全局逻辑，
     * 优先保证可用性（接受抖动窗口内极小概率的双跑）；Redis 恢复后下一次调用会自动修正状态。</p>
     *
     * @param name 注册的选举名称
     * @return 本次检查后当前实例是否为 Leader；未注册的 name 返回 false
     */
    public static boolean checkLeader(String name) {
        ElectorState state = states.get(name);
        if (state == null) {
            return false;
        }
        try {
            Long result = stringRedisTemplate.execute(
                    LEADER_SCRIPT, Collections.singletonList(REDIS_PREFIX + name), state.instanceId, String.valueOf(state.lockMillis));
            state.markLeader(name, result != null && result > 0);
        } catch (Exception e) {
            // Redis 异常时保留上一次 Leader 状态，优先保证可用性；接受抖动窗口内极小概率的双跑
            log.warn("GlobalLeaderElector: 检查 Leader 失败, 保留上次状态 leader=[{}], name=[{}]", state.leader, name, e);
        }
        return state.leader;
    }

    /**
     * 单个锁的选举状态：实例标识 + 任期 + Leader 快照。
     */
    private static final class ElectorState {

        /**
         * 稳定实例标识（锁值，便于运维排查当前 Leader 归属）。
         */
        final String instanceId;

        /**
         * 锁定（Leader 任期）时间，毫秒。
         */
        final long lockMillis;

        /**
         * 当前实例是否为 Leader（最近一次 checkLeader 的结果快照）。
         */
        volatile boolean leader;

        ElectorState(String instanceId, long lockMillis) {
            this.instanceId = instanceId;
            this.lockMillis = lockMillis;
        }

        /**
         * 更新本地 Leader 状态，并在状态翻转时记录日志（带 lockKey——一个 JVM 可能注册多把选举锁，
         * 排查切换时机须能区分）。
         *
         * @param lockKey   注册的锁 key
         * @param nowLeader 最新选举结果
         */
        void markLeader(String lockKey, boolean nowLeader) {
            if (nowLeader != leader) {
                leader = nowLeader;
                if (nowLeader) {
                    log.info("GlobalLeaderElector: 本实例当选 Leader, lockKey=[{}], instanceId=[{}]", lockKey, instanceId);
                } else {
                    log.info("GlobalLeaderElector: 本实例失去 Leader 身份, lockKey=[{}], instanceId=[{}]", lockKey, instanceId);
                }
            }
        }
    }

}
