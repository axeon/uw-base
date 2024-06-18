package uw.httpclient.http;

import okhttp3.Headers;
import uw.httpclient.exception.DataMapperException;

import java.util.Map;

/**
 * Http数据处理器。
 * 主要用于处理消息加密和签名验证情况。
 */
public interface HttpDataProcessor<D extends HttpData, T> {

    /**
     * 请求过滤器。
     * 在实际发送之间，对数据进行处理。
     *
     * @param requestBody
     * @return
     * @throws DataMapperException
     */
    void requestProcess(String requestBody, Map<String, String> formData, Map<String, String> headers) throws DataMapperException;

    /**
     * 输出过滤器。
     * 在接受到数据之后，对数据进行处理。
     *
     * @param httpData
     * @return
     */
    void responseProcess(D httpData, Headers headers) throws DataMapperException;

    /**
     * 在请求完成后，对数据进行处理。
     * 一般用于处理日志数据，发送到远端。
     *
     * @param httpData
     */
    void postProcess(D httpData,T t);

}
