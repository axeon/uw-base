package uw.httpclient.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import uw.common.util.SystemClock;
import uw.httpclient.exception.DataMapperException;
import uw.httpclient.exception.HttpRequestException;
import uw.httpclient.json.JsonInterfaceHelper;
import uw.httpclient.util.MediaTypes;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * HTTP 请求方法的抽象实现。
 * <p>
 * 基于 OkHttp 封装，统一处理请求构建、响应填充、日志记录与数据处理器回调，
 * 并通过 {@link DataObjectMapper} 完成请求/响应的序列化与反序列化。
 * {@link uw.httpclient.json.JsonInterfaceHelper} 与 {@link uw.httpclient.xml.XmlInterfaceHelper}
 * 为其针对 JSON / XML 的具体子类。
 * <p>
 * <b>方法命名规律</b>：{@code {httpMethod}{请求体形式}{返回形式}}
 * <ul>
 *   <li>请求体形式：{@code Form}（表单）、{@code Body}（请求体）、{@code FormFile}（含文件上传）、GET/DELETE 无；</li>
 *   <li>返回形式：{@code ForData}（返回 {@link HttpData}，二进制友好）、{@code ForEntity}（返回 {@link HttpEntity}，含反序列化对象）。</li>
 * </ul>
 * 每个 {@code ForEntity} 方法均提供 {@code Class}/{@code TypeReference}/{@code JavaType} 三套响应类型重载，
 * 及对应的带 {@code headers} 重载。
 * <p>
 * <b>线程模型</b>：每个实例持有独立的 OkHttpClient 与 Dispatcher，配置互不影响；
 * 静态 {@link #globalOkHttpClient} 作为无 HttpConfig 时的兜底共享实例。
 *
 * @since 2017/9/20
 */
public class HttpInterface {

    /**
     * 全局的 OkHttpClient，无 HttpConfig 时作为兜底共享实例，默认不开启连接失败重试。
     */
    private static final OkHttpClient globalOkHttpClient = new OkHttpClient.Builder().retryOnConnectionFailure(false).build();
    /**
     * 当前实例使用的 OkHttpClient（由 globalOkHttpClient 派生并应用 HttpConfig）。
     */
    private final OkHttpClient okHttpClient;
    /**
     * 自定义 HttpData 实现类（日志载体），为 null 时使用 {@link HttpDefaultData}。
     */
    private final Class<? extends HttpData> httpDataCls;
    /**
     * HttpData 日志级别，控制是否额外记录请求体。
     */
    private final HttpDataLogLevel httpDataLogLevel;
    /**
     * HttpData 数据处理器（加解密 / 日志上报等），可为 null。
     */
    private final HttpDataProcessor<? extends HttpData, ?> httpDataProcessor;
    /**
     * 默认发送的 MediaType（请求体序列化类型）。
     */
    private final MediaType mediaType;
    /**
     * 对象 Mapper，用于请求体序列化与响应体反序列化。
     */
    private final DataObjectMapper objectMapper;
    /**
     * 默认请求头（来自 HttpConfig，可能为 null），所有请求自动追加，业务传入同名头覆盖默认值。
     */
    private final java.util.Map<String, String> defaultHeaders;
    /**
     * 重试/重定向计数器（内部网络拦截器），统计每个 Call 的物理网络请求次数。
     */
    private final RetryCounter retryCounter;


    /**
     * 完整构造器。
     *
     * @param httpConfig        HttpConfig 配置参数，为 null 时使用 {@link #globalOkHttpClient}。
     * @param httpDataCls       自定义 HttpData 实现类，为 null 时使用 {@link HttpDefaultData}。
     * @param httpDataLogLevel  HttpData 日志级别，为 null 时默认 {@link HttpDataLogLevel#RECORD_RESPONSE}。
     * @param httpDataProcessor HttpData 数据处理器，可为 null。
     * @param objectMapper      对象 Mapper，为 null 时默认 JSON 转换器。
     * @param mediaType         默认请求体 MediaType，为 null 时默认 {@link uw.httpclient.util.MediaTypes#JSON_UTF8}。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public HttpInterface(HttpConfig httpConfig, Class<? extends HttpData> httpDataCls, HttpDataLogLevel httpDataLogLevel, HttpDataProcessor<? extends HttpData, ?> httpDataProcessor, DataObjectMapper objectMapper, MediaType mediaType) {
        if (httpConfig != null) {
            OkHttpClient.Builder okHttpClientBuilder = globalOkHttpClient.newBuilder()
                    .connectTimeout(httpConfig.getConnectTimeout(), TimeUnit.MILLISECONDS)
                    .readTimeout(httpConfig.getReadTimeout(), TimeUnit.MILLISECONDS)
                    .writeTimeout(httpConfig.getWriteTimeout(), TimeUnit.MILLISECONDS);
            if (httpConfig.isRetryOnConnectionFailure())
                okHttpClientBuilder.retryOnConnectionFailure(httpConfig.isRetryOnConnectionFailure());
            if (httpConfig.getSslSocketFactory() != null || httpConfig.getTrustManager() != null)
                okHttpClientBuilder.sslSocketFactory(httpConfig.getSslSocketFactory(), httpConfig.getTrustManager());
            if (httpConfig.getHostnameVerifier() != null)
                okHttpClientBuilder.hostnameVerifier(httpConfig.getHostnameVerifier());
            if (httpConfig.getMaxIdleConnections() > 0 && httpConfig.getKeepAliveTimeout() > 0) {
                okHttpClientBuilder.connectionPool(new ConnectionPool(httpConfig.getMaxIdleConnections(), httpConfig.getKeepAliveTimeout(), TimeUnit.MILLISECONDS));
            }
            // CookieJar：为 null 时 OkHttp 默认使用不持久化的 CookieJar.NO_COOKIES。
            if (httpConfig.getCookieJar() != null) {
                okHttpClientBuilder.cookieJar(httpConfig.getCookieJar());
            }
            // 拦截器：仅作用于本实例派生出的 client（实例间不共享 client）。
            for (Interceptor interceptor : httpConfig.getInterceptors()) {
                okHttpClientBuilder.addInterceptor(interceptor);
            }
            for (Interceptor interceptor : httpConfig.getNetworkInterceptors()) {
                okHttpClientBuilder.addNetworkInterceptor(interceptor);
            }
            // 重试/重定向观测：无条件注入内部网络拦截器，按 Call 统计每次物理网络请求次数。
            // retryCount = proceed 次数 - 1，含连接失败重试（retryOnConnectionFailure=true 时）与
            // follow-up（重定向/认证挑战，由 OkHttp 内部 RetryAndFollowUpInterceptor 处理，
            // 与 retryOnConnectionFailure 无关）。开销可忽略（一次 ConcurrentHashMap 原子操作）。
            this.retryCounter = new RetryCounter();
            okHttpClientBuilder.addNetworkInterceptor(this.retryCounter);
            // 使用独立的dispatcher，避免newBuilder()共享全局dispatcher导致并发放大配置互相串扰。
            Dispatcher dispatcher = new Dispatcher();
            if (httpConfig.getMaxRequestsPerHost() > 0) {
                dispatcher.setMaxRequestsPerHost(httpConfig.getMaxRequestsPerHost());
            }
            if (httpConfig.getMaxRequests() > 0) {
                dispatcher.setMaxRequests(httpConfig.getMaxRequests());
            }
            okHttpClientBuilder.dispatcher(dispatcher);
            this.okHttpClient = okHttpClientBuilder.build();
        } else {
            this.okHttpClient = globalOkHttpClient;
            // 未配置 HttpConfig 时走全局共享 client，无法为其注入实例级拦截器，retryCount 恒为 0。
            // （globalOkHttpClient 本身 retryOnConnectionFailure=false，但走全局兜底的场景一般也不需要诊断）
            this.retryCounter = null;
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
        // 默认请求头：HttpConfig 为 null 时无默认头。
        this.defaultHeaders = (httpConfig == null) ? null : httpConfig.getDefaultHeaders();
    }

    /**
     * 获取设置的 ObjectMapper。
     *
     * @return 对象 Mapper。
     */
    public DataObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * 获取原生的 OkHttpClient，可用于更底层的自定义操作（兜底方案）。
     *
     * @return OkHttpClient。
     */
    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    /**
     * 获取 HttpData 实现类。
     *
     * @return HttpData 实现类，可能为 null（表示使用默认）。
     */
    public Class<? extends HttpData> getHttpDataCls() {
        return httpDataCls;
    }

    /**
     * 获取 HttpData 日志级别。
     *
     * @return HttpData 日志级别。
     */
    public HttpDataLogLevel getHttpDataLogLevel() {
        return httpDataLogLevel;
    }

    /**
     * 获取 HttpData 数据处理器。
     *
     * @return 数据处理器，可能为 null。
     */
    public HttpDataProcessor<? extends HttpData, ?> getHttpDataProcessor() {
        return httpDataProcessor;
    }

    /**
     * 获取默认的 MediaType（请求体序列化类型）。
     *
     * @return MediaType。
     */
    public MediaType getMediaType() {
        return mediaType;
    }

    /**
     * 自定义 OkHttp Request 发起请求，返回 HttpData（不做响应反序列化）。
     * 适用于需要完全自定义请求构造、或上传数据的场景。
     *
     * @param request 自定义 OkHttp Request。
     * @param <D>     HttpData 实现类型。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时抛出。
     */
    public <D extends HttpData> D requestForData(final Request request) {
        D httpData = requestData(request);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * 自定义 OkHttp Request 发起请求，返回 HttpEntity（含按 {@code Class} 反序列化的响应对象）。
     *
     * @param request OkHttp Request。
     * @param responseType 响应目标类型。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> requestForEntity(final Request request, Class<T> responseType) {
        D httpData = requestData(request);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 自定义 OkHttp Request 发起请求，返回 HttpEntity（含按 {@code TypeReference} 反序列化的响应对象）。
     *
     * @param request OkHttp Request。
     * @param typeRef 响应泛型类型引用。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> requestForEntity(final Request request, TypeReference<T> typeRef) {
        D httpData = requestData(request);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 自定义 OkHttp Request 发起请求，返回 HttpEntity（含按 {@code JavaType} 反序列化的响应对象）。
     *
     * @param request OkHttp Request。
     * @param javaType 响应 JavaType。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> requestForEntity(final Request request, JavaType javaType) {
        D httpData = requestData(request);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 使用Get方法获取HttpData。
     *
     * @param url 请求地址。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D getForData(String url) {
        return getForData(url, null, null);
    }

    /**
     * 使用Get方法获取HttpData。
     *
     * @param url 请求地址。
     * @param queryParam 查询参数，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D getForData(String url, Map<String, String> queryParam) {
        return getForData(url, null, queryParam);
    }

    /**
     * 使用Get方法获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param queryParam 查询参数，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D getForData(String url, Map<String, String> headers, Map<String, String> queryParam) {
        D httpData = getData(url, headers, queryParam);
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, Class<T> responseType) {
        return getForEntity(url, responseType, null, null);
    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param queryParam 查询参数，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, Class<T> responseType, Map<String, String> queryParam) {
        return getForEntity(url, responseType, null, queryParam);
    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param headers 请求头，可为 null。
     * @param queryParam 查询参数，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> queryParam) {
        D httpData = getData(url, headers, queryParam);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, TypeReference<T> typeRef) {
        return getForEntity(url, typeRef, null, null);

    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param queryParam 查询参数，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, TypeReference<T> typeRef, Map<String, String> queryParam) {
        return getForEntity(url, typeRef, null, queryParam);
    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param headers 请求头，可为 null。
     * @param queryParam 查询参数，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Map<String, String> queryParam) {
        D httpData = getData(url, headers, queryParam);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, JavaType javaType) {
        return getForEntity(url, javaType, null, null);
    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param queryParam 查询参数，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, JavaType javaType, Map<String, String> queryParam) {
        return getForEntity(url, javaType, null, queryParam);
    }

    /**
     * 使用Get方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param headers 请求头，可为 null。
     * @param queryParam 查询参数，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, JavaType javaType, Map<String, String> headers, Map<String, String> queryParam) {
        D httpData = getData(url, headers, queryParam);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * POST Form获取HttpData。
     *
     * @param url 请求地址。
     * @param formData 表单数据，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D postFormForData(String url, Map<String, String> formData) {
        return postFormForData(url, null, formData);
    }


    /**
     * POST Form(含文件上传)获取HttpData。
     *
     * @param url 请求地址。
     * @param formData 表单数据，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D postFormFileForData(String url, Map<String, String> formData, Map<String, Object> fileData) {
        return postFormFileForData(url, null, formData, fileData);
    }

    /**
     * POST Form获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D postFormForData(String url, Map<String, String> headers, Map<String, String> formData) {
        D httpData = postFormData(url, headers, formData);
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, null);
        }
        return httpData;
    }


    /**
     * POST Form(含文件上传)获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D postFormFileForData(String url, Map<String, String> headers, Map<String, String> formData, Map<String, Object> fileData) {
        D httpData = postFormFileData(url, headers, formData, fileData);
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, Class<T> responseType, Map<String, String> formData) {
        return postFormForEntity(url, responseType, null, formData);
    }

    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> formData) {
        D httpData = postFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> formData) {
        return postFormForEntity(url, typeRef, null, formData);
    }

    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Map<String, String> formData) {
        D httpData = postFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, JavaType javaType, Map<String, String> formData) {
        return postFormForEntity(url, javaType, null, formData);
    }

    /**
     * POST FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, JavaType javaType, Map<String, String> headers, Map<String, String> formData) {
        D httpData = postFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }


    /**
     * POST FormData(含文件上传)获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormFileForEntity(String url, Class<T> responseType, Map<String, String> formData, Map<String, Object> fileData) {
        return postFormFileForEntity(url, responseType, null, formData, fileData);
    }

    /**
     * POST FormData(含文件上传)获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormFileForEntity(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> formData, Map<String, Object> fileData) {
        D httpData = postFormFileData(url, headers, formData, fileData);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * POST FormData(含文件上传)获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormFileForEntity(String url, TypeReference<T> typeRef, Map<String, String> formData, Map<String, Object> fileData) {
        return postFormFileForEntity(url, typeRef, null, formData, fileData);
    }

    /**
     * POST FormData(含文件上传)获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormFileForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Map<String, String> formData, Map<String, Object> fileData) {
        D httpData = postFormFileData(url, headers, formData, fileData);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * POST FormData(含文件上传)获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormFileForEntity(String url, JavaType javaType, Map<String, String> formData, Map<String, Object> fileData) {
        return postFormFileForEntity(url, javaType, null, formData, fileData);
    }

    /**
     * POST FormData(含文件上传)获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormFileForEntity(String url, JavaType javaType, Map<String, String> headers, Map<String, String> formData, Map<String, Object> fileData) {
        D httpData = postFormFileData(url, headers, formData, fileData);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * POST RequestBody获取HttpData。
     *
     * @param url 请求地址。
     * @param requestData 请求体对象，会被序列化。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D postBodyForData(String url, Object requestData) {
        return postBodyForData(url, null, requestData);
    }

    /**
     * POST RequestBody获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param requestData 请求体对象，会被序列化。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D postBodyForData(String url, Map<String, String> headers, Object requestData) {
        D httpData = postBodyData(url, headers, requestData);
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * POST RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, Class<T> responseType, Object requestData) {
        return postBodyForEntity(url, responseType, null, requestData);
    }

    /**
     * POST RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param headers 请求头，可为 null。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, Class<T> responseType, Map<String, String> headers, Object requestData) {
        D httpData = postBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * POST RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, TypeReference<T> typeRef, Object requestData) {
        return postBodyForEntity(url, typeRef, null, requestData);

    }

    /**
     * POST RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param headers 请求头，可为 null。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Object requestData) {
        D httpData = postBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * POST RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, JavaType javaType, Object requestData) {
        return postBodyForEntity(url, javaType, null, requestData);
    }

    /**
     * POST RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param headers 请求头，可为 null。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, JavaType javaType, Map<String, String> headers, Object requestData) {
        D httpData = postBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PUT Form获取HttpData。
     *
     * @param url 请求地址。
     * @param formData 表单数据，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D putFormForData(String url, Map<String, String> formData) {
        return putFormForData(url, null, formData);
    }

    /**
     * PUT Form获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D putFormForData(String url, Map<String, String> headers, Map<String, String> formData) {
        D httpData = putFormData(url, headers, formData);
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * PUT FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, Class<T> responseType, Map<String, String> formData) {
        return putFormForEntity(url, responseType, null, formData);
    }

    /**
     * PUT FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> formData) {
        D httpData = putFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PUT FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> formData) {
        return putFormForEntity(url, typeRef, null, formData);
    }

    /**
     * PUT FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Map<String, String> formData) {
        D httpData = putFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PUT FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, JavaType javaType, Map<String, String> formData) {
        return putFormForEntity(url, javaType, null, formData);
    }

    /**
     * PUT FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, JavaType javaType, Map<String, String> headers, Map<String, String> formData) {
        D httpData = putFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PUT RequestBody获取HttpData。
     *
     * @param url 请求地址。
     * @param requestData 请求体对象，会被序列化。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D putBodyForData(String url, Object requestData) {
        return putBodyForData(url, null, requestData);
    }

    /**
     * PUT RequestBody获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param requestData 请求体对象，会被序列化。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D putBodyForData(String url, Map<String, String> headers, Object requestData) {
        D httpData = putBodyData(url, headers, requestData);
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * PUT RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, Class<T> responseType, Object requestData) {
        return putBodyForEntity(url, responseType, null, requestData);
    }

    /**
     * PUT RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param headers 请求头，可为 null。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, Class<T> responseType, Map<String, String> headers, Object requestData) {
        D httpData = putBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PUT RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, TypeReference<T> typeRef, Object requestData) {
        return putBodyForEntity(url, typeRef, null, requestData);

    }

    /**
     * PUT RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param headers 请求头，可为 null。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Object requestData) {
        D httpData = putBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PUT RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, JavaType javaType, Object requestData) {
        return putBodyForEntity(url, javaType, null, requestData);
    }

    /**
     * PUT RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param headers 请求头，可为 null。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, JavaType javaType, Map<String, String> headers, Object requestData) {
        D httpData = putBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PATCH Form获取HttpData。
     *
     * @param url 请求地址。
     * @param formData 表单数据，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D patchFormForData(String url, Map<String, String> formData) {
        return patchFormForData(url, null, formData);
    }

    /**
     * PATCH Form获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D patchFormForData(String url, Map<String, String> headers, Map<String, String> formData) {
        D httpData = patchFormData(url, headers, formData);
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * PATCH FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, Class<T> responseType, Map<String, String> formData) {
        return patchFormForEntity(url, responseType, null, formData);
    }

    /**
     * PATCH FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> formData) {
        D httpData = patchFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PATCH FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> formData) {
        return patchFormForEntity(url, typeRef, null, formData);
    }

    /**
     * PATCH FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Map<String, String> formData) {
        D httpData = patchFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PATCH FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, JavaType javaType, Map<String, String> formData) {
        return patchFormForEntity(url, javaType, null, formData);
    }

    /**
     * PATCH FormData 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, JavaType javaType, Map<String, String> headers, Map<String, String> formData) {
        D httpData = patchFormData(url, headers, formData);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PATCH RequestBody获取HttpData。
     *
     * @param url 请求地址。
     * @param requestData 请求体对象，会被序列化。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D patchBodyForData(String url, Object requestData) {
        return patchBodyForData(url, null, requestData);
    }

    /**
     * PATCH RequestBody获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param requestData 请求体对象，会被序列化。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D patchBodyForData(String url, Map<String, String> headers, Object requestData) {
        D httpData = patchBodyData(url, headers, requestData);
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * PATCH RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, Class<T> responseType, Object requestData) {
        return patchBodyForEntity(url, responseType, null, requestData);
    }

    /**
     * PATCH RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param headers 请求头，可为 null。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, Class<T> responseType, Map<String, String> headers, Object requestData) {
        D httpData = patchBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PATCH RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, TypeReference<T> typeRef, Object requestData) {
        return patchBodyForEntity(url, typeRef, null, requestData);

    }

    /**
     * PATCH RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param headers 请求头，可为 null。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Object requestData) {
        D httpData = patchBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * PATCH RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, JavaType javaType, Object requestData) {
        return patchBodyForEntity(url, javaType, null, requestData);
    }

    /**
     * PATCH RequestBody 获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param headers 请求头，可为 null。
     * @param requestData 请求体对象，会被序列化。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, JavaType javaType, Map<String, String> headers, Object requestData) {
        D httpData = patchBodyData(url, headers, requestData);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 使用Delete方法获取HttpData。
     *
     * @param url 请求地址。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D deleteForData(String url) {
        return deleteForData(url, null, null);
    }

    /**
     * 使用Delete方法获取HttpData。
     *
     * @param url 请求地址。
     * @param queryParam 查询参数，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D deleteForData(String url, Map<String, String> queryParam) {
        return deleteForData(url, null, queryParam);
    }

    /**
     * 使用Delete方法获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param queryParam 查询参数，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    public <D extends HttpData> D deleteForData(String url, Map<String, String> headers, Map<String, String> queryParam) {
        D httpData = deleteData(url, headers, queryParam);
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, null);
        }
        return httpData;
    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, Class<T> responseType) {
        return deleteForEntity(url, responseType, null, null);
    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param queryParam 查询参数，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, Class<T> responseType, Map<String, String> queryParam) {
        return deleteForEntity(url, responseType, null, queryParam);
    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param responseType 响应目标类型。
     * @param headers 请求头，可为 null。
     * @param queryParam 查询参数，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> queryParam) {
        D httpData = deleteData(url, headers, queryParam);
        T t = this.objectMapper.parse(httpData.getResponseData(), responseType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, TypeReference<T> typeRef) {
        return deleteForEntity(url, typeRef, null, null);

    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param queryParam 查询参数，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, TypeReference<T> typeRef, Map<String, String> queryParam) {
        return deleteForEntity(url, typeRef, null, queryParam);
    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param typeRef 响应泛型类型引用。
     * @param headers 请求头，可为 null。
     * @param queryParam 查询参数，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Map<String, String> queryParam) {
        D httpData = deleteData(url, headers, queryParam);
        T t = this.objectMapper.parse(httpData.getResponseData(), typeRef);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, JavaType javaType) {
        return deleteForEntity(url, javaType, null, null);
    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param queryParam 查询参数，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, JavaType javaType, Map<String, String> queryParam) {
        return deleteForEntity(url, javaType, null, queryParam);
    }

    /**
     * 使用Delete方法获取HttpEntity。
     *
     * @param url 请求地址。
     * @param javaType 响应 JavaType。
     * @param headers 请求头，可为 null。
     * @param queryParam 查询参数，可为 null。
     * @return HttpEntity，含 HttpData 与反序列化后的响应对象。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     * @throws DataMapperException 当响应反序列化失败时。
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, JavaType javaType, Map<String, String> headers, Map<String, String> queryParam) {
        D httpData = deleteData(url, headers, queryParam);
        T t = this.objectMapper.parse(httpData.getResponseData(), javaType);
        //处理日志。
        if (this.httpDataProcessor != null) {
            invokePostProcess(httpData, t);
        }
        return new HttpEntity<>(httpData, t);
    }

    /**
     * 自定义Request请求，返回HttpData。
     *
     * @param request OkHttp Request。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    private <D extends HttpData> D requestData(final Request request) {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess(requestBodyToString(request.body()), null, null);
        }
        D httpData = initHttpData();
        fillRequestMeta(httpData, request);
        if (HttpDataLogLevel.isRecordRequest(httpDataLogLevel) && request.body() != null) {
            String requestData = requestBodyToString(request.body());
            if (requestData != null) {
                httpData.setRequestData(requestData);
                httpData.setRequestSize(requestData.length());
            }
        }
        fillResponse(httpData, request);
        return httpData;
    }

    /**
     * 使用Get方法获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param queryParam 查询参数，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    private <D extends HttpData> D getData(String url, Map<String, String> headers, Map<String, String> queryParam) {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess(null, queryParam, headers);
        }
        Request.Builder requestBuilder = new Request.Builder().url(buildUrl(url, queryParam));
        applyHeaders(requestBuilder, headers);
        Request request = requestBuilder.get().build();
        D httpData = initHttpData();
        fillRequestMeta(httpData, request);
        fillResponse(httpData, request);
        return httpData;
    }

    /**
     * POST Form获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    private <D extends HttpData> D postFormData(String url, Map<String, String> headers, Map<String, String> formData) {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess(null, formData, headers);
        }
        Request.Builder requestBuilder = new Request.Builder().url(url);
        applyHeaders(requestBuilder, headers);
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        //表单数据。
        if (formData != null) {
            for (Map.Entry<String, String> param : formData.entrySet()) {
                formBodyBuilder.add(param.getKey(), param.getValue());
            }
        }
        Request request = requestBuilder.post(formBodyBuilder.build()).build();
        D httpData = initHttpData();
        fillRequestMeta(httpData, request);
        if (HttpDataLogLevel.isRecordRequest(httpDataLogLevel) && formData != null && !formData.isEmpty()) {
            String requestData = JsonInterfaceHelper.JSON_CONVERTER.toString(formData);
            httpData.setRequestData(requestData);
            httpData.setRequestSize(requestData.length());
        }
        fillResponse(httpData, request);
        return httpData;
    }


    /**
     * POST Form获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    private <D extends HttpData> D postFormFileData(String url, Map<String, String> headers, Map<String, String> formData, Map<String, Object> fileData) {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess(null, formData, headers);
        }
        Request.Builder requestBuilder = new Request.Builder().url(url);
        applyHeaders(requestBuilder, headers);
        MultipartBody.Builder formBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        // 添加表单数据
        if (formData != null) {
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                formBodyBuilder.addFormDataPart(entry.getKey(), entry.getValue());
            }
        }
        if (fileData != null) {
            //添加文件数据，同时支持file和byte[]
            for (Map.Entry<String, Object> entry : fileData.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof byte[]) {
                    RequestBody body = RequestBody.create((byte[]) value, MediaTypes.OCTET_STREAM);
                    formBodyBuilder.addFormDataPart(key, key, body);
                } else if (value instanceof File) {
                    RequestBody body = RequestBody.create((File) value, MediaTypes.OCTET_STREAM);
                    formBodyBuilder.addFormDataPart(key, ((File) value).getName(), body);
                } else {
                    formBodyBuilder.addFormDataPart(key, value.toString());
                }
            }
        }
        Request request = requestBuilder.post(formBodyBuilder.build()).build();
        D httpData = initHttpData();
        fillRequestMeta(httpData, request);
        if (HttpDataLogLevel.isRecordRequest(httpDataLogLevel) && formData != null && !formData.isEmpty()) {
            String requestData = JsonInterfaceHelper.JSON_CONVERTER.toString(formData);
            httpData.setRequestData(requestData);
            httpData.setRequestSize(requestData.length());
        }
        fillResponse(httpData, request);
        return httpData;
    }


    /**
     * POST RequestBody获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param requestData 请求体对象，会被序列化。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
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
        Request request = builder.post(RequestBody.create(requestBody, this.mediaType)).build();
        D httpData = initHttpData();
        fillRequestMeta(httpData, request);
        fillRequestBody(httpData, requestBody);
        fillResponse(httpData, request);
        return httpData;
    }

    /**
     * PUT Form获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    private <D extends HttpData> D putFormData(String url, Map<String, String> headers, Map<String, String> formData) {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess(null, formData, headers);
        }
        Request.Builder requestBuilder = new Request.Builder().url(url);
        applyHeaders(requestBuilder, headers);
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        if (formData != null) {
            //表单数据。
            for (Map.Entry<String, String> param : formData.entrySet()) {
                formBodyBuilder.add(param.getKey(), param.getValue());
            }
        }
        Request request = requestBuilder.put(formBodyBuilder.build()).build();
        D httpData = initHttpData();
        fillRequestMeta(httpData, request);
        if (HttpDataLogLevel.isRecordRequest(httpDataLogLevel) && formData != null && !formData.isEmpty()) {
            String requestData = JsonInterfaceHelper.JSON_CONVERTER.toString(formData);
            httpData.setRequestData(requestData);
            httpData.setRequestSize(requestData.length());
        }
        fillResponse(httpData, request);
        return httpData;
    }

    /**
     * PUT RequestBody获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param requestData 请求体对象，会被序列化。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
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
        Request request = builder.put(RequestBody.create(requestBody, this.mediaType)).build();
        D httpData = initHttpData();
        fillRequestMeta(httpData, request);
        fillRequestBody(httpData, requestBody);
        fillResponse(httpData, request);
        return httpData;
    }

    /**
     * PATCH Form获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param formData 表单数据，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    private <D extends HttpData> D patchFormData(String url, Map<String, String> headers, Map<String, String> formData) {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess(null, formData, headers);
        }
        Request.Builder requestBuilder = new Request.Builder().url(url);
        applyHeaders(requestBuilder, headers);
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        if (formData != null) {
            //表单数据。
            for (Map.Entry<String, String> param : formData.entrySet()) {
                formBodyBuilder.add(param.getKey(), param.getValue());
            }
        }
        Request request = requestBuilder.patch(formBodyBuilder.build()).build();
        D httpData = initHttpData();
        fillRequestMeta(httpData, request);
        if (HttpDataLogLevel.isRecordRequest(httpDataLogLevel) && formData != null && !formData.isEmpty()) {
            String requestData = JsonInterfaceHelper.JSON_CONVERTER.toString(formData);
            httpData.setRequestData(requestData);
            httpData.setRequestSize(requestData.length());
        }
        fillResponse(httpData, request);
        return httpData;
    }

    /**
     * PATCH RequestBody获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param requestData 请求体对象，会被序列化。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
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
        Request request = builder.patch(RequestBody.create(requestBody, this.mediaType)).build();
        D httpData = initHttpData();
        fillRequestMeta(httpData, request);
        fillRequestBody(httpData, requestBody);
        fillResponse(httpData, request);
        return httpData;
    }

    /**
     * 使用Delete方法获取HttpData。
     *
     * @param url 请求地址。
     * @param headers 请求头，可为 null。
     * @param queryParam 查询参数，可为 null。
     * @return 承载请求/响应日志的 HttpData。
     * @throws HttpRequestException 当请求发生网络或 IO 错误时。
     */
    private <D extends HttpData> D deleteData(String url, Map<String, String> headers, Map<String, String> queryParam) {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess(null, queryParam, headers);
        }
        Request.Builder requestBuilder = new Request.Builder().url(buildUrl(url, queryParam));
        applyHeaders(requestBuilder, headers);
        Request request = requestBuilder.delete().build();
        D httpData = initHttpData();
        fillRequestMeta(httpData, request);
        fillResponse(httpData, request);
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
            throw new HttpRequestException("url:[" + url + "] is invalid!");
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
     * 向 Request.Builder 应用请求头：先追加默认头，再用业务传入的头覆盖同名项。
     * <p>
     * OkHttp 的 {@code headers(Headers)} 会整体替换，故采用逐个 {@code header(name, value)} 叠加，
     * 同名头以业务传入值为准（后 set 覆盖先 add）。null 头均跳过。
     *
     * @param requestBuilder OkHttp Request.Builder。
     * @param headers        业务传入的请求头，可为 null。
     */
    private void applyHeaders(Request.Builder requestBuilder, Map<String, String> headers) {
        if (defaultHeaders != null) {
            for (Map.Entry<String, String> e : defaultHeaders.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    requestBuilder.header(e.getKey(), e.getValue());
                }
            }
        }
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    requestBuilder.header(e.getKey(), e.getValue());
                }
            }
        }
    }

    /**
     * 填充请求元信息（method/url/header/请求时间）。
     *
     * @param httpData 请求/响应日志数据。
     * @param request OkHttp Request。
     */
    private void fillRequestMeta(HttpData httpData, Request request) {
        httpData.setRequestDate(SystemClock.nowDate());
        httpData.setRequestUrl(request.url().toString());
        httpData.setRequestMethod(request.method());
        httpData.setRequestHeader(request.headers().toString());
    }

    /**
     * 记录请求体数据与大小（受日志级别控制）。
     *
     * @param httpData 请求/响应日志数据。
     * @param requestBody 请求体字符串。
     */
    private void fillRequestBody(HttpData httpData, String requestBody) {
        if (HttpDataLogLevel.isRecordRequest(httpDataLogLevel) && StringUtils.isNotBlank(requestBody)) {
            httpData.setRequestData(requestBody);
            httpData.setRequestSize(requestBody.length());
        }
    }

    /**
     * 执行请求并填充响应数据。
     *
     * @param httpData 请求/响应日志数据。
     * @param request OkHttp Request。
     */
    private void fillResponse(HttpData httpData, Request request) {
        // 在真正发请求前，将构建完成的 Request（含已合并的业务头与 defaultHeaders、最终 URL，
        // 但不含 OkHttp 网络层注入的 Host/Content-Length/Cookie 等）交给 Processor，
        // 用于签名/验签/链路追踪等。默认实现为空，不影响现有行为。
        // Processor 抛出的 DataMapperException（RuntimeException，TaskDataException 体系）直接冒泡，
        // 交由 uw-task 框架按其异常分类策略处理（重试/告警），不在此吞掉或改写异常类型。
        if (this.httpDataProcessor != null) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            HttpDataProcessor processor = (HttpDataProcessor) this.httpDataProcessor;
            processor.requestProcess(request);
        }
        okhttp3.Call call = okHttpClient.newCall(request);
        try {
            try (Response response = call.execute()) {
                httpData.setResponseDate(SystemClock.nowDate());
                httpData.setStatusCode(response.code());
                httpData.setResponseType(response.header("Content-Type"));
                httpData.setResponseMessage(response.message());
                httpData.setElapsedMillis(response.receivedResponseAtMillis() - response.sentRequestAtMillis());
                fillResponseHeaders(httpData, response.headers());
                byte[] bytes = readBytes(response);
                httpData.setResponseBytes(bytes);
                if (bytes != null) {
                    httpData.setResponseSize(bytes.length);
                }
                if (this.httpDataProcessor != null) {
                    invokeResponseProcess(httpData, response.headers());
                }
            }
        } catch (IOException e) {
            throw new HttpRequestException(e.getMessage(), e);
        } finally {
            // 记录本次 Call 的重试/重定向次数（网络拦截器在每次 proceed 时递增），事后清理。
            httpData.setRetryCount(retryCountOf(call));
        }
    }

    /**
     * 取一个 Call 的重试/重定向次数（proceed 次数 - 1），并清理计数器条目。
     * <p>
     * retryCounter 为 null（走全局 client 的兜底场景）时返回 0。
     * 请求抛 IOException 时网络拦截器仍可能已记录部分 proceed，finally 中也会清理，避免泄漏。
     *
     * @param call OkHttp Call。
     * @return 重试次数，0 表示无重试。
     */
    private int retryCountOf(okhttp3.Call call) {
        if (this.retryCounter == null) {
            return 0;
        }
        return this.retryCounter.consumeRetryCount(call);
    }

    /**
     * 将 OkHttp 响应头转为大小写不敏感的多值 Map 快照并写入 HttpData。
     * <p>
     * 对所有 {@link HttpData} 实现（默认或自定义）走同一条转换路径，无特判：
     * {@code okhttp3.Headers} → 不可变 {@code Map<String, List<String>>}（TreeMap 大小写不敏感）。
     * OkHttp 类型知识收敛在此方法内部，不泄漏到 {@link HttpData} / {@link HttpDefaultData}。
     *
     * @param httpData HttpData。
     * @param headers  OkHttp 响应头。
     */
    private void fillResponseHeaders(HttpData httpData, okhttp3.Headers headers) {
        httpData.setResponseHeaders(toHeaderMultimap(headers));
    }

    /**
     * 将 OkHttp {@code Headers} 转为大小写不敏感、不可变的多值 Map 快照。
     *
     * @param headers OkHttp 响应头，为 null 或空时返回空 Map。
     * @return 多值 Map，键大小写不敏感。
     */
    private static Map<String, List<String>> toHeaderMultimap(okhttp3.Headers headers) {
        if (headers == null || headers.size() == 0) {
            return Collections.emptyMap();
        }
        // TreeMap + CASE_INSENSITIVE_ORDER 保证 get(name) 大小写不敏感。
        Map<String, List<String>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String name : headers.names()) {
            map.put(name, headers.values(name));
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * 安全读取响应体字节，response.body()为null时返回null。
     *
     * @param response OkHttp Response。
     * @return 响应体字节数组，body 为 null 时返回 null。
     * @throws IOException 读取响应体发生 IO 错误时。
     */
    private byte[] readBytes(Response response) throws IOException {
        ResponseBody body = response.body();
        return body == null ? null : body.bytes();
    }

    /**
     * 以 raw 视图调用 HttpDataProcessor 的 postProcess。
     * <p>
     * 字段声明为 {@code HttpDataProcessor<? extends HttpData, ?>}，无法直接接收具体类型 D，
     * 故通过 raw 转发。HttpDataProcessor 本就是多态处理器，此处 raw 调用在语义上是安全的。
     *
     * @param httpData HttpData。
     * @param value    反序列化对象，可为 null。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void invokePostProcess(HttpData httpData, Object value) {
        if (this.httpDataProcessor != null) {
            ((HttpDataProcessor) this.httpDataProcessor).postProcess(httpData, value);
        }
    }

    /**
     * 以 raw 视图调用 HttpDataProcessor 的 responseProcess。
     *
     * @param httpData HttpData。
     * @param headers  响应头。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void invokeResponseProcess(HttpData httpData, okhttp3.Headers headers) {
        if (this.httpDataProcessor != null) {
            ((HttpDataProcessor) this.httpDataProcessor).responseProcess(httpData, headers);
        }
    }

    /**
     * 将Request body转换为可记录的字符串。
     * 仅对FormBody做扁平化处理，其它类型（如MultipartBody/流式body）不做读取以避免破坏body，返回null。
     *
     * @param body RequestBody，可为 null。
     * @return 可记录的请求体字符串，无法安全读取时返回 null。
     */
    private String requestBodyToString(RequestBody body) {
        if (body == null) {
            return null;
        }
        if (body instanceof FormBody formBody) {
            StringBuilder sb = new StringBuilder(256);
            for (int i = 0; i < formBody.size(); i++) {
                sb.append(formBody.name(i)).append("=").append(formBody.value(i)).append("\n");
            }
            return sb.toString();
        }
        // 非FormBody（如MultipartBody/流式body）不读取，避免破坏请求体或输出无意义引用。
        return null;
    }

    /**
     * 构造httpData。
     *
     * @return HttpData 实例。
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

    /**
     * 重试/重定向计数器：作为内部网络拦截器注入 OkHttpClient，
     * 按每个 {@link okhttp3.Call} 统计物理网络请求次数（含连接失败重试与 follow-up）。
     * <p>
     * <b>计数语义</b>：网络拦截器在每次真实网络请求（含重试与重定向后的 follow-up）时进入一次，
     * 故对一次 Call 的 {@code proceed} 次数即物理网络请求次数，重试/重定向次数 = proceed 次数 - 1。
     * <p>
     * <b>线程模型</b>：以 {@code Call} 实例为 key（OkHttp 单次调用全程复用同一 Call 实例），
     * 用 {@link ConcurrentHashMap} + {@link AtomicInteger} 保证并发安全；
     * {@link #consumeRetryCount(okhttp3.Call)} 在请求结束后取出次数并删除条目，避免泄漏。
     * <p>
     * 包级可见以便单元测试（模拟多次 proceed 验证计数与清理）。
     */
    static final class RetryCounter implements Interceptor {

        private final ConcurrentHashMap<okhttp3.Call, AtomicInteger> counts =
                new ConcurrentHashMap<>();

        @Override
        public Response intercept(Chain chain) throws IOException {
            okhttp3.Call call = chain.call();
            // proceed 前先递增：本次 proceed 计为该 Call 的第 N 次物理网络请求。
            counts.computeIfAbsent(call, k -> new AtomicInteger()).incrementAndGet();
            return chain.proceed(chain.request());
        }

        /**
         * 取出指定 Call 的重试/重定向次数（proceed 次数 - 1），并删除计数条目。
         * <p>
         * min(0) 防御异常情况下计数缺失（如拦截器未触发即返回）时出现负数。
         *
         * @param call OkHttp Call。
         * @return 重试/重定向次数，0 表示无重试。
         */
        int consumeRetryCount(okhttp3.Call call) {
            AtomicInteger n = counts.remove(call);
            if (n == null) {
                return 0;
            }
            return Math.max(0, n.get() - 1);
        }
    }
}
