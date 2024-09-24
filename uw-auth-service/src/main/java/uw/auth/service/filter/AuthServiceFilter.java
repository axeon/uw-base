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
import uw.auth.service.constant.AuthConstants;
import uw.auth.service.constant.UserType;
import uw.auth.service.exception.TokenExpiredException;
import uw.auth.service.exception.TokenInvalidateException;
import uw.auth.service.log.AuthCriticalLogStorage;
import uw.auth.service.service.AuthPermService;
import uw.auth.service.token.AuthTokenData;
import uw.auth.service.util.IpWebUtils;
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
    private final AuthCriticalLogStorage authCriticalLogStorage;

    public AuthServiceFilter(final AuthServiceProperties authServerProperties, final RequestMappingHandlerMapping requestMappingHandlerMapping,
                             final AuthPermService authPermService, final LogClient logClient, final AuthCriticalLogStorage authCriticalLogStorage) {
        this.authServerProperties = authServerProperties;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.authPermService = authPermService;
        this.logClient = logClient;
        this.authCriticalLogStorage = authCriticalLogStorage;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info( "Init AuthServiceFilter." );
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
        String remoteIp = IpWebUtils.getTrueIp( httpServletRequest );

        MscActionLog mscActionLog = null;

        ActionLog logType = ActionLog.NONE;

        try {
            // 取得上下文用户对象
            String rawToken = httpServletRequest.getHeader( AuthConstants.TOKEN_HEADER_PARAM );
            AuthTokenData authToken = AuthServiceHelper.parseRawToken( remoteIp, rawToken );
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

            // 鉴权开始
            String permCode = uri + ":" + method;
            if (authPermService.hasPerm( authToken, mscPermDeclare, permCode )) {
                //准备操作日志内容，关键日志必须记录，并记录操作人员日志。
                logType = mscPermDeclare.log();
                if (logType.getValue() == ActionLog.CRIT.getValue() || (logType.getValue() > ActionLog.NONE.getValue() && mscPermDeclare.user().getValue() > UserType.RPC.getValue())) {
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
        logger.info( "Destroy AuthServiceFilter." );
    }
}
