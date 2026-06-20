package uw.httpclient.http;

import java.util.Date;

/**
 * HTTP 请求日志数据接口。
 * <p>
 * 记录一次 HTTP 请求从发起到收到响应的完整上下文：请求 URL、方法、头、请求体、
 * 请求/响应时间、状态码、响应字节、响应大小、错误信息等。
 * <p>
 * 为兼容二进制与文本数据，{@link #getResponseBytes()} 保存原始字节，
 * {@link #getResponseData()} 在访问时懒转换为字符串，避免不必要的转换。
 * 可通过自定义实现类（如继承 {@link HttpDefaultData}）直接作为业务日志实体复用，
 * 减少 DTO 间拷贝。
 */
public interface HttpData {

    /**
     * 获取 HTTP 请求 URL。
     *
     * @return 请求 URL。
     */
    String getRequestUrl();

    /**
     * 设置 HTTP 请求 URL。
     *
     * @param httpUrl 请求 URL。
     */
    void setRequestUrl(String httpUrl);

    /**
     * 获取 HTTP 方法（GET/POST/PUT/PATCH/DELETE）。
     *
     * @return HTTP 方法。
     */
    String getRequestMethod();

    /**
     * 设置 HTTP 方法。
     *
     * @param httpMethod HTTP 方法。
     */
    void setRequestMethod(String httpMethod);

    /**
     * 获取请求头（多行字符串形式）。
     *
     * @return 请求头。
     */
    String getRequestHeader();

    /**
     * 设置请求头。
     *
     * @param httpHeader 请求头。
     */
    void setRequestHeader(String httpHeader);

    /**
     * 获取 HTTP 响应状态码。
     *
     * @return 状态码。
     */
    int getStatusCode();

    /**
     * 设置 HTTP 响应状态码。
     *
     * @param statusCode 状态码。
     */
    void setStatusCode(int statusCode);

    /**
     * 获取请求数据大小（字符长度）。
     *
     * @return 请求数据大小。
     */
    long getRequestSize();

    /**
     * 设置请求数据大小。
     *
     * @param requestSize 请求数据大小。
     */
    void setRequestSize(long requestSize);

    /**
     * 获取响应原始字节（二进制优先）。
     *
     * @return 响应字节，可能为 null。
     */
    byte[] getResponseBytes();

    /**
     * 设置响应原始字节。
     *
     * @param responseBytes 响应字节。
     */
    void setResponseBytes(byte[] responseBytes);

    /**
     * 获取请求体数据（受 {@link HttpDataLogLevel} 控制）。
     *
     * @return 请求体数据，未记录时为 null。
     */
    String getRequestData();

    /**
     * 设置请求体数据。
     *
     * @param requestData 请求体数据。
     */
    void setRequestData(String requestData);

    /**
     * 获取响应数据大小（字节数）。
     *
     * @return 响应数据大小。
     */
    long getResponseSize();

    /**
     * 设置响应数据大小。
     *
     * @param responseSize 响应数据大小。
     */
    void setResponseSize(long responseSize);

    /**
     * 获取响应数据字符串。
     * <p>
     * 优先返回已显式设置的 responseData；否则从 {@link #getResponseBytes()} 懒转换。
     *
     * @return 响应数据字符串，可能为 null。
     */
    String getResponseData();

    /**
     * 设置响应数据字符串。
     *
     * @param responseData 响应数据字符串。
     */
    void setResponseData(String responseData);

    /**
     * 获取请求发起时间。
     *
     * @return 请求时间。
     */
    Date getRequestDate();

    /**
     * 设置请求发起时间。
     *
     * @param requestDate 请求时间。
     */
    void setRequestDate(Date requestDate);

    /**
     * 获取响应接收时间。
     *
     * @return 响应时间。
     */
    Date getResponseDate();

    /**
     * 设置响应接收时间。
     *
     * @param responseDate 响应时间。
     */
    void setResponseDate(Date responseDate);

    /**
     * 设置响应的 Content-Type。
     *
     * @param responseType Content-Type。
     */
    void setResponseType(String responseType);

    /**
     * 获取响应的 Content-Type。
     *
     * @return Content-Type。
     */
    String getResponseType();

    /**
     * 获取错误信息。
     *
     * @return 错误信息。
     */
    String getErrorInfo();

    /**
     * 设置错误信息。
     *
     * @param errorInfo 错误信息。
     */
    void setErrorInfo(String errorInfo);


}
