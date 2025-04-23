package uw.auth.service.filter;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import uw.auth.service.AuthServiceHelper;
import uw.auth.service.annotation.MscPermDeclare;
import uw.auth.service.conf.AuthServiceProperties;
import uw.auth.service.constant.ActionLog;
import uw.auth.service.constant.AuthServiceConstants;
import uw.auth.service.constant.AuthType;
import uw.auth.service.constant.UserType;
import uw.auth.service.log.AuthCriticalLogStorage;
import uw.auth.service.service.MscAuthPermService;
import uw.auth.service.token.AuthTokenData;
import uw.auth.service.util.IpWebUtils;
import uw.auth.service.util.logging.LoggingHttpServletRequestWrapper;
import uw.auth.service.util.logging.LoggingHttpServletResponseWrapper;
import uw.auth.service.vo.MscActionLog;
import uw.common.dto.ResponseData;
import uw.common.util.IpMatchUtils;
import uw.common.util.JsonUtils;
import uw.common.util.SystemClock;
import uw.log.es.LogClient;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * AuthServiceFilter组件。
 *
 * @author axeon
 * @since 2018/2/6
 */
public class AuthServiceFilter implements Filter {

    /**
     * 请求计数器
     */
    public static final LongAdder invokeCounter = new LongAdder();
    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceFilter.class);
    /**
     * AuthService配置文件。
     */
    private final AuthServiceProperties authServiceProperties;
    /**
     * 请求映射处理器。
     */
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    /**
     * 权限服务。
     */
    private final MscAuthPermService authPermService;
    /**
     * 日志客户端。
     */
    private final LogClient logClient;
    /**
     * CRIT日志存储器。
     */
    private final AuthCriticalLogStorage authCriticalLogStorage;
    /**
     * 白名单列表
     */
    private List<IpMatchUtils.IpRange> ipWhiteList;

    /**
     * IP受保护路径
     */
    private String[] ipProtectedPaths;

    public AuthServiceFilter(final AuthServiceProperties authServiceProperties, final RequestMappingHandlerMapping requestMappingHandlerMapping,
                             final MscAuthPermService authPermService, final LogClient logClient, final AuthCriticalLogStorage authCriticalLogStorage) {
        this.authServiceProperties = authServiceProperties;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.authPermService = authPermService;
        this.logClient = logClient;
        this.authCriticalLogStorage = authCriticalLogStorage;
        // 初始化白名单列表
        if (StringUtils.isNotBlank(authServiceProperties.getIpWhiteList())) {
            ipWhiteList = IpMatchUtils.sortList(authServiceProperties.getIpWhiteList().split(","));
        }
        // 初始化IP受保护路径
        if (StringUtils.isNotBlank(authServiceProperties.getIpProtectedPaths())) {
            ipProtectedPaths = authServiceProperties.getIpProtectedPaths().split(",");
            for (int i = 0; i < ipProtectedPaths.length; i++) {
                // 去除末尾的*，直接使用startWith判断降低损耗。
                ipProtectedPaths[i] = StringUtils.stripEnd(ipProtectedPaths[i].trim(), "*");
            }
        }

    }

    /**
     * 初始化。
     *
     * @param filterConfig
     * @throws ServletException
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Init AuthServiceFilter.");
    }

    /**
     * 过滤器。
     *
     * @param request
     * @param response
     * @param chain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
        // 请求对象和响应对象
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        //请求uri
        String uri = httpServletRequest.getRequestURI();
        //请求方法
        String method = httpServletRequest.getMethod();
        //远端IP
        String userIp = IpWebUtils.getRealIp(httpServletRequest);
        //执行IP保护。
        if (ipProtectedPaths != null && ipWhiteList != null) {
            for (String ipProtectedPath : ipProtectedPaths) {
                if (uri.startsWith(ipProtectedPath)) {
                    if (!IpMatchUtils.matches(ipWhiteList, userIp)) {
                        httpServletResponse.sendError(HttpStatus.FORBIDDEN.value(), "IP protected.");
                    } else {
                        break;
                    }
                }
            }
        }

        //操作日志
        MscActionLog mscActionLog = null;
        //整理权限信息
        UserType permUserType = UserType.ANY;
        AuthType permAuthType = AuthType.NONE;
        ActionLog permLogType = ActionLog.NONE;
        try {
            invokeCounter.increment();
            // 获取原始token
            String rawToken = httpServletRequest.getHeader(AuthServiceConstants.TOKEN_HEADER_PARAM);
            // 解析token
            ResponseData<AuthTokenData> authTokenDataResponse = AuthServiceHelper.parseRawToken(userIp, rawToken);
            // token异常直接抛出
            if (authTokenDataResponse.isNotSuccess()) {
                httpServletResponse.sendError(Integer.parseInt(authTokenDataResponse.getCode()), authTokenDataResponse.getMsg());
                return;
            }
            // 取出tokenData
            AuthTokenData authTokenData = authTokenDataResponse.getData();
            // 获取HandlerExecutionChain
            HandlerExecutionChain handlerExecutionChain = null;
            try {
                handlerExecutionChain = requestMappingHandlerMapping.getHandler((HttpServletRequest) request);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            // 404
            if (handlerExecutionChain == null) {
                chain.doFilter(request, response);
                return;
            }

            // 设定当前线程tokenData
            AuthServiceHelper.setContextToken(authTokenData);
            // 查询接口方法
            HandlerMethod handler = (HandlerMethod) handlerExecutionChain.getHandler();
            Method javaMethod = handler.getMethod();
            MscPermDeclare mscPermDeclare = javaMethod.getAnnotation(MscPermDeclare.class);
            // 鉴权开始
            String permCode = uri + ":" + method;
            // 权限鉴权
            ResponseData authPermResponse = authPermService.hasPerm(authTokenData, mscPermDeclare, permCode);
            // 鉴权异常直接抛出
            if (authPermResponse.isNotSuccess()) {
                httpServletResponse.sendError(Integer.parseInt(authPermResponse.getCode()), authPermResponse.getMsg());
                return;
            }
            //准备操作日志内容，关键日志必须记录，并记录操作人员日志。
            permUserType = mscPermDeclare.user();
            permAuthType = mscPermDeclare.auth();
            permLogType = mscPermDeclare.log();
            if (permLogType.getValue() == ActionLog.CRIT.getValue() || (permLogType.getValue() > ActionLog.NONE.getValue() && permUserType.getValue() > UserType.RPC.getValue())) {
                //设定操作名称，mscPermDeclare有优先级
                String apiName = mscPermDeclare.name();
                if (StringUtils.isBlank(apiName)) {
                    Operation operation = javaMethod.getAnnotation(Operation.class);
                    if (operation != null) {
                        apiName = operation.summary();
                    }
                }
                mscActionLog = new MscActionLog();
                mscActionLog.setAppInfo(authServiceProperties.getAppName() + ":" + authServiceProperties.getAppVersion());
                mscActionLog.setAppHost(authServiceProperties.getAppHost() + ":" + authServiceProperties.getAppPort());
                mscActionLog.setLogLevel(permLogType.getValue());
                mscActionLog.setUserId(authTokenData.getUserId());
                mscActionLog.setUserName(authTokenData.getUserName());
                mscActionLog.setNickName(authTokenData.getNickName());
                mscActionLog.setRealName(authTokenData.getRealName());
                mscActionLog.setSaasId(authTokenData.getSaasId());
                mscActionLog.setMchId(authTokenData.getMchId());
                mscActionLog.setGroupId(authTokenData.getGroupId());
                mscActionLog.setUserType(authTokenData.getUserType());
                mscActionLog.setApiUri(permCode);
                mscActionLog.setApiName(apiName);
                mscActionLog.setUserIp(userIp);
                mscActionLog.setRequestDate(new Date());
                if (permLogType == ActionLog.REQUEST || permLogType == ActionLog.ALL || permLogType == ActionLog.CRIT || permAuthType == AuthType.SUDO) {
                    request = new LoggingHttpServletRequestWrapper(httpServletRequest);
                }
                if (permLogType == ActionLog.RESPONSE || permLogType == ActionLog.ALL || permLogType == ActionLog.CRIT || permAuthType == AuthType.SUDO) {
                    response = new LoggingHttpServletResponseWrapper((HttpServletResponse) response);
                }
                AuthServiceHelper.setContextLog(mscActionLog);
            }
            // 执行后续Filter
            chain.doFilter(request, response);
        } catch (Throwable t) {
            httpServletResponse.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), t.getMessage());
        } finally {
            AuthServiceHelper.destroyContextToken();
            if (mscActionLog != null) {
                // 因为性能问题,返回大数据量时不建议记录RESPONSE
                mscActionLog.setResponseMillis(SystemClock.now() - mscActionLog.getRequestDate().getTime());
                mscActionLog.setStatusCode(((HttpServletResponse) response).getStatus());
                try {
                    //保存request
                    if (permLogType == ActionLog.REQUEST || permLogType == ActionLog.ALL || permLogType == ActionLog.CRIT) {
                        LoggingHttpServletRequestWrapper requestWrapper = (LoggingHttpServletRequestWrapper) request;
                        StringBuilder sb = new StringBuilder(1280);
                        sb.append("{");
                        Map<String, String[]> requestParamMap = requestWrapper.getParameterMap();
                        if (!requestParamMap.isEmpty()) {
                            sb.append("\"param\":").append(JsonUtils.toString(requestParamMap)).append(",");
                        }
                        byte[] requestContentBytes = requestWrapper.getContentAsByteArray();
                        if (requestContentBytes.length > 0) {
                            sb.append("\"body\":").append(new String(requestContentBytes, StandardCharsets.UTF_8));
                        }
                        if (sb.length() > 1) {
                            if (sb.charAt(sb.length() - 1) == ',') {
                                sb.deleteCharAt(sb.length() - 1);
                            }
                            sb.append("}");
                            mscActionLog.setRequestBody(sb.toString());
                        }
                    }
                    //保存response
                    if (permLogType == ActionLog.RESPONSE || permLogType == ActionLog.ALL || permLogType == ActionLog.CRIT) {
                        LoggingHttpServletResponseWrapper responseWrapper = (LoggingHttpServletResponseWrapper) response;
                        mscActionLog.setResponseBody(new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8));
                        responseWrapper.copyBodyToResponse();
                    }
                    //如果是crit数据，保存数据库。
                    if (permLogType == ActionLog.CRIT || permAuthType == AuthType.SUDO) {
                        authCriticalLogStorage.save(mscActionLog);
                    }
                    //发送到es
                    logClient.log(mscActionLog);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                AuthServiceHelper.destroyContextLog();
            }
        }
    }


    @Override
    public void destroy() {
        logger.info("Destroy AuthServiceFilter.");
    }
}
