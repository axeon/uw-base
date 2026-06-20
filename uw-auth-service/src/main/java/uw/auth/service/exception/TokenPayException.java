package uw.auth.service.exception;

/**
 * Token 需付费异常。
 * <p>
 * 当访问的接口要求开通付费功能但当前用户/租户未开通时抛出，
 * {@code GlobalExceptionAdvice} 将其映射为 HTTP <b>402</b>。
 *
 * @author axeon
 */
public class TokenPayException extends RuntimeException {
    public TokenPayException(String message) {
        super(message);
    }

    public TokenPayException(String message, Throwable cause) {
        super(message, cause);
    }
}
