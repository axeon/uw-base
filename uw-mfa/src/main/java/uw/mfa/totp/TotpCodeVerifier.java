package uw.mfa.totp;


import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.CodeGenerationException;
import uw.common.dto.ResponseData;
import uw.mfa.constant.HmacAlgorithm;
import uw.mfa.constant.MfaResponseCode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

/**
 * TOTP Code Verifier.
 *
 * @author axeon
 */
public class TotpCodeVerifier {
    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger( TotpCodeVerifier.class );
    /**
     * Hashing algorithm.
     */
    private final HmacAlgorithm algorithm;
    /**
     * 代码位数。
     */
    private final int digits;
    /**
     * 时间周期(s)。
     */
    private final int timePeriod;

    /**
     * 时间周期偏移量。
     */
    private final int allowedTimePeriodDiscrepancy;

    /**
     * 构造器。
     * @param algorithm
     * @param digits
     * @param timePeriod
     * @param allowedTimePeriodDiscrepancy
     */
    public TotpCodeVerifier(HmacAlgorithm algorithm, int digits, int timePeriod, int allowedTimePeriodDiscrepancy) {
        this.algorithm = algorithm;
        this.digits = digits;
        this.timePeriod = timePeriod;
        this.allowedTimePeriodDiscrepancy = allowedTimePeriodDiscrepancy;
    }

    /**
     * 检查代码是否正确。
     * @param secret
     * @param code
     * @return
     */
    public ResponseData verifyCode(String secret, String code) {
        if (StringUtils.isBlank( secret)){
            return ResponseData.errorCode( MfaResponseCode.TOTP_SECRET_LOST_ERROR );
        }
        if (StringUtils.isBlank( code )) {
            return ResponseData.errorCode( MfaResponseCode.TOTP_CODE_LOST_ERROR );
        }
        // 获取当前时间戳，并计算已过的周期数。
        long currentBucket = Math.floorDiv( Instant.now().getEpochSecond(), timePeriod );

        // 计算并比较所有“有效”时间周期的代码。
        // 即使提前匹配到一个有效代码，也要继续计算和比较所有有效时间周期的代码，以避免定时攻击。
        boolean success = false;
        for (int i = -allowedTimePeriodDiscrepancy; i <= allowedTimePeriodDiscrepancy; i++) {
            success = checkCode( secret, currentBucket + i, code );
            if (success) {
                break;
            }
        }
        if (!success) {
            return ResponseData.errorCode( MfaResponseCode.TOTP_CODE_VERIFY_ERROR );
        }else{
            return ResponseData.success();
        }
    }

    /**
     * 检查一个代码是否与给定的密钥和计数器匹配。
     */
    private boolean checkCode(String secret, long counter, String code) {
        try {
            String actualCode = generate( secret, counter );
            return timeSafeStringComparison( actualCode, code );
        } catch (CodeGenerationException e) {
            return false;
        }
    }

    /**
     * 比较两个字符串是否相等，而不泄露时间信息。
     */
    private boolean timeSafeStringComparison(String a, String b) {
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();

        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }

        return result == 0;
    }

    /**
     * 生成代码。
     * @param key
     * @param counter
     * @return
     * @throws CodeGenerationException
     */
    public String generate(String key, long counter) throws CodeGenerationException {
        try {
            byte[] hash = generateHash( key, counter );
            return getDigitsFromHash( hash );
        } catch (Exception e) {
            logger.error( e.getMessage(), e );
            return null;
        }
    }


    /**
     * 生成计数器数值的 HMAC-SHA1 哈希值。
     */
    private byte[] generateHash(String key, long counter) throws InvalidKeyException, NoSuchAlgorithmException {
        byte[] data = new byte[8];
        long value = counter;
        for (int i = 8; i-- > 0; value >>>= 8) {
            data[i] = (byte) value;
        }

        // Create a HMAC-SHA1 signing key from the shared key
        Base32 codec = new Base32();
        byte[] decodedKey = codec.decode( key );
        SecretKeySpec signKey = new SecretKeySpec( decodedKey, algorithm.getValue() );
        Mac mac = Mac.getInstance( algorithm.getValue() );
        mac.init( signKey );

        // Create a hash of the counter value
        return mac.doFinal( data );
    }

    /**
     * 从给定的哈希值中提取一个n位数字的代码。
     */
    private String getDigitsFromHash(byte[] hash) {
        int offset = hash[hash.length - 1] & 0xF;

        long truncatedHash = 0;

        for (int i = 0; i < 4; ++i) {
            truncatedHash <<= 8;
            truncatedHash |= (hash[offset + i] & 0xFF);
        }

        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= (long) Math.pow( 10, digits );

        // Left pad with 0s for a n-digit code
        return String.format( "%0" + digits + "d", truncatedHash );
    }
}