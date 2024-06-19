package uw.app.common.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import uw.dao.DataEntity;
import uw.dao.annotation.ColumnMeta;
import uw.dao.annotation.TableMeta;

/**
 * SysCritLog实体类
 * 系统关键日志
 *
 * @author axeon
 */
@TableMeta(tableName="sys_crit_log",tableType="table")
@Schema(title = "系统关键日志", description = "系统关键日志")
public class SysCritLog implements DataEntity,Serializable{


    /**
     * ID
     */
    @ColumnMeta(columnName="id", dataType="long", dataSize=19, nullable=false, primaryKey=true)
    @Schema(title = "ID", description = "ID")
    private long id;

    /**
     * saasId
     */
    @ColumnMeta(columnName="saas_id", dataType="long", dataSize=19, nullable=false, primaryKey=true)
    @Schema(title = "saasId", description = "saasId")
    private long saasId;

    /**
     * 商户ID
     */
    @ColumnMeta(columnName="mch_id", dataType="long", dataSize=19, nullable=true)
    @Schema(title = "商户ID", description = "商户ID")
    private long mchId;

    /**
     * 用户id
     */
    @ColumnMeta(columnName="user_id", dataType="long", dataSize=19, nullable=false)
    @Schema(title = "用户id", description = "用户id")
    private long userId;

    /**
     * 用户类型
     */
    @ColumnMeta(columnName="user_type", dataType="int", dataSize=10, nullable=true)
    @Schema(title = "用户类型", description = "用户类型")
    private int userType;

    /**
     * 用户组ID
     */
    @ColumnMeta(columnName="group_id", dataType="long", dataSize=19, nullable=true)
    @Schema(title = "用户组ID", description = "用户组ID")
    private long groupId;

    /**
     * 用户名
     */
    @ColumnMeta(columnName="user_name", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "用户名", description = "用户名")
    private String userName;

    /**
     * 用户昵称
     */
    @ColumnMeta(columnName="nick_name", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "用户昵称", description = "用户昵称")
    private String nickName;

    /**
     * 真实名称
     */
    @ColumnMeta(columnName="real_name", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "真实名称", description = "真实名称")
    private String realName;

    /**
     * 操作对象类型
     */
    @ColumnMeta(columnName="ref_type", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "操作对象类型", description = "操作对象类型")
    private String refType;

    /**
     * 操作对象id
     */
    @ColumnMeta(columnName="ref_id", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "操作对象id", description = "操作对象id")
    private String refId;

    /**
     * 请求uri
     */
    @ColumnMeta(columnName="api_uri", dataType="String", dataSize=200, nullable=true)
    @Schema(title = "请求uri", description = "请求uri")
    private String apiUri;

    /**
     * API名称
     */
    @ColumnMeta(columnName="api_name", dataType="String", dataSize=200, nullable=true)
    @Schema(title = "API名称", description = "API名称")
    private String apiName;

    /**
     * 操作状态
     */
    @ColumnMeta(columnName="op_state", dataType="String", dataSize=200, nullable=true)
    @Schema(title = "操作状态", description = "操作状态")
    private String opState;

    /**
     * 日志内容
     */
    @ColumnMeta(columnName="op_log", dataType="String", dataSize=65535, nullable=true)
    @Schema(title = "日志内容", description = "日志内容")
    private String opLog;

    /**
     * 请求参数
     */
    @ColumnMeta(columnName="request_body", dataType="String", dataSize=2147483647, nullable=true)
    @Schema(title = "请求参数", description = "请求参数")
    private String requestBody;

    /**
     * 响应日志
     */
    @ColumnMeta(columnName="response_body", dataType="String", dataSize=2147483647, nullable=true)
    @Schema(title = "响应日志", description = "响应日志")
    private String responseBody;

    /**
     * 请求毫秒数
     */
    @ColumnMeta(columnName="response_millis", dataType="long", dataSize=19, nullable=true)
    @Schema(title = "请求毫秒数", description = "请求毫秒数")
    private long responseMillis;

    /**
     * 异常信息
     */
    @ColumnMeta(columnName="exception", dataType="String", dataSize=65535, nullable=true)
    @Schema(title = "异常信息", description = "异常信息")
    private String exception;

    /**
     * 响应状态码
     */
    @ColumnMeta(columnName="status_code", dataType="int", dataSize=10, nullable=true)
    @Schema(title = "响应状态码", description = "响应状态码")
    private int statusCode;

    /**
     * 应用信息
     */
    @ColumnMeta(columnName="app_info", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "应用信息", description = "应用信息")
    private String appInfo;

    /**
     * 应用主机
     */
    @ColumnMeta(columnName="app_host", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "应用主机", description = "应用主机")
    private String appHost;

