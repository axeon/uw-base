package uw.gateway.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import uw.common.response.ResponseData;
import uw.gateway.client.conf.UwGatewayProperties;

import java.util.Date;


/**
 * 短链帮助类。
 */
public class GatewayClientHelper {

    private static final Logger log = LoggerFactory.getLogger(GatewayClientHelper.class);

    private static RestClient authRestClient;

    private static UwGatewayProperties uwGatewayProperties;

    public GatewayClientHelper(UwGatewayProperties uwGatewayProperties, RestClient authRestClient) {
        GatewayClientHelper.authRestClient = authRestClient;
        GatewayClientHelper.uwGatewayProperties = uwGatewayProperties;
    }

    public static ResponseData updateSaasRateLimit(long saasId, int limitSeconds, int limitRequests, int limitBytes, Date expireDate, String remark) {
        String targetUrl = uwGatewayProperties.getGatewayCenterHost() + "/rpc/service/updateSaasRateLimit";
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("saasId", String.valueOf(saasId));
            formData.add("limitSeconds", String.valueOf(limitSeconds));
            formData.add("limitRequests", String.valueOf(limitRequests));
            formData.add("limitBytes", String.valueOf(limitBytes));
            formData.add("expireDate", String.valueOf(expireDate.getTime()));
            formData.add("remark", remark);
            ResponseData result = authRestClient.put()
                    .uri(targetUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(ResponseData.class);
            if (result == null) {
                return ResponseData.errorMsg("GatewayClientHelper.updateSaasRateLimit() returned null body");
            }
            return result;
        } catch (Exception e) {
            log.error("GatewayClientHelper.updateSaasRateLimit()异常: {}", e.getMessage(), e);
            return ResponseData.errorMsg("GatewayClientHelper.updateSaasRateLimit()异常: " + e.getMessage());
        }
    }

    public static ResponseData clearSaasRateLimit(long saasId, String remark) {
        String targetUrl = uwGatewayProperties.getGatewayCenterHost() + "/rpc/service/clearSaasRateLimit";
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("saasId", String.valueOf(saasId));
            formData.add("remark", remark);
            ResponseData result = authRestClient.put()
                    .uri(targetUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(ResponseData.class);
            if (result == null) {
                return ResponseData.errorMsg("GatewayClientHelper.clearSaasRateLimit() returned null body");
            }
            return result;
        } catch (Exception e) {
            log.error("GatewayClientHelper.clearSaasRateLimit()异常: {}", e.getMessage(), e);
            return ResponseData.errorMsg("GatewayClientHelper.clearSaasRateLimit()异常: " + e.getMessage());
        }
    }

}
