package uw.auth.service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.auth.service.annotation.MscPermDeclare;
import uw.auth.service.constant.AuthServiceConstants;
import uw.auth.service.constant.AuthType;
import uw.auth.service.constant.TokenType;
import uw.auth.service.constant.UserType;
import uw.auth.service.token.AuthTokenData;
import uw.common.response.ResponseData;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 权限校验服务。
 * <p>
 * 持有应用注册后由 auth-center 下发的权限 ID 映射表（{@code appPermMap}），
 * {@code AuthServiceFilter} 调用 {@link #hasPerm} 完成用户类型匹配、Token 类型校验与
 * 权限集合判定。{@code appPermMap} 通过 {@link #initAppPerm} 在应用注册成功后初始化。
 *
 * @author axeon
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
    private volatile Map<String, Integer> appPermMap;

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
     * 检查当前 Token 是否具备访问目标接口的权限。
     *
     * @param authTokenData   当前请求的 Token 数据，null 时直接拒绝
     * @param mscPermDeclare  接口上的权限声明注解
     * @param uri             权限 code（请求 URI + ":" + 请求方法）
     * @return 校验通过返回成功，否则返回对应的错误响应（403/503/426 等）
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
                //依赖权限表的分支，必须确保权限数据已初始化。
                if (appPermMap == null) {
                    logger.warn("应用权限数据尚未初始化完成，请稍后重试!");
                    return RESPONSE_SERVICE_UNAVAILABLE;
                }
                if (authTokenData.getTokenType() >= TokenType.COMMON.getValue() && checkTokenPermSet(authTokenData.getPermSet(), uri)) {
                    return RESPONSE_SUCCESS;
                } else {
                    return RESPONSE_FORBIDDEN;
                }
            case SUDO:
                //超级用户权限：先校验Token类型（缺超级Token给出升级提示），再校验权限集合。
                if (authTokenData.getTokenType() < TokenType.SUDO.getValue()) {
                    return RESPONSE_UPGRADE_REQUIRED;
                }
                if (appPermMap == null) {
                    logger.warn("应用权限数据尚未初始化完成，请稍后重试!");
                    return RESPONSE_SERVICE_UNAVAILABLE;
                }
                if (!checkTokenPermSet(authTokenData.getPermSet(), uri)) {
                    return RESPONSE_FORBIDDEN;
                } else {
                    return RESPONSE_SUCCESS;
                }
            default:
                return RESPONSE_FORBIDDEN;
        }
    }

    /**
     * 初始化应用权限数据。
     * <p>
     * 由 {@code MscAppUpdateService} 在应用注册成功后调用，将 auth-center 下发的权限 ID 映射表
     * 深拷贝为不可变 Map 后持有。
     *
     * @param appId      应用 ID
     * @param appPermMap 权限 code → 权限 ID 映射，null 时视为空表
     * @param regState   注册状态
     */
    protected void initAppPerm(long appId, Map<String, Integer> appPermMap, int regState) {
        this.appId = appId;
        this.regState = regState;
        //深拷贝为不可变Map，避免调用方修改原Map导致并发问题与不可预期变更。
        this.appPermMap = appPermMap != null
                ? Map.copyOf(appPermMap)
                : Collections.emptyMap();
    }

    /**
     * 检查 Token 权限集合中是否包含目标 URI 对应的权限。
     *
     * @param permSet Token 携带的权限 ID 集合
     * @param uri     权限 code
     * @return true 表示权限命中
     */
    private boolean checkTokenPermSet(Set<Integer> permSet, String uri) {
        Integer permId = appPermMap.get(uri);
        return permSet != null && permId != null && permSet.contains(permId);
    }

}
