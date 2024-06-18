package uw.mfa.captcha.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 返回给前端的结果
 */
@Schema(title = "captcha问题信息", description = "captcha问题信息")
public class CaptchaQuestion implements Serializable {

    /**
     * captchaId(后台生成)
     */
    @Schema(title = "captchaId", description = "captchaId(后台生成)")
    private String captchaId;

    /**
     * captcha有效期秒数。
     */
    @Schema(title = "captcha有效期", description = "captcha有效期")
    private long captchaTtl;

    /**
     * captcha类型
     */
    @Schema(title = "captcha类型", description = "captcha类型")
    private String captchaType;

    /**
     * 原生图片base64
     */
    @Schema(title = "原生图片base64", description = "原生图片base64")
    private String mainImageBase64;

    /**
     * 拼图图片base64 (滑动、旋转使用)
     */
    @Schema(title = "拼图图片base64", description = "拼图图片base64(滑动、旋转使用)")
    private String subImageBase64;

    /**
     * 附加数据。
     */
    @Schema(title = "附加数据", description = "附加数据，已经AES加密。")
    private String subData;

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public long getCaptchaTtl() {
        return captchaTtl;
    }

    public void setCaptchaTtl(long captchaTtl) {
        this.captchaTtl = captchaTtl;
    }

    public String getCaptchaType() {
        return captchaType;
    }

    public void setCaptchaType(String captchaType) {
        this.captchaType = captchaType;
    }

    public String getMainImageBase64() {
        return mainImageBase64;
    }

    public void setMainImageBase64(String mainImageBase64) {
        this.mainImageBase64 = mainImageBase64;
    }

    public String getSubImageBase64() {
        return subImageBase64;
    }

    public void setSubImageBase64(String subImageBase64) {
        this.subImageBase64 = subImageBase64;
    }

    public String getSubData() {
        return subData;
    }

    public void setSubData(String subData) {
        this.subData = subData;
    }
}
