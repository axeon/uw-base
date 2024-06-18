package uw.auth.service.advice;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import uw.auth.service.annotation.ResponseAdviceIgnore;
import uw.common.dto.ResponseData;
import uw.httpclient.json.JsonInterfaceHelper;
import uw.httpclient.exception.DataMapperException;

import java.util.LinkedHashMap;


/**
 * 全局数据包裹处理。
 * 对于返回的数据，全部使用ResponseData来进行包裹。
 */
@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {
    private static final Logger log = LoggerFactory.getLogger(GlobalResponseAdvice.class);

    private static final String HTTP_OK = "200";

    public GlobalResponseAdvice() {
        log.info("Initializing GlobalResponseAdvice...");
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
    public boolean supports(MethodParameter methodParameter,
                            Class<? extends HttpMessageConverter<?>> aClass) {
        Class<?> declaringClass = methodParameter.getDeclaringClass();
        // 检查注解是否存在
        if (methodParameter.getDeclaringClass().isAnnotationPresent(ResponseAdviceIgnore.class)) {
            return false;
        }
        if (methodParameter.getMethod().isAnnotationPresent(ResponseAdviceIgnore.class)) {
            return false;
        }
        // 适配swagger的接口文档
        if (declaringClass.getPackageName().contains("org.springdoc")) {
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
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        // body is null -> return response
        if (body == null) {
            return ResponseData.success(null, HTTP_OK, "");
        }

        //已经封装过的返回信息直接返回
        if (body instanceof ResponseData) {
            return body;
        }

        //需要处理额外未拦截到的系统报错信息。
        if (returnType.getParameterType().equals(ResponseEntity.class)) {
            if (body instanceof LinkedHashMap data) {
                return ResponseData.error(body, String.valueOf(data.get("status")), String.valueOf(data.get("message")));
            }
        }

        // string 特殊处理
        if (body instanceof String) {
            try {
                return JsonInterfaceHelper.JSON_CONVERTER.toString(ResponseData.success(body, HTTP_OK, ""));
            } catch (DataMapperException e) {
                return body;
            }
        }

        return ResponseData.success(body);
    }

}
