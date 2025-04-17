package uw.auth.service.rpc;

import uw.auth.client.vo.TokenResponse;
import uw.auth.service.token.AuthTokenData;
import uw.auth.service.vo.MscUserGroupVo;
import uw.auth.service.vo.MscUserRegister;
import uw.auth.service.vo.MscUserVo;
import uw.common.dto.ResponseData;

import java.util.Date;
import java.util.List;

/**
 * auth-server向auth-center 交互信息。
 *
 * @author axeon
 */
public interface AuthServiceRpc {


    /**
     * 验证收到的token。
     *
     * @param token
     * @return
     */
    ResponseData<AuthTokenData> verifyToken(String token);

    /**
     * 生成guest的id。
     *
     * @return
     */
    ResponseData<Long> genUserId();

    /**
     * 生成guest的token。
     *
     * @param saasId
     * @param mchId
     * @param userId
     * @param userName
     * @param checkDoubleLogin 是否检查双登。
     * @param userIp           登录用户Ip。
     * @return
     * @throws Exception
     */
    TokenResponse genGuestToken(String loginAgent, String clientAgent, int loginType, String loginId, long saasId, long mchId, long userId, String userName, String nickName, String realName, String mobile, String email, String userIp, String remark, boolean checkDoubleLogin);

    /**
     * 通知guest登录失败。
     *
     * @param saasId
     * @param mchId
     * @param userId
     * @param userName
     * @param userIp   登录用户Ip。
     * @return
     * @throws Exception
     */
    ResponseData notifyGuestLoginFail(String loginAgent, String clientAgent, int loginType, String loginId, long saasId, long mchId, long userId, String userName, String nickName, String realName, String mobile, String email, String userIp, String remark);

    /**
     * 踢出Guest用户。
     *
     * @param saasId
     * @param userId
     * @param remark
     */
    ResponseData kickoutGuest(String loginAgent, long saasId, long userId, String remark);

    /**
     * 初始化Saas权限。
     *
     * @param saasId
     * @param saasName saas名称
     * @return
     */
    ResponseData initSaasPerm(long saasId, String saasName, String[] initAppNames, String adminPasswd, String adminMobile, String adminEmail);


    /**
     * 添加saas权限。
     *
     * @return
     */
    ResponseData grantSaasPerm(long saasId, String permIds, String remark);

    /**
     * 撤销saas权限。
     *
     * @return
     */
    ResponseData revokeSaasPerm(long saasId, String permIds, String remark);

    /**
     * 恢复指定SAAS权限。
     */
    ResponseData enableSaasPerm(long saasId, String remark);

    /**
     * 停用指定SAAS权限。
     */
    ResponseData disableSaasPerm(long saasId, String remark);

    /**
     * 运营商限速设置。
     *
     * @param saasId
     * @param remark
     * @return
     */
    ResponseData updateSaasRateLimit(long saasId, int limitSeconds, int limitRequests, int limitBytes, Date expireDate, String remark);

    /**
     * 清除运营商限速设置。
     *
     * @param saasId
     * @param remark
     * @return
     */
    ResponseData clearSaasRateLimit(long saasId, String remark);

    /**
     * 修改SAAS用户数限制。
     *
     * @param saasId
     * @param remark
     * @return
     */
    ResponseData updateSaasUserLimit(long saasId, int userLimit, String remark);

    /**
     * 获取Saas用户数限制。
     */
    ResponseData<Integer> getSaasUserLimit(long saasId);

    /**
     * 注册Saas用户。
     *
     * @param mscUserRegister
     * @return userId
     * @throws Exception
     */
    ResponseData createUser(MscUserRegister mscUserRegister);

    /**
     * 获取用户信息。
     *
     * @param userId
     */
    ResponseData<MscUserVo> getUser(long userId);

    /**
     * 获取saas用户列表。
     *
     * @param saasId
     * @param mchId
     * @param groupId
     * @param userId
     * @param userName
     * @param nickName
     * @param realName
     * @param mobile
     * @param email
     */
    ResponseData<List<MscUserVo>> getUserList(long saasId, int userType, long mchId, long groupId, long userId, String userName, String nickName, String realName, String mobile, String email);

    /**
     * 获取saas用户组列表。
     *
     * @param saasId
     * @param mchId
     * @param groupId
     * @param groupName
     */
    ResponseData<List<MscUserGroupVo>> getUserGroupList(long saasId, int userType, long mchId, long groupId, String groupName);

    /**
     * 更新saas名称。
     *
     * @param saasId
     * @param saasName
     */
    ResponseData updateSaasName(long saasId, String saasName);


    /**
     * 获取应用的权限ID列表。
     */
    ResponseData<String> getAppSaasPerm(String[] appNames);

}
