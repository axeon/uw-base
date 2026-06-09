package uw.mydb.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import uw.common.response.ResponseData;
import uw.mydb.client.conf.UwMydbClientProperties;
import uw.mydb.client.vo.DataNode;

import java.io.Serializable;

/**
 * MydbHelper。
 */
public class MydbClientHelper {

    private static final Logger log = LoggerFactory.getLogger(MydbClientHelper.class);
    private static final String DEFAULT_CONFIG = "default";

    private static RestClient authRestClient;

    private static UwMydbClientProperties uwMydbClientProperties;

    public MydbClientHelper(UwMydbClientProperties uwMydbClientProperties, RestClient authRestClient) {
        MydbClientHelper.uwMydbClientProperties = uwMydbClientProperties;
        MydbClientHelper.authRestClient = authRestClient;
    }

    public static ResponseData<DataNode> assignSaasNode(Serializable saasId) {
        return assignSaasNode(DEFAULT_CONFIG, saasId, null);
    }

    public static ResponseData<DataNode> assignSaasNode(Serializable saasId, String preferNode) {
        return assignSaasNode(DEFAULT_CONFIG, saasId, preferNode);
    }

    public static ResponseData<DataNode> assignSaasNode(String configKey, Serializable saasId, String preferNode) {
        String targetUrl = uwMydbClientProperties.getMydbCenterHost() + "/rpc/app/assignSaasNode";
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("configKey", configKey);
            formData.add("saasId", String.valueOf(saasId));
            if (preferNode != null) {
                formData.add("preferNode", preferNode);
            }
            ResponseData<DataNode> result = authRestClient.post()
                    .uri(targetUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ResponseData<DataNode>>() {});
            if (result == null) {
                return ResponseData.errorMsg("MydbClientHelper.assignSaasNode() returned null body");
            }
            return result;
        } catch (Exception e) {
            log.error("MydbClientHelper.assignSaasNode()异常: {}", e.getMessage(), e);
            return ResponseData.errorMsg("MydbClientHelper.assignSaasNode()异常: " + e.getMessage());
        }
    }

}
