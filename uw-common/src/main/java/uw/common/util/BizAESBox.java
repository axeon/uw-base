package uw.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 业务AES加解密盒子。
 * 此方法为了解决业务AES加解密对照问题，通过固定的密钥和向量来生成密文，并确保多次生成结果的一致性。
 * 1.通过BizAESBox.genAesConfig方法生成AES密钥和向量配置信息，并保存在配置文件中。
 * 2.通过BizAESBox.getInstance方法传入配置文件路径，获取aesBox实例。
 * 3.通过aesBox.encrypt方法加密数据。
 * 4.通过aesBox.decrypt方法解密数据。
 */
public class BizAESBox {

    private static final Logger log = LoggerFactory.getLogger(BizAESBox.class);

    /**
     * 实例缓存。
     */
    private static final Map<String, BizAESBox> map = new ConcurrentHashMap<>();

    /**
     * 密钥。
     */
    private final byte[] aesKey;

    /**
     * 向量。
     */
    private final byte[] aesIv;

    /**
     * 获取实例。
     *
     * @param configPath 配置文件路径。
     * @return
     */
    public static BizAESBox getInstance(String configPath) {
        return map.computeIfAbsent(configPath, (key) -> {
            Properties props = new Properties();
            // 加载配置文件
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(configPath)) {
                props.load(is);
            } catch (Exception e) {
                log.error("BizAES配置文件[{}]读取错误!", configPath, e);
            }
            // 获取密钥和向量
            byte[] aesKey = null;
            byte[] aesIv = null;
            String aesKeyStr = props.getProperty("aes.key");
            if (aesKeyStr != null && !aesKeyStr.isEmpty()) {
                aesKey = Base64.getDecoder().decode(props.getProperty("aes.key"));
            }
            String aesIvStr = props.getProperty("aes.iv");
            if (aesIvStr != null && !aesIvStr.isEmpty()) {
                aesIv = Base64.getDecoder().decode(props.getProperty("aes.iv"));
            }
            //当前只检测key不可缺失
            if (aesKey == null) {
                log.error("BizAES配置文件[{}]缺少密钥配置!", configPath);
                return null;
            }
            // 创建AES加解密盒子实例
            return new BizAESBox(aesKey, aesIv);
        });
    }

    /**
     * 构造函数。
     *
     * @param aesKey 密钥。
     * @param aesIv  向量。
     */
    public BizAESBox(byte[] aesKey, byte[] aesIv) {
        this.aesKey = aesKey;
        this.aesIv = aesIv;
    }

    /**
     * 获取密钥。
     *
     * @return
     */
    public byte[] getAesKey() {
        return aesKey;
    }

    /**
     * 获取向量。
     *
     * @return
     */
    public byte[] getAesIv() {
        return aesIv;
    }

    /**
     * 加密字符串。
     *
     * @param data
     * @return
     */
    public String encrypt(String data) {
        if (aesIv!=null) {
            return AESUtils.encryptString(aesKey, aesIv, data);
        } else {
            return AESUtils.encryptString(aesKey, data);
        }
    }

    /**
     * 解密字符串。
     *
     * @param encrypted
     * @return
     */
    public String decrypt(String encrypted) {
        if (aesIv!=null) {
            return AESUtils.decryptString(aesKey, aesIv, encrypted);
        }else{
            return AESUtils.decryptString(aesKey, encrypted);
        }
    }

    /**
     * 生成AES密钥和向量配置信息。
     *
     * @return
     */
    public static String genAesConfig() {
        byte[] key = AESUtils.generateKey(256);
        byte[] iv = AESUtils.generateIv();
        return "aes.key=" + Base64.getEncoder().encodeToString(key) + "\naes.iv=" + Base64.getEncoder().encodeToString(iv);
    }

//    public static void main(String[] args) {
//        // 生成AES密钥和向量配置信息，并保存的特定配置文件中。
//        System.out.println(genAesConfig());
//        // 获取AES加解密盒子实例,范例中的配置文件路径为bizaes.properties（可根据业务情况随意修改）
//        BizAESBox aesBox = BizAESBox.getInstance("bizaes.properties");
//        // 加密字符串
//        System.out.println(aesBox.encrypt("123456"));
//        // 解密字符串
//        System.out.println(aesBox.decrypt(aesBox.encrypt("123456")));
//    }

}
