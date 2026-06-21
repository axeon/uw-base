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
import java.util.List;
import java.util.Set;

/**
 * 基于 Redis Set 实现的全局集合。
 * <p>
 * 元素经 Kryo 序列化为 byte[] 后存入 Redis Set，{@code add/remove/contains} 三者序列化协议一致。
 * Redis key 格式：{@code uw-set:{setName}}。value 由 dataCacheRedisTemplate（byte[]）承载。
 */
public class GlobalHashSet {

    private static final Logger log = LoggerFactory.getLogger(GlobalHashSet.class);

    /**
     * redis前缀。
     */
    private static final String REDIS_PREFIX = "uw-set:";

    /**
     * redis操作模板。
     */
    private static RedisTemplate<String, byte[]> dataCacheRedisTemplate;

    /**
     * 构造方法，由 Spring 注入 byte[] RedisTemplate 到 static 字段。
     *
     * @param dataCacheRedisTemplate byte[] 值 RedisTemplate
     */
    public GlobalHashSet(RedisTemplate<String, byte[]> dataCacheRedisTemplate) {
        GlobalHashSet.dataCacheRedisTemplate = dataCacheRedisTemplate;
    }

    /**
     * 向集合中添加对象。
     *
     * @param setName Set名。
     * @param item    元素对象，null 或 setName 为 null 时返回 false。
     * @return true 表示新增成功，false 表示已存在或参数非法
     */
    public static boolean add(String setName, Object item) {
        if (setName == null || item == null) {
            return false;
        }
        return Boolean.TRUE.equals(dataCacheRedisTemplate.opsForSet().add(REDIS_PREFIX + setName, KryoUtils.serialize(item)));
    }

    /**
     * 获取集合大小。
     *
     * @param setName Set名。
     * @return 集合大小，setName 为 null 或 Redis 异常返回 -1
     */
    public static long size(String setName) {
        if (setName == null) {
            return -1L;
        }
        Long ret = dataCacheRedisTemplate.opsForSet().size(REDIS_PREFIX + setName);
        if (ret != null) {
            return ret;
        } else {
            return -1L;
        }
    }

    /**
     * 从集合中移除对象。
     *
     * @param setName Set名。
     * @param item    元素对象，null 或 setName 为 null 时返回 -1。
     * @return 删除数量（0 表示不存在），参数非法或 Redis 异常返回 -1
     */
    public static <T> long remove(String setName, T item) {
        if (setName == null || item == null) {
            return -1L;
        }
        Long ret = dataCacheRedisTemplate.opsForSet().remove(REDIS_PREFIX + setName, KryoUtils.serialize(item));
        if (ret != null) {
            return ret;
        } else {
            return -1L;
        }
    }

    /**
     * 判断集合中是否包含指定对象。
     *
     * @param setName Set名。
     * @param item    对象，null 或 setName 为 null 时返回 false。
     * @return true 表示包含，false 表示不包含或参数非法
     */
    public static boolean contains(String setName, Object item) {
        if (setName == null || item == null) {
            return false;
        }
        return Boolean.TRUE.equals(dataCacheRedisTemplate.opsForSet().isMember(REDIS_PREFIX + setName, KryoUtils.serialize(item)));
    }

    /**
     * 随机获取一个元素（不删除）。
     *
     * @param setName   Set名。
     * @param itemClazz 元素类型
     * @return 随机元素，集合为空或 setName 为 null 时返回 null
     */
    public static <T> T random(String setName, Class<T> itemClazz) {
        if (setName == null) {
            return null;
        }
        return KryoUtils.deserialize(dataCacheRedisTemplate.opsForSet().randomMember(REDIS_PREFIX + setName), itemClazz);
    }

