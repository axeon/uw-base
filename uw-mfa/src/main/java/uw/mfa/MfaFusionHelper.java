package uw.mfa;

import uw.common.response.ResponseData;
import uw.mfa.captcha.vo.CaptchaQuestion;
import uw.mfa.helper.MfaCaptchaHelper;
import uw.mfa.helper.MfaDeviceCodeHelper;
import uw.mfa.helper.MfaIPLimitHelper;
import uw.mfa.helper.MfaTotpHelper;
import uw.mfa.totp.vo.TotpSecretData;

import java.util.Set;

/**
 * MFA融合帮助类（对外统一入口）。
 * <p>融合IP限制、Captcha、设备验证码、TOTP四种认证能力，所有方法均为静态方法。</p>
 * <p>核心约定：Captcha生成与设备码/TOTP校验方法在执行前后会对IP限制进行检查，</p>
 * <p>warn状态触发Captcha要求，error状态直接拦截，白名单IP全程豁免。</p>
 */
public class MfaFusionHelper {

    /**
     * 检查IP是否在白名单中。
     *
     * @param ip 用户IP
     * @return 在白名单返回true，否则false
     */
    public static boolean checkIpWhiteList(String ip) {
        return MfaIPLimitHelper.checkIpWhiteList(ip);
    }

    /**
     * 检查IP错误限制。
     * <p>白名单IP直接放行；错误次数达warnTimes返回warn（需验证码），达errorTimes返回error（已屏蔽）。</p>
     *
     * @param ip 用户IP
     * @return success/warn/error 三态ResponseData
     */
    public static ResponseData checkIpErrorLimit(String ip) {
        return MfaIPLimitHelper.checkIpErrorLimit(ip);
    }

    /**
     * 递增IP错误次数。
     * <p>当调用方判定业务出错（如密码错误）需要限制用户行为时调用，白名单IP不计入。</p>
     *
     * @param ip     用户IP
     * @param remark 备注（错误码等，预留）
     */
    public static void incrementIpErrorTimes(String ip, String remark) {
        MfaIPLimitHelper.incrementIpErrorTimes(ip, remark);
    }

    /**
     * 清除IP错误限制。
     *
     * @param ip 用户IP
     * @return 删除成功返回true
     */
    public static boolean clearIpErrorLimit(String ip) {
        return MfaIPLimitHelper.clearIpErrorLimit(ip);
    }

    /**
     * 统计MFA限制信息条数（基于Redis dbSize）。
     *
     * @return 信息条数
     */
    public static long countMfaInfo() {
        return MfaIPLimitHelper.countMfaInfo();
    }

    /**
     * 获取IP错误限制列表。
     *
     * @return IP集合
     */
    public static Set<String> getIpErrorLimitList() {
        return MfaIPLimitHelper.getIpErrorLimitList();
    }

    /**
     * 获取Captcha发送限制列表。
     *
     * @return IP集合
     */
    public static Set<String> getCaptchaSendLimitList() {
        return MfaCaptchaHelper.getSendLimitList();
    }

    /**
     * 清除Captcha发送限制。
     *
     * @param ip 用户IP
     * @return 删除成功返回true
     */
    public static boolean clearCaptchaSendLimit(String ip) {
        return MfaCaptchaHelper.clearSendLimit(ip);
    }

    /**
     * 获取设备验证码发送限制列表。
     *
     * @return IP集合
     */
    public static Set<String> getDeviceCodeSendLimitList() {
        return MfaDeviceCodeHelper.getSendLimitList();
    }

    /**
     * 获取设备验证码校验限制列表。
     *
     * @return 设备ID集合
     */
    public static Set<String> getDeviceCodeVerifyLimitList() {
        return MfaDeviceCodeHelper.getVerifyErrorList();
    }

    /**
     * 清除设备验证码发送限制。
     *
     * @param ip 用户IP
     * @return 删除成功返回true
     */
    public static boolean clearDeviceCodeSendLimit(String ip) {
        return MfaDeviceCodeHelper.clearSendLimit(ip);
    }

