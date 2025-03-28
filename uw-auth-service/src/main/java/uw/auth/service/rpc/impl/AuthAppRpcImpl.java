package uw.auth.service.rpc.impl;

import org.springframework.web.client.RestTemplate;
import uw.auth.service.conf.AuthServiceProperties;
import uw.auth.service.rpc.AuthAppRpc;
import uw.auth.service.vo.*;
import uw.common.dto.ResponseData;

/**
 * auth-server向auth-center RPC实现
 *
 * @author axeon
 */
public class AuthAppRpcImpl implements AuthAppRpc {

    /**
     * 属性配置器
     */
    private final AuthServiceProperties authServiceProperties;

    /**
     * RPC Client
     */
    private final RestTemplate tokenRestTemplate;

    /**
     * @param authServiceProperties
     * @param tokenRestTemplate
     */
    public AuthAppRpcImpl(final AuthServiceProperties authServiceProperties, final RestTemplate tokenRestTemplate) {
        this.authServiceProperties = authServiceProperties;
        this.tokenRestTemplate = tokenRestTemplate;
    }

    /**
     * 发布当前App
     *
     * @param appRegRequest
     * @return
     */
    @Override
    public AppRegResponse regApp(AppRegRequest appRegRequest) {
        return tokenRestTemplate.postForObject( authServiceProperties.getAuthCenterHost() + "/rpc/app/regApp", appRegRequest, AppRegResponse.class );
    }

    /**
     * 报告状态，同时拉取非法TokenData。
     *
     * @param mscAppReportRequest
     * @return
     */
    @Override
    public MscAppReportResponse reportStatus(MscAppReportRequest mscAppReportRequest) {
        return tokenRestTemplate.postForObject( authServiceProperties.getAuthCenterHost() + "/rpc/app/reportStatus", mscAppReportRequest, MscAppReportResponse.class );
    }

    /**
     * 更新mscPerm授权状态。
     *
     * @return
     */
    @Override
    public ResponseData updatePermLicense(MscPermLicenseRequest mscPermLicenseRequest) {
        return tokenRestTemplate.postForObject( authServiceProperties.getAuthCenterHost() + "/rpc/app/updatePermLicense", mscPermLicenseRequest, ResponseData.class );
    }

}
