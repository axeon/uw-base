package uw.mfa.captcha;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.dto.ResponseData;
import uw.common.util.JsonUtils;
import uw.mfa.captcha.util.CaptchaAESUtils;
import uw.mfa.captcha.util.CaptchaRandomUtils;
import uw.mfa.captcha.vo.CaptchaAnswer;
import uw.mfa.captcha.vo.CaptchaData;
import uw.mfa.captcha.vo.CaptchaQuestion;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 图形Captcha服务帮助类
 */
public class CaptchaService {

    private static final Logger logger = LoggerFactory.getLogger( CaptchaService.class );

    private CaptchaStrategy[] captchaStrategies = CaptchaStrategy.ALL_STRATEGIES;

    /**
     * 默认使用全部生成策略。
     */
    public CaptchaService() {
    }

    /**
     * 指定生成策略。
     *
     * @param captchaStrategies
     */
    public CaptchaService(String captchaStrategies) {
        ArrayList<CaptchaStrategy> strategyList = new ArrayList<>();
        for (String strategyName : captchaStrategies.split( "," )) {
            for (CaptchaStrategy captchaStrategy : CaptchaStrategy.ALL_STRATEGIES) {
                if (captchaStrategy.captchaType().equals( strategyName.trim() )) {
                    strategyList.add( captchaStrategy );
                    break;
                }
            }
        }
        this.captchaStrategies = strategyList.toArray( new CaptchaStrategy[strategyList.size()] );
    }

    /**
     * 获得Captcha类型列表。
     *
     * @return
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
     *
     * @param captchaId 图形CaptchaId
     * @return
     */
    public ResponseData<CaptchaData> generateCaptcha(String captchaId) {
        // 获取验证码方式
        CaptchaStrategy captchaStrategy = captchaStrategies[ThreadLocalRandom.current().nextInt( captchaStrategies.length )];
        // 设置captchaId
        if (StringUtils.isBlank( captchaId ) || captchaId.length() != 32) {
            captchaId = CaptchaRandomUtils.getUUID();
        }
        // 获取captchaData，并对subData进行加密处理。
        ResponseData<CaptchaData> captchaDataResponseData = captchaStrategy.generate( captchaId );
        if (captchaDataResponseData.isSuccess()) {
            CaptchaData captchaData = captchaDataResponseData.getData();
            if (captchaData != null) {
                CaptchaQuestion captchaQuestion = captchaData.getCaptchaQuestion();
                if (captchaQuestion != null) {
                    if (StringUtils.isNotBlank( captchaQuestion.getSubData() )) {
                        captchaQuestion.setSubData( CaptchaAESUtils.aesEncrypt( captchaQuestion.getSubData(), captchaQuestion.getCaptchaId() ) );
                    }
                }
            }
        }
        return captchaDataResponseData;
    }

    /**
     * 校验Captcha
     *
     * @param captchaId     图形CaptchaId
     * @param captchaSign   Captcha回答
     * @param captchaResult Captcha答案
     * @return
     */
    public ResponseData verifyCaptcha(String captchaId, String captchaSign, String captchaResult) {
        if (StringUtils.isBlank( captchaId )) {
            return ResponseData.errorMsg( "captchaId is empty!" );
        }
        if (captchaId.length() != 32) {
            return ResponseData.errorMsg( "captchaId format invalid!" );
        }
        if (StringUtils.isBlank( captchaSign )) {
            return ResponseData.errorMsg( "captchaSign is empty!" );
        }
        if (StringUtils.isEmpty( captchaResult )) {
            return ResponseData.errorMsg( "captchaResult is empty!" );
        }

        String captchaAnswerStr = CaptchaAESUtils.aesDecrypt( captchaSign, captchaId );
        // 前端回答的答案
        CaptchaAnswer captchaAnswer = JsonUtils.parse( captchaAnswerStr, CaptchaAnswer.class );
        // 校验Captcha类型是否正确
        String captchaType = captchaAnswer.getCaptchaType();
        CaptchaStrategy captchaStrategy = getCaptchaStrategy( captchaType );
        if (captchaStrategy == null) {
            ResponseData.errorMsg( "captchaType[" + captchaType + "] invalid!" );
        }
        // 校验操作时间 (太快可能就是机器识别)
        boolean humanCheck = humanDetect( captchaStrategy, captchaAnswer );
        if (!humanCheck) {
            return ResponseData.errorMsg( "verifyCaptcha failed!" );
        }

        // 解析后选择对应的验证
        return captchaStrategy.verify( captchaAnswer.getAnswerData(), captchaResult );
    }

    /**
     * 检测是否人类操作。
     *
     * @return
     */
    private static boolean humanDetect(CaptchaStrategy captchaStrategy, CaptchaAnswer captchaAnswer) {
        // 前端识别Captcha操作的时间
        long opTime = captchaAnswer.getOpTime();
        // 如果识别时间达不到要求使用的时间
        if (captchaStrategy.humanOpTime() > opTime) {
            // 随机一半的概率放行
            return opTime % 2 == 0 ? true : false;
        }
        return true;
    }

    /**
     * 生成Catpcha服务
     *
     * @param captchaType 验证类型
     * @return
     */
    private CaptchaStrategy getCaptchaStrategy(String captchaType) {
        for (CaptchaStrategy captchaStrategy : captchaStrategies) {
            if (captchaStrategy.captchaType().equals( captchaType )) {
                return captchaStrategy;
            }
        }
        return null;
    }

}