    /**
     * 操作人ip
     */
    @ColumnMeta(columnName="user_ip", dataType="String", dataSize=50, nullable=true)
    @Schema(title = "操作人ip", description = "操作人ip")
    private String userIp;

    /**
     * 创建时间
     */
    @ColumnMeta(columnName="request_date", dataType="java.util.Date", dataSize=23, nullable=false)
    @Schema(title = "创建时间", description = "创建时间")
    private java.util.Date requestDate;

    /**
     * 轻量级状态下更新列表list.
     */
    private transient Set<String> UPDATED_COLUMN = null;

    /**
     * 更新的信息.
     */
    private transient StringBuilder UPDATED_INFO = null;

    /**
     * 获得更改的字段列表.
     */
    @Override
    public Set<String> GET_UPDATED_COLUMN() {
        return UPDATED_COLUMN;
    }

    /**
     * 获得文本更新信息.
     */
    @Override
    public String GET_UPDATED_INFO() {
        if (this.UPDATED_INFO == null) {
            return null;
        } else {
            return this.UPDATED_INFO.toString();
        }
    }

    /**
     * 清除更新信息.
     */
    @Override
    public void CLEAR_UPDATED_INFO() {
        UPDATED_COLUMN = null;
        UPDATED_INFO = null;
    }

    /**
     * 初始化set相关的信息.
     */
    private void _INIT_UPDATE_INFO() {
        this.UPDATED_COLUMN = new HashSet<String>();
        this.UPDATED_INFO = new StringBuilder("表sys_crit_log主键\"" + 
        this.id+ "\"更新为:\r\n");
    }


    /**
     * 获得ID。
     */
    public long getId(){
        return this.id;
    }

    /**
     * 获得saasId。
     */
    public long getSaasId(){
        return this.saasId;
    }

    /**
     * 获得商户ID。
     */
    public long getMchId(){
        return this.mchId;
    }

    /**
     * 获得用户id。
     */
    public long getUserId(){
        return this.userId;
    }

    /**
     * 获得用户类型。
     */
    public int getUserType(){
        return this.userType;
    }

    /**
     * 获得用户组ID。
     */
    public long getGroupId(){
        return this.groupId;
    }

    /**
     * 获得用户名。
     */
    public String getUserName(){
        return this.userName;
    }

    /**
     * 获得用户昵称。
     */
    public String getNickName(){
        return this.nickName;
    }

    /**
     * 获得真实名称。
     */
    public String getRealName(){
        return this.realName;
    }

    /**
     * 获得操作对象类型。
     */
    public String getRefType(){
        return this.refType;
    }

    /**
     * 获得操作对象id。
     */
    public String getRefId(){
        return this.refId;
    }

    /**
     * 获得请求uri。
     */
    public String getApiUri(){
        return this.apiUri;
    }

    /**
     * 获得API名称。
     */
    public String getApiName(){
        return this.apiName;
    }

    /**
     * 获得操作状态。
     */
    public String getOpState(){
        return this.opState;
    }

    /**
     * 获得日志内容。
     */
    public String getOpLog(){
        return this.opLog;
    }

    /**
     * 获得请求参数。
     */
    public String getRequestBody(){
        return this.requestBody;
    }

    /**
     * 获得响应日志。
     */
    public String getResponseBody(){
        return this.responseBody;
    }

    /**
     * 获得请求毫秒数。
     */
    public long getResponseMillis(){
        return this.responseMillis;
    }

    /**
     * 获得异常信息。
     */
    public String getException(){
        return this.exception;
    }

    /**
     * 获得响应状态码。
     */
    public int getStatusCode(){
        return this.statusCode;
    }

    /**
     * 获得应用信息。
     */
    public String getAppInfo(){
        return this.appInfo;
    }

    /**
     * 获得应用主机。
     */
    public String getAppHost(){
        return this.appHost;
    }

    /**
     * 获得操作人ip。
     */
    public String getUserIp(){
        return this.userIp;
    }

    /**
     * 获得创建时间。
     */
    public java.util.Date getRequestDate(){
        return this.requestDate;
    }


