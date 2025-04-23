package uw.common.app.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.dao.DataEntity;
import uw.dao.annotation.ColumnMeta;
import uw.dao.annotation.TableMeta;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
    @Schema(title = "ID", description = "ID", maxLength=19, nullable=false )
    private long id;

    /**
     * saasId
     */
    @ColumnMeta(columnName="saas_id", dataType="long", dataSize=19, nullable=false, primaryKey=true)
    @Schema(title = "saasId", description = "saasId", maxLength=19, nullable=false )
    private long saasId;

    /**
     * 商户ID
     */
    @ColumnMeta(columnName="mch_id", dataType="long", dataSize=19, nullable=true)
    @Schema(title = "商户ID", description = "商户ID", maxLength=19, nullable=true )
    private long mchId;

    /**
     * 用户id
     */
    @ColumnMeta(columnName="user_id", dataType="long", dataSize=19, nullable=false)
    @Schema(title = "用户id", description = "用户id", maxLength=19, nullable=false )
    private long userId;

    /**
     * 用户类型
     */
    @ColumnMeta(columnName="user_type", dataType="int", dataSize=10, nullable=true)
    @Schema(title = "用户类型", description = "用户类型", maxLength=10, nullable=true )
    private int userType;

    /**
     * 用户组ID
     */
    @ColumnMeta(columnName="group_id", dataType="long", dataSize=19, nullable=true)
    @Schema(title = "用户组ID", description = "用户组ID", maxLength=19, nullable=true )
    private long groupId;

    /**
     * 用户名
     */
    @ColumnMeta(columnName="user_name", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "用户名", description = "用户名", maxLength=100, nullable=true )
    private String userName;

    /**
     * 用户昵称
     */
    @ColumnMeta(columnName="nick_name", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "用户昵称", description = "用户昵称", maxLength=100, nullable=true )
    private String nickName;

    /**
     * 真实名称
     */
    @ColumnMeta(columnName="real_name", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "真实名称", description = "真实名称", maxLength=100, nullable=true )
    private String realName;

    /**
     * 用户ip
     */
    @ColumnMeta(columnName="user_ip", dataType="String", dataSize=50, nullable=true)
    @Schema(title = "用户ip", description = "用户ip", maxLength=50, nullable=true )
    private String userIp;

    /**
     * 请求uri
     */
    @ColumnMeta(columnName="api_uri", dataType="String", dataSize=200, nullable=true)
    @Schema(title = "请求uri", description = "请求uri", maxLength=200, nullable=true )
    private String apiUri;

    /**
     * API名称
     */
    @ColumnMeta(columnName="api_name", dataType="String", dataSize=200, nullable=true)
    @Schema(title = "API名称", description = "API名称", maxLength=200, nullable=true )
    private String apiName;

    /**
     * 业务类型
     */
    @ColumnMeta(columnName="biz_type", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "业务类型", description = "业务类型", maxLength=100, nullable=true )
    private String bizType;

    /**
     * 业务ID
     */
    @ColumnMeta(columnName="biz_id", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "业务ID", description = "业务ID", maxLength=100, nullable=true )
    private String bizId;

    /**
     * 业务日志
     */
    @ColumnMeta(columnName="biz_log", dataType="String", dataSize=65535, nullable=true)
    @Schema(title = "业务日志", description = "业务日志", maxLength=65535, nullable=true )
    private String bizLog;

    /**
     * 请求时间
     */
    @ColumnMeta(columnName="request_date", dataType="java.util.Date", dataSize=23, nullable=false)
    @Schema(title = "请求时间", description = "请求时间", maxLength=23, nullable=false )
    private java.util.Date requestDate;

    /**
     * 请求参数
     */
    @ColumnMeta(columnName="request_body", dataType="String", dataSize=2147483646, nullable=true)
    @Schema(title = "请求参数", description = "请求参数", maxLength=2147483646, nullable=true )
    private String requestBody;

    /**
     * 响应状态
     */
    @ColumnMeta(columnName="response_state", dataType="String", dataSize=10, nullable=true)
    @Schema(title = "响应状态", description = "响应状态", maxLength=10, nullable=true )
    private String responseState;

    /**
     * 响应代码
     */
    @ColumnMeta(columnName="response_code", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "响应代码", description = "响应代码", maxLength=100, nullable=true )
    private String responseCode;

    /**
     * 响应消息
     */
    @ColumnMeta(columnName="response_msg", dataType="String", dataSize=1000, nullable=true)
    @Schema(title = "响应消息", description = "响应消息", maxLength=1000, nullable=true )
    private String responseMsg;

    /**
     * 响应日志
     */
    @ColumnMeta(columnName="response_body", dataType="String", dataSize=2147483646, nullable=true)
    @Schema(title = "响应日志", description = "响应日志", maxLength=2147483646, nullable=true )
    private String responseBody;

    /**
     * 请求毫秒数
     */
    @ColumnMeta(columnName="response_millis", dataType="long", dataSize=19, nullable=true)
    @Schema(title = "请求毫秒数", description = "请求毫秒数", maxLength=19, nullable=true )
    private long responseMillis;

    /**
     * 响应状态码
     */
    @ColumnMeta(columnName="status_code", dataType="int", dataSize=10, nullable=true)
    @Schema(title = "响应状态码", description = "响应状态码", maxLength=10, nullable=true )
    private int statusCode;

    /**
     * 应用信息
     */
    @ColumnMeta(columnName="app_info", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "应用信息", description = "应用信息", maxLength=100, nullable=true )
    private String appInfo;

    /**
     * 应用主机
     */
    @ColumnMeta(columnName="app_host", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "应用主机", description = "应用主机", maxLength=100, nullable=true )
    private String appHost;

    /**
     * 轻量级状态下更新列表list.
     */
    private transient Set<String> UPDATED_COLUMN = null;

    /**
     * 更新的信息.
     */
    private transient StringBuilder UPDATED_INFO = null;


    /**
     * 获得实体的表名。
     */
    @Override
    public String ENTITY_TABLE(){
         return "sys_crit_log";
       }

    /**
     * 获得实体的表注释。
     */
    @Override
    public String ENTITY_NAME(){
          return "系统关键日志";
       }

    /**
     * 获得主键
     */
    @Override
    public Serializable ENTITY_ID(){
          return getId();
       }


    /**
     * 获取更改的字段列表.
     */
    @Override
    public Set<String> GET_UPDATED_COLUMN() {
        return UPDATED_COLUMN;
    }

    /**
     * 获取文本更新信息.
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
     * 获取ID。
     */
    public long getId(){
        return this.id;
    }

    /**
     * 获取saasId。
     */
    public long getSaasId(){
        return this.saasId;
    }

    /**
     * 获取商户ID。
     */
    public long getMchId(){
        return this.mchId;
    }

    /**
     * 获取用户id。
     */
    public long getUserId(){
        return this.userId;
    }

    /**
     * 获取用户类型。
     */
    public int getUserType(){
        return this.userType;
    }

    /**
     * 获取用户组ID。
     */
    public long getGroupId(){
        return this.groupId;
    }

    /**
     * 获取用户名。
     */
    public String getUserName(){
        return this.userName;
    }

    /**
     * 获取用户昵称。
     */
    public String getNickName(){
        return this.nickName;
    }

    /**
     * 获取真实名称。
     */
    public String getRealName(){
        return this.realName;
    }

    /**
     * 获取用户ip。
     */
    public String getUserIp(){
        return this.userIp;
    }

    /**
     * 获取请求uri。
     */
    public String getApiUri(){
        return this.apiUri;
    }

    /**
     * 获取API名称。
     */
    public String getApiName(){
        return this.apiName;
    }

    /**
     * 获取业务类型。
     */
    public String getBizType(){
        return this.bizType;
    }

    /**
     * 获取业务ID。
     */
    public String getBizId(){
        return this.bizId;
    }

    /**
     * 获取业务日志。
     */
    public String getBizLog(){
        return this.bizLog;
    }

    /**
     * 获取请求时间。
     */
    public java.util.Date getRequestDate(){
        return this.requestDate;
    }

    /**
     * 获取请求参数。
     */
    public String getRequestBody(){
        return this.requestBody;
    }

    /**
     * 获取响应状态。
     */
    public String getResponseState(){
        return this.responseState;
    }

    /**
     * 获取响应代码。
     */
    public String getResponseCode(){
        return this.responseCode;
    }

    /**
     * 获取响应消息。
     */
    public String getResponseMsg(){
        return this.responseMsg;
    }

    /**
     * 获取响应日志。
     */
    public String getResponseBody(){
        return this.responseBody;
    }

    /**
     * 获取请求毫秒数。
     */
    public long getResponseMillis(){
        return this.responseMillis;
    }

    /**
     * 获取响应状态码。
     */
    public int getStatusCode(){
        return this.statusCode;
    }

    /**
     * 获取应用信息。
     */
    public String getAppInfo(){
        return this.appInfo;
    }

    /**
     * 获取应用主机。
     */
    public String getAppHost(){
        return this.appHost;
    }


    /**
     * 设置ID。
     */
    public void setId(long id){
        if (!Objects.equals(this.id, id)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("id");
            this.UPDATED_INFO.append("id:\"").append(this.id).append("\"=>\"").append(id).append("\"\n");
            this.id = id;
        }
    }

    /**
     *  设置ID链式调用。
     */
    public SysCritLog id(long id){
        setId(id);
        return this;
        }

    /**
     * 设置saasId。
     */
    public void setSaasId(long saasId){
        if (!Objects.equals(this.saasId, saasId)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("saas_id");
            this.UPDATED_INFO.append("saas_id:\"").append(this.saasId).append("\"=>\"").append(saasId).append("\"\n");
            this.saasId = saasId;
        }
    }

    /**
     *  设置saasId链式调用。
     */
    public SysCritLog saasId(long saasId){
        setSaasId(saasId);
        return this;
        }

    /**
     * 设置商户ID。
     */
    public void setMchId(long mchId){
        if (!Objects.equals(this.mchId, mchId)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("mch_id");
            this.UPDATED_INFO.append("mch_id:\"").append(this.mchId).append("\"=>\"").append(mchId).append("\"\n");
            this.mchId = mchId;
        }
    }

    /**
     *  设置商户ID链式调用。
     */
    public SysCritLog mchId(long mchId){
        setMchId(mchId);
        return this;
        }

    /**
     * 设置用户id。
     */
    public void setUserId(long userId){
        if (!Objects.equals(this.userId, userId)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("user_id");
            this.UPDATED_INFO.append("user_id:\"").append(this.userId).append("\"=>\"").append(userId).append("\"\n");
            this.userId = userId;
        }
    }

    /**
     *  设置用户id链式调用。
     */
    public SysCritLog userId(long userId){
        setUserId(userId);
        return this;
        }

    /**
     * 设置用户类型。
     */
    public void setUserType(int userType){
        if (!Objects.equals(this.userType, userType)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("user_type");
            this.UPDATED_INFO.append("user_type:\"").append(this.userType).append("\"=>\"").append(userType).append("\"\n");
            this.userType = userType;
        }
    }

    /**
     *  设置用户类型链式调用。
     */
    public SysCritLog userType(int userType){
        setUserType(userType);
        return this;
        }

    /**
     * 设置用户组ID。
     */
    public void setGroupId(long groupId){
        if (!Objects.equals(this.groupId, groupId)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("group_id");
            this.UPDATED_INFO.append("group_id:\"").append(this.groupId).append("\"=>\"").append(groupId).append("\"\n");
            this.groupId = groupId;
        }
    }

    /**
     *  设置用户组ID链式调用。
     */
    public SysCritLog groupId(long groupId){
        setGroupId(groupId);
        return this;
        }

    /**
     * 设置用户名。
     */
    public void setUserName(String userName){
        if (!Objects.equals(this.userName, userName)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("user_name");
            this.UPDATED_INFO.append("user_name:\"").append(this.userName).append("\"=>\"").append(userName).append("\"\n");
            this.userName = userName;
        }
    }

    /**
     *  设置用户名链式调用。
     */
    public SysCritLog userName(String userName){
        setUserName(userName);
        return this;
        }

    /**
     * 设置用户昵称。
     */
    public void setNickName(String nickName){
        if (!Objects.equals(this.nickName, nickName)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("nick_name");
            this.UPDATED_INFO.append("nick_name:\"").append(this.nickName).append("\"=>\"").append(nickName).append("\"\n");
            this.nickName = nickName;
        }
    }

    /**
     *  设置用户昵称链式调用。
     */
    public SysCritLog nickName(String nickName){
        setNickName(nickName);
        return this;
        }

    /**
     * 设置真实名称。
     */
    public void setRealName(String realName){
        if (!Objects.equals(this.realName, realName)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("real_name");
            this.UPDATED_INFO.append("real_name:\"").append(this.realName).append("\"=>\"").append(realName).append("\"\n");
            this.realName = realName;
        }
    }

    /**
     *  设置真实名称链式调用。
     */
    public SysCritLog realName(String realName){
        setRealName(realName);
        return this;
        }

    /**
     * 设置用户ip。
     */
    public void setUserIp(String userIp){
        if (!Objects.equals(this.userIp, userIp)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("user_ip");
            this.UPDATED_INFO.append("user_ip:\"").append(this.userIp).append("\"=>\"").append(userIp).append("\"\n");
            this.userIp = userIp;
        }
    }

    /**
     *  设置用户ip链式调用。
     */
    public SysCritLog userIp(String userIp){
        setUserIp(userIp);
        return this;
        }

    /**
     * 设置请求uri。
     */
    public void setApiUri(String apiUri){
        if (!Objects.equals(this.apiUri, apiUri)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("api_uri");
            this.UPDATED_INFO.append("api_uri:\"").append(this.apiUri).append("\"=>\"").append(apiUri).append("\"\n");
            this.apiUri = apiUri;
        }
    }

    /**
     *  设置请求uri链式调用。
     */
    public SysCritLog apiUri(String apiUri){
        setApiUri(apiUri);
        return this;
        }

    /**
     * 设置API名称。
     */
    public void setApiName(String apiName){
        if (!Objects.equals(this.apiName, apiName)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("api_name");
            this.UPDATED_INFO.append("api_name:\"").append(this.apiName).append("\"=>\"").append(apiName).append("\"\n");
            this.apiName = apiName;
        }
    }

    /**
     *  设置API名称链式调用。
     */
    public SysCritLog apiName(String apiName){
        setApiName(apiName);
        return this;
        }

    /**
     * 设置业务类型。
     */
    public void setBizType(String bizType){
        if (!Objects.equals(this.bizType, bizType)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("biz_type");
            this.UPDATED_INFO.append("biz_type:\"").append(this.bizType).append("\"=>\"").append(bizType).append("\"\n");
            this.bizType = bizType;
        }
    }

    /**
     *  设置业务类型链式调用。
     */
    public SysCritLog bizType(String bizType){
        setBizType(bizType);
        return this;
        }

    /**
     * 设置业务ID。
     */
    public void setBizId(String bizId){
        if (!Objects.equals(this.bizId, bizId)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("biz_id");
            this.UPDATED_INFO.append("biz_id:\"").append(this.bizId).append("\"=>\"").append(bizId).append("\"\n");
            this.bizId = bizId;
        }
    }

    /**
     *  设置业务ID链式调用。
     */
    public SysCritLog bizId(String bizId){
        setBizId(bizId);
        return this;
        }

    /**
     * 设置业务日志。
     */
    public void setBizLog(String bizLog){
        if (!Objects.equals(this.bizLog, bizLog)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("biz_log");
            this.UPDATED_INFO.append("biz_log:\"").append(this.bizLog).append("\"=>\"").append(bizLog).append("\"\n");
            this.bizLog = bizLog;
        }
    }

    /**
     *  设置业务日志链式调用。
     */
    public SysCritLog bizLog(String bizLog){
        setBizLog(bizLog);
        return this;
        }

    /**
     * 设置请求时间。
     */
    public void setRequestDate(java.util.Date requestDate){
        if (!Objects.equals(this.requestDate, requestDate)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("request_date");
            this.UPDATED_INFO.append("request_date:\"").append(this.requestDate).append("\"=>\"").append(requestDate).append("\"\n");
            this.requestDate = requestDate;
        }
    }

    /**
     *  设置请求时间链式调用。
     */
    public SysCritLog requestDate(java.util.Date requestDate){
        setRequestDate(requestDate);
        return this;
        }

    /**
     * 设置请求参数。
     */
    public void setRequestBody(String requestBody){
        if (!Objects.equals(this.requestBody, requestBody)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("request_body");
            this.UPDATED_INFO.append("request_body:\"").append(this.requestBody).append("\"=>\"").append(requestBody).append("\"\n");
            this.requestBody = requestBody;
        }
    }

    /**
     *  设置请求参数链式调用。
     */
    public SysCritLog requestBody(String requestBody){
        setRequestBody(requestBody);
        return this;
        }

    /**
     * 设置响应状态。
     */
    public void setResponseState(String responseState){
        if (!Objects.equals(this.responseState, responseState)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("response_state");
            this.UPDATED_INFO.append("response_state:\"").append(this.responseState).append("\"=>\"").append(responseState).append("\"\n");
            this.responseState = responseState;
        }
    }

    /**
     *  设置响应状态链式调用。
     */
    public SysCritLog responseState(String responseState){
        setResponseState(responseState);
        return this;
        }

    /**
     * 设置响应代码。
     */
    public void setResponseCode(String responseCode){
        if (!Objects.equals(this.responseCode, responseCode)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("response_code");
            this.UPDATED_INFO.append("response_code:\"").append(this.responseCode).append("\"=>\"").append(responseCode).append("\"\n");
            this.responseCode = responseCode;
        }
    }

    /**
     *  设置响应代码链式调用。
     */
    public SysCritLog responseCode(String responseCode){
        setResponseCode(responseCode);
        return this;
        }

    /**
     * 设置响应消息。
     */
    public void setResponseMsg(String responseMsg){
        if (!Objects.equals(this.responseMsg, responseMsg)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("response_msg");
            this.UPDATED_INFO.append("response_msg:\"").append(this.responseMsg).append("\"=>\"").append(responseMsg).append("\"\n");
            this.responseMsg = responseMsg;
        }
    }

    /**
     *  设置响应消息链式调用。
     */
    public SysCritLog responseMsg(String responseMsg){
        setResponseMsg(responseMsg);
        return this;
        }

    /**
     * 设置响应日志。
     */
    public void setResponseBody(String responseBody){
        if (!Objects.equals(this.responseBody, responseBody)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("response_body");
            this.UPDATED_INFO.append("response_body:\"").append(this.responseBody).append("\"=>\"").append(responseBody).append("\"\n");
            this.responseBody = responseBody;
        }
    }

    /**
     *  设置响应日志链式调用。
     */
    public SysCritLog responseBody(String responseBody){
        setResponseBody(responseBody);
        return this;
        }

    /**
     * 设置请求毫秒数。
     */
    public void setResponseMillis(long responseMillis){
        if (!Objects.equals(this.responseMillis, responseMillis)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("response_millis");
            this.UPDATED_INFO.append("response_millis:\"").append(this.responseMillis).append("\"=>\"").append(responseMillis).append("\"\n");
            this.responseMillis = responseMillis;
        }
    }

    /**
     *  设置请求毫秒数链式调用。
     */
    public SysCritLog responseMillis(long responseMillis){
        setResponseMillis(responseMillis);
        return this;
        }

    /**
     * 设置响应状态码。
     */
    public void setStatusCode(int statusCode){
        if (!Objects.equals(this.statusCode, statusCode)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("status_code");
            this.UPDATED_INFO.append("status_code:\"").append(this.statusCode).append("\"=>\"").append(statusCode).append("\"\n");
            this.statusCode = statusCode;
        }
    }

    /**
     *  设置响应状态码链式调用。
     */
    public SysCritLog statusCode(int statusCode){
        setStatusCode(statusCode);
        return this;
        }

    /**
     * 设置应用信息。
     */
    public void setAppInfo(String appInfo){
        if (!Objects.equals(this.appInfo, appInfo)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("app_info");
            this.UPDATED_INFO.append("app_info:\"").append(this.appInfo).append("\"=>\"").append(appInfo).append("\"\n");
            this.appInfo = appInfo;
        }
    }

    /**
     *  设置应用信息链式调用。
     */
    public SysCritLog appInfo(String appInfo){
        setAppInfo(appInfo);
        return this;
        }

    /**
     * 设置应用主机。
     */
    public void setAppHost(String appHost){
        if (!Objects.equals(this.appHost, appHost)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("app_host");
            this.UPDATED_INFO.append("app_host:\"").append(this.appHost).append("\"=>\"").append(appHost).append("\"\n");
            this.appHost = appHost;
        }
    }

    /**
     *  设置应用主机链式调用。
     */
    public SysCritLog appHost(String appHost){
        setAppHost(appHost);
        return this;
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
        sb.append("user_ip:\"" + this.userIp + "\"\r\n");
        sb.append("api_uri:\"" + this.apiUri + "\"\r\n");
        sb.append("api_name:\"" + this.apiName + "\"\r\n");
        sb.append("biz_type:\"" + this.bizType + "\"\r\n");
        sb.append("biz_id:\"" + this.bizId + "\"\r\n");
        sb.append("biz_log:\"" + this.bizLog + "\"\r\n");
        sb.append("request_date:\"" + this.requestDate + "\"\r\n");
        sb.append("request_body:\"" + this.requestBody + "\"\r\n");
        sb.append("response_state:\"" + this.responseState + "\"\r\n");
        sb.append("response_code:\"" + this.responseCode + "\"\r\n");
        sb.append("response_msg:\"" + this.responseMsg + "\"\r\n");
        sb.append("response_body:\"" + this.responseBody + "\"\r\n");
        sb.append("response_millis:\"" + this.responseMillis + "\"\r\n");
        sb.append("status_code:\"" + this.statusCode + "\"\r\n");
        sb.append("app_info:\"" + this.appInfo + "\"\r\n");
        sb.append("app_host:\"" + this.appHost + "\"\r\n");
        return sb.toString();
    }

}