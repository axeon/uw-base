package uw.auth.service.exception;

/**
 * Token 权限不足异常。
 * <p>
 * 在用户类型不匹配、Token 类型不足或权限集合中缺少目标 permCode 时抛出，
 * {@code GlobalExceptionAdvice} 将其映射为 HTTP <b>403</b>。
 *
 * @author axeon
 */
public class TokenPermException extends RuntimeException {
    public TokenPermException(String message) {
        super(message);
    }

    public TokenPermException(String message, Throwable cause) {
        super(message, cause);
    }
}
