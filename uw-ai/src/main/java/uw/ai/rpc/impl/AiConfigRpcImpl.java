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
    public ResponseData<List<AiModelInfoVo>> listModelConfigBySaas(Long saasId, Long mchId) {
        ResponseData<List<AiModelInfoVo>> result = authRestClient.get()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/config/listModelConfigBySaas?saasId={saasId}&mchId={mchId}", saasId, mchId)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<List<AiModelInfoVo>>>() {});
        if (result == null) {
            return ResponseData.errorMsg("AiConfigRpcImpl.listModelConfigBySaas() returned null body");
        }
        return result;
    }

    @Override
    public ResponseData<List<AiModelInfoVo>> listModelConfigByApi(Long apiId, String apiCode) {
        ResponseData<List<AiModelInfoVo>> result = authRestClient.get()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/config/listModelConfigByApi?apiId={apiId}&apiCode={apiCode}", apiId, apiCode)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<List<AiModelInfoVo>>>() {});
        if (result == null) {
            return ResponseData.errorMsg("AiConfigRpcImpl.listModelConfigByApi() returned null body");
        }
        return result;
    }

    @Override
    public ResponseData<AiModelInfoVo> getModelConfig(Long id, String configCode) {
        ResponseData<AiModelInfoVo> result = authRestClient.get()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/config/getModelConfig?id={id}&configCode={configCode}", id, configCode)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<AiModelInfoVo>>() {});
        if (result == null) {
            return ResponseData.errorMsg("AiConfigRpcImpl.getModelConfig() returned null body");
        }
        return result;
    }

    @Override
    public ResponseData<List<AiModelInfoVo>> listModelConfigByType(String modelType, String modelTag) {
        ResponseData<List<AiModelInfoVo>> result = authRestClient.get()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/config/listModelConfigByType?modelType={modelType}&modelTag={modelTag}", modelType, modelTag)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<List<AiModelInfoVo>>>() {});
        if (result == null) {
            return ResponseData.errorMsg("AiConfigRpcImpl.listModelConfigByType() returned null body");
        }
        return result;
    }

    @Override
    public ResponseData<List<AiModelApiVo>> listApiConfigBySaas(Long saasId, Long mchId) {
        ResponseData<List<AiModelApiVo>> result = authRestClient.get()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/config/listApiConfigBySaas?saasId={saasId}&mchId={mchId}", saasId, mchId)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<List<AiModelApiVo>>>() {});
        if (result == null) {
            return ResponseData.errorMsg("AiConfigRpcImpl.listApiConfigBySaas() returned null body");
        }
        return result;
    }

    @Override
    public ResponseData<AiModelApiVo> getApiConfig(Long id, String apiCode) {
        ResponseData<AiModelApiVo> result = authRestClient.get()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/config/getApiConfig?id={id}&apiCode={apiCode}", id, apiCode)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<AiModelApiVo>>() {});
        if (result == null) {
            return ResponseData.errorMsg("AiConfigRpcImpl.getApiConfig() returned null body");
        }
        return result;
    }
}
