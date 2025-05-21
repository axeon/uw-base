package uw.common.app.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.common.util.JsonUtils;
import uw.dao.DataEntity;
import uw.dao.DataUpdateInfo;
import uw.dao.annotation.ColumnMeta;
import uw.dao.annotation.TableMeta;

import java.io.Serializable;


/**
 * SysCritLog实体类
 * 系统关键日志
 *
 * @author axeon
 */
@TableMeta(tableName = "sys_crit_log", tableType = "table")
@Schema(title = "系统关键日志", description = "系统关键日志")
public class SysCritLog implements DataEntity, Serializable {


    /**
     * ID
     */
    @ColumnMeta(columnName = "id", dataType = "long", dataSize = 19, nullable = false, primaryKey = true)
    @Schema(title = "ID", description = "ID", maxLength = 19, nullable = false)
    private long id;

    /**
     * saasId
     */
    @ColumnMeta(columnName = "saas_id", dataType = "long", dataSize = 19, nullable = false, primaryKey = true)
    @Schema(title = "saasId", description = "saasId", maxLength = 19, nullable = false)
    private long saasId;

    /**
     * 商户ID
     */
    @ColumnMeta(columnName = "mch_id", dataType = "long", dataSize = 19, nullable = true)
    @Schema(title = "商户ID", description = "商户ID", maxLength = 19, nullable = true)
    private long mchId;

    /**
     * 用户id
     */
    @ColumnMeta(columnName = "user_id", dataType = "long", dataSize = 19, nullable = false)
    @Schema(title = "用户id", description = "用户id", maxLength = 19, nullable = false)
    private long userId;

    /**
     * 用户类型
     */
    @ColumnMeta(columnName = "user_type", dataType = "int", dataSize = 10, nullable = true)
    @Schema(title = "用户类型", description = "用户类型", maxLength = 10, nullable = true)
    private int userType;

    /**
     * 用户组ID
     */
    @ColumnMeta(columnName = "group_id", dataType = "long", dataSize = 19, nullable = true)
    @Schema(title = "用户组ID", description = "用户组ID", maxLength = 19, nullable = true)
    private long groupId;

    /**
     * 用户名
     */
    @ColumnMeta(columnName = "user_name", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "用户名", description = "用户名", maxLength = 100, nullable = true)
    private String userName;

    /**
     * 用户昵称
     */
    @ColumnMeta(columnName = "nick_name", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "用户昵称", description = "用户昵称", maxLength = 100, nullable = true)
    private String nickName;

    /**
     * 真实名称
     */
    @ColumnMeta(columnName = "real_name", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "真实名称", description = "真实名称", maxLength = 100, nullable = true)
    private String realName;

    /**
     * 用户ip
     */
    @ColumnMeta(columnName = "user_ip", dataType = "String", dataSize = 50, nullable = true)
    @Schema(title = "用户ip", description = "用户ip", maxLength = 50, nullable = true)
    private String userIp;

    /**
     * 请求uri
     */
    @ColumnMeta(columnName = "api_uri", dataType = "String", dataSize = 200, nullable = true)
    @Schema(title = "请求uri", description = "请求uri", maxLength = 200, nullable = true)
    private String apiUri;

    /**
     * API名称
     */
    @ColumnMeta(columnName = "api_name", dataType = "String", dataSize = 200, nullable = true)
    @Schema(title = "API名称", description = "API名称", maxLength = 200, nullable = true)
    private String apiName;

    /**
     * 业务类型
     */
    @ColumnMeta(columnName = "biz_type", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "业务类型", description = "业务类型", maxLength = 100, nullable = true)
    private String bizType;

    /**
     * 业务ID
     */
    @ColumnMeta(columnName = "biz_id", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "业务ID", description = "业务ID", maxLength = 100, nullable = true)
    private String bizId;

    /**
     * 业务日志
     */
    @ColumnMeta(columnName = "biz_log", dataType = "String", dataSize = 65535, nullable = true)
    @Schema(title = "业务日志", description = "业务日志", maxLength = 65535, nullable = true)
    private String bizLog;

    /**
     * 请求时间
     */
    @ColumnMeta(columnName = "request_date", dataType = "java.util.Date", dataSize = 23, nullable = false)
    @Schema(title = "请求时间", description = "请求时间", maxLength = 23, nullable = false)
    private java.util.Date requestDate;

    /**
     * 请求参数
     */
    @ColumnMeta(columnName = "request_body", dataType = "String", dataSize = 2147483646, nullable = true)
    @Schema(title = "请求参数", description = "请求参数", maxLength = 2147483646, nullable = true)
    private String requestBody;

