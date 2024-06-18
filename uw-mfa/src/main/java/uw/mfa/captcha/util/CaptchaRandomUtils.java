package uw.mfa.captcha.util;


import org.apache.commons.lang3.RandomStringUtils;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


public class CaptchaRandomUtils {

    /**
     * 生成UUID。
     *
     * @return
     */
    public static final String getUUID() {
        return UUID.randomUUID().toString().replaceAll( "-", "" );
    }

    /**
     * 获得随机int数值。
     *
     * @param bound
     * @return
     */
    public static final int getRandomInt(int bound) {
        return ThreadLocalRandom.current().nextInt( bound );
    }

    /**
     * 获得随机范围内数字。
     *
     * @param startNum
     * @param endNum
     * @return
     */
    public static final int getRandomInt(int startNum, int endNum) {
        return ThreadLocalRandom.current().nextInt( endNum - startNum ) + startNum;
    }

    /**
     * 获取随机字符串。
     *
     * @param length
     * @return
     */
    public static final String getRandomString(int length) {
        return RandomStringUtils.randomAlphanumeric( length );
    }

}
