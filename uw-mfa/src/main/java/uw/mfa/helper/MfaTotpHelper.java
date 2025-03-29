package uw.mfa.helper;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.dto.ResponseData;
import uw.mfa.conf.UwMfaProperties;
import uw.mfa.totp.ToptRecoveryCodeGenerator;
import uw.mfa.totp.TotpCodeVerifier;
import uw.mfa.totp.TotpSecretDataGenerator;
import uw.mfa.totp.vo.TotpSecretData;

/**
 * 设备验证码帮助类。
 */
public class MfaTotpHelper {

    private static final Logger log = LoggerFactory.getLogger( MfaTotpHelper.class );

    /**
     * Totp签发器。
     */
    private static TotpSecretDataGenerator totpSecretDataGenerator;

    /**
     * Totp验证器。
     */
    private static TotpCodeVerifier totpCodeVerifier;

    public MfaTotpHelper(UwMfaProperties uwMfaProperties) {
        totpCodeVerifier = new TotpCodeVerifier( uwMfaProperties.getTotpAlgorithm(), uwMfaProperties.getTotpCodeLength(), uwMfaProperties.getTotpTimePeriod(),
                uwMfaProperties.getTotpTimePeriodDiscrepancy() );
        totpSecretDataGenerator = new TotpSecretDataGenerator( uwMfaProperties.getTotpAlgorithm(), uwMfaProperties.getTotpSecretLength(), uwMfaProperties.getTotpCodeLength(),
                uwMfaProperties.getTotpTimePeriod(), uwMfaProperties.isToptGenQr() );
    }

    /**
     * 签发Totp密钥数据。
     *
     * @param issuer 签发人
     * @param label  标签
     * @param qrSize 二维码尺寸
     * @return
     */
    public static ResponseData<TotpSecretData> issue(String issuer, String label, int qrSize) {
        return totpSecretDataGenerator.issue( issuer, label, qrSize );
    }

    /**
     * 验证Totp密钥数据。
     *
     * @param secret 密钥
     * @param code   验证码
     * @return
     */
    public static ResponseData verifyCode(String secret, String code) {
        return totpCodeVerifier.verifyCode( secret, code );
    }

    /**
     * 生成16位随机恢复码。
     *
     * @return
     */
    public static String[] generateRecoveryCode(int amount) {
        return ToptRecoveryCodeGenerator.generateCodes( amount );
    }

}
