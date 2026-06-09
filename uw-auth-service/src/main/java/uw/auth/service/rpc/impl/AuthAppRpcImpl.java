package uw.auth.service.rpc.impl;

import org.springframework.web.client.RestClient;
import uw.auth.service.conf.AuthServiceProperties;
import uw.auth.service.rpc.AuthAppRpc;
import uw.auth.service.vo.*;
import uw.common.response.ResponseData;

/**
 * auth-server向auth-center RPC实现
 *
 * @author axeon
 */
public class AuthAppRpcImpl implements AuthAppRpc {

    private final AuthServiceProperties authServiceProperties;
    private final RestClient authRestClient;

    public AuthAppRpcImpl(final AuthServiceProperties authServiceProperties, final RestClient authRestClient) {
        this.authServiceProperties = authServiceProperties;
        this.authRestClient = authRestClient;
    }

    @Override
    public MscAppRegResponse regApp(MscAppRegRequest appRegRequest) {
        return authRestClient.post()
                .uri(authServiceProperties.getAuthCenterHost() + "/rpc/app/regApp")
                .body(appRegRequest)
                .retrieve()
                .body(MscAppRegResponse.class);
    }

    @Override
    public MscAppReportResponse reportStatus(MscAppReportRequest mscAppReportRequest) {
        return authRestClient.post()
                .uri(authServiceProperties.getAuthCenterHost() + "/rpc/app/reportStatus")
                .body(mscAppReportRequest)
                .retrieve()
                .body(MscAppReportResponse.class);
    }

    @Override
    public ResponseData updatePermLicense(MscPermLicenseRequest mscPermLicenseRequest) {
        return authRestClient.put()
                .uri(authServiceProperties.getAuthCenterHost() + "/rpc/app/updatePermLicense")
                .body(mscPermLicenseRequest)
                .retrieve()
                .body(ResponseData.class);
    }

}
