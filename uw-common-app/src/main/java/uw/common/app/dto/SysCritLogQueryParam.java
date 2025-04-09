package uw.common.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.dao.annotation.QueryMeta;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
* 系统关键日志列表查询参数。
*/
@Schema(title = "系统关键日志列表查询参数", description = "系统关键日志列表查询参数")
public class SysCritLogQueryParam extends AuthPageQueryParam{

    public SysCritLogQueryParam() {
        super();
    }

    public SysCritLogQueryParam(Long saasId) {
        super(saasId);
    }
	
    /**
     * 允许的排序属性。
     * key:排序名 value:排序字段
     *
     * @return
     */
    @Override
    public Map<String, String> ALLOWED_SORT_PROPERTY() {
        return new HashMap<>() {{
            put( "id", "id" );
            put( "saasId", "saas_id" );
            put( "mchId", "mch_id" );
            put( "userId", "user_id" );
            put( "userType", "user_type" );
            put( "groupId", "group_id" );
            put( "userName", "user_name" );
            put( "nickName", "nick_name" );
            put( "realName", "real_name" );
            put( "userIp", "user_ip" );
            put( "apiUri", "api_uri" );
            put( "apiName", "api_name" );
            put( "bizType", "biz_type" );
            put( "bizId", "biz_id" );
            put( "requestDate", "request_date" );
            put( "responseState", "response_state" );
            put( "responseCode", "response_code" );
            put( "responseMillis", "response_millis" );
            put( "statusCode", "status_code" );
            put( "appInfo", "app_info" );
            put( "appHost", "app_host" );
        }};
    }

    /**
    * ID。
    */
    @QueryMeta(expr = "id=?")
    @Schema(title="ID", description = "ID")
    private Long id;

    /**
    * 数组ID。
    */
    @QueryMeta(expr = "id in (?)")
    @Schema(title="数组ID", description = "ID数组，可同时匹配多个。")
    private Long[] ids;

    /**
    * 商户ID。
    */
    @QueryMeta(expr = "mch_id=?")
    @Schema(title="商户ID", description = "商户ID")
    private Long mchId;
	
    /**
    * 用户id。
    */
    @QueryMeta(expr = "user_id=?")
    @Schema(title="用户id", description = "用户id")
    private Long userId;
	
    /**
    * 用户类型。
    */
    @QueryMeta(expr = "user_type=?")
    @Schema(title="用户类型", description = "用户类型")
    private Integer userType;
	
    /**
    * 用户组ID。
    */
    @QueryMeta(expr = "group_id=?")
    @Schema(title="用户组ID", description = "用户组ID")
    private Long groupId;
	
    /**
    * 用户名。
    */
    @QueryMeta(expr = "user_name like ?")
    @Schema(title="用户名", description = "用户名")
    private String userName;
	
    /**
    * 用户昵称。
    */
    @QueryMeta(expr = "nick_name like ?")
    @Schema(title="用户昵称", description = "用户昵称")
    private String nickName;
	
    /**
    * 真实名称。
    */
    @QueryMeta(expr = "real_name like ?")
    @Schema(title="真实名称", description = "真实名称")
    private String realName;
	
    /**
    * 用户ip。
    */
    @QueryMeta(expr = "user_ip like ?")
    @Schema(title="用户ip", description = "用户ip")
    private String userIp;
	
    /**
    * 请求uri。
    */
    @QueryMeta(expr = "api_uri like ?")
    @Schema(title="请求uri", description = "请求uri")
    private String apiUri;
	
    /**
    * API名称。
    */
    @QueryMeta(expr = "api_name like ?")
    @Schema(title="API名称", description = "API名称")
    private String apiName;
	
    /**
    * 业务类型。
    */
    @QueryMeta(expr = "biz_type like ?")
    @Schema(title="业务类型", description = "业务类型")
    private String bizType;
	