    /**
     * 随机获取指定数量的元素（不删除）。
     *
     * @param setName   Set名。
     * @param count     获取数量
     * @param itemClazz 元素类型
     * @return 随机元素集合，集合为空或 setName 为 null 时返回空集
     */
    public static <T> Set<T> random(String setName, long count, Class<T> itemClazz) {
        if (setName == null) {
            return null;
        }
        List<byte[]> byteSet = dataCacheRedisTemplate.opsForSet().randomMembers(REDIS_PREFIX + setName, count);
        if (byteSet == null) {
            return new HashSet<>();
        }
        HashSet<T> dataSet = new HashSet<T>((int) (byteSet.size() * 1.5f));
        for (byte[] item : byteSet) {
            //逐条反序列化，脏数据（旧协议/损坏，KryoUtils直接抛KryoException）跳过并降级WARN，
            //避免单条脏数据导致整个批量结果失败（遵循KryoUtils异常策略：由调用方catch处理，不泄露kryo细节对外）。
            try {
                dataSet.add(KryoUtils.deserialize(item, itemClazz));
            } catch (Exception e) {
                log.warn("!!![{}] GlobalHashSet.random deserialize failed, skipped (setName={}, len={}): {}",
                        REDIS_PREFIX, setName, item.length, e.toString());
            }
        }
        return dataSet;
    }

    /**
     * 随机弹出（删除并返回）一个元素。
     *
     * @param setName   Set名。
     * @param itemClazz 元素类型
     * @return 被弹出的元素，集合为空或 setName 为 null 时返回 null
     */
    public static <T> T pop(String setName, Class<T> itemClazz) {
        if (setName == null) {
            return null;
        }
        return KryoUtils.deserialize(dataCacheRedisTemplate.opsForSet().pop(REDIS_PREFIX + setName), itemClazz);
    }

    /**
     * 随机弹出（删除并返回）指定数量的元素。
     *
     * @param setName   Set名。
     * @param count     弹出数量
     * @param itemClazz 元素类型
     * @return 被弹出的元素集合，集合为空或 setName 为 null 时返回空集
     */
    public static <T> Set<T> pop(String setName, long count, Class<T> itemClazz) {
        if (setName == null) {
            return null;
        }
        List<byte[]> byteSet = dataCacheRedisTemplate.opsForSet().pop(REDIS_PREFIX + setName, count);
        if (byteSet == null) {
            return new HashSet<>();
        }
        HashSet<T> dataSet = new HashSet<T>((int) (byteSet.size() * 1.5f));
        for (byte[] item : byteSet) {
            //逐条反序列化，脏数据（旧协议/损坏，KryoUtils直接抛KryoException）跳过并降级WARN，
            //避免单条脏数据导致整个批量结果失败（遵循KryoUtils异常策略：由调用方catch处理，不泄露kryo细节对外）。
            try {
                dataSet.add(KryoUtils.deserialize(item, itemClazz));
            } catch (Exception e) {
                log.warn("!!![{}] GlobalHashSet.pop deserialize failed, skipped (setName={}, len={}): {}",
                        REDIS_PREFIX, setName, item.length, e.toString());
            }
        }
        return dataSet;
    }

    /**
     * 列出集合中的全部元素。
     *
     * @param setName   Set名。
     * @param itemClazz 元素类型
     * @return 全部元素集合，集合为空或 setName 为 null 时返回空集
     */
    public static <T> Set<T> list(String setName, Class<T> itemClazz) {
        if (setName == null) {
            return null;
        }
        Set<byte[]> byteSet = dataCacheRedisTemplate.opsForSet().members(REDIS_PREFIX + setName);
        if (byteSet == null) {
            return new HashSet<>();
        }
        HashSet<T> dataSet = new HashSet<T>((int) (byteSet.size() * 1.5f));
        for (byte[] item : byteSet) {
            //逐条反序列化，脏数据（旧协议/损坏，KryoUtils直接抛KryoException）跳过并降级WARN，
            //避免单条脏数据导致整个批量结果失败（遵循KryoUtils异常策略：由调用方catch处理，不泄露kryo细节对外）。
            try {
                dataSet.add(KryoUtils.deserialize(item, itemClazz));
            } catch (Exception e) {
                log.warn("!!![{}] GlobalHashSet.list deserialize failed, skipped (setName={}, len={}): {}",
                        REDIS_PREFIX, setName, item.length, e.toString());
            }
        }
        return dataSet;
    }

    /**
     * 删除整个 Set。
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
     * @param setName   集合名
     * @param keyPrefix key前缀，请注意key最后不用加"*",全部清除用*即可。
     * @return key集合
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
