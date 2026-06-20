package uw.auth.service.exception;

/**
 * Token 无效异常。
 * <p>
 * 在 Token 缺失、格式非法、命中非法 Token 黑名单或被 auth-center 判定无效时抛出，
 * {@code GlobalExceptionAdvice} 将其映射为 HTTP <b>401</b>。
 *
 * @author axeon
 */
public class TokenInvalidException extends RuntimeException {
    public TokenInvalidException(String message) {
        super(message);
    }

    public TokenInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
