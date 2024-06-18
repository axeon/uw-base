package uw.mfa.captcha;


import uw.common.dto.ResponseData;
import uw.mfa.captcha.strategy.*;
import uw.mfa.captcha.vo.CaptchaData;

/**
 * 验证服务抽象
 */
public interface CaptchaStrategy {

    /**
     * 初始化验证码生成策略
     */
    CaptchaStrategy[] ALL_STRATEGIES = new CaptchaStrategy[]{new StringCaptchaStrategy(), new CalculateCaptchaStrategy(), new SlidePuzzleCaptchaStrategy(),
            new ClickWordCaptchaStrategy(), new RotatePuzzleCaptchaStrategy()};

    /***
     * 验证码类型
     * @return
     */
    default String captchaType() {
        return getClass().getSimpleName();
    }

    /**
     * 获取验证码
     *
     * @param captchaId 行为验证码id
     * @return
     */
    ResponseData<CaptchaData> generate(String captchaId);

    /**
     * 校验验证码
     *
     * @param
     * @return
     */
    ResponseData verify(String answerData, String captchaResult);

    /**
     * 获得人类正常操作毫秒数。
     * 检测是否是人类，当前只有一个操作时间。
     *
     * @return
     */
    long humanOpTime();

}
