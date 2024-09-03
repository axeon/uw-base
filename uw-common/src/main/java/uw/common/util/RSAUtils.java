package uw.common.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA工具类。
 */
public class RSAUtils {

    private static final Logger log = LoggerFactory.getLogger( RSAUtils.class );

    /**
     * 从字符串中加载公钥。
     *
     * @param buffer 公钥数据
     * @throws Exception 加载公钥时产生的异常
     */
    public static RSAPublicKey loadPublicKeyFromBytes(byte[] buffer) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance( "RSA" );
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec( buffer );
            return (RSAPublicKey) keyFactory.generatePublic( keySpec );
        } catch (Throwable e) {
            log.error( "loadPublicKeyFromBytes error!", e );
        }
        return null;
    }

    /**
     * 从字符串中加载公钥。
     *
     * @param buffer 公钥数据
     * @throws Exception 加载公钥时产生的异常
     */
    public static RSAPrivateKey loadPrivateKeyFromBytes(byte[] buffer) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance( "RSA" );
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec( buffer );
            return (RSAPrivateKey) keyFactory.generatePrivate( keySpec );
        } catch (Throwable e) {
            log.error( "loadPrivateKeyFromBytes error!", e );
        }
        return null;
    }


    /**
     * 从字符串中加载公钥。
     *
     * @param publicKeyStr 公钥数据字符串
     * @throws Exception 加载公钥时产生的异常
     */
    public static RSAPublicKey loadPublicKeyFromBase64(String publicKeyStr) {
        if (StringUtils.isNotBlank( publicKeyStr )) {
            return loadPublicKeyFromBytes( Base64.getDecoder().decode( publicKeyStr ) );
        } else {
            return null;
        }
    }

    /**
     * 从Base64中加载私钥。
     *
     * @param privateKeyStr
     * @return
     * @throws Exception
     */
    public static RSAPrivateKey loadPrivateKeyFromBase64(String privateKeyStr) {
        if (StringUtils.isNotBlank( privateKeyStr )) {
            return loadPrivateKeyFromBytes( Base64.getDecoder().decode( privateKeyStr ) );
        } else {
            return null;
        }
    }

    /**
     * 私钥加密。
     *
     * @param privateKey 私钥
     * @param plainData  明文数据
     * @return
     * @throws Exception 加密过程中的异常信息
     */
    public static byte[] encrypt(RSAPrivateKey privateKey, byte[] plainData) {
        try {
            // 使用默认RSA
            Cipher cipher = Cipher.getInstance( "RSA" );
            cipher.init( Cipher.ENCRYPT_MODE, privateKey );
            //数据块大小
            int blockSize = privateKey.getModulus().bitLength() / 8 - 11;
            int inputLen = plainData.length;
            ByteArrayOutputStream out = new ByteArrayOutputStream( plainData.length * 3 );
            int offset = 0;
            byte[] cache;
            int i = 0;
            // 对数据分段解密
            while (inputLen - offset > 0) {
                if (inputLen - offset > blockSize) {
                    cache = cipher.doFinal( plainData, offset, blockSize );
                } else {
                    cache = cipher.doFinal( plainData, offset, inputLen - offset );
                }
                out.write( cache, 0, cache.length );
                i++;
                offset = i * blockSize;
            }
            byte[] encryptedData = out.toByteArray();
            out.close();
            return encryptedData;
        } catch (Throwable e) {
            log.error( "encrypt error!", e );
        }
        return null;
    }

    /**
     * 公钥加密。
     *
     * @param publicKey 公钥
     * @param plainData 明文数据
     * @return
     * @throws Exception 加密过程中的异常信息
     */
    public static byte[] encrypt(RSAPublicKey publicKey, byte[] plainData) {
        try {
            // 使用默认RSA
            Cipher cipher = Cipher.getInstance( "RSA" );
            cipher.init( Cipher.ENCRYPT_MODE, publicKey );
            //数据块大小
            int blockSize = publicKey.getModulus().bitLength() / 8 - 11;
            int inputLen = plainData.length;
            ByteArrayOutputStream out = new ByteArrayOutputStream( plainData.length * 3 );
            int offset = 0;
            byte[] cache;
            int i = 0;
            // 对数据分段解密
            while (inputLen - offset > 0) {
                if (inputLen - offset > blockSize) {
                    cache = cipher.doFinal( plainData, offset, blockSize );
                } else {
                    cache = cipher.doFinal( plainData, offset, inputLen - offset );
                }
                out.write( cache, 0, cache.length );
                i++;
                offset = i * blockSize;
            }
            byte[] encryptedData = out.toByteArray();
            out.close();
            return encryptedData;
        } catch (Throwable e) {
            log.error( "encrypt error!", e );
        }
        return null;
    }


    /**
     * 公钥解密。
     *
     * @param publicKey  公钥
     * @param cipherData 密文数据
     * @return 明文
     * @throws Exception 解密过程中的异常信息
     */
    public static byte[] decrypt(RSAPublicKey publicKey, byte[] cipherData) {
        try {
            // 使用默认RSA
            Cipher cipher = Cipher.getInstance( "RSA" );
            cipher.init( Cipher.DECRYPT_MODE, publicKey );
            //数据块大小
            int blockSize = publicKey.getModulus().bitLength() / 8;
            int inputLen = cipherData.length;
            ByteArrayOutputStream out = new ByteArrayOutputStream( cipherData.length / 2 );
            int offset = 0;
            byte[] cache;
            int i = 0;
            // 对数据分段解密
            while (inputLen - offset > 0) {
                if (inputLen - offset > blockSize) {
                    cache = cipher.doFinal( cipherData, offset, blockSize );
                } else {
                    cache = cipher.doFinal( cipherData, offset, inputLen - offset );
                }
                out.write( cache, 0, cache.length );
                i++;
                offset = i * blockSize;
            }
            byte[] decryptedData = out.toByteArray();
            out.close();
            return decryptedData;
        } catch (Throwable e) {
            log.error( "decrypt error!", e );
        }
        return null;
    }

    /**
     * 私钥解密。
     *
     * @param privateKey 私钥
     * @param cipherData 密文数据
     * @return 明文
     * @throws Exception 解密过程中的异常信息
     */
    public static byte[] decrypt(RSAPrivateKey privateKey, byte[] cipherData) {
        try {
            // 使用默认RSA
            Cipher cipher = Cipher.getInstance( "RSA" );
            cipher.init( Cipher.DECRYPT_MODE, privateKey );
            //数据块大小
            int blockSize = privateKey.getModulus().bitLength() / 8;
            int inputLen = cipherData.length;
            ByteArrayOutputStream out = new ByteArrayOutputStream( cipherData.length / 2 );
            int offset = 0;
            byte[] cache;
            int i = 0;
            // 对数据分段解密
            while (inputLen - offset > 0) {
                if (inputLen - offset > blockSize) {
                    cache = cipher.doFinal( cipherData, offset, blockSize );
                } else {
                    cache = cipher.doFinal( cipherData, offset, inputLen - offset );
                }
                out.write( cache, 0, cache.length );
                i++;
                offset = i * blockSize;
            }
            byte[] decryptedData = out.toByteArray();
            out.close();
            return decryptedData;
        } catch (Throwable e) {
            log.error( "decrypt error!", e );
        }
        return null;
    }

    /**
     * RSA签名。
     *
     * @param privateKey
     * @param content
     * @return
     */
    public static byte[] sign(RSAPrivateKey privateKey, byte[] content) {
        try {
            Signature signature = Signature.getInstance( "SHA1WithRSA" );
            signature.initSign( privateKey );
            signature.update( content );
            return signature.sign();
        } catch (Throwable e) {
            log.error( "sign error!", e );
        }
        return null;
    }

    /**
     * RSA验签名检查。
     *
     * @param publicKey 分配给开发商公钥
     * @param sign      签名值
     * @param content   待签名数据
     * @return 布尔值
     */
    public static boolean checkSign(RSAPublicKey publicKey, byte[] sign, byte[] content) {
        try {
            Signature signature = Signature.getInstance( "SHA1WithRSA" );
            signature.initVerify( publicKey );
            signature.update( content );
            return signature.verify( sign );
        } catch (Throwable e) {
            log.error( "checkSign error!", e );
        }
        return false;
    }

