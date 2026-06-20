package uw.mfa.captcha.util;


import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Captcha随机工具类。
 * <p>封装UUID生成、随机整数、随机字符串等通用随机能力，底层使用 {@link ThreadLocalRandom} 保证并发性能。</p>
 */
public class CaptchaRandomUtils {

    /**
     * 生成32位无横线UUID，用作captchaId。
     *
     * @return 32位UUID字符串
     */
    public static String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 获取[0, bound)范围内的随机int。
     *
     * @param bound 上界（不包含），必须大于0
     * @return 随机整数
     */
    public static int getRandomInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    /**
     * 获取[startNum, endNum)范围内的随机int。
     *
     * @param startNum 起始值（包含）
     * @param endNum   结束值（不包含），必须大于startNum
     * @return 随机整数
     */
    public static int getRandomInt(int startNum, int endNum) {
        return ThreadLocalRandom.current().nextInt(endNum - startNum) + startNum;
    }

    /**
     * 获取指定长度的随机字母数字字符串（强随机源）。
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String getRandomString(int length) {
        return RandomStringUtils.secureStrong().nextAlphanumeric(length);
    }

}
