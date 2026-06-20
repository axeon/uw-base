package uw.cache.util;

import org.apache.commons.lang3.StringUtils;

/**
 * RedisKey的工具类。
 * <p>
 * 用于拼接各组件的 Redis 主键，统一分隔符 {@link #KEY_SPLITTER}（":"），
 * 保证 {@code 前缀:类型:ID} 的一致格式，便于 SCAN 模式匹配与运维排查。
 */
public class RedisKeyUtils {

    /**
     * key 的分隔符。
     */
    public static final String KEY_SPLITTER = ":";

    /**
     * 拼接 RedisKey。
     * <p>
     * 格式：{@code keyPrefix:arg1:arg2:...}，所有参数以分隔符连接，末尾不保留分隔符。
     *
     * @param keyPrefix key 前缀，为空时不拼接前缀
     * @param args      各级 key 片段
     * @return 拼接后的 RedisKey
     */
    public static String buildKey(String keyPrefix, Object... args) {
        StringBuilder sb = new StringBuilder(60);
        if (StringUtils.isNotBlank(keyPrefix)) {
            sb.append(keyPrefix).append(KEY_SPLITTER);
        }
        for (Object arg : args) {
            sb.append(arg).append(KEY_SPLITTER);
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * 针对只有 type 和 id 的优化拼接。
     * <p>
     * 格式：{@code prefixWithColon + type + ":" + id}；id 为 null 时返回 {@code prefixWithColon + type}。
     * 比 {@link #buildKey} 少了 StringBuilder 和循环开销，适合高频路径。
     *
     * @param prefixWithColon 带冒号结尾的前缀（如 "uw-cache:"）
     * @param type            类型名（如 cacheName / counterType / lockerType）
     * @param id              主键，null 表示仅 type 维度
     * @return 拼接后的 RedisKey
     */
    public static String buildTypeId(String prefixWithColon, String type, Object id) {
        if (id == null) {
            return prefixWithColon + type;
        } else {
            return prefixWithColon + type + KEY_SPLITTER + id;
        }
    }

}
