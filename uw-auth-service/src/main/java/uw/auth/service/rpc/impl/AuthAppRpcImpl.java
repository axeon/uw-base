package uw.auth.service.rpc.impl;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uw.auth.service.conf.AuthServiceProperties;
import uw.auth.service.rpc.AuthAppRpc;
import uw.auth.service.vo.*;
import uw.common.dto.ResponseData;

import java.net.URI;

/**
 * auth-server向auth-center RPC实现
 *
 * @author axeon
 */
public class AuthAppRpcImpl implements AuthAppRpc {

    /**
     * 属性配置器
     */
    private AuthServiceProperties authServiceProperties;

    /**
     * RPC Client
     */
    private RestTemplate restTemplate;

    /**
     * @param authServiceProperties
     * @param restTemplate
     */
    public AuthAppRpcImpl(final AuthServiceProperties authServiceProperties, final RestTemplate restTemplate) {
        this.authServiceProperties = authServiceProperties;
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
        return restTemplate.postForObject( authServiceProperties.getAuthCenterHost() + "/rpc/app/regApp", appRegRequest, AppRegResponse.class );
    }

    /**
     * 报告状态，同时拉取非法TokenData。
     *
     * @param mscAppReportRequest
     * @return
     */
    @Override
    public MscAppReportResponse reportStatus(MscAppReportRequest mscAppReportRequest) {
        return restTemplate.postForObject( authServiceProperties.getAuthCenterHost() + "/rpc/app/reportStatus", mscAppReportRequest, MscAppReportResponse.class );
    }

    /**
     * 更新mscPerm授权状态。
     *
     * @return
     */
    @Override
    public ResponseData updatePermLicense(MscPermLicenseRequest mscPermLicenseRequest) {
        return restTemplate.postForObject( authServiceProperties.getAuthCenterHost() + "/rpc/app/updatePermLicense", mscPermLicenseRequest, ResponseData.class );
    }

}
