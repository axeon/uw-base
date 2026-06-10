package uw.ai.rpc.impl;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import uw.ai.conf.UwAiProperties;
import uw.ai.rpc.AiTranslateRpc;
import uw.ai.vo.AiTranslateListParam;
import uw.ai.vo.AiTranslateMapParam;
import uw.ai.vo.AiTranslateResultData;
import uw.common.response.ResponseData;

/**
 * AiToolRpcImpl.
 */
public class AiTranslateRpcImpl implements AiTranslateRpc {

    private final RestClient authRestClient;

    private final UwAiProperties uwAiProperties;

    public AiTranslateRpcImpl(UwAiProperties uwAiProperties, RestClient authRestClient) {
        this.authRestClient = authRestClient;
        this.uwAiProperties = uwAiProperties;
    }

    @Override
    public ResponseData<AiTranslateResultData[]> translateList(AiTranslateListParam param) {
        String url = uwAiProperties.getAiCenterHost() + "/rpc/translate/translateList";
        ResponseData<AiTranslateResultData[]> result = authRestClient.post()
                .uri(url)
                .body(param)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<AiTranslateResultData[]>>() {});
        if (result == null) {
            return ResponseData.errorMsg("AiTranslateRpcImpl.translateList() returned null body");
        }
        return result;
    }

    @Override
    public ResponseData<AiTranslateResultData[]> translateMap(AiTranslateMapParam param) {
        String url = uwAiProperties.getAiCenterHost() + "/rpc/translate/translateMap";
        ResponseData<AiTranslateResultData[]> result = authRestClient.post()
                .uri(url)
                .body(param)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<AiTranslateResultData[]>>() {});
        if (result == null) {
            return ResponseData.errorMsg("AiTranslateRpcImpl.translateMap() returned null body");
        }
        return result;
    }
}
