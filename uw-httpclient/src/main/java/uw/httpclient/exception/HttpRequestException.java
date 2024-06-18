package uw.httpclient.exception;

import uw.task.exception.TaskPartnerException;

/**
 * http请求异常。
 *
 * @since 2017/9/22
 */
public class HttpRequestException extends TaskPartnerException {

    private static final long serialVersionUID = -4816326148147854194L;

    public HttpRequestException() {
        super();
    }

    public HttpRequestException(String msg) {
        super( msg );
    }

    public HttpRequestException(Throwable nestedThrowable) {
        super( nestedThrowable );
    }

    public HttpRequestException(String msg, Throwable nestedThrowable) {
        super( msg, nestedThrowable );
    }
}
