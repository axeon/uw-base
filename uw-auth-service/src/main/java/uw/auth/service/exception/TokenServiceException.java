package uw.auth.service.exception;

/**
 * Token服务异常。
 */
public class TokenServiceException extends RuntimeException {
    public TokenServiceException(String message) {
        super(message);
    }

    public TokenServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
