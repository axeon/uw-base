package uw.auth.service.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.StringJoiner;

/**
 * token 返回 vo.
 **/
@Schema(title = "token返回结果", description = "token返回结果")
public class TokenResponse {


    /**
     * 运营商Id
     */
    @Schema(title = "saasId", description = "saasId")
    private long saasId;

    /**
     * 运营商名。
     */
    @Schema(title = "saas名称", description = "saas名称")
    private String saasName;

    /**
     * 用户登录类型
     */
    @Schema(title = "用户类型", description = "用户类型")
    private int userType;

    /**
     * 用户Id
     */
    @Schema(title = "用户Id", description = "用户Id")
    private long userId;

    /**
     * 商户Id
     */
    @Schema(title = "商户编号", description = "商户编号")
    private long mchId;

    /**
     * 所属用户组ID
     */
    @Schema(title = "所属用户组ID", description = "所属用户组ID")
    private long groupId;

    /**
     * 登录名。
     */
    @Schema(title = "登录名", description = "登录名")
    private String userName;

    /**
     * 真实名称
     */
    @Schema(title = "真实名称", description = "真实名称")
    private String realName;

    /**
     * 别名 [用于业务前台匿名]
     */
    @Schema(title = "别名 [用于业务前台匿名]", description = "别名 [用于业务前台匿名]")
    private String nickName;

    /**
     * 最后更新密码时间。用于前端提醒客户更新密码。
     */
    @Schema(title = "最后更新密码时间", description = "最后更新密码时间。用于前端提醒客户更新密码。")
    private long lastPasswdDate;

    /**
     * 登录提示信息。
     */
    @Schema(title = "登录提示信息", description = "登录提示信息")
    private String loginNotice;

    /**
     * access token.
     */
    @Schema(title = "token", description = "token")
    private String token;

    /**
     * 刷新token。
     */
    @Schema(title = "刷新token", description = "刷新token")
    private String refreshToken;

    /**
     * token过期时间。
     */
    @Schema(title = "token过期时间", description = "token过期时间")
    private long expiresIn;

    /**
     * 刷新token过期时间。
     */
    @Schema(title = "刷新token过期时间", description = "刷新token过期时间")
    private long refreshExpiresIn;

    /**
     * 创建时间
     */
    @Schema(title = "创建时间", description = "创建时间")
    private long createAt;

    public long getSaasId() {
        return saasId;
    }

    public void setSaasId(long saasId) {
        this.saasId = saasId;
    }

    public String getSaasName() {
        return saasName;
    }

    public void setSaasName(String saasName) {
        this.saasName = saasName;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getMchId() {
        return mchId;
    }

    public void setMchId(long mchId) {
        this.mchId = mchId;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public long getLastPasswdDate() {
        return lastPasswdDate;
    }

    public void setLastPasswdDate(long lastPasswdDate) {
        this.lastPasswdDate = lastPasswdDate;
    }

    public String getLoginNotice() {
        return loginNotice;
    }

    public void setLoginNotice(String loginNotice) {
        this.loginNotice = loginNotice;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public long getRefreshExpiresIn() {
        return refreshExpiresIn;
    }

    public void setRefreshExpiresIn(long refreshExpiresIn) {
        this.refreshExpiresIn = refreshExpiresIn;
    }

    public long getCreateAt() {
        return createAt;
    }

    public void setCreateAt(long createAt) {
        this.createAt = createAt;
    }

    @Override
    public String toString() {
        return new StringJoiner( ", ", TokenResponse.class.getSimpleName() + "[", "]" ).add( "saasId=" + saasId ).add( "saasName='" + saasName + "'" ).add( "userType=" + userType ).add( "userId=" + userId ).add( "mchId=" + mchId ).add( "groupId=" + groupId ).add( "userName='" + userName + "'" ).add( "realName='" + realName + "'" ).add( "nickName='" + nickName + "'" ).add( "lastPasswdDate=" + lastPasswdDate ).add( "token='" + token + "'" ).add( "refreshToken='" + refreshToken + "'" ).add( "expiresIn=" + expiresIn ).add( "refreshExpiresIn=" + refreshExpiresIn ).add( "createAt=" + createAt ).toString();
    }
}
