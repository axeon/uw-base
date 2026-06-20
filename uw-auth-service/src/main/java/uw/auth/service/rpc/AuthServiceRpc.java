package uw.auth.service.rpc;

import uw.auth.client.vo.TokenResponse;
import uw.auth.service.token.AuthTokenData;
import uw.auth.service.vo.MscUserGroupVo;
import uw.auth.service.vo.MscUserRegister;
import uw.auth.service.vo.MscUserVo;
import uw.common.response.ResponseData;

import java.util.List;

/**
 * 服务端向 auth-center 的交互接口。
 * <p>
 * 封装 Token 验证、Guest Token 生成、用户/用户组管理、Saas 权限管理等 RPC 调用。
 * 默认实现 {@code AuthServiceRpcImpl} 基于 {@code RestClient}。
 * 通过 {@code AuthServiceHelper.getAuthServiceRpc()} 获取实例。
 *
 * @author axeon
 * @see uw.auth.service.AuthServiceHelper#getAuthServiceRpc()
 */
public interface AuthServiceRpc {

    /**
     * 验证收到的 token 是否有效。
     *
     * @param token 待验证的 Access Token
     * @return 验证成功返回 Token 对应的 {@link AuthTokenData}，失败返回错误信息
     */
    ResponseData<AuthTokenData> verifyToken(String token);

    /**
     * 生成新的用户 ID。
     *
     * @return 新生成的用户 ID
     */
    ResponseData<Long> genUserId();

    /**
     * 生成 Guest（C 端访客）登录 Token。
     *
     * @param loginAgent        登录代理标识
     * @param clientAgent       客户端 User-Agent
     * @param loginType         登录类型
     * @param loginId           登录标识（用户名/手机号/邮箱）
     * @param saasId            运营商 ID
     * @param mchId             商户 ID
     * @param userId            用户 ID
     * @param userName          用户名
     * @param nickName          昵称
     * @param realName          真实姓名
     * @param mobile            手机号
     * @param email             邮箱
     * @param userIp            登录用户 IP
     * @param remark            备注
     * @param checkDoubleLogin  是否检查重复登录
     * @return 生成的 Token 响应
     */
    TokenResponse genGuestToken(String loginAgent, String clientAgent, int loginType, String loginId, long saasId, long mchId, long userId, String userName, String nickName, String realName, String mobile, String email, String userIp, String remark, boolean checkDoubleLogin);

    /**
     * 通知 auth-center 某 Guest 登录失败。
     *
     * @param loginAgent  登录代理标识
     * @param clientAgent 客户端 User-Agent
     * @param loginType   登录类型
     * @param loginId     登录标识
     * @param saasId      运营商 ID
     * @param mchId       商户 ID
     * @param userId      用户 ID
     * @param userName    用户名
     * @param nickName    昵称
     * @param realName    真实姓名
     * @param mobile      手机号
     * @param email       邮箱
     * @param userIp      登录用户 IP
     * @param remark      备注
     * @return 处理结果
     */
    ResponseData notifyGuestLoginFail(String loginAgent, String clientAgent, int loginType, String loginId, long saasId, long mchId, long userId, String userName, String nickName, String realName, String mobile, String email, String userIp, String remark);

    /**
     * 踢出指定的 Guest 用户，使其 Token 失效。
     *
     * @param loginAgent 登录代理标识
     * @param saasId     运营商 ID
     * @param userId     用户 ID
     * @param remark     踢出原因/备注
     * @return 处理结果
     */
    ResponseData kickoutGuest(String loginAgent, long saasId, long userId, String remark);


    /**
     * 获取指定应用集合的 Saas 权限配置（JSON）。
     *
     * @param appNames 应用名称数组
     * @return 权限配置 JSON 字符串
     */
    ResponseData<String> getAppSaasPerm(String[] appNames);

    /**
     * 初始化 Saas 权限，创建运营商管理员并分配应用权限。
     *
     * @param saasId       运营商 ID
     * @param saasName     Saas 名称
     * @param initAppNames 需初始化的应用名称数组
     * @param adminPasswd  管理员密码
     * @param adminMobile  管理员手机号
     * @param adminEmail   管理员邮箱
     * @return 初始化结果
     */
    ResponseData initSaasPerm(long saasId, String saasName, String[] initAppNames, String adminPasswd, String adminMobile, String adminEmail);

    /**
     * 更新 Saas 名称。
     *
     * @param saasId   运营商 ID
     * @param saasName 新的 Saas 名称
     * @return 更新结果
     */
    ResponseData updateSaasName(long saasId, String saasName);

    /**
     * 根据 Saas 访问域名获取 SaasId。
     *
     * @param saasHost Saas 访问域名
     * @return 对应的 SaasId
     */
    ResponseData<Long> getSaasIdByHost(String saasHost);

    /**
     * 授予 Saas 权限。
     *
     * @param saasId  运营商 ID
     * @param permIds 权限 ID 列表（逗号分隔）
     * @param remark  备注
     * @return 授权结果
     */
    ResponseData grantSaasPerm(long saasId, String permIds, String remark);

    /**
     * 撤销 Saas 权限。
     *
     * @param saasId  运营商 ID
     * @param permIds 权限 ID 列表（逗号分隔）
     * @param remark  备注
     * @return 撤销结果
     */
    ResponseData revokeSaasPerm(long saasId, String permIds, String remark);

    /**
     * 启用指定 Saas 的权限。
     *
     * @param saasId 运营商 ID
     * @param remark 备注
     * @return 启用结果
     */
    ResponseData enableSaasPerm(long saasId, String remark);

    /**
     * 停用指定 Saas 的权限。
     *
     * @param saasId 运营商 ID
     * @param remark 备注
     * @return 停用结果
     */
    ResponseData disableSaasPerm(long saasId, String remark);

    /**
     * 修改 Saas 用户数限制。
     *
     * @param saasId    运营商 ID
     * @param userLimit 用户数上限
     * @param remark    备注
     * @return 更新结果
     */
    ResponseData updateSaasUserLimit(long saasId, int userLimit, String remark);

    /**
     * 获取 Saas 用户数限制。
     *
     * @param saasId 运营商 ID
     * @return 用户数上限
     */
    ResponseData<Integer> getSaasUserLimit(long saasId);

    /**
     * 创建 Saas 用户。
     *
     * @param mscUserRegister 用户注册信息
     * @return 创建结果，data 为新用户 ID
     */
    ResponseData createUser(MscUserRegister mscUserRegister);

    /**
     * 获取用户信息。
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    ResponseData<MscUserVo> loadUser(long userId);

    /**
     * 按多条件查询 Saas 用户列表。
     *
     * @param saasId   运营商 ID
     * @param userType 用户类型
     * @param mchId    商户 ID
     * @param groupId  用户组 ID
     * @param userId   用户 ID
     * @param userName 用户名（模糊）
     * @param nickName 昵称（模糊）
     * @param realName 真实姓名（模糊）
     * @param mobile   手机号（模糊）
     * @param email    邮箱（模糊）
     * @return 用户列表
     */
    ResponseData<List<MscUserVo>> listUser(long saasId, int userType, long mchId, long groupId, long userId, String userName, String nickName, String realName, String mobile, String email);

    /**
     * 按多条件查询 Saas 用户组列表。
     *
     * @param saasId    运营商 ID
     * @param userType  用户类型
     * @param mchId     商户 ID
     * @param groupId   用户组 ID
     * @param groupName 用户组名称（模糊）
     * @return 用户组列表
     */
    ResponseData<List<MscUserGroupVo>> listUserGroup(long saasId, int userType, long mchId, long groupId, String groupName);


}
