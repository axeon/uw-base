package uw.auth.service.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uw.auth.service.util.MscUtils;
import uw.common.dto.ResponseData;

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
        String msg = "Path: " + request.getRequestURI() + ", Msg: " + ex.toString();
        String data = null;
        // 针对ErrorResponse异常，设置不同的状态码。
        if (ex instanceof ErrorResponse errorResponse) {
            response.setStatus(errorResponse.getStatusCode().value());
            log.warn(msg);
        } else {
            //其它错误都当做500类异常。
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            //500类异常，要打印到日志里。
            log.error(msg, ex);
            data = MscUtils.exceptionToString(ex);
        }
        return ResponseData.error(data, "http.status." + response.getStatus(), msg);
    }

}
