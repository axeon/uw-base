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
import uw.auth.service.annotation.RateLimitDeclare;
import uw.auth.service.conf.AuthServiceProperties;
import uw.auth.service.constant.ActionLog;
import uw.auth.service.constant.AuthConstants;
import uw.auth.service.constant.UserType;
import uw.auth.service.exception.TokenExpiredException;
import uw.auth.service.exception.TokenInvalidateException;
import uw.auth.service.ipblock.IpMatchUtils;
import uw.auth.service.log.AuthCriticalLogStorage;
import uw.auth.service.ratelimit.MscRateLimiter;
import uw.auth.service.ratelimit.RateLimitInfo;
import uw.auth.service.ratelimit.RateLimitUtils;
import uw.auth.service.service.AuthPermService;
import uw.auth.service.token.AuthTokenData;
import uw.auth.service.util.IPAddressUtils;
import uw.auth.service.util.logging.LoggingHttpServletRequestWrapper;
import uw.auth.service.util.logging.LoggingHttpServletResponseWrapper;
import uw.auth.service.vo.MscActionLog;
import uw.httpclient.json.JsonInterfaceHelper;
import uw.log.es.LogClient;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.concurrent.atomic.LongAdder;

/**
 * AuthServiceFilter组件。
 *
 * @author axeon
 * @since 2018/2/6
 */