    /**
     * 清除设备验证码校验限制。
     *
     * @param deviceId 设备ID
     * @return 删除成功返回true
     */
    public static boolean clearDeviceCodeVerifyLimit(String deviceId) {
        return MfaDeviceCodeHelper.clearVerifyLimit(deviceId);
    }

    /**
     * 清除TOTP校验限制。
     *
     * @param userInfo 用户标识
     * @return 删除成功返回true
     */
    public static boolean clearTotpVerifyLimit(String userInfo) {
        return MfaTotpHelper.clearVerifyLimit(userInfo);
    }

    /**
     * 获取TOTP校验限制列表。
     *
     * @return 用户标识集合
     */
    public static Set<String> getTotpVerifyLimitList() {
        return MfaTotpHelper.getVerifyErrorList();
    }

    /**
     * 生成Captcha。
     * <p>先检查IP限制，warn状态才生成Captcha（携带warn提示），error状态直接拦截。</p>
     *
     * @param userIp    用户IP
     * @param captchaId 前端captchaId，为空或非32位时自动生成
     * @return warn状态返回CaptchaQuestion（含warn提示），error状态返回拦截信息
     */
    public static ResponseData<CaptchaQuestion> generateCaptcha(String userIp, String captchaId) {
        ResponseData checkData = MfaIPLimitHelper.checkIpErrorLimit(userIp);
        if (checkData.isWarn()) {
            ResponseData<CaptchaQuestion> captchaData = MfaCaptchaHelper.generateCaptcha(userIp, captchaId);
            if (captchaData.isSuccess()) {
                return ResponseData.warn(captchaData.getData(), checkData.getCode(), checkData.getMsg());
            } else {
                return captchaData;
            }
        } else {
            return checkData;
        }
    }

    /**
     * 验证Captcha。
     * <p>warn状态才进行Captcha校验；success（无错误记录）或error（已屏蔽）状态直接返回checkData不校验。</p>
     * <p>⚠️ 注意：此方法不可直接对外暴露，否则会被重放/重试攻击。</p>
     *
     * @param userIp      用户IP
     * @param captchaId   captchaId
     * @param captchaSign 前端提交的加密应答
     * @return 校验结果ResponseData
     */
    public static ResponseData verifyCaptcha(String userIp, String captchaId, String captchaSign) {
        //检查IP限制。
        ResponseData checkData = MfaIPLimitHelper.checkIpErrorLimit(userIp);
        if (checkData.isWarn()) {
            // 检查captcha。
            checkData = MfaCaptchaHelper.verifyCaptcha(captchaId, captchaSign);
        }
        return checkData;
    }

    /**
     * 发送设备验证码（默认长度与模板，含Captcha校验）。
     * <p>先校验Captcha，通过后发送设备验证码。</p>
     *
     * @param userIp      用户IP
     * @param saasId      SaaS ID（-1表示欠费拦截）
     * @param deviceType  设备类型，见 {@link uw.mfa.constant.MfaDeviceType}
     * @param deviceId    设备ID（手机号或邮箱）
     * @param captchaId   captchaId
     * @param captchaSign 前端提交的加密应答
     * @return 发送结果ResponseData
     */
    public static ResponseData sendDeviceCode(String userIp, long saasId, int deviceType, String deviceId, String captchaId, String captchaSign) {
        return sendDeviceCode(userIp, saasId, deviceType, deviceId, captchaId, captchaSign, 0, null, null);
    }

    /**
     * 发送设备验证码（指定验证码长度，含Captcha校验）。
     *
     * @param userIp      用户IP
     * @param saasId      SaaS ID（-1表示欠费拦截）
     * @param deviceType  设备类型，见 {@link uw.mfa.constant.MfaDeviceType}
     * @param deviceId    设备ID（手机号或邮箱）
     * @param captchaId   captchaId
     * @param captchaSign 前端提交的加密应答
     * @param codeLen     验证码长度，小于1时使用默认长度
     * @return 发送结果ResponseData
     */
    public static ResponseData sendDeviceCode(String userIp, long saasId, int deviceType, String deviceId, String captchaId, String captchaSign, int codeLen) {
        return sendDeviceCode(userIp, saasId, deviceType, deviceId, captchaId, captchaSign, codeLen, null, null);
    }

