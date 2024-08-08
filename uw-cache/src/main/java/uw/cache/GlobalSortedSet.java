package uw.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import uw.cache.util.KryoUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 基于Redis实现的SortedSet。
 * 可以用于类似延迟队列的实现。
 */
public class GlobalSortedSet {

    private static final Logger log = LoggerFactory.getLogger( GlobalSortedSet.class );

    /**
     * redis前缀。
     */
    private static final String REDIS_PREFIX = "uw-zset:";

    /**
     * redis操作模板。
     */
    private static RedisTemplate<String, byte[]> dataCacheRedisTemplate;

    public GlobalSortedSet(RedisTemplate<String, byte[]> dataCacheRedisTemplate) {
        GlobalSortedSet.dataCacheRedisTemplate = dataCacheRedisTemplate;
    }

    /**
     * 向SortedSet中增加对象。
     *
     * @param setName   Set名。
     * @param itemData  队列对象。
     * @param itemScore 队列分数，一般为时间戳。
     * @return boolean 是否成功
     */
    public static boolean add(String setName, Object itemData, double itemScore) {
        if (setName == null || itemData == null) {
            return false;
        }
        return Boolean.TRUE.equals( dataCacheRedisTemplate.opsForZSet().add( REDIS_PREFIX + setName, KryoUtils.serialize( itemData ), itemScore ) );
    }

    /**
     * 统计SortedSet大小。
     *
     * @param setName Set名。
     * @return long 删除数量。
     */
    public static long size(String setName) {
        if (setName == null) {
            return -1L;
        }
        Long ret = dataCacheRedisTemplate.opsForZSet().size( REDIS_PREFIX + setName );
        if (ret != null) {
            return ret;
        } else {
            return -1L;
        }
    }

    /**
     * 从SortedSet中移除对象。
     *
     * @param setName   Set名。
     * @param itemData 队列对象。
     * @return long 删除数量。
     */
    public static <T> long remove(String setName, T itemData) {
        if (setName == null || itemData == null) {
            return -1L;
        }
        Long ret = dataCacheRedisTemplate.opsForZSet().remove( REDIS_PREFIX + setName, KryoUtils.serialize( itemData ) );
        if (ret != null) {
            return ret;
        } else {
            return -1L;
        }
    }

    /**
     * 从SortedSet中移除对象。
     *
     * @param setName    Set名。
     * @param itemDatas 队列对象。
     * @return long 删除数量。
     */
    public static long remove(String setName, Object... itemDatas) {
        if (setName == null || itemDatas == null) {
            return -1L;
        }
        Object[] dataArray = new Object[itemDatas.length];
        for (int i = 0; i < itemDatas.length; i++) {
            dataArray[i] = KryoUtils.serialize( itemDatas[i] );
        }
        Long ret = dataCacheRedisTemplate.opsForZSet().remove( REDIS_PREFIX + setName, dataArray );
        if (ret != null) {
            return ret;
        } else {
            return -1L;
        }
    }

    /**
     * 从SortedSet中根据Score批量移除对象。
     *
     * @param setName  Set名。
     * @param scoreMin 最小分。
     * @param scoreMax 最大分。
     * @return long 删除数量。
     */
    public static long removeRangeByScore(String setName, double scoreMin, double scoreMax) {
        if (setName == null) {
            return -1L;
        }
        Long ret = dataCacheRedisTemplate.opsForZSet().removeRangeByScore( REDIS_PREFIX + setName, scoreMin, scoreMax );
        if (ret != null) {
            return ret;
        } else {
            return -1L;
        }
    }

    /**
     * 根据Score队列列表。
     *
     * @param setName  Set名。
     * @param scoreMin 最小分。
     * @param scoreMax 最大分。
     * @return 是否解锁成功
     */
    public static <T> Set<T> listRangeByScore(String setName, Class<T> itemClazz, double scoreMin, double scoreMax) {
        if (setName == null) {
            return null;
        }
        Set<byte[]> byteSet = dataCacheRedisTemplate.opsForZSet().rangeByScore( REDIS_PREFIX + setName, scoreMin, scoreMax );
        HashSet<T> dataSet = new HashSet<T>( (int) (byteSet.size() * 1.5f) );
        for (byte[] item : byteSet) {
            dataSet.add( KryoUtils.deserialize( item, itemClazz ) );
        }
        return dataSet;
    }

    /**
     * 删除SortedSet。
     *
     * @param setName Set名。
     * @return 是否解锁成功
     */
    public static boolean delete(String setName) {
        if (setName == null) {
            return false;
        }
        return Boolean.TRUE.equals( dataCacheRedisTemplate.delete( REDIS_PREFIX + setName ) );
    }

}
