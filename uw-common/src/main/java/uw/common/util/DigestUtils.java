package uw.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Digest工具类（支持多种安全摘要算法）
 */
public class DigestUtils {

    /**
     * Digest工具（支持多种安全算法）
     *
     * @param msg       需要Digest的原始字符串
     * @param algorithm Digest算法类型
     * @return Base64编码的Digest结果
     * @throws IllegalArgumentException 当Digest类型无效时抛出
     */
    public static String sign(String msg, Algorithm algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException( "Digest类型不能为空" );
        }

        try {
            MessageDigest digest = MessageDigest.getInstance( algorithm.getAlgorithm() );
            byte[] hash = digest.digest( msg.getBytes() );
            return Base64.getEncoder().encodeToString( hash );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException( "无效的Digest类型: " + algorithm, e );
        }
    }

    /**
     * Digest工具（支持多种安全算法）.
     *
     * @param msg
     * @param algorithm
     * @return
     */
    public static String signHex(String msg, Algorithm algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException( "Digest类型不能为空" );
        }

        try {
            MessageDigest digest = MessageDigest.getInstance( algorithm.getAlgorithm() );
            byte[] hash = digest.digest( msg.getBytes() );
            return bytesToHex( hash );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException( "无效的Digest类型: " + algorithm, e );
        }
    }

    /**
     * 将字节数组转为十六进制字符串
     *
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append( String.format( "%02x", b ) );
        }
        return sb.toString();
    }

    /**
     * Digest类型枚举（按安全性排序，推荐使用SHA-256或更高）
     */
    public enum Algorithm {
        MD5( "MD5", "不安全，仅用于兼容旧系统" ),
        SHA( "SHA-1", "较弱，建议仅用于历史系统" ),
        SHA_256( "SHA-256", "推荐使用" ),
        SHA_384( "SHA-384", "高强度" ),
        SHA_512( "SHA-512", "最高强度" ),
        SHA3_256( "SHA3-256", "最新标准，推荐使用" ),
        SHA3_512( "SHA3-512", "最新标准，最高强度" );

        /**
         * Digest算法名称
         */
        private final String algorithm;

        /**
         * Digest算法描述
         */
        private final String description;

        Algorithm(String algorithm, String description) {
            this.algorithm = algorithm;
            this.description = description;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public String getDescription() {
            return description;
        }
    }
}
