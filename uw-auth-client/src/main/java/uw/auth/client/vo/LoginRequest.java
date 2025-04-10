package uw.auth.client.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录请求对象
 **/
@Schema(title = "登录请求对象", description = "登录请求对象")
public class LoginRequest {

    /**
     * 登录客户端。
     */
    @Schema(title = "登录代理", description = "登录代理")
    private String loginAgent;

    /**
     * 登录类型。
     */
    @Schema(title = "登录类型", description = "登录类型")
    private int loginType = -1;

    /**
     * 登录信息。
     * 可以支持传入用户名，手机号，email地址等登录方式。
     */
    @Schema(title = "登录信息", description = "登录信息")
    private String loginId;

    /**
     * 登录加密密码，如果有数值将会覆盖loginPass参数。
     */
    @Schema(title = "登录加密密码", description = "登录加密密码")
    private String loginSecret;

    /**
     * 登录明文密码。
     * 可能是密码，也可能是验证码。
     */
    @Schema(title = "登录明文密码", description = "登录明文密码")
    private String loginPass;

    /**
     * saasId
     */
    @Schema(title = "saasId", description = "saasId")
    private long saasId = -1;

    /**
     * 用户类型
     */
    @Schema(title = "用户类型", description = "用户类型")
    private int userType = -1;

    /**
     * 验证码Id
     */
    @Schema(title = "CaptchaId", description = "CaptchaId")
    private String captchaId;

    /**
     * Captcha用户答案
     */
    @Schema(title = "Captcha用户答案", description = "Captcha用户答案")
    private String captchaSign;

    /**
     * 强制登录
     */
    @Schema(title = "强制登录", description = "强制登录")
    private boolean forceLogin = true;

    public String getLoginAgent() {
        return loginAgent;
    }

    public void setLoginAgent(String loginAgent) {
        this.loginAgent = loginAgent;
    }

    public int getLoginType() {
        return loginType;
    }

    public void setLoginType(int loginType) {
        this.loginType = loginType;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getLoginSecret() {
        return loginSecret;
    }

    public void setLoginSecret(String loginSecret) {
        this.loginSecret = loginSecret;
    }

    public String getLoginPass() {
        return loginPass;
    }

    public void setLoginPass(String loginPass) {
        this.loginPass = loginPass;
    }

    public long getSaasId() {
        return saasId;
    }

    public void setSaasId(long saasId) {
        this.saasId = saasId;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getCaptchaSign() {
        return captchaSign;
    }

    public void setCaptchaSign(String captchaSign) {
        this.captchaSign = captchaSign;
    }

    public boolean isForceLogin() {
        return forceLogin;
    }

    public void setForceLogin(boolean forceLogin) {
        this.forceLogin = forceLogin;
    }

}
