package uw.ai.rpc.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import uw.ai.conf.UwAiProperties;
import uw.ai.rpc.AiConfigRpc;
import uw.ai.vo.AiApiConfigVo;
import uw.ai.vo.AiModelConfigVo;
import uw.common.dto.ResponseData;

import java.util.List;

/**
 * AI配置查询RPC客户端实现。
 */
public class AiConfigRpcImpl implements AiConfigRpc {

    private static final Logger logger = LoggerFactory.getLogger(AiConfigRpcImpl.class);

    private final RestTemplate authRestTemplate;

    private final UwAiProperties uwAiProperties;

    public AiConfigRpcImpl(UwAiProperties uwAiProperties, RestTemplate authRestTemplate) {
        this.uwAiProperties = uwAiProperties;
        this.authRestTemplate = authRestTemplate;
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
