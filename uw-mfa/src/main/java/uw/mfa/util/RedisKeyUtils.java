package uw.mfa.util;

import io.micrometer.common.util.StringUtils;

/**
 * RedisKey的工具类。
 * 用于拼接redis的主键。
 */
public class RedisKeyUtils {


    /**
     * key的分割符
     */
    public static final String KEY_SPLITTER = ":";

    /**
     * 拼接redisKey。
     *
     * @param keyPrefix
     * @param args
     * @return
     */
    public static String buildKey(String keyPrefix, Object... args) {
        StringBuilder sb = new StringBuilder( 60 );
        if (StringUtils.isNotBlank( keyPrefix )) {
            sb.append( keyPrefix ).append( KEY_SPLITTER );
        }
        for (Object arg : args) {
            sb.append( arg ).append( KEY_SPLITTER );
        }
        if (sb.length() > 0) {
            sb.deleteCharAt( sb.length() - 1 );
        }
        return sb.toString();
    }

    /**
     * 针对只有type和Id的优化拼接。
     *
     * @param prefixWithColon
     * @param type
     * @param id
     * @return
     */
    public static final String buildTypeId(String prefixWithColon, String type, Object id) {
        if (id == null) {
            return prefixWithColon + type;
        } else {
            return prefixWithColon + type + KEY_SPLITTER + id;
        }
    }

}