//    public static void main(String[]String args) throws Exception {
//        KeyPair keyPair = RSAUtils.genKeyPair(2048);
//        String publicKey = Base64.getEncoder().encodeToString( keyPair.getPublic().getEncoded() );
//        String privateKey =Base64.getEncoder().encodeToString( keyPair.getPrivate().getEncoded() );
//        String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlMYtTE8xBlM4AiwedCJRPEdyMaUA6ZnOovripQzVfZKgG/N846T5KbfnJljlz4QkLXjYUSSxu0
//        +TS26koyHV4fA6WqNzKzDXE0wVJI15eIj6KWuJF0gWwdOwUHJRHZHPH46NX5uqcckY7BW8S6rFxX4Ep7xGy0xT/KqqEoteqHRndX/Idpyg8RcglS4vDkXPz
//        /I4C8bxBnbVKkoYH3M1eTcaIPNRfKKCyGEQOYrpG1OVMJ2yW4Kf5bGOtYAP4ds5DzfpaYTSUjPbteHp7ry+Bp06fH3IY3NRuAU69tJpEIZSX4N9nRbfgieFUFRKMq9/XtPSHxm1AjpABNjvZpXBMQIDAQAB";
//        String privateKey =
//                "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCUxi1MTzEGUzgCLB50IlE8R3IxpQDpmc6i+uKlDNV9kqAb83zjpPkpt
//                +cmWOXPhCQteNhRJLG7T5NLbqSjIdXh8Dpao3MrMNcTTBUkjXl4iPopa4kXSBbB07BQclEdkc8fjo1fm6pxyRjsFbxLqsXFfgSnvEbLTFP8qqoSi16odGd1f8h2nKDxFyCVLi8ORc
//                /P8jgLxvEGdtUqShgfczV5Nxog81F8ooLIYRA5iukbU5UwnbJbgp/lsY61gA/h2zkPN+lphNJSM9u14enuvL4GnTp8fchjc1G4BTr20mkQhlJfg32dFt
//                +CJ4VQVEoyr39e09IfGbUCOkAE2O9mlcExAgMBAAECggEAArVouNhige0pEnONjqNbD6YUmnooxggDiMi48IS3PJb7QKUGSaH82hVHwWVKElUTYEtQY2pPzNNsIOoD1/c/TNK2CkVKbQ9xo
//                /+dLS4g93iFdvR28ZBBv0/IiI9rQgbAAtDiaqqjr9iHolE7PVSvrvq+UJQ7RPtn1kxgZi/oxlf4mKqZizKMm1dwa4irGvIIKHRllPBvunaJ9qIAUe+d2kPyCNKw58JE9lCqFvlrQEFWxTFMTP
//                +TZ06uAjx4v0W3PIxqSIOxHCE1miBc3K4YOlqLNRXhz1jBs/u6+fuj2Y/alNo/qO56hfqB2lLwZNbKIpgpC6sfrH6kRuLCsgiXIQKBgQDA+WKDrtlYF5fd9kJ
//                /tac0lQubINmPaeKjjgJojA9kEs92oePQC4IVejcakpG9juQ0tq7NUK61ff96UJvt0wrh4j2JmzPMoMe73maitaCeyYAActDmznnX7dFc0uJab2qxABveVPWId6iVQHgHAEp+g6nJnD91VraJ9soIxu
//                /Q4QKBgQDFXTeSu5gKc7WepG0mdv1IInGRPxTE50d+ehR/pn+iYovHpO8jzQ0fog2bBzYN6qFxOzqzJi7JKUiWMFtp66KnEPJPn1XIYhBQT9o4PEwUoShRD5i1fwW4p9JWAIMBzokkN9fUc6r4kS/ztI9P
//                /zK1wt6yGNrKx/f+TJeGnafqUQKBgHgtVyrHA1gXKV7z9CnwiPb4M0gixxrHSHuu/tT5FMSv6j
//                /pRiTUZejWOeb0jIrOqFQjyOzYaFVi6G3WMaWEnuxZpxVBcJrpLjpToPuHvUXmGy8sUejLgZn140K5mnnTlVAxzylBI8AfNP/uLFG/3qS3fk+uXN7IZ9m3tc6NheBBAoGBAIrAYCN
//                +UwN2GYcwXbQfDy25WVIdoQJU/oRCW0rqyOxmj5KJt73ZmJJWoW4OAciC3YD3fOsZuuadaXR9BqdOi4kcdt3XTL2Vg5aSrP3AFlSif
//                ++NIXTnmiQZh9wCfBe3Fd8qi0fHEbmyiJ2yoNZCuWnjWCCpLJhHQWe8fQKcr8IhAoGBAK3fgDQyjvULCVbWyHfh4pRs3QgKDI3KuX17P/Sneh4Q7jCffduzYjzsEp1PFRb3UYpNdb1WzQuRzM2w
//                /IR9LwNHz97Fpwu8wBobIudqAdhqs40fdGBCNFXuCSduNI7iO6Bwd5tZPdAWlDvlO+oYNeKURcri6VS0XMySn66ueEZ3";

