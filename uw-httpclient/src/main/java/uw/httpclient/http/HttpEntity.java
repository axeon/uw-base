package uw.httpclient.http;

/**
 * 封装返回数据的结构体。
 * 主要封装了请求日志。
 *
 * @since 2017/9/20
 */
public class HttpEntity<D extends HttpData, V> {


    /**
     * HttpData。
     */
    private D httpData;

    /**
     * 转换对象。
     */
    private V value;

    public HttpEntity(D httpData, V value) {
        this.httpData = httpData;
        this.value = value;
    }

    public D getHttpData() {
        return httpData;
    }

    public void setHttpData(D httpData) {
        this.httpData = httpData;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }
}
