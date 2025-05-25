package uw.auth.service.exception;

/**
 * token支付异常。
 */
public class TokenPayException extends RuntimeException {
    public TokenPayException(String message) {
        super(message);
    }

    public TokenPayException(String message, Throwable cause) {
        super(message, cause);
    }
}