    /**
     * 响应状态
     */
    @ColumnMeta(columnName = "response_state", dataType = "String", dataSize = 10, nullable = true)
    @Schema(title = "响应状态", description = "响应状态", maxLength = 10, nullable = true)
    private String responseState;

    /**
     * 响应代码
     */
    @ColumnMeta(columnName = "response_code", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "响应代码", description = "响应代码", maxLength = 100, nullable = true)
    private String responseCode;

    /**
     * 响应消息
     */
    @ColumnMeta(columnName = "response_msg", dataType = "String", dataSize = 1000, nullable = true)
    @Schema(title = "响应消息", description = "响应消息", maxLength = 1000, nullable = true)
    private String responseMsg;

    /**
     * 响应日志
     */
    @ColumnMeta(columnName = "response_body", dataType = "String", dataSize = 2147483646, nullable = true)
    @Schema(title = "响应日志", description = "响应日志", maxLength = 2147483646, nullable = true)
    private String responseBody;

    /**
     * 请求毫秒数
     */
    @ColumnMeta(columnName = "response_millis", dataType = "long", dataSize = 19, nullable = true)
    @Schema(title = "请求毫秒数", description = "请求毫秒数", maxLength = 19, nullable = true)
    private long responseMillis;

    /**
     * 响应状态码
     */
    @ColumnMeta(columnName = "status_code", dataType = "int", dataSize = 10, nullable = true)
    @Schema(title = "响应状态码", description = "响应状态码", maxLength = 10, nullable = true)
    private int statusCode;

    /**
     * 应用信息
     */
    @ColumnMeta(columnName = "app_info", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "应用信息", description = "应用信息", maxLength = 100, nullable = true)
    private String appInfo;

    /**
     * 应用主机
     */
    @ColumnMeta(columnName = "app_host", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "应用主机", description = "应用主机", maxLength = 100, nullable = true)
    private String appHost;

    /**
     * 数据更新信息.
     */
    private transient DataUpdateInfo _UPDATED_INFO = null;

    /**
     * 是否加载完成.
     */
    private transient boolean _IS_LOADED;

    /**
     * 获得实体的表名。
     */
    @Override
    public String ENTITY_TABLE() {
        return "sys_crit_log";
    }

    /**
     * 获得实体的表注释。
     */
    @Override
    public String ENTITY_NAME() {
        return "系统关键日志";
    }

    /**
     * 获得主键
     */
    @Override
    public Serializable ENTITY_ID() {
        return getId();
    }

    /**
     * 获取更新信息.
     */
    @Override
    public DataUpdateInfo GET_UPDATED_INFO() {
        return this._UPDATED_INFO;
    }

    /**
     * 清除更新信息.
     */
    @Override
    public void CLEAR_UPDATED_INFO() {
        _UPDATED_INFO = null;
    }


    /**
     * 获取ID。
     */
    public long getId() {
        return this.id;
    }

