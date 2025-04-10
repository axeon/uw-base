package uw.auth.service.rpc.impl;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uw.auth.client.vo.TokenResponse;
import uw.auth.service.conf.AuthServiceProperties;
import uw.auth.service.rpc.AuthServiceRpc;
import uw.auth.service.token.AuthTokenData;
import uw.auth.service.vo.MscUserGroupVo;
import uw.auth.service.vo.MscUserRegister;
import uw.auth.service.vo.MscUserVo;
import uw.common.dto.ResponseData;

import java.net.URI;
import java.util.Date;
import java.util.List;

/**
 * auth-server向auth-center RPC实现
 *
 * @author axeon
 */
public class AuthServiceRpcImpl implements AuthServiceRpc {

    /**
     * 属性配置器
     */
    private final AuthServiceProperties authServiceProperties;

    /**
     * RPC Client
     */
    private final RestTemplate authRestTemplate;

    /**
     * @param authServiceProperties
     * @param authRestTemplate
     */
    public AuthServiceRpcImpl(final AuthServiceProperties authServiceProperties, final RestTemplate authRestTemplate) {
        this.authServiceProperties = authServiceProperties;
        this.authRestTemplate = authRestTemplate;
    }

    /**
     * 验证收到的token。
     *
     * @param token
     * @return
     */
    @Override
    public ResponseData<AuthTokenData> verifyToken(String token) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/verifyToken").queryParam("token", token).build().encode().toUri();
        ResponseData<AuthTokenData> responseData = authRestTemplate.exchange(targetUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<ResponseData<AuthTokenData>>() {
        }).getBody();
        return responseData;
    }

    /**
     * 生成guest的id。
     *
     * @return
     */
    @Override
    public ResponseData<Long> genUserId() {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/genUserId").build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<ResponseData<Long>>() {
        }).getBody();
    }

    /**
     * 生成guest的token。
     *
     * @return
     */
    @Override
    public TokenResponse genGuestToken(String loginAgent, String clientAgent, int loginType, String loginId, long saasId, long mchId, long userId, String userName, String nickName, String realName, String mobile, String email,String userIp, String remark, boolean checkDoubleLogin) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/genGuestToken").queryParam("loginAgent", loginAgent).queryParam("clientAgent", clientAgent).queryParam("loginType", loginType).queryParam("loginId", loginId).queryParam("saasId", saasId).queryParam("mchId", mchId).queryParam("userId", userId).queryParam("userName", userName).queryParam("nickName", nickName).queryParam("realName", realName).queryParam("mobile", mobile).queryParam("email", email).queryParam("userIp", userIp).queryParam("remark", remark).queryParam("checkDoubleLogin", checkDoubleLogin).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.POST, HttpEntity.EMPTY, TokenResponse.class).getBody();
    }


    /**
     * 通知guest登录失败。
     *
     * @param loginAgent
     * @param saasId
     * @param mchId
     * @param userId
     * @param userName
     * @param nickName
     * @param realName
     * @param userIp     登录用户Ip。
     * @param remark
     * @return
     * @throws Exception
     */
    @Override
    public ResponseData notifyGuestLoginFail(String loginAgent, String clientAgent, int loginType, String loginId, long saasId, long mchId, long userId, String userName, String nickName, String realName, String mobile, String email, String userIp, String remark) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/notifyGuestLoginFail").queryParam("loginAgent", loginAgent).queryParam("clientAgent", clientAgent).queryParam("loginType", loginType).queryParam("loginId", loginId).queryParam("saasId", saasId).queryParam("mchId", mchId).queryParam("userId", userId).queryParam("userName", userName).queryParam("nickName", nickName).queryParam("realName", realName).queryParam("mobile", mobile).queryParam("email", email).queryParam("userIp", userIp).queryParam("remark", remark).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.POST, HttpEntity.EMPTY, ResponseData.class).getBody();
    }

    /**
     * 踢出Guest用户。
     *
     * @param loginAgent
     * @param saasId
     * @param userId
     * @param remark
     */
    @Override
    public ResponseData kickoutGuest(String loginAgent, long saasId, long userId, String remark) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/kickoutGuest").queryParam("loginAgent", loginAgent).queryParam("saasId", saasId).queryParam("userId", userId).queryParam("remark", remark).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.POST, HttpEntity.EMPTY, ResponseData.class).getBody();
    }

    /**
     * 初始化Saas权限。
     * 这里不赋权
     *
     * @param saasId
     * @param saasName saas名称。
     * @return
     */
    @Override
    public ResponseData initSaasPerm(long saasId, String saasName, String[] initAppNames, String adminPasswd, String adminMobile, String adminEmail) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/initSaasPerm").queryParam("saasId", saasId).queryParam("saasName", saasName).queryParam("initAppNames", (Object[]) initAppNames).queryParam("adminPasswd", adminPasswd).queryParam("adminMobile", adminMobile).queryParam("adminEmail", adminEmail).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.POST, HttpEntity.EMPTY, ResponseData.class).getBody();
    }

    /**
     * 授予Saas权限。
     *
     * @return
     */
    @Override
    public ResponseData grantSaasPerm(long saasId, String permIds, String remark) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/grantSaasPerm").queryParam("saasId", saasId).queryParam("permIds", permIds).queryParam("remark", remark).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.PUT, HttpEntity.EMPTY, ResponseData.class).getBody();
    }

    /**
     * 撤销Saas权限。
     *
     * @return
     */
    @Override
    public ResponseData revokeSaasPerm(long saasId, String permIds, String remark) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/revokeSaasPerm").queryParam("saasId", saasId).queryParam("permIds", permIds).queryParam("remark", remark).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.PUT, HttpEntity.EMPTY, ResponseData.class).getBody();
    }

    /**
     * 启用指定SAAS权限。
     *
     * @param saasId saas编号
     * @param remark
     * @return
     */
    @Override
    public ResponseData enableSaasPerm(long saasId, String remark) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/enableSaasPerm").queryParam("saasId", saasId).queryParam("remark", remark).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.PUT, HttpEntity.EMPTY, ResponseData.class).getBody();
    }

    /**
     * 停用指定SAAS权限。
     *
     * @param saasId saas编号
     * @param remark
     * @return
     */
    @Override
    public ResponseData disableSaasPerm(long saasId, String remark) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/disableSaasPerm").queryParam("saasId", saasId).queryParam("remark", remark).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.PUT, HttpEntity.EMPTY, ResponseData.class).getBody();
    }

    /**
     * 运营商限速设置。
     *
     * @param saasId
     * @param limitSeconds
     * @param limitRequests
     * @param limitBytes
     * @param remark
     * @return
     */
    @Override
    public ResponseData updateSaasRateLimit(long saasId, int limitSeconds, int limitRequests, int limitBytes, Date expireDate, String remark) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/updateSaasRateLimit").queryParam("saasId", saasId).queryParam("remark", remark).queryParam("limitSeconds", limitSeconds).queryParam("limitRequests", limitRequests).queryParam("expireDate", expireDate).queryParam("limitBytes", limitBytes).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.PUT, HttpEntity.EMPTY, ResponseData.class).getBody();
    }

    /**
     * 清除运营商限速设置。
     *
     * @param saasId
     * @param remark
     * @return
     */
    @Override
    public ResponseData clearSaasRateLimit(long saasId, String remark) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/clearSaasRateLimit").queryParam("saasId", saasId).queryParam("remark", remark).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.PUT, HttpEntity.EMPTY, ResponseData.class).getBody();
    }

    /**
     * 修改SAAS用户数限制。
     *
     * @param saasId
     * @param userLimit
     * @param remark
     * @return
     */
    @Override
    public ResponseData updateSaasUserLimit(long saasId, int userLimit, String remark) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/updateSaasUserLimit").queryParam("saasId", saasId).queryParam("remark", remark).queryParam("userLimit", userLimit).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.PUT, HttpEntity.EMPTY, ResponseData.class).getBody();
    }

    /**
     * 获得Saas用户数限制。
     *
     * @param saasId
     */
    @Override
    public ResponseData<Integer> getSaasUserLimit(long saasId) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/getSaasUserLimit").queryParam("saasId", saasId).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<ResponseData<Integer>>() {
        }).getBody();
    }

    /**
     * 创建SaasUser。
     *
     * @param mscUserRegister
     * @return
     */
    @Override
    public ResponseData createUser(MscUserRegister mscUserRegister) {
        return authRestTemplate.postForObject(authServiceProperties.getAuthCenterHost() + "/rpc/service/createUser", mscUserRegister, ResponseData.class);
    }

    /**
     * 获取用户信息
     *
     * @param userId
     * @return
     */
    @Override
    public ResponseData<MscUserVo> getUser(long userId) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/getUser").queryParam("userId", userId).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<ResponseData<MscUserVo>>() {
        }).getBody();
    }

    /**
     * 获得saas用户列表。
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
     * @param wxId
     */
    @Override
    public ResponseData<List<MscUserVo>> getUserList(long saasId, int userType, long mchId, long groupId, long userId, String userName, String nickName, String realName, String mobile, String email) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/getUserList").queryParam("saasId", saasId).queryParam("userType", userType).queryParam("mchId", mchId).queryParam("groupId", groupId).queryParam("userId", userId).queryParam("username", userName).queryParam("nickName", nickName).queryParam("realName", realName).queryParam("mobile", mobile).queryParam("email", email).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<ResponseData<List<MscUserVo>>>() {
        }).getBody();
    }

    /**
     * 获得saas用户组列表。
     *
     * @param saasId
     * @param mchId
     * @param groupId
     * @param groupName
     */
    @Override
    public ResponseData<List<MscUserGroupVo>> getUserGroupList(long saasId, int userType, long mchId, long groupId, String groupName) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/getUserGroupList").queryParam("saasId", saasId).queryParam("userType", userType).queryParam("mchId", mchId).queryParam("groupId", groupId).queryParam("groupName", groupName).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<ResponseData<List<MscUserGroupVo>>>() {
        }).getBody();
    }

    /**
     * 更新saas名称。
     *
     * @param saasId
     * @param saasName
     */
    @Override
    public ResponseData updateSaasName(long saasId, String saasName) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/updateSaasName").queryParam("saasId", saasId).queryParam("saasName", saasName).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.PUT, HttpEntity.EMPTY, ResponseData.class).getBody();
    }

    /**
     * 获得应用的权限ID列表
     *
     * @param appNames
     * @return
     */
    @Override
    public ResponseData<String> getAppSaasPerm(String[] appNames) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(authServiceProperties.getAuthCenterHost()).path("/rpc/service/getAppSaasPerm").queryParam("appNames", (Object[]) appNames).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<ResponseData<String>>() {
        }).getBody();
    }


}
