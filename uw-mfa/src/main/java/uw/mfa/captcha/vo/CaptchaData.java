package uw.mfa.captcha.vo;

/**
 * 生成的图形Captcha数据。
 * <p>包含返回前端的 {@link CaptchaQuestion} 与后端存储的答案captchaResult，</p>
 * <p>captchaResult由具体策略决定格式（单个数字、字符串、JSON字符串等）。</p>
 */
public class CaptchaData {

    /**
     * 返回前端的问题数据。
     */
    private CaptchaQuestion captchaQuestion;

    /**
     * 后端需要存储的答案，可能是单个数字、字符串、JSON格式字符串（由策略决定）。
     */
    private String captchaResult;

    public CaptchaData() {
    }

    public CaptchaData(CaptchaQuestion captchaQuestion, String captchaResult) {
        this.captchaQuestion = captchaQuestion;
        this.captchaResult = captchaResult;
    }

    public CaptchaQuestion getCaptchaQuestion() {
        return captchaQuestion;
    }

    public void setCaptchaQuestion(CaptchaQuestion captchaQuestion) {
        this.captchaQuestion = captchaQuestion;
    }

    public String getCaptchaResult() {
        return captchaResult;
    }

    public void setCaptchaResult(String captchaResult) {
        this.captchaResult = captchaResult;
    }
}
