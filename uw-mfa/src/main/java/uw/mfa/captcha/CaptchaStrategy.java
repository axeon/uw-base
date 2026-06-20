package uw.mfa.captcha;


import uw.common.response.ResponseData;
import uw.mfa.captcha.strategy.*;
import uw.mfa.captcha.vo.CaptchaData;

/**
 * Captcha验证策略抽象。
 * <p>每种策略实现一种验证码类型（字符串、计算式、滑动拼图、点选文字、旋转拼图），</p>
 * <p>提供问题生成、答案校验、人类操作时长判定能力。</p>
 */
public interface CaptchaStrategy {

    /**
     * 全部内置Captcha策略实例数组，用于策略查找与随机选择。
     */
    CaptchaStrategy[] ALL_STRATEGIES = new CaptchaStrategy[]{new StringCaptchaStrategy(), new CalculateCaptchaStrategy(), new SlidePuzzleCaptchaStrategy(),
            new ClickWordCaptchaStrategy(), new RotatePuzzleCaptchaStrategy()};

    /**
     * 获取Captcha类型标识，默认返回类名（如StringCaptchaStrategy）。
     *
     * @return 类型标识字符串
     */
    default String captchaType() {
        return getClass().getSimpleName();
    }

    /**
     * 生成Captcha问题数据。
     *
     * @param captchaId 32位captchaId，作为AES加密密钥与Redis存储key
     * @return 包含前端问题与后端答案的Captcha数据
     */
    ResponseData<CaptchaData> generate(String captchaId);

    /**
     * 校验Captcha答案。
     *
     * @param answerData    前端提交的应答数据（明文，由CaptchaAnswer.answerData提供）
     * @param captchaResult 后端存储的正确答案
     * @return 校验成功返回success，失败返回error
     */
    ResponseData verify(String answerData, String captchaResult);

    /**
     * 获取人类正常操作所需毫秒数，用于反机器人检测。
     * <p>当用户操作时间低于该值时，判定为可疑（可能有50%概率放行）。</p>
     *
     * @return 人类操作时长阈值（毫秒）
     */
    long humanOpTime();

}
