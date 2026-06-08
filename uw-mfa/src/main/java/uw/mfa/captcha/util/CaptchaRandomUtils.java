package uw.mfa.captcha.util;


import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机工具。
 */
public class CaptchaRandomUtils {

    /**
     * 生成UUID。
     *
     * @return
     */
    public static String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 获取随机int数值。
     *
     * @param bound
     * @return
     */
    public static int getRandomInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    /**
     * 获取随机范围内数字。
     *
     * @param startNum
     * @param endNum
     * @return
     */
    public static int getRandomInt(int startNum, int endNum) {
        return ThreadLocalRandom.current().nextInt(endNum - startNum) + startNum;
    }

    /**
     * 获取随机字符串。
     *
     * @param length
     * @return
     */
    public static String getRandomString(int length) {
        return RandomStringUtils.secureStrong().nextAlphanumeric(length);
    }

}
