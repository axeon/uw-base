package uw.mfa;

import uw.common.dto.ResponseData;
import uw.mfa.captcha.vo.CaptchaQuestion;
import uw.mfa.helper.MfaCaptchaHelper;
import uw.mfa.helper.MfaDeviceCodeHelper;
import uw.mfa.helper.MfaIPLimitHelper;
import uw.mfa.helper.MfaTotpHelper;
import uw.mfa.totp.vo.TotpSecretData;

/**
 * 一个融合MFA帮助类。
 * 输出和检测Captcha，DeviceCode前后对IP限制进行了检查。
 */
public class MfaFusionHelper {

    /**
     * 检查IP是否在白名单中。
     *
     * @param ip
     * @return
     */
    public static boolean checkIpWhiteList(String ip) {
        return MfaIPLimitHelper.checkIpWhiteList( ip );
    }

    /**
     * 检查IP错误限制。
     * 如果有报错记录，就开始输出warn。
     * 当超过报错限制，则输出error。
     *
     * @param ip
     * @return
     */
    public static ResponseData checkIpErrorLimit(String ip) {
        return MfaIPLimitHelper.checkIpErrorLimit( ip );
    }

    /**
     * 递增错误次数。
     * 当调用程序判定出错需要限制用户行为时，则需要调用此方法。
     *
     * @param ip
     */
    public static void incrementIpErrorTimes(String ip, String remark) {
        MfaIPLimitHelper.incrementIpErrorTimes( ip, remark );
    }

    /**
     * 清除IP错误限制。
     *
     * @param ip
     */
    public static void clearIpErrorLimit(String ip) {
        MfaIPLimitHelper.clearIpErrorLimit( ip );
    }

    /**
     * 统计限制信息条数，此信息直接从redis输出。
     *
     * @return
     */
    public static long countLimitInfo() {
        return MfaIPLimitHelper.countLimitInfo();
    }

    /**
     * 生成captcha。
     * 此方法内会对IP限制进行检测，如果warn才会输出captcha，否则不会输出。
     *
     * @param captchaId
     */
    public static ResponseData<CaptchaQuestion> generateCaptcha(String userIp, String captchaId) {
        ResponseData checkData = MfaIPLimitHelper.checkIpErrorLimit( userIp );
        if (checkData.isWarn()) {
            ResponseData<CaptchaQuestion> captchaData = MfaCaptchaHelper.generateCaptcha( userIp, captchaId );
            if (captchaData.isSuccess()) {
                return ResponseData.warn( captchaData.getData(), checkData.getCode(), checkData.getMsg() );
            } else {
                return captchaData;
            }
        } else {
            return checkData;
        }
    }

    /**
     * 验证captcha。
     * 调用此方法需要特别注意，不能直接外部暴露，否则会被重试攻击。
     *
     * @param captchaId
     * @return
     */
    public static ResponseData verifyCaptcha(String userIp, String captchaId, String captchaSign) {
        //检查IP限制。
        ResponseData checkData = MfaIPLimitHelper.checkIpErrorLimit( userIp );
        if (checkData.isWarn()) {
            // 检查captcha。
            checkData = MfaCaptchaHelper.verifyCaptcha( userIp, captchaId, captchaSign );
        }
        return checkData;
    }

    /**
     * 发送验证码。
     * 此方法内会对IP限制进行检测，如果warn会进行captcha检测，否则不会检测。
     *
     * @param deviceType 登录类型
     * @param deviceId
     */
    public static ResponseData sendDeviceCode(String userIp, long saasId, int deviceType, String deviceId, String captchaId, String captchaSign) {
        return sendDeviceCode( userIp, saasId, deviceType, deviceId, captchaId, captchaSign, 0, null, null );
    }

    /**
     * 发送验证码。
     * 此方法内会对IP限制进行检测，如果warn会进行captcha检测，否则不会检测。
     *
     * @param deviceType 登录类型
     * @param deviceId
     */
    public static ResponseData sendDeviceCode(String userIp, long saasId, int deviceType, String deviceId, String captchaId, String captchaSign, int codeLen) {
        return sendDeviceCode( userIp, saasId, deviceType, deviceId, captchaId, captchaSign, codeLen, null, null );
    }

    /**
     * 发送验证码。
     * 此方法内会对IP限制进行检测，如果warn会进行captcha检测，否则不会检测。
     *
     * @param deviceType 登录类型
     * @param deviceId
     */
    public static ResponseData sendDeviceCode(String userIp, long saasId, int deviceType, String deviceId, String captchaId, String captchaSign, int codeLen,
                                              String notifySubject, String notifyContent) {
        //检查IP限制。
        ResponseData checkData = MfaIPLimitHelper.checkIpErrorLimit( userIp );
        if (checkData.isError()) {
            return checkData;
        } else if (checkData.isWarn()) {
            // 先检查captcha。
            checkData = MfaCaptchaHelper.verifyCaptcha( userIp, captchaId, captchaSign );
            if (checkData.isNotSuccess()) {
                return checkData;
            }
        }
        return MfaDeviceCodeHelper.sendDeviceCode( userIp, saasId, deviceType, deviceId, codeLen, notifySubject, notifyContent );
    }


    /**
     * 检查设备识别码。
     * 如果识别错误，则直接递增IP错误。
     *
     * @return
     */
    public static ResponseData verifyDeviceCode(String userIp, int deviceType, String deviceId, String deviceCode) {
        //检查IP限制。
        ResponseData checkData = MfaIPLimitHelper.checkIpErrorLimit( userIp );
        if (!checkData.isError()) {
            // 检查captcha。
            checkData = MfaDeviceCodeHelper.verifyDeviceCode( userIp, deviceType, deviceId, deviceCode );
            if (checkData.isNotSuccess()) {
                MfaIPLimitHelper.incrementIpErrorTimes( userIp, checkData.getCode() );
            }
        }
        return checkData;
    }

    /**
     * 生成totp密钥。
     *
     * @param label
     * @return
     */
    public static ResponseData<TotpSecretData> issueTotpSecret(String label) {
        return MfaTotpHelper.issue( label);
    }

    /**
     * 生成totp密钥。
     *
     * @param label
     * @param issuer
     * @param qrSize
     * @return
     */
    public static ResponseData<TotpSecretData> issueTotpSecret(String label, String issuer, int qrSize) {
        return MfaTotpHelper.issue( label, issuer, qrSize );
    }

    /**
     * 验证totp密钥。
     *
     * @param secret
     * @param code
     * @return
     */
    public static ResponseData verifyTotpCode(String secret, String code) {
        return MfaTotpHelper.verifyCode( secret, code );
    }

    /**
     * 生成totp密钥。
     *
     * @param amount
     * @return
     */
    public static String[] generateRecoveryCode(int amount) {
        return MfaTotpHelper.generateRecoveryCode( amount );
    }

}
