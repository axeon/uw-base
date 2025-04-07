package uw.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uw.auth.service.conf.AuthServiceProperties;
import uw.auth.service.constant.*;
import uw.auth.service.rpc.AuthServiceRpc;
import uw.auth.service.service.AuthPermService;
import uw.auth.service.token.AuthTokenData;
import uw.auth.service.token.InvalidTokenData;
import uw.auth.service.util.IpWebUtils;
import uw.auth.service.vo.MscActionLog;
import uw.common.dto.ResponseData;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 登录工具类.
 *
 * @author axeon
 */
public class AuthServiceHelper {

    private static final Logger logger = LoggerFactory.getLogger( AuthServiceHelper.class );

    /**
     * access token的用户类型分隔符。
     */
    private static final char TOKEN_TYPE_SEPARATOR = '$';

    /**
     * InheritableThreadLocal 保存当前线程请求用户对象
     */
    private static final ThreadLocal<AuthTokenData> contextTokenHolder = new InheritableThreadLocal<AuthTokenData>();

    /**
     * InheritableThreadLocal 保存当前线程请求日志对象
     */
    private static final ThreadLocal<MscActionLog> contextLogHolder = new InheritableThreadLocal<MscActionLog>();

    /**
     * 针对token有效期，每个单独设定过期时间。
     */
    private static final Expiry<String, AuthTokenData> cacheExpiryPolicy = new Expiry<>() {
        @Override
        public long expireAfterCreate(String key, AuthTokenData authToken, long currentTime) {
            return TimeUnit.MILLISECONDS.toNanos( authToken.getExpireAt() - System.currentTimeMillis() );
        }

        @Override
        public long expireAfterUpdate(String key, AuthTokenData authToken, long currentTime, long currentDuration) {
            return TimeUnit.MILLISECONDS.toNanos( authToken.getExpireAt() - System.currentTimeMillis() );
        }

        @Override
        public long expireAfterRead(String key, AuthTokenData authToken, long currentTime, long currentDuration) {
            return currentDuration;
        }
    };

    /**
     * RPC用户
     */
    private static Cache<String, AuthTokenData> userRpcCache;
    /**
     * ROOT用户
     */
    private static Cache<String, AuthTokenData> userRootCache;
    /**
     * devops用户
     */
    private static Cache<String, AuthTokenData> userOpsCache;
    /**
     * Admin用户
     */
    private static Cache<String, AuthTokenData> userAdminCache;
    /**
     * 300-运营商用户
     */
    private static Cache<String, AuthTokenData> userSaasCache;
    /**
     * guest-C站用户类型
     */
    private static Cache<String, AuthTokenData> userGuestCache;
    /**
     * 非法token
     */
    private static Cache<String, String> invalidTokenCache;
    /**
     * authServer rpc。
     */
    private static AuthServiceRpc authServiceRpc;
    /**
     * AuthServiceProperties
     */
    private static AuthServiceProperties authServiceProperties;
    /**
     * authPermService。
     */
    private static AuthPermService authPermService;

