package uw.cache;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import uw.common.util.KryoUtils;
import uw.cache.util.RedisKeyUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 基于 Redis ZSet 实现的全局有序集合。
 * <p>
 * 可用于类似延迟队列/定时触发的实现：score 通常为时间戳，按 score 范围查询和删除到期元素。
 * 元素经 Kryo 序列化为 byte[]，value 由 dataCacheRedisTemplate 承载。
 */
public class GlobalSortedSet {

    private static final Logger log = LoggerFactory.getLogger(GlobalSortedSet.class);

    /**
     * redis前缀。
     */
    private static final String REDIS_PREFIX = "uw-zset:";

    /**
     * redis操作模板。
     */
    private static RedisTemplate<String, byte[]> dataCacheRedisTemplate;

    /**
     * 构造方法，由 Spring 注入 byte[] RedisTemplate 到 static 字段。
     *
     * @param dataCacheRedisTemplate byte[] 值 RedisTemplate
     */
    public GlobalSortedSet(RedisTemplate<String, byte[]> dataCacheRedisTemplate) {
        GlobalSortedSet.dataCacheRedisTemplate = dataCacheRedisTemplate;
    }

    /**
     * 向 SortedSet 中添加对象（若已存在则更新 score）。
     *
     * @param setName   Set名。
     * @param itemData  元素对象，null 或 setName 为 null 时返回 false。
     * @param itemScore 元素分数，一般为时间戳。
     * @return true 表示新增成功，false 表示已存在（仅更新 score）或参数非法
     */
    public static boolean add(String setName, Object itemData, double itemScore) {
        if (setName == null || itemData == null) {
            return false;
        }
        return Boolean.TRUE.equals(dataCacheRedisTemplate.opsForZSet().add(REDIS_PREFIX + setName, KryoUtils.serialize(itemData), itemScore));
    }

    /**
     * 统计 SortedSet 大小。
     *
     * @param setName Set名。
     * @return 集合大小，setName 为 null 或 Redis 异常返回 -1
     */
    public static long size(String setName) {
        if (setName == null) {
            return -1L;
        }
        Long ret = dataCacheRedisTemplate.opsForZSet().size(REDIS_PREFIX + setName);
        if (ret != null) {
            return ret;
        } else {
            return -1L;
        }
    }

    /**
     * 从 SortedSet 中移除指定对象。
     *
     * @param setName  Set名。
     * @param itemData 元素对象，null 或 setName 为 null 时返回 -1。
     * @return 删除数量（0 表示不存在），参数非法或 Redis 异常返回 -1
     */
    public static <T> long remove(String setName, T itemData) {
        if (setName == null || itemData == null) {
            return -1L;
        }
        Long ret = dataCacheRedisTemplate.opsForZSet().remove(REDIS_PREFIX + setName, KryoUtils.serialize(itemData));
        if (ret != null) {
            return ret;
        } else {
            return -1L;
        }
    }

    /**
     * 从 SortedSet 中批量移除对象。
     *
     * @param setName   Set名。
     * @param itemDatas 元素对象数组，null 或 setName 为 null 时返回 -1。
     * @return 删除数量，参数非法或 Redis 异常返回 -1
     */
    public static long remove(String setName, Object... itemDatas) {
        if (setName == null || itemDatas == null) {
            return -1L;
        }
        Object[] dataArray = new Object[itemDatas.length];
        for (int i = 0; i < itemDatas.length; i++) {
            dataArray[i] = KryoUtils.serialize(itemDatas[i]);
        }
        Long ret = dataCacheRedisTemplate.opsForZSet().remove(REDIS_PREFIX + setName, dataArray);
        if (ret != null) {
            return ret;
        } else {
            return -1L;
        }
    }

    /**
     * 按分数范围批量移除对象。
     *
     * @param setName  Set名。
     * @param scoreMin 最小分（含）。
     * @param scoreMax 最大分（含）。
     * @return 删除数量，setName 为 null 或 Redis 异常返回 -1
     */
    public static long removeRangeByScore(String setName, double scoreMin, double scoreMax) {
        if (setName == null) {
            return -1L;
        }
        Long ret = dataCacheRedisTemplate.opsForZSet().removeRangeByScore(REDIS_PREFIX + setName, scoreMin, scoreMax);
        if (ret != null) {
            return ret;
        } else {
            return -1L;
        }
    }

    /**
     * 按分数范围查询元素列表（升序）。
     *
     * @param setName   Set名。
     * @param itemClazz 元素类型
     * @param scoreMin  最小分（含）。
     * @param scoreMax  最大分（含）。
     * @return 命中元素集合，setName 为 null 或集合为空时返回空集/ null
     */
    public static <T> Set<T> listRangeByScore(String setName, Class<T> itemClazz, double scoreMin, double scoreMax) {
        if (setName == null) {
            return null;
        }
        Set<byte[]> byteSet = dataCacheRedisTemplate.opsForZSet().rangeByScore(REDIS_PREFIX + setName, scoreMin, scoreMax);
        if (byteSet == null) {
            return null;
        }
        HashSet<T> dataSet = new HashSet<>((int) (byteSet.size() * 1.5f));
        for (byte[] item : byteSet) {
            //逐条反序列化，脏数据（旧协议/损坏，KryoUtils直接抛KryoException）跳过并降级WARN，
            //避免单条脏数据导致整个批量结果失败（遵循KryoUtils异常策略：由调用方catch处理，不泄露kryo细节对外）。
            try {
                dataSet.add(KryoUtils.deserialize(item, itemClazz));
            } catch (Exception e) {
                log.warn("!!![{}] GlobalSortedSet.listRangeByScore deserialize failed, skipped (setName={}, len={}): {}",
                        REDIS_PREFIX, setName, item.length, e.toString());
            }
        }
        return dataSet;
    }

    /**
     * 删除整个 SortedSet。
     *
     * @param setName Set名。
     * @return true 表示删除成功，false 表示 key 不存在或 setName 为 null
     */
    public static boolean delete(String setName) {
        if (setName == null) {
            return false;
        }
        return Boolean.TRUE.equals(dataCacheRedisTemplate.delete(REDIS_PREFIX + setName));
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
     * @param setType   集合名
     * @param keyPrefix key前缀，请注意key最后不用加"*",全部清除用*即可。
     * @return key集合
     */
    public static Set<String> keys(String setType, String keyPrefix) {
        if (StringUtils.isBlank(keyPrefix)) {
            keyPrefix = "*";
        } else {
            keyPrefix = keyPrefix + "*";
        }
        int redisPrefixLength = REDIS_PREFIX.length() + setType.length() + 1;
        String pattern = RedisKeyUtils.buildTypeId(REDIS_PREFIX, setType, keyPrefix);
        Set<String> keys = new HashSet<>();
        try (Cursor<String> cursor = dataCacheRedisTemplate.scan(ScanOptions.scanOptions().match(pattern).count(1000).build())) {
            cursor.forEachRemaining(key -> {
                keys.add(key.substring(redisPrefixLength));
            });
        }
        return keys;
    }

}
