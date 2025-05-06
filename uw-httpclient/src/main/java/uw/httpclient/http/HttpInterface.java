package uw.httpclient.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.httpclient.exception.HttpRequestException;
import uw.httpclient.json.JsonInterfaceHelper;
import uw.httpclient.util.MediaTypes;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Http请求方法抽象实现
 *
 * @since 2017/9/20
 */
public class HttpInterface {

    private static final OkHttpClient globalOkHttpClient = new OkHttpClient.Builder().retryOnConnectionFailure(false).build();
    private static final Logger log = LoggerFactory.getLogger(HttpInterface.class);
    /**
     * okHttpClient。
     */
    private final OkHttpClient okHttpClient;
    /**
     * http数据类型。
     */
    private final Class<? extends HttpData> httpDataCls;
    /**
     * http数据日志级别。
     */
    private final HttpDataLogLevel httpDataLogLevel;
    /**
     * HttpData数据处理器。
     */
    private final HttpDataProcessor httpDataProcessor;
    /**
     * 默认发送的mediaType。
     */
    private final MediaType mediaType;
    /**
     * 对象Mapper。
     */
    public DataObjectMapper objectMapper;


    public HttpInterface(HttpConfig httpConfig, Class<? extends HttpData> httpDataCls, HttpDataLogLevel httpDataLogLevel, HttpDataProcessor<? extends HttpData, ?> httpDataProcessor, DataObjectMapper objectMapper, MediaType mediaType) {
        if (httpConfig != null) {
            OkHttpClient.Builder okHttpClientBuilder = globalOkHttpClient.newBuilder().connectTimeout(httpConfig.getConnectTimeout(), TimeUnit.MILLISECONDS).readTimeout(httpConfig.getConnectTimeout(), TimeUnit.MILLISECONDS).writeTimeout(httpConfig.getWriteTimeout(), TimeUnit.MILLISECONDS);
            if (httpConfig.isRetryOnConnectionFailure())
                okHttpClientBuilder.retryOnConnectionFailure(httpConfig.isRetryOnConnectionFailure());
            if (httpConfig.getSslSocketFactory() != null || httpConfig.getTrustManager() != null)
                okHttpClientBuilder.sslSocketFactory(httpConfig.getSslSocketFactory(), httpConfig.getTrustManager());
            if (httpConfig.getHostnameVerifier() != null)
                okHttpClientBuilder.hostnameVerifier(httpConfig.getHostnameVerifier());
            if (httpConfig.getMaxIdleConnections() > 0 && httpConfig.getKeepAliveTimeout() > 0) {
                okHttpClientBuilder.connectionPool(new ConnectionPool(httpConfig.getMaxIdleConnections(), httpConfig.getKeepAliveTimeout(), TimeUnit.MILLISECONDS));
            }
            this.okHttpClient = okHttpClientBuilder.build();
            if (httpConfig.getMaxRequestsPerHost() > 0) {
                this.okHttpClient.dispatcher().setMaxRequestsPerHost(httpConfig.getMaxRequestsPerHost());
            }
            if (httpConfig.getMaxRequests() > 0) {
                this.okHttpClient.dispatcher().setMaxRequests(httpConfig.getMaxRequests());
            }
        } else {
            this.okHttpClient = globalOkHttpClient;
        }
        this.httpDataCls = httpDataCls;
        if (httpDataLogLevel == null) {
            this.httpDataLogLevel = HttpDataLogLevel.RECORD_RESPONSE;
        } else {
            this.httpDataLogLevel = httpDataLogLevel;
        }
        this.httpDataProcessor = httpDataProcessor;
        if (objectMapper == null) {
            this.objectMapper = JsonInterfaceHelper.JSON_CONVERTER;
        } else {
            this.objectMapper = objectMapper;
        }
        if (mediaType == null) {
            this.mediaType = MediaTypes.JSON_UTF8;
        } else {
            this.mediaType = mediaType;
        }
    }

    /**
     * 获取设置的ObjectMapper。
     *
     * @return
     */
    public DataObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * 获取原生的OkHttpClient。
     *
     * @return
     */
    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    /**
     * 获取HttpDataClass。
     *
     * @return
     */
    public Class<? extends HttpData> getHttpDataCls() {
        return httpDataCls;
    }

