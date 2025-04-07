package uw.auth.service.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
     * 用户IP
     */
    @Schema(title = "用户IP", description = "用户IP" )
    private String userIp;

    /**
     * API URI
     */
    @Schema(title = "API URI", description = "API URI" )
    private String apiUri;

    /**
     * API名称
     */
    @Schema(title = "API名称", description = "API名称" )
    private String apiName;

    /**
     * 业务类型,用于查询
     */
    @Schema(title = "业务类型", description = "业务类型" )
    private String refType;

    /**
     * 业务Id,用于查询
     */
    @Schema(title = "业务Id", description = "业务Id" )
    private Serializable refId;

    /**
     * 请求参数
     */
    @Schema(title = "请求参数", description = "请求参数" )
    private String requestBody;

    /**
     * 请求时间
     */
    @Schema(title = "请求时间", description = "请求时间" )
    private Date requestDate;

    /**
     * 响应日志
     */
    @Schema(title = "响应日志", description = "响应日志" )
    private String responseBody;

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
     * 执行返回毫秒数。
     */
    @Schema(title = "执行返回毫秒数", description = "执行返回毫秒数" )
    private long responseMillis;

    /**
     * 响应状态码
     */
    @Schema(title = "响应状态码", description = "响应状态码" )
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

    public String getUserIp() {
        return userIp;
    }

    public void setUserIp(String userIp) {
        this.userIp = userIp;
    }

    public String getApiUri() {
        return apiUri;
    }

    public void setApiUri(String apiUri) {
        this.apiUri = apiUri;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getRefType() {
        return refType;
    }

    public void setRefType(String refType) {
        this.refType = refType;
    }

    public Serializable getRefId() {
        return refId;
    }

    public void setRefId(Serializable refId) {
        this.refId = refId;
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

    public long getResponseMillis() {
        return responseMillis;
    }

    public void setResponseMillis(long responseMillis) {
        this.responseMillis = responseMillis;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}
