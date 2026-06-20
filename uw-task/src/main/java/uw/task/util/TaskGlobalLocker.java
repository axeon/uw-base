package uw.task.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import uw.task.conf.TaskProperties;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的全局任务 Leader 选举锁。
 *
 * <p>实现思路：在多个实例之间选举出唯一一个负责运行全局任务的 Leader。
 * 锁的值为实例自身标识（app:version@host:port）。所有"抢锁 / 续期 / 判断归属"
 * 逻辑整合在一段 Lua 脚本中由 Redis 单线程原子执行，保证任意时刻最多一个实例认为自己是 Leader。</p>
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
 * <p>返回 1 表示本实例为 Leader，返回 0 表示非 Leader。整个流程只产生一次 Redis 往返。</p>
 *
 * <h3>使用约束</h3>
 * <ul>
 *   <li>非 Leader 实例不会执行全局任务，调用方应在任务执行前调用 {@link #checkLeader()}。</li>
 *   <li>{@link #checkLeader()} 的调用频率必须小于 {@link #LOCK_MILLIS}（默认 90 秒），
 *       否则 Leader 持有的锁会因 TTL 过期而丢失，可能引发 Leader 频繁切换。</li>
 *   <li>实例正常下线时锁依赖 TTL 自然过期，接管延迟最长为 {@link #LOCK_MILLIS}。</li>
 *   <li>实例重启后，只要原锁 key 尚未过期且值仍为自身标识，重启后首次 {@link #checkLeader()}
 *       会直接续期成功，不会丢失原本属于自己的 Leader 身份。</li>
 * </ul>
 *
 * <h3>线程安全</h3>
 * 本类是线程安全的：{@link #leader} 为 {@code volatile}，{@link StringRedisTemplate} 与
 * {@link DefaultRedisScript} 均线程安全，{@link #checkLeader()} 可被多个线程并发调用。
 *
 * @author axeon
 */
public class TaskGlobalLocker {

    private static final Logger log = LoggerFactory.getLogger(TaskGlobalLocker.class);

    /**
     * Redis 锁 key 的统一前缀。
     */
    private static final String REDIS_TAG = "uw-task-locker:";

    /**
     * 默认锁定（Leader 任期）时间，单位毫秒。
     * <p>Leader 必须在该时间内至少调用一次 {@link #checkLeader()} 续期，否则锁过期。</p>
     */
    private static final long LOCK_MILLIS = 90_000L;

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
     * Redis 操作模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 锁的完整 key 名（前缀 + 项目名）。
     */
    private final String lockerName;

    /**
     * 锁的值，即当前实例标识：{@code app:version@host:port}。
     * <p>用于判断锁是否由本实例持有，也便于运维排查当前 Leader 归属。</p>
     */
    private final String lockerData;

    /**
     * 当前实例是否为 Leader（最近一次 {@link #checkLeader()} 的结果快照）。
     */
    private volatile boolean leader;

    /**
     * 构造全局任务锁。
     *
     * @param redisConnectionFactory Redis 连接工厂，用于内部构建 {@link StringRedisTemplate}
     * @param taskProperties         任务配置，提供项目名（决定锁 key）及实例标识（决定锁值）
     */
    public TaskGlobalLocker(final RedisConnectionFactory redisConnectionFactory, TaskProperties taskProperties) {
        this.stringRedisTemplate = new StringRedisTemplate(redisConnectionFactory);
        this.stringRedisTemplate.afterPropertiesSet();
        this.lockerName = REDIS_TAG + taskProperties.getTaskProject();
        this.lockerData = taskProperties.getAppName() + ":" + taskProperties.getAppVersion() + "@"
                + taskProperties.getAppHost() + ":" + taskProperties.getAppPort();
    }

    /**
     * 返回最近一次 {@link #checkLeader()} 的选举结果。
     * <p>注意：该值为本地缓存的快照，不会主动刷新，调用方应通过 {@link #checkLeader()} 获取实时状态。</p>
     *
     * @return 当前实例是否为 Leader
     */
    public boolean isLeader() {
        return leader;
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
     * 这意味着 Redis 短暂抖动期间，原本的 Leader 仍会按本地快照继续执行 singleton 全局任务，
     * 优先保证可用性（接受抖动窗口内极小概率的双跑）；Redis 恢复后下一次调用会自动修正状态。</p>
     *
     * @return 本次检查后当前实例是否为 Leader
     */
    public boolean checkLeader() {
        try {
            Long result = stringRedisTemplate.execute(
                    LEADER_SCRIPT, Collections.singletonList(lockerName), lockerData, String.valueOf(LOCK_MILLIS));
            markLeader(result != null && result > 0);
        } catch (Exception e) {
            // Redis 异常时保留上一次 Leader 状态，优先保证可用性；接受抖动窗口内极小概率的双跑
            log.warn("TaskGlobalLocker: 检查 Leader 失败, 保留上次状态 leader=[{}], locker=[{}]", leader, lockerName, e);
        }
        return leader;
    }

    /**
     * 更新本地 Leader 状态，并在状态翻转时记录日志。
     *
     * @param nowLeader 最新选举结果
     */
    private void markLeader(boolean nowLeader) {
        if (nowLeader != leader) {
            leader = nowLeader;
            if (nowLeader) {
                log.info("TaskGlobalLocker: 本实例当选 Leader, locker=[{}], data=[{}]", lockerName, lockerData);
            } else {
                log.info("TaskGlobalLocker: 本实例失去 Leader 身份, locker=[{}]", lockerName);
            }
        }
    }

}