    /**
     * 获取HttpDataLogLevel。
     *
     * @return
     */
    public HttpDataLogLevel getHttpDataLogLevel() {
        return httpDataLogLevel;
    }

    /**
     * 获取HttpData过滤器。
     *
     * @return
     */
    public HttpDataProcessor getHttpDataProcessor() {
        return httpDataProcessor;
    }

    /**
     * 获取默认的MediaType。
     *
     * @return
     */
    public MediaType getMediaType() {
        return mediaType;
    }

    /**
     * 自定义Request请求，返回HttpData。
     *
     * @param request
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D requestForData(final Request request) {
        D httpData = requestData(request);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * 自定义Request请求，返回Entity。
     *
     * @param request
     * @param responseType
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> requestForEntity(final Request request, Class<T> responseType) {
        D httpData = requestData(request);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 自定义Request请求，返回Entity。
     *
     * @param request
     * @param typeRef
     * @param <T>
     * @return
     */
    public <D extends HttpData, T> HttpEntity<D, T> requestForEntity(final Request request, TypeReference<T> typeRef) {
        D httpData = requestData(request);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 自定义Request请求，返回Entity。
     *
     * @param request
     * @param javaType
     * @param <T>
     * @return
     */
    public <D extends HttpData, T> HttpEntity<D, T> requestForEntity(final Request request, JavaType javaType) {
        D httpData = requestData(request);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 使用Get方法获取HttpData。
     *
     * @param url
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D getForData(String url) {
        return getForData(url, null, null);
    }

    /**
     * 使用Get方法获取HttpData。
     *
     * @param url
     * @param queryParam
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D getForData(String url, Map<String, String> queryParam) {
        return getForData(url, null, queryParam);
    }

    /**
     * 使用Get方法获取HttpData。
     *
     * @param url
     * @param headers
     * @param queryParam
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D getForData(String url, Map<String, String> headers, Map<String, String> queryParam) {
        D httpData = getData(url, headers, queryParam);
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, Class<T> responseType) {
        return getForEntity(url, responseType, null, null);
    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, Class<T> responseType, Map<String, String> queryParam) {
        return getForEntity(url, responseType, null, queryParam);
    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param headers
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> queryParam) {
        D httpData = getData(url, headers, queryParam);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, TypeReference<T> typeRef) {
        return getForEntity(url, typeRef, null, null);

    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, TypeReference<T> typeRef, Map<String, String> queryParam) {
        return getForEntity(url, typeRef, null, queryParam);
    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param headers
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Map<String, String> queryParam) {
        D httpData = getData(url, headers, queryParam);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, JavaType javaType) {
        return getForEntity(url, javaType, null, null);
    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, JavaType javaType, Map<String, String> queryParam) {
        return getForEntity(url, javaType, null, queryParam);
    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param headers
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, JavaType javaType, Map<String, String> headers, Map<String, String> queryParam) {
        D httpData = getData(url, headers, queryParam);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * POST Form获取HttpData。
     *
     * @param url
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D postFormForData(String url, Map<String, String> formData) {
        return postFormForData(url, null, formData);
    }


    /**
     * POST Form获取HttpData。
     *
     * @param url
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D postFormFileForData(String url, Map<String, String> formData, Map<String, Object> fileData) {
        return postFormFileForData(url, null, formData, fileData);
    }

    /**
     * POST Form获取HttpData。
     *
     * @param url
     * @param headers
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D postFormForData(String url, Map<String, String> headers, Map<String, String> formData) {
        D httpData = postFormData(url, headers, formData);
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, null);
        }
        return httpData;
    }


    /**
     * POST Form获取HttpData。
     *
     * @param url
     * @param headers
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D postFormFileForData(String url, Map<String, String> headers, Map<String, String> formData, Map<String, Object> fileData) {
        D httpData = postFormFileData(url, headers, formData, fileData);
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, Class<T> responseType, Map<String, String> formData) {
        return postFormForEntity(url, responseType, null, formData);
    }

    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param headers
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> formData) {
        D httpData = postFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> formData) {
        return postFormForEntity(url, typeRef, null, formData);
    }

    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param headers
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Map<String, String> formData) {
        D httpData = postFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, JavaType javaType, Map<String, String> formData) {
        return postFormForEntity(url, javaType, null, formData);
    }

    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param headers
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, JavaType javaType, Map<String, String> headers, Map<String, String> formData) {
        D httpData = postFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }


    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormFileForEntity(String url, Class<T> responseType, Map<String, String> formData, Map<String, Object> fileData) {
        return postFormFileForEntity(url, responseType, null, formData, fileData);
    }

    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param headers
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormFileForEntity(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> formData, Map<String, Object> fileData) {
        D httpData = postFormFileData(url, headers, formData, fileData);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormFileForEntity(String url, TypeReference<T> typeRef, Map<String, String> formData, Map<String, Object> fileData) {
        return postFormFileForEntity(url, typeRef, null, formData, fileData);
    }

    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param headers
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormFileForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Map<String, String> formData, Map<String, Object> fileData) {
        D httpData = postFormFileData(url, headers, formData, fileData);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormFileForEntity(String url, JavaType javaType, Map<String, String> formData, Map<String, Object> fileData) {
        return postFormFileForEntity(url, javaType, null, formData, fileData);
    }

    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param headers
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormFileForEntity(String url, JavaType javaType, Map<String, String> headers, Map<String, String> formData, Map<String, Object> fileData) {
        D httpData = postFormFileData(url, headers, formData, fileData);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * POST RequestBody获取HttpData。
     *
     * @param url
     * @param requestData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D postBodyForData(String url, Object requestData) {
        return postBodyForData(url, null, requestData);
    }

    /**
     * POST RequestBody获取HttpData。
     *
     * @param url
     * @param headers
     * @param requestData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D postBodyForData(String url, Map<String, String> headers, Object requestData) {
        D httpData = postBodyData(url, headers, requestData);
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * POST RequestBody 获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, Class<T> responseType, Object requestData) {
        return postBodyForEntity(url, responseType, null, requestData);
    }

    /**
     * POST RequestBody 获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param headers
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, Class<T> responseType, Map<String, String> headers, Object requestData) {
        D httpData = postBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * POST RequestBody 获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, TypeReference<T> typeRef, Object requestData) {
        return postBodyForEntity(url, typeRef, null, requestData);

    }

    /**
     * POST RequestBody 获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param headers
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Object requestData) {
        D httpData = postBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * POST RequestBody 获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, JavaType javaType, Object requestData) {
        return postBodyForEntity(url, javaType, null, requestData);
    }

    /**
     * POST RequestBody 获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param headers
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, JavaType javaType, Map<String, String> headers, Object requestData) {
        D httpData = postBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PUT Form获取HttpData。
     *
     * @param url
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D putFormForData(String url, Map<String, String> formData) {
        return putFormForData(url, null, formData);
    }

    /**
     * PUT Form获取HttpData。
     *
     * @param url
     * @param headers
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D putFormForData(String url, Map<String, String> headers, Map<String, String> formData) {
        D httpData = putFormData(url, headers, formData);
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * PUT FormData 获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, Class<T> responseType, Map<String, String> formData) {
        return putFormForEntity(url, responseType, null, formData);
    }

    /**
     * PUT FormData 获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param headers
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> formData) {
        D httpData = putFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PUT FormData 获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> formData) {
        return putFormForEntity(url, typeRef, null, formData);
    }

    /**
     * PUT FormData 获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param headers
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Map<String, String> formData) {
        D httpData = putFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PUT FormData 获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, JavaType javaType, Map<String, String> formData) {
        return putFormForEntity(url, javaType, null, formData);
    }

    /**
     * PUT FormData 获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param headers
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, JavaType javaType, Map<String, String> headers, Map<String, String> formData) {
        D httpData = putFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PUT RequestBody获取HttpData。
     *
     * @param url
     * @param requestData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D putBodyForData(String url, Object requestData) {
        return putBodyForData(url, null, requestData);
    }

    /**
     * PUT RequestBody获取HttpData。
     *
     * @param url
     * @param headers
     * @param requestData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D putBodyForData(String url, Map<String, String> headers, Object requestData) {
        D httpData = putBodyData(url, headers, requestData);
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * PUT RequestBody 获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, Class<T> responseType, Object requestData) {
        return putBodyForEntity(url, responseType, null, requestData);
    }

    /**
     * PUT RequestBody 获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param headers
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, Class<T> responseType, Map<String, String> headers, Object requestData) {
        D httpData = putBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PUT RequestBody 获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, TypeReference<T> typeRef, Object requestData) {
        return putBodyForEntity(url, typeRef, null, requestData);

    }

    /**
     * PUT RequestBody 获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param headers
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Object requestData) {
        D httpData = putBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PUT RequestBody 获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, JavaType javaType, Object requestData) {
        return putBodyForEntity(url, javaType, null, requestData);
    }

    /**
     * PUT RequestBody 获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param headers
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, JavaType javaType, Map<String, String> headers, Object requestData) {
        D httpData = putBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PATCH Form获取HttpData。
     *
     * @param url
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D patchFormForData(String url, Map<String, String> formData) {
        return patchFormForData(url, null, formData);
    }

    /**
     * PATCH Form获取HttpData。
     *
     * @param url
     * @param headers
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D patchFormForData(String url, Map<String, String> headers, Map<String, String> formData) {
        D httpData = patchFormData(url, headers, formData);
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * PATCH FormData 获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, Class<T> responseType, Map<String, String> formData) {
        return patchFormForEntity(url, responseType, null, formData);
    }

    /**
     * PATCH FormData 获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param headers
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> formData) {
        D httpData = patchFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PATCH FormData 获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> formData) {
        return patchFormForEntity(url, typeRef, null, formData);
    }

    /**
     * PATCH FormData 获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param headers
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Map<String, String> formData) {
        D httpData = patchFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PATCH FormData 获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, JavaType javaType, Map<String, String> formData) {
        return patchFormForEntity(url, javaType, null, formData);
    }

    /**
     * PATCH FormData 获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param headers
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, JavaType javaType, Map<String, String> headers, Map<String, String> formData) {
        D httpData = patchFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PATCH RequestBody获取HttpData。
     *
     * @param url
     * @param requestData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D patchBodyForData(String url, Object requestData) {
        return patchBodyForData(url, null, requestData);
    }

    /**
     * PATCH RequestBody获取HttpData。
     *
     * @param url
     * @param headers
     * @param requestData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D patchBodyForData(String url, Map<String, String> headers, Object requestData) {
        D httpData = patchBodyData(url, headers, requestData);
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * PATCH RequestBody 获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, Class<T> responseType, Object requestData) {
        return patchBodyForEntity(url, responseType, null, requestData);
    }

    /**
     * PATCH RequestBody 获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param headers
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, Class<T> responseType, Map<String, String> headers, Object requestData) {
        D httpData = patchBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PATCH RequestBody 获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, TypeReference<T> typeRef, Object requestData) {
        return patchBodyForEntity(url, typeRef, null, requestData);

    }

    /**
     * PATCH RequestBody 获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param headers
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Object requestData) {
        D httpData = patchBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PATCH RequestBody 获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, JavaType javaType, Object requestData) {
        return patchBodyForEntity(url, javaType, null, requestData);
    }

    /**
     * PATCH RequestBody 获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param headers
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, JavaType javaType, Map<String, String> headers, Object requestData) {
        D httpData = patchBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 使用Delete方法获取HttpData。
     *
     * @param url
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D deleteForData(String url) {
        return deleteForData(url, null, null);
    }

    /**
     * 使用Delete方法获取HttpData。
     *
     * @param url
     * @param queryParam
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D deleteForData(String url, Map<String, String> queryParam) {
        return deleteForData(url, null, queryParam);
    }

    /**
     * 使用Delete方法获取HttpData。
     *
     * @param url
     * @param headers
     * @param queryParam
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D deleteForData(String url, Map<String, String> headers, Map<String, String> queryParam) {
        D httpData = deleteData(url, headers, queryParam);
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, Class<T> responseType) {
        return deleteForEntity(url, responseType, null, null);
    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, Class<T> responseType, Map<String, String> queryParam) {
        return deleteForEntity(url, responseType, null, queryParam);
    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url
     * @param responseType
     * @param headers
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> queryParam) {
        D httpData = deleteData(url, headers, queryParam);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, TypeReference<T> typeRef) {
        return deleteForEntity(url, typeRef, null, null);

    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, TypeReference<T> typeRef, Map<String, String> queryParam) {
        return deleteForEntity(url, typeRef, null, queryParam);
    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param headers
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Map<String, String> queryParam) {
        D httpData = deleteData(url, headers, queryParam);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, JavaType javaType) {
        return deleteForEntity(url, javaType, null, null);
    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, JavaType javaType, Map<String, String> queryParam) {
        return deleteForEntity(url, javaType, null, queryParam);
    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url
     * @param javaType
     * @param headers
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, JavaType javaType, Map<String, String> headers, Map<String, String> queryParam) {
        D httpData = deleteData(url, headers, queryParam);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 自定义Request请求，返回HttpData。
     *
     * @param request
     * @return
     * @throws Exception
     */
    private <D extends HttpData> D requestData(final Request request) {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess(null, null, null);
        }
        D httpData = initHttpData();
        httpData.setRequestDate(new Date());
        httpData.setRequestUrl(request.url().toString());
        httpData.setRequestMethod(request.method());
        httpData.setRequestHeader(request.headers().toString());
        if (HttpDataLogLevel.isRecordRequest(httpDataLogLevel)) {
            if (request.body() != null) {
                if (request.body() instanceof FormBody formBody) {
                    StringBuilder sb = new StringBuilder(256);
                    for (int i = 0; i < formBody.size(); i++) {
                        sb.append(formBody.name(i)).append("=").append(formBody.value(i)).append("\n");
                    }
                    httpData.setRequestData(sb.toString());
                } else {
                    httpData.setRequestData(request.body().toString());
                }
                if (httpData.getRequestData() != null) {
                    httpData.setRequestSize(httpData.getRequestData().length());
                }
            }
        }
        try (Response response = okHttpClient.newCall(request).execute()) {
            httpData.setResponseDate(new Date());
            httpData.setStatusCode(response.code());
            httpData.setResponseType(response.header("Content-Type"));
            httpData.setResponseBytes(response.body().bytes());
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize(httpData.getResponseBytes().length);
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess(httpData, response.headers());
            }

        } catch (IOException e) {
            throw new HttpRequestException(e.getMessage(), e);
        }

        return httpData;
    }

