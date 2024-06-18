package uw.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import uw.cache.util.RedisKeyUtils;

import java.util.concurrent.TimeUnit;

/**
 * 基于Redis实现的全局锁。
 */
public class GlobalLocker {

    private static final Logger log = LoggerFactory.getLogger( GlobalLocker.class );

    /**
     * redis前缀。
     */
    private static final String REDIS_PREFIX = "uw-locker:";

    /**
     * redis操作模板。
     */
    private static RedisTemplate<String, Long> longCacheRedisTemplate;

    public GlobalLocker(RedisTemplate<String, Long> longCacheRedisTemplate) {
        GlobalLocker.longCacheRedisTemplate = longCacheRedisTemplate;
    }

    /**
     * 尝试加锁。
     *
     * @param entityType     entity类型(主要用于构造lockerType)。
     * @param lockerId       锁id。
     * @param lockTimeMillis 锁住时间: 单位为毫秒,根据实际情况置锁,防止死锁
     * @return stamp 锁stamp，如果stamp>0则锁定成功。
     */
    public static long tryLock(Class entityType, Object lockerId, long lockTimeMillis) {
        return tryLock( entityType.getSimpleName(), lockerId, lockTimeMillis );
    }

    /**
     * 尝试加锁。
     *
     * @param lockerType     锁类型，一般可以是类名。
     * @param lockerId       锁id。
     * @param lockTimeMillis 锁住时间: 单位为毫秒,根据实际情况置锁,防死锁，也要防锁不住。
     * @return stamp，锁stamp，如果stamp>0则锁定成功。
     */
    public static long tryLock(String lockerType, Object lockerId, long lockTimeMillis) {
        if (lockerType == null || lockerId == null || lockTimeMillis < 1) {
            return 0;
        }
        long stamp = System.nanoTime();
        if (Boolean.TRUE.equals( longCacheRedisTemplate.opsForValue().setIfAbsent( RedisKeyUtils.buildTypeId( REDIS_PREFIX, lockerType, lockerId ), stamp, lockTimeMillis,
                TimeUnit.MILLISECONDS ) )) {
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
     * @return 是否锁定成功
     */
    public static boolean keepLock(Class entityType, Object lockerId, long stamp, long lockTimeMillis) {
        return keepLock( entityType.getSimpleName(), lockerId, stamp, lockTimeMillis );
    }

    /**
     * 保持锁定。
     *
     * @param lockerType     锁类型，一般可以是类名。
     * @param lockerId       锁id。
     * @param stamp          锁stamp。
     * @param lockTimeMillis 锁住时间: 单位为毫秒,根据实际情况置锁,防止死锁
     * @return 是否锁定成功
     */
    public static boolean keepLock(String lockerType, Object lockerId, long stamp, long lockTimeMillis) {
        if (lockerType == null || lockerId == null || stamp < 1) {
            return false;
        }
        String redisKey = RedisKeyUtils.buildTypeId( REDIS_PREFIX, lockerType, lockerId );
        Long data = longCacheRedisTemplate.opsForValue().get( redisKey );
        if (data == null) {
            return Boolean.TRUE.equals( longCacheRedisTemplate.opsForValue().setIfAbsent( redisKey, stamp, lockTimeMillis, TimeUnit.MILLISECONDS ) );
        } else {
            if (data != stamp) {
                return false;
            } else {
                return Boolean.TRUE.equals( longCacheRedisTemplate.expire( redisKey, lockTimeMillis, TimeUnit.MILLISECONDS ) );
            }
        }
    }

    /**
     * 解锁。
     *
     * @param entityType entity类型(主要用于构造lockerType)。
     * @param lockerId   锁id。
     * @param stamp      锁stamp。
     * @return 是否解锁成功
     */
    public static boolean unlock(Class entityType, Object lockerId, long stamp) {
        return unlock( entityType.getSimpleName(), lockerId, stamp, false );
    }

    /**
     * 解锁。
     *
     * @param entityType entity类型(主要用于构造lockerType)。
     * @param lockerId   锁id。
     * @param stamp      锁stamp。
     * @return 是否解锁成功
     */
    public static boolean unlock(Class entityType, Object lockerId, long stamp, boolean force) {
        return unlock( entityType.getSimpleName(), lockerId, stamp, force );
    }

    /**
     * 解锁。
     *
     * @param lockerType 锁类型，一般可以是类名。
     * @param lockerId   锁id。
     * @param stamp      锁stamp。
     * @return 是否解锁成功
     */
    public static boolean unlock(String lockerType, Object lockerId, long stamp) {
        return unlock( lockerType, lockerId, stamp, false );
    }

    /**
     * 解锁。
     *
     * @param lockerType 锁类型，一般可以是类名。
     * @param lockerId   锁id。
     * @param stamp      锁stamp。
     * @param force      强制解锁。
     * @return 是否解锁成功
     */
    public static boolean unlock(String lockerType, Object lockerId, long stamp, boolean force) {
        if (lockerType == null || lockerId == null || (stamp < 1 && !force)) {
            return false;
        }
        String redisKey = RedisKeyUtils.buildTypeId( REDIS_PREFIX, lockerType, lockerId );
        if (!force) {
            Long data = longCacheRedisTemplate.opsForValue().get( redisKey );
            if (data == null || data != stamp) {
                return false;
            }
        }
        return Boolean.TRUE.equals( longCacheRedisTemplate.delete( redisKey ) );
    }

}
