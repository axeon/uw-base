package uw.gateway.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uw.common.dto.ResponseData;
import uw.gateway.client.conf.UwGatewayProperties;

import java.net.URI;
import java.util.Date;


/**
 * 短链帮助类。
 */
public class GatewayClientHelper {

    private static final Logger log = LoggerFactory.getLogger(GatewayClientHelper.class);
    /**
     * Rest模板类
     */
    private static RestTemplate authRestTemplate;

    private static UwGatewayProperties uwGatewayProperties;

    public GatewayClientHelper(UwGatewayProperties uwGatewayProperties, RestTemplate authRestTemplate) {
        GatewayClientHelper.authRestTemplate = authRestTemplate;
        GatewayClientHelper.uwGatewayProperties = uwGatewayProperties;
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
    public ResponseData updateSaasRateLimit(long saasId, int limitSeconds, int limitRequests, int limitBytes, Date expireDate, String remark) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(uwGatewayProperties.getGatewayCenterHost()).path("/rpc/service/updateSaasRateLimit").queryParam("saasId", saasId).queryParam("remark", remark).queryParam("limitSeconds", limitSeconds).queryParam("limitRequests", limitRequests).queryParam("expireDate", expireDate).queryParam("limitBytes", limitBytes).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.PUT, HttpEntity.EMPTY, ResponseData.class).getBody();
    }

    /**
     * 清除运营商限速设置。
     *
     * @param saasId
     * @param remark
     * @return
     */
    public ResponseData clearSaasRateLimit(long saasId, String remark) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl(uwGatewayProperties.getGatewayCenterHost()).path("/rpc/service/clearSaasRateLimit").queryParam("saasId", saasId).queryParam("remark", remark).build().encode().toUri();
        return authRestTemplate.exchange(targetUrl, HttpMethod.PUT, HttpEntity.EMPTY, ResponseData.class).getBody();
    }

}
