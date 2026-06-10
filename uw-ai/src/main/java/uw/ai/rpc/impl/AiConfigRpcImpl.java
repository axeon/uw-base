package uw.ai.rpc.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import uw.ai.conf.UwAiProperties;
import uw.ai.rpc.AiConfigRpc;
import uw.ai.vo.AiApiConfigVo;
import uw.ai.vo.AiModelConfigVo;
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
    public ResponseData<List<AiModelConfigVo>> listModelConfig() {
        return authRestTemplate.exchange(
                uwAiProperties.getAiCenterHost() + "/rpc/config/listModelConfig",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<ResponseData<List<AiModelConfigVo>>>() {}
        ).getBody();
    }

    @Override
    public ResponseData<List<AiApiConfigVo>> listApiConfig() {
        return authRestTemplate.exchange(
                uwAiProperties.getAiCenterHost() + "/rpc/config/listApiConfig",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<ResponseData<List<AiApiConfigVo>>>() {}
        ).getBody();
    }
}