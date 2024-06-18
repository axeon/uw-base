package uw.auth.service.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 认证字段的小工具类。
 * 为了方便确认电话，email，身份证号类信息是否被认证过。
 * 认证过的信息，会增加一个"!"的标头。
 */
public class VerifiedUtils {

    /**
     * 认证标记。
     */
    private static final char VERIFIED_TAG = '!';

    /**
     * 判断信息是否是已认证过。
     *
     * @param data
     * @return
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
     * 给数据打上认证标记。
     *
     * @param data
     * @return
     */
    public static String tagData(String data) {
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
     * 给数据去除认证标记。
     *
     * @param data
     * @return
     */
    public static String untagData(String data) {
        if (StringUtils.isBlank(data)) {
            return data;
        }
        while (data.length() > 0 && data.charAt(0) == VERIFIED_TAG) {
            data = data.substring(1);
        }
        return data;
    }

}
