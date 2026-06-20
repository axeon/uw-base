package uw.auth.service.filter;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import uw.auth.service.AuthServiceHelper;
import uw.auth.service.annotation.MscPermDeclare;
import uw.auth.service.conf.AuthServiceProperties;
import uw.auth.service.constant.ActionLog;
import uw.auth.service.constant.AuthServiceConstants;
import uw.auth.service.constant.AuthType;
import uw.auth.service.constant.UserType;
import uw.auth.service.exception.AuthExceptionHelper;
import uw.auth.service.log.AuthCriticalLogStorage;
import uw.auth.service.service.MscAuthPermService;
import uw.auth.service.token.AuthTokenData;
import uw.auth.service.util.IpWebUtils;
import uw.auth.service.util.logging.LoggingHttpServletRequestWrapper;
import uw.auth.service.util.logging.LoggingHttpServletResponseWrapper;
import uw.auth.service.vo.MscActionLog;
import uw.common.response.ResponseData;
import uw.common.util.IpMatchUtils;
import uw.common.util.JsonUtils;
import uw.common.util.SystemClock;
import uw.log.es.LogClient;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * 认证授权过滤器。
 * <p>
 * 请求入口处的核心组件，依次完成：
 * <ol>
 *   <li>IP 白名单保护（对 {@code /rpc/*}、{@code /agent/*} 等受保护路径校验来源 IP）</li>
 *   <li>解析 {@code @MscPermDeclare} 注解，校验 Token、判定用户类型与权限</li>
 *   <li>按注解的 {@link ActionLog} 级别创建并填充操作日志，请求结束后发送到 ES / 数据库</li>
 * </ol>
 * 鉴权基于「精确请求 URI + 请求方法」，不支持路径变量的权限匹配。
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
     * 异常处理器。
     */
    private final HandlerExceptionResolver exceptionResolver;
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


    public AuthServiceFilter(final AuthServiceProperties authServiceProperties, final RequestMappingHandlerMapping requestMappingHandlerMapping, HandlerExceptionResolver exceptionResolver,
                             final MscAuthPermService authPermService, final LogClient logClient, final AuthCriticalLogStorage authCriticalLogStorage) {
        this.authServiceProperties = authServiceProperties;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.exceptionResolver = exceptionResolver;
        this.authPermService = authPermService;
        this.logClient = logClient;
        this.authCriticalLogStorage = authCriticalLogStorage;
        // 初始化IP受保护路径
        if (StringUtils.isNotBlank(authServiceProperties.getIpProtectedPaths())) {
            ipProtectedPaths = authServiceProperties.getIpProtectedPaths().split(",");
            for (int i = 0; i < ipProtectedPaths.length; i++) {
                // 去除末尾的*，直接使用startWith判断降低损耗。
                ipProtectedPaths[i] = StringUtils.stripEnd(ipProtectedPaths[i].trim(), "*");
            }
            if (StringUtils.isNotBlank(authServiceProperties.getIpWhiteList())) {
                ipWhiteList = IpMatchUtils.sortList(authServiceProperties.getIpWhiteList().split(","));
            } else {
                // 配置了受保护路径但白名单为空，将导致内部高危路径（/rpc/*,/agent/*）无任何限制。
                logger.warn("ip-protected-paths 已配置但 ip-white-list 为空，将导致受保护路径无任何限制！请配置 ip-white-list。");
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
     * 过滤器主逻辑：执行 IP 保护、Token 校验、权限判定与操作日志记录。
     *
     * @param request  Servlet 请求
     * @param response Servlet 响应
     * @param chain    过滤器链
     * @throws IOException      IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
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
            if (Strings.CS.startsWithAny(uri, ipProtectedPaths) && !IpMatchUtils.matches(ipWhiteList, userIp)) {
                throwException(httpServletRequest, httpServletResponse, ResponseData.errorCode(AuthServiceConstants.HTTP_FORBIDDEN_CODE, "IP protected."));
                return;
            }
        }
        //整理权限信息
        MscPermDeclare mscPermDeclare = null;
        UserType permUserType = UserType.ANY;
        AuthType permAuthType = AuthType.NONE;
        ActionLog permLogType = ActionLog.NONE;
        //操作日志对象。
        MscActionLog mscActionLog = null;
        try {
            invokeCounter.increment();
            // 获取HandlerExecutionChain
            HandlerExecutionChain handlerExecutionChain = null;
            try {
                handlerExecutionChain = requestMappingHandlerMapping.getHandler((HttpServletRequest) request);
            } catch (Exception e) {
                logger.warn("Failed to get handler: {}", e.getMessage());
            }
            // web mvc处理链异常，直接放行到GlobalExceptionHandler处理。
            if (handlerExecutionChain == null) {
                chain.doFilter(request, response);
                return;
            }
            // 查询接口方法
            HandlerMethod handler = (HandlerMethod) handlerExecutionChain.getHandler();
            Method javaMethod = handler.getMethod();
            mscPermDeclare = javaMethod.getAnnotation(MscPermDeclare.class);
            // 有权限注解的，才检测权限。
            if (mscPermDeclare != null) {
                // 获取原始token
                String rawToken = httpServletRequest.getHeader(AuthServiceConstants.TOKEN_HEADER_PARAM);
                // 解析token
                ResponseData<AuthTokenData> authTokenDataResponse = AuthServiceHelper.parseRawToken(userIp, rawToken);
                // token异常直接抛出
                if (authTokenDataResponse.isNotSuccess()) {
                    throwException(httpServletRequest, httpServletResponse, authTokenDataResponse);
                    return;
                }
                // 取出tokenData
                AuthTokenData authTokenData = authTokenDataResponse.getData();
                // 鉴权开始。
                // 注意：权限控制基于精确请求URI + 请求方法，不支持路径变量（如 /user/{id}）的权限匹配。
                // 带路径变量的接口请勿使用 auth=PERM/SUDO，否则权限无法命中。
                String permCode = uri + ":" + method;
                // 权限鉴权
                ResponseData<?> authPermResponse = authPermService.hasPerm(authTokenData, mscPermDeclare, permCode);
                // 鉴权异常直接抛出
                if (authPermResponse.isNotSuccess()) {
                    throwException(httpServletRequest, httpServletResponse, authPermResponse);
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
                    mscActionLog.setRequestDate(SystemClock.nowDate());
                    if (permLogType == ActionLog.REQUEST || permLogType == ActionLog.ALL || permLogType == ActionLog.CRIT || permAuthType == AuthType.SUDO) {
                        request = new LoggingHttpServletRequestWrapper(httpServletRequest, authServiceProperties.getLogBodyCacheLimit());
                    }
                    if (permLogType == ActionLog.RESPONSE || permLogType == ActionLog.ALL || permLogType == ActionLog.CRIT || permAuthType == AuthType.SUDO) {
                        response = new LoggingHttpServletResponseWrapper((HttpServletResponse) response);
                    }
                    AuthServiceHelper.setContextLog(mscActionLog);
                }
            }
            // 执行后续Filter
            chain.doFilter(request, response);
        } finally {
            try {
                if (mscPermDeclare != null && mscActionLog != null) {
                    // 因为性能问题,返回大数据量时不建议记录RESPONSE
                    mscActionLog.setResponseMillis(SystemClock.now() - mscActionLog.getRequestDate().getTime());
                    mscActionLog.setStatusCode(((HttpServletResponse) response).getStatus());
                    try {
                        //保存request
                        if ((permLogType == ActionLog.REQUEST || permLogType == ActionLog.ALL || permLogType == ActionLog.CRIT) && request instanceof LoggingHttpServletRequestWrapper requestWrapper) {
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
                        if ((permLogType == ActionLog.RESPONSE || permLogType == ActionLog.ALL || permLogType == ActionLog.CRIT) && response instanceof LoggingHttpServletResponseWrapper responseWrapper) {
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
                }
            } finally {
                // 无条件清理ThreadLocal，防止线程复用导致跨请求用户身份/日志泄漏。
                AuthServiceHelper.destroyContextToken();
                AuthServiceHelper.destroyContextLog();
            }
        }
    }

    @Override
    public void destroy() {
        logger.info("Destroy AuthServiceFilter.");
    }

    /**
     * 将鉴权产生的 {@link ResponseData} 转换为异常并交由 {@link HandlerExceptionResolver} 处理。
     *
     * @param httpServletRequest  HTTP 请求
     * @param httpServletResponse HTTP 响应
     * @param responseData        鉴权响应数据
     */
    private void throwException(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, ResponseData<?> responseData) {
        RuntimeException ex = AuthExceptionHelper.convertException(responseData);
        if (ex != null) {
            exceptionResolver.resolveException(httpServletRequest, httpServletResponse, null, ex);
        }
    }
}
