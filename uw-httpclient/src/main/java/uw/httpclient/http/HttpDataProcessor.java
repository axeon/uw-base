package uw.httpclient.http;

import okhttp3.Headers;
import uw.httpclient.exception.DataMapperException;

import java.util.Map;

/**
 * HTTP 数据处理器。
 * <p>
 * 在请求生命周期各阶段介入，典型用于请求/响应的加解密、签名验证、日志上报等，
 * 让业务代码保持整洁。三个回调分别对应：发送前、收到响应后、请求完成后。
 *
 * @param <D> HttpData 实现类型，承载请求/响应日志。
 * @param <T> 响应体反序列化后的值类型。
 */
public interface HttpDataProcessor<D extends HttpData, T> {

    /**
     * 请求前置处理器。
     * <p>
     * 在实际发送请求之前调用，可对请求数据做加解密、签名等处理。
     *
     * @param requestBody 请求体字符串（Form/GET/DELETE 场景可能为 null）。
     * @param formData    表单数据，可为 null。
     * @param headers     请求头，可为 null。
     * @throws DataMapperException 处理过程中发生数据映射错误时抛出。
     */
    void requestProcess(String requestBody, Map<String, String> formData, Map<String, String> headers) throws DataMapperException;

    /**
     * 响应处理器。
     * <p>
     * 在接收到响应数据之后调用，可对响应数据做解密、验签等处理。
     *
     * @param httpData 承载响应的 HttpData。
     * @param headers  响应头。
     * @throws DataMapperException 处理过程中发生数据映射错误时抛出。
     */
    void responseProcess(D httpData, Headers headers) throws DataMapperException;

    /**
     * 请求完成处理器。
     * <p>
     * 在请求与（可能的）反序列化都完成后调用，一般用于将日志数据发送到远端。
     *
     * @param httpData HttpData。
     * @param t        反序列化后的响应对象，ForData 调用时为 null。
     */
    void postProcess(D httpData, T t);

}
