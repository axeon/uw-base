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
import uw.auth.service.service.MscAuthPermService;
import uw.auth.service.token.AuthTokenData;
import uw.auth.service.token.InvalidTokenData;
import uw.auth.service.util.IpWebUtils;
import uw.auth.service.vo.MscActionLog;
import uw.common.response.ResponseData;
import uw.common.util.SystemClock;
import uw.log.es.LogClient;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务核心辅助类。
 * <p>
 * 提供：
 * <ul>
 *   <li>当前请求用户信息访问（基于 ThreadLocal 绑定的 {@link AuthTokenData}）</li>
 *   <li>Token 解析、本地 Caffeine 缓存（按 UserType 分层、按 expireAt 独立过期）与非法 Token 黑名单</li>
 *   <li>操作日志上下文管理（{@code logRef/logInfo/logSysInfo}）</li>
 *   <li>应用信息与 {@link AuthServiceRpc} 实例访问</li>
 * </ul>
 * 全静态方法设计，由 {@code AuthServiceAutoConfiguration} 在启动时完成依赖注入与缓存初始化。
 *
 * @author axeon
 */
public class AuthServiceHelper {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceHelper.class);

    /**
     * ThreadLocal 保存当前线程请求用户对象
     */
    private static final ThreadLocal<AuthTokenData> contextTokenHolder = new ThreadLocal<>();

    /**
     * ThreadLocal 保存当前线程请求日志对象
     */
    private static final ThreadLocal<MscActionLog> contextLogHolder = new ThreadLocal<>();

    /**
     * 针对token有效期，每个单独设定过期时间。
     */
    private static final Expiry<String, AuthTokenData> cacheExpiryPolicy = new Expiry<>() {
        @Override
        public long expireAfterCreate(String key, AuthTokenData authToken, long currentTime) {
            long ttlMillis = authToken.getExpireAt() - SystemClock.now();
            return TimeUnit.MILLISECONDS.toNanos(Math.max(0L, ttlMillis));
        }

        @Override
        public long expireAfterUpdate(String key, AuthTokenData authToken, long currentTime, long currentDuration) {
            long ttlMillis = authToken.getExpireAt() - SystemClock.now();
            return TimeUnit.MILLISECONDS.toNanos(Math.max(0L, ttlMillis));
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
    private static MscAuthPermService authPermService;

    public AuthServiceHelper(final AuthServiceProperties authServiceProperties, final MscAuthPermService authPermService, final AuthServiceRpc authServiceRpc) {
        AuthServiceHelper.authServiceProperties = authServiceProperties;
        AuthServiceHelper.authPermService = authPermService;
        AuthServiceHelper.authServiceRpc = authServiceRpc;
        Map<Integer, Long> userCacheConfig = authServiceProperties.getTokenCache();
        userRpcCache = Caffeine.newBuilder().maximumSize(userCacheConfig.getOrDefault(UserType.RPC.getValue(), 100L)).expireAfter(cacheExpiryPolicy).build();
        userRootCache = Caffeine.newBuilder().maximumSize(userCacheConfig.getOrDefault(UserType.ROOT.getValue(), 100L)).expireAfter(cacheExpiryPolicy).build();
        userOpsCache = Caffeine.newBuilder().maximumSize(userCacheConfig.getOrDefault(UserType.OPS.getValue(), 100L)).expireAfter(cacheExpiryPolicy).build();
        userAdminCache = Caffeine.newBuilder().maximumSize(userCacheConfig.getOrDefault(UserType.ADMIN.getValue(), 1000L)).expireAfter(cacheExpiryPolicy).build();
        userSaasCache = Caffeine.newBuilder().maximumSize(userCacheConfig.getOrDefault(UserType.SAAS.getValue(), 100_000L)).expireAfter(cacheExpiryPolicy).build();
        userGuestCache = Caffeine.newBuilder().maximumSize(userCacheConfig.getOrDefault(UserType.GUEST.getValue(), 1000_000L)).expireAfter(cacheExpiryPolicy).build();
        invalidTokenCache = Caffeine.newBuilder().maximumSize(10_000L).expireAfterWrite(20, TimeUnit.MINUTES).build();
    }

    /**
     * 获取当前appId。
     *
     * @return
     */
    public static long getAppId() {
        return authServiceProperties.getAppId();
    }

    /**
     * 获取当前appPermMap。
     *
     * @return
     */
    public static Map<String, Integer> getAppPermMap() {
        return authPermService.getAppPermMap();
    }

    /**
     * 获取当前appLabel。
     *
     * @return
     */
    public static String getAppLabel() {
        return authServiceProperties.getAppLabel();
    }

    /**
     * 获取当前appName。
     *
     * @return
     */
    public static String getAppName() {
        return authServiceProperties.getAppName();
    }

    /**
     * 获取当前appVersion。
     *
     * @return
     */
    public static String getAppVersion() {
        return authServiceProperties.getAppVersion();
    }

    /**
     * 获取当前appHost。
     *
     * @return
     */
    public static String getAppHost() {
        return authServiceProperties.getAppHost();
    }

    /**
     * 获取当前appPort。
     *
     * @return
     */
    public static int getAppPort() {
        return authServiceProperties.getAppPort();
    }

    /**
     * 获取当前appInfo。
     *
     * @return
     */
    public static String getAppInfo() {
        return authServiceProperties.getAppName() + ":" + authServiceProperties.getAppVersion();
    }

    /**
     * 获取当前appHostInfo。
     *
     * @return
     */
    public static String getAppHostInfo() {
        return authServiceProperties.getAppName() + ":" + authServiceProperties.getAppVersion() + "/" + authServiceProperties.getAppHost() + ":" + authServiceProperties.getAppPort();
    }

    /**
     * 处理非法token。
     *
     * @param invalidToken
     */
    public static void invalidToken(InvalidTokenData invalidToken) {
        if (invalidContextToken(invalidToken.getUserType(), invalidToken.getToken())) {
            invalidTokenCache.put(invalidToken.getToken(), TokenInvalidType.findByValue(invalidToken.getInvalidType()) + ":" + invalidToken.getNotice());
        }
    }

    /**
     * 缓存中失效用户对象
     *
     * @param userType
     * @param rawToken
     */
    private static boolean invalidContextToken(int userType, String rawToken) {
        if (userType == UserType.RPC.getValue()) {
            if (userRpcCache.getIfPresent(rawToken) != null) {
                userRpcCache.invalidate(rawToken);
                return true;
            }
        } else if (userType == UserType.GUEST.getValue()) {
            if (userGuestCache.getIfPresent(rawToken) != null) {
                userGuestCache.invalidate(rawToken);
                return true;
            }
        } else if (userType == UserType.ROOT.getValue()) {
            if (userRootCache.getIfPresent(rawToken) != null) {
                userRootCache.invalidate(rawToken);
                return true;
            }
        } else if (userType == UserType.OPS.getValue()) {
            if (userOpsCache.getIfPresent(rawToken) != null) {
                userOpsCache.invalidate(rawToken);
                return true;
            }
        } else if (userType == UserType.ADMIN.getValue()) {
            if (userAdminCache.getIfPresent(rawToken) != null) {
                userAdminCache.invalidate(rawToken);
                return true;
            }
        } else if (userType >= UserType.SAAS.getValue()) {
            if (userSaasCache.getIfPresent(rawToken) != null) {
                userSaasCache.invalidate(rawToken);
                return true;
            }
        }
        return false;
    }

    /**
     * 生成匿名用户token。
     *
     * @return
     */
    public static String genAnonymousToken(long saasId, long mchId) {
        return String.valueOf(UserType.ANY.getValue()) + AuthServiceConstants.TOKEN_ACCESS_TYPE_SEPARATOR + mchId + "!0@" + saasId;
    }

    /**
     * 获取authServer rpc实例。
     *
     * @return
     */
    public static AuthServiceRpc getAuthServiceRpc() {
        return authServiceRpc;
    }

    /**
     * 获取saas下用户数量限制。
     *
     * @param saasId
     * @return
     */
    public static int getSaasUserLimit(long saasId) {
        return authServiceRpc.getSaasUserLimit(saasId).getData();
    }

    /**
     * 获取当前token类型。
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
     * 获取当前的用户Id
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
     * 获取当前的用户名
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
     * 获取当前用户的组ID。
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
     * 获取当前用户真实姓名。
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
     * 获取当前用户昵称。
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
     * 获取手机号。
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
     * 获取Email地址。
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
     * 获取当前用户的登录IP。
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
     * 获取当前用户等级。
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
     * 获取当前的saasId
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
     * 获取当前的mchId
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
     * 获取当前的用户类型
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
        contextLogHolder.set(httpHandlerLog);
    }

    /**
     * 主动销毁DestroyLogContext,防止内存泄漏
     */
    public static void destroyContextLog() {
        contextLogHolder.remove();
    }

    /**
     * 写系统日志信息。
     * 主要用于系统后台操作调用，此操作不需要web环境依赖。
     *
     * @param apiCode      接口编码，会记录在apiUrl中。
     * @param apiName      接口名称，会记录在apiName中。
     * @param apiIp        接口来源IP，会记录在userIp中。
     * @param saasId       操作对应的SaasId。
     * @param bizTypeClass 业务类型类，会记录在bizType中。
     * @param bizId        业务主键，会记录在bizId中。
     * @param bizLog       业务日志，会记录在bizLog中。
     * @param responseData 业务响应数据，一般已记录和第三方系统交互的响应数据。
     */
    public static void logSysInfo(String apiCode, String apiName, String apiIp, long saasId, Class<?> bizTypeClass, Serializable bizId, String bizLog, ResponseData<?> responseData) {
        logSysInfo(apiCode, apiName, apiIp, saasId, bizTypeClass, bizId, bizLog, null, null, responseData);
    }


    /**
     * 写系统日志信息。
     * 主要用于系统后台操作调用，此操作不需要web环境依赖。
     *
     * @param apiCode      接口编码，会记录在apiUrl中。
     * @param apiName      接口名称，会记录在apiName中。
     * @param apiIp        接口来源IP，会记录在userIp中。
     * @param saasId       操作对应的SaasId。
     * @param bizType      业务类型，会记录在bizType中。
     * @param bizId        业务主键，会记录在bizId中。
     * @param bizLog       业务日志，会记录在bizLog中。
     * @param responseData 业务响应数据，一般已记录和第三方系统交互的响应数据。
     */
    public static void logSysInfo(String apiCode, String apiName, String apiIp, long saasId, String bizType, Serializable bizId, String bizLog, ResponseData<?> responseData) {
        logSysInfo(apiCode, apiName, apiIp, saasId, bizType, bizId, bizLog, null, null, responseData);
    }

    /**
     * 写系统日志信息。
     * 主要用于系统后台操作调用，此操作不需要web环境依赖。
     *
     * @param apiCode      接口编码，会记录在apiUrl中。
     * @param apiName      接口名称，会记录在apiName中。
     * @param apiIp        接口来源IP，会记录在userIp中。
     * @param saasId       操作对应的SaasId。
     * @param bizTypeClass 业务类型类，会记录在bizType中。
     * @param bizId        业务主键，会记录在bizId中。
     * @param bizLog       业务日志，会记录在bizLog中。
     * @param requestBody  业务请求Body，一般已记录和第三方系统交互的请求报文。
     * @param responseBody 业务响应Body，一般已记录和第三方系统交互的响应报文。
     * @param responseData 业务响应数据，一般已记录和第三方系统交互的响应数据。
     */
    public static void logSysInfo(String apiCode, String apiName, String apiIp, long saasId, Class<?> bizTypeClass, Serializable bizId, String bizLog, String requestBody, String responseBody, ResponseData<?> responseData) {
        logSysInfo(apiCode, apiName, apiIp, saasId, bizTypeClass.getName(), bizId, bizLog, requestBody, responseBody, responseData);
    }

    /**
     * 写系统日志信息。
     * 主要用于系统后台操作调用，此操作不需要web环境依赖。
     *
     * @param apiCode      接口编码，会记录在apiUrl中。
     * @param apiName      接口名称，会记录在apiName中。
     * @param apiIp        接口来源IP，会记录在userIp中。
     * @param saasId       操作对应的SaasId。
     * @param bizType      业务类型，会记录在bizType中。
     * @param bizId        业务主键，会记录在bizId中。
     * @param bizLog       业务日志，会记录在bizLog中。
     * @param requestBody  业务请求Body，一般已记录和第三方系统交互的请求报文。
     * @param responseBody 业务响应Body，一般已记录和第三方系统交互的响应报文。
     * @param responseData 业务响应数据，一般已记录和第三方系统交互的响应数据。
     */
    public static void logSysInfo(String apiCode, String apiName, String apiIp, long saasId, String bizType, Serializable bizId, String bizLog, String requestBody, String responseBody, ResponseData<?> responseData) {
        MscActionLog mscActionLog = new MscActionLog();
        mscActionLog.setAppInfo(authServiceProperties.getAppName() + ":" + authServiceProperties.getAppVersion());
        mscActionLog.setAppHost(authServiceProperties.getAppHost() + ":" + authServiceProperties.getAppPort());
        mscActionLog.setLogLevel(ActionLog.CRIT.getValue());
        mscActionLog.setUserId(0);
        mscActionLog.setUserName("system");
        mscActionLog.setNickName("system");
        mscActionLog.setRealName("system");
        mscActionLog.setSaasId(saasId);
        mscActionLog.setMchId(0);
        mscActionLog.setGroupId(0);
        mscActionLog.setUserType(UserType.RPC.getValue());
        mscActionLog.setApiUri(apiCode);
        mscActionLog.setApiName(apiName);
        mscActionLog.setUserIp(apiIp);
        mscActionLog.setRequestDate(SystemClock.nowDate());
        mscActionLog.setRequestBody(requestBody);
        mscActionLog.setResponseBody(responseBody);
        mscActionLog.setResponseState(responseData.getState());
        mscActionLog.setResponseCode(responseData.getCode());
        mscActionLog.setResponseMsg(responseData.getMsg());
        mscActionLog.setResponseMillis(0);
        mscActionLog.setStatusCode(0);
        //允许多次设置日志信息。
        if (bizType != null) {
            mscActionLog.setBizType(bizType);
        }
        if (bizId != null) {
            mscActionLog.setBizId(bizId);
        }
        if (bizLog != null) {
            if (mscActionLog.getBizLog() != null) {
                bizLog = mscActionLog.getBizLog() + "\n" + bizLog;
            }
            mscActionLog.setBizLog(bizLog);
        }
        LogClient.getInstance().log(mscActionLog);
    }


    /**
     * 绑定ref信息。
     *
     * @param bizType 业务类型 用户代码自行定义,不应有冲突
     */
    public static MscActionLog logRef(String bizType) {
        return logInfo(bizType, null, null);
    }

    /**
     * 写日志信息
     *
     * @param bizType 业务类 用户代码自行定义,不应有冲突
     * @param bizId   业务主键
     * @param bizLog  日志信息
     */
    public static MscActionLog logInfo(String bizType, Serializable bizId, String bizLog) {
        MscActionLog mscActionLog = contextLogHolder.get();
        if (mscActionLog == null) {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            String requestUri = "";
            if (attributes != null) {
                HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
                requestUri = request.getRequestURI();
            }
            logger.warn("RequestPath:[{}]日志相关设置无效，将不会记录日志。bizType=[{}], bizId=[{}], bizLog=[{}]", requestUri, bizType, bizId, bizLog);
        } else {
            //允许多次设置日志信息。
            if (bizType != null) {
                mscActionLog.setBizType(bizType);
            }
            if (bizId != null) {
                mscActionLog.setBizId(bizId);
            }
            if (bizLog != null) {
                if (mscActionLog.getBizLog() != null) {
                    bizLog = mscActionLog.getBizLog() + "\n" + bizLog;
                }
                mscActionLog.setBizLog(bizLog);
            }
        }
        return mscActionLog;
    }


    /**
     * 绑定ref信息
     *
     * @param bizTypeClass 业务类 用户代码自行定义,不应有冲突
     */
    public static MscActionLog logRef(Class<?> bizTypeClass) {
        return logInfo(bizTypeClass.getName(), null, null);
    }

    /**
     * 绑定ref信息。
     *
     * @param bizType 业务类型 用户代码自行定义,不应有冲突
     * @param bizId   业务主键
     */
    public static MscActionLog logRef(String bizType, Serializable bizId) {
        return logInfo(bizType, bizId, null);
    }

    /**
     * 绑定ref信息
     *
     * @param bizTypeClass 业务类 用户代码自行定义,不应有冲突
     * @param bizId        业务主键
     */
    public static MscActionLog logRef(Class<?> bizTypeClass, Serializable bizId) {
        return logInfo(bizTypeClass.getName(), bizId, null);
    }

    /**
     * 写日志信息
     *
     * @param bizLog 日志信息
     */
    public static MscActionLog logInfo(String bizLog) {
        return logInfo((String) null, null, bizLog);
    }

    /**
     * 写日志信息
     *
     * @param bizTypeClass 业务类 用户代码自行定义,不应有冲突
     * @param bizLog       日志信息
     */
    public static MscActionLog logInfo(Class<?> bizTypeClass, String bizLog) {
        return logInfo(bizTypeClass.getName(), null, bizLog);
    }

    /**
     * 写日志信息
     *
     * @param bizType 业务类 用户代码自行定义,不应有冲突
     * @param bizLog  日志信息
     */
    public static MscActionLog logInfo(String bizType, String bizLog) {
        return logInfo(bizType, null, bizLog);
    }

    /**
     * 写日志信息
     *
     * @param bizTypeClass 业务类 用户代码自行定义,不应有冲突
     * @param bizId        业务主键
     * @param bizLog       日志信息
     */
    public static MscActionLog logInfo(Class<?> bizTypeClass, Serializable bizId, String bizLog) {
        return logInfo(bizTypeClass.getName(), bizId, bizLog);
    }

    /**
     * 获取远端IP.
     *
     * @return
     */
    public static String getRemoteIp(HttpServletRequest request) {
        return IpWebUtils.getRealIp(request);
    }

    /**
     * 获取用户IP.
     *
     * @return
     */
    public static String getRemoteIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        return IpWebUtils.getRealIp(request);
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
    private static void setContextToken(AuthTokenData authToken) {
        contextTokenHolder.set(authToken);
    }

    /**
     * 获取活跃用户信息。
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
            return ResponseData.errorCode(AuthServiceConstants.HTTP_UNAUTHORIZED_CODE, "!!!Server Token header null.");
        }
        if (bearerToken.length() < AuthServiceConstants.TOKEN_HEADER_PREFIX.length()) {
            return ResponseData.errorCode(AuthServiceConstants.HTTP_UNAUTHORIZED_CODE, "!!!Server Token header invalid. Header Data: " + bearerToken);
        }
        int tokenStart = AuthServiceConstants.TOKEN_HEADER_PREFIX.length();
        //解析出token来
        String token = bearerToken.substring(tokenStart);
        int typeSeparator = token.indexOf(AuthServiceConstants.TOKEN_ACCESS_TYPE_SEPARATOR);
        if (typeSeparator == -1) {
            return ResponseData.errorCode(AuthServiceConstants.HTTP_UNAUTHORIZED_CODE, "!!!Server Token parse illegal. Token: " + token);
        }
        //解析token信息。
        int userType = -1;
        try {
            userType = Integer.parseInt(token.substring(0, typeSeparator));
        } catch (Exception e) {
            return ResponseData.errorCode(AuthServiceConstants.HTTP_UNAUTHORIZED_CODE, "!!!Server Token UserType parse illegal. Token: " + token);
        }
        //  检查用户类型映射
        if (!UserType.checkTypeValid(userType)) {
            return ResponseData.errorCode(AuthServiceConstants.HTTP_UNAUTHORIZED_CODE, "!!!Server Token UserType invalid. Token: " + token);
        }
        if (userType == UserType.ANY.getValue()) {
            return ResponseData.success(parseAnonymousToken(token.substring(typeSeparator + 1)));
        }
        //检查是否非法token请求，如果确认非法，则直接抛异常，引导用户重新登录。
        String invalidNotice = invalidTokenCache.getIfPresent(token);
        if (StringUtils.isNotBlank(invalidNotice)) {
            return ResponseData.errorCode(AuthServiceConstants.HTTP_UNAUTHORIZED_CODE, "!!!Server Token[" + token + "] Invalid. Msg: " + invalidNotice);
        }
        //检查缓存中的token。
        AuthTokenData authTokenData = loadCachedTokenData(userType, token);
        //服务器端拉取缓存。
        if (authTokenData == null) {
            ResponseData<AuthTokenData> verifyResponse = authServiceRpc.verifyToken(token);
            if (verifyResponse.isSuccess()) {
                authTokenData = verifyResponse.getData();
                putContextToken(ip, userType, token, authTokenData);
            } else {
                //仅当认证中心明确判定token失效（401/498等）时才入负缓存，防止有人乱试。
                //若为503等服务异常，不缓存，避免auth-center短暂故障把合法token误判为非法长达20分钟。
                if (shouldCacheInvalid(verifyResponse.getCode())) {
                    invalidTokenCache.put(token, verifyResponse.getMsg());
                }
                return verifyResponse;
            }
        }
        // 检查过期
        if (authTokenData.isExpired()) {
            invalidContextToken(userType, token);
            return ResponseData.errorCode(AuthServiceConstants.HTTP_TOKEN_EXPIRED_CODE, "!Server AccessToken expired. Token: " + token);
        }
        // 设定当前线程tokenData
        AuthServiceHelper.setContextToken(authTokenData);
        return ResponseData.success(authTokenData);
    }

    /**
     * 判断认证中心返回的失败码是否代表"token已被明确判定失效"。
     * 仅这类业务失败才应入负缓存；服务异常（503）不应缓存，避免误伤合法token。
     *
     * @param code 认证中心返回码
     * @return true 表示可缓存为非法token
     */
    private static boolean shouldCacheInvalid(String code) {
        if (StringUtils.isBlank(code)) {
            return false;
        }
        return code.equals(AuthServiceConstants.HTTP_UNAUTHORIZED_CODE)
                || code.equals(AuthServiceConstants.HTTP_TOKEN_EXPIRED_CODE)
                || code.equals(AuthServiceConstants.HTTP_FORBIDDEN_CODE);
    }

    /**
     * 解析匿名用户token。
     *
     * @return
     */
    private static AuthTokenData parseAnonymousToken(String tokenData) {
        AuthTokenData authToken = new AuthTokenData();
        //先给定默认值
        authToken.setSaasId(-1);
        authToken.setMchId(-1);
        authToken.setUserId(-1);
        String[] ids = tokenData.split("!0@");
        if (ids.length != 2) {
            //说明数据有问题，直接返回吧。
            return authToken;
        }
        authToken.setUserType(UserType.ANY.getValue());
        try {
            authToken.setSaasId(Long.parseLong(ids[1]));
        } catch (Exception ignored) {
        }
        try {
            authToken.setMchId(Long.parseLong(ids[0]));
        } catch (Exception ignored) {
        }
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
            authTokenData = userRpcCache.getIfPresent(rawToken);
        } else if (userType == UserType.GUEST.getValue()) {
            authTokenData = userGuestCache.getIfPresent(rawToken);
        } else if (userType == UserType.ROOT.getValue()) {
            authTokenData = userRootCache.getIfPresent(rawToken);
        } else if (userType == UserType.OPS.getValue()) {
            authTokenData = userOpsCache.getIfPresent(rawToken);
        } else if (userType == UserType.ADMIN.getValue()) {
            authTokenData = userAdminCache.getIfPresent(rawToken);
        } else if (userType >= UserType.SAAS.getValue()) {
            authTokenData = userSaasCache.getIfPresent(rawToken);
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
            userRpcCache.put(rawToken, authToken);
        } else if (userType == UserType.GUEST.getValue()) {
            userGuestCache.put(rawToken, authToken);
        } else if (userType == UserType.ROOT.getValue()) {
            userRootCache.put(rawToken, authToken);
        } else if (userType == UserType.OPS.getValue()) {
            userOpsCache.put(rawToken, authToken);
        } else if (userType == UserType.ADMIN.getValue()) {
            userAdminCache.put(rawToken, authToken);
        } else if (userType >= UserType.SAAS.getValue()) {
            userSaasCache.put(rawToken, authToken);
        }
    }
}
