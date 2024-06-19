package uw.app.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.dao.annotation.QueryMeta;

import java.util.Date;

/**
 * 系统关键日志列表查询参数。
 */
@Schema(title = "系统关键日志列表查询参数", description = "系统关键日志列表查询参数")
public class SysCritLogQueryParam extends AuthPageQueryParam {

    /**
     * ID
     */
    @QueryMeta(expr = "id=?")
    @Schema(title = "ID", description = "ID")
    private Long id;
    /**
     * 商户ID
     */
    @QueryMeta(expr = "mch_id=?")
    @Schema(title = "商户ID", description = "商户ID")
    private Long mchId;
    /**
     * 用户id
     */
    @QueryMeta(expr = "user_id=?")
    @Schema(title = "用户id", description = "用户id")
    private Long userId;
    /**
     * 用户类型
     */
    @QueryMeta(expr = "user_type=?")
    @Schema(title = "用户类型", description = "用户类型")
    private Integer userType;
    /**
     * 用户组ID
     */
    @QueryMeta(expr = "group_id=?")
    @Schema(title = "用户组ID", description = "用户组ID")
    private Long groupId;
    /**
     * 用户名
     */
    @QueryMeta(expr = "user_name like ?")
    @Schema(title = "用户名", description = "用户名")
    private String userName;
    /**
     * 用户昵称
     */
    @QueryMeta(expr = "nick_name like ?")
    @Schema(title = "用户昵称", description = "用户昵称")
    private String nickName;
    /**
     * 真实名称
     */
    @QueryMeta(expr = "real_name like ?")
    @Schema(title = "真实名称", description = "真实名称")
    private String realName;
    /**
     * 操作对象类型
     */
    @QueryMeta(expr = "ref_type like ?")
    @Schema(title = "操作对象类型", description = "操作对象类型")
    private String refType;
    /**
     * 操作对象id
     */
    @QueryMeta(expr = "ref_id like ?")
    @Schema(title = "操作对象id", description = "操作对象id")
    private String refId;
    /**
     * 请求uri
     */
    @QueryMeta(expr = "api_uri like ?")
    @Schema(title = "请求uri", description = "请求uri")
    private String apiUri;
    /**
     * API名称
     */
    @QueryMeta(expr = "api_name like ?")
    @Schema(title = "API名称", description = "API名称")
    private String apiName;
    /**
     * 操作状态
     */
    @QueryMeta(expr = "op_state like ?")
    @Schema(title = "操作状态", description = "操作状态")
    private String opState;
    /**
     * 请求毫秒数
     */
    @QueryMeta(expr = "response_millis=?")
    @Schema(title = "请求毫秒数", description = "请求毫秒数")
    private Long responseMillis;
    /**
     * 请求毫秒数范围
     */
    @QueryMeta(expr = "response_millis between ? and ?")
    @Schema(title = "请求毫秒数范围", description = "请求毫秒数范围")
    private Long[] responseMillisRange;
    /**
     * 响应状态码
     */
    @QueryMeta(expr = "status_code=?")
    @Schema(title = "响应状态码", description = "响应状态码")
    private Integer statusCode;
    /**
     * 响应状态码范围
     */
    @QueryMeta(expr = "status_code between ? and ?")
    @Schema(title = "响应状态码范围", description = "响应状态码范围")
    private Integer[] statusCodeRange;
    /**
     * 应用信息
     */
    @QueryMeta(expr = "app_info like ?")
    @Schema(title = "应用信息", description = "应用信息")
    private String appInfo;
    /**
     * 应用主机
     */
    @QueryMeta(expr = "app_host like ?")
    @Schema(title = "应用主机", description = "应用主机")
    private String appHost;
    /**
     * 操作人ip
     */
    @QueryMeta(expr = "user_ip like ?")
    @Schema(title = "操作人ip", description = "操作人ip")
    private String userIp;
    /**
     * 创建时间范围
     */
    @QueryMeta(expr = "request_date between ? and ?")
    @Schema(title = "创建时间范围", description = "创建时间范围")
    private Date[] requestDateRange;

    public SysCritLogQueryParam() {
        super();
    }

    public SysCritLogQueryParam(boolean ignoreException) {
        super( ignoreException );
    }