    /**
     * 使用Get方法获取HttpData。
     *
     * @param url
     * @param headers
     * @param queryParam
     * @param <D>
     * @return
     * @throws Exception
     */
    private <D extends HttpData> D getData(String url, Map<String, String> headers, Map<String, String> queryParam) {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess(null, queryParam, headers);
        }
        Request.Builder requestBuilder = new Request.Builder().url(buildUrl(url, queryParam));
        if (headers != null) {
            requestBuilder.headers(Headers.of(headers));
        }
        Request request = requestBuilder.get().build();
        D httpData = initHttpData();
        httpData.setRequestDate(new Date());
        httpData.setRequestUrl(request.url().toString());
        httpData.setRequestMethod(request.method());
        httpData.setRequestHeader(request.headers().toString());
        try (Response response = okHttpClient.newCall(request).execute()) {
            httpData.setResponseDate(new Date());
            httpData.setStatusCode(response.code());
            httpData.setResponseType(response.header("Content-Type"));
            httpData.setResponseBytes(response.body().bytes());
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize(httpData.getResponseBytes().length);
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess(httpData, response.headers());
            }
        } catch (IOException e) {
            throw new HttpRequestException(e.getMessage(), e);
        }

        return httpData;
    }

    /**
     * POST Form获取HttpData。
     *
     * @param url
     * @param headers
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    private <D extends HttpData> D postFormData(String url, Map<String, String> headers, Map<String, String> formData) {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess(null, formData, headers);
        }
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (headers != null) {
            requestBuilder.headers(Headers.of(headers));
        }
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        //表单数据。
        if (formData != null) {
            for (Map.Entry<String, String> param : formData.entrySet()) {
                formBodyBuilder.add(param.getKey(), param.getValue());
            }
        }
        Request request = requestBuilder.post(formBodyBuilder.build()).build();
        D httpData = initHttpData();
        httpData.setRequestDate(new Date());
        httpData.setRequestUrl(request.url().toString());
        httpData.setRequestMethod(request.method());
        httpData.setRequestHeader(request.headers().toString());
        if (HttpDataLogLevel.isRecordRequest(httpDataLogLevel) && formData != null && formData.size() > 0) {
            httpData.setRequestData(JsonInterfaceHelper.JSON_CONVERTER.toString(formData));
            if (httpData.getRequestData() != null) {
                httpData.setRequestSize(httpData.getRequestData().length());
            }
        }
        try (Response response = okHttpClient.newCall(request).execute()) {
            httpData.setResponseDate(new Date());
            httpData.setStatusCode(response.code());
            httpData.setResponseType(response.header("Content-Type"));
            httpData.setResponseBytes(response.body().bytes());
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize(httpData.getResponseBytes().length);
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess(httpData, response.headers());
            }
        } catch (IOException e) {
            throw new HttpRequestException(e.getMessage(), e);
        }
        return httpData;
    }


    /**
     * POST Form获取HttpData。
     *
     * @param url
     * @param headers
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    private <D extends HttpData> D postFormFileData(String url, Map<String, String> headers, Map<String, String> formData, Map<String, Object> fileData) {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess(null, formData, headers);
        }
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (headers != null) {
            requestBuilder.headers(Headers.of(headers));
        }
        MultipartBody.Builder formBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        // 添加表单数据
        if (formData != null) {
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                formBodyBuilder.addFormDataPart(entry.getKey(), entry.getValue());
            }
        }
        if (fileData != null) {
            //添加文件数据，同时支持file和bytep[
            for (Map.Entry<String, Object> entry : fileData.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof byte[]) {
                    RequestBody body = RequestBody.create((byte[]) value, MediaType.parse("application/octet-stream"));
                    formBodyBuilder.addFormDataPart(key, key, body);
                } else if (value instanceof File) {
                    RequestBody body = RequestBody.create((File) value, MediaType.parse("application/octet-stream"));
                    formBodyBuilder.addFormDataPart(key, ((File) value).getName(), body);
                } else {
                    formBodyBuilder.addFormDataPart(key, value.toString());
                }
            }
        }
        Request request = requestBuilder.post(formBodyBuilder.build()).build();
        D httpData = initHttpData();
        httpData.setRequestDate(new Date());
        httpData.setRequestUrl(request.url().toString());
        httpData.setRequestMethod(request.method());
        httpData.setRequestHeader(request.headers().toString());
        if (HttpDataLogLevel.isRecordRequest(httpDataLogLevel) && formData != null && formData.size() > 0) {
            httpData.setRequestData(JsonInterfaceHelper.JSON_CONVERTER.toString(formData));
            if (httpData.getRequestData() != null) {
                httpData.setRequestSize(httpData.getRequestData().length());
            }
        }
        try (Response response = okHttpClient.newCall(request).execute()) {
            httpData.setResponseDate(new Date());
            httpData.setStatusCode(response.code());
            httpData.setResponseType(response.header("Content-Type"));
            httpData.setResponseBytes(response.body().bytes());
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize(httpData.getResponseBytes().length);
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess(httpData, response.headers());
            }
        } catch (IOException e) {
            throw new HttpRequestException(e.getMessage(), e);
        }
        return httpData;
    }


    /**
     * POST RequestBody获取HttpData。
     *
     * @param url
     * @param headers
     * @param requestData
     * @param <D>
     * @return
     * @throws Exception
     */
    private <D extends HttpData> D postBodyData(String url, Map<String, String> headers, Object requestData) {
        //请求体。
        String requestBody = this.objectMapper.toString(requestData);
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess(requestBody, null, headers);
        }
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            builder.headers(Headers.of(headers));
        }
        Request requestBuilder = builder.post(RequestBody.create(requestBody, this.mediaType)).build();
        D httpData = initHttpData();
        httpData.setRequestDate(new Date());
        httpData.setRequestUrl(requestBuilder.url().toString());
        httpData.setRequestMethod(requestBuilder.method());
        httpData.setRequestHeader(requestBuilder.headers().toString());
        if (HttpDataLogLevel.isRecordRequest(httpDataLogLevel) && StringUtils.isNotBlank(requestBody)) {
            httpData.setRequestData(requestBody);
            if (httpData.getRequestData() != null) {
                httpData.setRequestSize(httpData.getRequestData().length());
            }
        }
        try (Response response = okHttpClient.newCall(requestBuilder).execute()) {
            httpData.setResponseDate(new Date());
            httpData.setStatusCode(response.code());
            httpData.setResponseType(response.header("Content-Type"));
            httpData.setResponseBytes(response.body().bytes());
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize(httpData.getResponseBytes().length);
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess(httpData, response.headers());
            }
        } catch (IOException e) {
            throw new HttpRequestException(e.getMessage(), e);
        }
        return httpData;
    }

    /**
     * PUT Form获取HttpData。
     *
     * @param url
     * @param headers
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    private <D extends HttpData> D putFormData(String url, Map<String, String> headers, Map<String, String> formData) {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess(null, formData, headers);
        }
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (headers != null) {
            requestBuilder.headers(Headers.of(headers));
        }
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        if (formData != null) {
            //表单数据。
            for (Map.Entry<String, String> param : formData.entrySet()) {
                formBodyBuilder.add(param.getKey(), param.getValue());
            }
        }
        Request request = requestBuilder.put(formBodyBuilder.build()).build();
        D httpData = initHttpData();
        httpData.setRequestDate(new Date());
        httpData.setRequestUrl(request.url().toString());
        httpData.setRequestMethod(request.method());
        httpData.setRequestHeader(request.headers().toString());
        if (HttpDataLogLevel.isRecordRequest(httpDataLogLevel) && formData != null && formData.size() > 0) {
            httpData.setRequestData(JsonInterfaceHelper.JSON_CONVERTER.toString(formData));
            if (httpData.getRequestData() != null) {
                httpData.setRequestSize(httpData.getRequestData().length());
            }
        }
        try (Response response = okHttpClient.newCall(request).execute()) {
            httpData.setResponseDate(new Date());
            httpData.setStatusCode(response.code());
            httpData.setResponseType(response.header("Content-Type"));
            httpData.setResponseBytes(response.body().bytes());
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize(httpData.getResponseBytes().length);
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess(httpData, response.headers());
            }
        } catch (IOException e) {
            throw new HttpRequestException(e.getMessage(), e);
        }
        return httpData;
    }

    /**
     * PUT RequestBody获取HttpData。
     *
     * @param url
     * @param headers
     * @param requestData
     * @param <D>
     * @return
     * @throws Exception
     */
    private <D extends HttpData> D putBodyData(String url, Map<String, String> headers, Object requestData) {
        //请求体。
        String requestBody = this.objectMapper.toString(requestData);
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess(requestBody, null, headers);
        }
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            builder.headers(Headers.of(headers));
        }
        Request requestBuilder = builder.put(RequestBody.create(requestBody, this.mediaType)).build();
        D httpData = initHttpData();
        httpData.setRequestDate(new Date());
        httpData.setRequestUrl(requestBuilder.url().toString());
        httpData.setRequestMethod(requestBuilder.method());
        httpData.setRequestHeader(requestBuilder.headers().toString());
        if (HttpDataLogLevel.isRecordRequest(httpDataLogLevel) && StringUtils.isNotBlank(requestBody)) {
            httpData.setRequestData(requestBody);
            if (httpData.getRequestData() != null) {
                httpData.setRequestSize(httpData.getRequestData().length());
            }
        }
        try (Response response = okHttpClient.newCall(requestBuilder).execute()) {
            httpData.setResponseDate(new Date());
            httpData.setStatusCode(response.code());
            httpData.setResponseType(response.header("Content-Type"));
            httpData.setResponseBytes(response.body().bytes());
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize(httpData.getResponseBytes().length);
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess(httpData, response.headers());
            }
        } catch (IOException e) {

            throw new HttpRequestException(e.getMessage(), e);
        }
        return httpData;
    }

    /**
     * PATCH Form获取HttpData。
     *
     * @param url
     * @param headers
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    private <D extends HttpData> D patchFormData(String url, Map<String, String> headers, Map<String, String> formData) {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess(null, formData, headers);
        }
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (headers != null) {
            requestBuilder.headers(Headers.of(headers));
        }
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        if (formData != null) {
            //表单数据。
            for (Map.Entry<String, String> param : formData.entrySet()) {
                formBodyBuilder.add(param.getKey(), param.getValue());
            }
        }
        Request request = requestBuilder.patch(formBodyBuilder.build()).build();
        D httpData = initHttpData();
        httpData.setRequestDate(new Date());
        httpData.setRequestUrl(request.url().toString());
        httpData.setRequestMethod(request.method());
        httpData.setRequestHeader(request.headers().toString());
        if (HttpDataLogLevel.isRecordRequest(httpDataLogLevel) && formData != null && formData.size() > 0) {
            httpData.setRequestData(JsonInterfaceHelper.JSON_CONVERTER.toString(formData));
            if (httpData.getRequestData() != null) {
                httpData.setRequestSize(httpData.getRequestData().length());
            }
        }
        try (Response response = okHttpClient.newCall(request).execute()) {
            httpData.setResponseDate(new Date());
            httpData.setStatusCode(response.code());
            httpData.setResponseType(response.header("Content-Type"));
            httpData.setResponseBytes(response.body().bytes());
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize(httpData.getResponseBytes().length);
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess(httpData, response.headers());
            }
        } catch (IOException e) {
            throw new HttpRequestException(e.getMessage(), e);
        }
        return httpData;
    }

    /**
     * PATCH RequestBody获取HttpData。
     *
     * @param url
     * @param headers
     * @param requestData
     * @param <D>
     * @return
     * @throws Exception
     */
    private <D extends HttpData> D patchBodyData(String url, Map<String, String> headers, Object requestData) {
        //请求体。
        String requestBody = this.objectMapper.toString(requestData);
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess(requestBody, null, headers);
        }
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            builder.headers(Headers.of(headers));
        }
        Request requestBuilder = builder.patch(RequestBody.create(requestBody, this.mediaType)).build();
        D httpData = initHttpData();
        httpData.setRequestDate(new Date());
        httpData.setRequestUrl(requestBuilder.url().toString());
        httpData.setRequestMethod(requestBuilder.method());
        httpData.setRequestHeader(requestBuilder.headers().toString());
        if (HttpDataLogLevel.isRecordRequest(httpDataLogLevel) && StringUtils.isNotBlank(requestBody)) {
            httpData.setRequestData(requestBody);
            if (httpData.getRequestData() != null) {
                httpData.setRequestSize(httpData.getRequestData().length());
            }
        }
        try (Response response = okHttpClient.newCall(requestBuilder).execute()) {
            httpData.setResponseDate(new Date());
            httpData.setStatusCode(response.code());
            httpData.setResponseType(response.header("Content-Type"));
            httpData.setResponseBytes(response.body().bytes());
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize(httpData.getResponseBytes().length);
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess(httpData, response.headers());
            }
        } catch (IOException e) {

            throw new HttpRequestException(e.getMessage(), e);
        }
        return httpData;
    }

    /**
     * 使用Delete方法获取HttpData。
     *
     * @param url
     * @param headers
     * @param queryParam
     * @param <D>
     * @return
     * @throws Exception
     */
    private <D extends HttpData> D deleteData(String url, Map<String, String> headers, Map<String, String> queryParam) {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess(null, queryParam, headers);
        }
        Request.Builder requestBuilder = new Request.Builder().url(buildUrl(url, queryParam));
        if (headers != null) {
            requestBuilder.headers(Headers.of(headers));
        }
        Request request = requestBuilder.delete().build();
        D httpData = initHttpData();
        httpData.setRequestDate(new Date());
        httpData.setRequestUrl(request.url().toString());
        httpData.setRequestMethod(request.method());
        httpData.setRequestHeader(request.headers().toString());
        try (Response response = okHttpClient.newCall(request).execute()) {
            httpData.setResponseDate(new Date());
            httpData.setStatusCode(response.code());
            httpData.setResponseType(response.header("Content-Type"));
            httpData.setResponseBytes(response.body().bytes());
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize(httpData.getResponseBytes().length);
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess(httpData, response.headers());
            }
        } catch (IOException e) {
            throw new HttpRequestException(e.getMessage(), e);
        }

        return httpData;
    }

    /**
     * 构造请求链接。
     *
     * @param url        url路径
     * @param queryParam 参数
     * @return HttpUrl url
     */
    private HttpUrl buildUrl(String url, Map<String, String> queryParam) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            throw new RuntimeException("url:[" + url + "] is invalid!");
        }
        if (queryParam != null) {
            HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
            queryParam.forEach((key, value) -> {
                if (StringUtils.isNotBlank(value)) {
                    urlBuilder.addQueryParameter(key, value);
                }
            });
            httpUrl = urlBuilder.build();
        }
        return httpUrl;
    }

    /**
     * 构造httpData。
     *
     * @return
     */
    private <D extends HttpData> D initHttpData() {
        HttpData httpLog = null;
        if (this.httpDataCls != null) {
            try {
                httpLog = this.httpDataCls.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (httpLog == null) {
            httpLog = new HttpDefaultData();
        }
        return (D) httpLog;
    }
}