    /**
     * 设置ID。
     */
    public void setId(long id) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "id", this.id, id, !_IS_LOADED);
        this.id = id;
    }

    /**
     * 获取saasId。
     */
    public long getSaasId() {
        return this.saasId;
    }

    /**
     * 设置saasId。
     */
    public void setSaasId(long saasId) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "saasId", this.saasId, saasId, !_IS_LOADED);
        this.saasId = saasId;
    }

    /**
     * 获取商户ID。
     */
    public long getMchId() {
        return this.mchId;
    }

    /**
     * 设置商户ID。
     */
    public void setMchId(long mchId) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "mchId", this.mchId, mchId, !_IS_LOADED);
        this.mchId = mchId;
    }

    /**
     * 获取用户id。
     */
    public long getUserId() {
        return this.userId;
    }

    /**
     * 设置用户id。
     */
    public void setUserId(long userId) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "userId", this.userId, userId, !_IS_LOADED);
        this.userId = userId;
    }

    /**
     * 获取用户类型。
     */
    public int getUserType() {
        return this.userType;
    }

    /**
     * 设置用户类型。
     */
    public void setUserType(int userType) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "userType", this.userType, userType, !_IS_LOADED);
        this.userType = userType;
    }

    /**
     * 获取用户组ID。
     */
    public long getGroupId() {
        return this.groupId;
    }

    /**
     * 设置用户组ID。
     */
    public void setGroupId(long groupId) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "groupId", this.groupId, groupId, !_IS_LOADED);
        this.groupId = groupId;
    }

    /**
     * 获取用户名。
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * 设置用户名。
     */
    public void setUserName(String userName) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "userName", this.userName, userName, !_IS_LOADED);
        this.userName = userName;
    }

    /**
     * 获取用户昵称。
     */
    public String getNickName() {
        return this.nickName;
    }

    /**
     * 设置用户昵称。
     */
    public void setNickName(String nickName) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "nickName", this.nickName, nickName, !_IS_LOADED);
        this.nickName = nickName;
    }

    /**
     * 获取真实名称。
     */
    public String getRealName() {
        return this.realName;
    }

    /**
     * 设置真实名称。
     */
    public void setRealName(String realName) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "realName", this.realName, realName, !_IS_LOADED);
        this.realName = realName;
    }

    /**
     * 获取用户ip。
     */
    public String getUserIp() {
        return this.userIp;
    }

    /**
     * 设置用户ip。
     */
    public void setUserIp(String userIp) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "userIp", this.userIp, userIp, !_IS_LOADED);
        this.userIp = userIp;
    }

    /**
     * 获取请求uri。
     */
    public String getApiUri() {
        return this.apiUri;
    }

    /**
     * 设置请求uri。
     */
    public void setApiUri(String apiUri) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "apiUri", this.apiUri, apiUri, !_IS_LOADED);
        this.apiUri = apiUri;
    }

    /**
     * 获取API名称。
     */
    public String getApiName() {
        return this.apiName;
    }

    /**
     * 设置API名称。
     */
    public void setApiName(String apiName) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "apiName", this.apiName, apiName, !_IS_LOADED);
        this.apiName = apiName;
    }

    /**
     * 获取业务类型。
     */
    public String getBizType() {
        return this.bizType;
    }

    /**
     * 设置业务类型。
     */
    public void setBizType(String bizType) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "bizType", this.bizType, bizType, !_IS_LOADED);
        this.bizType = bizType;
    }

    /**
     * 获取业务ID。
     */
    public String getBizId() {
        return this.bizId;
    }

    /**
     * 设置业务ID。
     */
    public void setBizId(String bizId) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "bizId", this.bizId, bizId, !_IS_LOADED);
        this.bizId = bizId;
    }

    /**
     * 获取业务日志。
     */
    public String getBizLog() {
        return this.bizLog;
    }

    /**
     * 设置业务日志。
     */
    public void setBizLog(String bizLog) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "bizLog", this.bizLog, bizLog, !_IS_LOADED);
        this.bizLog = bizLog;
    }

    /**
     * 获取请求时间。
     */
    public java.util.Date getRequestDate() {
        return this.requestDate;
    }

    /**
     * 设置请求时间。
     */
    public void setRequestDate(java.util.Date requestDate) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "requestDate", this.requestDate, requestDate, !_IS_LOADED);
        this.requestDate = requestDate;
    }

    /**
     * 获取请求参数。
     */
    public String getRequestBody() {
        return this.requestBody;
    }

    /**
     * 设置请求参数。
     */
    public void setRequestBody(String requestBody) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "requestBody", this.requestBody, requestBody, !_IS_LOADED);
        this.requestBody = requestBody;
    }

    /**
     * 获取响应状态。
     */
    public String getResponseState() {
        return this.responseState;
    }

    /**
     * 设置响应状态。
     */
    public void setResponseState(String responseState) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "responseState", this.responseState, responseState, !_IS_LOADED);
        this.responseState = responseState;
    }

    /**
     * 获取响应代码。
     */
    public String getResponseCode() {
        return this.responseCode;
    }

    /**
     * 设置响应代码。
     */
    public void setResponseCode(String responseCode) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "responseCode", this.responseCode, responseCode, !_IS_LOADED);
        this.responseCode = responseCode;
    }

    /**
     * 获取响应消息。
     */
    public String getResponseMsg() {
        return this.responseMsg;
    }

    /**
     * 设置响应消息。
     */
    public void setResponseMsg(String responseMsg) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "responseMsg", this.responseMsg, responseMsg, !_IS_LOADED);
        this.responseMsg = responseMsg;
    }

    /**
     * 获取响应日志。
     */
    public String getResponseBody() {
        return this.responseBody;
    }

    /**
     * 设置响应日志。
     */
    public void setResponseBody(String responseBody) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "responseBody", this.responseBody, responseBody, !_IS_LOADED);
        this.responseBody = responseBody;
    }

    /**
     * 获取请求毫秒数。
     */
    public long getResponseMillis() {
        return this.responseMillis;
    }

    /**
     * 设置请求毫秒数。
     */
    public void setResponseMillis(long responseMillis) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "responseMillis", this.responseMillis, responseMillis, !_IS_LOADED);
        this.responseMillis = responseMillis;
    }

    /**
     * 获取响应状态码。
     */
    public int getStatusCode() {
        return this.statusCode;
    }

    /**
     * 设置响应状态码。
     */
    public void setStatusCode(int statusCode) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "statusCode", this.statusCode, statusCode, !_IS_LOADED);
        this.statusCode = statusCode;
    }

    /**
     * 获取应用信息。
     */
    public String getAppInfo() {
        return this.appInfo;
    }

    /**
     * 设置应用信息。
     */
    public void setAppInfo(String appInfo) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "appInfo", this.appInfo, appInfo, !_IS_LOADED);
        this.appInfo = appInfo;
    }

    /**
     * 获取应用主机。
     */
    public String getAppHost() {
        return this.appHost;
    }

    /**
     * 设置应用主机。
     */
    public void setAppHost(String appHost) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "appHost", this.appHost, appHost, !_IS_LOADED);
        this.appHost = appHost;
    }

    /**
     * 设置ID链式调用。
     */
    public SysCritLog id(long id) {
        setId(id);
        return this;
    }

    /**
     * 设置saasId链式调用。
     */
    public SysCritLog saasId(long saasId) {
        setSaasId(saasId);
        return this;
    }

    /**
     * 设置商户ID链式调用。
     */
    public SysCritLog mchId(long mchId) {
        setMchId(mchId);
        return this;
    }

    /**
     * 设置用户id链式调用。
     */
    public SysCritLog userId(long userId) {
        setUserId(userId);
        return this;
    }

    /**
     * 设置用户类型链式调用。
     */
    public SysCritLog userType(int userType) {
        setUserType(userType);
        return this;
    }

    /**
     * 设置用户组ID链式调用。
     */
    public SysCritLog groupId(long groupId) {
        setGroupId(groupId);
        return this;
    }

    /**
     * 设置用户名链式调用。
     */
    public SysCritLog userName(String userName) {
        setUserName(userName);
        return this;
    }

    /**
     * 设置用户昵称链式调用。
     */
    public SysCritLog nickName(String nickName) {
        setNickName(nickName);
        return this;
    }

    /**
     * 设置真实名称链式调用。
     */
    public SysCritLog realName(String realName) {
        setRealName(realName);
        return this;
    }

    /**
     * 设置用户ip链式调用。
     */
    public SysCritLog userIp(String userIp) {
        setUserIp(userIp);
        return this;
    }

    /**
     * 设置请求uri链式调用。
     */
    public SysCritLog apiUri(String apiUri) {
        setApiUri(apiUri);
        return this;
    }

    /**
     * 设置API名称链式调用。
     */
    public SysCritLog apiName(String apiName) {
        setApiName(apiName);
        return this;
    }

    /**
     * 设置业务类型链式调用。
     */
    public SysCritLog bizType(String bizType) {
        setBizType(bizType);
        return this;
    }

    /**
     * 设置业务ID链式调用。
     */
    public SysCritLog bizId(String bizId) {
        setBizId(bizId);
        return this;
    }

    /**
     * 设置业务日志链式调用。
     */
    public SysCritLog bizLog(String bizLog) {
        setBizLog(bizLog);
        return this;
    }

    /**
     * 设置请求时间链式调用。
     */
    public SysCritLog requestDate(java.util.Date requestDate) {
        setRequestDate(requestDate);
        return this;
    }

    /**
     * 设置请求参数链式调用。
     */
    public SysCritLog requestBody(String requestBody) {
        setRequestBody(requestBody);
        return this;
    }

    /**
     * 设置响应状态链式调用。
     */
    public SysCritLog responseState(String responseState) {
        setResponseState(responseState);
        return this;
    }

    /**
     * 设置响应代码链式调用。
     */
    public SysCritLog responseCode(String responseCode) {
        setResponseCode(responseCode);
        return this;
    }

    /**
     * 设置响应消息链式调用。
     */
    public SysCritLog responseMsg(String responseMsg) {
        setResponseMsg(responseMsg);
        return this;
    }

    /**
     * 设置响应日志链式调用。
     */
    public SysCritLog responseBody(String responseBody) {
        setResponseBody(responseBody);
        return this;
    }

    /**
     * 设置请求毫秒数链式调用。
     */
    public SysCritLog responseMillis(long responseMillis) {
        setResponseMillis(responseMillis);
        return this;
    }

    /**
     * 设置响应状态码链式调用。
     */
    public SysCritLog statusCode(int statusCode) {
        setStatusCode(statusCode);
        return this;
    }

    /**
     * 设置应用信息链式调用。
     */
    public SysCritLog appInfo(String appInfo) {
        setAppInfo(appInfo);
        return this;
    }

    /**
     * 设置应用主机链式调用。
     */
    public SysCritLog appHost(String appHost) {
        setAppHost(appHost);
        return this;
    }

    /**
     * 重载toString方法.
     */
    @Override
    public String toString() {
        return JsonUtils.toString(this);
    }

}