    /**
     * 获得ID。
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 设置ID。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获得商户ID。
     */
    public Long getMchId() {
        return this.mchId;
    }

    /**
     * 设置商户ID。
     */
    public void setMchId(Long mchId) {
        this.mchId = mchId;
    }

    /**
     * 获得用户id。
     */
    public Long getUserId() {
        return this.userId;
    }

    /**
     * 设置用户id。
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获得用户类型。
     */
    public Integer getUserType() {
        return this.userType;
    }

    /**
     * 设置用户类型。
     */
    public void setUserType(Integer userType) {
        this.userType = userType;
    }

    /**
     * 获得用户组ID。
     */
    public Long getGroupId() {
        return this.groupId;
    }

    /**
     * 设置用户组ID。
     */
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    /**
     * 获得用户名。
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * 设置用户名。
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * 获得用户昵称。
     */
    public String getNickName() {
        return this.nickName;
    }

    /**
     * 设置用户昵称。
     */
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    /**
     * 获得真实名称。
     */
    public String getRealName() {
        return this.realName;
    }

    /**
     * 设置真实名称。
     */
    public void setRealName(String realName) {
        this.realName = realName;
    }

    /**
     * 获得操作对象类型。
     */
    public String getRefType() {
        return this.refType;
    }

    /**
     * 设置操作对象类型。
     */
    public void setRefType(String refType) {
        this.refType = refType;
    }

    /**
     * 获得操作对象id。
     */
    public String getRefId() {
        return this.refId;
    }

    /**
     * 设置操作对象id。
     */
    public void setRefId(String refId) {
        this.refId = refId;
    }

    /**
     * 获得请求uri。
     */
    public String getApiUri() {
        return this.apiUri;
    }

    /**
     * 设置请求uri。
     */
    public void setApiUri(String apiUri) {
        this.apiUri = apiUri;
    }

    /**
     * 获得API名称。
     */
    public String getApiName() {
        return this.apiName;
    }

    /**
     * 设置API名称。
     */
    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    /**
     * 获得操作状态。
     */
    public String getOpState() {
        return this.opState;
    }

    /**
     * 设置操作状态。
     */
    public void setOpState(String opState) {
        this.opState = opState;
    }

    /**
     * 获得请求毫秒数。
     */
    public Long getResponseMillis() {
        return this.responseMillis;
    }

    /**
     * 设置请求毫秒数。
     */
    public void setResponseMillis(Long responseMillis) {
        this.responseMillis = responseMillis;
    }

    /**
     * 获得请求毫秒数范围。
     */
    public Long[] getResponseMillisRange() {
        return this.responseMillisRange;
    }

    /**
     * 设置请求毫秒数范围。
     */
    public void setResponseMillisRange(Long[] responseMillisRange) {
        this.responseMillisRange = responseMillisRange;
    }

    /**
     * 获得响应状态码。
     */
    public Integer getStatusCode() {
        return this.statusCode;
    }

    /**
     * 设置响应状态码。
     */
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * 获得响应状态码范围。
     */
    public Integer[] getStatusCodeRange() {
        return this.statusCodeRange;
    }

    /**
     * 设置响应状态码范围。
     */
    public void setStatusCodeRange(Integer[] statusCodeRange) {
        this.statusCodeRange = statusCodeRange;
    }

    /**
     * 获得应用信息。
     */
    public String getAppInfo() {
        return this.appInfo;
    }

    /**
     * 设置应用信息。
     */
    public void setAppInfo(String appInfo) {
        this.appInfo = appInfo;
    }

    /**
     * 获得应用主机。
     */
    public String getAppHost() {
        return this.appHost;
    }

    /**
     * 设置应用主机。
     */
    public void setAppHost(String appHost) {
        this.appHost = appHost;
    }

    /**
     * 获得操作人ip。
     */
    public String getUserIp() {
        return this.userIp;
    }

    /**
     * 设置操作人ip。
     */
    public void setUserIp(String userIp) {
        this.userIp = userIp;
    }

    /**
     * 获得创建时间范围。
     */
    public Date[] getRequestDateRange() {
        return this.requestDateRange;
    }

    /**
     * 设置创建时间范围。
     */
    public void setRequestDateRange(Date[] requestDateRange) {
        this.requestDateRange = requestDateRange;
    }

}