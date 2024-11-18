package uw.app.common.entity;

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
@TableMeta(tableName = "sys_crit_log", tableType = "table")
@Schema(title = "系统关键日志", description = "系统关键日志")
public class SysCritLog implements DataEntity, Serializable {


    /**
     * ID
     */
    @ColumnMeta(columnName = "id", dataType = "long", dataSize = 19, nullable = false, primaryKey = true)
    @Schema(title = "ID", description = "ID")
    private long id;

    /**
     * saasId
     */
    @ColumnMeta(columnName = "saas_id", dataType = "long", dataSize = 19, nullable = false, primaryKey = true)
    @Schema(title = "saasId", description = "saasId")
    private long saasId;

    /**
     * 商户ID
     */
    @ColumnMeta(columnName = "mch_id", dataType = "long", dataSize = 19, nullable = true)
    @Schema(title = "商户ID", description = "商户ID")
    private long mchId;

    /**
     * 用户id
     */
    @ColumnMeta(columnName = "user_id", dataType = "long", dataSize = 19, nullable = false)
    @Schema(title = "用户id", description = "用户id")
    private long userId;

    /**
     * 用户类型
     */
    @ColumnMeta(columnName = "user_type", dataType = "int", dataSize = 10, nullable = true)
    @Schema(title = "用户类型", description = "用户类型")
    private int userType;

    /**
     * 用户组ID
     */
    @ColumnMeta(columnName = "group_id", dataType = "long", dataSize = 19, nullable = true)
    @Schema(title = "用户组ID", description = "用户组ID")
    private long groupId;

    /**
     * 用户名
     */
    @ColumnMeta(columnName = "user_name", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "用户名", description = "用户名")
    private String userName;

    /**
     * 用户昵称
     */
    @ColumnMeta(columnName = "nick_name", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "用户昵称", description = "用户昵称")
    private String nickName;

    /**
     * 真实名称
     */
    @ColumnMeta(columnName = "real_name", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "真实名称", description = "真实名称")
    private String realName;

    /**
     * 操作对象类型
     */
    @ColumnMeta(columnName = "ref_type", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "操作对象类型", description = "操作对象类型")
    private String refType;

    /**
     * 操作对象id
     */
    @ColumnMeta(columnName = "ref_id", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "操作对象id", description = "操作对象id")
    private String refId;

    /**
     * 请求uri
     */
    @ColumnMeta(columnName = "api_uri", dataType = "String", dataSize = 200, nullable = true)
    @Schema(title = "请求uri", description = "请求uri")
    private String apiUri;

    /**
     * API名称
     */
    @ColumnMeta(columnName = "api_name", dataType = "String", dataSize = 200, nullable = true)
    @Schema(title = "API名称", description = "API名称")
    private String apiName;

    /**
     * 操作状态
     */
    @ColumnMeta(columnName = "op_state", dataType = "String", dataSize = 200, nullable = true)
    @Schema(title = "操作状态", description = "操作状态")
    private String opState;

    /**
     * 日志内容
     */
    @ColumnMeta(columnName = "op_log", dataType = "String", dataSize = 65535, nullable = true)
    @Schema(title = "日志内容", description = "日志内容")
    private String opLog;

    /**
     * 请求参数
     */
    @ColumnMeta(columnName = "request_body", dataType = "String", dataSize = 2147483647, nullable = true)
    @Schema(title = "请求参数", description = "请求参数")
    private String requestBody;

    /**
     * 响应日志
     */
    @ColumnMeta(columnName = "response_body", dataType = "String", dataSize = 2147483647, nullable = true)
    @Schema(title = "响应日志", description = "响应日志")
    private String responseBody;

    /**
     * 请求毫秒数
     */
    @ColumnMeta(columnName = "response_millis", dataType = "long", dataSize = 19, nullable = true)
    @Schema(title = "请求毫秒数", description = "请求毫秒数")
    private long responseMillis;

    /**
     * 异常信息
     */
    @ColumnMeta(columnName = "exception", dataType = "String", dataSize = 65535, nullable = true)
    @Schema(title = "异常信息", description = "异常信息")
    private String exception;

    /**
     * 响应状态码
     */
    @ColumnMeta(columnName = "status_code", dataType = "int", dataSize = 10, nullable = true)
    @Schema(title = "响应状态码", description = "响应状态码")
    private int statusCode;

    /**
     * 应用信息
     */
    @ColumnMeta(columnName = "app_info", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "应用信息", description = "应用信息")
    private String appInfo;

    /**
     * 应用主机
     */
    @ColumnMeta(columnName = "app_host", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "应用主机", description = "应用主机")
    private String appHost;

    /**
     * 用户ip
     */
    @ColumnMeta(columnName = "user_ip", dataType = "String", dataSize = 50, nullable = true)
    @Schema(title = "用户ip", description = "用户ip")
    private String userIp;

