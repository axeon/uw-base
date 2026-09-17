package uw.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
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
        return sign(new byte[][]{message.getBytes(StandardCharsets.UTF_8)}, secret);
    }

    /**
     * 使用HMAC-SHA256对多段消息签名：逐段update，与拼接成单段后调用
     * {@link #sign(String, String)}的结果逐字节等价（HMAC本身是流式计算），
     * 但省去大payload为凑单段消息而做的整份拼接拷贝（如 body 级消息的分段签名）。
     *
     * @param parts 消息分段（按序拼接即完整消息；null/空段跳过，不影响结果）
     * @param secret 密钥
     * @return 十六进制签名结果
     */
    public static String sign(byte[][] parts, String secret) {
        try {
            Mac mac = newMac(secret);
            for (byte[] part : parts) {
                if (part != null && part.length > 0) {
                    mac.update(part);
                }
            }
            return DigestUtils.bytesToHex(mac.doFinal());
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
        return verify(new byte[][]{message.getBytes(StandardCharsets.UTF_8)}, secret, signature);
    }

    /**
     * 验证多段消息签名（与拼接成单段后调用 {@link #verify(String, String, String)}等价）。
     *
     * @param parts     待验证消息分段（按序拼接即完整消息）
     * @param secret    密钥
     * @param signature 待比对签名
     * @return 签名是否有效
     */
    public static boolean verify(byte[][] parts, String secret, String signature) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }
        String expected = sign(parts, secret);
        // 使用常量时间比较，避免时序攻击按字节推断签名。
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 初始化HMAC计算器。
     */
    private static Mac newMac(String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
        return mac;
    }
}
