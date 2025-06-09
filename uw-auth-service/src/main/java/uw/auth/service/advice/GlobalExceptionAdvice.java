package uw.auth.service.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import uw.auth.service.constant.AuthServiceConstants;
import uw.auth.service.exception.*;
import uw.auth.service.util.IpWebUtils;
import uw.auth.service.util.MscUtils;
import uw.common.dto.ResponseData;

import java.io.IOException;

/**
 * 全局异常处理，通过此类捕获全局异常。
 */
@RestControllerAdvice
public class GlobalExceptionAdvice {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionAdvice.class);

    public GlobalExceptionAdvice() {
        log.info("Init GlobalExceptionAdvice.");
    }

    @ExceptionHandler({Throwable.class})
    public ResponseData<String> exceptionHandle(Throwable ex, HttpServletRequest request, HttpServletResponse response) {
        //针对不同类型异常，设置不同的详细消息。
        String userIp = IpWebUtils.getRealIp(request);
        String msg = "UserIp: [" + userIp + "], Request Path: [" + request.getRequestURI() + "], Method: [" + request.getMethod() + "], Msg: " + ex.toString();
        String data = null;
        // 针对ErrorResponse异常，设置不同的状态码。
        if (ex instanceof ErrorResponse errorResponse) {
            response.setStatus(errorResponse.getStatusCode().value());
            log.warn(msg);
        } else if (ex instanceof TokenInvalidException) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        } else if (ex instanceof TokenExpiredException) {
            response.setStatus(Integer.parseInt(AuthServiceConstants.HTTP_TOKEN_EXPIRED_CODE));
        } else if (ex instanceof TokenPermException) {
            response.setStatus(Integer.parseInt(AuthServiceConstants.HTTP_FORBIDDEN_CODE));
        } else if (ex instanceof TokenPayException) {
            response.setStatus(Integer.parseInt(AuthServiceConstants.HTTP_PAYMENT_REQUIRED_CODE));
        } else if (ex instanceof TokenServiceException) {
            response.setStatus(Integer.parseInt(AuthServiceConstants.HTTP_SERVICE_UNAVAILABLE_CODE));
        } else if (ex instanceof TokenSudoException) {
            response.setStatus(Integer.parseInt(AuthServiceConstants.HTTP_UPGRADE_REQUIRED_CODE));
        } else if (ex instanceof IOException) {
            //IO异常（如AsyncRequestNotUsableException），一般是客户端主动断开的请求，这里返回null，不返回错误信息。
            log.error(msg);
            return null;
        }else {
            //其它错误都当做500类异常。
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            //500类异常，要打印到日志里。
            log.error(msg, ex);
            data = MscUtils.exceptionToString(ex);
        }
        return ResponseData.error(data, "http.status." + response.getStatus(), msg);
    }

}
