package uw.httpclient.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import uw.httpclient.json.JsonInterfaceHelper;
import uw.httpclient.util.MediaTypes;
import uw.task.exception.TaskPartnerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Http请求方法抽象实现
 *
 * @since 2017/9/20
 */
public class HttpInterface {

    private static final OkHttpClient globalOkHttpClient = new OkHttpClient.Builder().retryOnConnectionFailure( false ).build();
    /**
     * 对象Mapper。
     */
    public ObjectMapper objectMapper;

    /**
     * okHttpClient。
     */
    private OkHttpClient okHttpClient;

    /**
     * http数据类型。
     */
    private Class<? extends HttpData> httpDataCls;

    /**
     * http数据日志级别。
     */
    private HttpDataLogLevel httpDataLogLevel;

    /**
     * HttpData数据处理器。
     */
    private HttpDataProcessor httpDataProcessor;

    /**
     * 默认发送的mediaType。
     */
    private MediaType mediaType;


    public HttpInterface(HttpConfig httpConfig, Class<? extends HttpData> httpDataCls, HttpDataLogLevel httpDataLogLevel, HttpDataProcessor httpDataProcessor,
                         ObjectMapper objectMapper, MediaType mediaType) {
        if (httpConfig != null) {
            OkHttpClient.Builder okHttpClientBuilder =
                    globalOkHttpClient.newBuilder().connectTimeout( httpConfig.getConnectTimeout(), TimeUnit.MILLISECONDS ).readTimeout( httpConfig.getConnectTimeout(),
                            TimeUnit.MILLISECONDS ).writeTimeout( httpConfig.getWriteTimeout(), TimeUnit.MILLISECONDS );
            if (httpConfig.isRetryOnConnectionFailure()) okHttpClientBuilder.retryOnConnectionFailure( httpConfig.isRetryOnConnectionFailure() );
            if (httpConfig.getSslSocketFactory() != null || httpConfig.getTrustManager() != null)
                okHttpClientBuilder.sslSocketFactory( httpConfig.getSslSocketFactory(), httpConfig.getTrustManager() );
            if (httpConfig.getHostnameVerifier() != null) okHttpClientBuilder.hostnameVerifier( httpConfig.getHostnameVerifier() );
            if (httpConfig.getMaxIdleConnections() > 0 && httpConfig.getKeepAliveTimeout() > 0) {
                okHttpClientBuilder.connectionPool( new ConnectionPool( httpConfig.getMaxIdleConnections(), httpConfig.getKeepAliveTimeout(), TimeUnit.MILLISECONDS ) );
            }
            this.okHttpClient = okHttpClientBuilder.build();
            if (httpConfig.getMaxRequestsPerHost() > 0) {
                this.okHttpClient.dispatcher().setMaxRequestsPerHost( httpConfig.getMaxRequestsPerHost() );
            }
            if (httpConfig.getMaxRequests() > 0) {
                this.okHttpClient.dispatcher().setMaxRequests( httpConfig.getMaxRequests() );
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
     * 获得设置的ObjectMapper。
     *
     * @return
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * 获得原生的OkHttpClient。
     *
     * @return
     */
    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    /**
     * 获得HttpDataClass。
     *
     * @return
     */
    public Class<? extends HttpData> getHttpDataCls() {
        return httpDataCls;
    }

    /**
     * 获得HttpDataLogLevel。
     *
     * @return
     */
    public HttpDataLogLevel getHttpDataLogLevel() {
        return httpDataLogLevel;
    }

    /**
     * 获得HttpData过滤器。
     *
     * @return
     */
    public HttpDataProcessor getHttpDataProcessor() {
        return httpDataProcessor;
    }

    /**
     * 获得默认的MediaType。
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
    public <D extends HttpData> D requestForData(final Request request) throws Exception {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess( null, null, null );
        }
        D httpData = initHttpData();
        httpData.setRequestDate( new Date() );
        httpData.setRequestUrl( request.url().toString() );
        httpData.setRequestMethod( request.method() );
        httpData.setRequestHeader( request.headers().toString() );
        if (HttpDataLogLevel.isRecordRequest( httpDataLogLevel )) {
            if (request.body() != null) {
                if (request.body() instanceof FormBody formBody) {
                    StringBuilder sb = new StringBuilder( 256 );
                    for (int i = 0; i < formBody.size(); i++) {
                        sb.append( formBody.name( i ) ).append( "=" ).append( formBody.value( i ) ).append( "\n" );
                    }
                    httpData.setRequestData( sb.toString() );
                } else {
                    httpData.setRequestData( request.body().toString() );
                }
                if (httpData.getRequestData() != null) {
                    httpData.setRequestSize( httpData.getRequestData().length() );
                }
            }
        }
        try (Response response = okHttpClient.newCall( request ).execute()) {
            httpData.setResponseDate( new Date() );
            httpData.setStatusCode( response.code() );
            httpData.setResponseType( response.header( "Content-Type" ) );
            httpData.setResponseBytes( response.body().bytes() );
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize( httpData.getResponseBytes().length );
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess( httpData, response.headers() );
            }
        } catch (IOException e) {
            throw new TaskPartnerException( e.getMessage(), e );
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
    public <D extends HttpData, T> HttpEntity<D, T> requestForEntity(final Request request, Class<T> responseType) throws Exception {
        D httpData = requestForData( request );
        T t = this.objectMapper.parse( httpData.getResponseData(), responseType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }

    /**
     * 自定义Request请求，返回Entity。
     *
     * @param request
     * @param typeRef
     * @param <T>
     * @return
     */
    public <D extends HttpData, T> HttpEntity<D, T> requestForEntity(final Request request, TypeReference<T> typeRef) throws Exception {
        D httpData = requestForData( request );
        T t = this.objectMapper.parse( httpData.getResponseData(), typeRef );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }

    /**
     * 自定义Request请求，返回Entity。
     *
     * @param request
     * @param javaType
     * @param <T>
     * @return
     */
    public <D extends HttpData, T> HttpEntity<D, T> requestForEntity(final Request request, JavaType javaType) throws Exception {
        D httpData = requestForData( request );
        T t = this.objectMapper.parse( httpData.getResponseData(), javaType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }

    /**
     * 使用Get方法获得HttpData。
     *
     * @param url
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D getForData(String url) throws Exception {
        return getForData( url, null, null );
    }

    /**
     * 使用Get方法获得HttpData。
     *
     * @param url
     * @param queryParam
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D getForData(String url, Map<String, String> queryParam) throws Exception {
        return getForData( url, null, queryParam );
    }

    /**
     * 使用Get方法获得HttpData。
     *
     * @param url
     * @param headers
     * @param queryParam
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D getForData(String url, Map<String, String> headers, Map<String, String> queryParam) throws Exception {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess( null, queryParam, headers );
        }
        Request.Builder requestBuilder = new Request.Builder().url( buildUrl( url, queryParam ) );
        if (headers != null) {
            requestBuilder.headers( Headers.of( headers ) );
        }
        Request request = requestBuilder.get().build();
        D httpData = initHttpData();
        httpData.setRequestDate( new Date() );
        httpData.setRequestUrl( request.url().toString() );
        httpData.setRequestMethod( request.method() );
        httpData.setRequestHeader( request.headers().toString() );
        try (Response response = okHttpClient.newCall( request ).execute()) {
            httpData.setResponseDate( new Date() );
            httpData.setStatusCode( response.code() );
            httpData.setResponseType( response.header( "Content-Type" ) );
            httpData.setResponseBytes( response.body().bytes() );
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize( httpData.getResponseBytes().length );
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess( httpData, response.headers() );
            }
        } catch (IOException e) {
            throw new TaskPartnerException( e.getMessage(), e );
        }

        return httpData;
    }

    /**
     * 使用Get方法获得HttpEntity。
     *
     * @param url
     * @param responseType
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, Class<T> responseType) throws Exception {
        return getForEntity( url, responseType, null, null );
    }

    /**
     * 使用Get方法获得HttpEntity。
     *
     * @param url
     * @param responseType
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, Class<T> responseType, Map<String, String> queryParam) throws Exception {
        return getForEntity( url, responseType, null, queryParam );
    }

    /**
     * 使用Get方法获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> queryParam) throws Exception {
        D httpData = getForData( url, headers, queryParam );
        T t = this.objectMapper.parse( httpData.getResponseData(), responseType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }


    /**
     * 使用Get方法获得HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, TypeReference<T> typeRef) throws Exception {
        return getForEntity( url, typeRef, null, null );

    }

    /**
     * 使用Get方法获得HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, TypeReference<T> typeRef, Map<String, String> queryParam) throws Exception {
        return getForEntity( url, typeRef, null, queryParam );
    }

    /**
     * 使用Get方法获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Map<String, String> queryParam) throws Exception {
        D httpData = getForData( url, headers, queryParam );
        T t = this.objectMapper.parse( httpData.getResponseData(), typeRef );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }


    /**
     * 使用Get方法获得HttpEntity。
     *
     * @param url
     * @param javaType
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, JavaType javaType) throws Exception {
        return getForEntity( url, javaType, null, null );
    }

    /**
     * 使用Get方法获得HttpEntity。
     *
     * @param url
     * @param javaType
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, JavaType javaType, Map<String, String> queryParam) throws Exception {
        return getForEntity( url, javaType, null, queryParam );
    }

    /**
     * 使用Get方法获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> getForEntity(String url, JavaType javaType, Map<String, String> headers, Map<String, String> queryParam) throws Exception {
        D httpData = getForData( url, headers, queryParam );
        T t = this.objectMapper.parse( httpData.getResponseData(), javaType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }


    /**
     * POST Form获得HttpData。
     *
     * @param url
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D postFormForData(String url, Map<String, String> formData) throws Exception {
        return postFormForData( url, null, formData );
    }

    /**
     * POST Form获得HttpData。
     *
     * @param url
     * @param headers
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D postFormForData(String url, Map<String, String> headers, Map<String, String> formData) throws Exception {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess( null, formData, headers );
        }
        Request.Builder requestBuilder = new Request.Builder().url( url );
        if (headers != null) {
            requestBuilder.headers( Headers.of( headers ) );
        }
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        if (formData != null) {
            //表单数据。
            for (Map.Entry<String, String> param : formData.entrySet()) {
                formBodyBuilder.add( param.getKey(), param.getValue() );
            }
        }
        Request request = requestBuilder.post( formBodyBuilder.build() ).build();
        D httpData = initHttpData();
        httpData.setRequestDate( new Date() );
        httpData.setRequestUrl( request.url().toString() );
        httpData.setRequestMethod( request.method() );
        httpData.setRequestHeader( request.headers().toString() );
        if (HttpDataLogLevel.isRecordRequest( httpDataLogLevel ) && formData != null && formData.size() > 0) {
            httpData.setRequestData( JsonInterfaceHelper.JSON_CONVERTER.toString( formData ) );
            if (httpData.getRequestData() != null) {
                httpData.setRequestSize( httpData.getRequestData().length() );
            }
        }
        try (Response response = okHttpClient.newCall( request ).execute()) {
            httpData.setResponseDate( new Date() );
            httpData.setStatusCode( response.code() );
            httpData.setResponseType( response.header( "Content-Type" ) );
            httpData.setResponseBytes( response.body().bytes() );
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize( httpData.getResponseBytes().length );
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess( httpData, response.headers() );
            }
        } catch (IOException e) {
            throw new TaskPartnerException( e.getMessage(), e );
        }
        return httpData;
    }

    /**
     * POST FormData 获得HttpEntity。
     *
     * @param url
     * @param responseType
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, Class<T> responseType, Map<String, String> formData) throws Exception {
        return postFormForEntity( url, responseType, null, formData );
    }

    /**
     * POST FormData 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> formData) throws Exception {
        D httpData = postFormForData( url, headers, formData );
        T t = this.objectMapper.parse( httpData.getResponseData(), responseType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }


    /**
     * POST FormData 获得HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> formData) throws Exception {
        return postFormForEntity( url, typeRef, null, formData );
    }

    /**
     * POST FormData 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Map<String, String> formData) throws Exception {
        D httpData = postFormForData( url, headers, formData );
        T t = this.objectMapper.parse( httpData.getResponseData(), typeRef );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }


    /**
     * POST FormData 获得HttpEntity。
     *
     * @param url
     * @param javaType
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, JavaType javaType, Map<String, String> formData) throws Exception {
        return postFormForEntity( url, javaType, null, formData );
    }

    /**
     * POST FormData 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> postFormForEntity(String url, JavaType javaType, Map<String, String> headers, Map<String, String> formData) throws Exception {
        D httpData = postFormForData( url, headers, formData );
        T t = this.objectMapper.parse( httpData.getResponseData(), javaType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }


    /**
     * POST RequestBody获得HttpData。
     *
     * @param url
     * @param requestData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D postBodyForData(String url, Object requestData) throws Exception {
        Request request = new Request.Builder().url( url ).post( RequestBody.create( this.objectMapper.toString( requestData ), this.mediaType ) ).build();
        return requestForData( request );
    }

    /**
     * POST RequestBody获得HttpData。
     *
     * @param url
     * @param headers
     * @param requestData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D postBodyForData(String url, Map<String, String> headers, Object requestData) throws Exception {
        //请求体。
        String requestBody = this.objectMapper.toString( requestData );
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess( requestBody, null, headers );
        }
        Request.Builder builder = new Request.Builder().url( url );
        if (headers != null) {
            builder.headers( Headers.of( headers ) );
        }
        Request requestBuilder = builder.post( RequestBody.create( requestBody, this.mediaType ) ).build();
        D httpData = initHttpData();
        httpData.setRequestDate( new Date() );
        httpData.setRequestUrl( requestBuilder.url().toString() );
        httpData.setRequestMethod( requestBuilder.method() );
        httpData.setRequestHeader( requestBuilder.headers().toString() );
        if (HttpDataLogLevel.isRecordRequest( httpDataLogLevel ) && StringUtils.isNotBlank( requestBody )) {
            httpData.setRequestData( requestBody );
            if (httpData.getRequestData() != null) {
                httpData.setRequestSize( httpData.getRequestData().length() );
            }
        }
        try (Response response = okHttpClient.newCall( requestBuilder ).execute()) {
            httpData.setResponseDate( new Date() );
            httpData.setStatusCode( response.code() );
            httpData.setResponseType( response.header( "Content-Type" ) );
            httpData.setResponseBytes( response.body().bytes() );
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize( httpData.getResponseBytes().length );
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess( httpData, response.headers() );
            }
        } catch (IOException e) {

            throw new TaskPartnerException( e.getMessage(), e );
        }
        return httpData;
    }

    /**
     * POST RequestBody 获得HttpEntity。
     *
     * @param url
     * @param responseType
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, Class<T> responseType, Object requestData) throws Exception {
        return postBodyForEntity( url, responseType, null, requestData );
    }

    /**
     * POST RequestBody 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, Class<T> responseType, Map<String, String> headers, Object requestData) throws Exception {
        D httpData = postBodyForData( url, headers, requestData );
        T t = this.objectMapper.parse( httpData.getResponseData(), responseType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }

    /**
     * POST RequestBody 获得HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, TypeReference<T> typeRef, Object requestData) throws Exception {
        return postBodyForEntity( url, typeRef, null, requestData );

    }

    /**
     * POST RequestBody 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Object requestData) throws Exception {
        D httpData = postBodyForData( url, headers, requestData );
        T t = this.objectMapper.parse( httpData.getResponseData(), typeRef );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }

    /**
     * POST RequestBody 获得HttpEntity。
     *
     * @param url
     * @param javaType
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, JavaType javaType, Object requestData) throws Exception {
        return postBodyForEntity( url, javaType, null, requestData );
    }

    /**
     * POST RequestBody 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> postBodyForEntity(String url, JavaType javaType, Map<String, String> headers, Object requestData) throws Exception {
        D httpData = postBodyForData( url, headers, requestData );
        T t = this.objectMapper.parse( httpData.getResponseData(), javaType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }


    /**
     * PUT Form获得HttpData。
     *
     * @param url
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D putFormForData(String url, Map<String, String> formData) throws Exception {
        return putFormForData( url, null, formData );
    }

    /**
     * PUT Form获得HttpData。
     *
     * @param url
     * @param headers
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D putFormForData(String url, Map<String, String> headers, Map<String, String> formData) throws Exception {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess( null, formData, headers );
        }
        Request.Builder requestBuilder = new Request.Builder().url( url );
        if (headers != null) {
            requestBuilder.headers( Headers.of( headers ) );
        }
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        if (formData != null) {
            //表单数据。
            for (Map.Entry<String, String> param : formData.entrySet()) {
                formBodyBuilder.add( param.getKey(), param.getValue() );
            }
        }
        Request request = requestBuilder.put( formBodyBuilder.build() ).build();
        D httpData = initHttpData();
        httpData.setRequestDate( new Date() );
        httpData.setRequestUrl( request.url().toString() );
        httpData.setRequestMethod( request.method() );
        httpData.setRequestHeader( request.headers().toString() );
        if (HttpDataLogLevel.isRecordRequest( httpDataLogLevel ) && formData != null && formData.size() > 0) {
            httpData.setRequestData( JsonInterfaceHelper.JSON_CONVERTER.toString( formData ) );
            if (httpData.getRequestData() != null) {
                httpData.setRequestSize( httpData.getRequestData().length() );
            }
        }
        try (Response response = okHttpClient.newCall( request ).execute()) {
            httpData.setResponseDate( new Date() );
            httpData.setStatusCode( response.code() );
            httpData.setResponseType( response.header( "Content-Type" ) );
            httpData.setResponseBytes( response.body().bytes() );
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize( httpData.getResponseBytes().length );
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess( httpData, response.headers() );
            }
        } catch (IOException e) {
            throw new TaskPartnerException( e.getMessage(), e );
        }
        return httpData;
    }

    /**
     * PUT FormData 获得HttpEntity。
     *
     * @param url
     * @param responseType
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, Class<T> responseType, Map<String, String> formData) throws Exception {
        return putFormForEntity( url, responseType, null, formData );
    }

    /**
     * PUT FormData 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> formData) throws Exception {
        D httpData = putFormForData( url, headers, formData );
        T t = this.objectMapper.parse( httpData.getResponseData(), responseType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }


    /**
     * PUT FormData 获得HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> formData) throws Exception {
        return putFormForEntity( url, typeRef, null, formData );
    }

    /**
     * PUT FormData 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Map<String, String> formData) throws Exception {
        D httpData = putFormForData( url, headers, formData );
        T t = this.objectMapper.parse( httpData.getResponseData(), typeRef );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }


    /**
     * PUT FormData 获得HttpEntity。
     *
     * @param url
     * @param javaType
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, JavaType javaType, Map<String, String> formData) throws Exception {
        return putFormForEntity( url, javaType, null, formData );
    }

    /**
     * PUT FormData 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> putFormForEntity(String url, JavaType javaType, Map<String, String> headers, Map<String, String> formData) throws Exception {
        D httpData = putFormForData( url, headers, formData );
        T t = this.objectMapper.parse( httpData.getResponseData(), javaType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }


    /**
     * PUT RequestBody获得HttpData。
     *
     * @param url
     * @param requestData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D putBodyForData(String url, Object requestData) throws Exception {
        Request request = new Request.Builder().url( url ).put( RequestBody.create( this.objectMapper.toString( requestData ), this.mediaType ) ).build();
        return requestForData( request );
    }

    /**
     * PUT RequestBody获得HttpData。
     *
     * @param url
     * @param headers
     * @param requestData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D putBodyForData(String url, Map<String, String> headers, Object requestData) throws Exception {
        //请求体。
        String requestBody = this.objectMapper.toString( requestData );
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess( requestBody, null, headers );
        }
        Request.Builder builder = new Request.Builder().url( url );
        if (headers != null) {
            builder.headers( Headers.of( headers ) );
        }
        Request requestBuilder = builder.put( RequestBody.create( requestBody, this.mediaType ) ).build();
        D httpData = initHttpData();
        httpData.setRequestDate( new Date() );
        httpData.setRequestUrl( requestBuilder.url().toString() );
        httpData.setRequestMethod( requestBuilder.method() );
        httpData.setRequestHeader( requestBuilder.headers().toString() );
        if (HttpDataLogLevel.isRecordRequest( httpDataLogLevel ) && StringUtils.isNotBlank( requestBody )) {
            httpData.setRequestData( requestBody );
            if (httpData.getRequestData() != null) {
                httpData.setRequestSize( httpData.getRequestData().length() );
            }
        }
        try (Response response = okHttpClient.newCall( requestBuilder ).execute()) {
            httpData.setResponseDate( new Date() );
            httpData.setStatusCode( response.code() );
            httpData.setResponseType( response.header( "Content-Type" ) );
            httpData.setResponseBytes( response.body().bytes() );
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize( httpData.getResponseBytes().length );
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess( httpData, response.headers() );
            }
        } catch (IOException e) {

            throw new TaskPartnerException( e.getMessage(), e );
        }
        return httpData;
    }

    /**
     * PUT RequestBody 获得HttpEntity。
     *
     * @param url
     * @param responseType
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, Class<T> responseType, Object requestData) throws Exception {
        return putBodyForEntity( url, responseType, null, requestData );
    }

    /**
     * PUT RequestBody 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, Class<T> responseType, Map<String, String> headers, Object requestData) throws Exception {
        D httpData = putBodyForData( url, headers, requestData );
        T t = this.objectMapper.parse( httpData.getResponseData(), responseType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }

    /**
     * PUT RequestBody 获得HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, TypeReference<T> typeRef, Object requestData) throws Exception {
        return putBodyForEntity( url, typeRef, null, requestData );

    }

    /**
     * PUT RequestBody 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Object requestData) throws Exception {
        D httpData = putBodyForData( url, headers, requestData );
        T t = this.objectMapper.parse( httpData.getResponseData(), typeRef );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }

    /**
     * PUT RequestBody 获得HttpEntity。
     *
     * @param url
     * @param javaType
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, JavaType javaType, Object requestData) throws Exception {
        return putBodyForEntity( url, javaType, null, requestData );
    }

    /**
     * PUT RequestBody 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> putBodyForEntity(String url, JavaType javaType, Map<String, String> headers, Object requestData) throws Exception {
        D httpData = putBodyForData( url, headers, requestData );
        T t = this.objectMapper.parse( httpData.getResponseData(), javaType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }

    /**
     * PATCH Form获得HttpData。
     *
     * @param url
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D patchFormForData(String url, Map<String, String> formData) throws Exception {
        return patchFormForData( url, null, formData );
    }

    /**
     * PATCH Form获得HttpData。
     *
     * @param url
     * @param headers
     * @param formData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D patchFormForData(String url, Map<String, String> headers, Map<String, String> formData) throws Exception {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess( null, formData, headers );
        }
        Request.Builder requestBuilder = new Request.Builder().url( url );
        if (headers != null) {
            requestBuilder.headers( Headers.of( headers ) );
        }
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        if (formData != null) {
            //表单数据。
            for (Map.Entry<String, String> param : formData.entrySet()) {
                formBodyBuilder.add( param.getKey(), param.getValue() );
            }
        }
        Request request = requestBuilder.patch( formBodyBuilder.build() ).build();
        D httpData = initHttpData();
        httpData.setRequestDate( new Date() );
        httpData.setRequestUrl( request.url().toString() );
        httpData.setRequestMethod( request.method() );
        httpData.setRequestHeader( request.headers().toString() );
        if (HttpDataLogLevel.isRecordRequest( httpDataLogLevel ) && formData != null && formData.size() > 0) {
            httpData.setRequestData( JsonInterfaceHelper.JSON_CONVERTER.toString( formData ) );
            if (httpData.getRequestData() != null) {
                httpData.setRequestSize( httpData.getRequestData().length() );
            }
        }
        try (Response response = okHttpClient.newCall( request ).execute()) {
            httpData.setResponseDate( new Date() );
            httpData.setStatusCode( response.code() );
            httpData.setResponseType( response.header( "Content-Type" ) );
            httpData.setResponseBytes( response.body().bytes() );
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize( httpData.getResponseBytes().length );
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess( httpData, response.headers() );
            }
        } catch (IOException e) {
            throw new TaskPartnerException( e.getMessage(), e );
        }
        return httpData;
    }

    /**
     * PATCH FormData 获得HttpEntity。
     *
     * @param url
     * @param responseType
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, Class<T> responseType, Map<String, String> formData) throws Exception {
        return patchFormForEntity( url, responseType, null, formData );
    }

    /**
     * PATCH FormData 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> formData) throws Exception {
        D httpData = patchFormForData( url, headers, formData );
        T t = this.objectMapper.parse( httpData.getResponseData(), responseType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }


    /**
     * PATCH FormData 获得HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> formData) throws Exception {
        return patchFormForEntity( url, typeRef, null, formData );
    }

    /**
     * PATCH FormData 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Map<String, String> formData) throws Exception {
        D httpData = patchFormForData( url, headers, formData );
        T t = this.objectMapper.parse( httpData.getResponseData(), typeRef );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }


    /**
     * PATCH FormData 获得HttpEntity。
     *
     * @param url
     * @param javaType
     * @param formData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, JavaType javaType, Map<String, String> formData) throws Exception {
        return patchFormForEntity( url, javaType, null, formData );
    }

    /**
     * PATCH FormData 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> patchFormForEntity(String url, JavaType javaType, Map<String, String> headers, Map<String, String> formData) throws Exception {
        D httpData = patchFormForData( url, headers, formData );
        T t = this.objectMapper.parse( httpData.getResponseData(), javaType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }


    /**
     * PATCH RequestBody获得HttpData。
     *
     * @param url
     * @param requestData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D patchBodyForData(String url, Object requestData) throws Exception {
        Request request = new Request.Builder().url( url ).patch( RequestBody.create( this.objectMapper.toString( requestData ), this.mediaType ) ).build();
        return requestForData( request );
    }

    /**
     * PATCH RequestBody获得HttpData。
     *
     * @param url
     * @param headers
     * @param requestData
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D patchBodyForData(String url, Map<String, String> headers, Object requestData) throws Exception {
        //请求体。
        String requestBody = this.objectMapper.toString( requestData );
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess( requestBody, null, headers );
        }
        Request.Builder builder = new Request.Builder().url( url );
        if (headers != null) {
            builder.headers( Headers.of( headers ) );
        }
        Request requestBuilder = builder.patch( RequestBody.create( requestBody, this.mediaType ) ).build();
        D httpData = initHttpData();
        httpData.setRequestDate( new Date() );
        httpData.setRequestUrl( requestBuilder.url().toString() );
        httpData.setRequestMethod( requestBuilder.method() );
        httpData.setRequestHeader( requestBuilder.headers().toString() );
        if (HttpDataLogLevel.isRecordRequest( httpDataLogLevel ) && StringUtils.isNotBlank( requestBody )) {
            httpData.setRequestData( requestBody );
            if (httpData.getRequestData() != null) {
                httpData.setRequestSize( httpData.getRequestData().length() );
            }
        }
        try (Response response = okHttpClient.newCall( requestBuilder ).execute()) {
            httpData.setResponseDate( new Date() );
            httpData.setStatusCode( response.code() );
            httpData.setResponseType( response.header( "Content-Type" ) );
            httpData.setResponseBytes( response.body().bytes() );
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize( httpData.getResponseBytes().length );
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess( httpData, response.headers() );
            }
        } catch (IOException e) {

            throw new TaskPartnerException( e.getMessage(), e );
        }
        return httpData;
    }

    /**
     * PATCH RequestBody 获得HttpEntity。
     *
     * @param url
     * @param responseType
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, Class<T> responseType, Object requestData) throws Exception {
        return patchBodyForEntity( url, responseType, null, requestData );
    }

    /**
     * PATCH RequestBody 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, Class<T> responseType, Map<String, String> headers, Object requestData) throws Exception {
        D httpData = patchBodyForData( url, headers, requestData );
        T t = this.objectMapper.parse( httpData.getResponseData(), responseType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }

    /**
     * PATCH RequestBody 获得HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, TypeReference<T> typeRef, Object requestData) throws Exception {
        return patchBodyForEntity( url, typeRef, null, requestData );

    }

    /**
     * PATCH RequestBody 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Object requestData) throws Exception {
        D httpData = patchBodyForData( url, headers, requestData );
        T t = this.objectMapper.parse( httpData.getResponseData(), typeRef );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }

    /**
     * PATCH RequestBody 获得HttpEntity。
     *
     * @param url
     * @param javaType
     * @param requestData
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, JavaType javaType, Object requestData) throws Exception {
        return patchBodyForEntity( url, javaType, null, requestData );
    }

    /**
     * PATCH RequestBody 获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> patchBodyForEntity(String url, JavaType javaType, Map<String, String> headers, Object requestData) throws Exception {
        D httpData = patchBodyForData( url, headers, requestData );
        T t = this.objectMapper.parse( httpData.getResponseData(), javaType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }

    /**
     * 使用Delete方法获得HttpData。
     *
     * @param url
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D deleteForData(String url) throws Exception {
        return deleteForData( url, null, null );
    }

    /**
     * 使用Delete方法获得HttpData。
     *
     * @param url
     * @param queryParam
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D deleteForData(String url, Map<String, String> queryParam) throws Exception {
        return deleteForData( url, null, queryParam );
    }

    /**
     * 使用Delete方法获得HttpData。
     *
     * @param url
     * @param headers
     * @param queryParam
     * @param <D>
     * @return
     * @throws Exception
     */
    public <D extends HttpData> D deleteForData(String url, Map<String, String> headers, Map<String, String> queryParam) throws Exception {
        //request过滤器。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.requestProcess( null, queryParam, headers );
        }
        Request.Builder requestBuilder = new Request.Builder().url( buildUrl( url, queryParam ) );
        if (headers != null) {
            requestBuilder.headers( Headers.of( headers ) );
        }
        Request request = requestBuilder.delete().build();
        D httpData = initHttpData();
        httpData.setRequestDate( new Date() );
        httpData.setRequestUrl( request.url().toString() );
        httpData.setRequestMethod( request.method() );
        httpData.setRequestHeader( request.headers().toString() );
        try (Response response = okHttpClient.newCall( request ).execute()) {
            httpData.setResponseDate( new Date() );
            httpData.setStatusCode( response.code() );
            httpData.setResponseType( response.header( "Content-Type" ) );
            httpData.setResponseBytes( response.body().bytes() );
            if (httpData.getResponseBytes() != null) {
                httpData.setResponseSize( httpData.getResponseBytes().length );
            }
            if (this.httpDataProcessor != null) {
                this.httpDataProcessor.responseProcess( httpData, response.headers() );
            }
        } catch (IOException e) {
            throw new TaskPartnerException( e.getMessage(), e );
        }

        return httpData;
    }

    /**
     * 使用Delete方法获得HttpEntity。
     *
     * @param url
     * @param responseType
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, Class<T> responseType) throws Exception {
        return deleteForEntity( url, responseType, null, null );
    }

    /**
     * 使用Delete方法获得HttpEntity。
     *
     * @param url
     * @param responseType
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, Class<T> responseType, Map<String, String> queryParam) throws Exception {
        return deleteForEntity( url, responseType, null, queryParam );
    }

    /**
     * 使用Delete方法获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> queryParam) throws Exception {
        D httpData = deleteForData( url, headers, queryParam );
        T t = this.objectMapper.parse( httpData.getResponseData(), responseType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }


    /**
     * 使用Delete方法获得HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, TypeReference<T> typeRef) throws Exception {
        return deleteForEntity( url, typeRef, null, null );

    }

    /**
     * 使用Delete方法获得HttpEntity。
     *
     * @param url
     * @param typeRef
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, TypeReference<T> typeRef, Map<String, String> queryParam) throws Exception {
        return deleteForEntity( url, typeRef, null, queryParam );
    }

    /**
     * 使用Delete方法获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, TypeReference<T> typeRef, Map<String, String> headers, Map<String, String> queryParam) throws Exception {
        D httpData = deleteForData( url, headers, queryParam );
        T t = this.objectMapper.parse( httpData.getResponseData(), typeRef );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }


    /**
     * 使用Delete方法获得HttpEntity。
     *
     * @param url
     * @param javaType
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, JavaType javaType) throws Exception {
        return deleteForEntity( url, javaType, null, null );
    }

    /**
     * 使用Delete方法获得HttpEntity。
     *
     * @param url
     * @param javaType
     * @param queryParam
     * @param <D>
     * @param <T>
     * @return
     * @throws Exception
     */
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, JavaType javaType, Map<String, String> queryParam) throws Exception {
        return deleteForEntity( url, javaType, null, queryParam );
    }

    /**
     * 使用Delete方法获得HttpEntity。
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
    public <D extends HttpData, T> HttpEntity<D, T> deleteForEntity(String url, JavaType javaType, Map<String, String> headers, Map<String, String> queryParam) throws Exception {
        D httpData = deleteForData( url, headers, queryParam );
        T t = this.objectMapper.parse( httpData.getResponseData(), javaType );
        //处理日志。
        if (this.httpDataProcessor != null) {
            this.httpDataProcessor.postProcess( httpData, t );
        }
        return new HttpEntity<>( httpData, t );
    }


    /**
     * 构造请求链接。
     *
     * @param url        url路径
     * @param queryParam 参数
     * @return HttpUrl url
     */
    private HttpUrl buildUrl(String url, Map<String, String> queryParam) {
        HttpUrl httpUrl = HttpUrl.parse( url );
        if (httpUrl == null) {
            throw new RuntimeException( "url:[" + url + "] is invalid!" );
        }
        if (queryParam != null) {
            HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
            queryParam.forEach( (key, value) -> {
                if (StringUtils.isNotBlank( value )) {
                    urlBuilder.addQueryParameter( key, value );
                }
            } );
            httpUrl = urlBuilder.build();
        }
        return httpUrl;
    }

    /**
     * 构造httpData。
     *
     * @return
     */
    private <D extends HttpData> D initHttpData() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        HttpData httpLog = null;
        if (this.httpDataCls != null) {
            httpLog = this.httpDataCls.getDeclaredConstructor().newInstance();
        } else {
            httpLog = new HttpDefaultData();
        }
        return (D) httpLog;
    }
}
