package uw.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AES加解密工具类。
 */
public class AESUtils {

    private static final Logger log = LoggerFactory.getLogger(AESUtils.class);

    /**
     * 初始化向量字节数。
     */
    private static final int IV_SIZE = 16;

    /**
     * 生成AES对称密钥。
     *
     * @return AES密钥字节数组
     * @throws NoSuchAlgorithmException 当AES算法不可用时抛出异常
     */
    public static byte[] generateKey(int keySize) throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(keySize);
        SecretKey secretKey = keyGen.generateKey();
        return secretKey.getEncoded();
    }

    /**
     * 生成初始化向量（IV）。
     *
     * @return 16字节的随机IV
     */
    public static byte[] generateIv() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[IV_SIZE];
        random.nextBytes(iv);
        return iv;
    }

    /**
     * AES加密。
     *
     * @param key  AES密钥字节数组
     * @param iv   初始化向量字节数组（必须16字节）
     * @param data 明文字节数组
     * @return 密文字节数组（IV+密文）
     */
    public static byte[] encrypt(byte[] key, byte[] iv, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(data);
            return ByteBuffer
                    .allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();
        } catch (Exception e) {
            log.error("AES加密失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * AES解密。
     *
     * @param key       AES密钥字节数组
     * @param encrypted 密文字节数组（需包含前16字节IV）
     * @return 明文字节数组
     */
    public static byte[] decrypt(byte[] key, byte[] encrypted) {
        try {
            if (encrypted.length < IV_SIZE) {
                log.error("密文长度不足");
                return null;
            }
            byte[] iv = Arrays.copyOfRange(encrypted, 0, IV_SIZE);
            byte[] cipherData = Arrays.copyOfRange(encrypted, IV_SIZE, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(cipherData);
        } catch (Exception e) {
            log.error("AES解密失败: {}", e.getMessage());
            return null;
        }
    }


}
