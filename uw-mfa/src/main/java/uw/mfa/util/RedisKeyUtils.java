package uw.mfa.util;

import org.apache.commons.lang3.StringUtils;

/**
 * RedisKey的工具类。
 * <p>用于拼接MFA模块各业务（IP限制/Captcha/设备码/TOTP校验）的Redis主键。</p>
 */
public class RedisKeyUtils {


    /**
     * key的分隔符，固定为冒号 ":"。
     */
    public static final String KEY_SPLITTER = ":";

    /**
     * 拼接redisKey。
     * <p>格式为 {@code prefix:arg1:arg2:...}，prefix为空时不带前缀，args为空时仅返回prefix。</p>
     *
     * @param keyPrefix key前缀，可为空
     * @param args      key组成部分（如IP、deviceId、userInfo等）
     * @return 拼接后的完整key
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
     * 针对只有type和Id的场景优化拼接。
     * <p>格式为 {@code prefixWithColon + type[:id]}，id为null时不追加id段。</p>
     *
     * @param prefixWithColon 已含分隔符的前缀（如 "ipLimit:"）
     * @param type            类型段
     * @param id              id段，可为null
     * @return 拼接后的完整key
     */
    public static String buildTypeId(String prefixWithColon, String type, Object id) {
        if (id == null) {
            return prefixWithColon + type;
        } else {
            return prefixWithColon + type + KEY_SPLITTER + id;
        }
    }

}
