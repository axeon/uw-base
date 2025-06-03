package uw.ai.rpc.impl;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uw.ai.conf.UwAiProperties;
import uw.ai.rpc.AiTranslateRpc;
import uw.ai.vo.AiTranslateListParam;
import uw.ai.vo.AiTranslateMapParam;
import uw.ai.vo.AiTranslateResultData;
import uw.common.dto.ResponseData;

import java.net.URI;

/**
 * AiToolRpcImpl.
 */
@Component
public class AiTranslateRpcImpl implements AiTranslateRpc {

    /**
     * Rest模板类
     */
    private final RestTemplate authRestTemplate;

    private final UwAiProperties uwAiProperties;

    public AiTranslateRpcImpl(RestTemplate authRestTemplate, UwAiProperties uwAiProperties) {
        this.authRestTemplate = authRestTemplate;
        this.uwAiProperties = uwAiProperties;
    }

    /**
     * 翻译列表实现。
     * @param param 翻译参数
     * @return 翻译结果数组
     */
    @Override
    public ResponseData<AiTranslateResultData[]> translateList(AiTranslateListParam param) {
        // 构建请求URL（假设基础路径为 uwAiProperties.getAiCenterHost()）
        URI url = UriComponentsBuilder.fromUriString(uwAiProperties.getAiCenterHost())
                .path("/rpc/translate/translateList")
                .build()
                .encode()
                .toUri();

        // 发送POST请求并处理响应
        ResponseEntity<ResponseData<AiTranslateResultData[]>> response = authRestTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(param),
                new ParameterizedTypeReference<ResponseData<AiTranslateResultData[]>>() {}
        );

        return response.getBody();
    }

    /**
     * 翻译Map实现。
     * @param param 翻译参数
     * @return 翻译结果数组
     */
    @Override
    public ResponseData<AiTranslateResultData[]> translateMap(AiTranslateMapParam param) {
        // 构建请求URL
        URI url = UriComponentsBuilder.fromUriString(uwAiProperties.getAiCenterHost())
                .path("/rpc/translate/translateMap")
                .build()
                .encode()
                .toUri();

        // 发送POST请求并处理响应
        ResponseEntity<ResponseData<AiTranslateResultData[]>> response = authRestTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(param),
                new ParameterizedTypeReference<ResponseData<AiTranslateResultData[]>>() {}
        );

        return response.getBody();
    }
}
