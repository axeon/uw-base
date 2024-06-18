package uw.auth.service.rpc.impl;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uw.auth.service.conf.AuthServiceProperties;
import uw.common.dto.ResponseData;
import uw.auth.service.rpc.AuthServiceRpc;
import uw.auth.service.token.AuthTokenData;
import uw.auth.service.vo.*;

import java.net.URI;
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
    private AuthServiceProperties authServerProperties;

    /**
     * RPC Client
     */
    private RestTemplate restTemplate;

    /**
     * @param authServerProperties
     * @param restTemplate
     */
    public AuthServiceRpcImpl(final AuthServiceProperties authServerProperties, final RestTemplate restTemplate) {
        this.authServerProperties = authServerProperties;
        this.restTemplate = restTemplate;
    }

    /**
     * 发布当前App
     *
     * @param appRegRequest
     * @return
     */
    @Override
    public AppRegResponse regApp(AppRegRequest appRegRequest) {
        return restTemplate.postForObject( authServerProperties.getAuthCenterHost() + "/rpc/auth/regApp", appRegRequest, AppRegResponse.class );
    }

    /**
     * 报告状态，同时拉取非法TokenData。
     *
     * @param mscAppReportRequest
     * @return
     */
    @Override
    public MscAppReportResponse reportStatus(MscAppReportRequest mscAppReportRequest) {
        return restTemplate.postForObject( authServerProperties.getAuthCenterHost() + "/rpc/auth/reportStatus", mscAppReportRequest, MscAppReportResponse.class );
    }

    /**
     * 验证收到的token。
     *
     * @param token
     * @return
     */
    @Override
    public ResponseData<AuthTokenData> verifyToken(String token) {
        URI targetUrl =
                UriComponentsBuilder.fromHttpUrl( authServerProperties.getAuthCenterHost() ).path( "/rpc/auth/verifyToken" ).queryParam( "token", token ).build().encode().toUri();
        return restTemplate.exchange( targetUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<ResponseData<AuthTokenData>>() {
        } ).getBody();

    }

    /**
     * 获得应用的权限ID列表
     *
     * @param appNames
     * @return
     */
    @Override
    public ResponseData<String> getAppPermIdList(String[] appNames) {
        URI targetUrl =
                UriComponentsBuilder.fromHttpUrl( authServerProperties.getAuthCenterHost() ).path( "/rpc/auth/getAppPermIdList" ).queryParam( "appNames", appNames ).build().encode().toUri();
        return restTemplate.exchange( targetUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<ResponseData<String>>() {
        } ).getBody();
    }

    /**
     * 生成guest的id。
     *
     * @return
     */
    @Override
    public ResponseData<Long> genUserId() {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl( authServerProperties.getAuthCenterHost() ).path( "/rpc/auth/genUserId" ).build().encode().toUri();
        return restTemplate.exchange( targetUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<ResponseData<Long>>() {
        } ).getBody();
    }

    /**
     * 生成guest的token。
     *
     * @return
     */
    @Override
    public TokenResponse genGuestToken(long saasId, long mchId, long userId, String userName) {
        URI targetUrl =
                UriComponentsBuilder.fromHttpUrl( authServerProperties.getAuthCenterHost() ).path( "/rpc/auth/genGuestToken" ).queryParam( "saasId", saasId ).queryParam( "mchId"
                        , mchId ).queryParam( "userId", userId ).queryParam( "username", userName ).build().encode().toUri();
        return restTemplate.getForObject( targetUrl, TokenResponse.class );
    }

    /**
     * 生成guest的token。
     *
     * @return
     */
    @Override
    public TokenResponse genGuestToken(long saasId, long mchId, long userId, String userName,boolean checkDoubleLogin,String userIp) {
        URI targetUrl =
                UriComponentsBuilder.fromHttpUrl( authServerProperties.getAuthCenterHost() ).path( "/rpc/auth/genGuestToken" ).queryParam( "saasId", saasId ).queryParam( "mchId"
                        , mchId ).queryParam( "userId", userId ).queryParam( "username", userName ).queryParam( "checkDoubleLogin", checkDoubleLogin ).queryParam( "userIp", userIp ).build().encode().toUri();
        return restTemplate.getForObject( targetUrl, TokenResponse.class );
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
        URI targetUrl = UriComponentsBuilder.fromHttpUrl( authServerProperties.getAuthCenterHost() ).path( "/rpc/auth/initSaasPerm" ).queryParam( "saasId", saasId ).queryParam(
                "saasName", saasName ).queryParam( "initAppNames", initAppNames ).queryParam( "adminPasswd", adminPasswd ).queryParam( "adminMobile", adminMobile ).queryParam(
                "adminEmail", adminEmail ).build().encode().toUri();
        return restTemplate.exchange( targetUrl, HttpMethod.POST, HttpEntity.EMPTY, ResponseData.class ).getBody();
    }

    /**
     * 创建SaasUser。
     *
     * @param mscUserRegister
     * @return
     */
    @Override
    public ResponseData createSaasUser(MscUserRegister mscUserRegister) {
        return restTemplate.postForObject( authServerProperties.getAuthCenterHost() + "/rpc/auth/createSaasUser", mscUserRegister, ResponseData.class );
    }

    /**
     * 授予Saas权限。
     *
     * @return
     */
    @Override
    public ResponseData grantSaasPerm(long saasId, String permIds, String remark) {
        URI targetUrl =
                UriComponentsBuilder.fromHttpUrl( authServerProperties.getAuthCenterHost() ).path( "/rpc/auth/grantSaasPerm" ).queryParam( "saasId", saasId ).queryParam(
                        "permIds", permIds ).queryParam( "remark",remark ).build().encode().toUri();
        return restTemplate.exchange( targetUrl, HttpMethod.PUT, HttpEntity.EMPTY, ResponseData.class ).getBody();
    }

    /**
     * 撤销Saas权限。
     *
     * @return
     */
    @Override
    public ResponseData revokeSaasPerm(long saasId, String permIds, String remark) {
        URI targetUrl =
                UriComponentsBuilder.fromHttpUrl( authServerProperties.getAuthCenterHost() ).path( "/rpc/auth/revokeSaasPerm" ).queryParam( "saasId", saasId ).queryParam(
                        "permIds", permIds ).queryParam( "remark",remark ).build().encode().toUri();
        return restTemplate.exchange( targetUrl, HttpMethod.PUT, HttpEntity.EMPTY, ResponseData.class ).getBody();
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
        URI targetUrl =
                UriComponentsBuilder.fromHttpUrl( authServerProperties.getAuthCenterHost() ).path( "/rpc/auth/enableSaasPerm" ).queryParam( "saasId", saasId ).queryParam( "remark",remark ).build().encode().toUri();
        return restTemplate.exchange( targetUrl, HttpMethod.PUT, HttpEntity.EMPTY, ResponseData.class ).getBody();
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
        URI targetUrl =
                UriComponentsBuilder.fromHttpUrl( authServerProperties.getAuthCenterHost() ).path( "/rpc/auth/disableSaasPerm" ).queryParam( "saasId", saasId ).queryParam( "remark",remark ).build().encode().toUri();
        return restTemplate.exchange( targetUrl, HttpMethod.PUT, HttpEntity.EMPTY, ResponseData.class ).getBody();
    }

    /**
     * 运营商限速设置
     *
     * @param saasId
     * @param rateLimit
     * @param remark
     * @return
     */
    @Override
    public ResponseData<Integer> updateSaasRateLimit(long saasId, String rateLimit, String remark) {
        URI targetUrl =
                UriComponentsBuilder.fromHttpUrl( authServerProperties.getAuthCenterHost() ).path( "/rpc/auth/updateSaasRateLimit" ).queryParam( "saasId", saasId ).queryParam( "remark",remark ).queryParam( rateLimit, rateLimit ).build().encode().toUri();
        return restTemplate.exchange( targetUrl, HttpMethod.PUT, HttpEntity.EMPTY, ResponseData.class ).getBody();
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
    public ResponseData<Integer> updateSaasUserLimit(long saasId, int userLimit, String remark) {
        URI targetUrl =
                UriComponentsBuilder.fromHttpUrl( authServerProperties.getAuthCenterHost() ).path( "/rpc/auth/updateSaasUserLimit" ).queryParam( "saasId", saasId ).queryParam( "remark",remark ).queryParam(
                        "userLimit", userLimit ).build().encode().toUri();
        return restTemplate.exchange( targetUrl, HttpMethod.PUT, HttpEntity.EMPTY, ResponseData.class ).getBody();
    }

    /**
     * 获得Saas用户数限制。
     *
     * @param saasId
     */
    @Override
    public ResponseData<Integer> getSaasUserLimit(long saasId) {
        URI targetUrl =
                UriComponentsBuilder.fromHttpUrl( authServerProperties.getAuthCenterHost() ).path( "/rpc/auth/getSaasUserLimit" ).queryParam( "saasId", saasId ).build().encode().toUri();
        return restTemplate.exchange( targetUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<ResponseData<Integer>>() {
        } ).getBody();
    }

    /**
     * 获取用户信息
     *
     * @param userId
     * @return
     */
    @Override
    public ResponseData<MscUserVo> getMscUserByUserId(long userId) {
        URI targetUrl =
                UriComponentsBuilder.fromHttpUrl( authServerProperties.getAuthCenterHost() ).path( "/rpc/auth/getMscUserByUserId" ).queryParam( "userId", userId ).build().encode().toUri();
        return restTemplate.exchange( targetUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<ResponseData<MscUserVo>>() {
        } ).getBody();
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
    public ResponseData<List<MscUserVo>> getSaasUserList(long saasId, int userType, long mchId, long groupId, long userId, String userName, String nickName, String realName,
                                                         String mobile, String email, String wxId) {
        URI targetUrl =
                UriComponentsBuilder.fromHttpUrl( authServerProperties.getAuthCenterHost() ).path( "/rpc/auth/getSaasUserList" ).queryParam( "saasId", saasId ).queryParam(
                        "userType", userType ).queryParam( "mchId", mchId ).queryParam( "groupId", groupId ).queryParam( "userId", userId ).queryParam( "username", userName ).queryParam( "nickName", nickName ).queryParam( "realName", realName ).queryParam( "mobile", mobile ).queryParam( "email", email ).queryParam( "wxId", wxId ).build().encode().toUri();
        return restTemplate.exchange( targetUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<ResponseData<List<MscUserVo>>>() {
        } ).getBody();
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
    public ResponseData<List<MscUserGroupVo>> getSaasUserGroupList(long saasId, int userType, long mchId, long groupId, String groupName) {
        URI targetUrl =
                UriComponentsBuilder.fromHttpUrl( authServerProperties.getAuthCenterHost() ).path( "/rpc/auth/getSaasUserGroupList" ).queryParam( "saasId", saasId ).queryParam(
                        "userType", userType ).queryParam( "mchId", mchId ).queryParam( "groupId", groupId ).queryParam( "groupName", groupName ).build().encode().toUri();
        return restTemplate.exchange( targetUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<ResponseData<List<MscUserGroupVo>>>() {
        } ).getBody();
    }

    /**
     * 更新saas名称。
     *
     * @param saasId
     * @param saasName
     */
    @Override
    public ResponseData updateSaasName(long saasId, String saasName) {
        URI targetUrl =
                UriComponentsBuilder.fromHttpUrl( authServerProperties.getAuthCenterHost() ).path( "/rpc/auth/updateSaasName" ).queryParam( "saasId", saasId ).queryParam(
                        "saasName", saasName ).build().encode().toUri();
        return restTemplate.exchange( targetUrl, HttpMethod.PUT, HttpEntity.EMPTY, ResponseData.class ).getBody();
    }

    /**
     * 更新mscPerm授权状态。
     *
     * @return
     */
    @Override
    public ResponseData updatePermLicense(MscPermLicenseRequest mscPermLicenseRequest) {
        return restTemplate.postForObject( authServerProperties.getAuthCenterHost() + "/rpc/auth/updatePermLicense", mscPermLicenseRequest, ResponseData.class );
    }
}
