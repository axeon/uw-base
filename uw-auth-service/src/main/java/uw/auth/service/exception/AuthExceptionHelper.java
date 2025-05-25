package uw.auth.service.exception;

import org.apache.commons.lang3.StringUtils;
import uw.auth.service.constant.AuthServiceConstants;
import uw.common.dto.ResponseData;

/**
 * 认证异常处理.
 */
public class AuthExceptionHelper {

    /**
     * 转换异常.
     *
     * @param responseData
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
