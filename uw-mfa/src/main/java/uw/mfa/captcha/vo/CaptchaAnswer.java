package uw.mfa.captcha.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 前端做出的回答。
 */
@Schema(title = "Captcha应答信息", description = "Captcha应答信息")
public class CaptchaAnswer {
    /**
     * Captcha类型
     */
    @Schema(title = "Captcha类型", description = "Captcha回答信息")
    private String captchaType;

    /**
     * 界面操作时间(毫秒)。
     */
    @Schema(title = "界面操作时间", description = "界面操作时间(毫秒)")
    private long opTime;

    /**
     * 应答数据(根据不同场景 返回为整型、对象等)
     */
    @Schema(title = "应答数据", description = "应答数据(根据不同场景 返回为整型、对象等)")
    private String answerData;

    public String getCaptchaType() {
        return captchaType;
    }

    public void setCaptchaType(String captchaType) {
        this.captchaType = captchaType;
    }

    public long getOpTime() {
        return opTime;
    }

    public void setOpTime(long opTime) {
        this.opTime = opTime;
    }

    public String getAnswerData() {
        return answerData;
    }

    public void setAnswerData(String answerData) {
        this.answerData = answerData;
    }
}