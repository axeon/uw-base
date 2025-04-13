package uw.auth.service.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uw.auth.service.AuthServiceHelper;
import uw.auth.service.util.MscUtils;
import uw.auth.service.vo.MscActionLog;
import uw.common.dto.ResponseData;

/**
 * 全局异常处理，通过此类捕获全局异常。
 */
@RestControllerAdvice
public class GlobalExceptionAdvice {
    private static final Logger log = LoggerFactory.getLogger( GlobalExceptionAdvice.class );

    public GlobalExceptionAdvice() {
        log.info( "Init GlobalExceptionAdvice." );
    }

    @ExceptionHandler({Throwable.class})
    public ResponseData<String> exceptionHandle(Throwable ex, HttpServletRequest request, HttpServletResponse response) {
        // 针对ErrorResponse异常，设置不同的状态码。
        if (ex instanceof ErrorResponse errorResponse) {
            response.setStatus( errorResponse.getStatusCode().value() );
            log.warn( ex.getMessage() );
        } else {
            response.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
            //500类异常，要打印到日志里。
            log.error( ex.getMessage(), ex );
        }
        //针对不同类型异常，设置不同的详细消息。
        String detailData;
        if (response.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            detailData = MscUtils.exceptionToString( ex );
        } else {
            detailData = ex.getMessage();
        }
        return ResponseData.error( detailData, "http.status." + String.valueOf( response.getStatus() ), ex.getMessage() );
    }

}
