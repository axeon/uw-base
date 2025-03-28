package uw.mfa.totp;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Writer;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.commons.codec.binary.Base32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.dto.ResponseData;
import uw.mfa.constant.HashingAlgorithm;
import uw.mfa.totp.vo.TotpSecretData;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 二维码签发器。
 */
public class TotpSecretDataGenerator {

    private static final Logger logger = LoggerFactory.getLogger( TotpSecretDataGenerator.class );
    /**
     * 二维码生成器。
     */
    private final Writer writer = new QRCodeWriter();
    /**
     * 随机数生成器.
     */
    private final Random randomBytes = ThreadLocalRandom.current();
    /**
     * Base32编码器.
     */
    private final Base32 encoder = new Base32();

    /**
     * Hashing算法.
     */
    private final HashingAlgorithm hashingAlgorithm;

    /**
     * 密钥长度.
     */
    private final int secretLength;

    /**
     * CODE长度.
     */
    private final int codeLength;

    /**
     * 时间间隔(s).
     */
    private final int timePeriod;

    /**
     * 是否生成二维码.
     */
    private final boolean isGenQr;

    public TotpSecretDataGenerator(HashingAlgorithm hashingAlgorithm, int secretLength, int codeLength, int timePeriod, boolean isGenQr) {
        this.hashingAlgorithm = hashingAlgorithm;
        this.secretLength = secretLength;
        this.codeLength = codeLength;
        this.timePeriod = timePeriod;
        this.isGenQr = isGenQr;
    }

    /**
     * 生成二维码。
     *
     * @param issuer 签发者
     * @param label  标签
     * @param qrSize 二维码尺寸。
     * @return
     */
    public ResponseData<TotpSecretData> issue(String issuer, String label, int qrSize) {
        String secret = generateSecret( secretLength );
        String totpUri = getTotpUri( issuer, label, secret, hashingAlgorithm.getLabel(), codeLength, timePeriod );
        // 不生成二维码则直接返回，让前端生成。
        if (!isGenQr) {
            return ResponseData.success( new TotpSecretData( secret, totpUri, null ) );
        } else {
            try {
                BitMatrix bitMatrix = writer.encode( totpUri, BarcodeFormat.QR_CODE, qrSize, qrSize );
                ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream( bitMatrix, "PNG", pngOutputStream );
                return ResponseData.success( new TotpSecretData( secret, totpUri, Base64.getEncoder().encodeToString( pngOutputStream.toByteArray() ) ) );
            } catch (Exception e) {
                logger.error( e.getMessage(), e );
                return ResponseData.errorMsg( e.getMessage() );
            }
        }
    }

    /**
     * 生成TOTP二维码Uri。
     *
     * @return The URI/message to encode into the QR image, in the format specified here:
     * https://github.com/google/google-authenticator/wiki/Key-Uri-Format
     */
    private String getTotpUri(String issuer, String label, String secret, String algorithm, int digits, int period) {
        return "otpauth://totp/" +
                uriEncode( label ) + "?" +
                "secret=" + uriEncode( secret ) +
                "&issuer=" + uriEncode( issuer ) +
                "&algorithm=" + uriEncode( algorithm ) +
                "&digits=" + digits +
                "&period=" + period;
    }

    /**
     * 生成密钥.
     *
     * @return
     */
    private String generateSecret(int secretLength) {
        // 5 bits per char in base32
        byte[] bytes = new byte[(secretLength * 5) / 8];
        randomBytes.nextBytes( bytes );
        return new String( encoder.encode( bytes ) );
    }

    /**
     * URI编码.
     *
     * @param text
     * @return
     */
    private String uriEncode(String text) {
        if (text == null) {
            return "";
        }
        return URLEncoder.encode( text, StandardCharsets.UTF_8 ).replaceAll( "\\+", "%20" );
    }
}
