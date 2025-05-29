package uw.cache;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import uw.cache.util.KryoUtils;
import uw.cache.util.RedisKeyUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 基于Redis实现的HashSet。
 */
public class GlobalHashSet {

    private static final Logger log = LoggerFactory.getLogger( GlobalHashSet.class );

    /**
     * redis前缀。
     */
    private static final String REDIS_PREFIX = "uw-set:";

    /**
     * redis操作模板。
     */
    private static RedisTemplate<String, byte[]> dataCacheRedisTemplate;

    public GlobalHashSet(RedisTemplate<String, byte[]> dataCacheRedisTemplate) {
        GlobalHashSet.dataCacheRedisTemplate = dataCacheRedisTemplate;
    }

    /**
     * 向集合中增加对象。
     *
     * @param setName Set名。
     * @param item    队列对象。
     * @return boolean 是否成功
     */
    public static boolean add(String setName, Object item) {
        if (setName == null || item == null) {
            return false;
        }
        return Boolean.TRUE.equals( dataCacheRedisTemplate.opsForSet().add( REDIS_PREFIX + setName, KryoUtils.serialize( item ) ) );
    }

    /**
     * 从队列中移除对象。
     *
     * @param setName Set名。
     * @return long 删除数量。
     */
    public static long size(String setName) {
        if (setName == null) {
            return -1L;
        }
        Long ret = dataCacheRedisTemplate.opsForSet().size( REDIS_PREFIX + setName );
        if (ret != null) {
            return ret;
        } else {
            return -1L;
        }
    }

    /**
     * 从队列中移除对象。
     *
     * @param setName Set名。
     * @param item    对象。
     * @return long 删除数量。
     */
    public static <T> long remove(String setName, T item) {
        if (setName == null || item == null) {
            return -1L;
        }
        Long ret = dataCacheRedisTemplate.opsForSet().remove( REDIS_PREFIX + setName, KryoUtils.serialize( item ) );
        if (ret != null) {
            return ret;
        } else {
            return -1L;
        }
    }

    /**
     * 从队列中根据Score批量移除对象。
     *
     * @param setName Set名。
     * @param item    最小分。
     * @return long 删除数量。
     */
    public static boolean contains(String setName, Object item) {
        if (setName == null) {
            return false;
        }
        return Boolean.TRUE.equals( dataCacheRedisTemplate.opsForSet().isMember( REDIS_PREFIX + setName, item ) );
    }

    /**
     * 随机获取一个元素。
     *
     * @param setName Set名。
     * @return 是否解锁成功
     */
    public static <T> T random(String setName, Class<T> itemClazz) {
        if (setName == null) {
            return null;
        }
        return KryoUtils.deserialize( dataCacheRedisTemplate.opsForSet().randomMember( REDIS_PREFIX + setName ), itemClazz );
    }

    /**
     * 随机获取指定数量的元素。
     *
     * @param setName Set名。
     * @return 是否解锁成功
     */
    public static <T> Set<T> random(String setName, long count, Class<T> itemClazz) {
        if (setName == null) {
            return null;
        }
        List<byte[]> byteSet =  dataCacheRedisTemplate.opsForSet().randomMembers( REDIS_PREFIX + setName, count );
        HashSet<T> dataSet = new HashSet<T>((int)(byteSet.size()*1.5f));
        for ( byte[] item : byteSet ) {
            dataSet.add( KryoUtils.deserialize( item, itemClazz ) );
        }
        return dataSet;
    }

    /**
     * 随机删除后获取一个元素。
     *
     * @param setName Set名。
     * @return 是否解锁成功
     */
    public static <T> T pop(String setName, Class<T> itemClazz) {
        if (setName == null) {
            return null;
        }
        return KryoUtils.deserialize( dataCacheRedisTemplate.opsForSet().pop( REDIS_PREFIX + setName ), itemClazz );
    }

    /**
     * 随机删除后获取指定数量元素。
     *
     * @param setName Set名。
     * @return 是否解锁成功
     */
    public static <T> Set<T> pop(String setName, long count, Class<T> itemClazz) {
        if (setName == null) {
            return null;
        }
        List<byte[]> byteSet =  dataCacheRedisTemplate.opsForSet().pop( REDIS_PREFIX + setName, count );
        HashSet<T> dataSet = new HashSet<T>((int)(byteSet.size()*1.5f));
        for ( byte[] item : byteSet ) {
            dataSet.add( KryoUtils.deserialize( item, itemClazz ) );
        }
        return dataSet;
    }

    /**
     * 列表。
     *
     * @param setName Set名。
     * @return 是否解锁成功
     */
    public static <T> Set<T> list(String setName, Class<T> itemClazz) {
        if (setName == null) {
            return null;
        }
        Set<byte[]> byteSet =  dataCacheRedisTemplate.opsForSet().members( REDIS_PREFIX + setName );
        HashSet<T> dataSet = new HashSet<T>((int)(byteSet.size()*1.5f));
        for ( byte[] item : byteSet ) {
            dataSet.add( KryoUtils.deserialize( item, itemClazz ) );
        }
        return dataSet;
    }

    /**
     * 删除Set。
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

    /**
     * 获取缓存中的所有key。
     *
     * @param setName 集合名
     * @param keyPrefix key前缀，请注意key最后不用加"*",全部清除用*即可。
     * @return
     */
    public static Set<String> keys(String setName, String keyPrefix) {
        if (StringUtils.isBlank(keyPrefix)) {
            keyPrefix = "*";
        } else {
            keyPrefix = keyPrefix + "*";
        }
        int redisPrefixLength = REDIS_PREFIX.length() + setName.length() + 1;
        String pattern = RedisKeyUtils.buildTypeId(REDIS_PREFIX, setName, keyPrefix);
        Set<String> keys = new HashSet<>();
        try (Cursor<String> cursor = dataCacheRedisTemplate.scan(ScanOptions.scanOptions().match(pattern).count(1000).build())) {
            cursor.forEachRemaining(key -> {
                keys.add(key.substring(redisPrefixLength));
            });
        }
        return keys;
    }

}