    public AuthServiceHelper(final AuthServiceProperties authServiceProperties, final AuthPermService authPermService, final AuthServiceRpc authServiceRpc) {
        AuthServiceHelper.authServiceProperties = authServiceProperties;
        AuthServiceHelper.authPermService = authPermService;
        AuthServiceHelper.authServiceRpc = authServiceRpc;
        Map<Integer, Long> userCacheConfig = authServiceProperties.getTokenCache();
        userRpcCache = Caffeine.newBuilder().maximumSize( userCacheConfig.getOrDefault( UserType.RPC.getValue(), 100L ) ).expireAfter( cacheExpiryPolicy ).build();
        userRootCache = Caffeine.newBuilder().maximumSize( userCacheConfig.getOrDefault( UserType.ROOT.getValue(), 100L ) ).expireAfter( cacheExpiryPolicy ).build();
        userOpsCache = Caffeine.newBuilder().maximumSize( userCacheConfig.getOrDefault( UserType.OPS.getValue(), 100L ) ).expireAfter( cacheExpiryPolicy ).build();
        userAdminCache = Caffeine.newBuilder().maximumSize( userCacheConfig.getOrDefault( UserType.ADMIN.getValue(), 1000L ) ).expireAfter( cacheExpiryPolicy ).build();
        userSaasCache = Caffeine.newBuilder().maximumSize( userCacheConfig.getOrDefault( UserType.SAAS.getValue(), 10_000L ) ).expireAfter( cacheExpiryPolicy ).build();
        userGuestCache = Caffeine.newBuilder().maximumSize( userCacheConfig.getOrDefault( UserType.GUEST.getValue(), 100_000L ) ).expireAfter( cacheExpiryPolicy ).build();
        invalidTokenCache = Caffeine.newBuilder().maximumSize( 10_000L ).expireAfterWrite( 20, TimeUnit.MINUTES ).build();
    }

    /**
     * 获得当前appId。
     *
     * @return
     */
    public static long getAppId() {
        return authServiceProperties.getAppId();
    }

    /**
     * 获得当前appPermMap。
     *
     * @return
     */
    public static Map<String, Integer> getAppPermMap() {
        return authPermService.getAppPermMap();
    }

    /**
     * 获得当前appLabel。
     *
     * @return
     */
    public static String getAppLabel() {
        return authServiceProperties.getAppLabel();
    }

    /**
     * 获得当前appName。
     *
     * @return
     */
    public static String getAppName() {
        return authServiceProperties.getAppName();
    }

    /**
     * 获得当前appVersion。
     *
     * @return
     */
    public static String getAppVersion() {
        return authServiceProperties.getAppVersion();
    }

    /**
     * 获得当前appHost。
     *
     * @return
     */
    public static String getAppHost() {
        return authServiceProperties.getAppHost();
    }

    /**
     * 获得当前appPort。
     *
     * @return
     */
    public static int getAppPort() {
        return authServiceProperties.getAppPort();
    }

    /**
     * 获得当前appInfo。
     *
     * @return
     */
    public static String getAppInfo() {
        return authServiceProperties.getAppName() + ":" + authServiceProperties.getAppVersion();
    }

    /**
     * 获得当前appHostInfo。
     *
     * @return
     */
    public static String getAppHostInfo() {
        return authServiceProperties.getAppName() + ":" + authServiceProperties.getAppVersion() + "/" + authServiceProperties.getAppHost() + ":" + authServiceProperties.getAppPort();
    }

    /**
     * 处理非法token。¬
     *
     * @param invalidToken
     */
    public static void invalidToken(InvalidTokenData invalidToken) {
        if (invalidContextToken( invalidToken.getUserType(), invalidToken.getToken() )) {
            invalidTokenCache.put( invalidToken.getToken(), TokenInvalidType.findByValue( invalidToken.getInvalidType() ) + ":" + invalidToken.getNotice() );
        }
    }

    /**
     * 生成匿名用户token。
     *
     * @return
     */
    public static String genAnonymousToken(long saasId, long mchId) {
        return String.valueOf( UserType.ANYONE.getValue() ) + TOKEN_TYPE_SEPARATOR + mchId + "!0@" + saasId;
    }

    /**
     * 获得authServer rpc实例。
     *
     * @return
     */
    public static AuthServiceRpc getAuthServiceRpc() {
        return authServiceRpc;
    }

    /**
     * 获得saas下用户数量限制。
     *
     * @param saasId
     * @return
     */
    public static int getSaasUserLimit(long saasId) {
        return authServiceRpc.getSaasUserLimit( saasId ).getData();
    }

    /**
     * 获得当前token类型。
     */
    public static int getTokenType() {
        AuthTokenData authToken = contextTokenHolder.get();
        if (authToken != null) {
            return authToken.getTokenType();
        } else {
            return TokenType.NONE.getValue();
        }
    }

