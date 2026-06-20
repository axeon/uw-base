package uw.auth.service.exception;

import org.apache.commons.lang3.StringUtils;
import uw.auth.service.constant.AuthServiceConstants;
import uw.common.response.ResponseData;

/**
 * 认证异常转换辅助类。
 * <p>
 * 将 {@code ResponseData} 携带的业务错误码转换为对应的运行时异常，
 * 供 {@code AuthServiceFilter} 调用 {@code HandlerExceptionResolver} 统一处理，
 * 再由 {@code GlobalExceptionAdvice} 映射为 HTTP 状态码。
 *
 * @author axeon
 * @see uw.auth.service.advice.GlobalExceptionAdvice
 */
public class AuthExceptionHelper {

    /**
     * 将 ResponseData 的 code 转换为对应的认证运行时异常。
     *
     * @param responseData 鉴权流程产生的响应数据
     * @return 对应的 RuntimeException；code 无法识别时返回 {@code null}
     */
    public static RuntimeException convertException(ResponseData<?> responseData) throws RuntimeException {
        String code = responseData.getCode();
        if (StringUtils.isBlank(code)) {
            return null;
        }
        if (code.equals(AuthServiceConstants.HTTP_UNAUTHORIZED_CODE)) {
            return new TokenInvalidException(responseData.getMsg());
        } else if (code.equals(AuthServiceConstants.HTTP_FORBIDDEN_CODE)) {
            return new TokenPermException(responseData.getMsg());
        } else if (code.equals(AuthServiceConstants.HTTP_TOKEN_EXPIRED_CODE)) {
            return new TokenExpiredException(responseData.getMsg());
        } else if (code.equals(AuthServiceConstants.HTTP_PAYMENT_REQUIRED_CODE)) {
            return new TokenPayException(responseData.getMsg());
        } else if (code.equals(AuthServiceConstants.HTTP_UPGRADE_REQUIRED_CODE)) {
            return new TokenSudoException(responseData.getMsg());
        } else if (code.equals(AuthServiceConstants.HTTP_SERVICE_UNAVAILABLE_CODE)) {
            return new TokenServiceException(responseData.getMsg());
        } else {
            return null;
        }
    }

}
