package uw.auth.service.exception;

/**
 * Token无效异常。
 */
public class TokenInvalidateException extends RuntimeException {

    public TokenInvalidateException(String msg) {
        super(msg);
    }

    public TokenInvalidateException(String msg, Throwable t) {
        super(msg, t);
    }
}
