package uw.auth.service.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.log.es.vo.LogBaseVo;

import java.io.Serializable;
import java.util.Date;

/**
 * 游客登陆日志。
 */
public class MscGuestLoginLog extends LogBaseVo implements Serializable {


    /**
     * 登录客户端。
     */
    @Schema(title = "登录客户端", description = "登录客户端")
    private String loginAgent;

    /**
     * 登录类型。
     */
    @Schema(title = "登录类型", description = "登录类型" )
    private int loginType;

    /**
     * 登录标识，用户名/手机号/email地址。
     */
    @Schema(title = "登录标识", description = "登录标识" )
    private String loginId;

    /**
     * 用户Id
     */
    @Schema(title = "用户Id", description = "用户Id" )
    private long userId;

    /**
     * 用户信息。
     */
    @Schema(title = "用户信息", description = "用户信息" )
    private String userName;

    /**
     * 昵称信息。
     */
    @Schema(title = "昵称信息", description = "昵称信息" )
    private String nickName;

    /**
     * 真实姓名。
     */
    @Schema(title = "真实姓名", description = "真实姓名" )
    private String realName;
    /**
     * 运营商Id
     */
    @Schema(title = "运营商Id", description = "运营商Id" )
    private long saasId;

    /**
     * 商户Id
     */
    @Schema(title = "商户Id", description = "商户Id" )
    private long mchId;

    /**
     * 用户组Id。
     */
    @Schema(title = "用户组Id", description = "用户组Id" )
    private long groupId;

    /**
     * 用户类型 ==> msc_user_type.type_code
     */
    @Schema(title = "用户类型", description = "用户类型" )
    private int userType;

    /**
     * 登录IP
     */
    @Schema(title = "登录IP", description = "登录IP" )
    private String userIp;

    /**
     * 用户登录设备类型: 1: PC-WEB; 2: MOBILE-WEB; 3: MOBILE-APP; 4: WEIXIN-APP etc
     */
    @Schema(title = "登录设备", description = "登录设备" )
    private int clientType;

    /**
     * 用户代理
     */
    @Schema(title = "用户代理", description = "用户代理" )
    private String clientAgent;

    /**
     * 响应状态
     */
    @Schema(title = "响应状态", description = "响应状态" )
    private String responseState;

    /**
     * 响应代码
     */
    @Schema(title = "响应代码", description = "响应代码" )
    private String responseCode;

    /**
     * 响应消息
     */
    @Schema(title = "响应消息", description = "响应消息" )
    private String responseMsg;

    /**
     * 登录时间
     */
    @Schema(title = "登录时间", description = "登录时间" )
    private Date loginDate;

    /**
     * 执行返回毫秒数。
     */
    @Schema(title = "响应毫秒数", description = "响应毫秒数" )
    private long responseMillis;

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

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public long getSaasId() {
        return saasId;
    }

    public void setSaasId(long saasId) {
        this.saasId = saasId;
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

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public String getUserIp() {
        return userIp;
    }

    public void setUserIp(String userIp) {
        this.userIp = userIp;
    }

    public int getClientType() {
        return clientType;
    }

    public void setClientType(int clientType) {
        this.clientType = clientType;
    }

    public String getClientAgent() {
        return clientAgent;
    }

    public void setClientAgent(String clientAgent) {
        this.clientAgent = clientAgent;
    }

    public String getResponseState() {
        return responseState;
    }

    public void setResponseState(String responseState) {
        this.responseState = responseState;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMsg() {
        return responseMsg;
    }

    public void setResponseMsg(String responseMsg) {
        this.responseMsg = responseMsg;
    }

    public Date getLoginDate() {
        return loginDate;
    }

    public void setLoginDate(Date loginDate) {
        this.loginDate = loginDate;
    }

    public long getResponseMillis() {
        return responseMillis;
    }

    public void setResponseMillis(long responseMillis) {
        this.responseMillis = responseMillis;
    }
}
