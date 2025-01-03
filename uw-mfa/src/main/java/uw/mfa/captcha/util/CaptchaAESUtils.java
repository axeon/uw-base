package uw.mfa.captcha.util;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES加解密工具类.
 */
public class CaptchaAESUtils {

    private static final Logger logger = LoggerFactory.getLogger( CaptchaAESUtils.class );

    //算法
    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";

    /**
     * AES解密
     *
     * @param encryptData 待解密的字符串
     * @param decryptKey  解密密钥
     * @return 解密后的String
     * @throws Exception
     */
    public static String aesDecrypt(String encryptData, String decryptKey) {
        if (StringUtils.isBlank( encryptData ) || StringUtils.isBlank( decryptKey )) {
            return null;
        }
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance( "AES" );
            keyGen.init( 128 );
            Cipher cipher = Cipher.getInstance( ALGORITHM );
            cipher.init( Cipher.DECRYPT_MODE, new SecretKeySpec( decryptKey.getBytes(), "AES" ) );
            byte[] decryptBytes = cipher.doFinal( Base64.getDecoder().decode( encryptData ) );
            return new String( decryptBytes );
        } catch (Exception e) {
            logger.error( "aesDecrypt exception:{}", e.getMessage(), e );
            return "";
        }
    }

    /**
     * AES加密
     *
     * @param data       待加密的内容
     * @param encryptKey 加密密钥
     * @return 加密后的byte[]
     * @throws Exception
     */
    public static String aesEncrypt(String data, String encryptKey) {
        if (StringUtils.isBlank( data ) || StringUtils.isBlank( encryptKey )) {
            return null;
        }
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance( "AES" );
            keyGen.init( 128 );
            Cipher cipher = Cipher.getInstance( ALGORITHM );
            cipher.init( Cipher.ENCRYPT_MODE, new SecretKeySpec( encryptKey.getBytes(), "AES" ) );
            return Base64.getEncoder().encodeToString( cipher.doFinal( data.getBytes( StandardCharsets.UTF_8 ) ) );
        } catch (Exception e) {
            logger.error( "aesEncryptToBytes exception:{}", e.getMessage(), e );
            return null;
        }
    }


//    /**
//     * 测试
//     */
//    public static void main(String[] args) throws Exception {
//        String key = UUID.randomUUID().toString().substring( 0, 32 );
//        String content = RandomStringUtils.randomAlphanumeric( 256 );
//        System.out.println( "密钥:" + key );
//        System.out.println( "加密前：" + content );
//        String encryptData = aesEncrypt( content, key );
//        System.out.println( "加密后：" + encryptData );
//        String decryptData = aesDecrypt( encryptData, key );
//        System.out.println( "解密后：" + decryptData );
//    }

}
