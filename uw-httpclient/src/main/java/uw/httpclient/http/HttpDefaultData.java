package uw.httpclient.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uw.common.util.JsonUtils;

import java.util.Date;

/**
 * http默认日志实现。
 */
public class HttpDefaultData implements HttpData {

    /**
     * http url。
     */
    private String requestUrl;

    /**
     * http方法。
     */
    private String requestMethod;

    /**
     * http header。
     */
    private String requestHeader;

    /**
     * http状态码。
     */
    private int statusCode;

    /**
     * 请求数据大小。
     */
    private long requestSize;

    /**
     * 返回数据大小。
     */
    private long responseSize;

    /**
     * 返回类型。
     */
    private String responseType;

    /**
     * 请求数据。
     */
    private String requestData;

    /**
     * 返回数据。
     */
    private String responseData;

    /**
     * 返回byte数组。
     */
    @JsonIgnore
    private byte[] responseBytes;

    /**
     * 请求时间。
     */
    private Date requestDate;

    /**
     * 返回数据。
     */
    private Date responseDate;

    /**
     * 错误信息。
     */
    private String errorInfo;

    @Override
    public String toString() {
        return JsonUtils.toString(this);
    }

    @Override
    public String getRequestUrl() {
        return requestUrl;
    }

    @Override
    public void setRequestUrl(String httpUrl) {
        this.requestUrl = httpUrl;
    }

    @Override
    public String getRequestMethod() {
        return requestMethod;
    }

    @Override
    public void setRequestMethod(String httpMethod) {
        this.requestMethod = httpMethod;
    }

    @Override
    public String getRequestHeader() {
        return requestHeader;
    }

    @Override
    public void setRequestHeader(String requestHeader) {
        this.requestHeader = requestHeader;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public long getRequestSize() {
        return requestSize;
    }

    @Override
    public void setRequestSize(long requestSize) {
        this.requestSize = requestSize;
    }

    @Override
    public byte[] getResponseBytes() {
        return responseBytes;
    }

    @Override
    public String getResponseType() {
        return responseType;
    }

    @Override
    public void setResponseBytes(byte[] responseBytes) {
        this.responseBytes = responseBytes;
    }

    @Override
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    @Override
    public String getRequestData() {
        return requestData;
    }

    @Override
    public void setRequestData(String requestData) {
        this.requestData = requestData;
    }

    @Override
    public long getResponseSize() {
        return responseSize;
    }

    @Override
    public void setResponseSize(long responseSize) {
        this.responseSize = responseSize;
    }

    @Override
    public String getResponseData() {
        if (responseData != null) {
            return responseData;
        } else {
            if (responseBytes != null) {
                responseData = new String(this.responseBytes);
                return responseData;
            } else {
                return null;
            }
        }
    }

    @Override
    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    @Override
    public Date getRequestDate() {
        return requestDate;
    }

    @Override
    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    @Override
    public Date getResponseDate() {
        return responseDate;
    }

    @Override
    public void setResponseDate(Date responseDate) {
        this.responseDate = responseDate;
    }

    @Override
    public String getErrorInfo() {
        return errorInfo;
    }

    @Override
    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }
}