    /**
     * 设置ID。
     */
    public void setId(long id){
        if ((!String.valueOf(this.id).equals(String.valueOf(id)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("id");
            this.UPDATED_INFO.append("id:\"" + this.id+ "\"=>\"" + id + "\"\r\n");
            this.id = id;
        }
    }

    /**
     * 设置saasId。
     */
    public void setSaasId(long saasId){
        if ((!String.valueOf(this.saasId).equals(String.valueOf(saasId)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("saas_id");
            this.UPDATED_INFO.append("saas_id:\"" + this.saasId+ "\"=>\"" + saasId + "\"\r\n");
            this.saasId = saasId;
        }
    }

    /**
     * 设置商户ID。
     */
    public void setMchId(long mchId){
        if ((!String.valueOf(this.mchId).equals(String.valueOf(mchId)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("mch_id");
            this.UPDATED_INFO.append("mch_id:\"" + this.mchId+ "\"=>\"" + mchId + "\"\r\n");
            this.mchId = mchId;
        }
    }

    /**
     * 设置用户id。
     */
    public void setUserId(long userId){
        if ((!String.valueOf(this.userId).equals(String.valueOf(userId)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("user_id");
            this.UPDATED_INFO.append("user_id:\"" + this.userId+ "\"=>\"" + userId + "\"\r\n");
            this.userId = userId;
        }
    }

    /**
     * 设置用户类型。
     */
    public void setUserType(int userType){
        if ((!String.valueOf(this.userType).equals(String.valueOf(userType)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("user_type");
            this.UPDATED_INFO.append("user_type:\"" + this.userType+ "\"=>\"" + userType + "\"\r\n");
            this.userType = userType;
        }
    }

    /**
     * 设置用户组ID。
     */
    public void setGroupId(long groupId){
        if ((!String.valueOf(this.groupId).equals(String.valueOf(groupId)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("group_id");
            this.UPDATED_INFO.append("group_id:\"" + this.groupId+ "\"=>\"" + groupId + "\"\r\n");
            this.groupId = groupId;
        }
    }

    /**
     * 设置用户名。
     */
    public void setUserName(String userName){
        if ((!String.valueOf(this.userName).equals(String.valueOf(userName)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("user_name");
            this.UPDATED_INFO.append("user_name:\"" + this.userName+ "\"=>\"" + userName + "\"\r\n");
            this.userName = userName;
        }
    }

    /**
     * 设置用户昵称。
     */
    public void setNickName(String nickName){
        if ((!String.valueOf(this.nickName).equals(String.valueOf(nickName)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("nick_name");
            this.UPDATED_INFO.append("nick_name:\"" + this.nickName+ "\"=>\"" + nickName + "\"\r\n");
            this.nickName = nickName;
        }
    }

    /**
     * 设置真实名称。
     */
    public void setRealName(String realName){
        if ((!String.valueOf(this.realName).equals(String.valueOf(realName)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("real_name");
            this.UPDATED_INFO.append("real_name:\"" + this.realName+ "\"=>\"" + realName + "\"\r\n");
            this.realName = realName;
        }
    }

    /**
     * 设置操作对象类型。
     */
    public void setRefType(String refType){
        if ((!String.valueOf(this.refType).equals(String.valueOf(refType)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("ref_type");
            this.UPDATED_INFO.append("ref_type:\"" + this.refType+ "\"=>\"" + refType + "\"\r\n");
            this.refType = refType;
        }
    }

    /**
     * 设置操作对象id。
     */
    public void setRefId(String refId){
        if ((!String.valueOf(this.refId).equals(String.valueOf(refId)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("ref_id");
            this.UPDATED_INFO.append("ref_id:\"" + this.refId+ "\"=>\"" + refId + "\"\r\n");
            this.refId = refId;
        }
    }

    /**
     * 设置请求uri。
     */
    public void setApiUri(String apiUri){
        if ((!String.valueOf(this.apiUri).equals(String.valueOf(apiUri)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("api_uri");
            this.UPDATED_INFO.append("api_uri:\"" + this.apiUri+ "\"=>\"" + apiUri + "\"\r\n");
            this.apiUri = apiUri;
        }
    }

    /**
     * 设置API名称。
     */
    public void setApiName(String apiName){
        if ((!String.valueOf(this.apiName).equals(String.valueOf(apiName)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("api_name");
            this.UPDATED_INFO.append("api_name:\"" + this.apiName+ "\"=>\"" + apiName + "\"\r\n");
            this.apiName = apiName;
        }
    }

    /**
     * 设置操作状态。
     */
    public void setOpState(String opState){
        if ((!String.valueOf(this.opState).equals(String.valueOf(opState)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("op_state");
            this.UPDATED_INFO.append("op_state:\"" + this.opState+ "\"=>\"" + opState + "\"\r\n");
            this.opState = opState;
        }
    }

    /**
     * 设置日志内容。
     */
    public void setOpLog(String opLog){
        if ((!String.valueOf(this.opLog).equals(String.valueOf(opLog)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("op_log");
            this.UPDATED_INFO.append("op_log:\"" + this.opLog+ "\"=>\"" + opLog + "\"\r\n");
            this.opLog = opLog;
        }
    }

    /**
     * 设置请求参数。
     */
    public void setRequestBody(String requestBody){
        if ((!String.valueOf(this.requestBody).equals(String.valueOf(requestBody)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("request_body");
            this.UPDATED_INFO.append("request_body:\"" + this.requestBody+ "\"=>\"" + requestBody + "\"\r\n");
            this.requestBody = requestBody;
        }
    }

    /**
     * 设置响应日志。
     */
    public void setResponseBody(String responseBody){
        if ((!String.valueOf(this.responseBody).equals(String.valueOf(responseBody)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("response_body");
            this.UPDATED_INFO.append("response_body:\"" + this.responseBody+ "\"=>\"" + responseBody + "\"\r\n");
            this.responseBody = responseBody;
        }
    }

    /**
     * 设置请求毫秒数。
     */
    public void setResponseMillis(long responseMillis){
        if ((!String.valueOf(this.responseMillis).equals(String.valueOf(responseMillis)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("response_millis");
            this.UPDATED_INFO.append("response_millis:\"" + this.responseMillis+ "\"=>\"" + responseMillis + "\"\r\n");
            this.responseMillis = responseMillis;
        }
    }

    /**
     * 设置异常信息。
     */
    public void setException(String exception){
        if ((!String.valueOf(this.exception).equals(String.valueOf(exception)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("exception");
            this.UPDATED_INFO.append("exception:\"" + this.exception+ "\"=>\"" + exception + "\"\r\n");
            this.exception = exception;
        }
    }

    /**
     * 设置响应状态码。
     */
    public void setStatusCode(int statusCode){
        if ((!String.valueOf(this.statusCode).equals(String.valueOf(statusCode)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("status_code");
            this.UPDATED_INFO.append("status_code:\"" + this.statusCode+ "\"=>\"" + statusCode + "\"\r\n");
            this.statusCode = statusCode;
        }
    }

    /**
     * 设置应用信息。
     */
    public void setAppInfo(String appInfo){
        if ((!String.valueOf(this.appInfo).equals(String.valueOf(appInfo)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("app_info");
            this.UPDATED_INFO.append("app_info:\"" + this.appInfo+ "\"=>\"" + appInfo + "\"\r\n");
            this.appInfo = appInfo;
        }
    }

    /**
     * 设置应用主机。
     */
    public void setAppHost(String appHost){
        if ((!String.valueOf(this.appHost).equals(String.valueOf(appHost)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("app_host");
            this.UPDATED_INFO.append("app_host:\"" + this.appHost+ "\"=>\"" + appHost + "\"\r\n");
            this.appHost = appHost;
        }
    }

    /**
     * 设置操作人ip。
     */
    public void setUserIp(String userIp){
        if ((!String.valueOf(this.userIp).equals(String.valueOf(userIp)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("user_ip");
            this.UPDATED_INFO.append("user_ip:\"" + this.userIp+ "\"=>\"" + userIp + "\"\r\n");
            this.userIp = userIp;
        }
    }

    /**
     * 设置创建时间。
     */
    public void setRequestDate(java.util.Date requestDate){
        if ((!String.valueOf(this.requestDate).equals(String.valueOf(requestDate)))) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("request_date");
            this.UPDATED_INFO.append("request_date:\"" + this.requestDate+ "\"=>\"" + requestDate + "\"\r\n");
            this.requestDate = requestDate;
        }
    }

    /**
     * 重载toString方法.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id:\"" + this.id + "\"\r\n");
        sb.append("saas_id:\"" + this.saasId + "\"\r\n");
        sb.append("mch_id:\"" + this.mchId + "\"\r\n");
        sb.append("user_id:\"" + this.userId + "\"\r\n");
        sb.append("user_type:\"" + this.userType + "\"\r\n");
        sb.append("group_id:\"" + this.groupId + "\"\r\n");
        sb.append("user_name:\"" + this.userName + "\"\r\n");
        sb.append("nick_name:\"" + this.nickName + "\"\r\n");
        sb.append("real_name:\"" + this.realName + "\"\r\n");
        sb.append("ref_type:\"" + this.refType + "\"\r\n");
        sb.append("ref_id:\"" + this.refId + "\"\r\n");
        sb.append("api_uri:\"" + this.apiUri + "\"\r\n");
        sb.append("api_name:\"" + this.apiName + "\"\r\n");
        sb.append("op_state:\"" + this.opState + "\"\r\n");
        sb.append("op_log:\"" + this.opLog + "\"\r\n");
        sb.append("request_body:\"" + this.requestBody + "\"\r\n");
        sb.append("response_body:\"" + this.responseBody + "\"\r\n");
        sb.append("response_millis:\"" + this.responseMillis + "\"\r\n");
        sb.append("exception:\"" + this.exception + "\"\r\n");
        sb.append("status_code:\"" + this.statusCode + "\"\r\n");
        sb.append("app_info:\"" + this.appInfo + "\"\r\n");
        sb.append("app_host:\"" + this.appHost + "\"\r\n");
        sb.append("user_ip:\"" + this.userIp + "\"\r\n");
        sb.append("request_date:\"" + this.requestDate + "\"\r\n");
        return sb.toString();
    }

}