    /**
    * 业务ID。
    */
    @QueryMeta(expr = "biz_id like ?")
    @Schema(title="业务ID", description = "业务ID")
    private String bizId;
	
    /**
    * 请求时间范围。
    */
    @QueryMeta(expr = "request_date between ? and ?")
    @Schema(title="请求时间范围", description = "请求时间范围")
    private Date[] requestDateRange;

    /**
    * 响应状态。
    */
    @QueryMeta(expr = "response_state like ?")
    @Schema(title="响应状态", description = "响应状态")
    private String responseState;
	
    /**
    * 响应代码。
    */
    @QueryMeta(expr = "response_code like ?")
    @Schema(title="响应代码", description = "响应代码")
    private String responseCode;
	
    /**
    * 请求毫秒数。
    */
    @QueryMeta(expr = "response_millis=?")
    @Schema(title="请求毫秒数", description = "请求毫秒数")
    private Long responseMillis;

    /**
    * 请求毫秒数范围。
    */
    @QueryMeta(expr = "response_millis between ? and ?")
    @Schema(title="请求毫秒数范围", description = "请求毫秒数范围")
    private Long[] responseMillisRange;
	
    /**
    * 响应状态码。
    */
    @QueryMeta(expr = "status_code=?")
    @Schema(title="响应状态码", description = "响应状态码")
    private Integer statusCode;

    /**
    * 响应状态码范围。
    */
    @QueryMeta(expr = "status_code between ? and ?")
    @Schema(title="响应状态码范围", description = "响应状态码范围")
    private Integer[] statusCodeRange;
	
    /**
    * 应用信息。
    */
    @QueryMeta(expr = "app_info like ?")
    @Schema(title="应用信息", description = "应用信息")
    private String appInfo;
	
    /**
    * 应用主机。
    */
    @QueryMeta(expr = "app_host like ?")
    @Schema(title="应用主机", description = "应用主机")
    private String appHost;
	

    /**
    * 获取ID。
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
    * 设置ID链式调用。
    */
    public SysCritLogQueryParam id(Long id) {
        setId(id);
        return this;
    }

    /**
    * 获取数组ID。
    */
    public Long[] getIds() {
        return this.ids;
    }

    /**
    * 设置数组ID。
    */
    public void setIds(Long[] ids) {
        this.ids = ids;
    }

    /**
    * 设置数组ID链式调用。
    */
    public SysCritLogQueryParam ids(Long[] ids) {
        setIds(ids);
        return this;
    }

    /**
    * 获取商户ID。
    */
    public Long getMchId(){
        return this.mchId;
    }

    /**
    * 设置商户ID。
    */
    public void setMchId(Long mchId){
        this.mchId = mchId;
    }
	
    /**
    * 设置商户ID链式调用。
    */
	public SysCritLogQueryParam mchId(Long mchId){
        setMchId(mchId);
        return this;
    }
	
    /**
    * 获取用户id。
    */
    public Long getUserId(){
        return this.userId;
    }

    /**
    * 设置用户id。
    */
    public void setUserId(Long userId){
        this.userId = userId;
    }
	
    /**
    * 设置用户id链式调用。
    */
	public SysCritLogQueryParam userId(Long userId){
        setUserId(userId);
        return this;
    }
	
    /**
    * 获取用户类型。
    */
    public Integer getUserType(){
        return this.userType;
    }

    /**
    * 设置用户类型。
    */
    public void setUserType(Integer userType){
        this.userType = userType;
    }
	
    /**
    * 设置用户类型链式调用。
    */
	public SysCritLogQueryParam userType(Integer userType){
        setUserType(userType);
        return this;
    }
	
    /**
    * 获取用户组ID。
    */
    public Long getGroupId(){
        return this.groupId;
    }

    /**
    * 设置用户组ID。
    */
    public void setGroupId(Long groupId){
        this.groupId = groupId;
    }
	