    /**
     * 获得当前的用户Id
     *
     * @return
     */
    public static long getUserId() {
        AuthTokenData authToken = contextTokenHolder.get();
        if (authToken != null) {
            return authToken.getUserId();
        } else {
            return -1;
        }
    }

    /**
     * 获得当前的用户名
     *
     * @return
     */
    public static String getUserName() {
        AuthTokenData authToken = contextTokenHolder.get();
        if (authToken != null) {
            return authToken.getUserName();
        } else {
            return null;
        }
    }

    /**
     * 获得当前用户的组ID。
     *
     * @return
     */
    public static long getGroupId() {
        AuthTokenData authToken = contextTokenHolder.get();
        if (authToken != null) {
            return authToken.getGroupId();
        } else {
            return -1;
        }
    }

    /**
     * 获得当前用户真实姓名。
     *
     * @return
     */
    public static String getRealName() {
        AuthTokenData authToken = contextTokenHolder.get();
        if (authToken != null) {
            return authToken.getRealName();
        } else {
            return null;
        }
    }

    /**
     * 获得当前用户昵称。
     *
     * @return
     */
    public static String getNickName() {
        AuthTokenData authToken = contextTokenHolder.get();
        if (authToken != null) {
            return authToken.getNickName();
        } else {
            return null;
        }
    }

    /**
     * 获得手机号。
     *
     * @return
     */
    public static String getMobile() {
        AuthTokenData authToken = contextTokenHolder.get();
        if (authToken != null) {
            return authToken.getMobile();
        } else {
            return null;
        }
    }

    /**
     * 获得Email地址。
     *
     * @return
     */
    public static String getEmail() {
        AuthTokenData authToken = contextTokenHolder.get();
        if (authToken != null) {
            return authToken.getEmail();
        } else {
            return null;
        }
    }

    /**
     * 获得当前用户的登录IP。
     *
     * @return
     */
    public static String getLoginIp() {
        AuthTokenData authToken = contextTokenHolder.get();
        if (authToken != null) {
            return authToken.getUserIp();
        } else {
            return null;
        }
    }

    /**
     * 获得当前用户等级。
     *
     * @return
     */
    public static int getUserGrade() {
        AuthTokenData authToken = contextTokenHolder.get();
        if (authToken != null) {
            return authToken.getUserGrade();
        } else {
            return -1;
        }
    }

    /**
     * 获得当前的saasId
     *
     * @return
     */
    public static long getSaasId() {
        AuthTokenData authToken = contextTokenHolder.get();
        if (authToken != null) {
            return authToken.getSaasId();
        } else {
            return -1;
        }
    }

    /**
     * 获得当前的mchId
     *
     * @return
     */
    public static long getMchId() {
        AuthTokenData authToken = contextTokenHolder.get();
        if (authToken != null) {
            return authToken.getMchId();
        } else {
            return -1;
        }
    }

    /**
     * 获得当前的用户类型
     *
     * @return
     */
    public static int getUserType() {
        AuthTokenData authToken = contextTokenHolder.get();
        if (authToken != null) {
            return authToken.getUserType();
        } else {
            return -1;
        }
    }

    /**
     * 主动销毁DestroyContext,防止内存泄漏
     */
    public static void destroyContextToken() {
        contextTokenHolder.remove();
    }

    /**
     * 请求内Get业务对象上下文
     */
    public static MscActionLog getContextLog() {
        return contextLogHolder.get();
    }

    /**
     * 请求内Set日志对象上下文
     *
     * @param httpHandlerLog
     */
    public static void setContextLog(MscActionLog httpHandlerLog) {
        contextLogHolder.set( httpHandlerLog );
    }

    /**
     * 主动销毁DestroyLogContext,防止内存泄漏
     */
    public static void destroyContextLog() {
        contextLogHolder.remove();
    }

