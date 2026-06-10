package uw.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * HMAC签名工具类，用于任务脚本签名校验。
 */
public class HmacUtils {

    private static final String ALGORITHM = "HmacSHA256";

    /**
     * 使用HMAC-SHA256对消息签名，返回十六进制字符串。
     *
     * @param message 待签名消息
     * @param secret  密钥
     * @return 十六进制签名结果
     */
    public static String sign(String message, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return DigestUtils.bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC签名失败", e);
        }
    }

    /**
     * 验证消息签名。
     *
     * @param message   待验证消息
     * @param secret    密钥
     * @param signature 待比对签名
     * @return 签名是否有效
     */
    public static boolean verify(String message, String secret, String signature) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }
        String expected = sign(message, secret);
        return expected.equals(signature);
    }
}
