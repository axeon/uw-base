package uw.auth.service.exception;

/**
 * token提权异常。
 */
public class TokenSudoException extends RuntimeException {
    public TokenSudoException(String message) {
        super(message);
    }

    public TokenSudoException(String message, Throwable cause) {
        super(message, cause);
    }
}