    /**
     * 请求时间
     */
    @ColumnMeta(columnName = "request_date", dataType = "java.util.Date", dataSize = 23, nullable = false)
    @Schema(title = "请求时间", description = "请求时间")
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
     * 获取ID。
     */
    public long getId() {
        return this.id;
    }

    /**
     * 设置ID。
     */
    public void setId(long id) {
        if (!Objects.equals( this.id, id )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "id" );
            this.UPDATED_INFO.append( "id:\"" + this.id + "\"=>\"" + id + "\"\r\n" );
            this.id = id;
        }
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
        if (!Objects.equals( this.saasId, saasId )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "saas_id" );
            this.UPDATED_INFO.append( "saas_id:\"" + this.saasId + "\"=>\"" + saasId + "\"\r\n" );
            this.saasId = saasId;
        }
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
        if (!Objects.equals( this.mchId, mchId )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "mch_id" );
            this.UPDATED_INFO.append( "mch_id:\"" + this.mchId + "\"=>\"" + mchId + "\"\r\n" );
            this.mchId = mchId;
        }
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
        if (!Objects.equals( this.userId, userId )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "user_id" );
            this.UPDATED_INFO.append( "user_id:\"" + this.userId + "\"=>\"" + userId + "\"\r\n" );
            this.userId = userId;
        }
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
        if (!Objects.equals( this.userType, userType )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "user_type" );
            this.UPDATED_INFO.append( "user_type:\"" + this.userType + "\"=>\"" + userType + "\"\r\n" );
            this.userType = userType;
        }
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
        if (!Objects.equals( this.groupId, groupId )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "group_id" );
            this.UPDATED_INFO.append( "group_id:\"" + this.groupId + "\"=>\"" + groupId + "\"\r\n" );
            this.groupId = groupId;
        }
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
        if (!Objects.equals( this.userName, userName )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "user_name" );
            this.UPDATED_INFO.append( "user_name:\"" + this.userName + "\"=>\"" + userName + "\"\r\n" );
            this.userName = userName;
        }
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
        if (!Objects.equals( this.nickName, nickName )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "nick_name" );
            this.UPDATED_INFO.append( "nick_name:\"" + this.nickName + "\"=>\"" + nickName + "\"\r\n" );
            this.nickName = nickName;
        }
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
        if (!Objects.equals( this.realName, realName )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "real_name" );
            this.UPDATED_INFO.append( "real_name:\"" + this.realName + "\"=>\"" + realName + "\"\r\n" );
            this.realName = realName;
        }
    }

    /**
     * 获取操作对象类型。
     */
    public String getRefType() {
        return this.refType;
    }

    /**
     * 设置操作对象类型。
     */
    public void setRefType(String refType) {
        if (!Objects.equals( this.refType, refType )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "ref_type" );
            this.UPDATED_INFO.append( "ref_type:\"" + this.refType + "\"=>\"" + refType + "\"\r\n" );
            this.refType = refType;
        }
    }

    /**
     * 获取操作对象id。
     */
    public String getRefId() {
        return this.refId;
    }

    /**
     * 设置操作对象id。
     */
    public void setRefId(String refId) {
        if (!Objects.equals( this.refId, refId )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "ref_id" );
            this.UPDATED_INFO.append( "ref_id:\"" + this.refId + "\"=>\"" + refId + "\"\r\n" );
            this.refId = refId;
        }
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
        if (!Objects.equals( this.apiUri, apiUri )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "api_uri" );
            this.UPDATED_INFO.append( "api_uri:\"" + this.apiUri + "\"=>\"" + apiUri + "\"\r\n" );
            this.apiUri = apiUri;
        }
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
        if (!Objects.equals( this.apiName, apiName )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "api_name" );
            this.UPDATED_INFO.append( "api_name:\"" + this.apiName + "\"=>\"" + apiName + "\"\r\n" );
            this.apiName = apiName;
        }
    }

    /**
     * 获取操作状态。
     */
    public String getOpState() {
        return this.opState;
    }

    /**
     * 设置操作状态。
     */
    public void setOpState(String opState) {
        if (!Objects.equals( this.opState, opState )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "op_state" );
            this.UPDATED_INFO.append( "op_state:\"" + this.opState + "\"=>\"" + opState + "\"\r\n" );
            this.opState = opState;
        }
    }

    /**
     * 获取日志内容。
     */
    public String getOpLog() {
        return this.opLog;
    }

    /**
     * 设置日志内容。
     */
    public void setOpLog(String opLog) {
        if (!Objects.equals( this.opLog, opLog )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "op_log" );
            this.UPDATED_INFO.append( "op_log:\"" + this.opLog + "\"=>\"" + opLog + "\"\r\n" );
            this.opLog = opLog;
        }
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
        if (!Objects.equals( this.requestBody, requestBody )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "request_body" );
            this.UPDATED_INFO.append( "request_body:\"" + this.requestBody + "\"=>\"" + requestBody + "\"\r\n" );
            this.requestBody = requestBody;
        }
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
        if (!Objects.equals( this.responseBody, responseBody )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "response_body" );
            this.UPDATED_INFO.append( "response_body:\"" + this.responseBody + "\"=>\"" + responseBody + "\"\r\n" );
            this.responseBody = responseBody;
        }
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
        if (!Objects.equals( this.responseMillis, responseMillis )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "response_millis" );
            this.UPDATED_INFO.append( "response_millis:\"" + this.responseMillis + "\"=>\"" + responseMillis + "\"\r\n" );
            this.responseMillis = responseMillis;
        }
    }

    /**
     * 获取异常信息。
     */
    public String getException() {
        return this.exception;
    }

    /**
     * 设置异常信息。
     */
    public void setException(String exception) {
        if (!Objects.equals( this.exception, exception )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "exception" );
            this.UPDATED_INFO.append( "exception:\"" + this.exception + "\"=>\"" + exception + "\"\r\n" );
            this.exception = exception;
        }
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
        if (!Objects.equals( this.statusCode, statusCode )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "status_code" );
            this.UPDATED_INFO.append( "status_code:\"" + this.statusCode + "\"=>\"" + statusCode + "\"\r\n" );
            this.statusCode = statusCode;
        }
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
        if (!Objects.equals( this.appInfo, appInfo )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "app_info" );
            this.UPDATED_INFO.append( "app_info:\"" + this.appInfo + "\"=>\"" + appInfo + "\"\r\n" );
            this.appInfo = appInfo;
        }
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
        if (!Objects.equals( this.appHost, appHost )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "app_host" );
            this.UPDATED_INFO.append( "app_host:\"" + this.appHost + "\"=>\"" + appHost + "\"\r\n" );
            this.appHost = appHost;
        }
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
        if (!Objects.equals( this.userIp, userIp )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "user_ip" );
            this.UPDATED_INFO.append( "user_ip:\"" + this.userIp + "\"=>\"" + userIp + "\"\r\n" );
            this.userIp = userIp;
        }
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
        if (!Objects.equals( this.requestDate, requestDate )) {
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add( "request_date" );
            this.UPDATED_INFO.append( "request_date:\"" + this.requestDate + "\"=>\"" + requestDate + "\"\r\n" );
            this.requestDate = requestDate;
        }
    }

    /**
     * 重载toString方法.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append( "id:\"" + this.id + "\"\r\n" );
        sb.append( "saas_id:\"" + this.saasId + "\"\r\n" );
        sb.append( "mch_id:\"" + this.mchId + "\"\r\n" );
        sb.append( "user_id:\"" + this.userId + "\"\r\n" );
        sb.append( "user_type:\"" + this.userType + "\"\r\n" );
        sb.append( "group_id:\"" + this.groupId + "\"\r\n" );
        sb.append( "user_name:\"" + this.userName + "\"\r\n" );
        sb.append( "nick_name:\"" + this.nickName + "\"\r\n" );
        sb.append( "real_name:\"" + this.realName + "\"\r\n" );
        sb.append( "ref_type:\"" + this.refType + "\"\r\n" );
        sb.append( "ref_id:\"" + this.refId + "\"\r\n" );
        sb.append( "api_uri:\"" + this.apiUri + "\"\r\n" );
        sb.append( "api_name:\"" + this.apiName + "\"\r\n" );
        sb.append( "op_state:\"" + this.opState + "\"\r\n" );
        sb.append( "op_log:\"" + this.opLog + "\"\r\n" );
        sb.append( "request_body:\"" + this.requestBody + "\"\r\n" );
        sb.append( "response_body:\"" + this.responseBody + "\"\r\n" );
        sb.append( "response_millis:\"" + this.responseMillis + "\"\r\n" );
        sb.append( "exception:\"" + this.exception + "\"\r\n" );
        sb.append( "status_code:\"" + this.statusCode + "\"\r\n" );
        sb.append( "app_info:\"" + this.appInfo + "\"\r\n" );
        sb.append( "app_host:\"" + this.appHost + "\"\r\n" );
        sb.append( "user_ip:\"" + this.userIp + "\"\r\n" );
        sb.append( "request_date:\"" + this.requestDate + "\"\r\n" );
        return sb.toString();
    }

    /**
     * 初始化set相关的信息.
     */
    private void _INIT_UPDATE_INFO() {
        this.UPDATED_COLUMN = new HashSet<String>();
        this.UPDATED_INFO = new StringBuilder( "表sys_crit_log主键\"" +
                this.id + "\"更新为:\r\n" );
    }

}