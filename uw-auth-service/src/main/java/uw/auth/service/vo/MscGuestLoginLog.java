package uw.auth.service.vo;

import uw.log.es.vo.LogBaseVo;

import java.io.Serializable;
import java.util.Date;

/**
 * 登陆日志。
 */
public class MscGuestLoginLog extends LogBaseVo implements Serializable {


    /**
     * 登录客户端。
     */
    private String loginAgent;

    /**
     * 登录类型。
     */
    private int loginType;

    /**
     * 登录标识，用户名/手机号/email地址。
     */
    private String loginId;

    /**
     * 用户Id
     */
    private long userId;

    /**
     * 用户类型 ==> msc_user_type.type_code
     */
    private int userType;

    /**
     * 运营商Id
     */
    private long saasId;

    /**
     * 分销商Id
     */
    private long mchId;

    /**
     * 用户组Id。
     */
    private long groupId;

    /**
     * 用户登录名
     */
    private String userName;

    /**
     * 昵称信息。
     */
    private String nickName;

    /**
     * 真实姓名。
     */
    private String realName;

    /**
     * 登录IP
     */
    private String userIp;

    /**
     * 用户登录设备类型: 1: PC-WEB; 2: MOBILE-WEB; 3: MOBILE-APP; 4: WEIXIN-APP etc
     */
    private int clientType;

    /**
     * 用户代理
     */
    private String clientAgent;

    /**
     * 登录结果 按照http code进行。
     */
    private int loginResult;

    /**
     * 备注: 失败原因,成功备注
     */
    private String remark;

    /**
     * 登录时间
     */
    private Date loginDate;

    /**
     * 执行返回毫秒数。
     */
    private long responseMillis;

    public String getLoginAgent() {
        return loginAgent;
    }

    public void setLoginAgent(String loginAgent) {
        this.loginAgent = loginAgent;
    }

    public Date getLoginDate() {
        return loginDate;
    }

    public void setLoginDate(Date loginDate) {
        this.loginDate = loginDate;
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

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
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


    public int getLoginResult() {
        return loginResult;
    }

    public void setLoginResult(int loginResult) {
        this.loginResult = loginResult;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public long getResponseMillis() {
        return responseMillis;
    }

    public void setResponseMillis(long responseMillis) {
        this.responseMillis = responseMillis;
    }
}