//        System.out.println(publicKey);
//        System.out.println(privateKey);
//        System.out.println( "--------------公钥加密私钥解密过程-------------------" );
//        String plainText =  RandomStringUtils.random( 1000 );
//        System.out.println( "原文：" + plainText );
//        byte[] cipherData = RSAUtils.encrypt( RSAUtils.loadPublicKeyFromBase64( publicKey ), plainText.getBytes() );
//        String cipher = Base64.getEncoder().encodeToString( cipherData );
//        String cipher = "M4FCW+T5N1AoHNaY3LXy0tN6droWRUzN0mTA7lrsuEO8dGRcOgOyS0ruUbA7PrhLTjQkVF2EwiOJCcV1/MQQtv7A1l2eYI+Rk6k0
//        +R3DlX7knPJi5FUp6W0AJ2HaPZWmrQ3fg8CaTLwR8T2iAlJs4uClbw4X9CyZk/fNYzfpvTsNX2AzRMGvxxYRSmpKGCpa1iSSX2BPuUIaxV/dsoY/8RnDo+724OCE9CMOiILVgVjeS2qQXeY8fYIRxB
//        /EDUfCdx0qsUP8YWMcJtGcHflCgSouEQF91fAhKtUaYgYBPTjJj9U0AGQ0rmZHUW0/mRe5nhdH94sSWrCQODOhb95Qgw==";
//        System.out.println( "加密：" + cipher );
//        System.out.println( cipherData.length );
//        System.out.println( "加密：" + Base64.getEncoder().encodeToString( RSAUtils.encrypt( RSAUtils.loadPublicKeyFromBase64( publicKey ), plainText.getBytes() ) ) );
//        System.out.println( "加密：" + Base64.getEncoder().encodeToString( RSAUtils.encrypt( RSAUtils.loadPublicKeyFromBase64( publicKey ), plainText.getBytes() ) ) );
//        byte[] res = RSAUtils.decrypt( RSAUtils.loadPrivateKeyFromBase64( privateKey ), Base64.getDecoder().decode( cipher ) );
//        String restr = new String( res );
//        System.out.println( "解密：" + restr );
//
//        System.out.println( "---------------私钥签名过程------------------" );
//        String content = "ihep_这是用于签名的原始数据";
//        byte[] signed = RSAUtils.sign( RSAUtils.loadPrivateKeyFromBase64( privateKey ), content.getBytes() );
//        System.out.println( "签名原串：" + content );
//        System.out.println( "签名串：" + org.apache.commons.codec.binary.Base64.encodeBase64String( signed ) );
//        System.out.println( "验签结果：" + RSAUtils.checkSign( RSAUtils.loadPublicKeyFromBase64( publicKey ), signed, content.getBytes() ) );
//    }

