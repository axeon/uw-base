package uw.mfa.captcha;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.response.ResponseData;
import uw.common.util.JsonUtils;
import uw.mfa.captcha.util.CaptchaAESUtils;
import uw.mfa.captcha.util.CaptchaRandomUtils;
import uw.mfa.captcha.vo.CaptchaAnswer;
import uw.mfa.captcha.vo.CaptchaData;
import uw.mfa.captcha.vo.CaptchaQuestion;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 图形Captcha服务。
 * <p>负责Captcha策略的注册与随机选择、问题生成（含subData加密）、答案校验（含解密与人类操作检测）。</p>
 * <p>captchaId作为AES加密密钥与Redis存储key，subData加密后下发前端，校验时由前端回传captchaSign解密。</p>
 */
public class CaptchaService {

    private static final Logger logger = LoggerFactory.getLogger(CaptchaService.class);

    /**
     * 已注册的Captcha策略数组，生成时随机选择其一。
     */
    private CaptchaStrategy[] captchaStrategies = CaptchaStrategy.ALL_STRATEGIES;

    /**
     * 默认构造器，使用全部内置策略。
     */
    public CaptchaService() {
    }

    /**
     * 指定生成策略构造器。
     *
     * @param captchaStrategies 逗号分隔的策略类名（如 "StringCaptchaStrategy,CalculateCaptchaStrategy"）
     */
    public CaptchaService(String captchaStrategies) {
        ArrayList<CaptchaStrategy> strategyList = new ArrayList<>();
        for (String strategyName : captchaStrategies.split(",")) {
            for (CaptchaStrategy captchaStrategy : CaptchaStrategy.ALL_STRATEGIES) {
                if (captchaStrategy.captchaType().equals(strategyName.trim())) {
                    strategyList.add(captchaStrategy);
                    break;
                }
            }
        }
        this.captchaStrategies = strategyList.toArray(new CaptchaStrategy[strategyList.size()]);
    }

    /**
     * 获取已注册的Captcha类型列表。
     *
     * @return 类型标识数组
     */
    public String[] getCaptchaStrategies() {
        String[] strategies = new String[captchaStrategies.length];
        for (int i = 0; i < captchaStrategies.length; i++) {
            strategies[i] = captchaStrategies[i].captchaType();
        }
        return strategies;
    }

    /**
     * 生成Captcha数据。
     * <p>随机选择策略生成问题，对subData进行AES加密后返回。captchaId为空或非32位时自动生成。</p>
     *
     * @param captchaId 图形captchaId，为空或非32位时自动生成（作为AES密钥）
     * @return 包含加密后问题的Captcha数据
     */
    public ResponseData<CaptchaData> generateCaptcha(String captchaId) {
        // 获取验证码方式
        CaptchaStrategy captchaStrategy = captchaStrategies[ThreadLocalRandom.current().nextInt(captchaStrategies.length)];
        // 设置captchaId
        if (StringUtils.isBlank(captchaId) || captchaId.length() != 32) {
            captchaId = CaptchaRandomUtils.getUUID();
        }
        // 获取captchaData，并对subData进行加密处理。
        ResponseData<CaptchaData> captchaDataResponseData = captchaStrategy.generate(captchaId);
        if (captchaDataResponseData.isSuccess()) {
            CaptchaData captchaData = captchaDataResponseData.getData();
            if (captchaData != null) {
                CaptchaQuestion captchaQuestion = captchaData.getCaptchaQuestion();
                if (captchaQuestion != null) {
                    if (StringUtils.isNotBlank(captchaQuestion.getSubData())) {
                        captchaQuestion.setSubData(CaptchaAESUtils.aesEncrypt(captchaQuestion.getSubData(), captchaQuestion.getCaptchaId()));
                    }
                }
            }
        }
        return captchaDataResponseData;
    }

    /**
     * 校验Captcha。
     * <p>解密captchaSign得到前端CaptchaAnswer，校验类型、操作时长（反机器人）、答案匹配。</p>
     *
     * @param captchaId     图形captchaId（AES解密密钥）
     * @param captchaSign   前端提交的加密应答（AES加密的CaptchaAnswer JSON）
     * @param captchaResult 后端Redis存储的正确答案
     * @return 校验成功返回success，失败返回errorMsg（解密/格式/类型/人类检测/答案任一失败）
     */
    public ResponseData verifyCaptcha(String captchaId, String captchaSign, String captchaResult) {
        if (StringUtils.isBlank(captchaId)) {
            return ResponseData.errorMsg("captchaId is empty!");
        }
        if (captchaId.length() != 32) {
            return ResponseData.errorMsg("captchaId format invalid!");
        }
        if (StringUtils.isBlank(captchaSign)) {
            return ResponseData.errorMsg("captchaSign is empty!");
        }
        if (StringUtils.isEmpty(captchaResult)) {
            return ResponseData.errorMsg("captchaResult is empty!");
        }

        // AES解密失败返回null或空串, 解析失败抛RuntimeException, 均视为校验失败而非500
        String captchaAnswerStr = CaptchaAESUtils.aesDecrypt(captchaSign, captchaId);
        if (StringUtils.isBlank(captchaAnswerStr)) {
            return ResponseData.errorMsg("captchaSign decrypt failed!");
        }
        CaptchaAnswer captchaAnswer;
        try {
            // 前端回答的答案
            captchaAnswer = JsonUtils.parse(captchaAnswerStr, CaptchaAnswer.class);
        } catch (RuntimeException e) {
            return ResponseData.errorMsg("captchaSign format invalid!");
        }
        if (captchaAnswer == null) {
            return ResponseData.errorMsg("captchaAnswer is empty!");
        }
        // 校验Captcha类型是否正确
        String captchaType = captchaAnswer.getCaptchaType();
        CaptchaStrategy captchaStrategy = getCaptchaStrategy(captchaType);
        if (captchaStrategy == null) {
            return ResponseData.errorMsg("captchaType[" + captchaType + "] invalid!");
        }
        // 校验操作时间 (太快可能就是机器识别)
        boolean humanCheck = humanDetect(captchaStrategy, captchaAnswer);
        if (!humanCheck) {
            return ResponseData.errorMsg("verifyCaptcha failed!");
        }

        // 解析后选择对应的验证
        return captchaStrategy.verify(captchaAnswer.getAnswerData(), captchaResult);
    }

    /**
     * 检测是否人类操作。
     * <p>当操作时间低于策略阈值时判定可疑，随机50%概率放行（opTime为偶数放行）。</p>
     *
     * @param captchaStrategy 当前Captcha策略
     * @param captchaAnswer   前端应答（含opTime操作时长）
     * @return 判定为人类返回true，否则false
     */
    private static boolean humanDetect(CaptchaStrategy captchaStrategy, CaptchaAnswer captchaAnswer) {
        // 前端识别Captcha操作的时间
        long opTime = captchaAnswer.getOpTime();
        // 如果识别时间达不到要求使用的时间
        if (captchaStrategy.humanOpTime() > opTime) {
            // 随机一半的概率放行
            return opTime % 2 == 0;
        }
        return true;
    }

    /**
     * 根据类型标识查找已注册的Captcha策略。
     *
     * @param captchaType 类型标识（策略类名）
     * @return 匹配的策略，无匹配返回null
     */
    private CaptchaStrategy getCaptchaStrategy(String captchaType) {
        for (CaptchaStrategy captchaStrategy : captchaStrategies) {
            if (captchaStrategy.captchaType().equals(captchaType)) {
                return captchaStrategy;
            }
        }
        return null;
    }

}
