package uw.auth.service.exception;

/**
 * Token 过期异常。
 * <p>
 * 由 {@code AuthServiceHelper.parseRawToken} 在检测到 Token 已超过 expireAt 时抛出，
 * {@code GlobalExceptionAdvice} 将其映射为 HTTP <b>498</b>（自定义 Token 过期码）。
 *
 * @author axeon
 */
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }

    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
