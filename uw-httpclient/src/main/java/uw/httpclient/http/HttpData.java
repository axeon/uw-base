package uw.httpclient.http;

import java.util.Date;

/**
 * http日志接口。
 */
public interface HttpData {

    /**
     * 获得http请求url。
     *
     * @return
     */
    String getRequestUrl();

    /**
     * 设置http请求url。
     *
     * @param httpUrl
     */
    void setRequestUrl(String httpUrl);

    /**
     * 获得http方法。
     *
     * @return
     */
    String getRequestMethod();

    /**
     * 设置http方法。
     *
     * @param httpMethod
     */
    void setRequestMethod(String httpMethod);

    /**
     * 获得http header。
     *
     * @return
     */
    String getRequestHeader();

    /**
     * 设置http header。
     *
     * @param httpHeader
     */
    void setRequestHeader(String httpHeader);

    /**
     * 获得http状态码。
     *
     * @return
     */
    int getStatusCode();

    /**
     * 设置http状态码。
     *
     * @param statusCode
     */
    void setStatusCode(int statusCode);

    /**
     * 获得请求数据大小。
     *
     * @return
     */
    long getRequestSize();

    /**
     * 设置请求数据大小。
     */
    void setRequestSize(long requestSize);

    /**
     * 获得返回byte[]。
     *
     * @return
     */
    byte[] getResponseBytes();

    /**
     * 设置返回byte[]。
     *
     * @param responseBytes
     */
    void setResponseBytes(byte[] responseBytes);

    /**
     * 获得请求数据。
     *
     * @return
     */
    String getRequestData();

    /**
     * 设置请求数据。
     *
     * @param requestData
     */
    void setRequestData(String requestData);

    /**
     * 获得返回数据大小。
     *
     * @return
     */
    long getResponseSize();

    /**
     * 设置返回数据大小。
     */
    void setResponseSize(long responseSize);

    /**
     * 获得返回数据。
     *
     * @return
     */
    String getResponseData();

    /**
     * 设置返回数据。
     *
     * @param responseData
     */
    void setResponseData(String responseData);

    /**
     * 获得请求时间。
     *
     * @return
     */
    Date getRequestDate();

    /**
     * 设置请求时间。
     *
     * @param requestDate
     */
    void setRequestDate(Date requestDate);

    /**
     * 获得返回时间。
     *
     * @return
     */
    Date getResponseDate();

    /**
     * 设置返回时间。
     *
     * @param responseDate
     */
    void setResponseDate(Date responseDate);

    /**
     * 设置返回的contentType。
     * @param responseType
     */
    void setResponseType(String responseType);

    /**
     * 返回的contentType。
     * @return
     */
    String getResponseType();
    /**
     * 获得错误信息。
     *
     * @return
     */
    String getErrorInfo();

    /**
     * 设置错误信息。
     *
     * @param errorInfo
     */
    void setErrorInfo(String errorInfo);


}
