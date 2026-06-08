package uw.gateway.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uw.common.dto.ResponseData;
import uw.gateway.client.conf.UwGatewayProperties;

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
     */
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
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            ResponseData result = authRestTemplate.exchange(targetUrl, HttpMethod.PUT, new HttpEntity<>(formData, headers), ResponseData.class).getBody();
            if (result == null) {
                return ResponseData.errorMsg("GatewayClientHelper.updateSaasRateLimit() returned null body");
            }
            return result;
        } catch (Exception e) {
            log.error("GatewayClientHelper.updateSaasRateLimit()异常: {}", e.getMessage(), e);
            return ResponseData.errorMsg("GatewayClientHelper.updateSaasRateLimit()异常: " + e.getMessage());
        }
    }

    /**
     * 清除运营商限速设置。
     */
    public static ResponseData clearSaasRateLimit(long saasId, String remark) {
        String targetUrl = uwGatewayProperties.getGatewayCenterHost() + "/rpc/service/clearSaasRateLimit";
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("saasId", String.valueOf(saasId));
            formData.add("remark", remark);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            ResponseData result = authRestTemplate.exchange(targetUrl, HttpMethod.PUT, new HttpEntity<>(formData, headers), ResponseData.class).getBody();
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
