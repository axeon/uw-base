package uw.mfa.totp;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Writer;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;
import uw.common.response.ResponseData;
import uw.mfa.constant.HmacAlgorithm;
import uw.mfa.constant.MfaResponseCode;
import uw.mfa.totp.vo.TotpSecretData;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * TOTP密钥数据生成器。
 * <p>负责生成Base32编码的随机密钥、构造otpauth URI、可选生成二维码PNG（Base64），</p>
 * <p>遵循Google Authenticator的Key-Uri-Format规范。</p>
 */
public class TotpSecretDataGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TotpSecretDataGenerator.class);
    /**
     * 二维码生成器（ZXing QRCodeWriter，线程安全）。
     */
    private final Writer writer = new QRCodeWriter();
    /**
     * 随机数生成器（线程局部）。
     */
    private final Random randomBytes = ThreadLocalRandom.current();
    /**
     * Base32编码器。
     */
    private final Base32 encoder = new Base32();

    /**
     * 默认签发者。
     */
    private final String issuer;

    /**
     * HMAC哈希算法。
     */
    private final HmacAlgorithm hashingAlgorithm;

    /**
     * 密钥长度（Base32字符数）。
     */
    private final int secretLength;

    /**
     * 验证码位数。
     */
    private final int codeLength;

    /**
     * 时间窗口（秒）。
     */
    private final int timePeriod;

    /**
     * 是否生成二维码图片。
     */
    private final boolean enableGenQr;

    /**
     * 二维码图片尺寸（像素）。
     */
    private final int qrSize;

    /**
     * 构造器。
     *
     * @param issuer           默认签发者
     * @param hashingAlgorithm HMAC算法
     * @param secretLength     密钥长度（Base32字符数）
     * @param codeLength       验证码位数
     * @param timePeriod       时间窗口（秒）
     * @param enableGenQr      是否生成二维码
     * @param qrSize           二维码尺寸
     */
    public TotpSecretDataGenerator(String issuer, HmacAlgorithm hashingAlgorithm, int secretLength, int codeLength, int timePeriod, boolean enableGenQr, int qrSize) {
        this.issuer = issuer;
        this.hashingAlgorithm = hashingAlgorithm;
        this.secretLength = secretLength;
        this.codeLength = codeLength;
        this.timePeriod = timePeriod;
        this.enableGenQr = enableGenQr;
        this.qrSize = qrSize;
    }

    /**
     * 签发TOTP密钥数据。
     * <p>生成随机密钥与otpauth URI，按配置可选生成二维码PNG（Base64）。</p>
     *
     * @param label  标签（通常为用户标识，如 "user:123"）
     * @param issuer 签发者，为空时使用构造器默认值
     * @param qrSize 二维码尺寸，小于100时使用构造器默认值
     * @return 包含密钥、URI、二维码的TotpSecretData
     */
    public ResponseData<TotpSecretData> issue(String label, String issuer, int qrSize) {
        if (StringUtils.isBlank(issuer)) {
            issuer = this.issuer;
        }
        if (qrSize < 100) {
            qrSize = this.qrSize;
        }
        String secret = generateSecret(secretLength);
        String totpUri = getTotpUri(issuer, label, secret, hashingAlgorithm.getLabel(), codeLength, timePeriod);
        // 不生成二维码则直接返回，让前端生成。
        if (!enableGenQr) {
            return ResponseData.success(new TotpSecretData(secret, totpUri, null));
        } else {
            try {
                BitMatrix bitMatrix = writer.encode(totpUri, BarcodeFormat.QR_CODE, qrSize, qrSize);
                ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
                return ResponseData.success(new TotpSecretData(secret, totpUri, Base64.getEncoder().encodeToString(pngOutputStream.toByteArray())));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return ResponseData.errorCode(MfaResponseCode.TOTP_SECRET_GEN_ERROR, e.getMessage());
            }
        }
    }

    /**
     * 生成TOTP二维码Uri（otpauth格式）。
     *
     * @param issuer    签发者
     * @param label     标签
     * @param secret    Base32密钥
     * @param algorithm 算法标签
     * @param digits    验证码位数
     * @param period    时间窗口
     * @return otpauth URI，格式见 https://github.com/google/google-authenticator/wiki/Key-Uri-Format
     */
    private String getTotpUri(String issuer, String label, String secret, String algorithm, int digits, int period) {
        return "otpauth://totp/" +
                uriEncode(label) + "?" +
                "secret=" + uriEncode(secret) +
                "&issuer=" + uriEncode(issuer) +
                "&algorithm=" + uriEncode(algorithm) +
                "&digits=" + digits +
                "&period=" + period;
    }

    /**
     * 生成指定长度的Base32随机密钥。
     *
     * @param secretLength 密钥长度（Base32字符数）
     * @return Base32编码的密钥字符串
     */
    private String generateSecret(int secretLength) {
        // 5 bits per char in base32
        byte[] bytes = new byte[(secretLength * 5) / 8];
        randomBytes.nextBytes(bytes);
        return new String(encoder.encode(bytes));
    }

    /**
     * URI编码（UTF-8），null返回空串。
     *
     * @param text 待编码文本
     * @return 编码后字符串
     */
    private String uriEncode(String text) {
        if (text == null) {
            return "";
        }
        return UriUtils.encode(text, StandardCharsets.UTF_8);
    }
}
