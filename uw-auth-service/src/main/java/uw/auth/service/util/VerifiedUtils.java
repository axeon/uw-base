package uw.auth.service.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 认证字段小工具类。
 * <p>
 * 用于标识电话、邮箱、身份证等信息是否已通过认证：已认证信息会在头部追加 {@code !} 标记，
 * 业务侧据此区分「已认证」与「未认证但填写」的数据。
 *
 * @author axeon
 */
public class VerifiedUtils {

    /**
     * 认证标记。
     */
    private static final char VERIFIED_TAG = '!';

    /**
     * 判断信息是否已打上认证标记。
     *
     * @param data 待检测的字符串
     * @return true 表示已认证（以 {@code !} 开头）
     */
    public static boolean isTagged(String data) {
        if (StringUtils.isBlank(data)) {
            return false;
        }
        if (data.charAt(0) == VERIFIED_TAG) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 给数据打上认证标记（若已标记则原样返回）。
     *
     * @param data 原始字符串
     * @return 带 {@code !} 前缀的字符串
     */
    public static String tag(String data) {
        if (StringUtils.isBlank(data)) {
            return data;
        }
        if (data.charAt(0) == VERIFIED_TAG) {
            //已经打过标记，直接返回。
            return data;
        } else {
            return VERIFIED_TAG + data;
        }
    }

    /**
     * 去除数据上的所有认证标记。
     *
     * @param data 带标记的字符串
     * @return 去除 {@code !} 前缀后的原始字符串
     */
    public static String untag(String data) {
        if (StringUtils.isBlank(data)) {
            return data;
        }
        while (data.length() > 1 && data.charAt(0) == VERIFIED_TAG) {
            data = data.substring(1);
        }
        return data;
    }

}