    /**
    * 设置用户组ID链式调用。
    */
	public SysCritLogQueryParam groupId(Long groupId){
        setGroupId(groupId);
        return this;
    }
	
    /**
    * 获取用户名。
    */
    public String getUserName(){
        return this.userName;
    }

    /**
    * 设置用户名。
    */
    public void setUserName(String userName){
        this.userName = userName;
    }
	
    /**
    * 设置用户名链式调用。
    */
    public SysCritLogQueryParam userName(String userName) {
        setUserName(userName);
        return this;
    }
	
    /**
    * 获取用户昵称。
    */
    public String getNickName(){
        return this.nickName;
    }

    /**
    * 设置用户昵称。
    */
    public void setNickName(String nickName){
        this.nickName = nickName;
    }
	
    /**
    * 设置用户昵称链式调用。
    */
    public SysCritLogQueryParam nickName(String nickName) {
        setNickName(nickName);
        return this;
    }
	
    /**
    * 获取真实名称。
    */
    public String getRealName(){
        return this.realName;
    }

    /**
    * 设置真实名称。
    */
    public void setRealName(String realName){
        this.realName = realName;
    }
	
    /**
    * 设置真实名称链式调用。
    */
    public SysCritLogQueryParam realName(String realName) {
        setRealName(realName);
        return this;
    }
	
    /**
    * 获取用户ip。
    */
    public String getUserIp(){
        return this.userIp;
    }

    /**
    * 设置用户ip。
    */
    public void setUserIp(String userIp){
        this.userIp = userIp;
    }
	
    /**
    * 设置用户ip链式调用。
    */
    public SysCritLogQueryParam userIp(String userIp) {
        setUserIp(userIp);
        return this;
    }
	
    /**
    * 获取请求uri。
    */
    public String getApiUri(){
        return this.apiUri;
    }

    /**
    * 设置请求uri。
    */
    public void setApiUri(String apiUri){
        this.apiUri = apiUri;
    }
	
    /**
    * 设置请求uri链式调用。
    */
    public SysCritLogQueryParam apiUri(String apiUri) {
        setApiUri(apiUri);
        return this;
    }
	
    /**
    * 获取API名称。
    */
    public String getApiName(){
        return this.apiName;
    }

    /**
    * 设置API名称。
    */
    public void setApiName(String apiName){
        this.apiName = apiName;
    }
	
    /**
    * 设置API名称链式调用。
    */
    public SysCritLogQueryParam apiName(String apiName) {
        setApiName(apiName);
        return this;
    }
	
    /**
    * 获取业务类型。
    */
    public String getBizType(){
        return this.bizType;
    }

    /**
    * 设置业务类型。
    */
    public void setBizType(String bizType){
        this.bizType = bizType;
    }
	
    /**
    * 设置业务类型链式调用。
    */
    public SysCritLogQueryParam bizType(String bizType) {
        setBizType(bizType);
        return this;
    }

    /**
     * 设置业务对象类。
     */
    public void setBizTypeClass(Class bizTypeClass) {
        this.bizType = bizTypeClass.getName();
    }

    /**
     * 设置业务对象类型链式调用。
     */
    public SysCritLogQueryParam bizTypeClass(Class bizTypeClass) {
        setBizTypeClass(bizTypeClass);
        return this;
    }


    /**
    * 获取业务ID。
    */
    public String getBizId(){
        return this.bizId;
    }

    /**
    * 设置业务ID。
    */
    public void setBizId(String bizId){
        this.bizId = bizId;
    }
	
    /**
    * 设置业务ID链式调用。
    */
    public SysCritLogQueryParam bizId(String bizId) {
        setBizId(bizId);
        return this;
    }
	
    /**
    * 获取请求时间范围。
    */
    public Date[] getRequestDateRange(){
        return this.requestDateRange;
    }

    /**
    * 设置请求时间范围。
    */
    public void setRequestDateRange(Date[] requestDateRange){
        this.requestDateRange = requestDateRange;
    }
	
