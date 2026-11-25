package uw.auth.service.advice;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.HtmlUtils;
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
 * <p>
 * 响应体为 {@code ResponseData} 时原样透传，但有一处例外：uw-dao 查无数据（{@code warn} + {@code uw.dao.DATA_NOT_FOUND_WARN}）
 * 时把 HTTP 状态码改为 404——否则探测一个不存在的资源与正常请求在状态码上无法区分（都是 200）。
 * 该状态码只改状态行，响应体不变；标了 {@link ResponseAdviceIgnore} 的接口不经过这里，状态码不变。
 * <p>
 * <b>服务间 RPC 接口除外</b>（路径含 {@code /rpc/}）：RPC 调用方（{@code RestClient}）对 4xx 抛异常，拿到的不再是
 * {@code ResponseData}，而不少 RPC 接口把查无数据的结果直接返回（如按 ID 取用户、取运营商）。所以 RPC 接口保持 200 + warn，
 * 只有网关/前端面对的接口改为 404。
 *
 * @author axeon
 */
@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {
    private static final Logger log = LoggerFactory.getLogger(GlobalResponseAdvice.class);

    /**
     * uw-dao 查无数据时 {@code ResponseData.code} 的取值。
     * <p>
     * 即 {@code uw.dao.constant.DaoResponseCode.DATA_NOT_FOUND_WARN} 的完整响应码：枚举名按下划线拆成点并转小写
     * （{@code ResponseCode.code()}），再加前缀 {@code uw.dao}——注意不是枚举名的原样大写形式。
     * uw-auth-service 不依赖 uw-dao，这里按字符串比较，不为这一个判断引入模块依赖。
     */
    private static final String DAO_DATA_NOT_FOUND_WARN = "uw.dao.data.not.found.warn";

    /**
     * 服务间 RPC 接口的路径片段（如 {@code /rpc/service/loadUser}，前面可带 servlet 上下文路径）。
     */
    private static final String RPC_PATH_SEGMENT = "/rpc/";

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
     * null → {@code warn()}；已是 ResponseData → 透传（uw-dao 查无数据时状态码改为 404，RPC 接口除外）；ResponseEntity 错误体 → 转为 errorCode；
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
            if (responseData.isWarn() && DAO_DATA_NOT_FOUND_WARN.equals(responseData.getCode()) && !isRpcRequest(request)) {
                // 目标数据不存在：状态码改 404（读写的查无数据同属此类），响应体原样返回。
                // 不记 ERROR/WARN——查无数据是正常业务结果，MscActionLog 照常记录 state/code
                response.setStatusCode(HttpStatus.NOT_FOUND);
            }
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
     * 是否服务间 RPC 接口的请求：路径含 {@code /rpc/} 片段（兼容带 servlet 上下文路径的部署）。
     *
     * @param request 服务端请求
     * @return 是 RPC 接口返回 true
     */
    private static boolean isRpcRequest(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        return path != null && path.contains(RPC_PATH_SEGMENT);
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
