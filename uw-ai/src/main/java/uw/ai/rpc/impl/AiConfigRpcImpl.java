package uw.ai.rpc.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import uw.ai.conf.UwAiProperties;
import uw.ai.rpc.AiConfigRpc;
import uw.ai.vo.AiModelApiVo;
import uw.ai.vo.AiModelInfoVo;
import uw.common.response.ResponseData;

import java.util.List;

/**
 * AiConfigRpcImpl — 查询AI模型配置列表。
 */
public class AiConfigRpcImpl implements AiConfigRpc {

    private static final Logger logger = LoggerFactory.getLogger(AiConfigRpcImpl.class);

    private final RestClient authRestClient;
    private final UwAiProperties uwAiProperties;

    public AiConfigRpcImpl(UwAiProperties uwAiProperties, RestClient authRestClient) {
        this.uwAiProperties = uwAiProperties;
        this.authRestClient = authRestClient;
    }

    @Override
    public ResponseData<List<AiModelInfoVo>> listModelConfig() {
        ResponseData<List<AiModelInfoVo>> result = authRestClient.get()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/config/listModelConfig")
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<List<AiModelInfoVo>>>() {});
        if (result == null) {
            return ResponseData.errorMsg("AiConfigRpcImpl.listModelConfig() returned null body");
        }
        return result;
    }

    @Override
    public ResponseData<List<AiModelApiVo>> listApiConfig() {
        ResponseData<List<AiModelApiVo>> result = authRestClient.get()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/config/listApiConfig")
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<List<AiModelApiVo>>>() {});
        if (result == null) {
            return ResponseData.errorMsg("AiConfigRpcImpl.listApiConfig() returned null body");
        }
        return result;
    }
}
