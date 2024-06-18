package uw.auth.service.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uw.auth.service.AuthServiceHelper;
import uw.common.dto.ResponseData;
import uw.auth.service.util.MscUtils;
import uw.auth.service.vo.MscActionLog;

/**
 * 全局异常处理，通过此类捕获全局异常。
 *
 */
@RestControllerAdvice
public class GlobalExceptionAdvice {
    private static final Logger log = LoggerFactory.getLogger( GlobalExceptionAdvice.class );

    public GlobalExceptionAdvice() {
        log.info( "Initializing GlobalExceptionAdvice..." );
    }

    @ExceptionHandler({Throwable.class})
    public ResponseData<String> exceptionHandle(Throwable ex, HttpServletRequest request, HttpServletResponse response) {
        MscActionLog mscActionLog = AuthServiceHelper.getContextLog();
        Class<?> exceptionClass = ex.getClass();
        String detailData;
        if (exceptionClass.equals( HttpMediaTypeNotAcceptableException.class )) {
            response.setStatus( HttpStatus.NOT_ACCEPTABLE.value() );
        } else if (exceptionClass.equals( HttpMediaTypeNotSupportedException.class )) {
            response.setStatus( HttpStatus.UNSUPPORTED_MEDIA_TYPE.value() );
        } else if (exceptionClass.equals( HttpRequestMethodNotSupportedException.class )) {
            response.setStatus( HttpStatus.METHOD_NOT_ALLOWED.value() );
        } else if (exceptionClass.equals( NoResourceFoundException.class )) {
            //找不到页面
            response.setStatus( HttpStatus.NOT_FOUND.value() );
            log.warn( ex.getMessage() );
        } else {
            response.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
            //500类异常，要打印到日志里。
            log.error( ex.getMessage(), ex );
        }
        //针对不同类型异常，设置不同的详细消息。
        if (response.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            detailData = MscUtils.exceptionToString( ex );
        } else {
            detailData = ex.getMessage();
        }
        if (mscActionLog != null) {
            mscActionLog.setException( detailData );
        }
        return ResponseData.error( detailData, String.valueOf( response.getStatus() ), ex.getMessage() );
    }

}
