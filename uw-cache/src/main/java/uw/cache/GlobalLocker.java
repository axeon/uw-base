package uw.cache;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import uw.cache.util.RedisKeyUtils;
import uw.common.util.SnowflakeIdGenerator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 实现的全局锁。
 * <p>
 * 基于 setnx（setIfAbsent）实现分布式互斥锁。stamp 为 SnowflakeId（跨 JVM 全局唯一），
 * {@link #unlock} / {@link #keepLock} 通过 Lua CAS 脚本保证原子性，避免锁过期被抢占后误删他人锁。
 * <p>
 * 关键约束：业务执行时间不可超过 {@code lockTimeMillis}，否则锁自动释放、其他实例可抢占；
 * 长任务需周期性调用 {@link #keepLock} 续期。
 */
public class GlobalLocker {

    private static final Logger log = LoggerFactory.getLogger(GlobalLocker.class);

    /**
     * redis前缀。
     */
    private static final String REDIS_PREFIX = "uw-locker:";

    /**
     * CAS解锁Lua脚本。
     * <p>
     * 旧的 unlock 实现先 get 再 delete，两步非原子：当持锁者的锁恰好过期、且被其他实例抢占后，
     * 原 holder 的 delete 可能误删新 holder 的锁。使用 Lua 脚本保证 "比较 stamp 匹配后再删除" 的原子性。
     * 返回 1 表示解锁成功，0 表示 stamp 不匹配或锁已不存在。
     */
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    /**
     * CAS续期Lua脚本。
     * <p>
     * keepLock 同样存在 get + expire 非原子的竞态，统一改为 Lua 原子续期。
     * 若锁不存在则尝试以 setIfAbsent 重新获取（与原逻辑保持一致，但合并到原子判断中）。
     */
    private static final RedisScript<Long> KEEP_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('pexpire', KEYS[1], ARGV[2]) " +
                    "elseif redis.call('get', KEYS[1]) == false then redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[2]) return 1 else return 0 end",
            Long.class);

    /**
     * redis操作模板。
     */
    private static RedisTemplate<String, Long> longCacheRedisTemplate;

    /**
     * 构造方法，由 Spring 注入 Long RedisTemplate 到 static 字段。
     *
     * @param longCacheRedisTemplate Long 值 RedisTemplate
     */
    public GlobalLocker(RedisTemplate<String, Long> longCacheRedisTemplate) {
        GlobalLocker.longCacheRedisTemplate = longCacheRedisTemplate;
    }

    /**
     * 尝试加锁。
     *
     * @param entityType     entity类型(主要用于构造lockerType)。
     * @param lockerId       锁id。
     * @param lockTimeMillis 锁住时间: 单位为毫秒,根据实际情况置锁,防止死锁
     * @return stamp，>0 表示加锁成功（返回值即为解锁凭证），0 表示加锁失败
     */
    public static long tryLock(Class<?> entityType, Object lockerId, long lockTimeMillis) {
        return tryLock(entityType.getSimpleName(), lockerId, lockTimeMillis);
    }

    /**
     * 尝试加锁。
     *
     * @param lockerType     锁类型，一般可以是类名。
     * @param lockerId       锁id。
     * @param lockTimeMillis 锁住时间: 单位为毫秒,根据实际情况置锁,防死锁，也要防锁不住。
     * @return stamp，>0 表示加锁成功（返回值即为解锁凭证），0 表示加锁失败
     */
    public static long tryLock(String lockerType, Object lockerId, long lockTimeMillis) {
        if (lockerType == null || lockerId == null || lockTimeMillis < 1) {
            return 0;
        }
        // 使用 SnowflakeId 作为 stamp，避免跨 JVM 使用 System.nanoTime() 可能出现的重复值，
        // 保证不同实例、不同持锁时刻的 stamp 全局唯一，CAS 解锁时不会误判。
        long stamp = SnowflakeIdGenerator.getInstance().generateId();
        if (Boolean.TRUE.equals(longCacheRedisTemplate.opsForValue().setIfAbsent(RedisKeyUtils.buildTypeId(REDIS_PREFIX, lockerType, lockerId), stamp, lockTimeMillis,
                TimeUnit.MILLISECONDS))) {
            return stamp;
        } else {
            return 0;
        }
    }

    /**
     * 保持锁定。
     * 如果执行中发现设定锁的时间不足，则可以通过keepLock保持锁。
     *
     * @param entityType     entity类型(主要用于构造lockerType)。
     * @param lockerId       锁id。
     * @param stamp          锁stamp。
     * @param lockTimeMillis 锁住时间: 单位为毫秒,根据实际情况置锁,防止死锁
     * @return true 表示续期成功，false 表示 stamp 不匹配或锁已失效
     */
    public static boolean keepLock(Class<?> entityType, Object lockerId, long stamp, long lockTimeMillis) {
        return keepLock(entityType.getSimpleName(), lockerId, stamp, lockTimeMillis);
    }

    /**
     * 保持锁定。
     *
     * @param lockerType     锁类型，一般可以是类名。
     * @param lockerId       锁id。
     * @param stamp          锁stamp。
     * @param lockTimeMillis 锁住时间: 单位为毫秒,根据实际情况置锁,防止死锁
     * @return true 表示续期成功，false 表示 stamp 不匹配或锁已失效
     */
    public static boolean keepLock(String lockerType, Object lockerId, long stamp, long lockTimeMillis) {
        if (lockerType == null || lockerId == null || stamp < 1 || lockTimeMillis < 1) {
            return false;
        }
        String redisKey = RedisKeyUtils.buildTypeId(REDIS_PREFIX, lockerType, lockerId);
        // 原子 CAS 续期：stamp 匹配则续期，锁不存在则重新获取，否则返回失败。
        Long ret = longCacheRedisTemplate.execute(KEEP_LOCK_SCRIPT, Collections.singletonList(redisKey),
                String.valueOf(stamp), String.valueOf(lockTimeMillis));
        return ret != null && ret > 0;
    }

    /**
     * 解锁。
     *
     * @param entityType entity类型(主要用于构造lockerType)。
     * @param lockerId   锁id。
     * @param stamp      锁stamp。
     * @return true 表示解锁成功（stamp 匹配并删除），false 表示 stamp 不匹配或锁已不存在
     */
    public static boolean unlock(Class<?> entityType, Object lockerId, long stamp) {
        return unlock(entityType.getSimpleName(), lockerId, stamp, false);
    }

    /**
     * 解锁。
     *
     * @param entityType entity类型(主要用于构造lockerType)。
     * @param lockerId   锁id。
     * @param stamp      锁stamp。
     * @param force      是否强制解锁（true 时忽略 stamp 直接删除）。
     * @return true 表示解锁成功，false 表示未删除（stamp 不匹配或锁不存在）
     */
    public static boolean unlock(Class<?> entityType, Object lockerId, long stamp, boolean force) {
        return unlock(entityType.getSimpleName(), lockerId, stamp, force);
    }

    /**
     * 解锁。
     *
     * @param lockerType 锁类型，一般可以是类名。
     * @param lockerId   锁id。
     * @param stamp      锁stamp。
     * @return true 表示解锁成功（stamp 匹配并删除），false 表示 stamp 不匹配或锁已不存在
     */
    public static boolean unlock(String lockerType, Object lockerId, long stamp) {
        return unlock(lockerType, lockerId, stamp, false);
    }

    /**
     * 解锁。
     *
     * @param lockerType 锁类型，一般可以是类名。
     * @param lockerId   锁id。
     * @param stamp      锁stamp。
     * @param force      强制解锁。
     * @return true 表示解锁成功，false 表示未删除
     */
    public static boolean unlock(String lockerType, Object lockerId, long stamp, boolean force) {
        if (lockerType == null || lockerId == null || (stamp < 1 && !force)) {
            return false;
        }
        String redisKey = RedisKeyUtils.buildTypeId(REDIS_PREFIX, lockerType, lockerId);
        if (force) {
            return Boolean.TRUE.equals(longCacheRedisTemplate.delete(redisKey));
        }
        // 原子 CAS 解锁：只有 stamp 匹配当前持有者才删除，避免误删其他实例抢占后的锁。
        Long ret = longCacheRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(redisKey), String.valueOf(stamp));
        return ret != null && ret > 0;
    }


    /**
     * 获取缓存中的所有key。
     *
     * @param entityType 缓存对象类(主要用于构造cacheName)
     * @param keyPrefix  key前缀，请注意key最后不用加"*"
     * @return key集合
     */
    public static Set<String> keys(Class<?> entityType, String keyPrefix) {
        return keys(entityType.getSimpleName(), keyPrefix);
    }

    /**
     * 获取缓存中的所有key。
     *
     * @param lockerType 缓存名
     * @param keyPrefix  key前缀，请注意key最后不用加"*",全部清除用*即可。
     * @return key集合
     */
    public static Set<String> keys(String lockerType, String keyPrefix) {
        if (StringUtils.isBlank(keyPrefix)) {
            keyPrefix = "*";
        } else {
            keyPrefix = keyPrefix + "*";
        }
        int redisPrefixLength = REDIS_PREFIX.length() + lockerType.length() + 1;
        String pattern = RedisKeyUtils.buildTypeId(REDIS_PREFIX, lockerType, keyPrefix);
        Set<String> keys = new HashSet<>();
        try (Cursor<String> cursor = longCacheRedisTemplate.scan(ScanOptions.scanOptions().match(pattern).count(1000).build())) {
            cursor.forEachRemaining(key -> {
                keys.add(key.substring(redisPrefixLength));
            });
        }
        return keys;
    }

}