public class AuthServiceFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger( AuthServiceFilter.class );
    public static LongAdder invokeCounter = new LongAdder();
    private final AuthServiceProperties authServerProperties;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final AuthPermService authPermService;
    private final LogClient logClient;
    private final AuthServiceHelper authServiceHelper;
    private final MscRateLimiter mscRateLimiter;
    private final AuthCriticalLogStorage authCriticalLogStorage;

    public AuthServiceFilter(final AuthServiceProperties authServerProperties, final RequestMappingHandlerMapping requestMappingHandlerMapping, final AuthPermService authPermService,
                             final MscRateLimiter mscRateLimiter, final LogClient logClient, final AuthCriticalLogStorage authCriticalLogStorage,
                             final AuthServiceHelper authServiceHelper) {
        this.authServerProperties = authServerProperties;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.authPermService = authPermService;
        this.mscRateLimiter = mscRateLimiter;
        this.logClient = logClient;
        this.authServiceHelper = authServiceHelper;
        this.authCriticalLogStorage = authCriticalLogStorage;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info( "Init AuthService filter." );
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        //请求uri
        String uri = httpServletRequest.getRequestURI();
        //请求方法
        String method = httpServletRequest.getMethod();
        //远端IP
        String remoteIp = IPAddressUtils.getTrueIp( httpServletRequest );

        MscActionLog mscActionLog = null;

        ActionLog logType = ActionLog.NONE;

        try {
            // 取得上下文用户对象
            String rawToken = httpServletRequest.getHeader( AuthConstants.TOKEN_HEADER_PARAM );
            AuthTokenData authToken = authServiceHelper.parseRawToken( remoteIp, rawToken );
            invokeCounter.increment();
            HandlerExecutionChain handlerExecutionChain = null;
            try {
                handlerExecutionChain = requestMappingHandlerMapping.getHandler( (HttpServletRequest) request );
            } catch (Exception e) {
                logger.error( e.getMessage(), e );
            }
            // 404
            if (handlerExecutionChain == null) {
                chain.doFilter( request, response );
                return;
            }
            // 设定当前线程专有对象
            AuthServiceHelper.setContextToken( authToken );

            // 查询接口方法
            HandlerMethod handler = (HandlerMethod) handlerExecutionChain.getHandler();
            Method javaMethod = handler.getMethod();
            MscPermDeclare mscPermDeclare = javaMethod.getAnnotation( MscPermDeclare.class );
            RateLimitDeclare rateLimitDeclare = javaMethod.getAnnotation( RateLimitDeclare.class );

            // 鉴权开始
            String permCode = uri + ":" + method;
            if (authPermService.hasPerm( authToken, mscPermDeclare, permCode )) {
                //rpc不检查ip和限速
                if (authToken.getUserType() > UserType.RPC.getValue()) {
                    //此处开始检测ip block。
                    int filterType = authToken.getTokenPerm().getIpFilterType();
                    if (filterType != 0) {
                        boolean flag = IpMatchUtils.matches( authToken.getTokenPerm().getIpRanges(), remoteIp );
                        if (filterType == -1) {
                            //黑名单模式取反
                            flag = !flag;
                        }
                        //如果无法匹配，则输出相关信息。
                        if (!flag) {
                            httpServletResponse.sendError( HttpStatus.NETWORK_AUTHENTICATION_REQUIRED.value(), "!!!IP BLOCK!!!" );
                            return;
                        }
                    }
                    //检查限速
                    RateLimitInfo rateLimitInfo = RateLimitUtils.match( authToken, rateLimitDeclare, remoteIp, authServerProperties.getAppName() + uri );
                    //执行限速。
                    if (rateLimitInfo != null) {
                        //检测是否满足用户总限定。
                        int[] limitResult = mscRateLimiter.tryAcquire( rateLimitInfo );
                        //输出限速信息
                        httpServletResponse.addHeader( "X-RateLimit-Info", rateLimitInfo.toString() );
                        if (limitResult != null && limitResult.length == 2) {
                            if (limitResult[0] > 0) {
                                //输出剩余许可
                                httpServletResponse.addHeader( "X-RateLimit-RemainPermits", String.valueOf( limitResult[0] ) );
                            }
                            if (limitResult[1] > 0) {
                                //输出等待毫秒数。
                                httpServletResponse.addHeader( "X-RateLimit-WaitMillis", String.valueOf( limitResult[1] ) );
                                httpServletResponse.addHeader( "Retry-After", String.valueOf( Math.floor( limitResult[1] / 1000 ) ) );
                                //强制让其等待
                                try {
                                    Thread.sleep( limitResult[1] );
                                } catch (InterruptedException e) {
                                }
                            }
                        }

                        if (limitResult[0] < 0) {
                            //此处返回429 Too Many Requests
                            httpServletResponse.sendError( HttpStatus.TOO_MANY_REQUESTS.value(), "!!!RATE LIMIT!!!" );
                            return;
                        }
                    }
                }
                //准备操作日志内容，关键日志必须记录，并记录操作人员日志。
                logType = mscPermDeclare.log();
                if (logType.getValue() == ActionLog.CRIT.getValue() || (logType.getValue() > ActionLog.NONE.getValue() && mscPermDeclare.type().getValue() > UserType.RPC.getValue())) {
                    //设定操作名称，mscPermDeclare有优先级
                    String apiName = mscPermDeclare.name();
                    if (StringUtils.isBlank( apiName )) {
                        Operation operation = javaMethod.getAnnotation( Operation.class );
                        if (operation != null) {
                            apiName = operation.summary();
                        }
                    }
                    mscActionLog = new MscActionLog();
                    mscActionLog.setAppInfo( authServerProperties.getAppName() + ":" + authServerProperties.getAppVersion() );
                    mscActionLog.setAppHost( authServerProperties.getAppHost() + ":" + authServerProperties.getAppPort() );
                    mscActionLog.setLogLevel( logType.getValue() );
                    mscActionLog.setUserId( authToken.getUserId() );
                    mscActionLog.setUserName( authToken.getUserName() );
                    mscActionLog.setNickName( authToken.getNickName() );
                    mscActionLog.setRealName( authToken.getRealName() );
                    mscActionLog.setSaasId( authToken.getSaasId() );
                    mscActionLog.setMchId( authToken.getMchId() );
                    mscActionLog.setGroupId( authToken.getGroupId() );
                    mscActionLog.setUserType( authToken.getUserType() );
                    mscActionLog.setApiUri( permCode );
                    mscActionLog.setApiName( apiName );
                    mscActionLog.setUserIp( remoteIp );
                    mscActionLog.setRequestDate( new Date() );
                    if (logType == ActionLog.REQUEST || logType == ActionLog.ALL || logType == ActionLog.CRIT) {
                        request = new LoggingHttpServletRequestWrapper( httpServletRequest );
                    }
                    if (logType == ActionLog.RESPONSE || logType == ActionLog.ALL || logType == ActionLog.CRIT) {
                        response = new LoggingHttpServletResponseWrapper( (HttpServletResponse) response );
                    }
                    AuthServiceHelper.setContextLog( mscActionLog );
                }
                // 执行后续Filter
                chain.doFilter( request, response );
                return;
            }
            // 没有权限访问
            httpServletResponse.sendError( HttpStatus.FORBIDDEN.value(), "!!!No Permission!!!" );
        } catch (TokenInvalidateException e) {// Token无效，此时需要重新登录。
            httpServletResponse.sendError( HttpStatus.UNAUTHORIZED.value(), e.getMessage() );
        } catch (TokenExpiredException e) {// Token过期，此时需要刷新Token。
            httpServletResponse.sendError( HttpStatus.LOCKED.value(), e.getMessage() );
        } finally {
            AuthServiceHelper.destroyContextToken();
            if (mscActionLog != null) {
                // 因为性能问题,返回大数据量时不建议记录RESPONSE
                mscActionLog.setResponseMillis( System.currentTimeMillis() - mscActionLog.getRequestDate().getTime() );
                mscActionLog.setStatusCode( ((HttpServletResponse) response).getStatus() );
                try {
                    //保存request
                    if (logType == ActionLog.REQUEST || logType == ActionLog.ALL || logType == ActionLog.CRIT) {
                        LoggingHttpServletRequestWrapper requestWrapper = (LoggingHttpServletRequestWrapper) request;
                        StringBuilder sb = new StringBuilder( 2560 );
                        sb.append( "{" );
                        if (!requestWrapper.getParameterMap().isEmpty()) {
                            sb.append( "\"param\":" ).append( JsonInterfaceHelper.JSON_CONVERTER.toString( requestWrapper.getParameterMap() ) ).append( "," );
                        }
                        if (requestWrapper.getContentAsByteArray() != null && requestWrapper.getContentAsByteArray().length > 0) {
                            sb.append( "\"body\":" ).append( new StringBuilder( new String( requestWrapper.getContentAsByteArray(), requestWrapper.getCharacterEncoding() ) ) );
                        }
                        if (sb.length() > 1) {
                            if (sb.charAt( sb.length() - 1 ) == ',') {
                                sb.deleteCharAt( sb.length() - 1 );
                            }
                            sb.append( "}" );
                            mscActionLog.setRequestBody( sb.toString() );
                        }
                    }
                    //保存response
                    if (logType == ActionLog.RESPONSE || logType == ActionLog.ALL || logType == ActionLog.CRIT) {
                        LoggingHttpServletResponseWrapper responseWrapper = (LoggingHttpServletResponseWrapper) response;
                        mscActionLog.setResponseBody( new String( responseWrapper.getContentAsByteArray() ) );
                        responseWrapper.copyBodyToResponse();
                    }
                    //如果是crit数据，保存数据库。
                    if (logType == ActionLog.CRIT) {
                        authCriticalLogStorage.save( mscActionLog );
                    }
                    //发送到es
                    logClient.log( mscActionLog );
                } catch (Exception e) {
                    logger.error( e.getMessage(), e );
                }
                AuthServiceHelper.destroyContextLog();
            }
        }
    }


    @Override
    public void destroy() {
        logger.info( "Destroy AuthService filter." );
    }
}
