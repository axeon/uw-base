package uw.httpclient.http;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
     * 获取完整的 HTTP 响应头（按出现顺序的多值视图）。
     * <p>
     * 实现应返回大小写不敏感（HTTP 头名本身大小写不敏感）的不可变视图，
     * 值为同名头可能出现多次（如 {@code Set-Cookie}），故用 {@code List<String>}。
     * 未设置或无响应时可能返回 null 或空 Map。
     *
     * @return 响应头多值 Map，可能为 null。
     */
    Map<String, List<String>> getResponseHeaders();

    /**
     * 设置完整的 HTTP 响应头。
     *
     * @param responseHeaders 响应头多值 Map。
     */
    void setResponseHeaders(Map<String, List<String>> responseHeaders);

    /**
     * 获取 HTTP 响应状态消息（reason phrase，如 "Not Found"）。
     *
     * @return 状态消息，可能为 null。
     */
    String getResponseMessage();

    /**
     * 设置 HTTP 响应状态消息。
     *
     * @param responseMessage 状态消息。
     */
    void setResponseMessage(String responseMessage);

    /**
     * 获取本次请求整体耗时（毫秒）。
     * <p>
     * 基于 OkHttp 的 {@code receivedResponseAtMillis - sentRequestAtMillis}，
     * 已包含连接建立、重试与响应体传输耗时，比 {@code responseDate - requestDate} 更贴近实际网络耗时。
     * -1 表示未设置。
     *
     * @return 耗时毫秒数，未设置时为 -1。
     */
    long getElapsedMillis();

    /**
     * 设置本次请求整体耗时（毫秒）。
     *
     * @param elapsedMillis 耗时毫秒数。
     */
    void setElapsedMillis(long elapsedMillis);

    /**
     * 获取本次请求的重试/重定向次数。
     * <p>
     * 定义为「除首次外的额外尝试次数」，包含连接失败重试与 follow-up（重定向、认证重试）。
     * 由内部网络拦截器统计每次真实网络请求得到。{@code retryOnConnectionFailure=false} 时恒为 0。
     *
     * @return 重试次数，0 表示无重试。
     */
    int getRetryCount();

    /**
     * 设置本次请求的重试次数。
     *
     * @param retryCount 重试次数。
     */
    void setRetryCount(int retryCount);

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
