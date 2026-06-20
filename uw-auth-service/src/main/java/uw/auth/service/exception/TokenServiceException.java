package uw.auth.service.exception;

/**
 * Token 服务不可用异常。
 * <p>
 * 当应用权限数据（appPermMap）尚未初始化完成、或 auth-center 不可达导致无法判定权限时抛出，
 * {@code GlobalExceptionAdvice} 将其映射为 HTTP <b>503</b>。
 *
 * @author axeon
 */
public class TokenServiceException extends RuntimeException {
    public TokenServiceException(String message) {
        super(message);
    }

    public TokenServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
