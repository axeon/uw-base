package uw.httpclient.http;

/**
 * 封装返回数据的结构体，主要承载请求日志与反序列化后的响应对象。
 * <p>
 * 由 {@code *ForEntity} 系列方法返回，包含两部分：
 * <ul>
 *   <li>{@link #httpData}：完整的请求/响应日志数据（URL、状态码、响应字节等）；</li>
 *   <li>{@link #value}：响应体按指定类型反序列化后的对象。</li>
 * </ul>
 *
 * @param <D> HttpData 实现类型，承载请求/响应日志。
 * @param <V> 响应体反序列化后的值类型。
 * @since 2017/9/20
 */
public class HttpEntity<D extends HttpData, V> {


    /**
     * HttpData，请求/响应日志数据。
     */
    private D httpData;

    /**
     * 转换对象，响应体反序列化后的值。
     */
    private V value;

    /**
     * 构造 HttpEntity。
     *
     * @param httpData 请求/响应日志数据。
     * @param value    反序列化后的响应对象。
     */
    public HttpEntity(D httpData, V value) {
        this.httpData = httpData;
        this.value = value;
    }

    /**
     * 获取请求/响应日志数据。
     *
     * @return HttpData。
     */
    public D getHttpData() {
        return httpData;
    }

    /**
     * 设置请求/响应日志数据。
     *
     * @param httpData HttpData。
     */
    public void setHttpData(D httpData) {
        this.httpData = httpData;
    }

    /**
     * 获取反序列化后的响应对象。
     * <p>
     * 注意：方法名为 {@code getValue}，而非 {@code getBody}。
     *
     * @return 反序列化后的响应对象，可能为 null。
     */
    public V getValue() {
        return value;
    }

    /**
     * 设置反序列化后的响应对象。
     *
     * @param value 响应对象。
     */
    public void setValue(V value) {
        this.value = value;
    }
}
