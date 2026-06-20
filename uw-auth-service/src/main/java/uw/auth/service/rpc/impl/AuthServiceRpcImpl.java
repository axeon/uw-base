package uw.auth.service.rpc.impl;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import uw.auth.client.vo.TokenResponse;
import uw.auth.service.conf.AuthServiceProperties;
import uw.auth.service.rpc.AuthServiceRpc;
import uw.auth.service.token.AuthTokenData;
import uw.auth.service.vo.MscUserGroupVo;
import uw.auth.service.vo.MscUserRegister;
import uw.auth.service.vo.MscUserVo;
import uw.common.response.ResponseData;

import java.util.List;

/**
 * {@link AuthServiceRpc} 的默认实现。
 * <p>
 * 基于 {@code RestClient} 以 form / query / json 形式调用 auth-center 的 {@code /rpc/service/*} 接口。
 *
 * @author axeon
 */
public class AuthServiceRpcImpl implements AuthServiceRpc {

    private final AuthServiceProperties authServiceProperties;
    private final RestClient authRestClient;

    public AuthServiceRpcImpl(final AuthServiceProperties authServiceProperties, final RestClient authRestClient) {
        this.authServiceProperties = authServiceProperties;
        this.authRestClient = authRestClient;
    }

    /**
     * 验证token有效性。
     *
     * @param token 认证token
     * @return token数据
     */
    @Override
    public ResponseData<AuthTokenData> verifyToken(String token) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("token", token);
        return authRestClient.post()
                .uri(baseUrl + "/rpc/service/verifyToken")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<AuthTokenData>>() {});
    }

    /**
     * 生成用户ID。
     *
     * @return 新用户ID
     */
    @Override
    public ResponseData<Long> genUserId() {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        return authRestClient.get()
                .uri(baseUrl + "/rpc/service/genUserId")
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<Long>>() {});
    }

    /**
     * 生成访客token。
     *
     * @param loginAgent 登录代理
     * @param clientAgent 客户端代理
     * @param loginType 登录类型
     * @param loginId 登录ID
     * @param saasId SaaS ID
     * @param mchId 商户ID
     * @param userId 用户ID
     * @param userName 用户名
     * @param nickName 昵称
     * @param realName 真实姓名
     * @param mobile 手机号
     * @param email 邮箱
     * @param userIp 用户IP
     * @param remark 备注
     * @param checkDoubleLogin 是否检查重复登录
     * @return token响应
     */
    @Override
    public TokenResponse genGuestToken(String loginAgent, String clientAgent, int loginType, String loginId, long saasId, long mchId, long userId, String userName, String nickName, String realName, String mobile, String email, String userIp, String remark, boolean checkDoubleLogin) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("loginAgent", loginAgent);
        formData.add("clientAgent", clientAgent);
        formData.add("loginType", String.valueOf(loginType));
        formData.add("loginId", loginId);
        formData.add("saasId", String.valueOf(saasId));
        formData.add("mchId", String.valueOf(mchId));
        formData.add("userId", String.valueOf(userId));
        formData.add("userName", userName);
        formData.add("nickName", nickName);
        formData.add("realName", realName);
        formData.add("mobile", mobile);
        formData.add("email", email);
        formData.add("userIp", userIp);
        formData.add("remark", remark);
        formData.add("checkDoubleLogin", String.valueOf(checkDoubleLogin));
        return authRestClient.post()
                .uri(baseUrl + "/rpc/service/genGuestToken")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(TokenResponse.class);
    }

    /**
     * 通知访客登录失败。
     *
     * @param loginAgent 登录代理
     * @param clientAgent 客户端代理
     * @param loginType 登录类型
     * @param loginId 登录ID
     * @param saasId SaaS ID
     * @param mchId 商户ID
     * @param userId 用户ID
     * @param userName 用户名
     * @param nickName 昵称
     * @param realName 真实姓名
     * @param mobile 手机号
     * @param email 邮箱
     * @param userIp 用户IP
     * @param remark 备注
     * @return 处理结果
     */
    @Override
    public ResponseData notifyGuestLoginFail(String loginAgent, String clientAgent, int loginType, String loginId, long saasId, long mchId, long userId, String userName, String nickName, String realName, String mobile, String email, String userIp, String remark) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("loginAgent", loginAgent);
        formData.add("clientAgent", clientAgent);
        formData.add("loginType", String.valueOf(loginType));
        formData.add("loginId", loginId);
        formData.add("saasId", String.valueOf(saasId));
        formData.add("mchId", String.valueOf(mchId));
        formData.add("userId", String.valueOf(userId));
        formData.add("userName", userName);
        formData.add("nickName", nickName);
        formData.add("realName", realName);
        formData.add("mobile", mobile);
        formData.add("email", email);
        formData.add("userIp", userIp);
        formData.add("remark", remark);
        return authRestClient.post()
                .uri(baseUrl + "/rpc/service/notifyGuestLoginFail")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(ResponseData.class);
    }

    /**
     * 踢出访客。
     *
     * @param loginAgent 登录代理
     * @param saasId SaaS ID
     * @param userId 用户ID
     * @param remark 备注
     * @return 处理结果
     */
    @Override
    public ResponseData kickoutGuest(String loginAgent, long saasId, long userId, String remark) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        return authRestClient.post()
                .uri(baseUrl + "/rpc/service/kickoutGuest?loginAgent={loginAgent}&saasId={saasId}&userId={userId}&remark={remark}",
                        loginAgent, saasId, userId, remark)
                .retrieve()
                .body(ResponseData.class);
    }

    /**
     * 获取应用SaaS权限配置。
     *
     * @param appNames 应用名称数组
     * @return 权限配置JSON
     */
    @Override
    public ResponseData<String> getAppSaasPerm(String[] appNames) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        //使用UriComponentsBuilder正确编码数组参数，避免数组toString成内存地址。
        org.springframework.web.util.UriComponentsBuilder builder = org.springframework.web.util.UriComponentsBuilder.fromUriString(baseUrl + "/rpc/service/getAppSaasPerm");
        if (appNames != null) {
            for (String name : appNames) {
                builder.queryParam("appNames", name);
            }
        }
        return authRestClient.get()
                .uri(builder.build().encode().toUri())
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<String>>() {});
    }

    /**
     * 初始化SaaS权限。
     *
     * @param saasId SaaS ID
     * @param saasName SaaS名称
     * @param initAppNames 初始化应用名称数组
     * @param adminPasswd 管理员密码
     * @param adminMobile 管理员手机号
     * @param adminEmail 管理员邮箱
     * @return 初始化结果
     */
    @Override
    public ResponseData initSaasPerm(long saasId, String saasName, String[] initAppNames, String adminPasswd, String adminMobile, String adminEmail) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("adminPasswd", adminPasswd);
        formData.add("adminMobile", adminMobile);
        formData.add("adminEmail", adminEmail);
        return authRestClient.post()
                .uri(baseUrl + "/rpc/service/initSaasPerm?saasId={saasId}&saasName={saasName}&initAppNames={initAppNames}", saasId, saasName, initAppNames)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(ResponseData.class);
    }

    /**
     * 更新SaaS名称。
     *
     * @param saasId SaaS ID
     * @param saasName SaaS名称
     * @return 更新结果
     */
    @Override
    public ResponseData updateSaasName(long saasId, String saasName) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        return authRestClient.put()
                .uri(baseUrl + "/rpc/service/updateSaasName?saasId={saasId}&saasName={saasName}", saasId, saasName)
                .retrieve()
                .body(ResponseData.class);
    }

    /**
     * 根据域名获取SaaS ID。
     *
     * @param saasHost SaaS域名
     * @return SaaS ID
     */
    @Override
    public ResponseData<Long> getSaasIdByHost(String saasHost) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        return authRestClient.get()
                .uri(baseUrl + "/rpc/service/getSaasIdByHost?saasHost={saasHost}", saasHost)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<Long>>() {});
    }

    /**
     * 授予SaaS权限。
     *
     * @param saasId SaaS ID
     * @param permIds 权限ID列表
     * @param remark 备注
     * @return 授权结果
     */
    @Override
    public ResponseData grantSaasPerm(long saasId, String permIds, String remark) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        return authRestClient.put()
                .uri(baseUrl + "/rpc/service/grantSaasPerm?saasId={saasId}&permIds={permIds}&remark={remark}", saasId, permIds, remark)
                .retrieve()
                .body(ResponseData.class);
    }

    /**
     * 撤销SaaS权限。
     *
     * @param saasId SaaS ID
     * @param permIds 权限ID列表
     * @param remark 备注
     * @return 撤销结果
     */
    @Override
    public ResponseData revokeSaasPerm(long saasId, String permIds, String remark) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        return authRestClient.put()
                .uri(baseUrl + "/rpc/service/revokeSaasPerm?saasId={saasId}&permIds={permIds}&remark={remark}", saasId, permIds, remark)
                .retrieve()
                .body(ResponseData.class);
    }

    /**
     * 启用SaaS权限。
     *
     * @param saasId SaaS ID
     * @param remark 备注
     * @return 启用结果
     */
    @Override
    public ResponseData enableSaasPerm(long saasId, String remark) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        return authRestClient.put()
                .uri(baseUrl + "/rpc/service/enableSaasPerm?saasId={saasId}&remark={remark}", saasId, remark)
                .retrieve()
                .body(ResponseData.class);
    }

    /**
     * 禁用SaaS权限。
     *
     * @param saasId SaaS ID
     * @param remark 备注
     * @return 禁用结果
     */
    @Override
    public ResponseData disableSaasPerm(long saasId, String remark) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        return authRestClient.put()
                .uri(baseUrl + "/rpc/service/disableSaasPerm?saasId={saasId}&remark={remark}", saasId, remark)
                .retrieve()
                .body(ResponseData.class);
    }

    /**
     * 更新SaaS用户数量限制。
     *
     * @param saasId SaaS ID
     * @param userLimit 用户数量限制
     * @param remark 备注
     * @return 更新结果
     */
    @Override
    public ResponseData updateSaasUserLimit(long saasId, int userLimit, String remark) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        return authRestClient.put()
                .uri(baseUrl + "/rpc/service/updateSaasUserLimit?saasId={saasId}&remark={remark}&userLimit={userLimit}", saasId, remark, userLimit)
                .retrieve()
                .body(ResponseData.class);
    }

    /**
     * 获取SaaS用户数量限制。
     *
     * @param saasId SaaS ID
     * @return 用户数量限制
     */
    @Override
    public ResponseData<Integer> getSaasUserLimit(long saasId) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        return authRestClient.get()
                .uri(baseUrl + "/rpc/service/getSaasUserLimit?saasId={saasId}", saasId)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<Integer>>() {});
    }

    /**
     * 创建用户。
     *
     * @param mscUserRegister 用户注册信息
     * @return 创建结果
     */
    @Override
    public ResponseData createUser(MscUserRegister mscUserRegister) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        return authRestClient.post()
                .uri(baseUrl + "/rpc/service/createUser")
                .body(mscUserRegister)
                .retrieve()
                .body(ResponseData.class);
    }

    /**
     * 加载用户信息。
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @Override
    public ResponseData<MscUserVo> loadUser(long userId) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        return authRestClient.get()
                .uri(baseUrl + "/rpc/service/loadUser?userId={userId}", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<MscUserVo>>() {});
    }

    /**
     * 查询用户列表。
     *
     * @param saasId SaaS ID
     * @param userType 用户类型
     * @param mchId 商户ID
     * @param groupId 分组ID
     * @param userId 用户ID
     * @param userName 用户名
     * @param nickName 昵称
     * @param realName 真实姓名
     * @param mobile 手机号
     * @param email 邮箱
     * @return 用户列表
     */
    @Override
    public ResponseData<List<MscUserVo>> listUser(long saasId, int userType, long mchId, long groupId, long userId, String userName, String nickName, String realName, String mobile, String email) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        return authRestClient.get()
                .uri(baseUrl + "/rpc/service/listUser?saasId={saasId}&userType={userType}&mchId={mchId}&groupId={groupId}&userId={userId}&userName={userName}&nickName={nickName}&realName={realName}&mobile={mobile}&email={email}",
                        saasId, userType, mchId, groupId, userId, userName, nickName, realName, mobile, email)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<List<MscUserVo>>>() {});
    }

    /**
     * 查询用户分组列表。
     *
     * @param saasId SaaS ID
     * @param userType 用户类型
     * @param mchId 商户ID
     * @param groupId 分组ID
     * @param groupName 分组名称
     * @return 用户分组列表
     */
    @Override
    public ResponseData<List<MscUserGroupVo>> listUserGroup(long saasId, int userType, long mchId, long groupId, String groupName) {
        String baseUrl = authServiceProperties.getAuthCenterHost();
        return authRestClient.get()
                .uri(baseUrl + "/rpc/service/listUserGroup?saasId={saasId}&userType={userType}&mchId={mchId}&groupId={groupId}&groupName={groupName}",
                        saasId, userType, mchId, groupId, groupName)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<List<MscUserGroupVo>>>() {});
    }

}