    /**
    * 设置请求时间范围链式调用。
    */
    public SysCritLogQueryParam requestDateRange(Date[] requestDateRange) {
        setRequestDateRange(requestDateRange);
        return this;
    }
	
    /**
    * 获取响应状态。
    */
    public String getResponseState(){
        return this.responseState;
    }

    /**
    * 设置响应状态。
    */
    public void setResponseState(String responseState){
        this.responseState = responseState;
    }
	
    /**
    * 设置响应状态链式调用。
    */
    public SysCritLogQueryParam responseState(String responseState) {
        setResponseState(responseState);
        return this;
    }
	
    /**
    * 获取响应代码。
    */
    public String getResponseCode(){
        return this.responseCode;
    }

    /**
    * 设置响应代码。
    */
    public void setResponseCode(String responseCode){
        this.responseCode = responseCode;
    }
	
    /**
    * 设置响应代码链式调用。
    */
    public SysCritLogQueryParam responseCode(String responseCode) {
        setResponseCode(responseCode);
        return this;
    }
	
    /**
    * 获取请求毫秒数。
    */
    public Long getResponseMillis(){
        return this.responseMillis;
    }

    /**
    * 设置请求毫秒数。
    */
    public void setResponseMillis(Long responseMillis){
        this.responseMillis = responseMillis;
    }
	
    /**
    * 设置请求毫秒数链式调用。
    */
    public SysCritLogQueryParam responseMillis(Long responseMillis){
        setResponseMillis(responseMillis);
        return this;
    }

    /**
    * 获取请求毫秒数范围。
    */
    public Long[] getResponseMillisRange(){
        return this.responseMillisRange;
    }

    /**
    * 设置请求毫秒数范围。
    */
    public void setResponseMillisRange(Long[] responseMillisRange){
        this.responseMillisRange = responseMillisRange;
    }
	
    /**
    * 设置请求毫秒数范围链式调用。
    */
    public SysCritLogQueryParam responseMillisRange(Long[] responseMillisRange){
        setResponseMillisRange(responseMillisRange);
        return this;
    }
	
    /**
    * 获取响应状态码。
    */
    public Integer getStatusCode(){
        return this.statusCode;
    }

    /**
    * 设置响应状态码。
    */
    public void setStatusCode(Integer statusCode){
        this.statusCode = statusCode;
    }
	
    /**
    * 设置响应状态码链式调用。
    */
    public SysCritLogQueryParam statusCode(Integer statusCode){
        setStatusCode(statusCode);
        return this;
    }

    /**
    * 获取响应状态码范围。
    */
    public Integer[] getStatusCodeRange(){
        return this.statusCodeRange;
    }

    /**
    * 设置响应状态码范围。
    */
    public void setStatusCodeRange(Integer[] statusCodeRange){
        this.statusCodeRange = statusCodeRange;
    }
	
    /**
    * 设置响应状态码范围链式调用。
    */
    public SysCritLogQueryParam statusCodeRange(Integer[] statusCodeRange){
        setStatusCodeRange(statusCodeRange);
        return this;
    }
	
    /**
    * 获取应用信息。
    */
    public String getAppInfo(){
        return this.appInfo;
    }

    /**
    * 设置应用信息。
    */
    public void setAppInfo(String appInfo){
        this.appInfo = appInfo;
    }
	
    /**
    * 设置应用信息链式调用。
    */
    public SysCritLogQueryParam appInfo(String appInfo) {
        setAppInfo(appInfo);
        return this;
    }
	
    /**
    * 获取应用主机。
    */
    public String getAppHost(){
        return this.appHost;
    }

    /**
    * 设置应用主机。
    */
    public void setAppHost(String appHost){
        this.appHost = appHost;
    }
	
    /**
    * 设置应用主机链式调用。
    */
    public SysCritLogQueryParam appHost(String appHost) {
        setAppHost(appHost);
        return this;
    }
	

}