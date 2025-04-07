package uw.mfa.captcha.vo;

/**
 * 生成的图形Captcha数据。
 *
 */
public class CaptchaData {

    /**
     * 返回前端的问题
     */
    private CaptchaQuestion captchaQuestion;

    /**
     * 后端需要存储的的答案 可能是单个数字、字符串、json格式字符串
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
