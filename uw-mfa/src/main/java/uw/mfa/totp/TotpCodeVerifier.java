package uw.mfa.totp;


import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.CodeGenerationException;
import uw.common.response.ResponseData;
import uw.mfa.constant.HmacAlgorithm;
import uw.mfa.constant.MfaResponseCode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * TOTP验证码校验器。
 * <p>实现RFC 6238基于时间的一次性密码（TOTP）校验，</p>
 * <p>允许配置时间窗口偏移量以容忍客户端与服务端时钟不同步。</p>
 *
 * @author axeon
 */
public class TotpCodeVerifier {
    /**
     * Logger。
     */
    private static final Logger logger = LoggerFactory.getLogger(TotpCodeVerifier.class);
    /**
     * HMAC哈希算法。
     */
    private final HmacAlgorithm algorithm;
    /**
     * 验证码位数。
     */
    private final int digits;
    /**
     * 时间周期（秒），默认30s。
     */
    private final int timePeriod;

    /**
     * 允许的时间周期偏移量（前后各N个窗口），默认2。
     */
    private final int allowedTimePeriodDiscrepancy;

    /**
     * 构造器。
     *
     * @param algorithm                     HMAC算法
     * @param digits                        验证码位数
     * @param timePeriod                    时间周期（秒）
     * @param allowedTimePeriodDiscrepancy  允许的时间窗口偏移量
     */
    public TotpCodeVerifier(HmacAlgorithm algorithm, int digits, int timePeriod, int allowedTimePeriodDiscrepancy) {
        this.algorithm = algorithm;
        this.digits = digits;
        this.timePeriod = timePeriod;
        this.allowedTimePeriodDiscrepancy = allowedTimePeriodDiscrepancy;
    }

    /**
     * 校验TOTP验证码。
     * <p>在 [-discrepancy, +discrepancy] 窗口范围内逐一比对，全部计算完成后再判定以规避定时攻击。</p>
     *
     * @param secret Base32编码的密钥
     * @param code   待校验的验证码
     * @return 校验成功返回success，密钥/验证码缺失或校验失败返回对应errorCode
     */
    public ResponseData verifyCode(String secret, String code) {
        if (StringUtils.isBlank(secret)) {
            return ResponseData.errorCode(MfaResponseCode.TOTP_SECRET_LOST_ERROR);
        }
        if (StringUtils.isBlank(code)) {
            return ResponseData.errorCode(MfaResponseCode.TOTP_CODE_LOST_ERROR);
        }
        // 获取当前时间戳，并计算已过的周期数。
        long currentBucket = Math.floorDiv(Instant.now().getEpochSecond(), timePeriod);

        // 计算并比较所有”有效”时间周期的代码。
        // 即使提前匹配到一个有效代码，也要继续计算和比较所有有效时间周期的代码，以避免定时攻击。
        boolean success = false;
        for (int i = -allowedTimePeriodDiscrepancy; i <= allowedTimePeriodDiscrepancy; i++) {
            success |= checkCode(secret, currentBucket + i, code);
        }
        if (!success) {
            return ResponseData.errorCode(MfaResponseCode.TOTP_CODE_VERIFY_ERROR);
        } else {
            return ResponseData.success();
        }
    }

    /**
     * 检查验证码是否与给定密钥和时间计数器匹配。
     *
     * @param secret  Base32编码的密钥
     * @param counter 时间计数器（当前秒数/时间周期）
     * @param code    待校验的验证码
     * @return 匹配返回true；生成失败、非法密钥或比对不一致返回false
     */
    private boolean checkCode(String secret, long counter, String code) {
        try {
            String actualCode = generate(secret, counter);
            if (actualCode == null) {
                return false;
            }
            return timeSafeStringComparison(actualCode, code);
        } catch (RuntimeException e) {
            // 非法密钥(Base32解码异常)等运行时异常视为校验失败, 而非抛500
            return false;
        }
    }

    /**
     * 常量时间比较两个字符串是否相等，避免通过响应耗时泄露字符信息（定时攻击防护）。
     *
     * @param a 字符串a
     * @param b 字符串b
     * @return 相等返回true，长度不同或内容不一致返回false
     */
    private boolean timeSafeStringComparison(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

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
     * 根据密钥和时间计数器生成TOTP验证码。
     *
     * @param key     Base32编码的密钥
     * @param counter 时间计数器
     * @return 生成的验证码字符串；生成异常返回null
     * @throws CodeGenerationException 生成过程中的异常（实际异常被捕获并返回null）
     */
    public String generate(String key, long counter) throws CodeGenerationException {
        try {
            byte[] hash = generateHash(key, counter);
            return getDigitsFromHash(hash);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }


    /**
     * 生成计数器数值的HMAC哈希值。
     *
     * @param key     Base32编码的密钥
     * @param counter 时间计数器
     * @return HMAC哈希字节数组
     * @throws InvalidKeyException       密钥非法
     * @throws NoSuchAlgorithmException  算法不支持
     */
    private byte[] generateHash(String key, long counter) throws InvalidKeyException, NoSuchAlgorithmException {
        byte[] data = new byte[8];
        long value = counter;
        for (int i = 8; i-- > 0; value >>>= 8) {
            data[i] = (byte) value;
        }

        // Create a HMAC-SHA1 signing key from the shared key
        Base32 codec = new Base32();
        byte[] decodedKey = codec.decode(key);
        SecretKeySpec signKey = new SecretKeySpec(decodedKey, algorithm.getValue());
        Mac mac = Mac.getInstance(algorithm.getValue());
        mac.init(signKey);

        // Create a hash of the counter value
        return mac.doFinal(data);
    }

    /**
     * 从HMAC哈希值中动态截断并提取指定位数的数字验证码（RFC 4226动态截断算法）。
     *
     * @param hash HMAC哈希字节数组
     * @return 左侧补零的n位数字验证码字符串
     */
    private String getDigitsFromHash(byte[] hash) {
        int offset = hash[hash.length - 1] & 0xF;

        long truncatedHash = 0;

        for (int i = 0; i < 4; ++i) {
            truncatedHash <<= 8;
            truncatedHash |= (hash[offset + i] & 0xFF);
        }

        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= (long) Math.pow(10, digits);

        // Left pad with 0s for a n-digit code
        return String.format("%0" + digits + "d", truncatedHash);
    }
}