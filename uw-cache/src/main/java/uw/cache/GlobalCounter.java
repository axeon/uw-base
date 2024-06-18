package uw.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import uw.cache.util.RedisKeyUtils;

/**
 * 基于Redis实现的全局计数器。
 */
public class GlobalCounter {

    private static final Logger log = LoggerFactory.getLogger( GlobalCounter.class );

    /**
     * redis前缀。
     */
    private static final String REDIS_PREFIX = "uw-counter:";

    /**
     * redis操作模板。
     */
    private static RedisTemplate<String, Long> longCacheRedisTemplate;


    public GlobalCounter(RedisTemplate<String, Long> longCacheRedisTemplate) {
        GlobalCounter.longCacheRedisTemplate = longCacheRedisTemplate;
    }

    /**
     * 增加计数。
     *
     * @param entityType   entity类型(主要用于构造counterType)。
     * @param counterId    计数器ID
     * @param incrementNum 增加的计数。
     * @return 计数数值
     */
    public static long increment(Class entityType, Object counterId, long incrementNum) {
        return increment( entityType.getSimpleName(), counterId, incrementNum );
    }

    /**
     * 增加计数。
     *
     * @param counterType  计数器类型
     * @param counterId    计数器ID
     * @param incrementNum 增加的计数。
     * @return 计数数值
     */
    public static long increment(String counterType, Object counterId, long incrementNum) {
        return longCacheRedisTemplate.opsForValue().increment( RedisKeyUtils.buildTypeId( REDIS_PREFIX, counterType, counterId ), incrementNum );
    }

    /**
     * 减少计数。
     *
     * @param entityType   entity类型(主要用于构造counterType)。
     * @param counterId    计数器ID
     * @param decrementNum 减少的计数。
     * @return 计数数值
     */
    public static long decrement(Class entityType, Object counterId, long decrementNum) {
        return decrement( entityType.getSimpleName(), counterId, decrementNum );
    }

    /**
     * 减少计数。
     *
     * @param counterType  计数器类型
     * @param counterId    计数器ID
     * @param decrementNum 减少的计数。
     * @return 计数数值
     */
    public static long decrement(String counterType, Object counterId, long decrementNum) {
        return longCacheRedisTemplate.opsForValue().decrement( RedisKeyUtils.buildTypeId( REDIS_PREFIX, counterType, counterId ), decrementNum );
    }

    /**
     * 设置计数器数值。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @param num        数值
     */
    public static void set(Class entityType, Object counterId, long num) {
        set( entityType.getSimpleName(), counterId, num );
    }

    /**
     * 设置计数器数值。
     *
     * @param counterType 计数器类型
     * @param counterId   计数器ID
     * @param num         数值
     */
    public static void set(String counterType, Object counterId, long num) {
        longCacheRedisTemplate.opsForValue().set( RedisKeyUtils.buildTypeId( REDIS_PREFIX, counterType, counterId ), num );
    }

    /**
     * 如果没有，则设置计数器数值。
     * 一般用于设置初始数值。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @param num        数值
     */
    public static void setIfAbsent(Class entityType, Object counterId, long num) {
        setIfAbsent( entityType.getSimpleName(), counterId, num );
    }

    /**
     * 如果没有，则设置计数器数值。
     * 一般用于设置初始数值。
     *
     * @param counterType 计数器类型
     * @param counterId   计数器ID
     * @param num         数值
     */
    public static boolean setIfAbsent(String counterType, Object counterId, long num) {
        return Boolean.TRUE.equals( longCacheRedisTemplate.opsForValue().setIfAbsent( RedisKeyUtils.buildTypeId( REDIS_PREFIX, counterType, counterId ), num ) );
    }

    /**
     * 获得计数器数值。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @return 计数数值
     */
    public static long get(Class entityType, Object counterId) {
        return get( entityType.getSimpleName(), counterId );
    }

    /**
     * 获得计数器数值。
     *
     * @param counterType 计数器类型
     * @param counterId   计数器ID
     * @return 计数数值
     */
    public static long get(String counterType, Object counterId) {
        Long count = longCacheRedisTemplate.opsForValue().get( RedisKeyUtils.buildTypeId( REDIS_PREFIX, counterType, counterId ) );
        return count != null ? count : 0;
    }

    /**
     * 删除计数器。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @return 是否成功
     */
    public static boolean delete(Class entityType, Object counterId) {
        return delete( entityType.getSimpleName(), counterId );
    }

    /**
     * 删除计数器。
     *
     * @param counterType 计数器类型
     * @param counterId   计数器ID
     * @return 是否成功
     */
    public static boolean delete(String counterType, Object counterId) {
        return Boolean.TRUE.equals( longCacheRedisTemplate.delete( RedisKeyUtils.buildTypeId( REDIS_PREFIX, counterType, counterId ) ) );
    }

    /**
     * 获取数值后删除计数器。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @return 计数数值
     */
    public static long getAndDelete(Class entityType, Object counterId) {
        return getAndDelete( entityType.getSimpleName(), counterId );
    }

    /**
     * 获取数值后删除计数器。
     *
     * @param counterType 计数器类型
     * @param counterId   计数器ID
     * @return 计数数值
     */
    public static long getAndDelete(String counterType, Object counterId) {
        Long count = longCacheRedisTemplate.opsForValue().getAndDelete( RedisKeyUtils.buildTypeId( REDIS_PREFIX, counterType, counterId ) );
        return count != null ? count : 0;
    }

}
