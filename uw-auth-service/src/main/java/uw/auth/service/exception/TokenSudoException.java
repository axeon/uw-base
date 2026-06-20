package uw.auth.service.exception;

/**
 * Token 需提权（SUDO）异常。
 * <p>
 * 访问 {@link uw.auth.service.constant.AuthType#SUDO} 接口但当前 Token
 * 非 SUDO 类型、需要切换到超级权限模式时抛出，
 * {@code GlobalExceptionAdvice} 将其映射为 HTTP <b>426</b>。
 *
 * @author axeon
 */
public class TokenSudoException extends RuntimeException {
    public TokenSudoException(String message) {
        super(message);
    }

    public TokenSudoException(String message, Throwable cause) {
        super(message, cause);
    }
}
