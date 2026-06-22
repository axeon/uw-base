package uw.httpclient.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uw.common.util.JsonUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * {@link HttpData} 的默认实现。
 * <p>
 * 以普通 JavaBean 形式承载全部请求/响应字段，并支持：
 * <ul>
 *   <li>{@link #getResponseData()} 从 {@link #responseBytes} 按 UTF-8 懒转换；</li>
 *   <li>{@link #toString()} 经 Jackson 序列化为 JSON，便于日志输出。</li>
 * </ul>
 * 可直接作为业务接口日志实体继承复用，避免 DTO 间拷贝。
 */
public class HttpDefaultData implements HttpData {

    /**
     * HTTP 请求 URL。
     */
    private String requestUrl;

    /**
     * HTTP 方法。
     */
    private String requestMethod;

    /**
     * HTTP 请求头。
     */
    private String requestHeader;

    /**
     * HTTP 响应状态码。
     */
    private int statusCode;

    /**
     * 请求数据大小（字符长度）。
     */
    private long requestSize;

    /**
     * 返回数据大小（字节数）。
     */
    private long responseSize;

    /**
     * 响应的 Content-Type。
     */
    private String responseType;

    /**
     * 请求数据（受日志级别控制）。
     */
    private String requestData;

    /**
     * 响应数据字符串（懒转换缓存）。
     */
    private String responseData;

    /**
     * 响应原始字节数组（二进制优先，不参与 JSON 序列化）。
     */
    @JsonIgnore
    private byte[] responseBytes;

    /**
     * 请求发起时间。
     */
    private Date requestDate;

    /**
     * 响应接收时间。
     */
    private Date responseDate;

    /**
     * 错误信息。
     */
    private String errorInfo;

    /**
     * 响应头多值视图（大小写不敏感）。
     */
    private Map<String, List<String>> responseHeaders;

    /**
     * HTTP 响应状态消息（reason phrase）。
     */
    private String responseMessage;

    /**
     * 本次请求整体耗时（毫秒），-1 表示未设置。
     */
    private long elapsedMillis = -1L;

    /**
     * 重试/重定向次数，0 表示无重试。
     */
    private int retryCount = 0;

    /**
     * 将本对象序列化为 JSON 字符串，便于日志输出。
     *
     * @return JSON 字符串。
     */
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

    /**
     * {@inheritDoc}
     * <p>
     * 优先返回已显式设置的 responseData；否则从 {@link #responseBytes} 按 UTF-8 懒转换并缓存。
     */
    @Override
    public String getResponseData() {
        if (responseData != null) {
            return responseData;
        } else {
            if (responseBytes != null) {
                responseData = new String(this.responseBytes, StandardCharsets.UTF_8);
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

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    @Override
    public void setResponseHeaders(Map<String, List<String>> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    /**
     * 按名称便捷取单个响应头值（首个），大小写不敏感。
     * <p>
     * 用于业务侧取 {@code X-Trace-Id}、{@code Location} 等单值头；
     * 取同名多值头（如 {@code Set-Cookie}）请直接用 {@link #getResponseHeaders()}。
     *
     * @param name 响应头名称，大小写不敏感。
     * @return 首个值，不存在或无响应头时返回 null。
     */
    @JsonIgnore
    public String getResponseHeader(String name) {
        if (responseHeaders == null || name == null) {
            return null;
        }
        List<String> values = responseHeaders.get(name); // Map 为大小写不敏感视图
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    @Override
    public String getResponseMessage() {
        return responseMessage;
    }

    @Override
    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    @Override
    public long getElapsedMillis() {
        return elapsedMillis;
    }

    @Override
    public void setElapsedMillis(long elapsedMillis) {
        this.elapsedMillis = elapsedMillis;
    }

    @Override
    public int getRetryCount() {
        return retryCount;
    }

    @Override
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
