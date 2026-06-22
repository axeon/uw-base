package uw.httpclient.http;

import okhttp3.CookieJar;
import okhttp3.Interceptor;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * HTTP 客户端配置。
 * <p>
 * 不可变配置对象，承载连接/读/写超时、连接失败重试、连接池、并发限制以及 SSL 相关参数。
 * 推荐通过 {@link #builder()} 或 {@link #builder(HttpConfig)} 构建；
 * 全参构造器因参数过多易混淆已标记 {@link Deprecated}。
 * <p>
 * 传入 {@code HttpInterface} 后，每个实例使用独立的 Dispatcher，
 * maxRequests / maxRequestsPerHost 仅作用于该实例，不会影响全局或其他实例。
 *
 * @since 2017/9/21
 */
public class HttpConfig {

    /**
     * 连接超时时间 - 毫秒。
     */
    private final long connectTimeout;

    /**
     * 读超时时间 - 毫秒。
     */
    private final long readTimeout;

    /**
     * 写超时时间 - 毫秒。
     */
    private final long writeTimeout;

    /**
     * 当一个连接失败时,配置此值可以进行重试。
     */
    private final boolean retryOnConnectionFailure;

    /**
     * 每主机最大并发请求数。
     */
    private final int maxRequestsPerHost;

    /**
     * 全局最大并发请求数。
     */
    private final int maxRequests;

    /**
     * 连接池最大空闲连接数。
     */
    private final int maxIdleConnections;

    /**
     * 连接池中空闲连接存活时间毫秒数。
     */
    private final long keepAliveTimeout;

    /**
     * sslSocketFactory
     */
    private final SSLSocketFactory sslSocketFactory;

    /**
     * trustManager
     */
    private final X509TrustManager trustManager;

    /**
     * hostnameVerifier
     */
    private final HostnameVerifier hostnameVerifier;

    /**
     * 默认请求头，对所有请求自动追加（业务侧传入的同名头会覆盖默认值）。可能为 null。
     */
    private final Map<String, String> defaultHeaders;

    /**
     * CookieJar，用于会话/Cookie 持久化，为 null 时使用 OkHttp 默认（不持久化）。可能为 null。
     */
    private final CookieJar cookieJar;

    /**
     * 应用拦截器链（在 OkHttp 重试/重定向前介入），不可为 null（无则为空列表）。
     */
    private final List<Interceptor> interceptors;

    /**
     * 网络拦截器链（在 OkHttp 重试/重定向后、真实网络请求前介入），不可为 null（无则为空列表）。
     */
    private final List<Interceptor> networkInterceptors;

    /**
     * 全参构造器。
     *
     * @param connectTimeout          连接超时（毫秒）。
     * @param readTimeout             读超时（毫秒）。
     * @param writeTimeout            写超时（毫秒）。
     * @param retryOnConnectionFailure 连接失败是否重试。
     * @param maxRequestsPerHost      每主机最大并发请求数。
     * @param maxRequests             全局最大并发请求数。
     * @param maxIdleConnections      连接池最大空闲连接数。
     * @param keepAliveTimeout        空闲连接存活时间（毫秒）。
     * @param sslSocketFactory        SSLSocketFactory。
     * @param trustManager            X509TrustManager。
     * @param hostnameVerifier        HostnameVerifier。
     */
    public HttpConfig(long connectTimeout, long readTimeout, long writeTimeout, boolean retryOnConnectionFailure, int maxRequestsPerHost, int maxRequests, int maxIdleConnections
            , long keepAliveTimeout, SSLSocketFactory sslSocketFactory, X509TrustManager trustManager, HostnameVerifier hostnameVerifier) {
        this(connectTimeout, readTimeout, writeTimeout, retryOnConnectionFailure,
                maxRequestsPerHost, maxRequests, maxIdleConnections, keepAliveTimeout,
                sslSocketFactory, trustManager, hostnameVerifier, null, null, null, null);
    }

    /**
     * 全参构造器（含默认头/Cookie/拦截器）。
     *
     * @param connectTimeout          连接超时（毫秒）。
     * @param readTimeout             读超时（毫秒）。
     * @param writeTimeout            写超时（毫秒）。
     * @param retryOnConnectionFailure 连接失败是否重试。
     * @param maxRequestsPerHost      每主机最大并发请求数。
     * @param maxRequests             全局最大并发请求数。
     * @param maxIdleConnections      连接池最大空闲连接数。
     * @param keepAliveTimeout        空闲连接存活时间（毫秒）。
     * @param sslSocketFactory        SSLSocketFactory。
     * @param trustManager            X509TrustManager。
     * @param hostnameVerifier        HostnameVerifier。
     * @param defaultHeaders          默认请求头（自动追加，业务传入同名头会覆盖），可为 null。
     * @param cookieJar               CookieJar，可为 null。
     * @param interceptors            应用拦截器链，可为 null。
     * @param networkInterceptors     网络拦截器链，可为 null。
     */
    public HttpConfig(long connectTimeout, long readTimeout, long writeTimeout, boolean retryOnConnectionFailure,
                      int maxRequestsPerHost, int maxRequests, int maxIdleConnections, long keepAliveTimeout,
                      SSLSocketFactory sslSocketFactory, X509TrustManager trustManager, HostnameVerifier hostnameVerifier,
                      Map<String, String> defaultHeaders, CookieJar cookieJar,
                      List<Interceptor> interceptors, List<Interceptor> networkInterceptors) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
        this.retryOnConnectionFailure = retryOnConnectionFailure;
        this.maxRequestsPerHost = maxRequestsPerHost;
        this.maxRequests = maxRequests;
        this.maxIdleConnections = maxIdleConnections;
        this.keepAliveTimeout = keepAliveTimeout;
        this.sslSocketFactory = sslSocketFactory;
        this.trustManager = trustManager;
        this.hostnameVerifier = hostnameVerifier;
        this.defaultHeaders = defaultHeaders == null ? null : Collections.unmodifiableMap(new java.util.LinkedHashMap<>(defaultHeaders));
        this.cookieJar = cookieJar;
        this.interceptors = interceptors == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(interceptors));
        this.networkInterceptors = networkInterceptors == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(networkInterceptors));
    }

    private HttpConfig(Builder builder) {
        connectTimeout = builder.connectTimeout;
        readTimeout = builder.readTimeout;
        writeTimeout = builder.writeTimeout;
        retryOnConnectionFailure = builder.retryOnConnectionFailure;
        maxRequestsPerHost = builder.maxRequestsPerHost;
        maxRequests = builder.maxRequests;
        maxIdleConnections = builder.maxIdleConnections;
        keepAliveTimeout = builder.keepAliveTimeout;
        sslSocketFactory = builder.sslSocketFactory;
        trustManager = builder.trustManager;
        hostnameVerifier = builder.hostnameVerifier;
        defaultHeaders = builder.defaultHeaders == null ? null : Collections.unmodifiableMap(new java.util.LinkedHashMap<>(builder.defaultHeaders));
        cookieJar = builder.cookieJar;
        interceptors = builder.interceptors == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(builder.interceptors));
        networkInterceptors = builder.networkInterceptors == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(builder.networkInterceptors));
    }

    /**
     * 创建一个新的 {@link Builder}。
     *
     * @return Builder 实例。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 基于已有 {@link HttpConfig} 创建一个拷贝用的 {@link Builder}，便于在其基础上修改少量字段。
     *
     * @param copy 源配置。
     * @return 已载入源配置的 Builder。
     */
    public static Builder builder(HttpConfig copy) {
        Builder builder = new Builder();
        builder.connectTimeout = copy.getConnectTimeout();
        builder.readTimeout = copy.getReadTimeout();
        builder.writeTimeout = copy.getWriteTimeout();
        builder.retryOnConnectionFailure = copy.isRetryOnConnectionFailure();
        builder.maxRequestsPerHost = copy.getMaxRequestsPerHost();
        builder.maxRequests = copy.getMaxRequests();
        builder.maxIdleConnections = copy.getMaxIdleConnections();
        builder.keepAliveTimeout = copy.getKeepAliveTimeout();
        builder.sslSocketFactory = copy.getSslSocketFactory();
        builder.trustManager = copy.getTrustManager();
        builder.hostnameVerifier = copy.getHostnameVerifier();
        builder.defaultHeaders = copy.getDefaultHeaders();
        builder.cookieJar = copy.getCookieJar();
        builder.interceptors = copy.getInterceptors();
        builder.networkInterceptors = copy.getNetworkInterceptors();
        return builder;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public long getWriteTimeout() {
        return writeTimeout;
    }

    public boolean isRetryOnConnectionFailure() {
        return retryOnConnectionFailure;
    }

    public int getMaxRequestsPerHost() {
        return maxRequestsPerHost;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public long getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    public X509TrustManager getTrustManager() {
        return trustManager;
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    /**
     * 获取默认请求头。
     *
     * @return 默认请求头 Map，可能为 null。
     */
    public Map<String, String> getDefaultHeaders() {
        return defaultHeaders;
    }

    /**
     * 获取 CookieJar。
     *
     * @return CookieJar，可能为 null。
     */
    public CookieJar getCookieJar() {
        return cookieJar;
    }

    /**
     * 获取应用拦截器链（不可变）。
     *
     * @return 应用拦截器列表，不会为 null（无则为空列表）。
     */
    public List<Interceptor> getInterceptors() {
        return interceptors;
    }

    /**
     * 获取网络拦截器链（不可变）。
     *
     * @return 网络拦截器列表，不会为 null（无则为空列表）。
     */
    public List<Interceptor> getNetworkInterceptors() {
        return networkInterceptors;
    }

    /**
     * {@link HttpConfig} 的构建器，支持链式设置各配置项。
     */
    public static final class Builder {
        private long connectTimeout;
        private long readTimeout;
        private long writeTimeout;
        private boolean retryOnConnectionFailure;
        private int maxRequestsPerHost;
        private int maxRequests;
        private int maxIdleConnections;
        private long keepAliveTimeout;
        private SSLSocketFactory sslSocketFactory;
        private X509TrustManager trustManager;
        private HostnameVerifier hostnameVerifier;
        private Map<String, String> defaultHeaders;
        private CookieJar cookieJar;
        private List<Interceptor> interceptors;
        private List<Interceptor> networkInterceptors;

        private Builder() {
        }

        public Builder connectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(long readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder writeTimeout(long writeTimeout) {
            this.writeTimeout = writeTimeout;
            return this;
        }

        public Builder retryOnConnectionFailure(boolean retryOnConnectionFailure) {
            this.retryOnConnectionFailure = retryOnConnectionFailure;
            return this;
        }

        public Builder maxRequestsPerHost(int maxRequestsPerHost) {
            this.maxRequestsPerHost = maxRequestsPerHost;
            return this;
        }

        public Builder maxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
            return this;
        }

        public Builder maxIdleConnections(int maxIdleConnections) {
            this.maxIdleConnections = maxIdleConnections;
            return this;
        }

        public Builder keepAliveTimeout(long keepAliveTimeout) {
            this.keepAliveTimeout = keepAliveTimeout;
            return this;
        }

        public Builder sslSocketFactory(SSLSocketFactory sslSocketFactory) {
            this.sslSocketFactory = sslSocketFactory;
            return this;
        }

        public Builder trustManager(X509TrustManager trustManager) {
            this.trustManager = trustManager;
            return this;
        }

        public Builder hostnameVerifier(HostnameVerifier hostnameVerifier) {
            this.hostnameVerifier = hostnameVerifier;
            return this;
        }

        /**
         * 设置默认请求头，对所有请求自动追加。
         * <p>
         * 业务侧调用时传入的同名头会覆盖默认值。多次调用会覆盖之前的默认头。
         *
         * @param defaultHeaders 默认请求头，可为 null。
         * @return Builder。
         */
        public Builder defaultHeaders(Map<String, String> defaultHeaders) {
            this.defaultHeaders = defaultHeaders;
            return this;
        }

        /**
         * 设置 CookieJar（会话/Cookie 持久化）。
         *
         * @param cookieJar CookieJar，可为 null。
         * @return Builder。
         */
        public Builder cookieJar(CookieJar cookieJar) {
            this.cookieJar = cookieJar;
            return this;
        }

        /**
         * 设置应用拦截器链（OkHttp 重试/重定向前介入）。
         * <p>
         * 注意：拦截器会注册到本实例派生出的 OkHttpClient 上，
         * 由于实例间不共享 client，拦截器仅作用于本 {@code HttpInterface} 实例。
         *
         * @param interceptors 应用拦截器列表，可为 null。
         * @return Builder。
         */
        public Builder interceptors(List<Interceptor> interceptors) {
            this.interceptors = interceptors;
            return this;
        }

        /**
         * 追加单个应用拦截器。
         *
         * @param interceptor 应用拦截器。
         * @return Builder。
         */
        public Builder addInterceptor(Interceptor interceptor) {
            if (interceptor != null) {
                if (this.interceptors == null) {
                    this.interceptors = new ArrayList<>();
                }
                this.interceptors.add(interceptor);
            }
            return this;
        }

        /**
         * 设置网络拦截器链（OkHttp 重试/重定向后、真实网络请求前介入）。
         *
         * @param networkInterceptors 网络拦截器列表，可为 null。
         * @return Builder。
         */
        public Builder networkInterceptors(List<Interceptor> networkInterceptors) {
            this.networkInterceptors = networkInterceptors;
            return this;
        }

        /**
         * 追加单个网络拦截器。
         *
         * @param interceptor 网络拦截器。
         * @return Builder。
         */
        public Builder addNetworkInterceptor(Interceptor interceptor) {
            if (interceptor != null) {
                if (this.networkInterceptors == null) {
                    this.networkInterceptors = new ArrayList<>();
                }
                this.networkInterceptors.add(interceptor);
            }
            return this;
        }

        /**
         * 构建不可变的 {@link HttpConfig}。
         *
         * @return HttpConfig 实例。
         */
        public HttpConfig build() {
            return new HttpConfig(this);
        }
    }
}