//    public static void main(String[] args) {
//        String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlMYtTE8xBlM4AiwedCJRPEdyMaUA6ZnOovripQzVfZKgG/N846T5KbfnJljlz4QkLXjYUSSxu0" +
//                "+TS26koyHV4fA6WqNzKzDXE0wVJI15eIj6KWuJF0gWwdOwUHJRHZHPH46NX5uqcckY7BW8S6rFxX4Ep7xGy0xT/KqqEoteqHRndX/Idpyg8RcglS4vDkXPz" +
//                "/I4C8bxBnbVKkoYH3M1eTcaIPNRfKKCyGEQOYrpG1OVMJ2yW4Kf5bGOtYAP4ds5DzfpaYTSUjPbteHp7ry+Bp06fH3IY3NRuAU69tJpEIZSX4N9nRbfgieFUFRKMq9/XtPSHxm1AjpABNjvZpXBMQIDAQAB";
//
//        String privateKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCUxi1MTzEGUzgCLB50IlE8R3IxpQDpmc6i+uKlDNV9kqAb83zjpPkpt" +
//                "+cmWOXPhCQteNhRJLG7T5NLbqSjIdXh8Dpao3MrMNcTTBUkjXl4iPopa4kXSBbB07BQclEdkc8fjo1fm6pxyRjsFbxLqsXFfgSnvEbLTFP8qqoSi16odGd1f8h2nKDxFyCVLi8ORc" +
//                "/P8jgLxvEGdtUqShgfczV5Nxog81F8ooLIYRA5iukbU5UwnbJbgp/lsY61gA/h2zkPN+lphNJSM9u14enuvL4GnTp8fchjc1G4BTr20mkQhlJfg32dFt" +
//                "+CJ4VQVEoyr39e09IfGbUCOkAE2O9mlcExAgMBAAECggEAArVouNhige0pEnONjqNbD6YUmnooxggDiMi48IS3PJb7QKUGSaH82hVHwWVKElUTYEtQY2pPzNNsIOoD1/c/TNK2CkVKbQ9xo" +
//                "/+dLS4g93iFdvR28ZBBv0/IiI9rQgbAAtDiaqqjr9iHolE7PVSvrvq+UJQ7RPtn1kxgZi/oxlf4mKqZizKMm1dwa4irGvIIKHRllPBvunaJ9qIAUe+d2kPyCNKw58JE9lCqFvlrQEFWxTFMTP" +
//                "+TZ06uAjx4v0W3PIxqSIOxHCE1miBc3K4YOlqLNRXhz1jBs/u6+fuj2Y/alNo/qO56hfqB2lLwZNbKIpgpC6sfrH6kRuLCsgiXIQKBgQDA+WKDrtlYF5fd9kJ" +
//                "/tac0lQubINmPaeKjjgJojA9kEs92oePQC4IVejcakpG9juQ0tq7NUK61ff96UJvt0wrh4j2JmzPMoMe73maitaCeyYAActDmznnX7dFc0uJab2qxABveVPWId6iVQHgHAEp+g6nJnD91VraJ9soIxu" +
//                "/Q4QKBgQDFXTeSu5gKc7WepG0mdv1IInGRPxTE50d+ehR/pn+iYovHpO8jzQ0fog2bBzYN6qFxOzqzJi7JKUiWMFtp66KnEPJPn1XIYhBQT9o4PEwUoShRD5i1fwW4p9JWAIMBzokkN9fUc6r4kS" +
//                "/ztI9P" +
//                "/zK1wt6yGNrKx/f+TJeGnafqUQKBgHgtVyrHA1gXKV7z9CnwiPb4M0gixxrHSHuu/tT5FMSv6j" +
//                "/pRiTUZejWOeb0jIrOqFQjyOzYaFVi6G3WMaWEnuxZpxVBcJrpLjpToPuHvUXmGy8sUejLgZn140K5mnnTlVAxzylBI8AfNP/uLFG/3qS3fk+uXN7IZ9m3tc6NheBBAoGBAIrAYCN" +
//                "+UwN2GYcwXbQfDy25WVIdoQJU/oRCW0rqyOxmj5KJt73ZmJJWoW4OAciC3YD3fOsZuuadaXR9BqdOi4kcdt3XTL2Vg5aSrP3AFlSif" +
//                "++NIXTnmiQZh9wCfBe3Fd8qi0fHEbmyiJ2yoNZCuWnjWCCpLJhHQWe8fQKcr8IhAoGBAK3fgDQyjvULCVbWyHfh4pRs3QgKDI3KuX17P/Sneh4Q7jCffduzYjzsEp1PFRb3UYpNdb1WzQuRzM2w" +
//                "/IR9LwNHz97Fpwu8wBobIudqAdhqs40fdGBCNFXuCSduNI7iO6Bwd5tZPdAWlDvlO+oYNeKURcri6VS0XMySn66ueEZ3";
//        String cryptData = "H6jpGTAPWFN/gVngiq/S0vmEK23Z6VQO8NwNrwIIOSPPmzozbMnA9WeYAPLkdKHI6jSYAgRdZEihaiHSLNLrL9Q0tywxZu/TN3A5u1hsrdjYPPkeaqEeclTrLO9EVaQiY1AmRpuEgVKPm6h" +
//                "/2qP2dLsvkZI7Gn0olsCoZUZaMDRrkPREC5/rE+ESEsG750hIbztxyEdOqpJ1PtZ7xFL8FbLnfBsj/LE939BruPGQ6SQ5HQyLDvX7nw5fIjW95Nu" +
//                "+y4I9xn3kv0b5zHyj9eUJhfKVVEypozBpVU56pUMRflKlfZ6nN6D5m/iDA9j3L/uJHlJqEuwW45uu9op3fFjxAg==";
//        byte[] data = RSAUtils.decrypt( RSAUtils.loadPrivateKeyFromBase64( privateKey ), Base64.getDecoder().decode( cryptData ) );
//        System.out.println( "解密：" + new String( data ) );
//        String data = RandomStringUtils.randomAlphanumeric( 1000 );
//        System.out.println(data);
//        System.out.println(new String(decrypt( loadPublicKeyFromBase64( publicKey ),encrypt(  loadPrivateKeyFromBase64( privateKey ),data.getBytes() ) )));
//        System.out.println();
//    }

    /**
     * 随机生成密钥对。
     */
    public static KeyPair genKeyPair(int keySize) {
        // KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象S
        KeyPairGenerator keyPairGen = null;
        try {
            keyPairGen = KeyPairGenerator.getInstance( "RSA" );
        } catch (NoSuchAlgorithmException e) {
            log.error( e.getMessage(), e );
        }
        // 初始化密钥对生成器，密钥大小为96-2048位
        keyPairGen.initialize( keySize, new SecureRandom() );
        // 生成一个密钥对，保存在keyPair中
        KeyPair keyPair = keyPairGen.generateKeyPair();
        return keyPair;
    }

    /**
     * 序列化RSAKeys到byte[]
     *
     * @return
     */
    public static byte[] serializeRSAKeys(KeyPair keyPair) {
        // 得到私钥
        byte[] privateKey = keyPair.getPrivate().getEncoded();
        // 得到公钥
        byte[] publicKey = keyPair.getPublic().getEncoded();
        byte[] allKey = ByteBuffer.allocate( publicKey.length + privateKey.length + 4 ).putInt( publicKey.length ).put( publicKey ).put( privateKey ).array();
        return allKey;
    }

    /**
     * 反序列化RSAKey。
     *
     * @param allKeyData
     * @return
     */
    public static Pair<RSAPublicKey, RSAPrivateKey> deserializeRSAKeys(byte[] allKeyData) {
        ByteBuffer bb = ByteBuffer.wrap( allKeyData );
        int publicKeySize = bb.getInt();
        byte[] publicKeyData = new byte[publicKeySize];
        byte[] privateKeyData = new byte[allKeyData.length - 4 - publicKeySize];
        bb.get( publicKeyData );
        bb.get( privateKeyData );
        return new ImmutablePair<>( loadPublicKeyFromBytes( publicKeyData ), loadPrivateKeyFromBytes( privateKeyData ) );
    }


}
