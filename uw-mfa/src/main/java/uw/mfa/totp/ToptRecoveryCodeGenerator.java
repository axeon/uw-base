package uw.mfa.totp;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * TOTP恢复码生成器。
 * <p>生成16位小写字母数字混合的一次性恢复码，按4字符分组（如 {@code abcd-efgh-ijkl-mnop}），</p>
 * <p>用于TOTP密钥丢失时的应急登录。</p>
 */
public class ToptRecoveryCodeGenerator {

    /**
     * 恢复码字符集（小写字母+数字）。
     */
    private static final char[] CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    /**
     * 恢复码总长度（不含分隔符）。
     */
    private static final int CODE_LENGTH = 16;

    /**
     * 每组字符数，达到该数插入分隔符。
     */
    private static final int GROUPS_NBR = 4;

    /**
     * 随机数生成器（线程局部）。
     */
    private static final Random random = ThreadLocalRandom.current();

    /**
     * 批量生成恢复码。
     *
     * @param amount 生成数量，小于1时按1处理
     * @return 恢复码字符串数组
     */
    public static String[] generateCodes(int amount) {
        // Must generate at least one code
        if (amount < 1) {
            amount = 1;
        }
        // Create an array and fill with generated codes
        String[] codes = new String[amount];
        Arrays.setAll(codes, i -> generateCode());
        return codes;
    }

    /**
     * 生成单个16位恢复码（4字符分组，横线分隔）。
     *
     * @return 恢复码字符串
     */
    private static String generateCode() {
        final StringBuilder code = new StringBuilder(CODE_LENGTH + (CODE_LENGTH / GROUPS_NBR) - 1);
        for (int i = 0; i < CODE_LENGTH; i++) {
            // Append random character from authorized ones
            code.append(CHARACTERS[random.nextInt(CHARACTERS.length)]);
            // Split code into groups for increased readability
            if ((i + 1) % GROUPS_NBR == 0 && (i + 1) != CODE_LENGTH) {
                code.append("-");
            }
        }
        return code.toString();
    }

}