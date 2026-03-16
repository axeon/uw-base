package uw.common.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES加解密工具类。
 */
public class AESUtils {

    private static final Logger log = LoggerFactory.getLogger(AESUtils.class);

    /**
     * 随机数生成器。
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 初始化向量字节数。
     */
    private static final int IV_SIZE = 16;

    /**
     * 生成AES对称密钥。
     * 可选的密钥长度为128、192、256位。
     *
     * @return AES密钥字节数组
     * @throws NoSuchAlgorithmException 当AES算法不可用时抛出异常
     */
    public static byte[] generateKey(int keySize) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(keySize);
            SecretKey secretKey = keyGen.generateKey();
            return secretKey.getEncoded();
        } catch (Exception e) {
            log.error("AES生成密钥失败: {}", e.getMessage());
            return generateRandomBytes(keySize / 8);
        }
    }

    /**
     * 生成初始化向量（IV）。
     *
     * @return 16字节的随机IV
     */
    public static byte[] generateIv() {
        return generateRandomBytes(IV_SIZE);
    }

    /**
     * 生成随机字节数组。
     *
     * @param size
     * @return
     */
    private static byte[] generateRandomBytes(int size) {
        byte[] iv = new byte[size];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    /**
     * AES加密。
     * 参数指定IV，生成的密文不再带有iv信息。
     *
     * @param key  AES密钥字节数组
     * @param iv   初始化向量字节数组（必须16字节）
     * @param data 明文字节数组
     * @return 密文字节数组（IV+密文）
     */
    public static byte[] encrypt(byte[] key, byte[] iv, byte[] data) {
        if (iv == null || iv.length != IV_SIZE) {
            log.warn("AES加密失败: IV长度必须为16字节");
            return null;
        }
        if (key == null || key.length == 0) {
            log.warn("AES加密失败: 密钥不能为空");
            return null;
        }
        if (data == null || data.length == 0) {
            log.warn("AES加密失败: 明文不能为空");
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            log.error("AES加密失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * AES加密字符串。
     * 参数指定IV，生成的密文不再带有iv信息。
     * 带字符串参数，返回字符串结果。
     *
     * @param key  AES密钥字节数组
     * @param iv   16字节的初始化向量字节数组
     * @param data 明文字符串
     * @return 密文字符串
     */
    public static String encryptString(byte[] key, byte[] iv, String data) {
        if (StringUtils.isBlank(data)) {
            log.warn("AES加密失败: 明文不能为空");
            return data;
        }
        return Base64.getEncoder().encodeToString(encrypt(key, iv, data.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * AES解密。
     *
     * @param key       AES密钥字节数组
     * @param encrypted 密文字节数组（需包含前16字节IV）
     * @return 明文字节数组
     */
    public static byte[] decrypt(byte[] key, byte[] iv, byte[] encrypted) {
        if (iv == null || iv.length != IV_SIZE) {
            log.warn("AES解密失败: IV长度必须为16字节");
            return null;
        }
        if (key == null || key.length == 0) {
            log.warn("AES解密失败: 密钥不能为空");
            return null;
        }
        if (encrypted == null || encrypted.length == 0) {
            log.warn("AES解密失败: 密文不能为空");
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            log.error("AES解密失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * AES解密。
     * 带字符串参数，返回字符串结果。
     *
     * @param key       AES密钥字节数组
     * @param iv        16字字节数组
     * @param encrypted 密文字符串
     * @return 明文字符串
     */
    public static String decryptString(byte[] key, byte[] iv, String encrypted) {
        if (StringUtils.isBlank(encrypted)) {
            log.warn("AES解密失败: 密文不能为空");
            return encrypted;
        }
        byte[] decrypted = decrypt(key, iv, Base64.getDecoder().decode(encrypted));
        if (decrypted == null) {
            return null;
        }
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * AES加密。
     * 动态生成IV，生成的密文携带有iv信息。
     *
     * @param key  AES密钥字节数组
     * @param data 明文字节数组
     * @return 密文字节数组（IV+密文）
     */
    public static byte[] encrypt(byte[] key, byte[] data) {
        if (key == null || key.length == 0) {
            log.warn("AES加密失败: 密钥不能为空");
            return null;
        }
        if (data == null || data.length == 0) {
            log.warn("AES加密失败: 明文不能为空");
            return null;
        }
        byte[] iv = generateIv();
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(data);
            return ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
        } catch (Exception e) {
            log.error("AES加密失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * AES加密。
     * 动态生成IV，生成的密文携带有iv信息。
     * 带字符串参数，返回字符串结果。
     *
     * @param key  AES密钥字节数组
     * @param data 明文字符串
     * @return 密文字符串
     */
    public static String encryptString(byte[] key, String data) {
        if (StringUtils.isBlank(data)) {
            log.warn("AES加密失败: 明文不能为空");
            return data;
        }
        return Base64.getEncoder().encodeToString(encrypt(key, data.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * AES解密。
     * 需要密文中携带iv信息。
     *
     * @param key       AES密钥字节数组
     * @param encrypted 密文字节数组（需包含前16字节IV）
     * @return 明文字节数组
     */
    public static byte[] decrypt(byte[] key, byte[] encrypted) {
        if (key == null || key.length == 0) {
            log.warn("AES解密失败: 密钥不能为空");
            return null;
        }
        if (encrypted == null || encrypted.length < IV_SIZE) {
            log.warn("AES解密失败: 密文为空或长度小于IV长度");
            return null;
        }
        try {
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

    /**
     * AES解密。
     * 需要密文中携带iv信息。
     * 带字符串参数，返回字符串结果。
     *
     * @param key       AES密钥字节数组
     * @param encrypted 密文字符串
     * @return 明文字符串
     */
    public static String decryptString(byte[] key, String encrypted) {
        if (StringUtils.isBlank(encrypted)) {
            log.warn("AES解密失败: 密文不能为空");
            return encrypted;
        }
        byte[] decrypted = decrypt(key, Base64.getDecoder().decode(encrypted));
        if (decrypted == null) {
            return null;
        }
        return new String(decrypted, StandardCharsets.UTF_8);
    }

//    public static void main(String[] args) {
//        byte[] key = generateKey(256);
//        byte[] iv = generateIv();
//        //使用自动生成iv，并内联到密文。
//        String encryptString = AESUtils.encryptString(key, "123456");
//        System.out.println(encryptString);
//        String decryptString = AESUtils.decryptString(key, encryptString);
//        System.out.println(decryptString);
//        //指定iv。
//        String encryptString1 = AESUtils.encryptString(key, iv, "123456");
//        System.out.println(encryptString1);
//        String decryptString1 = AESUtils.decryptString(key, iv, encryptString1);
//        System.out.println(decryptString1);
//    }


}
