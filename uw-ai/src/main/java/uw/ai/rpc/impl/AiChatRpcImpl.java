package uw.ai.rpc.impl;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uw.ai.conf.UwAiProperties;
import uw.ai.rpc.AiChatRpc;
import uw.ai.vo.AiChatGenerateParam;
import uw.common.dto.ResponseData;

/**
 * AiChatRpcImpl.
 */
@Component
public class AiChatRpcImpl implements AiChatRpc {


    /**
     * Rest模板类
     */
    private final RestTemplate tokenRestTemplate;

    private final UwAiProperties uwAiProperties;

    public AiChatRpcImpl(RestTemplate tokenRestTemplate, UwAiProperties uwAiProperties) {
        this.tokenRestTemplate = tokenRestTemplate;
        this.uwAiProperties = uwAiProperties;
    }

    /**
     * 生成。
     *
     * @param param
     * @return
     */
    @Override
    public ResponseData<String> generate(AiChatGenerateParam param) {
        String url = uwAiProperties.getAiCenterHost() + "/rpc/chat/generate";
        return tokenRestTemplate.exchange( url, HttpMethod.POST, new HttpEntity<>( param ), new ParameterizedTypeReference<ResponseData<String>>() {
        } ).getBody();
    }

}