    /**
     * 绑定ref信息。
     *
     * @param refType 业务类型 用户代码自行定义,不应有冲突
     */
    public static MscActionLog logRef(String refType) {
        return logInfo( refType, null, null, null );
    }

    /**
     * 绑定ref信息
     *
     * @param refTypeClass 业务类 用户代码自行定义,不应有冲突
     */
    public static MscActionLog logRef(Class refTypeClass) {
        return logInfo( refTypeClass.getName(), null, null, null );
    }

    /**
     * 绑定ref信息。
     *
     * @param refType 业务类型 用户代码自行定义,不应有冲突
     * @param refId   业务主键
     */
    public static MscActionLog logRef(String refType, Serializable refId) {
        return logInfo( refType, refId, null, null );
    }

    /**
     * 绑定ref信息
     *
     * @param refTypeClass 业务类 用户代码自行定义,不应有冲突
     * @param refId        业务主键
     */
    public static MscActionLog logRef(Class refTypeClass, Serializable refId) {
        return logInfo( refTypeClass.getName(), refId, null, null );
    }

    /**
     * 写日志信息
     *
     * @param opLog 日志信息
     */
    public static MscActionLog logInfo(String opLog) {
        return logInfo( (String) null, null, null, opLog );
    }

    /**
     * 写日志信息
     *
     * @param refTypeClass 业务类 用户代码自行定义,不应有冲突
     * @param opLog        日志信息
     */
    public static MscActionLog logInfo(Class refTypeClass, String opLog) {
        return logInfo( refTypeClass.getName(), null, null, opLog );
    }

    /**
     * 写日志信息
     *
     * @param refType 业务类 用户代码自行定义,不应有冲突
     * @param opLog   日志信息
     */
    public static MscActionLog logInfo(String refType, String opLog) {
        return logInfo( refType, null, null, opLog );
    }

    /**
     * 写日志信息
     *
     * @param refType 业务类 用户代码自行定义,不应有冲突
     * @param refId   业务主键
     * @param opLog   日志信息
     */
    public static MscActionLog logInfo(String refType, Serializable refId, String opLog) {
        return logInfo( refType, refId, null, opLog );
    }

    /**
     * 写日志信息
     *
     * @param refTypeClass 业务类 用户代码自行定义,不应有冲突
     * @param refId        业务主键
     * @param opLog        日志信息
     */
    public static MscActionLog logInfo(Class refTypeClass, Serializable refId, String opLog) {
        return logInfo( refTypeClass.getName(), refId, null, opLog );
    }

    /**
     * 写日志信息
     *
     * @param refTypeClass 业务类 用户代码自行定义,不应有冲突
     * @param refId        业务主键
     * @param opLog        日志信息
     */
    public static MscActionLog logInfo(Class refTypeClass, Serializable refId, String opState, String opLog) {
        return logInfo( refTypeClass.getName(), refId, opState, opLog );
    }

    /**
     * 写日志信息
     *
     * @param refType 业务类型 用户代码自行定义,不应有冲突
     * @param refId   业务主键
     * @param opLog   日志信息
     */
    public static MscActionLog logInfo(String refType, Serializable refId, String opState, String opLog) {
        MscActionLog mscActionLog = contextLogHolder.get();
        if (mscActionLog == null) {
            logger.warn( "未设置日志属性, 请检查代码: refType={}, refId={}, opState={}, opLog={}", refType, refId, opState, opLog );
        } else {
            //允许多次设置日志信息。
            if (refType != null) {
                mscActionLog.setRefType( refType );
            }
            if (refId != null) {
                mscActionLog.setRefId( refId );
            }
            if (opState != null) {
                mscActionLog.setOpState( opState );
            }
            if (opLog != null) {
                if (mscActionLog.getOpLog() != null) {
                    opLog = mscActionLog.getOpLog() + "\n" + opLog;
                }
                mscActionLog.setOpLog( opLog );
            }
        }
        return mscActionLog;
    }

