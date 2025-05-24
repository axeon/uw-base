package uw.auth.service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.auth.service.annotation.MscPermDeclare;
import uw.auth.service.constant.AuthServiceConstants;
import uw.auth.service.constant.AuthType;
import uw.auth.service.constant.TokenType;
import uw.auth.service.constant.UserType;
import uw.auth.service.token.AuthTokenData;
import uw.common.dto.ResponseData;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 用户信息服务接口
 */
public class MscAuthPermService {

    private static final Logger logger = LoggerFactory.getLogger(MscAuthPermService.class);

    /**
     * 默认成功返回。
     */
    private static final ResponseData RESPONSE_SUCCESS = ResponseData.SUCCESS;

    /**
     * 拒绝权限返回。
     */
    private static final ResponseData RESPONSE_FORBIDDEN = ResponseData.of(0, null, ResponseData.STATE_ERROR, AuthServiceConstants.HTTP_FORBIDDEN_CODE, "No Permission!");

    /**
     * 服务不可用返回。
     */
    private static final ResponseData RESPONSE_SERVICE_UNAVAILABLE = ResponseData.of(0, null, ResponseData.STATE_ERROR, AuthServiceConstants.HTTP_SERVICE_UNAVAILABLE_CODE, "Service Starting...");

    /**
     * 需要升级返回。
     */
    private static final ResponseData RESPONSE_UPGRADE_REQUIRED = ResponseData.of(0, null, ResponseData.STATE_WARN, AuthServiceConstants.HTTP_UPGRADE_REQUIRED_CODE, "Upgrade Required!");

    /**
     * 需要支付返回。
     */
    private static final ResponseData RESPONSE_PAYMENT_REQUIRED = ResponseData.of(0, null, ResponseData.STATE_WARN, AuthServiceConstants.HTTP_PAYMENT_REQUIRED_CODE, "Payment Required!");

    /**
     * 应用权限数据,初始化载入。
     * key: 权限标识
     * value: 权限ID
     */
    private Map<String, Integer> appPermMap;

    /**
     * appId。
     */
    private long appId;

    /**
     * 注册状态。0:服务器信息，1:新注册信息。
     */
    private int regState;


    public MscAuthPermService() {
    }

    /**
     * 获取appId。
     *
     * @return
     */
    public long getAppId() {
        return appId;
    }

    /**
     * 获取注册状态。
     *
     * @return
     */
    public int getRegState() {
        return regState;
    }

    /**
     * 获取AppPermMap。
     *
     * @return
     */
    public Map<String, Integer> getAppPermMap() {
        return appPermMap;
    }

    /**
     * 检查权限。
     *
     * @param authTokenData
     * @param mscPermDeclare
     * @param uri
     * @return
     */
    public ResponseData<?> hasPerm(AuthTokenData authTokenData, MscPermDeclare mscPermDeclare, String uri) {
        //没有加注解或者uri为空的，直接返回true。从调用关系看，貌似不会为null。
        //优先判定主要是为了尽量直通。
        if (mscPermDeclare == null) {
            return RESPONSE_SUCCESS;
        }
        //token为null，直接返回false。
        if (authTokenData == null) {
            return RESPONSE_FORBIDDEN;
        }
        //权限定义用户类型。
        UserType permUserType = mscPermDeclare.user();
        //权限定义验证类型。
        AuthType permAuthType = mscPermDeclare.auth();
        //token用户类型。
        long tokenUserType = authTokenData.getUserType();
        //token用户类型和权限用户类型不匹配，直接返回false。
        if (permUserType.getValue() > UserType.ANY.getValue() && permUserType.getValue() != tokenUserType) {
            return RESPONSE_FORBIDDEN;
        }
        //检测应用权限表。
        if (appPermMap == null) {
            logger.warn("应用权限数据尚未初始化完成，请稍后重试!");
            return RESPONSE_SERVICE_UNAVAILABLE;
        }
        //根据用户类型进行权限验证。
        switch (permAuthType) {
            case NONE:
                //无验证直接返回成功。
                return RESPONSE_SUCCESS;
            case TEMP:
                //临时用户验证需要仅确认临时Token类型。
                if (authTokenData.getTokenType() >= TokenType.TEMP.getValue()) {
                    return RESPONSE_SUCCESS;
                } else {
                    return RESPONSE_FORBIDDEN;
                }
            case USER:
                //用户验证需要仅确认标准Token类型。
                if (authTokenData.getTokenType() >= TokenType.COMMON.getValue()) {
                    return RESPONSE_SUCCESS;
                } else {
                    return RESPONSE_FORBIDDEN;
                }
            case PERM:
                //权限验证需要确认标准Token类型和权限集合。
                if (authTokenData.getTokenType() >= TokenType.COMMON.getValue() && checkTokenPermSet(authTokenData.getPermSet(), uri)) {
                    return RESPONSE_SUCCESS;
                } else {
                    return RESPONSE_FORBIDDEN;
                }
            case SUDO:
                //超级用户权限需要确认超级Token类型和权限集合。
                if (!checkTokenPermSet(authTokenData.getPermSet(), uri)) {
                    return RESPONSE_FORBIDDEN;
                }
                if (authTokenData.getTokenType() < TokenType.SUDO.getValue()) {
                    return RESPONSE_UPGRADE_REQUIRED;
                } else {
                    return RESPONSE_SUCCESS;
                }
            default:
                return RESPONSE_FORBIDDEN;
        }
    }

    /**
     * 初始化应用权限数据
     *
     * @param appId
     * @param appPermMap
     * @param regState
     */
    protected void initAppPerm(long appId, Map<String, Integer> appPermMap, int regState) {
        this.appId = appId;
        this.regState = regState;
        this.appPermMap = appPermMap != null
                ? Collections.unmodifiableMap(appPermMap)
                : Collections.emptyMap();
    }

    /**
     * 检查Token权限集合中是否有URI权限。
     *
     * @param permSet
     * @param uri
     * @return
     */
    private boolean checkTokenPermSet(Set<Integer> permSet, String uri) {
        Integer permId = appPermMap.get(uri);
        return permId != null && permSet.contains(permId);
    }

}
