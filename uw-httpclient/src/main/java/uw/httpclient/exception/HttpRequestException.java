package uw.httpclient.exception;

import uw.task.exception.TaskPartnerException;

/**
 * HTTP 请求异常。
 * <p>
 * 在 HTTP 请求阶段发生错误时抛出，包括但不限于：
 * URL 非法、连接超时、读/写超时、网络 IO 失败、SSL 握手失败等。
 * <p>
 * 继承 {@link TaskPartnerException}，表示此类错误通常源于接口方/网络环境。
 *
 * @since 2017/9/22
 */
public class HttpRequestException extends TaskPartnerException {

    public HttpRequestException() {
        super();
    }

    public HttpRequestException(String msg) {
        super(msg);
    }

    public HttpRequestException(Throwable nestedThrowable) {
        super(nestedThrowable);
    }

    public HttpRequestException(String msg, Throwable nestedThrowable) {
        super(msg, nestedThrowable);
    }
}