    /**
     * 发送设备验证码（完整参数，含Captcha校验）。
     *
     * @param userIp         用户IP
     * @param saasId         SaaS ID（-1表示欠费拦截）
     * @param deviceType     设备类型，见 {@link uw.mfa.constant.MfaDeviceType}
     * @param deviceId       设备ID（手机号或邮箱）
     * @param captchaId      captchaId
     * @param captchaSign    前端提交的加密应答
     * @param codeLen        验证码长度，小于1时使用默认长度
     * @param notifySubject  通知主题（邮件用），为空使用默认值
     * @param notifyContent  通知内容模板，为空使用默认值
     * @return 发送结果ResponseData
     */
    public static ResponseData sendDeviceCode(String userIp, long saasId, int deviceType, String deviceId, String captchaId, String captchaSign, int codeLen, String notifySubject, String notifyContent) {
        //检查Captcha限制。
        ResponseData verifyData = verifyCaptcha(userIp, captchaId, captchaSign);
        if (!verifyData.isSuccess()) {
            return verifyData;
        }
        return MfaDeviceCodeHelper.sendDeviceCode(userIp, saasId, deviceType, deviceId, codeLen, notifySubject, notifyContent);
    }

    /**
     * 校验设备验证码（仅校验验证码本身，不涉及IP）。
     *
     * @param deviceType 设备类型
     * @param deviceId   设备ID
     * @param deviceCode 用户输入的验证码
     * @return 校验结果ResponseData
     */
    public static ResponseData verifyDeviceCode(int deviceType, String deviceId, String deviceCode) {
        return MfaDeviceCodeHelper.verifyDeviceCode(deviceType, deviceId, deviceCode);
    }

    /**
     * 校验设备验证码（同时校验IP限制）。
     * <p>IP为error状态直接拦截；验证码校验失败时递增IP错误次数。</p>
     *
     * @param userIp     用户IP
     * @param deviceType 设备类型
     * @param deviceId   设备ID
     * @param deviceCode 用户输入的验证码
     * @return 校验结果ResponseData
     */
    public static ResponseData verifyDeviceCode(String userIp, int deviceType, String deviceId, String deviceCode) {
        //检查IP限制。
        ResponseData verifyData = checkIpErrorLimit(userIp);
        if (verifyData.isError()) {
            return verifyData;
        }
        verifyData = MfaDeviceCodeHelper.verifyDeviceCode(deviceType, deviceId, deviceCode);
        if (verifyData.isNotSuccess()) {
            MfaIPLimitHelper.incrementIpErrorTimes(userIp, verifyData.getCode());
        }
        return verifyData;
    }

    /**
     * 校验设备验证码（同时校验IP限制与Captcha）。
     * <p>先校验Captcha，再校验设备验证码；验证码校验失败时递增IP错误次数。</p>
     *
     * @param userIp      用户IP
     * @param deviceType  设备类型
     * @param deviceId    设备ID
     * @param deviceCode  用户输入的验证码
     * @param captchaId   captchaId
     * @param captchaSign 前端提交的加密应答
     * @return 校验结果ResponseData
     */
    public static ResponseData verifyDeviceCode(String userIp, int deviceType, String deviceId, String deviceCode, String captchaId, String captchaSign) {
        //检查Captcha限制。
        ResponseData verifyData = verifyCaptcha(userIp, captchaId, captchaSign);
        if (!verifyData.isSuccess()) {
            return verifyData;
        }
        verifyData = MfaDeviceCodeHelper.verifyDeviceCode(deviceType, deviceId, deviceCode);
        if (verifyData.isNotSuccess()) {
            MfaIPLimitHelper.incrementIpErrorTimes(userIp, verifyData.getCode());
        }
        return verifyData;
    }