    /**
     * 写日志信息
     *
     * @param opLog 日志信息
     */
    public static MscActionLog logWarn(String opLog) {
        return logWarn( (String) null, null, opLog );
    }

    /**
     * 写日志信息
     *
     * @param refTypeClass 业务类 用户代码自行定义,不应有冲突
     * @param opLog        日志信息
     */
    public static MscActionLog logWarn(Class refTypeClass, String opLog) {
        return logWarn( refTypeClass.getName(), null, opLog );
    }

    /**
     * 写日志信息
     *
     * @param refType 业务类 用户代码自行定义,不应有冲突
     * @param opLog   日志信息
     */
    public static MscActionLog logWarn(String refType, String opLog) {
        return logWarn( refType, null, opLog );
    }

    /**
     * 写日志信息
     *
     * @param refTypeClass 业务类 用户代码自行定义,不应有冲突
     * @param refId        业务主键
     * @param opLog        日志信息
     */
    public static MscActionLog logWarn(Class refTypeClass, Serializable refId, String opLog) {
        return logWarn( refTypeClass.getName(), refId, opLog );
    }

    /**
     * 写日志信息
     *
     * @param refType 业务类 用户代码自行定义,不应有冲突
     * @param refId   业务主键
     * @param opLog   日志信息
     */
    public static MscActionLog logWarn(String refType, Serializable refId, String opLog) {
        return logInfo( refType, refId, ResponseData.STATE_WARN, opLog );
    }

    /**
     * 写日志信息
     *
     * @param opLog 日志信息
     */
    public static MscActionLog logError(String opLog) {
        return logError( (String) null, null, opLog );
    }

    /**
     * 写日志信息
     *
     * @param refTypeClass 业务类 用户代码自行定义,不应有冲突
     * @param opLog        日志信息
     */
    public static MscActionLog logError(Class refTypeClass, String opLog) {
        return logError( refTypeClass.getName(), null, opLog );
    }

    /**
     * 写日志信息
     *
     * @param refType 业务类 用户代码自行定义,不应有冲突
     * @param opLog   日志信息
     */
    public static MscActionLog logError(String refType, String opLog) {
        return logError( refType, null, opLog );
    }

    /**
     * 写日志信息
     *
     * @param refTypeClass 业务类 用户代码自行定义,不应有冲突
     * @param refId        业务主键
     * @param opLog        日志信息
     */
    public static MscActionLog logError(Class refTypeClass, Serializable refId, String opLog) {
        return logError( refTypeClass.getName(), refId, opLog );
    }

    /**
     * 写日志信息
     *
     * @param refType 业务类 用户代码自行定义,不应有冲突
     * @param refId   业务主键
     * @param opLog   日志信息
     */
    public static MscActionLog logError(String refType, Serializable refId, String opLog) {
        return logInfo( refType, refId, ResponseData.STATE_ERROR, opLog );
    }

    /**
     * 获得远端IP.
     *
     * @return
     */
    public static String getRemoteIp(HttpServletRequest request) {
        return IpWebUtils.getRealIpString( request );
    }

