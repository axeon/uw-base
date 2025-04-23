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
 * TOTP验证码帮助类。
 */
public class MfaTotpHelper {

    private static final Logger log = LoggerFactory.getLogger( MfaTotpHelper.class );

    /**
     * MFA配置。
     */
    private static UwMfaProperties uwMfaProperties;

    /**
     * Totp签发器。
     */
    private static TotpSecretDataGenerator totpSecretDataGenerator;

    /**
     * Totp验证器。
     */
    private static TotpCodeVerifier totpCodeVerifier;

    public MfaTotpHelper(UwMfaProperties uwMfaProperties) {
        MfaTotpHelper.uwMfaProperties = uwMfaProperties;
        MfaTotpHelper.totpCodeVerifier = new TotpCodeVerifier( uwMfaProperties.getTotpAlgorithm(), uwMfaProperties.getTotpCodeLength(), uwMfaProperties.getTotpTimePeriod(),
                uwMfaProperties.getTotpTimePeriodDiscrepancy() );
        MfaTotpHelper.totpSecretDataGenerator = new TotpSecretDataGenerator( uwMfaProperties.getTotpIssuer(), uwMfaProperties.getTotpAlgorithm(),
                uwMfaProperties.getTotpSecretLength(), uwMfaProperties.getTotpCodeLength(), uwMfaProperties.getTotpTimePeriod(), uwMfaProperties.isTotpGenQr(),
                uwMfaProperties.getTotpQrSize() );
    }

    /**
     * 签发Totp密钥数据。
     *
     * @param label
     * @return
     */
    public static ResponseData<TotpSecretData> issue(String label) {
        return totpSecretDataGenerator.issue( label, null, 0 );
    }

    /**
     * 签发Totp密钥数据。
     *
     * @param label  标签
     * @param issuer 签发人
     * @param qrSize 二维码尺寸
     * @return
     */
    public static ResponseData<TotpSecretData> issue(String label, String issuer, int qrSize) {
        return totpSecretDataGenerator.issue( label, issuer, qrSize );
    }

    /**
     * 验证Totp密钥数据。
     *
     * @param totpSecret 密钥
     * @param totpCode   验证码
     * @return
     */
    public static ResponseData verifyCode(String totpSecret, String totpCode) {
        return totpCodeVerifier.verifyCode( totpSecret, totpCode );
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
