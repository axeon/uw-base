package uw.auth.service.exception;

/**
 * token权限异常。
 */
public class TokenPermException extends RuntimeException {
    public TokenPermException(String message) {
        super(message);
    }

    public TokenPermException(String message, Throwable cause) {
        super(message, cause);
    }
}
