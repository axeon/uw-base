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

    /**
     * 构造一个不带消息和原因的 HTTP 请求异常。
     */
    public HttpRequestException() {
        super();
    }

    /**
     * 构造一个带消息的 HTTP 请求异常。
     *
     * @param msg 异常消息（如底层 IOException 的信息）。
     */
    public HttpRequestException(String msg) {
        super(msg);
    }

    /**
     * 构造一个带原因的 HTTP 请求异常。
     *
     * @param nestedThrowable 原始异常（通常是 IOException）。
     */
    public HttpRequestException(Throwable nestedThrowable) {
        super(nestedThrowable);
    }

    /**
     * 构造一个带消息和原因的 HTTP 请求异常。
     *
     * @param msg             异常消息。
     * @param nestedThrowable 原始异常。
     */
    public HttpRequestException(String msg, Throwable nestedThrowable) {
        super(msg, nestedThrowable);
    }
}
