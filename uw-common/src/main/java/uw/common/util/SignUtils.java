package uw.common.util;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 签名工具类
 * <p>
 * *
 */
public class SignUtils {

    /**
     * 签名工具。
     *
     * @param msg
     * @param type
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String sign(String msg, SignType type) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance( type.name() );
        messageDigest.update( msg.getBytes() );
        byte[] encode = messageDigest.digest();
        return Base64.getEncoder().encodeToString( encode );
    }

    public enum SignType {
        MD5, SHA
    }
}
