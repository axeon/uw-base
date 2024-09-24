package uw.auth.service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.auth.service.annotation.MscPermDeclare;
import uw.auth.service.constant.AuthType;
import uw.auth.service.constant.UserType;
import uw.auth.service.token.AuthTokenData;

import java.util.Collections;
import java.util.Map;

/**
 * 用户信息服务接口
 */
public class AuthPermService {

    private static final Logger logger = LoggerFactory.getLogger( AuthPermService.class );

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


    public AuthPermService() {
    }

    /**
     * 初始化应用权限数据
     *
     * @param appId
     * @param appPermMap
     * @param regState
     */
    public void initAppPerm(long appId, Map<String, Integer> appPermMap, int regState) {
        this.appId = appId;
        this.regState = regState;
        if (appPermMap != null) {
            this.appPermMap = Collections.unmodifiableMap( appPermMap );
        }
    }

    /**
     * 获得appId。
     *
     * @return
     */
    public long getAppId() {
        return appId;
    }

    /**
     * 获得注册状态。
     *
     * @return
     */
    public int getRegState() {
        return regState;
    }

    /**
     * 获得AppPermMap。
     *
     * @return
     */
    public Map<String, Integer> getAppPermMap() {
        return appPermMap;
    }

    /**
     * 判断用户权限信息
     *
     * @param authToken - 业务对象上下文
     * @param uri       - URI
     * @return
     */
    public boolean hasPerm(AuthTokenData authToken, MscPermDeclare mscPermDeclare, String uri) {
        //没有加注解的，直接返回true
        if (authToken == null || mscPermDeclare == null) {
            return true;
        }
        long currentUserType = authToken.getUserType();
        UserType declareType = mscPermDeclare.user();
        // RPC用户 调用RPC权限，放在第一位是因为rpc操作高频，减少不必要的判定直通。
        if (currentUserType == UserType.RPC.getValue() && declareType == UserType.RPC) {
            return true;
        }
        // ANONYMOUS权限，所有人直接过
        if (declareType == UserType.ANONYMOUS) {
            return true;
        }
        // GUEST权限，并且是guest用户，直接过。
        if (declareType == UserType.GUEST && currentUserType == UserType.GUEST.getValue()) {
            return true;
        }
        /** 以下为需要严格判定权限的功能 , UserType>=100**/
        // 超级管理员 调用ROOT权限直接过
        if (declareType == UserType.ROOT && currentUserType == UserType.ROOT.getValue() && authToken.getIsMaster() == 1) {
            return true;
        }
        //处理验证类型，如果不判定验证类型，那就直接过。
        if (mscPermDeclare.auth() == AuthType.NONE) {
            //不验证类型
            return true;
        } else if (mscPermDeclare.auth() == AuthType.USER) {
            if (currentUserType == declareType.getValue()) {
                return true;
            }
        }
        //此时需要校验权限id。
        if (authToken.getPermSet() == null) {
            return false;
        }
        if (appPermMap == null) {
            logger.warn( "App初始化过程中......请等待!" );
            return false;
        }
        Integer permId = appPermMap.get( uri );
        return permId != null && authToken.getPermSet().contains( permId );
    }
}
