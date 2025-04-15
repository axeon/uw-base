package uw.auth.service.advice;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import uw.auth.service.AuthServiceHelper;
import uw.auth.service.annotation.ResponseAdviceIgnore;
import uw.auth.service.vo.MscActionLog;
import uw.common.dto.ResponseData;
import uw.common.util.JsonUtils;

import java.util.LinkedHashMap;


/**
 * 全局数据包裹处理。
 * 对于返回的数据，全部使用ResponseData来进行包裹。
 */
@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {
    private static final Logger log = LoggerFactory.getLogger( GlobalResponseAdvice.class );

    public GlobalResponseAdvice() {
        log.info( "Init GlobalResponseAdvice." );
    }

    /**
     * 对于使用了ResponseAdviceIgnore注解的类和方法进行过滤。
     *
     * @param methodParameter
     * @param aClass
     * @return
     */
    @Override
    @SuppressWarnings("all")
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        Class<?> declaringClass = methodParameter.getDeclaringClass();
        // 检查注解是否存在
        if (declaringClass.isAnnotationPresent( ResponseAdviceIgnore.class )) {
            return false;
        }
        if (methodParameter.getMethod().isAnnotationPresent( ResponseAdviceIgnore.class )) {
            return false;
        }
        // 适配swagger的接口文档
        if (declaringClass.getPackageName().contains( "org.springdoc" )) {
            return false;
        }

        return true;
    }

    /**
     * 在结果输出前用ResponseData包裹。
     *
     * @param body
     * @param returnType
     * @param selectedContentType
     * @param selectedConverterType
     * @param request
     * @param response
     * @return
     */
    @Nullable
    @Override
    @SuppressWarnings("all")
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // body is null 特殊处理。
        if (body == null) {
            return logResponseData( ResponseData.warn() );
        }

        //单独提前处理responseData类型，减少不必要的判定。
        if (body instanceof ResponseData responseData) {
            logResponseData( responseData );
            //是否字符串类型要单独处理一下，否则会抛错。
            if (returnType.getParameterType().equals( String.class )) {
                return JsonUtils.toString( responseData );
            } else {
                return responseData;
            }
        }

        //需要处理额外未拦截到的系统报错信息。
        if (returnType.getParameterType().equals( ResponseEntity.class )) {
            if (body instanceof LinkedHashMap data) {
                String status = String.valueOf( data.get( "status" ) );
                int statusCode = 500;
                try {
                    statusCode = Integer.parseInt(status);
                } catch (NumberFormatException ignored) {
                }
                response.setStatusCode(HttpStatusCode.valueOf( statusCode));
                String code = "http.status." + statusCode ;
                String msg = "path: "+String.valueOf(data.get( "path" ))+", msg: " +String.valueOf( data.get( "message" ) );
                return logResponseData( ResponseData.errorCode( code, msg ) );
            }
        } else if (returnType.getParameterType().equals( String.class )) {
            //字符串类型需要单独包裹。
            return JsonUtils.toString( logResponseData( ResponseData.success( body ) ) );
        }
        //默认情况，直接返回。
        return logResponseData( ResponseData.success( body ) );
    }

    /**
     * 封装记录操作日志。
     *
     * @param responseData
     * @return
     */
    private ResponseData<?> logResponseData(ResponseData<?> responseData) {
        MscActionLog mscActionLog = AuthServiceHelper.getContextLog();
        if (mscActionLog != null) {
            mscActionLog.setResponseState( responseData.getState() );
            mscActionLog.setResponseCode( responseData.getCode() );
            mscActionLog.setResponseMsg( responseData.getMsg() );
        }
        return responseData;
    }

}
