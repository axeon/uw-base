package uw.task.util;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * uw-task 私有 Leader 选举器（语义等价移植自 uw-kv GlobalLeaderElector，key 空间私有 uw-task-leader:）。
 *
 * <p>注册制：config(name, instanceId) 注册稳定实例标识（跨重启续期）；checkLeader 周期执行
 * Lua（持有则续期 / 空则抢锁 / 否则失败），异常时保留上次状态不翻转。SINGLETON croner 仅在
 * Leader 实例执行。</p>
 *
 * @author axeon
 */
public final class TaskLeaderElector {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(TaskLeaderElector.class);

    /**
     * redis key 前缀（uw-task 私有空间）。
     */
    private static final String REDIS_PREFIX = "uw-task-leader:";

    /**
     * 锁 TTL（毫秒）：覆盖检查周期（60s）的 1.5 倍余量。
     */
    private static final long DEFAULT_LOCK_MILLIS = 90_000L;

    /**
     * 选举 Lua：持有者续期返回 1；key 空则 SET 抢锁返回 1；否则返回 0。
     */
    private static final DefaultRedisScript<Long> LEADER_SCRIPT = new DefaultRedisScript<>(
            "local v=redis.call('get',KEYS[1]) "
                    + "if v==ARGV[1] then redis.call('pexpire',KEYS[1],ARGV[2]) return 1 end "
                    + "if v==false then redis.call('set',KEYS[1],ARGV[1],'PX',ARGV[2]) return 1 end "
                    + "return 0",
            Long.class);

    /**
     * 私有 String 模板（init 注入连接工厂后自建）。
     */
    private static StringRedisTemplate stringRedisTemplate;

    /**
     * 注册表：选举名 → 参与者状态。
     */
    private static final ConcurrentMap<String, ElectorState> states = new ConcurrentHashMap<>();

    private TaskLeaderElector() {
    }

    /**
     * 注入私有连接工厂（自动配置调用一次）。
     */
    public static void init(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();
        TaskLeaderElector.stringRedisTemplate = template;
    }

    /**
     * 注册选举参与者（稳定实例标识，跨重启续期）。
     *
     * @param name       选举名（如任务项目名）
     * @param instanceId 稳定实例标识（appName:version@host:port）
     */
    public static void config(String name, String instanceId) {
        states.compute(name, (key, existing) ->
                existing != null && existing.instanceId.equals(instanceId)
                        ? existing
                        : new ElectorState(instanceId));
    }

    /**
     * 本实例当前是否 Leader（未注册/未检查过返回 false）。
     */
    public static boolean isLeader(String name) {
        ElectorState state = states.get(name);
        return state != null && state.leader;
    }

    /**
     * 执行一次选举检查（周期调用，如 60s）；异常保留上次状态。
     */
    public static boolean checkLeader(String name) {
        ElectorState state = states.get(name);
        if (state == null) {
            return false;
        }
        try {
            Long result = stringRedisTemplate.execute(
                    LEADER_SCRIPT, Collections.singletonList(REDIS_PREFIX + name), state.instanceId, String.valueOf(DEFAULT_LOCK_MILLIS));
            state.markLeader(name, result != null && result > 0);
        } catch (Exception e) {
            log.warn("TaskLeaderElector: 检查 Leader 失败, 保留上次状态 leader=[{}], name=[{}]", state.leader, name, e);
        }
        return state.leader;
    }

    private static final class ElectorState {
        final String instanceId;
        volatile boolean leader;

        ElectorState(String instanceId) {
            this.instanceId = instanceId;
        }

        void markLeader(String lockKey, boolean nowLeader) {
            if (nowLeader != leader) {
                leader = nowLeader;
                if (nowLeader) {
                    log.info("TaskLeaderElector: 本实例当选 Leader, lockKey=[{}], instanceId=[{}]", lockKey, instanceId);
                } else {
                    log.info("TaskLeaderElector: 本实例失去 Leader 身份, lockKey=[{}], instanceId=[{}]", lockKey, instanceId);
                }
            }
        }
    }
}
