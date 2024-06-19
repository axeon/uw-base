package uw.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uw.auth.service.conf.AuthServiceProperties;
import uw.auth.service.constant.AuthConstants;
import uw.auth.service.constant.InvalidTokenType;
import uw.auth.service.constant.UserType;
import uw.auth.service.exception.TokenExpiredException;
import uw.auth.service.exception.TokenInvalidateException;
import uw.auth.service.rpc.AuthServiceRpc;
import uw.auth.service.service.AuthPermService;
import uw.auth.service.token.AuthTokenData;
import uw.auth.service.token.InvalidTokenData;
import uw.auth.service.util.IPAddressUtils;
import uw.auth.service.vo.MscActionLog;
import uw.common.dto.ResponseData;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private static final Expiry<String, AuthTokenData> cacheExpiryPolicy = new Expiry<String, AuthTokenData>() {
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
    private static Cache<String, AuthTokenData> userDevCache;
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
     * 反射的属性缓存
     */
    private static LoadingCache<Class, Field[]> authFieldsCache = Caffeine.newBuilder().build( cls -> {
        List<Field> fields = new ArrayList<>();
        //获取类的所有属性，包括父类
        Class clazz = cls;
        int i = 0;
        while (clazz != null && clazz != Object.class && i < 5) {
            fields.addAll( Arrays.asList( clazz.getDeclaredFields() ) );
            clazz = clazz.getSuperclass();
            i++;
        }
        // 过滤,只保存auth相关的属性
        Field[] authFields = new Field[4];
        if (fields != null && fields.size() > 0) {
            for (int j = 0; j < fields.size(); j++) {
                Field field = fields.get( j );
                switch (field.getName()) {
                    case "saasId":
                        if (authFields[0] == null) {
                            field.setAccessible( true );
                            authFields[0] = field;
                        }
                        break;
                    case "userType":
                        if (authFields[1] == null) {
                            field.setAccessible( true );
                            authFields[1] = field;
                        }
                        break;
                    case "mchId":
                        if (authFields[2] == null) {
                            field.setAccessible( true );
                            authFields[2] = field;
                        }
                        break;
                    case "userId":
                        if (authFields[3] == null) {
                            field.setAccessible( true );
                            authFields[3] = field;
                        }
                        break;
                    default:
                        continue;
                }
            }
        }
        return authFields;
    } );
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
        Map<Integer, Long> userCacheConfig = authServiceProperties.getUserCache();
        userRpcCache = Caffeine.newBuilder().maximumSize( userCacheConfig.getOrDefault( UserType.RPC.getValue(), 100L ) ).expireAfter( cacheExpiryPolicy ).build();
        userRootCache = Caffeine.newBuilder().maximumSize( userCacheConfig.getOrDefault( UserType.ROOT.getValue(), 100L ) ).expireAfter( cacheExpiryPolicy ).build();
        userDevCache = Caffeine.newBuilder().maximumSize( userCacheConfig.getOrDefault( UserType.OPS.getValue(), 100L ) ).expireAfter( cacheExpiryPolicy ).build();
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
     * 获得当前appName。
     *
     * @return
     */
    public static String getAppName() {
        return authServiceProperties.getAppName();
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
     * 处理非法token。¬
     *
     * @param invalidToken
     */
    public static void invalidToken(InvalidTokenData invalidToken) {
        if (invalidContextToken( invalidToken.getUserType(), invalidToken.getToken() )) {
            invalidTokenCache.put( invalidToken.getToken(), InvalidTokenType.findByValue( invalidToken.getInvalidType() ) + ":" + invalidToken.getNotice() );
        }
    }

    /**
     * 生成匿名用户token。
     *
     * @return
     */
    public static String genAnonymousToken(long saasId, long mchId) {
        StringBuilder sb = new StringBuilder( 60 );
        sb.append( UserType.ANONYMOUS.getValue() ).append( TOKEN_TYPE_SEPARATOR ).append( mchId ).append( "!0@" ).append( saasId );
        return sb.toString();
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
     * 获得当前用户的微信id。
     *
     * @return
     */
    public static String getWxId() {
        AuthTokenData authToken = contextTokenHolder.get();
        if (authToken != null) {
            return authToken.getWxId();
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
        return IPAddressUtils.getTrueIp( request );
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
        return IPAddressUtils.getTrueIp( request );
    }

    /**
     * checkAuthInfo的便利方法，只填入saasId
     *
     * @param obj
     */
    public static void checkSaasId(Object obj) {
        checkAuthInfo( obj, true, false, false, false );
    }

    /**
     * 检查一个对象的auth相关属性是否正确。
     *
     * @param obj
     * @return
     */
    public static ResponseData checkAuthInfo(Object obj, boolean checkSaasId, boolean checkUserId, boolean checkMchId, boolean checkUserType) {
        Class cls = obj.getClass();
        // 从缓存获取auth相关属性，数组中不同index代表不同属性，获取到的field已经setAccessible(true);
        Field[] authFields = authFieldsCache.get( cls );
        AuthTokenData authToken = getContextToken();
        if (authToken == null) {
            return ResponseData.errorMsg( "用户信息获取为空，该方法仅能在Controller线程中使用" );
        }
        // 判断对象是否有auth属性，有则填入当前用户的auth属性
        // 0：saasId
        if (checkSaasId && authFields[0] != null) {
            if (authFields[0].getType().equals( Long.class )) {
                try {
                    if (!authFields[0].get( obj ).equals( authToken.getSaasId() )) {
                        return ResponseData.errorMsg( "saasId不匹配！" );
                    }
                } catch (IllegalAccessException e) {
                }
            }
        }
        // 1：userType
        if (checkUserType && authFields[1] != null) {
            if (authFields[1].getType().equals( Integer.class )) {
                try {
                    if (!authFields[1].get( obj ).equals( authToken.getUserType() )) {
                        return ResponseData.errorMsg( "userType不匹配！" );
                    }
                } catch (IllegalAccessException e) {
                }
            }
        }
        // 2：mchId
        if (checkMchId && authFields[2] != null) {
            if (authFields[2].getType().equals( Long.class )) {
                try {
                    if (!authFields[2].get( obj ).equals( authToken.getMchId() )) {
                        return ResponseData.errorMsg( "mchId不匹配！" );
                    }
                } catch (IllegalAccessException e) {
                }
            }
        }
        // 3：userId
        if (checkUserId && authFields[3] != null) {
            if (authFields[3].getType().equals( Long.class )) {
                try {
                    if (!authFields[3].get( obj ).equals( authToken.getUserId() )) {
                        return ResponseData.errorMsg( "userId不匹配！" );
                    }
                } catch (IllegalAccessException e) {
                }
            }
        }
        return ResponseData.success();
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
     * checkAuthInfo的便利方法，只填入saasId和userId
     *
     * @param obj
     */
    public static ResponseData checkSaasUserId(Object obj) {
        return checkAuthInfo( obj, true, true, false, false );
    }

    /**
     * checkAuthInfo的便利方法，只填入saasId和mchId
     *
     * @param obj
     */
    public static ResponseData checkSaasMchId(Object obj) {
        return checkAuthInfo( obj, true, false, true, false );
    }

    /**
     * checkAuthInfo的便利方法，填入当前用户全部权限属性
     *
     * @param obj
     */
    public static ResponseData checkAuthInfo(Object obj) {
        return checkAuthInfo( obj, true, true, true, true );
    }

    /**
     * assignAuthInfo的便利方法，只填入saasId
     *
     * @param obj
     */
    public static void bindSaasId(Object obj) {
        bindAuthInfo( obj, true, false, false, false );
    }

    /**
     * 判断一个对象若有auth相关属性，则填入当前用户的权限属性
     *
     * @param obj            对象
     * @param assignSaasId   是否填入saasId
     * @param assignUserId   是否填入用户id
     * @param assignMchId    是否填入商户id
     * @param assignUserType 是否填入用户类型
     */
    public static void bindAuthInfo(Object obj, boolean assignSaasId, boolean assignUserId, boolean assignMchId, boolean assignUserType) {
        Class cls = obj.getClass();
        // 从缓存获取auth相关属性，数组中不同index代表不同属性，获取到的field已经setAccessible(true);
        Field[] authFields = authFieldsCache.get( cls );
        AuthTokenData authToken = getContextToken();
        if (authToken == null) {
            throw new RuntimeException( "用户信息获取为空，该方法仅能在Controller线程中使用" );
        }
        // 判断对象是否有auth属性，有则填入当前用户的auth属性
        // 0：saasId
        if (assignSaasId && authFields[0] != null) {
            if (!authFields[0].getType().equals( Long.class )) {
                throw new RuntimeException( "saasId属性类型错误，应为Long" );
            }
            try {
                authFields[0].set( obj, authToken.getSaasId() );
            } catch (IllegalAccessException e) {
                throw new RuntimeException( "填入saasId失败，请检查代码", e );
            }
        }
        // 1：userType
        if (assignUserType && authFields[1] != null) {
            if (!authFields[1].getType().equals( Integer.class )) {
                throw new RuntimeException( "userType属性类型错误，应为Integer" );
            }
            try {
                authFields[1].set( obj, authToken.getUserType() );
            } catch (IllegalAccessException e) {
                throw new RuntimeException( "填入userType失败，请检查代码", e );
            }
        }
        // 2：mchId
        if (assignMchId && authFields[2] != null) {
            //userType的值大于1时，设置商户id
            if (authToken.getUserType() > UserType.RPC.getValue()) {
                if (!authFields[2].getType().equals( Long.class )) {
                    throw new RuntimeException( "mchId属性类型错误，应为Long" );
                }
                try {
                    authFields[2].set( obj, authToken.getMchId() );
                } catch (IllegalAccessException e) {
                    throw new RuntimeException( "填入mchId失败，请检查代码", e );
                }
            }
        }
        // 3：userId
        if (assignUserId && authFields[3] != null) {
            if (!authFields[3].getType().equals( Long.class )) {
                throw new RuntimeException( "userId属性类型错误，应为Long" );
            }
            try {
                authFields[3].set( obj, authToken.getUserId() );
            } catch (IllegalAccessException e) {
                throw new RuntimeException( "填入userId失败，请检查代码", e );
            }
        }
    }

    /**
     * assignAuthInfo的便利方法，只填入saasId和userId
     *
     * @param obj
     */
    public static void bindSaasUserId(Object obj) {
        bindAuthInfo( obj, true, true, false, false );
    }

    /**
     * assignAuthInfo的便利方法，只填入saasId和mchId
     *
     * @param obj
     */
    public static void bindSaasMchId(Object obj) {
        bindAuthInfo( obj, true, false, true, false );
    }

    /**
     * assignAuthInfo的便利方法，填入当前用户全部权限属性
     *
     * @param obj
     */
    public static void bindAuthInfo(Object obj) {
        bindAuthInfo( obj, true, true, true, true );
    }

    /**
     * genAuthSql的便利方法，生成当前用户saasId的sql
     */
    public static String genSaasIdSql() {
        return genAuthSql( true, false, false, false );
    }

    /**
     * 生成当前用户的权限属性的sql
     *
     * @param assignSaasId   是否填入saasId
     * @param assignUserId   是否填入用户id
     * @param assignMchId    是否填入商户id
     * @param assignUserType 是否填入用户类型
     */
    public static String genAuthSql(boolean assignSaasId, boolean assignUserId, boolean assignMchId, boolean assignUserType) {
        AuthTokenData authToken = getContextToken();
        if (authToken == null) {
            throw new RuntimeException( "用户信息获取为空，该方法仅能在Controller线程中使用" );
        }
        StringBuilder sql = new StringBuilder();
        // saasId
        if (assignSaasId) {
            sql.append( "saas_id_=" ).append( authToken.getSaasId() );
        }
        // userType
        if (assignUserType) {
            sql.append( " and user_type=" ).append( authToken.getUserType() );
        }
        // mchId
        if (assignMchId) {
            sql.append( " and mch_id=" ).append( authToken.getMchId() );
        }
        // userId
        if (assignUserId) {
            sql.append( " and user_id=" ).append( authToken.getUserId() );
        }
        // 如果是and开头就去掉
        if (sql.indexOf( " and " ) == 0) {
            return sql.substring( 5 );
        }
        return sql.toString();
    }

    /**
     * genAuthSql的便利方法，生成当前用户saasId和userId的sql
     */
    public static String genSaasUserIdSql() {
        return genAuthSql( true, true, false, false );
    }

    /**
     * genAuthSql的便利方法，生成当前用户saasId和mchId的sql
     */
    public static String genSaasMchIdSql() {
        return genAuthSql( true, false, true, false );
    }

    /**
     * genAuthSql的便利方法，生成当前用户saasId,userType,mchId,userId的sql
     */
    public static String genAuthSql() {
        return genAuthSql( true, true, true, true );
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
            return (int) userDevCache.estimatedSize();
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
    public AuthTokenData parseRawToken(String ip, String bearerToken) {
        if (bearerToken == null) {
            throw new TokenInvalidateException( "!!!Server Token header null. " );
        }
        if (bearerToken.length() < AuthConstants.TOKEN_HEADER_PREFIX.length()) {
            throw new TokenInvalidateException( "!!!Server Token header invalid. Header Data: " + bearerToken );
        }
        int tokenStart = AuthConstants.TOKEN_HEADER_PREFIX.length();
        //解析出token来
        String token = bearerToken.substring( tokenStart );
        int typeSeparator = token.indexOf( TOKEN_TYPE_SEPARATOR );
        if (typeSeparator == -1) {
            throw new TokenInvalidateException( "!!!Server Token parse illegal. Token: " + token );
        }
        //解析token信息。
        int userType = -1;
        try {
            userType = Integer.parseInt( token.substring( 0, typeSeparator ) );
        } catch (Exception e) {
            throw new TokenInvalidateException( "!!!Server Token UserType parse illegal. Token: " + token );
        }
        //  检查用户类型映射
        if (!UserType.checkTypeValid( userType )) {
            throw new TokenInvalidateException( "!!!Server Token UserType invalid. Token: " + token );
        }
        if (userType == UserType.ANONYMOUS.getValue()) {
            return parseAnonymousToken( token.substring( typeSeparator + 1 ) );
        }
        //检查缓存中的token。
        AuthTokenData authTokenData = parseAuthToken( userType, token );
        if (authTokenData == null) {
            //去中心服务器验证一下token。
            if (logger.isDebugEnabled()) {
                logger.debug( "uw-auth-service send to center verifyToken: {}", token );
            }
            ResponseData<AuthTokenData> verifyResponse = authServiceRpc.verifyToken( token );
            if (verifyResponse.isSuccess()) {
                authTokenData = verifyResponse.getData();
                putContextToken( ip, userType, token, authTokenData );
            } else {
                //失败的token要缓存一下，防止有人乱试
                invalidTokenCache.put( token, "INVALID:ERROR" );
                //找不到的抛Token过期异常。
                throw new TokenExpiredException( verifyResponse.getMsg() );
            }
        }
        // 检查过期
        if (authTokenData.isExpired()) {
            invalidContextToken( userType, token );
            throw new TokenExpiredException( "!Server Token expired. " );
        }
        return authTokenData;
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
            if (userDevCache.getIfPresent( rawToken ) != null) {
                userDevCache.invalidate( rawToken );
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
        authToken.setUserType( UserType.ANONYMOUS.getValue() );
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
    private static AuthTokenData parseAuthToken(int userType, String rawToken) {
        AuthTokenData authTokenData = null;
        if (userType == UserType.RPC.getValue()) {
            authTokenData = userRpcCache.getIfPresent( rawToken );
        } else if (userType == UserType.GUEST.getValue()) {
            authTokenData = userGuestCache.getIfPresent( rawToken );
        } else if (userType == UserType.ROOT.getValue()) {
            authTokenData = userRootCache.getIfPresent( rawToken );
        } else if (userType == UserType.OPS.getValue()) {
            authTokenData = userDevCache.getIfPresent( rawToken );
        } else if (userType == UserType.ADMIN.getValue()) {
            authTokenData = userAdminCache.getIfPresent( rawToken );
        } else if (UserType.SAAS.getValue() <= userType) {
            authTokenData = userSaasCache.getIfPresent( rawToken );
        }
        if (authTokenData != null) {
            return authTokenData;
        }
        //检查是否非法token请求，如果确认非法，则直接抛异常，引导用户重新登录。
        String invalidNotice = invalidTokenCache.getIfPresent( rawToken );
        if (StringUtils.isNotBlank( invalidNotice )) {
            throw new TokenInvalidateException( "!!!Server Token[" + rawToken + "] Invalid. Msg: " + invalidNotice );
        }
        return null;
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
            userDevCache.put( rawToken, authToken );
        } else if (userType == UserType.ADMIN.getValue()) {
            userAdminCache.put( rawToken, authToken );
        } else if (UserType.SAAS.getValue() <= userType) {
            userSaasCache.put( rawToken, authToken );
        }
    }
}