    /**
     * 获得远端IP.
     *
     * @return
     */
    public static String getRemoteIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        return IpWebUtils.getRealIpString( request );
    }

    /**
     * 请求内Get业务对象上下文
     */
    public static AuthTokenData getContextToken() {
        return contextTokenHolder.get();
    }

    /**
     * 请求内Set业务对象上下文
     *
     * @param authToken
     */
    public static void setContextToken(AuthTokenData authToken) {
        contextTokenHolder.set( authToken );
    }

    /**
     * 获得活跃用户信息。
     *
     * @param userType
     */
    public static int getActiveUserNum(int userType) {
        if (userType == UserType.RPC.getValue()) {
            return (int) userRpcCache.estimatedSize();
        } else if (userType == UserType.GUEST.getValue()) {
            return (int) userGuestCache.estimatedSize();
        } else if (userType == UserType.ROOT.getValue()) {
            return (int) userRootCache.estimatedSize();
        } else if (userType == UserType.OPS.getValue()) {
            return (int) userOpsCache.estimatedSize();
        } else if (userType == UserType.ADMIN.getValue()) {
            return (int) userAdminCache.estimatedSize();
        } else if (UserType.SAAS.getValue() <= userType) {
            return (int) userSaasCache.estimatedSize();
        }
        return -1;
    }

    /**
     * parseRawToken
     * 注意与TokenFactory.createAccessToken保持一致.
     *
     * @param bearerToken
     * @return
     */
    public static ResponseData<AuthTokenData> parseRawToken(String ip, String bearerToken) {
        if (bearerToken == null) {
            return ResponseData.errorCode( AuthServiceConstants.HTTP_UNAUTHORIZED_CODE, "!!!Server Token header null." );
        }
        if (bearerToken.length() < AuthServiceConstants.TOKEN_HEADER_PREFIX.length()) {
            return ResponseData.errorCode( AuthServiceConstants.HTTP_UNAUTHORIZED_CODE, "!!!Server Token header invalid. Header Data: " + bearerToken );
        }
        int tokenStart = AuthServiceConstants.TOKEN_HEADER_PREFIX.length();
        //解析出token来
        String token = bearerToken.substring( tokenStart );
        int typeSeparator = token.indexOf( TOKEN_TYPE_SEPARATOR );
        if (typeSeparator == -1) {
            return ResponseData.errorCode( AuthServiceConstants.HTTP_UNAUTHORIZED_CODE, "!!!Server Token parse illegal. Token: " + token );
        }
        //解析token信息。
        int userType = -1;
        try {
            userType = Integer.parseInt( token.substring( 0, typeSeparator ) );
        } catch (Exception e) {
            return ResponseData.errorCode( AuthServiceConstants.HTTP_UNAUTHORIZED_CODE, "!!!Server Token UserType parse illegal. Token: " + token );
        }
        //  检查用户类型映射
        if (!UserType.checkTypeValid( userType )) {
            return ResponseData.errorCode( AuthServiceConstants.HTTP_UNAUTHORIZED_CODE, "!!!Server Token UserType invalid. Token: " + token );
        }
        if (userType == UserType.ANYONE.getValue()) {
            return ResponseData.success( parseAnonymousToken( token.substring( typeSeparator + 1 ) ) );
        }
        //检查是否非法token请求，如果确认非法，则直接抛异常，引导用户重新登录。
        String invalidNotice = invalidTokenCache.getIfPresent( token );
        if (StringUtils.isNotBlank( invalidNotice )) {
            return ResponseData.errorCode( AuthServiceConstants.HTTP_UNAUTHORIZED_CODE, "!!!Server Token[" + token + "] Invalid. Msg: " + invalidNotice );
        }
        //检查缓存中的token。
        AuthTokenData authTokenData = loadCachedTokenData( userType, token );
        //服务器端拉取缓存。
        if (authTokenData == null) {
            ResponseData<AuthTokenData> verifyResponse = authServiceRpc.verifyToken( token );
            if (verifyResponse.isSuccess()) {
                authTokenData = verifyResponse.getData();
                putContextToken( ip, userType, token, authTokenData );
            } else {
                //失败的token要缓存一下，防止有人乱试
                invalidTokenCache.put( token, verifyResponse.getMsg() );
                //找不到的抛Token过期异常。
                return verifyResponse;
            }
        }
        // 检查过期
        if (authTokenData.isExpired()) {
            invalidContextToken( userType, token );
            return ResponseData.errorCode( AuthServiceConstants.HTTP_TOKEN_EXPIRED_CODE, "!Server AccessToken expired. Token: " + token );
        }
        return ResponseData.success( authTokenData );
    }

    /**
     * 缓存中失效用户对象
     *
     * @param userType
     * @param rawToken
     */
    private static boolean invalidContextToken(int userType, String rawToken) {
        if (userType == UserType.RPC.getValue()) {
            if (userRpcCache.getIfPresent( rawToken ) != null) {
                userRpcCache.invalidate( rawToken );
                return true;
            }
        } else if (userType == UserType.GUEST.getValue()) {
            if (userGuestCache.getIfPresent( rawToken ) != null) {
                userGuestCache.invalidate( rawToken );
                return true;
            }
        } else if (userType == UserType.ROOT.getValue()) {
            if (userRootCache.getIfPresent( rawToken ) != null) {
                userRootCache.invalidate( rawToken );
                return true;
            }
        } else if (userType == UserType.OPS.getValue()) {
            if (userOpsCache.getIfPresent( rawToken ) != null) {
                userOpsCache.invalidate( rawToken );
                return true;
            }
        } else if (userType == UserType.ADMIN.getValue()) {
            if (userAdminCache.getIfPresent( rawToken ) != null) {
                userAdminCache.invalidate( rawToken );
                return true;
            }
        } else if (UserType.SAAS.getValue() <= userType) {
            if (userSaasCache.getIfPresent( rawToken ) != null) {
                userSaasCache.invalidate( rawToken );
                return true;
            }
        }
        return false;
    }

    /**
     * 解析匿名用户token。
     *
     * @return
     */
    private static AuthTokenData parseAnonymousToken(String tokenData) {
        AuthTokenData authToken = new AuthTokenData();
        String[] ids = tokenData.split( "!0@" );
        if (ids.length != 2) {
            //说明数据有问题，直接返回吧。
            return authToken;
        }
        authToken.setUserType( UserType.ANYONE.getValue() );
        authToken.setSaasId( Long.parseLong( ids[1] ) );
        authToken.setMchId( Long.parseLong( ids[0] ) );
        authToken.setUserId( 0 );
        return authToken;
    }

    /**
     * 缓存中取用户对象
     *
     * @param userType
     * @param rawToken
     * @return
     */
    private static AuthTokenData loadCachedTokenData(int userType, String rawToken) {
        //检查缓存。
        AuthTokenData authTokenData = null;
        if (userType == UserType.RPC.getValue()) {
            authTokenData = userRpcCache.getIfPresent( rawToken );
        } else if (userType == UserType.GUEST.getValue()) {
            authTokenData = userGuestCache.getIfPresent( rawToken );
        } else if (userType == UserType.ROOT.getValue()) {
            authTokenData = userRootCache.getIfPresent( rawToken );
        } else if (userType == UserType.OPS.getValue()) {
            authTokenData = userOpsCache.getIfPresent( rawToken );
        } else if (userType == UserType.ADMIN.getValue()) {
            authTokenData = userAdminCache.getIfPresent( rawToken );
        } else if (UserType.SAAS.getValue() <= userType) {
            authTokenData = userSaasCache.getIfPresent( rawToken );
        }
        return authTokenData;
    }

    /**
     * 把用户对象放到缓存
     *
     * @param userType
     * @param rawToken
     * @param authToken
     */
    private static void putContextToken(String ip, int userType, String rawToken, AuthTokenData authToken) {
        if (userType == UserType.RPC.getValue()) {
            userRpcCache.put( rawToken, authToken );
        } else if (userType == UserType.GUEST.getValue()) {
            userGuestCache.put( rawToken, authToken );
        } else if (userType == UserType.ROOT.getValue()) {
            userRootCache.put( rawToken, authToken );
        } else if (userType == UserType.OPS.getValue()) {
            userOpsCache.put( rawToken, authToken );
        } else if (userType == UserType.ADMIN.getValue()) {
            userAdminCache.put( rawToken, authToken );
        } else if (UserType.SAAS.getValue() <= userType) {
            userSaasCache.put( rawToken, authToken );
        }
    }
}
