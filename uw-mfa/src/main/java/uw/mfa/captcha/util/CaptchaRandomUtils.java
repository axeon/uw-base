package uw.mfa.captcha.util;


import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import java.util.UUID;


public class CaptchaRandomUtils {

    /**
     * 生成UUID。
     *
     * @return
     */
    public static String getUUID() {
        return UUID.randomUUID().toString().replaceAll( "-", "" );
    }

    /**
     * 获得随机int数值。
     *
     * @param bound
     * @return
     */
    public static int getRandomInt(int bound) {
        return RandomUtils.insecure().randomInt( 0, bound );
    }

    /**
     * 获得随机范围内数字。
     *
     * @param startNum
     * @param endNum
     * @return
     */
    public static int getRandomInt(int startNum, int endNum) {
        return RandomUtils.insecure().randomInt( 0, endNum - startNum ) + startNum;
    }

    /**
     * 获取随机字符串。
     *
     * @param length
     * @return
     */
    public static String getRandomString(int length) {
        return RandomStringUtils.secureStrong().nextAlphanumeric( length );
    }

}
