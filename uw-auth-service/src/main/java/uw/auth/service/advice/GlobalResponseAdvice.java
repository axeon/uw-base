package uw.auth.service.advice;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.util.HtmlUtils;
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
import uw.common.response.ResponseData;
import uw.common.util.JsonUtils;

import java.util.Map;

/**
 * 全局响应包裹处理器。
 * <p>
 * 将所有 Controller 返回值统一包裹为 {@code ResponseData}，开发者无需手动包装。
 * 同时将响应状态写入当前请求的 {@code MscActionLog}，配合 {@code AuthServiceFilter} 完成操作日志记录。
 * 可通过 {@link ResponseAdviceIgnore} 跳过包裹。
 *
 * @author axeon
 */
@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {
    private static final Logger log = LoggerFactory.getLogger(GlobalResponseAdvice.class);

    public GlobalResponseAdvice() {
        log.info("Init GlobalResponseAdvice.");
    }

    /**
     * 决定是否对当前返回值进行包裹。
     * <p>
     * 标注 {@link ResponseAdviceIgnore} 的类/方法、以及 springdoc 接口文档返回值不包裹。
     *
     * @param methodParameter 返回类型描述
     * @param aClass          选中的消息转换器
     * @return true 表示需要包裹
     */
    @Override
    @SuppressWarnings("all")
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        Class<?> declaringClass = methodParameter.getDeclaringClass();
        // 检查注解是否存在
        if (declaringClass.isAnnotationPresent(ResponseAdviceIgnore.class)) {
            return false;
        }
        if (methodParameter.getMethod().isAnnotationPresent(ResponseAdviceIgnore.class)) {
            return false;
        }
        // 适配swagger的接口文档
        if (declaringClass.getPackageName().startsWith("org.springdoc")) {
            return false;
        }

        return true;
    }

    /**
     * 在响应输出前用 {@code ResponseData} 包裹。
     * <p>
     * null → {@code warn()}；已是 ResponseData → 透传；ResponseEntity 错误体 → 转为 errorCode；
     * 其它对象 → {@code success(body)}。String 返回值会序列化为 JSON 字符串以兼容 Spring MVC。
     *
     * @param body               原始响应体
     * @param returnType         返回类型
     * @param selectedContentType 选中的内容类型
     * @param selectedConverterType 选中的转换器
     * @param request            服务端请求
     * @param response           服务端响应
     * @return 包裹后的响应体
     */
    @Nullable
    @Override
    @SuppressWarnings("all")
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {

        // body is null 特殊处理。
        if (body == null) {
            return logResponseData(returnType, ResponseData.warn());
        }

        //单独提前处理responseData类型，减少不必要的判定。
        if (body instanceof ResponseData responseData) {
            return logResponseData(returnType, responseData);
        }
        //需要处理额外未拦截到的系统报错信息。
        if (returnType.getParameterType().equals(ResponseEntity.class)) {
            if (body instanceof Map<?, ?> data) {
                Object statusObj = data.get("status");
                int statusCode = 500;
                if (statusObj instanceof Number num) {
                    statusCode = num.intValue();
                } else if (statusObj != null) {
                    try {
                        statusCode = Integer.parseInt(String.valueOf(statusObj));
                    } catch (NumberFormatException ignored) {
                    }
                }
                try {
                    response.setStatusCode(HttpStatusCode.valueOf(statusCode));
                } catch (IllegalArgumentException ex) {
                    //非法状态码回退为500，避免覆盖原始异常。
                    response.setStatusCode(HttpStatusCode.valueOf(500));
                    statusCode = 500;
                }
                String code = "http.status." + statusCode;
                Object pathObj = data.get("path");
                Object msgObj = data.get("message");
                String path = pathObj == null ? "" : HtmlUtils.htmlEscape(String.valueOf(pathObj));
                String message = msgObj == null ? "" : HtmlUtils.htmlEscape(String.valueOf(msgObj));
                String msg = "RequestPath: [" + path + "], Msg: " + message;
                return logResponseData(returnType, ResponseData.errorCode(code, msg));
            }
        }
        //默认情况，直接返回。
        return logResponseData(returnType, ResponseData.success(body));
    }

    /**
     * 将响应状态写入当前请求操作日志，并按返回类型决定输出形式。
     *
     * @param returnType    返回类型
     * @param responseData  已包裹的响应数据
     * @return String 类型返回 JSON 字符串，否则返回 responseData
     */
    private Object logResponseData(MethodParameter returnType, ResponseData<?> responseData) {
        MscActionLog mscActionLog = AuthServiceHelper.getContextLog();
        if (mscActionLog != null) {
            mscActionLog.setResponseState(responseData.getState());
            mscActionLog.setResponseCode(responseData.getCode());
            mscActionLog.setResponseMsg(responseData.getMsg());
        }
        if (returnType.getParameterType().equals(String.class)) {
            return JsonUtils.toString(responseData);
        } else {
            return responseData;
        }
    }

}
