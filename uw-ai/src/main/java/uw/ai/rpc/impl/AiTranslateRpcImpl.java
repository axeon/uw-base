package uw.ai.rpc.impl;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import uw.ai.conf.UwAiProperties;
import uw.ai.rpc.AiTranslateRpc;
import uw.ai.vo.AiTranslateListParam;
import uw.ai.vo.AiTranslateMapParam;
import uw.ai.vo.AiTranslateResultData;
import uw.common.dto.ResponseData;

/**
 * AiToolRpcImpl.
 */
public class AiTranslateRpcImpl implements AiTranslateRpc {

    /**
     * Rest模板类
     */
    private final RestTemplate authRestTemplate;

    private final UwAiProperties uwAiProperties;

    public AiTranslateRpcImpl(UwAiProperties uwAiProperties, RestTemplate authRestTemplate) {
        this.authRestTemplate = authRestTemplate;
        this.uwAiProperties = uwAiProperties;
    }

    /**
     * 翻译列表实现。
     *
     * @param param 翻译参数
     * @return 翻译结果数组
     */
    @Override
    public ResponseData<AiTranslateResultData[]> translateList(AiTranslateListParam param) {
        String url = uwAiProperties.getAiCenterHost() + "/rpc/translate/translateList";
        ResponseData<AiTranslateResultData[]> result = authRestTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(param),
                new ParameterizedTypeReference<ResponseData<AiTranslateResultData[]>>() {
                }
        ).getBody();
        if (result == null) {
            return ResponseData.errorMsg("AiTranslateRpcImpl.translateList() returned null body");
        }
        return result;
    }

    /**
     * 翻译Map实现。
     *
     * @param param 翻译参数
     * @return 翻译结果数组
     */
    @Override
    public ResponseData<AiTranslateResultData[]> translateMap(AiTranslateMapParam param) {
        String url = uwAiProperties.getAiCenterHost() + "/rpc/translate/translateMap";
        ResponseData<AiTranslateResultData[]> result = authRestTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(param),
                new ParameterizedTypeReference<ResponseData<AiTranslateResultData[]>>() {
                }
        ).getBody();
        if (result == null) {
            return ResponseData.errorMsg("AiTranslateRpcImpl.translateMap() returned null body");
        }
        return result;
    }
}
