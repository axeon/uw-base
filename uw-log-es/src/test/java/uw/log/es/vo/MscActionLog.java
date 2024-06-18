package uw.log.es.vo;


import java.io.Serializable;
import java.util.Date;

/**
 * 应用Http接口操作日志
 *
 * 
 * @since 2018-05-12
 */
public class MscActionLog extends LogBaseVo implements Serializable {
    private static final long serialVersionUID = 8206859674146770491L;

    /**
     * 用户Id
     */
    private long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 运营商Id
     */
    private long saasId;

    /**
     * 商户Id
     */
    private long mchId;

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
    private String ip;

    /**
     * 业务Id,用于查询
     */
    private long objectId;

    /**
     * 业务标识类型,用于查询
     */
    private String objectType;

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
     * 响应时间
     */
    private Date responseDate;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public long getObjectId() {
        return objectId;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
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

    public Date getResponseDate() {
        return responseDate;
    }

    public void setResponseDate(Date responseDate) {
        this.responseDate = responseDate;
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
