package uw.auth.service.vo;

import uw.log.es.vo.LogBaseVo;

import java.io.Serializable;
import java.util.Date;

/**
 * 接口操作日志
 */
public class MscActionLog extends LogBaseVo implements Serializable {

    /**
     * 用户Id
     */
    private long userId;

    /**
     * 登录名。
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
     * 运营商Id
     */
    private long saasId;

    /**
     * 商户Id
     */
    private long mchId;

    /**
     * 用户组Id。
     */
    private long groupId;

    /**
     * 用户类型 ==> msc_user_type.type_code
     */
    private int userType;

    /**
     * 请求uri
     */
    private String uri;

    /**
     * 方法操作描述
     */
    private String info;

    /**
     * 请求Ip
     */
    private String userIp;

    /**
     * 业务标识类型,用于查询
     */
    private String refType;

    /**
     * 业务Id,用于查询
     */
    private Serializable refId;

    /**
     * 日志内容 (需要客户代码写的)
     */
    private String log;

    /**
     * 请求参数
     */
    private String requestBody;

    /**
     * 请求时间
     */
    private Date requestDate;

    /**
     * 响应日志
     */
    private String responseBody;

    /**
     * 执行返回毫秒数。
     */
    private long responseMillis;

    /**
     * 异常信息
     */
    private String exception;

    /**
     * 响应状态码
     */
    private int statusCode;

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

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getUserIp() {
        return userIp;
    }

    public void setUserIp(String userIp) {
        this.userIp = userIp;
    }

    public Serializable getRefId() {
        return refId;
    }

    public void setRefId(Serializable refId) {
        this.refId = refId;
    }

    public String getRefType() {
        return refType;
    }

    public void setRefType(String refType) {
        this.refType = refType;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public Date getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public long getResponseMillis() {
        return responseMillis;
    }

    public void setResponseMillis(long responseMillis) {
        this.responseMillis = responseMillis;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}
