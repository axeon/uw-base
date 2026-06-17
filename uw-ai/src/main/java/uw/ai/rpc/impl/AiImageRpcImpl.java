package uw.ai.rpc.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import uw.ai.conf.UwAiProperties;
import uw.ai.rpc.AiImageRpc;
import uw.ai.vo.AiImageGenerateParam;
import uw.ai.vo.AiImageResultData;
import uw.common.response.ResponseData;

/**
 * AiImageRpcImpl — AI图片生成。
 */
public class AiImageRpcImpl implements AiImageRpc {

    private static final Logger logger = LoggerFactory.getLogger(AiImageRpcImpl.class);

    private final RestClient authRestClient;
    private final UwAiProperties uwAiProperties;

    public AiImageRpcImpl(UwAiProperties uwAiProperties, RestClient authRestClient) {
        this.uwAiProperties = uwAiProperties;
        this.authRestClient = authRestClient;
    }

    @Override
    public ResponseData<AiImageResultData> generate(AiImageGenerateParam param) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("configId", param.getConfigId());
        if (param.getConfigCode() != null) {
            body.add("configCode", param.getConfigCode());
        }
        if (param.getSessionId() > 0) {
            body.add("sessionId", param.getSessionId());
        }
        if (param.getUserPrompt() != null) {
            body.add("prompt", param.getUserPrompt());
        }

        ResponseData<AiImageResultData> result = authRestClient.post()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/image/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<AiImageResultData>>() {});
        if (result == null) {
            return ResponseData.errorMsg("AiImageRpcImpl.generate() returned null body");
        }
        return result;
    }
}