    /**
     * 生成TOTP密钥（使用默认签发人与二维码尺寸）。
     *
     * @param label 标签（通常为用户标识）
     * @return 含密钥、URI、二维码的TotpSecretData
     */
    public static ResponseData<TotpSecretData> issueTotpSecret(String label) {
        return MfaTotpHelper.issue(label);
    }

    /**
     * 生成TOTP密钥（自定义签发人与二维码尺寸）。
     *
     * @param label  标签（通常为用户标识）
     * @param issuer 签发人，为空使用默认值
     * @param qrSize 二维码尺寸，小于100使用默认值
     * @return 含密钥、URI、二维码的TotpSecretData
     */
    public static ResponseData<TotpSecretData> issueTotpSecret(String label, String issuer, int qrSize) {
        return MfaTotpHelper.issue(label, issuer, qrSize);
    }

    /**
     * 校验TOTP验证码（仅校验验证码本身，不涉及IP）。
     *
     * @param userInfo   用户标识（用于校验错误限制key）
     * @param totpSecret Base32密钥
     * @param totpCode   待校验的验证码
     * @return 校验结果ResponseData
     */
    public static ResponseData verifyTotpCode(String userInfo, String totpSecret, String totpCode) {
        return MfaTotpHelper.verifyCode(userInfo, totpSecret, totpCode);
    }

    /**
     * 校验TOTP验证码（同时校验IP限制）。
     * <p>IP为error状态直接拦截；验证码校验失败时递增IP错误次数。</p>
     *
     * @param userIp     用户IP
     * @param userInfo   用户标识（用于校验错误限制key）
     * @param totpSecret Base32密钥
     * @param totpCode   待校验的验证码
     * @return 校验结果ResponseData
     */
    public static ResponseData verifyTotpCode(String userIp, String userInfo, String totpSecret, String totpCode) {
        //检查IP限制。
        ResponseData verifyData = checkIpErrorLimit(userIp);
        if (verifyData.isError()) {
            return verifyData;
        }
        verifyData = MfaTotpHelper.verifyCode(userInfo, totpSecret, totpCode);
        if (verifyData.isNotSuccess()) {
            MfaIPLimitHelper.incrementIpErrorTimes(userIp, verifyData.getCode());
        }
        return verifyData;
    }

    /**
     * 校验TOTP验证码（同时校验IP限制与Captcha）。
     * <p>先校验Captcha，再校验TOTP验证码；验证码校验失败时递增IP错误次数。</p>
     *
     * @param userIp      用户IP
     * @param userInfo    用户标识（用于校验错误限制key）
     * @param totpSecret  Base32密钥
     * @param totpCode    待校验的验证码
     * @param captchaId   captchaId
     * @param captchaSign 前端提交的加密应答
     * @return 校验结果ResponseData
     */
    public static ResponseData verifyTotpCode(String userIp, String userInfo, String totpSecret, String totpCode, String captchaId, String captchaSign) {
        //检查Captcha限制。
        ResponseData verifyData = verifyCaptcha(userIp, captchaId, captchaSign);
        if (!verifyData.isSuccess()) {
            return verifyData;
        }
        verifyData = MfaTotpHelper.verifyCode(userInfo, totpSecret, totpCode);
        if (verifyData.isNotSuccess()) {
            MfaIPLimitHelper.incrementIpErrorTimes(userIp, verifyData.getCode());
        }
        return verifyData;
    }

    /**
     * 批量生成16位随机恢复码（用于TOTP密钥丢失应急登录）。
     *
     * @param amount 生成数量，小于1按1处理
     * @return 恢复码字符串数组
     */
    public static String[] generateRecoveryCode(int amount) {
        return MfaTotpHelper.generateRecoveryCode(amount);
    }

}
