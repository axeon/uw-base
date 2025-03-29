package uw.mfa.totp;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 恢复码生成器。
 */
public class ToptRecoveryCodeGenerator {

    /**
     * 恢复码字符集.
     */
    private static final char[] CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    /**
     * 恢复码长度.
     */
    private static final int CODE_LENGTH = 16;

    /**
     * 恢复码分组数.
     */
    private static final int GROUPS_NBR = 4;

    /**
     * 随机数生成器.
     */
    private static final Random random = ThreadLocalRandom.current();

    /**
     * 生成恢复码.
     *
     * @param amount 数量
     * @return
     */
    public static String[] generateCodes(int amount) {
        // Must generate at least one code
        if (amount < 1) {
            amount = 1;
        }
        // Create an array and fill with generated codes
        String[] codes = new String[amount];
        Arrays.setAll( codes, i -> generateCode() );
        return codes;
    }

    /**
     * 生成恢复码.
     *
     * @return
     */
    private static String generateCode() {
        final StringBuilder code = new StringBuilder( CODE_LENGTH + (CODE_LENGTH / GROUPS_NBR) - 1 );
        for (int i = 0; i < CODE_LENGTH; i++) {
            // Append random character from authorized ones
            code.append( CHARACTERS[random.nextInt( CHARACTERS.length )] );
            // Split code into groups for increased readability
            if ((i + 1) % GROUPS_NBR == 0 && (i + 1) != CODE_LENGTH) {
                code.append( "-" );
            }
        }
        return code.toString();
    }

}